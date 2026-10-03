package com.aelux.buddybattles.registries;

import com.aelux.buddybattles.BuddyBattles;
import com.aelux.buddybattles.menu.BattleMenu;
import com.aelux.buddybattles.menu.DeckboxMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BuddyBattlesMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, BuddyBattles.MOD_ID);

    // No extra data read on open.
    public static final DeferredHolder<MenuType<?>, MenuType<BattleMenu>> BATTLE_MENU =
            MENUS.register("battle", () -> IMenuTypeExtension.create((windowId, inv, buf) -> new BattleMenu(windowId, inv.player)));

    // No extra data either.
    public static final DeferredHolder<MenuType<?>, MenuType<DeckboxMenu>> DECKBOX_MENU =
            MENUS.register("deckbox", () -> IMenuTypeExtension.create((windowId, inv, buf) -> new DeckboxMenu(windowId, inv)));
}
