package com.carpentersblocks.forge.mixin;

import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Applies the integrations only when their mod is installed: the CTM facade interface, and
 * the two OptiFine mixins (added here rather than listed in the JSON, so that Mixin does not
 * warn about missing OptiFine classes on every start without OptiFine).
 */
public final class ForgeMixinPlugin implements IMixinConfigPlugin {
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith(".CtmFacadeMixin")) {
            return LoadingModList.get() != null && LoadingModList.get().getModFileById("ctm") != null;
        }
        return true;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        boolean optiFine = loader != null && loader.getResource("net/optifine/Config.class") != null
            || ForgeMixinPlugin.class.getClassLoader().getResource("net/optifine/Config.class") != null;
        return optiFine ? java.util.Arrays.asList("OptiFineBlockUtilsMixin", "OptiFineConnectedTexturesMixin") : null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
