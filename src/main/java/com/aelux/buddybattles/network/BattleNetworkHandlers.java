package com.aelux.buddybattles.network;

import com.aelux.buddybattles.client.ClientBattleState;
import com.aelux.buddybattles.engine.BattleGame;
import com.aelux.buddybattles.session.BattleSessionManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class BattleNetworkHandlers {
    public static void handlePlayCard(PlayCardPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            BattleSessionManager.INSTANCE.get(player.getUUID()).ifPresent(session -> {
                boolean mySide = session.p1.getParticipantId().equals(player.getUUID());
                if (session.game.isP1() != mySide) return; // not your turn
                if (payload.handIndex() < 0 || payload.handIndex() > 2) return;
                if (payload.boardSlot() < 0 || payload.boardSlot() > 5) return;
                if (BattleGame.getOwner(payload.boardSlot()) != mySide) return; // can only play into your own row
                ItemStack card = session.board.getHandCard(mySide, payload.handIndex());
                if (card.isEmpty()) return;
                if (!session.game.canPlay(mySide, card)) return;
                session.board.removeHandCard(mySide, payload.handIndex());
                if (session.game.playCard(payload.boardSlot(), card, mySide)) {
                    if (!session.game.hasGameEnded()) session.board.syncData();
                } else {
                    // playCard can still fail after this point (e.g. energy checked inside playCard itself).
                    session.board.tryPutInHand(mySide, card);
                }
            });
        });
    }

    public static void handleEndTurn(EndTurnPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            BattleSessionManager.INSTANCE.get(player.getUUID()).ifPresent(session -> {
                boolean mySide = session.p1.getParticipantId().equals(player.getUUID());
                if (session.game.isP1() != mySide) return; // not your turn
                // endTurn() returning false means the game just ended (health hit 0).
                if (session.game.endTurn()) {
                    session.game.nextTurn();
                }
            });
        });
    }

    public static void handleActivateCard(ActivateCardPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            BattleSessionManager.INSTANCE.get(player.getUUID()).ifPresent(session -> {
                boolean mySide = session.p1.getParticipantId().equals(player.getUUID());
                if (session.game.isP1() != mySide) return; // TODO: decide if activation should be allowed off-turn
                int slot = payload.boardSlot();
                if (slot < 0 || slot > 5) return;
                if (BattleGame.getOwner(slot) != mySide) return; // not your card
                if (session.game.getCard(slot).isEmpty()) return;
                session.game.activate(slot);
                if (!session.game.hasGameEnded()) session.board.syncData();
            });
        });
    }

    /** Runs client-side. */
    public static void handleBattleSync(BattleSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientBattleState.update(payload));
    }
}
