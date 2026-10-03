package com.aelux.buddybattles.client;

import com.wildcard.buddycards.screens.BinderScreen;
import com.aelux.buddybattles.BuddyBattles;
import com.aelux.buddybattles.engine.BattleAbility;
import com.aelux.buddybattles.engine.CardBattleStats;
import com.aelux.buddybattles.engine.CardBattleStatsRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@EventBusSubscriber(modid = BuddyBattles.MOD_ID, value = Dist.CLIENT)
public class BattleTooltipHandler {
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!isRelevantScreen() && !Screen.hasShiftDown()) return;

        ItemStack stack = event.getItemStack();
        Optional<CardBattleStats> statsOpt = CardBattleStatsRegistry.get(stack);
        if (statsOpt.isEmpty()) return;
        CardBattleStats stats = statsOpt.get();

        List<Component> tooltip = event.getToolTip();
        Component name = tooltip.isEmpty() ? stack.getHoverName() : tooltip.get(0);
        tooltip.clear();
        tooltip.add(name);

        MutableComponent costPower = Component.literal(Integer.toString(stats.cost()))
                .append(Component.translatable("item.buddycards.buddycard.cost"))
                .append(Component.translatable("item.buddycards.buddycard.number_separator"))
                .append(Integer.toString(stats.power()))
                .append(Component.translatable("item.buddycards.buddycard.power"));
        tooltip.add(costPower);

        if (stats.abilities().isEmpty()) {
            tooltip.add(Component.translatable("item.buddycards.buddycard.unimplemented").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            // A dual-ability card registers two BattleAbility entries under different BattleEvent keys but with distinct names.
            Set<String> shown = new LinkedHashSet<>();
            for (BattleAbility ability : stats.abilities().values()) {
                if (!shown.add(ability.name)) continue;
                tooltip.add(Component.translatable("battles.ability.buddycards." + ability.name).withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("battles.ability.buddycards." + ability.name + ".desc").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    private static boolean isRelevantScreen() {
        Screen screen = Minecraft.getInstance().screen;
        return screen instanceof BattleScreen
                || screen instanceof DeckboxScreen
                || screen instanceof BinderScreen;
    }
}
