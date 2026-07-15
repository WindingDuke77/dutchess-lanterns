package com.dutchess77.lantern.network;

import com.dutchess77.lantern.block.DarknessWardTileEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Ward GUI button press: bump one size/offset field on the ward at pos. */
public class WardAdjustMessage implements IMessage {

    private long pos;
    private int field;
    private int delta;

    public WardAdjustMessage() {
    }

    public WardAdjustMessage(BlockPos pos, int field, int delta) {
        this.pos = pos.toLong();
        this.field = field;
        this.delta = delta;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = buf.readLong();
        field = buf.readByte();
        delta = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos);
        buf.writeByte(field);
        buf.writeByte(delta);
    }

    public static class Handler implements IMessageHandler<WardAdjustMessage, IMessage> {
        @Override
        public IMessage onMessage(WardAdjustMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                BlockPos pos = BlockPos.fromLong(message.pos);
                if (player.getDistanceSq(pos) > 64.0D) {
                    return;
                }
                TileEntity te = player.world.getTileEntity(pos);
                if (te instanceof DarknessWardTileEntity) {
                    ((DarknessWardTileEntity) te).adjust(message.field, message.delta);
                }
            });
            return null;
        }
    }
}
