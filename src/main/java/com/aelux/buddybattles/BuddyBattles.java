package com.aelux.buddybattles;

import com.wildcard.buddycards.registries.BuddycardsMisc;
import com.aelux.buddybattles.config.BuddyBattlesConfig;
import com.aelux.buddybattles.datapack.DeckDefinitionLoader;
import com.aelux.buddybattles.datapack.MobDeckLoader;
import com.aelux.buddybattles.datapack.VillagerDeckLoader;
import com.aelux.buddybattles.engine.CardBattleStatsBootstrap;
import com.aelux.buddybattles.interrupt.BattleInterruptHandler;
import com.aelux.buddybattles.network.BuddyBattlesNetworking;
import com.aelux.buddybattles.registries.BuddyBattlesItems;
import com.aelux.buddybattles.registries.BuddyBattlesMenus;
import com.aelux.buddybattles.session.BattleSessionManager;
import com.aelux.buddybattles.trigger.BattleChallengeHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/** Buddy Battles - challenge villagers, piglins, enderlings and players to Buddycards duels. */
@Mod(BuddyBattles.MOD_ID)
public class BuddyBattles {
    public static final String MOD_ID = "buddybattles";

    public BuddyBattles(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, BuddyBattlesConfig.SPEC);

        BuddyBattlesItems.ITEMS.register(modEventBus);
        BuddyBattlesMenus.MENUS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::buildCreativeTab);
        modEventBus.addListener(BuddyBattlesNetworking::register);

        NeoForge.EVENT_BUS.register(new BattleChallengeHandler());
        NeoForge.EVENT_BUS.register(new BattleInterruptHandler());
        NeoForge.EVENT_BUS.register(BattleSessionManager.INSTANCE);
        NeoForge.EVENT_BUS.addListener(DeckDefinitionLoader::register);
        NeoForge.EVENT_BUS.addListener(VillagerDeckLoader::register);
        NeoForge.EVENT_BUS.addListener(MobDeckLoader::register);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Runs after all item registries (including Buddycards') are populated.
        event.enqueueWork(CardBattleStatsBootstrap::register);
    }

    private void buildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == BuddycardsMisc.MAIN_TAB.getKey()) {
            event.accept(BuddyBattlesItems.DECKBOX);
        }
    }
}
