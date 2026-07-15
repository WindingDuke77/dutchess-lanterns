package com.dutchess77.lantern.network;

import com.dutchess77.lantern.Lantern;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Ward GUI button press: bump one size/offset field on the ward at pos. */
public record WardAdjustMessage(BlockPos pos, int field, int delta) implements CustomPacketPayload {

    public static final Type<WardAdjustMessage> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Lantern.MODID, "ward_adjust"));

    public static final StreamCodec<ByteBuf, WardAdjustMessage> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, WardAdjustMessage::pos,
        ByteBufCodecs.VAR_INT, WardAdjustMessage::field,
        ByteBufCodecs.VAR_INT, WardAdjustMessage::delta,
        WardAdjustMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
