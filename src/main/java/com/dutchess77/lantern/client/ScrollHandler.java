package com.dutchess77.lantern.client;

import com.dutchess77.lantern.Lantern;
import com.dutchess77.lantern.item.LanternItem;
import com.dutchess77.lantern.network.PacketAdjustSpacing;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = Lantern.MODID)
public class ScrollHandler {

    @SubscribeEvent
    public static void onMouseScroll(MouseEvent event) {
        if (event.getDwheel() == 0) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null || mc.currentScreen != null || !player.isSneaking()) {
            return;
        }
        if (!(player.getHeldItemMainhand().getItem() instanceof LanternItem)
            && !(player.getHeldItemOffhand().getItem() instanceof LanternItem)) {
            return;
        }
        // keep the hotbar from scrolling while adjusting the lantern
        event.setCanceled(true);
        Lantern.NETWORK.sendToServer(new PacketAdjustSpacing(event.getDwheel() > 0 ? 1 : -1));
    }
}
