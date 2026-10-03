package com.aelux.buddybattles.session;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Tracks active battles by participant UUID.
 * TODO: in-memory only - a restart mid-duel drops the session without calling onBattleEnded.
 */
public class BattleSessionManager {
    public static final BattleSessionManager INSTANCE = new BattleSessionManager();

    private final Map<UUID, BattleSession> byParticipant = new HashMap<>();

    private BattleSessionManager() {}

    public BattleSession start(BattleParticipant p1, BattleParticipant p2) {
        BattleSession session = new BattleSession(p1, p2);
        byParticipant.put(p1.getParticipantId(), session);
        byParticipant.put(p2.getParticipantId(), session);
        return session;
    }

    public Optional<BattleSession> get(UUID participantId) {
        return Optional.ofNullable(byParticipant.get(participantId));
    }

    public boolean isInBattle(UUID participantId) {
        return byParticipant.containsKey(participantId);
    }

    void remove(BattleSession session) {
        byParticipant.remove(session.p1.getParticipantId());
        byParticipant.remove(session.p2.getParticipantId());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        new HashMap<>(byParticipant).values().stream().distinct().forEach(BattleSession::interrupt);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        // Distinct: a session is stored twice (once per participant UUID), don't tick it twice.
        for (BattleSession session : new HashMap<>(byParticipant).values().stream().distinct().toList()) {
            session.p1.tick();
            session.p2.tick();
        }
    }
}
