package com.aelux.buddybattles.deck;

import com.aelux.buddybattles.menu.DeckboxMenu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Item that stores a deck of cards in its container component.
 * TODO: add the "open" (not full) model variant via an item model predicate.
 */
public class DeckboxItem extends Item implements DeckSource {
    public DeckboxItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack deck = player.getItemInHand(hand);
        if (level instanceof ServerLevel && player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, playerInventory, entity) -> new DeckboxMenu(id, player.getInventory(), new DeckboxContainer(deck)),
                    deck.getHoverName()));
        }
        return InteractionResultHolder.success(deck);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (contents != null) {
            for (ItemStack card : contents.nonEmptyItemsCopy()) {
                counts.merge(card.getHoverName().getString(), 1, Integer::sum);
            }
        }
        if (counts.isEmpty()) {
            tooltip.add(Component.translatable("item.buddybattles.deckbox.empty"));
        } else {
            counts.forEach((name, count) -> tooltip.add(Component.literal(count + " " + name)));
        }
    }

    @Override
    public ItemStack[] getCards(ItemStack deckStack) {
        ItemContainerContents contents = deckStack.get(DataComponents.CONTAINER);
        if (contents == null) return new ItemStack[0];
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack card : contents.nonEmptyItems()) list.add(card);
        return list.toArray(new ItemStack[0]);
    }

    @Override
    public boolean isEmpty(ItemStack deckStack) {
        return getCards(deckStack).length == 0;
    }
}
