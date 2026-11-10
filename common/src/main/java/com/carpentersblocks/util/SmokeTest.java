package com.carpentersblocks.util;

import com.carpentersblocks.CarpentersBlocks;
import com.carpentersblocks.block.CoverableBlock;
import com.carpentersblocks.blockentity.CoverableBlockEntity;
import com.carpentersblocks.core.attribute.AttributeKey;
import com.carpentersblocks.core.attribute.AttributeLocation;
import com.carpentersblocks.core.attribute.AttributeType;
import com.carpentersblocks.core.interaction.HeldItemKind;
import com.carpentersblocks.interaction.ItemClassifier;
import com.carpentersblocks.platform.Platform;
import com.carpentersblocks.registry.CBContent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Automated check run on a real dedicated server when started with
 * {@code -Dcarpentersblocks.smokeTest=true} (see {@code tools/smoke-test.sh}).
 * Exercises placement, attributes, light, save/load and drops, then stops the server.
 * The script looks for {@link #PASSED} in the log.
 */
public final class SmokeTest {
    public static final String PASSED = "CARPENTERS SMOKE TEST PASSED";
    public static final String FAILED = "CARPENTERS SMOKE TEST FAILED";

    private SmokeTest() {
    }

    public static boolean enabled() {
        return Boolean.getBoolean("carpentersblocks.smokeTest");
    }

    public static void onServerStarted(MinecraftServer server) {
        if (!enabled()) {
            return;
        }
        List<String> failures = new ArrayList<>();
        try {
            run(server.overworld(), failures);
        } catch (Throwable t) {
            CarpentersBlocks.LOGGER.error("Smoke test crashed", t);
            failures.add("exception: " + t);
        }
        if (failures.isEmpty()) {
            CarpentersBlocks.LOGGER.info(PASSED);
        } else {
            CarpentersBlocks.LOGGER.error("{}: {}", FAILED, failures);
        }
        server.halt(false);
    }

    private static void run(ServerLevel level, List<String> failures) {
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos pos = new BlockPos(spawn.getX(), 250, spawn.getZ());
        AttributeKey cover = AttributeKey.of(AttributeType.COVER, AttributeLocation.HOST);
        AttributeKey dye = AttributeKey.of(AttributeType.DYE, AttributeLocation.HOST);
        AttributeKey light = AttributeKey.of(AttributeType.ILLUMINATOR, AttributeLocation.HOST);

        check(failures, "stone is a cover", ItemClassifier.classify(new ItemStack(Items.STONE)) == HeldItemKind.COVER);
        check(failures, "chest is not a cover", ItemClassifier.classify(new ItemStack(Items.CHEST)) == HeldItemKind.OTHER);
        check(failures, "glowstone dust is an illuminator",
            ItemClassifier.classify(new ItemStack(Items.GLOWSTONE_DUST)) == HeldItemKind.ILLUMINATOR);

        level.setBlockAndUpdate(pos, CBContent.BLOCK.get().defaultBlockState());
        BlockEntity raw = level.getBlockEntity(pos);
        if (!(raw instanceof CoverableBlockEntity)) {
            failures.add("block entity missing: " + raw);
            return;
        }
        CoverableBlockEntity be = (CoverableBlockEntity) raw;
        be.putItem(cover, new ItemStack(Items.STONE, 5));
        be.putItem(dye, new ItemStack(Items.RED_DYE));
        be.putItem(light, new ItemStack(Items.GLOWSTONE_DUST));

        check(failures, "only one item stored", be.getItem(cover).getCount() == 1);
        check(failures, "illuminator lights block", level.getBlockState(pos).getValue(CoverableBlock.LIGHT) == 15);

        CompoundTag saved = be.save(new CompoundTag());
        CoverableBlockEntity copy = Platform.createCoverableBlockEntity();
        copy.load(level.getBlockState(pos), saved);
        check(failures, "save/load keeps cover", copy.getItem(cover).getItem() == Items.STONE);
        check(failures, "save/load keeps dye", copy.getItem(dye).getItem() == Items.RED_DYE);

        be.removeAndDrop(java.util.Collections.singletonList(light));
        check(failures, "removing illuminator turns light off", level.getBlockState(pos).getValue(CoverableBlock.LIGHT) == 0);

        BlockState before = level.getBlockState(pos);
        check(failures, "block still present", before.getBlock() == CBContent.BLOCK.get());
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());

        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3));
        long stone = drops.stream().filter(e -> e.getItem().getItem() == Items.STONE).count();
        long redDye = drops.stream().filter(e -> e.getItem().getItem() == Items.RED_DYE).count();
        long glow = drops.stream().filter(e -> e.getItem().getItem() == Items.GLOWSTONE_DUST).count();
        check(failures, "cover dropped once (got " + stone + ")", stone == 1);
        check(failures, "dye dropped once (got " + redDye + ")", redDye == 1);
        check(failures, "illuminator dropped once (got " + glow + ")", glow == 1);
        drops.forEach(ItemEntity::remove);

        runSlopes(level, pos, failures);
    }

    private static void runSlopes(ServerLevel level, BlockPos pos, List<String> failures) {
        com.carpentersblocks.block.SlopeBlock slope = CBContent.SLOPE.get();
        for (com.carpentersblocks.core.shape.SlopeType type : com.carpentersblocks.core.shape.SlopeType.values()) {
            BlockState state = slope.defaultBlockState()
                .setValue(com.carpentersblocks.block.SlopeBlock.TYPE, com.carpentersblocks.block.SlopeTypeProperty.of(type));
            level.setBlockAndUpdate(pos, state);
            check(failures, type + " has collision", !state.getShape(level, pos).isEmpty());
            check(failures, type + " has faces", !slope.getRenderPolygons(state).isEmpty());

            level.destroyBlock(pos, true);
            net.minecraft.world.item.Item expected = CBContent.slopeItem(com.carpentersblocks.item.SlopeItem.kindOf(type));
            // Removed entities linger until the next tick: count only live ones.
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3), ItemEntity::isAlive);
            long count = drops.stream().filter(e -> e.getItem().getItem() == expected).count();
            check(failures, type + " drops " + expected + " (got " + drops + ")", count == 1 && drops.size() == 1);
            drops.forEach(ItemEntity::remove);
        }

        BlockState wedge = slope.defaultBlockState()
            .setValue(com.carpentersblocks.block.SlopeBlock.TYPE, com.carpentersblocks.block.SlopeTypeProperty.WEDGE_POS_N);
        check(failures, "wedge allows a side cover on its full back face",
            slope.canCover(wedge, AttributeLocation.SOUTH));
        check(failures, "wedge refuses a side cover on its sloped side",
            !slope.canCover(wedge, AttributeLocation.NORTH));
        check(failures, "wedge back face is sturdy (torches attach)",
            wedge.isFaceSturdy(level, pos, net.minecraft.core.Direction.SOUTH));

        runBlocks(level, pos.below(50), failures);  // room above for doors and garage doors
    }

    /** Every other block: placed on a stone pedestal, has faces, drops itself; plus key behaviour. */
    private static void runBlocks(ServerLevel level, BlockPos pos, List<String> failures) {
        net.minecraft.core.Direction up = net.minecraft.core.Direction.UP;
        BlockState stone = Blocks.STONE.defaultBlockState();
        java.util.function.Supplier<?>[] simple = {
            com.carpentersblocks.registry.CBBlocks.STAIRS, com.carpentersblocks.registry.CBBlocks.COLLAPSIBLE,
            com.carpentersblocks.registry.CBBlocks.BARRIER, com.carpentersblocks.registry.CBBlocks.GATE,
            com.carpentersblocks.registry.CBBlocks.BUTTON, com.carpentersblocks.registry.CBBlocks.LEVER,
            com.carpentersblocks.registry.CBBlocks.PRESSURE_PLATE,
            com.carpentersblocks.registry.CBBlocks.TORCH,
        };
        for (java.util.function.Supplier<?> supplier : simple) {
            com.carpentersblocks.block.CoverableBlock block = (com.carpentersblocks.block.CoverableBlock) supplier.get();
            level.setBlockAndUpdate(pos.below(), stone);
            level.setBlockAndUpdate(pos, block.defaultBlockState());
            BlockState placed = level.getBlockState(pos);
            check(failures, block + " stays placed", placed.getBlock() == block);
            check(failures, block + " has faces", !block.getRenderPolygons(placed).isEmpty());
            level.destroyBlock(pos, true);
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3), ItemEntity::isAlive);
            check(failures, block + " drops itself (got " + drops + ")",
                drops.size() == 1 && drops.get(0).getItem().getItem() == block.asItem());
            drops.forEach(ItemEntity::remove);
        }

        // Button and lever: a signal while active, inverted by polarity.
        com.carpentersblocks.block.LeverBlock lever = com.carpentersblocks.registry.CBBlocks.LEVER.get();
        BlockState on = lever.defaultBlockState().setValue(com.carpentersblocks.block.LeverBlock.POWERED, true);
        check(failures, "lever on gives 15", lever.output(on) == 15);
        check(failures, "lever off gives 0", lever.output(on.setValue(com.carpentersblocks.block.LeverBlock.POWERED, false)) == 0);
        check(failures, "inverted lever off gives 15",
            lever.output(on.setValue(com.carpentersblocks.block.LeverBlock.POWERED, false).setValue(com.carpentersblocks.block.PolarityBlock.INVERTED, true)) == 15);
        check(failures, "lever powers its support strongly", on.getDirectSignal(level, pos, up) == 15);

        // Door: two halves that open together; breaking either drops exactly one door.
        com.carpentersblocks.block.DoorBlock door = com.carpentersblocks.registry.CBBlocks.DOOR.get();
        level.setBlockAndUpdate(pos.below(), stone);
        BlockState lower = door.defaultBlockState();
        level.setBlockAndUpdate(pos, lower);
        door.setPlacedBy(level, pos, lower, null, ItemStack.EMPTY);
        check(failures, "door has an upper half", level.getBlockState(pos.above()).getBlock() == door);
        level.setBlockAndUpdate(pos, lower.setValue(com.carpentersblocks.block.DoorBlock.OPEN, true));
        check(failures, "upper door half follows the lower one",
            level.getBlockState(pos.above()).getValue(com.carpentersblocks.block.DoorBlock.OPEN));
        level.destroyBlock(pos.above(), true);
        check(failures, "breaking the top removes the bottom", level.getBlockState(pos).isAir());
        List<ItemEntity> doorDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3), ItemEntity::isAlive);
        long doors = doorDrops.stream().filter(e -> e.getItem().getItem() == door.asItem()).mapToLong(e -> e.getItem().getCount()).sum();
        check(failures, "a broken door drops one door (got " + doorDrops + ")", doors == 1);
        doorDrops.forEach(ItemEntity::remove);

        // Garage door: hangs from a ceiling to the floor, breaks as a whole, drops one item.
        com.carpentersblocks.block.GarageDoorBlock garage = com.carpentersblocks.registry.CBBlocks.GARAGE_DOOR.get();
        BlockPos ceiling = pos.above(4);
        level.setBlockAndUpdate(pos.below(), stone);
        level.setBlockAndUpdate(ceiling, stone);
        BlockPos top = ceiling.below();
        BlockState host = garage.defaultBlockState();
        level.setBlockAndUpdate(top, host);
        garage.setPlacedBy(level, top, host, null, ItemStack.EMPTY);
        check(failures, "garage door reaches the floor", level.getBlockState(pos).getBlock() == garage);
        level.destroyBlock(pos, true);
        check(failures, "garage door column is gone", level.getBlockState(top).isAir() && level.getBlockState(pos.above()).isAir());
        List<ItemEntity> garageDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(5), ItemEntity::isAlive);
        long garages = garageDrops.stream().filter(e -> e.getItem().getItem() == garage.asItem()).mapToLong(e -> e.getItem().getCount()).sum();
        check(failures, "a broken garage door drops one item (got " + garageDrops + ")", garages == 1);
        garageDrops.forEach(ItemEntity::remove);

        runRecipes(level, failures);
        runBehaviour(level, pos.east(4), failures);
        runRegressions(level, pos.east(12), failures);
    }

    /** Every item of the mod has a recipe that loaded on this server, and every recipe of the mod makes one of its items. */
    private static void runRecipes(ServerLevel level, List<String> failures) {
        java.util.Set<net.minecraft.world.item.Item> crafted = new java.util.HashSet<>();
        int recipes = 0;
        for (net.minecraft.world.item.crafting.Recipe<?> recipe : level.getRecipeManager().getRecipes()) {
            if (recipe.getId().getNamespace().equals(CarpentersBlocks.MOD_ID)) {
                recipes++;
                net.minecraft.world.item.Item result = recipe.getResultItem().getItem();
                crafted.add(result);
                check(failures, "recipe " + recipe.getId() + " makes an item of the mod (" + result + ")",
                    net.minecraft.core.Registry.ITEM.getKey(result).getNamespace().equals(CarpentersBlocks.MOD_ID));
                check(failures, "recipe " + recipe.getId() + " has ingredients", !recipe.getIngredients().isEmpty());
            }
        }
        List<String> missing = new ArrayList<>();
        int items = 0;
        for (net.minecraft.world.item.Item item : net.minecraft.core.Registry.ITEM) {
            if (net.minecraft.core.Registry.ITEM.getKey(item).getNamespace().equals(CarpentersBlocks.MOD_ID)) {
                items++;
                if (!crafted.contains(item)) {
                    missing.add(net.minecraft.core.Registry.ITEM.getKey(item).getPath());
                }
            }
        }
        check(failures, "every item has a recipe (missing " + missing + ")", missing.isEmpty());
        CarpentersBlocks.LOGGER.info("[SmokeTest] {} items, {} recipes loaded", items, recipes);
    }

    /** Bugs found in review: each of these once crashed, duplicated or lost something. */
    private static void runRegressions(ServerLevel level, BlockPos pos, List<String> failures) {
        BlockState stone = Blocks.STONE.defaultBlockState();
        AttributeKey cover = AttributeKey.of(AttributeType.COVER, AttributeLocation.HOST);

        // Door: removing the LOWER half used to throw in the upper half's neighbour update.
        com.carpentersblocks.block.DoorBlock door = com.carpentersblocks.registry.CBBlocks.DOOR.get();
        level.setBlockAndUpdate(pos.below(), stone);
        for (int round = 0; round < 3; round++) {
            BlockState lower = door.defaultBlockState();
            level.setBlockAndUpdate(pos, lower);
            door.setPlacedBy(level, pos, lower, null, ItemStack.EMPTY);
            if (round == 0) {
                level.destroyBlock(pos, true);                       // a player breaking the bottom
            } else if (round == 1) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());   // /setblock
            } else {
                level.setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState()); // the floor goes
                level.setBlockAndUpdate(pos.below(), stone);
            }
            check(failures, "door round " + round + ": both halves gone",
                level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir());
        }
        List<ItemEntity> doorDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3), ItemEntity::isAlive);
        long doors = doorDrops.stream().filter(e -> e.getItem().getItem() == door.asItem()).mapToLong(e -> e.getItem().getCount()).sum();
        check(failures, "doors broken from below drop one each, except /setblock (got " + doors + ")", doors == 2);
        doorDrops.forEach(ItemEntity::remove);

        // Garage door, 3 wide and 3 tall: redstone opens and closes every piece, once.
        com.carpentersblocks.block.GarageDoorBlock garage = com.carpentersblocks.registry.CBBlocks.GARAGE_DOOR.get();
        BlockPos top = pos.above(2);
        for (int x = 0; x < 3; x++) {
            level.setBlockAndUpdate(pos.east(x).below(), stone);
            level.setBlockAndUpdate(top.east(x).above(), stone);
            BlockState host = garage.defaultBlockState();
            level.setBlockAndUpdate(top.east(x), host);
            garage.setPlacedBy(level, top.east(x), host, null, ItemStack.EMPTY);
        }
        check(failures, "garage door is 3 high", level.getBlockState(pos.east(2)).getBlock() == garage);
        level.setBlockAndUpdate(pos.east(3), Blocks.REDSTONE_BLOCK.defaultBlockState());
        boolean allOpen = true, allClosed = true;
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                allOpen &= level.getBlockState(pos.offset(x, y, 0)).getValue(com.carpentersblocks.block.GarageDoorBlock.OPEN);
            }
        }
        check(failures, "redstone opens every garage door piece", allOpen);
        level.setBlockAndUpdate(pos.east(3), Blocks.AIR.defaultBlockState());
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                allClosed &= !level.getBlockState(pos.offset(x, y, 0)).getValue(com.carpentersblocks.block.GarageDoorBlock.OPEN);
            }
        }
        check(failures, "removing the signal closes every garage door piece", allClosed);
        for (int x = 0; x < 3; x++) {
            level.destroyBlock(top.east(x), false);
            level.setBlockAndUpdate(top.east(x).above(), Blocks.AIR.defaultBlockState());
        }
        check(failures, "garage door removed", level.getBlockState(pos.east(1)).isAir());
        level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(6), ItemEntity::isAlive).forEach(ItemEntity::remove);

        // Crafted NBT: a slot never holds more than one item, and only blocks can be covers.
        level.setBlockAndUpdate(pos, CBContent.BLOCK.get().defaultBlockState());
        CoverableBlockEntity be = (CoverableBlockEntity) level.getBlockEntity(pos);
        be.putItem(cover, new ItemStack(Items.STONE));
        CompoundTag forged = be.save(new CompoundTag());
        forged.getCompound("cbItems").getCompound(cover.id()).putByte("Count", (byte) 64);
        forged.getCompound("cbItems").put(AttributeKey.of(AttributeType.COVER, AttributeLocation.UP).id(),
            new ItemStack(Items.DIAMOND, 64).save(new CompoundTag()));
        be.load(level.getBlockState(pos), forged);
        check(failures, "a forged stack of 64 covers loads as one", be.getItem(cover).getCount() == 1);
        check(failures, "a forged non-block cover is ignored",
            !be.has(AttributeKey.of(AttributeType.COVER, AttributeLocation.UP)));

        // Moving a block with its contents (/clone ... move) must not also drop the contents.
        be.clearContent();
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        check(failures, "a cleared block drops nothing",
            level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3), ItemEntity::isAlive).isEmpty());

        // Structure rotation: four quarter turns are the identity, one turn changes the state.
        BlockState slope = CBContent.SLOPE.get().defaultBlockState();
        BlockState turned = slope.rotate(net.minecraft.world.level.block.Rotation.CLOCKWISE_90);
        check(failures, "rotating a slope changes it", turned != slope);
        check(failures, "four quarter turns of a slope are the identity",
            turned.rotate(net.minecraft.world.level.block.Rotation.CLOCKWISE_90).rotate(net.minecraft.world.level.block.Rotation.CLOCKWISE_180) == slope);
        BlockState doorState = door.defaultBlockState();
        check(failures, "rotating a door turns its facing",
            doorState.rotate(net.minecraft.world.level.block.Rotation.CLOCKWISE_90).getValue(com.carpentersblocks.block.DoorBlock.FACING)
                == doorState.getValue(com.carpentersblocks.block.DoorBlock.FACING).getClockWise());

        // A collapsible block lowered to nothing is not a full cube for things placed on it.
        com.carpentersblocks.block.CollapsibleBlock collapsible = com.carpentersblocks.registry.CBBlocks.COLLAPSIBLE.get();
        check(failures, "collapsible block has a dynamic shape", collapsible.hasDynamicShape());
    }

    /** Behaviour that needs a world: plate triggers, sensor, rain, hatch support. */
    private static void runBehaviour(ServerLevel level, BlockPos pos, List<String> failures) {
        BlockState stone = Blocks.STONE.defaultBlockState();
        java.util.Random random = new java.util.Random(1);

        // Pressure plate set to monsters: a zombie presses it, the tick releases it when it is gone.
        com.carpentersblocks.block.PressurePlateBlock plate = com.carpentersblocks.registry.CBBlocks.PRESSURE_PLATE.get();
        level.setBlockAndUpdate(pos.below(), stone);
        BlockState monsters = plate.defaultBlockState().setValue(com.carpentersblocks.block.PressurePlateBlock.TRIGGER,
            com.carpentersblocks.block.PressurePlateBlock.Trigger.MONSTER);
        level.setBlockAndUpdate(pos, monsters);
        net.minecraft.world.entity.monster.Zombie zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        zombie.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        level.addFreshEntity(zombie);
        plate.entityInside(level.getBlockState(pos), level, pos, zombie);
        check(failures, "a monster presses a monster plate", level.getBlockState(pos).getValue(com.carpentersblocks.block.PressurePlateBlock.POWERED));
        check(failures, "a pressed plate powers the block under it", level.getDirectSignal(pos, net.minecraft.core.Direction.UP) == 15);
        zombie.remove();
        plate.tick(level.getBlockState(pos), level, pos, random);
        check(failures, "the plate releases when the monster leaves", !level.getBlockState(pos).getValue(com.carpentersblocks.block.PressurePlateBlock.POWERED));
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());


        // Torch in the rain: lit -> smouldering -> out, and back once it stops.
        // (The rain level fades in over many ticks and its setter is client-only, so the rule
        // itself is checked; randomTick feeds it level.isRainingAt.)
        com.carpentersblocks.block.TorchBlock.Flame flame = com.carpentersblocks.block.TorchBlock.Flame.LIT;
        flame = com.carpentersblocks.block.TorchBlock.nextFlame(flame, true);
        flame = com.carpentersblocks.block.TorchBlock.nextFlame(flame, true);
        check(failures, "a torch goes out after two rainy ticks", flame == com.carpentersblocks.block.TorchBlock.Flame.UNLIT);
        flame = com.carpentersblocks.block.TorchBlock.nextFlame(flame, false);
        flame = com.carpentersblocks.block.TorchBlock.nextFlame(flame, false);
        check(failures, "a torch relights after two dry ticks", flame == com.carpentersblocks.block.TorchBlock.Flame.LIT);
        com.carpentersblocks.block.TorchBlock torch = com.carpentersblocks.registry.CBBlocks.TORCH.get();
        level.setBlockAndUpdate(pos.below(), stone);
        level.setBlockAndUpdate(pos, torch.defaultBlockState());
        check(failures, "a lit torch gives light 14", level.getBlockState(pos).getLightEmission() == 14);
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());

        // Hatch: when its support disappears it moves to another one instead of breaking.
        com.carpentersblocks.block.HatchBlock hatch = com.carpentersblocks.registry.CBBlocks.HATCH.get();
        level.setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.south(), stone);
        level.setBlockAndUpdate(pos.east(), stone);
        level.setBlockAndUpdate(pos, hatch.defaultBlockState().setValue(com.carpentersblocks.block.HatchBlock.FACING, net.minecraft.core.Direction.NORTH));
        level.setBlockAndUpdate(pos.south(), Blocks.AIR.defaultBlockState());
        BlockState moved = level.getBlockState(pos);
        check(failures, "a hatch moves to another support (now " + moved + ")",
            moved.getBlock() == hatch && moved.getValue(com.carpentersblocks.block.HatchBlock.FACING) == net.minecraft.core.Direction.WEST);
        // An open hidden hatch in the upper position is a ladder (Forge isLadder / Fabric mixin).
        level.setBlockAndUpdate(pos, moved.setValue(com.carpentersblocks.block.HatchBlock.STYLE, com.carpentersblocks.block.HatchBlock.Style.HIDDEN)
            .setValue(com.carpentersblocks.block.HatchBlock.HIGH, true).setValue(com.carpentersblocks.block.HatchBlock.OPEN, true));
        net.minecraft.world.entity.monster.Zombie climber = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        climber.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        check(failures, "an open hidden hatch can be climbed", climber.onClimbable());
        level.setBlockAndUpdate(pos, level.getBlockState(pos).setValue(com.carpentersblocks.block.HatchBlock.OPEN, false));
        check(failures, "a closed hatch cannot be climbed", !climber.onClimbable());
        level.setBlockAndUpdate(pos.east(), Blocks.AIR.defaultBlockState());
        check(failures, "a hatch with no support left breaks", level.getBlockState(pos).isAir());
        level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3), ItemEntity::isAlive).forEach(ItemEntity::remove);
    }

    private static void check(List<String> failures, String what, boolean ok) {
        if (!ok) {
            failures.add(what);
        }
    }
}
