package com.dutchess77.lantern.update;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Tells the player once, in chat, that a staged update installs on exit. */
@SideOnly(Side.CLIENT)
public class UpdateChatNotifier {

    private boolean shown;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (shown || event.phase != TickEvent.Phase.END) {
            return;
        }
        String version = SelfUpdater.pendingVersion;
        if (version == null || Minecraft.getMinecraft().player == null) {
            return;
        }
        shown = true;
        Minecraft.getMinecraft().player.sendMessage(new TextComponentTranslation(
            "chat.lantern.update", version).setStyle(
                new net.minecraft.util.text.Style().setColor(TextFormatting.LIGHT_PURPLE)));
    }
}
