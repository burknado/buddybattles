package com.aelux.buddybattles.engine;

import com.google.common.collect.ImmutableListMultimap;

/** The battle-relevant stats for a card. */
public record CardBattleStats(int cost, int power, ImmutableListMultimap<BattleEvent, BattleAbility> abilities) {
    public static CardBattleStats of(int cost, int power) {
        return new CardBattleStats(cost, power, ImmutableListMultimap.of());
    }

    public static CardBattleStats of(int cost, int power, BattleAbility... abilities) {
        BattleAbility.Builder builder = new BattleAbility.Builder();
        for (BattleAbility ability : abilities) builder.add(ability);
        return new CardBattleStats(cost, power, builder.build());
    }
}
