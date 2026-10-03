package com.aelux.buddybattles.engine;

import com.aelux.buddybattles.BuddyBattles;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class CardTags {
    public static final TagKey<Item> ANIMAL = of("animal");
    public static final TagKey<Item> ENCHANTABLE = of("enchantable");
    public static final TagKey<Item> FIRE = of("fire");
    public static final TagKey<Item> FOOD = of("food");
    public static final TagKey<Item> FUNGAL = of("fungal");
    public static final TagKey<Item> METAL = of("metal");
    public static final TagKey<Item> MONSTER = of("monster");
    public static final TagKey<Item> REDSTONE = of("redstone");
    public static final TagKey<Item> EQUIPMENT = of("equipment");

    private static TagKey<Item> of(String name) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(BuddyBattles.MOD_ID, "battles/" + name));
    }
}
