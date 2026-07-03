package com.dutchess77.lantern.network;

import com.dutchess77.lantern.item.LanternItem;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client -> server: sneak+scroll on a held Lantern shifts its grid spacing. */
public class PacketAdjustSpacing implements IMessage {

    private int direction;

    public PacketAdjustSpacing() {
    }

    public PacketAdjustSpacing(int direction) {
        this.direction = direction;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        direction = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(direction);
    }

    public static class Handler implements IMessageHandler<PacketAdjustSpacing, IMessage> {
        @Override
        public IMessage onMessage(PacketAdjustSpacing message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            int dir = Integer.signum(message.direction);
            if (dir == 0) {
                return null;
            }
            player.getServerWorld().addScheduledTask(() -> {
                ItemStack held = player.getHeldItemMainhand();
                if (held.isEmpty() || !(held.getItem() instanceof LanternItem)) {
                    held = player.getHeldItemOffhand();
                }
                if (!held.isEmpty() && held.getItem() instanceof LanternItem) {
                    int spacing = LanternItem.adjustSpacing(held, dir);
                    player.sendStatusMessage(new TextComponentTranslation("chat.lantern.spacing", spacing, spacing), true);
                }
            });
            return null;
        }
    }
}
