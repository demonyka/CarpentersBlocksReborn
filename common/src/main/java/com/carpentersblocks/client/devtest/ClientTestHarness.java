package com.carpentersblocks.client.devtest;

import com.carpentersblocks.CarpentersBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Automated client run for development: creates a world, plays a {@link ClientScenario},
 * takes screenshots and quits. Enabled only with {@code -Dcarpentersblocks.clientTest=true}
 * (the {@code runClientTest} Gradle task). Inert in normal play.
 * <p>
 * Output in the run directory: {@code screenshots/cbtest-*.png} and {@code cbtest-report.txt}.
 */
public final class ClientTestHarness {
    public static final String DONE = "CARPENTERS CLIENT TEST DONE";

    private static final List<Step> STEPS = new ArrayList<>();
    private static final List<String> REPORT = new ArrayList<>();
    private static boolean started;
    private static int index;
    private static int delay;
    private static int failures;

    private ClientTestHarness() {
    }

    /** "true" for the single-player run, "mp-a" / "mp-b" for the two multiplayer roles. */
    public static String mode() {
        return System.getProperty("carpentersblocks.clientTest", "");
    }

    public static boolean enabled() {
        return !mode().isEmpty() && !mode().equals("false");
    }

    // ---- step building (used by ClientScenario) --------------------------------------

    static void step(String name, Runnable action, int ticksAfter) {
        STEPS.add(new Step(name, action, null, 0, ticksAfter));
    }

    static void waitUntil(String name, BooleanSupplier condition, int timeoutTicks, int ticksAfter) {
        STEPS.add(new Step(name, () -> { }, condition, timeoutTicks, ticksAfter));
    }

    static void check(String what, boolean ok) {
        record((ok ? "PASS  " : "FAIL  ") + what);
        if (!ok) {
            failures++;
        }
    }

    static void record(String line) {
        REPORT.add(line);
        CarpentersBlocks.LOGGER.info("[ClientTest] {}", line);
    }

    // ---- driver -----------------------------------------------------------------------

    /** Called every client tick (end phase) by the loader-specific client entry point. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (!started) {
            if (!(mc.screen instanceof TitleScreen)) {
                return;
            }
            started = true;
            if (mode().startsWith("mp-")) {
                MultiplayerScenario.build(mode().substring(3));
            } else {
                ClientScenario.build();
            }
        }
        if (delay > 0) {
            delay--;
            return;
        }
        if (index >= STEPS.size()) {
            return;
        }
        Step step = STEPS.get(index);
        if (step.condition != null && !step.condition.getAsBoolean()) {
            if (++step.waited > step.timeout) {
                check("timed out: " + step.name, false);
                finish(mc);
            }
            return;
        }
        try {
            step.action.run();
        } catch (Throwable t) {
            CarpentersBlocks.LOGGER.error("[ClientTest] step failed: " + step.name, t);
            check("step '" + step.name + "' threw " + t, false);
        }
        delay = step.ticksAfter;
        index++;
        if (index == STEPS.size()) {
            finish(mc);
        }
    }

    private static void finish(Minecraft mc) {
        index = STEPS.size();
        record(failures == 0 ? "RESULT: all checks passed" : "RESULT: " + failures + " check(s) failed");
        Path report = mc.gameDirectory.toPath().resolve(mode().startsWith("mp-") ? "cbtest-report-" + mode() + ".txt" : "cbtest-report.txt");
        try {
            Files.write(report, REPORT, StandardCharsets.UTF_8);
        } catch (IOException e) {
            CarpentersBlocks.LOGGER.error("[ClientTest] could not write report", e);
        }
        CarpentersBlocks.LOGGER.info(DONE);
        mc.stop();
    }

    private static final class Step {
        final String name;
        final Runnable action;
        final BooleanSupplier condition;
        final int timeout;
        final int ticksAfter;
        int waited;

        Step(String name, Runnable action, BooleanSupplier condition, int timeout, int ticksAfter) {
            this.name = name;
            this.action = action;
            this.condition = condition;
            this.timeout = timeout;
            this.ticksAfter = ticksAfter;
        }
    }
}
