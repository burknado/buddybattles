package com.aelux.buddybattles.engine.log;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.util.ArrayList;
import java.util.List;

public class BattleComponent {
    public static final Codec<Component> COMPONENT_CODEC = Codec.of(
            new Encoder<>() {
                @Override
                public <T> DataResult<T> encode(Component input, DynamicOps<T> ops, T prefix) {
                    JsonElement jsonInput = ComponentSerialization.CODEC.encodeStart(
                            com.mojang.serialization.JsonOps.INSTANCE,
                            input
                    ).getOrThrow();

                    return DataResult.success(JsonOps.INSTANCE.convertTo(ops, jsonInput));
                }
            }, new Decoder<>() {
                @Override
                public <T> DataResult<Pair<Component, T>> decode(DynamicOps<T> ops, T input) {
                    JsonElement tag = ops.convertTo(JsonOps.INSTANCE, input);
                    try {
                        return DataResult.success(Pair.of(ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, tag).getOrThrow(), ops.empty()));
                    } catch (JsonSyntaxException e) {
                        return DataResult.error(e::getMessage);
                    }
                }
            }, "component");

    public static final Codec<BattleComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            COMPONENT_CODEC.fieldOf("hover").forGetter(BattleComponent::getHoverText),
            IBattleIcon.ALL_ICON_CODEC.listOf().fieldOf("icons").forGetter(BattleComponent::getBattleIcons)
    ).apply(instance, BattleComponent::new));
    public static final Codec<List<BattleComponent>> LIST_CODEC = CODEC.listOf();

    private final Component hoverText;
    private final List<IBattleIcon> battleIcons = new ArrayList<>();

    public Component getHoverText() {
        return hoverText;
    }

    public List<IBattleIcon> getBattleIcons() {
        return battleIcons;
    }

    public BattleComponent(Component hoverText) {
        this.hoverText = hoverText;
    }

    public BattleComponent(Component hoverText, List<IBattleIcon> icons) {
        this.hoverText = hoverText;
        this.battleIcons.addAll(icons);
    }
}
