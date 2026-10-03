package com.aelux.buddybattles.registries;

import com.aelux.buddybattles.BuddyBattles;
import com.aelux.buddybattles.deck.DeckboxItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BuddyBattlesItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BuddyBattles.MOD_ID);

    public static final DeferredItem<DeckboxItem> DECKBOX =
            ITEMS.register("deckbox", () -> new DeckboxItem(new Item.Properties().stacksTo(1)));
}
