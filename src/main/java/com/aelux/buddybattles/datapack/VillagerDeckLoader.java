package com.aelux.buddybattles.datapack;

import com.google.gson.JsonElement;
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

/**
 * Loads villager_decks/<profession>/<level>.json. Falls back to the _default profession
 * folder when a profession has no file for that level.
 */
public class VillagerDeckLoader extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DIRECTORY = "buddybattles/villager_decks";
    public static final String DEFAULT_PROFESSION = "_default";

    private record Assignment(List<String> deckNames, List<Map<Item, Integer>> lootBundles) {
        static final Assignment EMPTY = new Assignment(List.of(), List.of());
    }

    /** profession path (e.g. "farmer", or DEFAULT_PROFESSION) -> level -> that tier's assignment. */
    private static final Map<String, Map<Integer, Assignment>> ASSIGNMENTS = new HashMap<>();

    public static void register(AddReloadListenerEvent event) {
        event.addListener(new VillagerDeckLoader());
    }

    public static List<Item> getPoolFor(String professionPath, int level) {
        Assignment a = resolveAssignment(professionPath, level);
        if (a.deckNames().isEmpty()) return List.of();
        String picked = a.deckNames().get(BattleBoard.RANDOM.nextInt(a.deckNames().size()));
        return DeckDefinitionLoader.getDeck(picked);
    }

    /** Randomly picks ONE loot bundle from the resolved assignment's list and returns it whole. */
    public static Map<Item, Integer> getLootFor(String professionPath, int level) {
        List<Map<Item, Integer>> bundles = resolveAssignment(professionPath, level).lootBundles();
        if (bundles.isEmpty()) return Map.of();
        return bundles.get(BattleBoard.RANDOM.nextInt(bundles.size()));
    }

    /** Resolves which assignment actually applies. */
    private static Assignment resolveAssignment(String professionPath, int level) {
        Assignment exact = assignmentOrNull(professionPath, level);
        if (exact != null && !exact.deckNames().isEmpty()) return exact;
        Assignment fallback = assignmentOrNull(DEFAULT_PROFESSION, level);
        return fallback != null ? fallback : Assignment.EMPTY;
    }

    private static Assignment assignmentOrNull(String professionPath, int level) {
        Map<Integer, Assignment> byLevel = ASSIGNMENTS.get(professionPath);
        return byLevel == null ? null : byLevel.get(level);
    }

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonElement> result = new HashMap<>();
        for (Map.Entry<ResourceLocation, Resource> entry : resourceManager.listResources(DIRECTORY, path -> path.getPath().endsWith(".json")).entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                result.put(entry.getKey(), com.google.gson.JsonParser.parseReader(reader));
            } catch (IOException | RuntimeException e) {
                LOGGER.error("Buddy Battles: failed to parse villager deck file {} - skipping it.", entry.getKey(), e);
            }
        }
        return result;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> loaded, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, Map<Integer, Assignment>> newAssignments = new HashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : loaded.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            String[] professionAndLevel = parseProfessionAndLevel(fileId);
            if (professionAndLevel == null) {
                LOGGER.warn("Buddy Battles: villager deck file {} isn't at buddybattles/villager_decks/<profession>/<level>.json - skipping.", fileId);
                continue;
            }
            String profession = professionAndLevel[0];
            int level;
            try {
                level = Integer.parseInt(professionAndLevel[1]);
            } catch (NumberFormatException e) {
                LOGGER.warn("Buddy Battles: villager deck file {} doesn't name a trade level 1-5 (expected e.g. \"3.json\") - skipping.", fileId);
                continue;
            }
            if (level < 1 || level > 5) {
                LOGGER.warn("Buddy Battles: villager deck file {} names trade level {}, outside 1-5 - skipping.", fileId, level);
                continue;
            }

            List<String> deckNames = parseDeckNames(fileId, entry.getValue());
            List<Map<Item, Integer>> lootBundles = DeckDefinitionLoader.parseLootBundles(fileId, entry.getValue(), "loot");
            if (!deckNames.isEmpty() || !lootBundles.isEmpty()) {
                newAssignments.computeIfAbsent(profession, k -> new HashMap<>()).put(level, new Assignment(deckNames, lootBundles));
            }
        }

        ASSIGNMENTS.clear();
        ASSIGNMENTS.putAll(newAssignments);
        LOGGER.info("Buddy Battles: loaded villager deck assignments for profession(s) {}.", newAssignments.keySet());
    }

    private static String[] parseProfessionAndLevel(ResourceLocation fileId) {
        String path = fileId.getPath();
        if (!path.startsWith(DIRECTORY + "/")) return null;
        String relative = path.substring((DIRECTORY + "/").length()); // "farmer/3.json"
        String[] parts = relative.split("/");
        if (parts.length != 2) return null; // must be exactly <profession>/<level>.json, no other nesting
        String profession = parts[0];
        String fileName = parts[1];
        if (!fileName.endsWith(".json")) return null;
        String level = fileName.substring(0, fileName.length() - ".json".length());
        return new String[]{profession, level};
    }

    private static List<String> parseDeckNames(ResourceLocation fileId, JsonElement json) {
        List<String> names = new ArrayList<>();
        if (!json.isJsonObject() || !json.getAsJsonObject().has("decks")) {
            LOGGER.warn("Buddy Battles: villager deck file {} is missing its \"decks\" array - skipping.", fileId);
            return names;
        }
        JsonElement decksElement = json.getAsJsonObject().get("decks");
        if (!decksElement.isJsonArray()) {
            LOGGER.warn("Buddy Battles: villager deck file {}'s \"decks\" field isn't an array - skipping.", fileId);
            return names;
        }

        for (JsonElement nameElement : decksElement.getAsJsonArray()) {
            if (!nameElement.isJsonPrimitive()) {
                LOGGER.warn("Buddy Battles: villager deck file {} has a non-string entry in \"decks\" - skipping just that entry.", fileId);
                continue;
            }
            names.add(nameElement.getAsString());
        }
        return names;
    }
}
