package com.dutchess77.lantern.client;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.dutchess77.lantern.Lantern;
import com.dutchess77.lantern.block.DarknessWardTileEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Draws a translucent purple box over each ward whose "show area" toggle is
 * on (EnderIO range-display style). Pure client state - nothing is synced.
 */
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = Lantern.MODID)
@SideOnly(Side.CLIENT)
public final class WardAreaRenderer {

    private static final Set<Long> SHOWN = new HashSet<>();

    private WardAreaRenderer() {
    }

    private static long key(World world, BlockPos pos) {
        return pos.toLong() ^ ((long) world.provider.getDimension() << 58);
    }

    public static boolean isShown(World world, BlockPos pos) {
        return SHOWN.contains(key(world, pos));
    }

    public static void toggle(World world, BlockPos pos) {
        long key = key(world, pos);
        if (!SHOWN.remove(key)) {
            SHOWN.add(key);
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            SHOWN.clear();
        }
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        if (SHOWN.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.world;
        Entity view = mc.getRenderViewEntity();
        if (world == null || view == null) {
            return;
        }
        float partial = event.getPartialTicks();
        double camX = view.lastTickPosX + (view.posX - view.lastTickPosX) * partial;
        double camY = view.lastTickPosY + (view.posY - view.lastTickPosY) * partial;
        double camZ = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * partial;

        GlStateManager.pushMatrix();
        GlStateManager.translate(-camX, -camY, -camZ);
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);

        // entries are always from the current world: SHOWN clears on world unload
        long dimBits = (long) world.provider.getDimension() << 58;
        Iterator<Long> it = SHOWN.iterator();
        while (it.hasNext()) {
            BlockPos pos = BlockPos.fromLong(it.next() ^ dimBits);
            TileEntity te = world.getTileEntity(pos);
            if (!(te instanceof DarknessWardTileEntity)) {
                it.remove(); // ward gone (broken or unloaded) - stop drawing it
                continue;
            }
            DarknessWardTileEntity ward = (DarknessWardTileEntity) te;
            double minX = pos.getX() + ward.getOffset(0) - ward.getRadius(0);
            double minY = pos.getY() + ward.getOffset(1) - ward.getRadius(1);
            double minZ = pos.getZ() + ward.getOffset(2) - ward.getRadius(2);
            double maxX = pos.getX() + ward.getOffset(0) + ward.getRadius(0) + 1;
            double maxY = pos.getY() + ward.getOffset(1) + ward.getRadius(1) + 1;
            double maxZ = pos.getZ() + ward.getOffset(2) + ward.getRadius(2) + 1;
            drawBox(minX, minY, minZ, maxX, maxY, maxZ);
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private static void drawBox(double x1, double y1, double z1, double x2, double y2, double z2) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        float r = 0.56F;
        float g = 0.25F;
        float b = 0.85F;

        // faces
        buf.begin(7, DefaultVertexFormats.POSITION_COLOR); // GL_QUADS
        float fa = 0.15F;
        // bottom + top
        quad(buf, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, fa);
        quad(buf, x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2, r, g, b, fa);
        // north + south
        quad(buf, x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1, r, g, b, fa);
        quad(buf, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, r, g, b, fa);
        // west + east
        quad(buf, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, r, g, b, fa);
        quad(buf, x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1, r, g, b, fa);
        tess.draw();

        // edges
        GlStateManager.glLineWidth(2.0F);
        buf.begin(1, DefaultVertexFormats.POSITION_COLOR); // GL_LINES
        float ea = 0.9F;
        // bottom ring
        line(buf, x1, y1, z1, x2, y1, z1, r, g, b, ea);
        line(buf, x2, y1, z1, x2, y1, z2, r, g, b, ea);
        line(buf, x2, y1, z2, x1, y1, z2, r, g, b, ea);
        line(buf, x1, y1, z2, x1, y1, z1, r, g, b, ea);
        // top ring
        line(buf, x1, y2, z1, x2, y2, z1, r, g, b, ea);
        line(buf, x2, y2, z1, x2, y2, z2, r, g, b, ea);
        line(buf, x2, y2, z2, x1, y2, z2, r, g, b, ea);
        line(buf, x1, y2, z2, x1, y2, z1, r, g, b, ea);
        // verticals
        line(buf, x1, y1, z1, x1, y2, z1, r, g, b, ea);
        line(buf, x2, y1, z1, x2, y2, z1, r, g, b, ea);
        line(buf, x2, y1, z2, x2, y2, z2, r, g, b, ea);
        line(buf, x1, y1, z2, x1, y2, z2, r, g, b, ea);
        tess.draw();
    }

    private static void quad(BufferBuilder buf,
                             double ax, double ay, double az, double bx, double by, double bz,
                             double cx, double cy, double cz, double dx, double dy, double dz,
                             float r, float g, float b, float a) {
        buf.pos(ax, ay, az).color(r, g, b, a).endVertex();
        buf.pos(bx, by, bz).color(r, g, b, a).endVertex();
        buf.pos(cx, cy, cz).color(r, g, b, a).endVertex();
        buf.pos(dx, dy, dz).color(r, g, b, a).endVertex();
    }

    private static void line(BufferBuilder buf,
                             double ax, double ay, double az, double bx, double by, double bz,
                             float r, float g, float b, float a) {
        buf.pos(ax, ay, az).color(r, g, b, a).endVertex();
        buf.pos(bx, by, bz).color(r, g, b, a).endVertex();
    }
}
