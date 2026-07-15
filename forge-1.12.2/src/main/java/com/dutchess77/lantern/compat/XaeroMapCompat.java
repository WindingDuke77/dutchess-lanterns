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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Teaches Xaero's Minimap and World Map to color hidden lights as their
 * disguise. Both mods ship camouflage support built for the FramedBlocks mod:
 * the bundled xfacthd.framedblocks.FramedBlocks marker unlocks it and the
 * bundled FramedTileEntity skeleton satisfies its getCamoState() reflection.
 * The remaining gap is their private "framedBlocks" set, which only admits
 * registry names matching framedblocks:framed_* and is thrown away on every
 * world change - so this shim re-adds our block to it once a second on the
 * client tick. Fail-soft everywhere; no-op on dedicated servers and when the
 * map mods are absent.
 */
public final class XaeroMapCompat {

    private XaeroMapCompat() {
    }

    public static void init() {
        if (!FMLCommonHandler.instance().getSide().isClient()) {
            return;
        }
        if (Loader.isModLoaded("xaerominimap") || Loader.isModLoaded("xaeroworldmap")) {
            MinecraftForge.EVENT_BUS.register(new Ticker());
        }
    }

    public static final class Ticker {
        private static final int INTERVAL_TICKS = 20;

        private final Hook worldMap = new Hook("xaeroworldmap");
        private final Hook minimap = new Hook("xaerominimap");
        private int cooldown;

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END || --cooldown > 0) {
                return;
            }
            cooldown = INTERVAL_TICKS;
            worldMap.ensureHooked();
            minimap.ensureHooked();
        }
    }

    /** Keeps one map mod's framedBlocks set containing our block. */
    private static final class Hook {
        private final String modid;
        private Object support;         // SupportFramedBlocks instance, resolved lazily
        private Field setField;         // its private framedBlocks set
        private boolean announced;
        private boolean dead;           // resolution failed for good - stop trying

        Hook(String modid) {
            this.modid = modid;
        }

        void ensureHooked() {
            if (dead) {
                return;
            }
            try {
                if (support == null) {
                    support = resolveSupport();
                    if (support == null) {
                        return; // Xaero not done loading yet - retry next interval
                    }
                    setField = support.getClass().getDeclaredField("framedBlocks");
                    setField.setAccessible(true);
                }
                Set<Object> framedBlocks = readSet();
                if (framedBlocks == null) {
                    // lazily (re)built on first isFrameBlock call after a world change
                    invokeIsFrameBlock(support);
                    framedBlocks = readSet();
                }
                if (framedBlocks != null && framedBlocks.add(ModBlocks.HIDDEN_LIGHT) && !announced) {
                    announced = true;
                    Lantern.LOGGER.info("Hooked {}: hidden lights map as their disguise", modid);
                }
            } catch (Throwable t) {
                dead = true;
                Lantern.LOGGER.info("Xaero map camo hook failed for {} (maps will show a fallback color): {}",
                    modid, t.toString());
            }
        }

        @SuppressWarnings("unchecked")
        private Set<Object> readSet() throws Exception {
            return (Set<Object>) setField.get(support);
        }

        private Object resolveSupport() throws Exception {
            if ("xaeroworldmap".equals(modid)) {
                // world map keeps everything static on xaero.map.mods.SupportMods
                Class<?> supportMods = Class.forName("xaero.map.mods.SupportMods");
                Field field = supportMods.getDeclaredField("supportFramedBlocks");
                field.setAccessible(true);
                return field.get(null);
            }
            // minimap: mod instance -> SupportMods instance -> its public field
            ModContainer container = Loader.instance().getIndexedModList().get(modid);
            if (container == null || container.getMod() == null) {
                dead = true;
                return null;
            }
            Object supportMods = findSupportMods(container.getMod());
            if (supportMods == null) {
                return null;
            }
            return findFieldOfType(supportMods, "SupportFramedBlocks");
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
                }
            }
            method.invoke(supportFramed, args);
            return;
        }
    }
}
