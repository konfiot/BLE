package com.example.ble;

import java.util.HashMap;

enum ConsolType {
    BLE_SERVER(1), CLASSIC_SERVER(2), BRIDGE_INVALID(4), BRIDGE_BLE_ONLY(5), BRIDEG_CLC_ONLY(6), BRIDGE(7);
    // There is't a 3, cause of the bit wise operation and a server cannot be both BLE and Classic

    private int flag;

    private static HashMap<Integer, ConsolType> hMap = new HashMap<>();

    ConsolType(int flag) {
        this.flag = flag;
    }

    static {
        for(ConsolType type : ConsolType.values()) {
            hMap.put(type.flag, type);
        }
    }

    int getFlagValue() {
        return flag;
    }

    static ConsolType getTypeFromFlag(int flagValue) {
        return hMap.get(flagValue);
    }
}
