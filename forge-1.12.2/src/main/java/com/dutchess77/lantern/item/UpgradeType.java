package com.dutchess77.lantern.item;

import javax.annotation.Nullable;

public enum UpgradeType {

    RANGE("range_upgrade"),
    EFFICIENCY("efficiency_upgrade"),
    CAPACITY("capacity_upgrade");

    public final String key;

    UpgradeType(String key) {
        this.key = key;
    }

    @Nullable
    public static UpgradeType byKey(String key) {
        for (UpgradeType type : values()) {
            if (type.key.equals(key)) {
                return type;
            }
        }
        return null;
    }
}
