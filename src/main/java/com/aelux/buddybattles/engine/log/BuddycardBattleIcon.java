package com.aelux.buddybattles.engine.log;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class BuddycardBattleIcon implements IBattleIcon {
    public static final Codec<BuddycardBattleIcon> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("card").forGetter(BuddycardBattleIcon::getItem)
    ).apply(instance, BuddycardBattleIcon::new));

    private final Item item;

    private BuddycardBattleIcon(Item item) {
        this.item = item;
    }

    @Override
    public int width() {
        return 16;
    }

    @Override
    public int height() {
        return width();
    }

    public Item getItem() {
        return item;
    }

    public static IBattleIcon create(ItemStack stack) {
        return new BuddycardBattleIcon(stack.getItem());
    }
}
