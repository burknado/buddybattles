package com.aelux.buddybattles.client;

import com.aelux.buddybattles.config.BuddyBattlesConfig;
import com.aelux.buddybattles.menu.DeckboxMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class DeckboxScreen extends AbstractContainerScreen<DeckboxMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("buddycards", "textures/gui/deckbox.png");

    public DeckboxScreen(DeckboxMenu container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 150;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 4210752, false);
        graphics.drawString(font, playerInventoryTitle, 8, 56, 4210752, false);
        if (mouseX >= 157 + leftPos && mouseX <= 172 + leftPos && mouseY >= 22 + topPos && mouseY <= 33 + topPos) {
            List<Component> tooltips = List.of(
                    Component.translatable("item.buddybattles.deckbox.deckbuilding_limit"),
                    Component.literal("" + BuddyBattlesConfig.deckLimitCommon.get()).append(Component.translatable("item.buddybattles.deckbox.deckbuilding_limit.common")).withStyle(ChatFormatting.GRAY),
                    Component.literal("" + BuddyBattlesConfig.deckLimitUncommon.get()).append(Component.translatable("item.buddybattles.deckbox.deckbuilding_limit.uncommon")).withStyle(ChatFormatting.GRAY),
                    Component.literal("" + BuddyBattlesConfig.deckLimitRare.get()).append(Component.translatable("item.buddybattles.deckbox.deckbuilding_limit.rare")).withStyle(ChatFormatting.GRAY),
                    Component.literal("" + BuddyBattlesConfig.deckLimitEpic.get()).append(Component.translatable("item.buddybattles.deckbox.deckbuilding_limit.epic")).withStyle(ChatFormatting.GRAY));
            graphics.renderComponentTooltip(font, tooltips, mouseX - leftPos, mouseY - topPos);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
