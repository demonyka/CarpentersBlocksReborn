package com.carpentersblocks.forge.client;

import com.carpentersblocks.CarpentersBlocks;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

/**
 * OptiFine's connected textures for covers.
 * <p>
 * OptiFine chooses the connected sprite from the block being drawn and its neighbours (its
 * built-in connected glass matches the glass block itself), so a glass cover on a Carpenter's
 * block is invisible to it. Here each cover quad is handed to OptiFine as if the cover block
 * stood in the world ({@code FacadeLevel}), and OptiFine's answer is then projected onto our
 * geometry like any other cover quad.
 * <p>
 * OptiFine has no API: this calls its internals by reflection (written against 1.16.5
 * HD U G7) and switches itself off for good on the first failure, leaving plain textures.
 */
final class OptiFineConnectedTextures {
    private static final MethodHandle IS_ENABLED, NEW_ENV, RESET_ENV, GET_CONNECTED;
    private static volatile boolean available;
    /** OptiFine's per-thread scratch object; ours is separate from the chunk builder's own. */
    private static final ThreadLocal<Object> ENV = new ThreadLocal<>();

    static {
        MethodHandle enabled = null, newEnv = null, reset = null, connected = null;
        try {
            Class<?> config = Class.forName("net.optifine.Config");
            Class<?> env = Class.forName("net.optifine.render.RenderEnv");
            Class<?> textures = Class.forName("net.optifine.ConnectedTextures");
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            enabled = lookup.findStatic(config, "isConnectedTextures", MethodType.methodType(boolean.class));
            newEnv = lookup.findConstructor(env, MethodType.methodType(void.class, BlockState.class, BlockPos.class))
                .asType(MethodType.methodType(Object.class, BlockState.class, BlockPos.class));
            reset = lookup.findVirtual(env, "reset", MethodType.methodType(void.class, BlockState.class, BlockPos.class))
                .asType(MethodType.methodType(void.class, Object.class, BlockState.class, BlockPos.class));
            connected = lookup.findStatic(textures, "getConnectedTexture", MethodType.methodType(BakedQuad[].class,
                    BlockAndTintGetter.class, BlockState.class, BlockPos.class, BakedQuad.class, env))
                .asType(MethodType.methodType(BakedQuad[].class,
                    BlockAndTintGetter.class, BlockState.class, BlockPos.class, BakedQuad.class, Object.class));
            available = true;
            CarpentersBlocks.LOGGER.info("OptiFine found: covers use its connected textures");
        } catch (ClassNotFoundException e) {
            // No OptiFine.
        } catch (ReflectiveOperationException | RuntimeException e) {
            CarpentersBlocks.LOGGER.warn("This OptiFine version is not supported for connected covers: {}", e.toString());
        }
        IS_ENABLED = enabled;
        NEW_ENV = newEnv;
        RESET_ENV = reset;
        GET_CONNECTED = connected;
    }

    private OptiFineConnectedTextures() {
    }

    /**
     * @param facade level in which covered Carpenter's blocks appear as their cover
     * @param cover  the cover block whose quads these are
     * @return the quads with OptiFine's connected sprites, or {@code quads} unchanged
     */
    static List<BakedQuad> apply(BlockAndTintGetter facade, BlockState cover, BlockPos pos, List<BakedQuad> quads) {
        if (!available || quads.isEmpty()) {
            return quads;
        }
        try {
            if (!(boolean) IS_ENABLED.invokeExact()) {
                return quads;
            }
            Object env = ENV.get();
            if (env == null) {
                env = (Object) NEW_ENV.invokeExact(cover, pos);
                ENV.set(env);
            } else {
                RESET_ENV.invokeExact(env, cover, pos);
            }
            List<BakedQuad> out = new ArrayList<>(quads.size());
            for (BakedQuad quad : quads) {
                // The returned array is reused by OptiFine: copy its contents out right away.
                BakedQuad[] connected = (BakedQuad[]) GET_CONNECTED.invokeExact(facade, cover, pos, quad, env);
                for (BakedQuad result : connected) {
                    if (result != null) {
                        out.add(result);
                    }
                }
            }
            return out.isEmpty() ? quads : out;
        } catch (Throwable t) {
            available = false;
            CarpentersBlocks.LOGGER.warn("OptiFine connected textures failed for covers; using plain textures from now on", t);
            return quads;
        }
    }
}
