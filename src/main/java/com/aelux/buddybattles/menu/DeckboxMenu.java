package com.aelux.buddybattles.menu;

import com.wildcard.buddycards.item.BuddycardItem;
import com.aelux.buddybattles.config.BuddyBattlesConfig;
import com.aelux.buddybattles.deck.DeckboxContainer;
import com.aelux.buddybattles.engine.CardBattleStatsRegistry;
import com.aelux.buddybattles.registries.BuddyBattlesMenus;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public class DeckboxMenu extends AbstractContainerMenu {
    private final DeckboxContainer inventory;

    public DeckboxMenu(int id, Inventory playerInv) {
        this(id, playerInv, new DeckboxContainer(playerInv.getSelected()));
    }

    public DeckboxMenu(int id, Inventory playerInv, DeckboxContainer inv) {
        super(BuddyBattlesMenus.DECKBOX_MENU.get(), id);
        checkContainerSize(inv, inv.getContainerSize());
        this.inventory = inv;

        // Deck slots (2 rows x 8 = 16)
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 8; x++) {
                this.addSlot(new DeckSlot(inventory, x + (y * 8), 8 + x * 18, 18 + y * 18));
            }
        }
        // Player inventory
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                this.addSlot(new InvSlot(playerInv, x + (y * 9) + 9, 8 + x * 18, 68 + y * 18));
            }
        }
        // Hotbar
        for (int x = 0; x < 9; x++) {
            this.addSlot(new InvSlot(playerInv, x, 8 + x * 18, 126));
        }

        this.inventory.startOpen(playerInv.player);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public static class DeckSlot extends Slot {
        public DeckSlot(Container inventoryIn, int index, int xPosition, int yPosition) {
            super(inventoryIn, index, xPosition, yPosition);
        }

        // Only let cards go into deck slots if they're battle-ready and within their rarity's duplicate limit.
        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!(stack.getItem() instanceof BuddycardItem item)) return false;
            if (CardBattleStatsRegistry.get(stack).isEmpty()) return false;

            Rarity r = item.getRarity();
            int max = BuddyBattlesConfig.maxCopiesFor(r);

            for (int i = 0; i < DeckboxContainer.SIZE; i++) {
                ItemStack existing = container.getItem(i);
                if (!existing.isEmpty() && existing.getItem() == item) {
                    if (--max <= 0) return false;
                }
            }
            return true;
        }

        // Only 1 card per slot
        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    public class InvSlot extends Slot {
        public InvSlot(Container inventoryIn, int index, int xPosition, int yPosition) {
            super(inventoryIn, index, xPosition, yPosition);
        }

        // Don't let the currently-open deckbox itself be picked up out from under its own menu.
        @Override
        public boolean mayPickup(Player player) {
            return this.getItem() != inventory.deckbox;
        }
    }

    @Override
    public void removed(Player player) {
        inventory.stopOpen(player);
        super.removed(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            stack = slotStack.copy();
            if (index < slots.size() - 36) {
                if (!this.moveItemStackTo(slotStack, slots.size() - 36, slots.size(), true)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(slotStack, 0, slots.size() - 36, false)) {
                return ItemStack.EMPTY;
            }
            if (slotStack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return stack;
    }
}
