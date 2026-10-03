package com.aelux.buddybattles.menu;

import com.aelux.buddybattles.registries.BuddyBattlesMenus;
import com.aelux.buddybattles.session.BattleSessionManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class BattleMenu extends AbstractContainerMenu {
    public BattleMenu(int windowId, Player player) {
        super(BuddyBattlesMenus.BATTLE_MENU.get(), windowId);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // no slots to shift-click between
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /** Called server-side (and client-side too, for carried-item cleanup. */
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer) {
            BattleSessionManager.INSTANCE.get(player.getUUID()).ifPresent(session -> session.interrupt());
        }
    }
}
