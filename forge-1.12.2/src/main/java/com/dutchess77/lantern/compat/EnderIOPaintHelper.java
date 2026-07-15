package com.dutchess77.lantern.compat;

import java.lang.reflect.Method;

import com.dutchess77.lantern.Lantern;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * Runtime-only bridge to EnderIO's painted glowstone. The block is resolved
 * from the registry and {@code IPaintableTileEntity.setPaintSource(IBlockState)}
 * is invoked reflectively, so no compile-time EnderIO dependency is needed.
 */
public final class EnderIOPaintHelper {

    private static final ResourceLocation PAINTED_GLOWSTONE_SOLID =
        new ResourceLocation("enderio", "block_painted_glowstone_solid");

    private static Block paintedGlowstone;
    private static Method setPaintSource;
    private static Method getPaintSource;
    private static boolean lookupFailed;
    private static boolean paintBroken;

    private EnderIOPaintHelper() {
    }

    /** Must run after registries are frozen (FML init or later). */
    public static void init() {
        Block block = ForgeRegistries.BLOCKS.getValue(PAINTED_GLOWSTONE_SOLID);
        if (block == null || block == Blocks.AIR) {
            Lantern.LOGGER.info("EnderIO not present; legacy light migration/reclaim disabled");
        } else {
            paintedGlowstone = block;
            Lantern.LOGGER.info("Legacy EnderIO lights ({}) will be recognized for migration and reclaim",
                PAINTED_GLOWSTONE_SOLID);
        }
    }

    public static boolean isAvailable() {
        return paintedGlowstone != null;
    }

    public static Block paintedGlowstoneSolid() {
        return paintedGlowstone;
    }

    public static boolean isPaintedGlowstone(Block block) {
        return paintedGlowstone != null && block == paintedGlowstone;
    }

    /** Paints the freshly placed glowstone to look like {@code source}. Fail-soft. */
    public static void paint(TileEntity te, IBlockState source) {
        if (te == null || paintBroken) {
            return;
        }
        if (setPaintSource == null && !lookupFailed) {
            // name-based lookup: setPaintSource is a mod-defined method and is
            // never obfuscated at runtime, unlike its IBlockState parameter type
            for (Method method : te.getClass().getMethods()) {
                if ("setPaintSource".equals(method.getName()) && method.getParameterCount() == 1) {
                    method.setAccessible(true);
                    setPaintSource = method;
                    break;
                }
            }
            if (setPaintSource == null) {
                lookupFailed = true;
                Lantern.LOGGER.warn("No setPaintSource method on {}; lights will stay unpainted",
                    te.getClass().getName());
            }
        }
        if (setPaintSource != null) {
            try {
                setPaintSource.invoke(te, source);
            } catch (Throwable t) {
                paintBroken = true;
                Lantern.LOGGER.warn("Failed to paint glowstone; lights will stay unpainted", t);
            }
        }
    }

    /** Reads the paint back off a placed light; null when unpainted or unreadable. */
    public static IBlockState getPaint(TileEntity te) {
        if (te == null) {
            return null;
        }
        try {
            if (getPaintSource == null) {
                for (Method method : te.getClass().getMethods()) {
                    if ("getPaintSource".equals(method.getName()) && method.getParameterCount() == 0) {
                        method.setAccessible(true);
                        getPaintSource = method;
                        break;
                    }
                }
            }
            return getPaintSource != null ? (IBlockState) getPaintSource.invoke(te) : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
