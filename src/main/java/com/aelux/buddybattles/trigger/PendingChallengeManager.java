package com.aelux.buddybattles.trigger;

import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PendingChallengeManager {
    public static final PendingChallengeManager INSTANCE = new PendingChallengeManager();

    private record Key(UUID recipientId, UUID challengerId) {}

    private final Map<Key, PendingChallenge> pending = new HashMap<>();

    private PendingChallengeManager() {}

    public void challenge(UUID recipientId, UUID challengerId, ItemStack challengerDeck) {
        // A repeat challenge from the same challenger to the same recipient just refreshes the timestamp/deck snapshot.
        pending.put(new Key(recipientId, challengerId), new PendingChallenge(challengerId, challengerDeck.copy(), System.currentTimeMillis()));
    }

    public Optional<PendingChallenge> tryAccept(UUID accepterId, UUID allegedChallengerId, long expiryMillis) {
        Key key = new Key(accepterId, allegedChallengerId);
        PendingChallenge challenge = pending.get(key);
        if (challenge == null) return Optional.empty();
        if (challenge.isExpiredAt(System.currentTimeMillis(), expiryMillis)) {
            pending.remove(key);
            return Optional.empty();
        }
        pending.remove(key);
        return Optional.of(challenge);
    }

    public void cancel(UUID recipientId, UUID challengerId) {
        pending.remove(new Key(recipientId, challengerId));
    }
}
