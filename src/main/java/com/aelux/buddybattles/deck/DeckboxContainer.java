package com.aelux.buddybattles.deck;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

/**
 * The deckbox's editable card slots, stored in its DataComponents.CONTAINER component.
 * TODO: per-card win/loss stats from 1.20 aren't persisted yet (needs its own data component).
 */
public class DeckboxContainer extends SimpleContainer {
    public static final int SIZE = 16;

    public final ItemStack deckbox;

    public DeckboxContainer(ItemStack deck) {
        super(SIZE);
        this.deckbox = deck;
    }

    @Override
    public void startOpen(Player player) {
        for (int i = 0; i < getContainerSize(); i++) setItem(i, ItemStack.EMPTY);
        ItemContainerContents contents = deckbox.get(DataComponents.CONTAINER);
        if (contents == null) return;
        int i = 0;
        for (ItemStack stack : contents.nonEmptyItems()) {
            if (i >= getContainerSize()) break;
            setItem(i++, stack);
        }
    }

    @Override
    public void stopOpen(Player player) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = getItem(i);
            if (!stack.isEmpty()) items.add(stack);
        }
        deckbox.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }
}
