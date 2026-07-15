package baubles.api;

/**
 * Bundled Baubles API stub (Baubles for 1.12.2 by Azanor). Shipping the API
 * lets the Lantern implement IBauble without a hard Baubles dependency; at
 * runtime the real Baubles classes win the classloading race when present.
 */
public enum BaubleType {

    AMULET(0),
    RING(1, 2),
    BELT(3),
    TRINKET(0, 1, 2, 3, 4, 5, 6),
    HEAD(4),
    BODY(5),
    CHARM(6);

    private final int[] validSlots;

    BaubleType(int... validSlots) {
        this.validSlots = validSlots;
    }

    public int[] getValidSlots() {
        return validSlots;
    }

    public boolean hasSlot(int slot) {
        for (int s : validSlots) {
            if (s == slot) {
                return true;
            }
        }
        return false;
    }
}
