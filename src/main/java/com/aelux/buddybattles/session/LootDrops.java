package com.aelux.buddybattles.session;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Map;

final class LootDrops {
    private LootDrops() {
    }

    static void drop(Level level, double x, double y, double z, Map<Item, Integer> loot) {
        for (Map.Entry<Item, Integer> entry : loot.entrySet()) {
            Item item = entry.getKey();
            int remaining = entry.getValue();
            int maxStackSize = new ItemStack(item).getMaxStackSize();

            while (remaining > 0) {
                int stackSize = Math.min(remaining, maxStackSize);
                level.addFreshEntity(new ItemEntity(level, x, y, z, new ItemStack(item, stackSize)));
                remaining -= stackSize;
            }
        }
    }
}
