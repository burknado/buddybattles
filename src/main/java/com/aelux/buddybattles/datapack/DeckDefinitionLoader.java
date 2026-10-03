package com.aelux.buddybattles.datapack;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Loads data/<any namespace>/buddybattles/decks/<deck_name>.json. */
public class DeckDefinitionLoader extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DIRECTORY = "buddybattles/decks";

    private static final Map<String, List<Item>> DECKS = new HashMap<>();

    public static void register(AddReloadListenerEvent event) {
        event.addListener(new DeckDefinitionLoader());
    }

    /** The named deck's exact card list, or empty if no deck by that name is currently loaded. Never null. */
    public static List<Item> getDeck(String name) {
        return DECKS.getOrDefault(name, List.of());
    }

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonElement> result = new HashMap<>();
        for (Map.Entry<ResourceLocation, Resource> entry : resourceManager.listResources(DIRECTORY, path -> path.getPath().endsWith(".json")).entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                result.put(entry.getKey(), com.google.gson.JsonParser.parseReader(reader));
            } catch (IOException | RuntimeException e) {
                LOGGER.error("Buddy Battles: failed to parse deck definition file {} - skipping it.", entry.getKey(), e);
            }
        }
        return result;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> loaded, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, List<Item>> newDecks = new HashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : loaded.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            String name = deckNameFromPath(fileId);
            if (name == null) {
                LOGGER.warn("Buddy Battles: deck definition file {} isn't directly under buddybattles/decks/ - skipping.", fileId);
                continue;
            }

            List<Item> cards = parseCards(fileId, entry.getValue());
            if (!cards.isEmpty()) newDecks.put(name, cards);
        }

        DECKS.clear();
        DECKS.putAll(newDecks);
        LOGGER.info("Buddy Battles: loaded deck definition(s): {}.", newDecks.keySet());
    }

    private static String deckNameFromPath(ResourceLocation fileId) {
        String path = fileId.getPath();
        if (!path.startsWith(DIRECTORY + "/")) return null;
        String relative = path.substring((DIRECTORY + "/").length());
        if (relative.contains("/")) return null; // no subfolders - flat namespace of deck names
        if (!relative.endsWith(".json")) return null;
        return relative.substring(0, relative.length() - ".json".length());
    }

    /** Parses an optional "loot" field: a list of bundles, each an item id -> count object. */
    static List<Map<Item, Integer>> parseLootBundles(ResourceLocation fileId, JsonElement json, String fieldName) {
        List<Map<Item, Integer>> bundles = new ArrayList<>();
        if (!json.isJsonObject() || !json.getAsJsonObject().has(fieldName)) return bundles;
        JsonElement fieldElement = json.getAsJsonObject().get(fieldName);
        if (!fieldElement.isJsonArray()) {
            LOGGER.warn("Buddy Battles: file {}'s \"{}\" field isn't an array of bundles - skipping.", fileId, fieldName);
            return bundles;
        }

        for (JsonElement bundleElement : fieldElement.getAsJsonArray()) {
            if (!bundleElement.isJsonObject()) {
                LOGGER.warn("Buddy Battles: file {} has a non-object entry in \"{}\" - skipping just that bundle.", fileId, fieldName);
                continue;
            }

            Map<Item, Integer> counts = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : bundleElement.getAsJsonObject().entrySet()) {
                String idString = entry.getKey();
                ResourceLocation itemId = ResourceLocation.tryParse(idString);
                if (itemId == null) {
                    LOGGER.warn("Buddy Battles: file {} has an invalid item id \"{}\" in a \"{}\" bundle - skipping just that entry.", fileId, idString, fieldName);
                    continue;
                }
                Item item = BuiltInRegistries.ITEM.get(itemId);
                if (item == Items.AIR) {
                    LOGGER.warn("Buddy Battles: file {} references \"{}\" in a \"{}\" bundle, which isn't a registered item - skipping just that entry.", fileId, idString, fieldName);
                    continue;
                }

                int count;
                try {
                    count = entry.getValue().getAsInt();
                } catch (RuntimeException e) {
                    LOGGER.warn("Buddy Battles: file {}'s count for \"{}\" in a \"{}\" bundle isn't a whole number - skipping just that entry.", fileId, idString, fieldName);
                    continue;
                }
                if (count <= 0) {
                    LOGGER.warn("Buddy Battles: file {} has a non-positive count ({}) for \"{}\" in a \"{}\" bundle - skipping just that entry.", fileId, count, idString, fieldName);
                    continue;
                }

                counts.merge(item, count, Integer::sum);
            }
            if (!counts.isEmpty()) bundles.add(counts);
        }
        return bundles;
    }

    /** "cards" is an OBJECT (card ID -> count) now, not an array. */
    private static List<Item> parseCards(ResourceLocation fileId, JsonElement json) {
        List<Item> cards = new ArrayList<>();
        if (!json.isJsonObject() || !json.getAsJsonObject().has("cards")) {
            LOGGER.warn("Buddy Battles: deck definition file {} is missing its \"cards\" object - skipping.", fileId);
            return cards;
        }
        JsonElement cardsElement = json.getAsJsonObject().get("cards");
        if (!cardsElement.isJsonObject()) {
            LOGGER.warn("Buddy Battles: deck definition file {}'s \"cards\" field isn't an object (card id -> count) - skipping.", fileId);
            return cards;
        }

        for (Map.Entry<String, JsonElement> cardEntry : cardsElement.getAsJsonObject().entrySet()) {
            String idString = cardEntry.getKey();
            ResourceLocation itemId = ResourceLocation.tryParse(idString);
            if (itemId == null) {
                LOGGER.warn("Buddy Battles: deck definition file {} has an invalid item id \"{}\" - skipping just that entry.", fileId, idString);
                continue;
            }
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item == Items.AIR) {
                LOGGER.warn("Buddy Battles: deck definition file {} references \"{}\", which isn't a registered item - skipping just that entry.", fileId, idString);
                continue;
            }

            int count;
            try {
                count = cardEntry.getValue().getAsInt();
            } catch (RuntimeException e) {
                LOGGER.warn("Buddy Battles: deck definition file {}'s count for \"{}\" isn't a whole number - skipping just that entry.", fileId, idString);
                continue;
            }
            if (count <= 0) {
                LOGGER.warn("Buddy Battles: deck definition file {} has a non-positive count ({}) for \"{}\" - skipping just that entry.", fileId, count, idString);
                continue;
            }

            for (int i = 0; i < count; i++) cards.add(item);
        }
        return cards;
    }
}
