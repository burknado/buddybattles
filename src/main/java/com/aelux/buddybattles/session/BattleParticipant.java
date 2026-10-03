package com.aelux.buddybattles.session;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** One side of a battle: a player, or an AI-controlled mob. */
public interface BattleParticipant {
    /** Stable id for this participant - the player's or mob's UUID. */
    UUID getParticipantId();

    Component getDisplayName();

    /** True for AI-controlled mobs, false for players. */
    boolean isAI();

    void markDirty();

    /** Called once when the battle ends, however it ends. */
    void onBattleEnded(boolean won, boolean interrupted);

    /** The card stacks making up this participant's deck for this battle. */
    ItemStack[] getDeck();

    /** Called once, right after construction, so a participant can learn about the session it's now part of. */
    default void attachSession(BattleSession session) {
    }

    /** Called every server tick by BattleSessionManager, for AI-controlled participants to drive their own turn logic. */
    default void tick() {
    }
}
