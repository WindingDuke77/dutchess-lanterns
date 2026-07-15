package com.dutchess77.lantern.compat;

import java.lang.reflect.Constructor;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * CTM's baked models only compute connected textures when the state handed to
 * them is CTM's own CTMExtendedState (an instanceof check inside getQuads) -
 * anything else renders the raw texture sheet with no connections. CTM
 * normally injects that wrapper via an ASM hook in Block#getExtendedState, but
 * the hook lives in the BASE method, so any block that overrides
 * getExtendedState (Chisel's carvables do) returns unwrapped states when
 * called from our code. Wrap the resolved mimic ourselves - reflection only,
 * no compile-time dependency, and a no-op when CTM is absent.
 */
public final class CTMCompat {

    private static final Class<?> STATE_CLASS;
    private static final Constructor<?> STATE_CTOR;

    static {
        Class<?> clazz = null;
        Constructor<?> ctor = null;
        try {
            clazz = Class.forName("team.chisel.ctm.client.state.CTMExtendedState");
            ctor = clazz.getConstructor(IBlockState.class, IBlockAccess.class, BlockPos.class);
        } catch (Throwable t) {
            clazz = null;
            ctor = null;
        }
        STATE_CLASS = clazz;
        STATE_CTOR = ctor;
    }

    private CTMCompat() {
    }

    /** Wraps a mimic state so CTM models can compute connections at this position. */
    public static IBlockState wrapForCTM(IBlockState mimic, IBlockAccess world, BlockPos pos) {
        if (STATE_CTOR == null || STATE_CLASS.isInstance(mimic)) {
            return mimic;
        }
        try {
            return (IBlockState) STATE_CTOR.newInstance(mimic, world, pos);
        } catch (Throwable t) {
            return mimic;
        }
    }
}
