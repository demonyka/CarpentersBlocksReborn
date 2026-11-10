package com.carpentersblocks.client.devtest;

import com.carpentersblocks.block.CarpentersBlock;
import com.carpentersblocks.block.CoverableBlock;
import com.carpentersblocks.block.SlabShapeProperty;
import com.carpentersblocks.blockentity.CoverableBlockEntity;
import com.carpentersblocks.core.attribute.AttributeKey;
import com.carpentersblocks.core.attribute.AttributeLocation;
import com.carpentersblocks.core.attribute.AttributeType;
import com.carpentersblocks.registry.CBContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * What the automated client run does. Scene: a stone platform high in the sky with one
 * exhibit per feature in the front row (z = 0) and an interaction test row (z = 4) where
 * the client clicks like a real player.
 */
final class ClientScenario {
    private static final int Y = 200;
    private static final AttributeKey COVER = AttributeKey.of(AttributeType.COVER, AttributeLocation.HOST);
    private static final AttributeKey DYE = AttributeKey.of(AttributeType.DYE, AttributeLocation.HOST);
    private static final AttributeKey LIGHT = AttributeKey.of(AttributeType.ILLUMINATOR, AttributeLocation.HOST);
    private static final AttributeKey NORTH_COVER = AttributeKey.of(AttributeType.COVER, AttributeLocation.NORTH);

    private ClientScenario() {
    }

