package com.aelux.buddybattles.engine.cards;

import com.mojang.logging.LogUtils;
import com.wildcard.buddycards.item.BuddycardItem;
import com.aelux.buddybattles.engine.CardBattleStats;
import com.aelux.buddybattles.engine.CardBattleStatsRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;

/** Gives a minimal 1/1 stat line to any registered card without its own battle stats. */
public class FallbackCards {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** 1 cost, 1 power, no ability. */
    public static final CardBattleStats FALLBACK = CardBattleStats.of(1, 1);

    public static void fillGaps() {
        int filled = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof BuddycardItem && CardBattleStatsRegistry.get(item).isEmpty()) {
                CardBattleStatsRegistry.register(item, FALLBACK);
                filled++;
            }
        }
        if (filled > 0) {
            LOGGER.warn("Buddy Battles: {} Buddycard item(s) had no ported battle data and got the 1-power/no-ability fallback instead - "
                    + "check which ones with CardBattleStatsRegistry.getAllRegisteredItems() if you want to hand-port them properly.", filled);
        }
    }
}
