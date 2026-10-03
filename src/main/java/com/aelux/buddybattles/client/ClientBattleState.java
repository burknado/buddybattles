package com.aelux.buddybattles.client;

import com.aelux.buddybattles.network.BattleSyncPayload;

/** Purely a rendering cache - holds whatever BattleSyncPayload the server last sent, for BattleScreen to read. */
public class ClientBattleState {
    private static BattleSyncPayload latest;

    public static void update(BattleSyncPayload payload) {
        latest = payload;
    }

    public static BattleSyncPayload get() {
        return latest;
    }

    public static void clear() {
        latest = null;
    }
}
