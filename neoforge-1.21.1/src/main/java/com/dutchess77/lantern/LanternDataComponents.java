package com.dutchess77.lantern;

import java.util.List;

import com.dutchess77.lantern.item.UpgradeType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * 1.21 replacement for the 1.12.2 item NBT: Active/Charge/Energy flags plus the
 * socketed upgrade list live in data components now.
 */
public final class LanternDataComponents {

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Lantern.MODID);

    /** One socketed upgrade: {type, tier}. Same-type upgrades stack additively. */
    public record InstalledUpgrade(UpgradeType type, int tier) {
        public static final Codec<InstalledUpgrade> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.xmap(UpgradeType::byKey, t -> t.key).fieldOf("type").forGetter(InstalledUpgrade::type),
            Codec.intRange(1, 4).fieldOf("tier").forGetter(InstalledUpgrade::tier)
        ).apply(inst, InstalledUpgrade::new));
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> ACTIVE =
        COMPONENTS.register("active", () -> DataComponentType.<Boolean>builder()
            .persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CHARGE =
        COMPONENTS.register("charge", () -> DataComponentType.<Integer>builder()
            .persistent(Codec.intRange(0, Integer.MAX_VALUE)).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY =
        COMPONENTS.register("energy", () -> DataComponentType.<Integer>builder()
            .persistent(Codec.intRange(0, Integer.MAX_VALUE)).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<InstalledUpgrade>>> UPGRADES =
        COMPONENTS.register("upgrades", () -> DataComponentType.<List<InstalledUpgrade>>builder()
            .persistent(InstalledUpgrade.CODEC.listOf())
            .networkSynchronized(ByteBufCodecs.fromCodec(InstalledUpgrade.CODEC.listOf())).build());

    private LanternDataComponents() {
    }
}
