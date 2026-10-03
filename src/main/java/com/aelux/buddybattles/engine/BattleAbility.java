package com.aelux.buddybattles.engine;

import com.google.common.collect.ImmutableListMultimap;

public class BattleAbility {
    public final BattleEvent event;
    public final BattleAbilityFunc ability;
    public final String name;

    public BattleAbility(BattleEvent event, String name, BattleAbilityFunc ability) {
        this.event = event;
        this.name = name;
        this.ability = ability;
    }

    public ImmutableListMultimap<BattleEvent, BattleAbility> single() {
        return new Builder().add(this).build();
    }

    /** Battle ability function (game, slot, target, source) -&gt. */
    @FunctionalInterface
    public interface BattleAbilityFunc {
        boolean trigger(BattleGame game, int slot, int target, int source);
    }

    public static class Builder {
        private final ImmutableListMultimap.Builder<BattleEvent, BattleAbility> abilityMap = ImmutableListMultimap.builder();

        public Builder add(BattleAbility ability) {
            abilityMap.put(ability.event, ability);
            return this;
        }

        public ImmutableListMultimap<BattleEvent, BattleAbility> build() {
            return abilityMap.build();
        }
    }
}
