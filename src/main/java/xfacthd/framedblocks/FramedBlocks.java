package xfacthd.framedblocks;

/**
 * Bundled marker class. Xaero's Minimap and World Map only construct their
 * camouflage-block ("Framed Blocks") support when Class.forName finds this
 * exact name - FramedBlocks itself does not exist on 1.12, so shipping the
 * name unlocks the support path our hidden lights piggyback on (see
 * XaeroMapCompat and the bundled FramedTileEntity skeleton).
 */
public final class FramedBlocks {

    private FramedBlocks() {
    }
}
