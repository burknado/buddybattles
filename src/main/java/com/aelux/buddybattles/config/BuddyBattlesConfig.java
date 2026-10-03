package com.aelux.buddybattles.config;

import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.ModConfigSpec;

public class BuddyBattlesConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static ModConfigSpec.BooleanValue enableVillagerDuels;
    public static ModConfigSpec.BooleanValue enablePiglinDuels;
    public static ModConfigSpec.BooleanValue enableEnderlingDuels;
    public static ModConfigSpec.DoubleValue villagerAiThinkDelaySeconds;
    public static ModConfigSpec.IntValue challengeExpirySeconds;
    public static ModConfigSpec.IntValue rebattleCooldownSeconds;
    public static ModConfigSpec.IntValue deckLimitCommon;
    public static ModConfigSpec.IntValue deckLimitUncommon;
    public static ModConfigSpec.IntValue deckLimitRare;
    public static ModConfigSpec.IntValue deckLimitEpic;

    static {
        BUILDER.comment("Buddy Battles config");

        enableVillagerDuels = BUILDER.comment(
                        "\nEnables challenging villagers to Buddycards duels via right-click.",
                        "Default is true - this addon's whole purpose is this feature, so",
                        "defaulting it off just confuses testing.")
                .define("villagerDuels.enabled", true);

        enablePiglinDuels = BUILDER.comment(
                        "\nEnables challenging (adult) piglins to Buddycards duels via right-click,",
                        "the same way villagers work. Default is true.")
                .define("piglinDuels.enabled", true);

        enableEnderlingDuels = BUILDER.comment(
                        "\nEnables challenging Enderlings to Buddycards duels via right-click,",
                        "the same way villagers work. Default is true.")
                .define("enderlingDuels.enabled", true);

        villagerAiThinkDelaySeconds = BUILDER.comment("\nHow long (in seconds) the villager/piglin/enderling 'thinks' before playing a card/ending its turn, purely for pacing/readability. Default 1.0")
                .defineInRange("villagerDuels.aiThinkDelaySeconds", 1.0, 0.0, 10.0);

        challengeExpirySeconds = BUILDER.comment(
                        "\nHow long (in seconds) a player-vs-player duel challenge stays open.",
                        "The recipient must right-click the challenger back, holding their own deck,",
                        "within this window to accept. Default 30.")
                .defineInRange("challengeExpirySeconds", 30, 5, 300);

        rebattleCooldownSeconds = BUILDER.comment(
                        "\nHow long (in seconds) after a villager/piglin duel STARTS before the same",
                        "player can challenge that exact mob again. Default 60.")
                .defineInRange("rebattleCooldownSeconds", 60, 0, 600);

        deckLimitCommon = BUILDER.comment("\nMax duplicates of a common card in a deck, 0-16. Default 4.")
                .defineInRange("deck.limitCommon", 4, 0, 16);
        deckLimitUncommon = BUILDER.comment("\nMax duplicates of an uncommon card in a deck, 0-16. Default 3.")
                .defineInRange("deck.limitUncommon", 3, 0, 16);
        deckLimitRare = BUILDER.comment("\nMax duplicates of a rare card in a deck, 0-16. Default 2.")
                .defineInRange("deck.limitRare", 2, 0, 16);
        deckLimitEpic = BUILDER.comment("\nMax duplicates of an epic card in a deck, 0-16. Default 1.")
                .defineInRange("deck.limitEpic", 1, 0, 16);

        SPEC = BUILDER.build();
    }

    public static int maxCopiesFor(Rarity rarity) {
        if (rarity.equals(Rarity.COMMON)) return deckLimitCommon.get();
        if (rarity.equals(Rarity.UNCOMMON)) return deckLimitUncommon.get();
        if (rarity.equals(Rarity.RARE)) return deckLimitRare.get();
        return deckLimitEpic.get();
    }
}
