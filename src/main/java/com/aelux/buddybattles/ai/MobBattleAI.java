package com.aelux.buddybattles.ai;

import com.aelux.buddybattles.config.BuddyBattlesConfig;
import com.aelux.buddybattles.session.BattleParticipant;
import com.aelux.buddybattles.session.BattleSession;
import net.minecraft.world.item.ItemStack;

public class MobBattleAI {
    private final BattleSession session;
    private final BattleParticipant participant;

    private int thinkTicksRemaining = 0;

    public MobBattleAI(BattleSession session, BattleParticipant participant) {
        this.session = session;
        this.participant = participant;
    }

    /** Called on every server tick. No-ops unless it's actually this side's turn. */
    public void tick() {
        if (session.isEnded()) return;

        boolean iAmP1 = session.p1 == participant;

        if (session.game.isP1() != iAmP1) {
            // Not my turn - reset the think timer so the delay starts fresh once it is.
            thinkTicksRemaining = thinkDelayTicks();
            return;
        }

        if (thinkTicksRemaining > 0) {
            thinkTicksRemaining--;
            return;
        }

        act(iAmP1);
        thinkTicksRemaining = thinkDelayTicks();
    }

    private int thinkDelayTicks() {
        return (int) (BuddyBattlesConfig.villagerAiThinkDelaySeconds.get() * 20.0);
    }

    private void act(boolean mySide) {
        var board = session.board;
        var game = session.game;

        ItemStack[] hand = board.getHand(mySide);
        for (int handIndex = 0; handIndex < hand.length; handIndex++) {
            ItemStack card = hand[handIndex];
            if (card.isEmpty() || !game.canPlay(mySide, card)) continue;

            int emptySlot = findEmptyOwnSlot(mySide);
            if (emptySlot == -1) continue; // hand has a playable card but no room on the board

            board.removeHandCard(mySide, handIndex);
            if (game.playCard(emptySlot, card, mySide)) {
                if (!game.hasGameEnded()) board.syncData();
                return; // one action per think-tick, matching the pacing config
            } else {
                board.tryPutInHand(mySide, card); // playCard failed after all - don't lose the card
            }
        }

        // Nothing playable (or no room) - end the turn.
        if (game.endTurn()) {
            game.nextTurn();
        }
        // If endTurn() returned false, the game just ended.
    }

    private int findEmptyOwnSlot(boolean mySide) {
        int start = mySide ? 0 : 3;
        for (int i = start; i < start + 3; i++) {
            if (session.board.getBoardCard(i).isEmpty()) return i;
        }
        return -1;
    }
}
