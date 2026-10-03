package com.aelux.buddybattles.trigger;

import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** An outstanding "so-and-so wants to duel you" request, stored on the recipient's side. */
public record PendingChallenge(UUID challengerId, ItemStack challengerDeck, long createdAtMillis) {
    public boolean isExpiredAt(long nowMillis, long expiryMillis) {
        return nowMillis - createdAtMillis > expiryMillis;
    }
}
