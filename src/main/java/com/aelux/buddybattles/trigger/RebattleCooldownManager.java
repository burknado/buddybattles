package com.aelux.buddybattles.trigger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Tracks when each player last started a duel against each mob, to block immediate rematches. */
public class RebattleCooldownManager {
    public static final RebattleCooldownManager INSTANCE = new RebattleCooldownManager();

    private record Key(UUID playerId, UUID mobId) {}

    private final Map<Key, Long> lastBattleStartMillis = new HashMap<>();

    private RebattleCooldownManager() {}

    /**
     * Call when a battle actually begins - the cooldown clock starts here. Also drops entries whose
     * cooldown has expired, so the map only holds duels started within the last cooldown window.
     */
    public void recordBattleStart(UUID playerId, UUID mobId, long cooldownSeconds) {
        long now = System.currentTimeMillis();
        long cooldownMillis = cooldownSeconds * 1000L;
        lastBattleStartMillis.values().removeIf(startedAt -> now - startedAt >= cooldownMillis);
        lastBattleStartMillis.put(new Key(playerId, mobId), now);
    }

    /** Seconds left before this player can challenge this mob again, rounded up; 0 if none. */
    public long secondsRemaining(UUID playerId, UUID mobId, long cooldownSeconds) {
        Long startedAt = lastBattleStartMillis.get(new Key(playerId, mobId));
        if (startedAt == null) return 0;
        long remainingMillis = cooldownSeconds * 1000L - (System.currentTimeMillis() - startedAt);
        return remainingMillis <= 0 ? 0 : (remainingMillis + 999) / 1000;
    }
}
