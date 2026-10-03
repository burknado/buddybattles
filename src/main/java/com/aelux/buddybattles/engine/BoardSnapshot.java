package com.aelux.buddybattles.engine;

import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

/** A complete picture of the board AND both hands at one instant. */
public record BoardSnapshot(
        int health1, int health2,
        int energy1, int energy2,
        ItemStack[] board, // 6 slots, absolute indices (0-2 = p1, 3-5 = p2)
        int[] power, // 6 slots, current in-battle power (not base card power)
        int[] status, // 6 slots, BattleStatusEffect ordinals, EMPTY (0) meaning none
        ItemStack[] hand1, // 3 slots, p1's real hand
        ItemStack[] hand2, // 3 slots, p2's real hand,
        boolean activePlayer // True if p1's turn, False if p2's turn
) {
    public static final BoardSnapshot EMPTY = new BoardSnapshot(
            0, 0, 0, 0,
            emptySlots(6), new int[6], new int[6],
            emptySlots(3), emptySlots(3),
            true
    );

    private static ItemStack[] emptySlots(int count) {
        ItemStack[] slots = new ItemStack[count];
        Arrays.fill(slots, ItemStack.EMPTY);
        return slots;
    }

    /** The sanitized, per-player view actually sent over the wire. */
    public PlayerView toPlayerView(boolean mySide) {
        ItemStack[] myHand = mySide ? hand1 : hand2;
        ItemStack[] opponentHand = mySide ? hand2 : hand1;
        int opponentHandCount = (int) Arrays.stream(opponentHand).filter(s -> !s.isEmpty()).count();
        return new PlayerView(health1, health2, energy1, energy2, board, power, status, myHand, opponentHandCount, activePlayer);
    }

    public record PlayerView(
            int health1, int health2,
            int energy1, int energy2,
            ItemStack[] board,
            int[] power,
            int[] status,
            ItemStack[] myHand, // 3 slots, real cards - always the receiving player's own hand
            int opponentHandCount, // never the actual cards,
            boolean activePlayer
    ) {
        public static final PlayerView EMPTY = BoardSnapshot.EMPTY.toPlayerView(true);
    }
}