    static void build() {
        Minecraft mc = Minecraft.getInstance();

        ClientTestHarness.step("create world", () -> {
            mc.options.pauseOnLostFocus = false;
            RegistryAccess.RegistryHolder registries = RegistryAccess.builtin();
            WorldGenSettings gen = WorldGenSettings.makeDefault(
                registries.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY),
                registries.registryOrThrow(Registry.BIOME_REGISTRY),
                registries.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY));
            LevelSettings settings = new LevelSettings("cbtest-" + System.currentTimeMillis(), GameType.CREATIVE,
                false, Difficulty.PEACEFUL, true, new GameRules(), DataPackConfig.DEFAULT);
            mc.createLevel(settings.levelName(), settings, registries, gen);
        }, 0);
        ClientTestHarness.waitUntil("join world",
            () -> mc.player != null && mc.level != null && mc.getSingleplayerServer() != null && mc.screen == null, 2400, 40);

        if (ClientTestHarness.mode().equals("showcase")) {
            ShowcaseScenario.build();
            return;
        }
        if (ClientTestHarness.mode().equals("look")) {
            LookScenario.build();
            return;
        }

        // The world spawn can be far from (0, 0): go there first and let the chunks load,
        // otherwise /fill refuses to touch unloaded chunks and there is no platform.
        ClientTestHarness.step("fly to test area", () -> onServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            player.abilities.flying = true;
            player.onUpdateAbilities();
            player.teleportTo(server.overworld(), 0.5, Y + 1, -10, 0, 25);
        }), 100);
        ClientTestHarness.waitUntil("test area loaded",
            () -> mc.level.hasChunkAt(new BlockPos(8, Y, 8)) && mc.level.hasChunkAt(new BlockPos(-8, Y, -8)), 1200, 20);

        ClientTestHarness.step("build scene", () -> onServer(server -> {
            command(server, "gamerule doDaylightCycle false");
            command(server, "gamerule doWeatherCycle false");
            command(server, "gamerule doMobSpawning false");
            command(server, "time set 6000");
            command(server, "weather clear");
            command(server, "fill -9 " + (Y - 1) + " -9 9 " + (Y - 1) + " 9 minecraft:smooth_stone");
            ClientTestHarness.check("platform was built",
                server.overworld().getBlockState(new BlockPos(9, Y - 1, 9)).getBlock() == net.minecraft.world.level.block.Blocks.SMOOTH_STONE);
            command(server, "fill -8 " + Y + " -8 8 " + (Y + 6) + " 8 minecraft:air");
            ServerLevel level = server.overworld();

            exhibit(level, -5, SlabShapeProperty.FULL, be -> { });
            exhibit(level, -3, SlabShapeProperty.DOWN, be -> { });
            exhibit(level, -1, SlabShapeProperty.FULL, be -> be.putItem(COVER, new ItemStack(Items.STONE)));
            exhibit(level, 1, SlabShapeProperty.FULL, be -> {
                be.putItem(COVER, new ItemStack(Items.OAK_PLANKS));
                be.putItem(DYE, new ItemStack(Items.RED_DYE));
            });
            exhibit(level, 3, SlabShapeProperty.FULL, be -> be.putItem(LIGHT, new ItemStack(Items.GLOWSTONE_DUST)));
            exhibit(level, 5, SlabShapeProperty.FULL, be -> {
                be.putItem(COVER, new ItemStack(Items.STONE));
                be.putItem(NORTH_COVER, new ItemStack(Items.BRICKS));
            });
            // Row B (z = -3): covers that are hard to render.
            exhibitAt(level, -6, -3, SlabShapeProperty.FULL, be -> be.putItem(COVER, new ItemStack(Items.GRASS_BLOCK)));
            exhibitAt(level, -4, -3, SlabShapeProperty.FULL, be -> be.putItem(COVER, new ItemStack(Items.OAK_LOG)));
            exhibitAt(level, -2, -3, SlabShapeProperty.FULL, be -> be.putItem(COVER, new ItemStack(Items.GLASS)));
            exhibitAt(level, 0, -3, SlabShapeProperty.FULL, be -> be.putItem(COVER, new ItemStack(Items.OAK_LEAVES)));
            exhibitAt(level, 2, -3, SlabShapeProperty.FULL, be -> {
                be.putItem(COVER, new ItemStack(Items.WHITE_WOOL));
                be.putItem(DYE, new ItemStack(Items.BLUE_DYE));
            });
            exhibitAt(level, 4, -3, SlabShapeProperty.DOWN, be -> be.putItem(COVER, new ItemStack(Items.BRICKS)));
            exhibitAt(level, 6, -3, SlabShapeProperty.FULL, be -> be.putItem(COVER, new ItemStack(Items.GLOWSTONE)));
            // Two covered blocks side by side: textures must line up, no seam faces between them.
            exhibitAt(level, -7, 0, SlabShapeProperty.FULL, be -> be.putItem(COVER, new ItemStack(Items.STONE_BRICKS)));
            exhibitAt(level, -8, 0, SlabShapeProperty.FULL, be -> be.putItem(COVER, new ItemStack(Items.STONE_BRICKS)));

            // Interaction row: bare blocks the client will click.
            for (int x = -3; x <= 3; x += 3) {
                level.setBlockAndUpdate(new BlockPos(x, Y, 4), CBContent.BLOCK.get().defaultBlockState());
            }
        }), 20);

        // ---- interactions through the real client input path ---------------------------
        BlockPos coverPos = new BlockPos(-3, Y, 4);
        BlockPos dyePos = new BlockPos(0, Y, 4);
        BlockPos hammerPos = new BlockPos(3, Y, 4);

        ClientTestHarness.step("stand next to interaction row", () -> teleport(0.5, Y, 2.5, 0, 30), 40);

        ClientTestHarness.step("check client received covers", () -> {
            BlockEntity clientBe = mc.level.getBlockEntity(new BlockPos(-1, Y, 0));
            ClientTestHarness.check("client block entity has the stone cover (sync works)",
                clientBe instanceof CoverableBlockEntity
                    && ((CoverableBlockEntity) clientBe).getRenderData().cover(AttributeLocation.HOST) != null);
        }, 0);

        give(Items.STONE);
        ClientTestHarness.step("right-click stone onto block", () -> useOn(coverPos, Direction.NORTH), 10);
        ClientTestHarness.step("check stone cover", () -> onServer(server -> {
            CoverableBlockEntity be = be(server, coverPos);
            ClientTestHarness.check("right-click with stone sets host cover",
                be != null && be.getItem(COVER).getItem() == Items.STONE);
        }), 0);

        give(Items.RED_DYE);
        ClientTestHarness.step("sneak", () -> mc.options.keyShift.setDown(true), 5);
        ClientTestHarness.step("sneak-right-click dye", () -> useOn(dyePos, Direction.NORTH), 10);
        ClientTestHarness.step("check dye", () -> onServer(server -> {
            CoverableBlockEntity be = be(server, dyePos);
            ClientTestHarness.check("sneak + right-click with dye sets host dye", be != null && be.has(DYE));
        }), 0);
        ClientTestHarness.step("unsneak", () -> mc.options.keyShift.setDown(false), 5);

        give(CBContent.HAMMER.get());
        ClientTestHarness.step("hammer right-click top", () -> useOn(hammerPos, Direction.UP), 10);
        ClientTestHarness.step("check slab", () -> onServer(server -> ClientTestHarness.check(
            "hammer on top face makes a bottom slab",
            server.overworld().getBlockState(hammerPos).getValue(CarpentersBlock.SHAPE) == SlabShapeProperty.DOWN)), 0);

        ClientTestHarness.step("sneak", () -> mc.options.keyShift.setDown(true), 5);
        ClientTestHarness.step("precondition: block is covered", () -> onServer(server -> {
            CoverableBlockEntity be = be(server, coverPos);
            ClientTestHarness.check("hammer test starts with a covered block", be != null && be.has(COVER));
        }), 0);
        ClientTestHarness.step("sneak-left-click hammer on covered block", () -> attack(coverPos, Direction.NORTH), 10);
        ClientTestHarness.step("check cover removed", () -> onServer(server -> {
            CoverableBlockEntity be = be(server, coverPos);
            ClientTestHarness.check("sneak + left-click with hammer removes the cover", be != null && !be.has(COVER));
            ClientTestHarness.check("the block itself survives the hammer",
                server.overworld().getBlockState(coverPos).getBlock() == CBContent.BLOCK.get());
            // The drop may already have been picked up by the nearby player: count both.
            long onGround = server.overworld().getEntitiesOfClass(ItemEntity.class, new AABB(coverPos).inflate(4))
                .stream().filter(e -> e.isAlive() && e.getItem().getItem() == Items.STONE).mapToLong(e -> e.getItem().getCount()).sum();
            int picked = server.getPlayerList().getPlayers().get(0).inventory.countItem(Items.STONE);
            ClientTestHarness.check("removed cover dropped as one item (ground " + onGround + ", picked up " + picked + ")",
                onGround + picked == 1);
        }), 0);
        ClientTestHarness.step("unsneak", () -> mc.options.keyShift.setDown(false), 5);

        ClientTestHarness.step("check illuminator light", () -> onServer(server -> ClientTestHarness.check(
            "illuminated block emits light 15",
            server.overworld().getBlockState(new BlockPos(3, Y, 0)).getValue(CoverableBlock.LIGHT) == 15)), 0);

        // ---- screenshots ---------------------------------------------------------------
        ClientTestHarness.step("hide hud", () -> {
            mc.options.hideGui = true;
            mc.getToasts().clear();
        }, 0);
        shot("overview_day", 0.5, Y + 4, -10, 0, 25);
        shot("rowB_left", -3, Y + 1.5, -6.5, -15, 15);
        shot("rowB_right", 4, Y + 1.5, -6.5, 15, 15);
        shot("rowA_left", -4, Y + 1.5, -1.6, -10, 25);
        shot("rowA_right", 3, Y + 1.5, -1.6, 10, 25);
        shot("north_side_cover", 5.5, Y + 0.5, -1.2, 0, 5);
        shot("interaction_row", 0.5, Y + 2.5, 1.5, 0, 40);
        ClientTestHarness.step("night", () -> onServer(server -> command(server, "time set 18000")), 0);
        shot("overview_night", 0.5, Y + 4, -10, 0, 25);

        SlopeScenario.build();
        BlocksScenario.build();
        ConnectedScenario.build();
    }

    // ---- helpers ---------------------------------------------------------------------

    private static void exhibit(ServerLevel level, int x, SlabShapeProperty shape, Consumer<CoverableBlockEntity> setup) {
        exhibitAt(level, x, 0, shape, setup);
    }

    private static void exhibitAt(ServerLevel level, int x, int z, SlabShapeProperty shape, Consumer<CoverableBlockEntity> setup) {
        BlockPos pos = new BlockPos(x, Y, z);
        level.setBlockAndUpdate(pos, CBContent.BLOCK.get().defaultBlockState().setValue(CarpentersBlock.SHAPE, shape));
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CoverableBlockEntity) {
            setup.accept((CoverableBlockEntity) be);
        }
    }

    static void give(net.minecraft.world.item.Item item) {
        ClientTestHarness.step("give " + item, () -> onServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            player.inventory.setItem(0, new ItemStack(item, 16));
        }), 0);
        ClientTestHarness.step("select slot 0", () -> Minecraft.getInstance().player.inventory.selected = 0, 5);
    }

    static void useOn(BlockPos pos, Direction face) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 hit = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        mc.gameMode.useItemOn(mc.player, mc.level, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, pos, false));
    }

    static void attack(BlockPos pos, Direction face) {
        Minecraft mc = Minecraft.getInstance();
        mc.gameMode.startDestroyBlock(pos, face);
        mc.gameMode.stopDestroyBlock();
    }

    static void teleport(double x, double y, double z, float yaw, float pitch) {
        onServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            // Sneaking while flying descends, and landing turns flight off: re-enable every time.
            player.abilities.flying = true;
            player.onUpdateAbilities();
            player.teleportTo(server.overworld(), x, y, z, yaw, pitch);
        });
    }

    /** Teleports, waits for chunks to re-render, then saves screenshots/cbtest-NAME.png. */
    static void shot(String name, double x, double y, double z, float yaw, float pitch) {
        ClientTestHarness.step("move for " + name, () -> teleport(x, y, z, yaw, pitch), 60);
        ClientTestHarness.step("screenshot " + name, () -> {
            Minecraft mc = Minecraft.getInstance();
            Screenshot.grab(mc.gameDirectory, "cbtest-" + name + ".png", mc.getWindow().getWidth(),
                mc.getWindow().getHeight(), mc.getMainRenderTarget(), message -> { });
            ClientTestHarness.record("SHOT  screenshots/cbtest-" + name + ".png");
        }, 10);
    }

    private static CoverableBlockEntity be(MinecraftServer server, BlockPos pos) {
        BlockEntity be = server.overworld().getBlockEntity(pos);
        return be instanceof CoverableBlockEntity ? (CoverableBlockEntity) be : null;
    }

    static void command(MinecraftServer server, String command) {
        server.getCommands().performCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
    }

    /** Runs on the integrated server thread and waits for it. */
    static void onServer(Consumer<MinecraftServer> action) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        AtomicBoolean done = new AtomicBoolean();
        server.executeBlocking(() -> {
            action.accept(server);
            done.set(true);
        });
        if (!done.get()) {
            throw new IllegalStateException("server task did not run");
        }
    }
}
