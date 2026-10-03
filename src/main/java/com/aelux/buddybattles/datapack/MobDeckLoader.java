package com.aelux.buddybattles.datapack;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.aelux.buddybattles.engine.BattleBoard;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Loads data/<namespace>/buddybattles/<mob>_decks/default.json for mobs without tiers (piglins, enderlings). */
public class MobDeckLoader extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final MobDeckLoader PIGLIN = new MobDeckLoader("piglin");
    public static final MobDeckLoader ENDERLING = new MobDeckLoader("enderling");

    private final String mobName;
    private final String directory;
    private List<String> deckNames = List.of();
    private List<Map<Item, Integer>> lootBundles = List.of();

    private MobDeckLoader(String mobName) {
        this.mobName = mobName;
        this.directory = "buddybattles/" + mobName + "_decks";
    }

    public static void register(AddReloadListenerEvent event) {
        event.addListener(PIGLIN);
        event.addListener(ENDERLING);
    }

    /** A randomly picked deck from the assignment file, or empty if none resolves. */
    public List<Item> getPool() {
        if (deckNames.isEmpty()) return List.of();
        return DeckDefinitionLoader.getDeck(deckNames.get(BattleBoard.RANDOM.nextInt(deckNames.size())));
    }

    /** A randomly picked loot bundle, or empty if none is configured. */
    public Map<Item, Integer> getLoot() {
        if (lootBundles.isEmpty()) return Map.of();
        return lootBundles.get(BattleBoard.RANDOM.nextInt(lootBundles.size()));
    }

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonElement> result = new HashMap<>();
        for (Map.Entry<ResourceLocation, Resource> entry : resourceManager.listResources(directory, path -> path.getPath().endsWith(".json")).entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                result.put(entry.getKey(), JsonParser.parseReader(reader));
            } catch (IOException | RuntimeException e) {
                LOGGER.error("Buddy Battles: failed to parse {} deck file {} - skipping it.", mobName, entry.getKey(), e);
            }
        }
        return result;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> loaded, ResourceManager resourceManager, ProfilerFiller profiler) {
        List<String> newDeckNames = List.of();
        List<Map<Item, Integer>> newLootBundles = List.of();

        for (Map.Entry<ResourceLocation, JsonElement> entry : loaded.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            if (!fileId.getPath().equals(directory + "/default.json")) {
                LOGGER.warn("Buddy Battles: {} deck file {} isn't named \"default.json\" - skipping.", mobName, fileId);
                continue;
            }
            List<String> parsedNames = parseDeckNames(fileId, entry.getValue());
            if (!parsedNames.isEmpty()) newDeckNames = parsedNames;
            newLootBundles = DeckDefinitionLoader.parseLootBundles(fileId, entry.getValue(), "loot");
        }

        deckNames = newDeckNames;
        lootBundles = newLootBundles;
        LOGGER.info("Buddy Battles: loaded {} deck assignment, decks: {}, loot bundles: {}.", mobName, newDeckNames, newLootBundles.size());
    }

    private List<String> parseDeckNames(ResourceLocation fileId, JsonElement json) {
        List<String> names = new ArrayList<>();
        if (!json.isJsonObject() || !json.getAsJsonObject().has("decks")) {
            LOGGER.warn("Buddy Battles: {} deck file {} is missing its \"decks\" array - skipping.", mobName, fileId);
            return names;
        }
        JsonElement decksElement = json.getAsJsonObject().get("decks");
        if (!decksElement.isJsonArray()) {
            LOGGER.warn("Buddy Battles: {} deck file {}'s \"decks\" field isn't an array - skipping.", mobName, fileId);
            return names;
        }
        for (JsonElement nameElement : decksElement.getAsJsonArray()) {
            if (nameElement.isJsonPrimitive()) {
                names.add(nameElement.getAsString());
            } else {
                LOGGER.warn("Buddy Battles: {} deck file {} has a non-string entry in \"decks\" - skipping it.", mobName, fileId);
            }
        }
        return names;
    }
}
