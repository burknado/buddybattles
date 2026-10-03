package com.aelux.buddybattles.deck;

import net.minecraft.world.item.ItemStack;

public interface DeckSource {
    /** The cards contained, in play order. May contain empty stacks for unfilled slots. */
    ItemStack[] getCards(ItemStack deckStack);

    boolean isEmpty(ItemStack deckStack);
}
