package com.dutchess77.lantern.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import com.dutchess77.lantern.Lantern;
import com.dutchess77.lantern.ModBlocks;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.registry.IRegistry;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

/**
 * Teaches Xaero's Minimap and World Map to color hidden lights as their
 * disguise. Both mods support camouflage tile entities that extend the
 * (bundled) FramedTileEntity class and expose getCamoState(), but they only
 * consult blocks in their internal "framed blocks" set - this shim adds our
 * block to that set reflectively. Fail-soft everywhere; no-op when the map
 * mods are absent (e.g. dedicated servers).
 */
public final class XaeroMapCompat {

    private XaeroMapCompat() {
    }

    public static void init() {
        hook("xaerominimap");
        hook("xaeroworldmap");
    }

    private static void hook(String modid) {
        try {
            ModContainer container = Loader.instance().getIndexedModList().get(modid);
            if (container == null || container.getMod() == null) {
                return;
            }
            Object supportMods = findSupportMods(container.getMod());
            if (supportMods == null) {
                Lantern.LOGGER.info("Xaero map camo hook: no SupportMods on {}", modid);
                return;
            }
            Object supportFramed = findFieldOfType(supportMods, "SupportFramedBlocks");
            if (supportFramed == null) {
                Lantern.LOGGER.info("Xaero map camo hook: no SupportFramedBlocks on {}", modid);
                return;
            }
            // force the lazy framedBlocks set to build, then add our block to it
            invokeIsFrameBlock(supportFramed);
            Field setField = supportFramed.getClass().getDeclaredField("framedBlocks");
            setField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Set<Object> framedBlocks = (Set<Object>) setField.get(supportFramed);
            if (framedBlocks != null) {
                framedBlocks.add(ModBlocks.HIDDEN_LIGHT);
                Lantern.LOGGER.info("Hooked {}: hidden lights map as their disguise", modid);
            }
        } catch (Throwable t) {
            Lantern.LOGGER.info("Xaero map camo hook failed for {} (maps will show a fallback color): {}",
                modid, t.toString());
        }
    }

    private static Object findSupportMods(Object mod) throws Exception {
        try {
            Method getter = mod.getClass().getMethod("getSupportMods");
            Object result = getter.invoke(mod);
            if (result != null) {
                return result;
            }
        } catch (NoSuchMethodException ignored) {
            // fall through to field scan
        }
        return findFieldOfType(mod, "SupportMods");
    }

    /** First declared field (incl. superclasses) whose type's simple name matches. */
    private static Object findFieldOfType(Object owner, String simpleTypeName) throws Exception {
        Class<?> clazz = owner.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getType().getSimpleName().equals(simpleTypeName)) {
                    field.setAccessible(true);
                    return field.get(owner);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    /** Calls isFrameBlock with null world/registry and a dummy state to force set init. */
    private static void invokeIsFrameBlock(Object supportFramed) throws Exception {
        for (Method method : supportFramed.getClass().getMethods()) {
            if (!method.getName().equals("isFrameBlock")) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            Object[] args = new Object[params.length];
            for (int i = 0; i < params.length; i++) {
                if (params[i] == IBlockState.class) {
                    args[i] = Blocks.STONE.getDefaultState();
                } else if (params[i] == World.class || params[i] == IRegistry.class) {
                    args[i] = null;
                } else {
                    args[i] = null;
                }
            }
            method.invoke(supportFramed, args);
            return;
        }
    }
}
