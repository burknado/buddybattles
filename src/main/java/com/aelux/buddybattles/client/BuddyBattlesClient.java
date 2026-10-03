package com.aelux.buddybattles.client;

import com.aelux.buddybattles.BuddyBattles;
import com.aelux.buddybattles.registries.BuddyBattlesMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = BuddyBattles.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class BuddyBattlesClient {
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(BuddyBattlesMenus.BATTLE_MENU.get(), BattleScreen::new);
        event.register(BuddyBattlesMenus.DECKBOX_MENU.get(), DeckboxScreen::new);
    }
}
