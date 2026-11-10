#!/usr/bin/env node
// Installs and launches a PRODUCTION Forge client (what a player's launcher runs: SRG names,
// real mod jars), unlike the Gradle dev client. Needed for mods that do not load in the dev
// environment, such as OptiFine.
//
//   node tools/prod-client.mjs <gameDir> [--user NAME] [--jvm "-Dfoo=bar ..."] [--timeout SEC]
//
// Mods are whatever is in <gameDir>/mods. Shared files (Forge, libraries, assets) live in
// run-dedicated/client-forge. Env: CLIENT_JAVA (default: JDK 8 from ~/.jdks).
import { spawn, spawnSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const props = Object.fromEntries(fs.readFileSync(path.join(ROOT, "gradle.properties"), "utf8")
  .split(/\r?\n/).filter(l => l.includes("=")).map(l => l.split("=").map(s => s.trim())));
const MC = props.minecraft_version;
const FORGE = props.forge_version;
const FORGE_ID = `${MC}-forge-${FORGE.split("-")[1]}`;
const HOME = path.join(ROOT, "run-dedicated", "client-forge");

const args = process.argv.slice(2);
const gameDir = path.resolve(args[0] ?? fail("usage: prod-client.mjs <gameDir> [--user N] [--jvm ARGS] [--timeout SEC]"));
const opt = name => { const i = args.indexOf(name); return i < 0 ? undefined : args[i + 1]; };
const user = opt("--user") ?? "CBTester";
const jvmExtra = (opt("--jvm") ?? "").split(" ").filter(Boolean);
const timeoutSec = Number(opt("--timeout") ?? 600);

function fail(msg) { console.error(msg); process.exit(2); }

function findJava8() {
  if (process.env.CLIENT_JAVA) return process.env.CLIENT_JAVA;
  const jdks = path.join(os.homedir(), ".jdks");
  const dir = fs.readdirSync(jdks).find(d => d.startsWith("jdk8"));
  if (!dir) fail("JDK 8 not found in ~/.jdks; set CLIENT_JAVA");
  return path.join(jdks, dir, "bin", process.platform === "win32" ? "java.exe" : "java");
}

async function download(url, file) {
  if (fs.existsSync(file) && fs.statSync(file).size > 0) return;
  fs.mkdirSync(path.dirname(file), { recursive: true });
  const res = await fetch(url);
  if (!res.ok) throw new Error(`${res.status} for ${url}`);
  fs.writeFileSync(file, Buffer.from(await res.arrayBuffer()));
}

const readJson = file => JSON.parse(fs.readFileSync(file, "utf8"));
const mavenPath = name => {
  const [group, artifact, version, classifier] = name.split(":");
  return `${group.replace(/\./g, "/")}/${artifact}/${version}/${artifact}-${version}${classifier ? "-" + classifier : ""}.jar`;
};

function allowed(lib) {
  if (!lib.rules) return true;
  let ok = false;
  for (const rule of lib.rules) {
    const matches = !rule.os || rule.os.name === "windows";
    if (matches) ok = rule.action === "allow";
  }
  return ok;
}

async function install(java) {
  fs.mkdirSync(HOME, { recursive: true });
  const vanillaJson = path.join(HOME, "versions", MC, `${MC}.json`);
  const forgeJson = path.join(HOME, "versions", FORGE_ID, `${FORGE_ID}.json`);
  if (!fs.existsSync(forgeJson)) {
    const installer = path.join(HOME, "installer.jar");
    await download(`https://maven.minecraftforge.net/net/minecraftforge/forge/${FORGE}/forge-${FORGE}-installer.jar`, installer);
    fs.writeFileSync(path.join(HOME, "launcher_profiles.json"), '{"profiles":{}}');
    console.log("Installing Forge client...");
    const r = spawnSync(java, ["-jar", installer, "--installClient", HOME], { cwd: HOME, encoding: "utf8" });
    fs.writeFileSync(path.join(HOME, "installer.log"), (r.stdout ?? "") + (r.stderr ?? ""));
    if (!fs.existsSync(forgeJson)) fail(`Forge installer failed, see ${path.join(HOME, "installer.log")}`);
  }
  if (!fs.existsSync(vanillaJson)) {
    const manifest = await (await fetch("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json")).json();
    await download(manifest.versions.find(v => v.id === MC).url, vanillaJson);
  }
  const vanilla = readJson(vanillaJson);
  await download(vanilla.downloads.client.url, path.join(HOME, "versions", MC, `${MC}.jar`));

  const classpath = [];
  const natives = path.join(HOME, "natives");
  for (const lib of [...readJson(forgeJson).libraries, ...vanilla.libraries]) {
    if (!allowed(lib)) continue;
    const artifact = lib.downloads?.artifact;
    const file = path.join(HOME, "libraries", artifact?.path ?? mavenPath(lib.name));
    if (artifact?.url) await download(artifact.url, file);
    if (fs.existsSync(file) && !classpath.includes(file)) classpath.push(file);
    const nativeKey = lib.natives?.windows;
    const native = nativeKey && lib.downloads?.classifiers?.[nativeKey];
    if (native) {
      const nativeJar = path.join(HOME, "libraries", native.path);
      await download(native.url, nativeJar);
      const marker = path.join(natives, path.basename(nativeJar) + ".done");
      if (!fs.existsSync(marker)) {
        fs.mkdirSync(natives, { recursive: true });
        spawnSync(path.join(path.dirname(java), "jar"), ["xf", nativeJar], { cwd: natives });
        fs.writeFileSync(marker, "");
      }
    }
  }
  classpath.push(path.join(HOME, "versions", MC, `${MC}.jar`));

  // Assets: reuse the Gradle (Loom) cache when present, otherwise download.
  const assets = path.join(HOME, "assets");
  const indexFile = path.join(assets, "indexes", `${vanilla.assetIndex.id}.json`);
  await download(vanilla.assetIndex.url, indexFile);
  const loomObjects = path.join(os.homedir(), ".gradle", "caches", "fabric-loom", "assets", "objects");
  const objects = Object.values(readJson(indexFile).objects);
  let fetched = 0;
  for (let i = 0; i < objects.length; i += 16) {
    await Promise.all(objects.slice(i, i + 16).map(async o => {
      const rel = path.join(o.hash.slice(0, 2), o.hash);
      const target = path.join(assets, "objects", rel);
      if (fs.existsSync(target)) return;
      fs.mkdirSync(path.dirname(target), { recursive: true });
      const cached = path.join(loomObjects, rel);
      if (fs.existsSync(cached)) fs.copyFileSync(cached, target);
      else { await download(`https://resources.download.minecraft.net/${o.hash.slice(0, 2)}/${o.hash}`, target); fetched++; }
    }));
  }
  if (fetched) console.log(`Downloaded ${fetched} assets.`);
  return { classpath, natives, assets, assetIndex: vanilla.assetIndex.id, forge: readJson(forgeJson) };
}

const java = findJava8();
const env = await install(java);
fs.mkdirSync(gameDir, { recursive: true });
const gameArgs = env.forge.minecraftArguments
  ? env.forge.minecraftArguments.split(" ")
  : env.forge.arguments.game.filter(a => typeof a === "string");
const values = {
  auth_player_name: user, version_name: FORGE_ID, game_directory: gameDir, assets_root: env.assets,
  assets_index_name: env.assetIndex, auth_uuid: "00000000-0000-0000-0000-000000000000",
  auth_access_token: "0", user_type: "legacy", version_type: "release",
};
const vanillaArgs = ["--username", "${auth_player_name}", "--version", "${version_name}", "--gameDir", "${game_directory}",
  "--assetsDir", "${assets_root}", "--assetIndex", "${assets_index_name}", "--uuid", "${auth_uuid}",
  "--accessToken", "${auth_access_token}", "--userType", "${user_type}", "--versionType", "${version_type}"];
const finalArgs = [...vanillaArgs, ...gameArgs, "--width", "1280", "--height", "720"]
  .map(a => a.replace(/\$\{(\w+)\}/g, (_, k) => values[k] ?? ""));

// Java 8 has no @argfiles; the classpath goes through the environment to stay under the
// Windows command line limit.
const child = spawn(java, ["-Xmx3G", `-Djava.library.path=${env.natives}`, ...jvmExtra,
  env.forge.mainClass, ...finalArgs],
  { cwd: gameDir, stdio: "inherit", env: { ...process.env, CLASSPATH: env.classpath.join(path.delimiter) } });
const timer = setTimeout(() => { console.error("Timed out, killing client."); child.kill("SIGKILL"); }, timeoutSec * 1000);
child.on("exit", code => { clearTimeout(timer); process.exit(code ?? 1); });
