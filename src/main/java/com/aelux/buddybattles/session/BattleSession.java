package com.aelux.buddybattles.session;

import com.aelux.buddybattles.engine.BattleBoard;
import com.aelux.buddybattles.engine.BattleGame;
import com.aelux.buddybattles.engine.log.BattleComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** One active battle: two participants plus the BattleGame/BattleBoard pair actually driving the card logic. */
public class BattleSession {
    public final UUID sessionId;
    public final BattleParticipant p1;
    public final BattleParticipant p2;
    public final BattleBoard board;
    public final BattleGame game;

    private boolean ended = false;

    // Spectators are NOT participants.
    private final Set<ServerPlayer> spectators = new HashSet<>();

    public BattleSession(BattleParticipant p1, BattleParticipant p2) {
        this.sessionId = UUID.randomUUID();
        this.p1 = p1;
        this.p2 = p2;
        this.board = new BattleBoard(p1, p2, this::notifySpectators, this::end);
        this.game = new BattleGame(board);
        board.setStateSupplier(new BattleBoard.StateSupplier() {
            @Override
            public int power(int slot) {
                return game.state[slot].power;
            }

            @Override
            public int statusOrdinal(int slot) {
                return game.state[slot].status.ordinal();
            }
        });

        p1.attachSession(this);
        p2.attachSession(this);
    }

    /** Shuffles decks, draws opening hands, and starts turn 1. Call once, right after construction. */
    public void beginGame() {
        game.beginGame();
    }

    public boolean addSpectator(ServerPlayer player) {
        if (ended) return false;
        return spectators.add(player);
    }

    public void removeSpectator(ServerPlayer player) {
        spectators.remove(player);
    }

    public Set<ServerPlayer> getSpectators() {
        return spectators;
    }

    /** Called by BattleBoard.syncData() whenever engine state changes, in addition to participant.markDirty(). */
    public void notifySpectators() {
        // TODO: once there's an actual read-only spectate screen/packet, push updated state to each entry in
        // `spectators` here.
    }

    public boolean involves(UUID participantId) {
        return p1.getParticipantId().equals(participantId) || p2.getParticipantId().equals(participantId);
    }

    public BattleParticipant other(UUID participantId) {
        return p1.getParticipantId().equals(participantId) ? p2 : p1;
    }

    public boolean isEnded() {
        return ended;
    }

    /** Ends the battle normally, crediting a win/loss. Also called directly by BattleBoard.endGame(). */
    public void end(boolean p1Won) {
        if (ended) return;
        ended = true;
        p1.onBattleEnded(p1Won, false);
        p2.onBattleEnded(!p1Won, false);
        // TODO: tell spectators the match is over (result screen / just close their view) before clearing,
        // rather than silently dropping them.
        spectators.clear();
        BattleSessionManager.INSTANCE.remove(this);
    }

    /** Aborts the battle without a winner - e.g. because a participant took damage. */
    public void interrupt() {
        if (ended) return;
        ended = true;
        board.addLog(new BattleComponent(Component.translatable("gui.buddybattles.interrupted")));
        p1.onBattleEnded(false, true);
        p2.onBattleEnded(false, true);
        spectators.clear();
        BattleSessionManager.INSTANCE.remove(this);
    }
}
