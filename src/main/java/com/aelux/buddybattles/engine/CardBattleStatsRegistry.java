package com.aelux.buddybattles.engine;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Maps card items to their battle stats and abilities. */
public class CardBattleStatsRegistry {
    private static final Map<Item, CardBattleStats> STATS = new HashMap<>();

    public static void register(Item item, CardBattleStats stats) {
        STATS.put(item, stats);
    }

    public static Optional<CardBattleStats> get(Item item) {
        return Optional.ofNullable(STATS.get(item));
    }

    public static Optional<CardBattleStats> get(ItemStack stack) {
        return get(stack.getItem());
    }

    public static boolean isBattleCard(ItemStack stack) {
        return !stack.isEmpty() && STATS.containsKey(stack.getItem());
    }

    /** All items currently registered with battle stats - used e.g. to generate a villager's deck. */
    public static java.util.Set<Item> getAllRegisteredItems() {
        return java.util.Collections.unmodifiableSet(STATS.keySet());
    }
}
