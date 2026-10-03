package com.aelux.buddybattles.client;

import com.aelux.buddybattles.client.BattleAnimation;
import com.aelux.buddybattles.engine.BattleStatusEffect;
import com.aelux.buddybattles.engine.BoardSnapshot;
import com.aelux.buddybattles.engine.CardBattleStats;
import com.aelux.buddybattles.engine.CardBattleStatsRegistry;
import com.aelux.buddybattles.engine.LoggedEvent;
import com.aelux.buddybattles.engine.log.BuddycardBattleIcon;
import com.aelux.buddybattles.engine.log.IBattleIcon;
import com.aelux.buddybattles.engine.log.TextureBattleIcon;
import com.aelux.buddybattles.menu.BattleMenu;
import com.aelux.buddybattles.network.ActivateCardPayload;
import com.aelux.buddybattles.network.BattleSyncPayload;
import com.aelux.buddybattles.network.EndTurnPayload;
import com.aelux.buddybattles.network.PlayCardPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BattleScreen extends AbstractContainerScreen<BattleMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("buddybattles", "textures/gui/playmat.png");
    private static final ResourceLocation SMALL_FONT = ResourceLocation.fromNamespaceAndPath("buddycards", "smallnumbers");

    private static final BattleAnimation battleAnimator = new BattleAnimation();

    // Column centers and row centers, tuned against the actual in-game render.
    private static final int[] COLUMN_CENTER_X = {72, 98, 124};
    private static final int OPP_HAND_CENTER_Y = 24;
    private static final int RED_FIELD_CENTER_Y = 66;
    private static final int BLUE_FIELD_CENTER_Y = 100;
    private static final int YOUR_HAND_CENTER_Y = 142;
    private static final int CARD_SIZE = 32; // 2x the native 16px item icon

    // Specifies the card hitbox, so that it isn't overlapping squares
    private static final int CARD_HITBOX_WIDTH = 24;
    private static final int CARD_HITBOX_HEIGHT = 32;

    // The turn-indicator/end-turn square next to each hand row.
    private static final int INDICATOR_X = 145;
    private static final int INDICATOR_SIZE = 20;
    private static final int OPP_INDICATOR_Y = 14;
    private static final int YOUR_INDICATOR_Y = 132;

    // The vs-indicator on the left-side of the battle
    private static final int VS_INDICATOR_X = 14;
    private static final int VS_INDICATOR_Y = 73;

    // Source rects (in the same texture) for the 4 indicator states
    private static final int INDICATOR_SRC_X = 176;
    private static final int INDICATOR_SRC_W = 20, INDICATOR_SRC_H = 20;
    private static final int SRC_Y_NEUTRAL_PRESSED = 0;
    private static final int SRC_Y_PLAYER_TURN = 20;
    private static final int SRC_Y_ENEMY_TURN = 40;
    private static final int SRC_Y_NOT_TURN = 60;

    // Log content: icons at their native pixel size (no scaling), wrapped to the panel width and rendered as icon ROWS.
    private static final int ICON_ROW_HEIGHT = 16;

    // Reveals new messages at a speed to match the animations
    private static final long LOG_REVEAL_INTERVAL_MS = BattleAnimation.ANIMATION_DURATION_MS;
    private static final int LOG_TEXT_PADDING = 10; // horizontal margin reserved inside the panel

    // Log margins to cap text and border areas
    private static final int LOG_TEXT_MARGIN_Y = 8;

    private int draggingHandIndex = -1;
    private float scrollPosition = 0;
    private int revealedLineCount = 0;
    private long nextRevealTimeMs = 0L;

    /** -1 until the battle actually ends, then set to the tick it did - see tick(). */
    private long gameEndedAtMs = -1L;
    private static final long AUTO_CLOSE_DELAY_MS = 3000;

    /** True until the user manually scrolls away from the bottom - see getWrappedIconRows(). */
    private boolean followLatestLog = true;

    private int battleScreenWidth = 176;
    private int logPanelWidth = 108;

    public BattleScreen(BattleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = battleScreenWidth + logPanelWidth;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, battleScreenWidth, imageHeight);

        BattleSyncPayload state = ClientBattleState.get();
        if (state != null) getWrappedIconRows(state);
        tickReveal();
        int battleLogLength = visibleLineCount();
        if (battleLogLength > 0) {
            ScrollerData data = ScrollerData.from(this, battleLogLength, imageHeight, ICON_ROW_HEIGHT);
            int panelX = leftPos + battleScreenWidth;
            int scrollbarX = panelX + logPanelWidth;
            int height = 0;

            graphics.blit(TEXTURE, panelX, topPos + height, 0, 166, logPanelWidth, 5);
            if (data.needsScroller()) graphics.blit(TEXTURE, scrollbarX, topPos + height, 108, 166, 10, 5);

            height += 5;
            while (height < data.drawableHeight() + 5) {
                int drawnHeight = Math.min(14, data.drawableHeight() - height + 5);
                graphics.blit(TEXTURE, panelX, topPos + height, 0, 176, logPanelWidth, drawnHeight);
                if (data.needsScroller()) graphics.blit(TEXTURE, scrollbarX, topPos + height, 108, 176, 10, drawnHeight);
                height += drawnHeight;
            }

            graphics.blit(TEXTURE, panelX, topPos + height, 0, 171, logPanelWidth, 5);
            if (data.needsScroller()) {
                graphics.blit(TEXTURE, scrollbarX, topPos + height, 108, 171, 10, 5);
                graphics.blit(TEXTURE, scrollbarX + 3, topPos + (int) scrollPosition + 3, 118, 176, 4, 14);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        BattleSyncPayload state = ClientBattleState.get();
        if (state == null) {
            graphics.drawCenteredString(font, Component.translatable("gui.buddybattles.connecting"), battleScreenWidth / 2, imageHeight / 2, 0xFFFFFF);
            return;
        }

        boolean iAmP1 = state.mySide();
        int oppEnergy = iAmP1 ? displayedSnapshot.energy2() : displayedSnapshot.energy1();
        int oppHealth = iAmP1 ? displayedSnapshot.health2() : displayedSnapshot.health1();
        int myEnergy = iAmP1 ? displayedSnapshot.energy1() : displayedSnapshot.energy2();
        int myHealth = iAmP1 ? displayedSnapshot.health1() : displayedSnapshot.health2();

        // Energy/health text sits next to the sun/heart icons baked into the art, near each hand row.
        graphics.drawString(font, Integer.toString(oppEnergy), 22, 12, 4210752, false);
        graphics.drawString(font, Integer.toString(oppHealth), 22, 29, 4210752, false);
        graphics.drawString(font, Integer.toString(myEnergy), 22, 130, 4210752, false);
        graphics.drawString(font, Integer.toString(myHealth), 22, 147, 4210752, false);

        renderBattleLog(graphics, state, mouseX, mouseY);
        renderCardPowerAndStatus(graphics, state);
    }

    /** Wrapped icon ROWS, cached and updated incrementally. */
    private final List<List<IBattleIcon>> wrappedIconRows = new ArrayList<>();
    /** Parallel to wrappedIconRows - what (if anything) happens when THIS row reveals. */
    private final List<LineEvent> wrappedLineEvents = new ArrayList<>();
    /** Parallel to wrappedIconRows. */
    private final List<Integer> wrappedRowMessageIndex = new ArrayList<>();
    private int wrappedLogRawCount = 0;

    // Records what happens at each line - the animation type, what slot to animate, and a snapshot to reference
    private record LineEvent(String animationType, int attackSlot, BoardSnapshot.PlayerView snapshotToApply) {
        static final LineEvent NONE = new LineEvent("", -1, null);
    }

    /** slot -> when its attack-lunge animation started (ms). Absent = not currently animating. */
    private final Map<Integer, Pair<Long, String>> animationStartMs = new HashMap<>();

    // The board state as seen by the player, not authoritative.
    private BoardSnapshot.PlayerView displayedSnapshot = BoardSnapshot.PlayerView.EMPTY;

    private static List<List<IBattleIcon>> wrapIconsIntoRows(List<IBattleIcon> icons, int wrapWidth) {
        List<List<IBattleIcon>> rows = new ArrayList<>();
        List<IBattleIcon> currentRow = new ArrayList<>();
        int currentWidth = 0;
        for (IBattleIcon icon : icons) {
            if (!currentRow.isEmpty() && currentWidth + icon.width() > wrapWidth) {
                rows.add(currentRow);
                currentRow = new ArrayList<>();
                currentWidth = 0;
            }
            currentRow.add(icon);
            currentWidth += icon.width();
        }
        if (!currentRow.isEmpty()) rows.add(currentRow);
        return rows;
    }

    private List<List<IBattleIcon>> getWrappedIconRows(BattleSyncPayload state) {
        List<String> log = state.recentLog();

        if (log.size() < wrappedLogRawCount) {
            // In case log somehow shrank, just clear it to prevent errors
            wrappedIconRows.clear();
            wrappedLineEvents.clear();
            wrappedRowMessageIndex.clear();
            wrappedLogRawCount = 0;
            displayedSnapshot = BoardSnapshot.PlayerView.EMPTY;
        }

        if (log.size() > wrappedLogRawCount) {
            int wrapWidth = logPanelWidth - LOG_TEXT_PADDING;
            for (int i = wrappedLogRawCount; i < log.size(); i++) {
                List<IBattleIcon> icons = i < state.logIcons().size() ? state.logIcons().get(i) : List.of();
                if (icons.isEmpty()) continue; // nothing to show for this message at all

                // Only the FIRST row of this message gets a real event.
                int sourceSlot = i < state.logSourceSlots().size() ? state.logSourceSlots().get(i) : -1;
                String abilityId = i < state.logAbilityIds().size() ? state.logAbilityIds().get(i) : LoggedEvent.NONE_ID;
                BoardSnapshot.PlayerView snapshotForThisMessage = i < state.logSnapshots().size() ? state.logSnapshots().get(i) : null;
                assert snapshotForThisMessage != null;

                LineEvent firstRowEvent = new LineEvent(abilityId, sourceSlot, snapshotForThisMessage);
                boolean myTurn = snapshotForThisMessage.activePlayer() == state.mySide();

                //Set a spacer to the respective player color
                for (int j = 0; j < icons.size(); j++) {
                    if (icons.get(j).equals(TextureBattleIcon.spacerIcon)) {
                        IBattleIcon useSpacer = myTurn ? TextureBattleIcon.playerSpacerIcon : TextureBattleIcon.opponentSpacerIcon;
                        icons.set(j, useSpacer);
                    } else if (icons.get(j).equals(TextureBattleIcon.drawIcon) && (icons.getFirst().equals(TextureBattleIcon.playerSpacerIcon) || icons.getFirst().equals(TextureBattleIcon.opponentSpacerIcon))){
                        IBattleIcon useDraw = myTurn ? TextureBattleIcon.playerDrawIcon : TextureBattleIcon.opponentDrawIcon;
                        icons.set(j, useDraw);
                    }
                }

                List<List<IBattleIcon>> rows = wrapIconsIntoRows(icons, wrapWidth);
                if (rows.isEmpty()) continue;

                for (int j = 0; j < rows.size(); j++) {
                    wrappedIconRows.add(rows.get(j));
                    wrappedLineEvents.add(j == 0 ? firstRowEvent : LineEvent.NONE);
                    wrappedRowMessageIndex.add(i);
                }
            }
            wrappedLogRawCount = log.size();
        }

        return wrappedIconRows;
    }

    // Reveal one line at a time at a consistent rate
    private void tickReveal() {
        if (revealedLineCount >= wrappedIconRows.size()) return;
        long now = System.currentTimeMillis();
        if (now < nextRevealTimeMs) return;

        int previousRevealedLineCount = revealedLineCount;

        if (followLatestLog) {
            revealNextMessageChunk();
            nextRevealTimeMs = now + LOG_REVEAL_INTERVAL_MS;
            scrollPosition = imageHeight - 20;
            applyRevealedLineEffects(previousRevealedLineCount, revealedLineCount);
            return;
        }

        ScrollerData before = ScrollerData.from(this, visibleLineCount(), imageHeight, ICON_ROW_HEIGHT);
        int absoluteOffsetBefore = before.scrollerOffset(scrollPosition);

        revealNextMessageChunk();
        nextRevealTimeMs = now + LOG_REVEAL_INTERVAL_MS;

        ScrollerData after = ScrollerData.from(this, visibleLineCount(), imageHeight, ICON_ROW_HEIGHT);
        if (after.needsScroller()) {
            float newFraction = (float) absoluteOffsetBefore / (after.requiredHeight() - after.availableHeight());
            scrollPosition = Mth.clamp(newFraction * (after.scrollerPosMax() - after.scrollerPosMin()), 0, after.scrollerPosMax() - after.scrollerPosMin());
        }
        applyRevealedLineEffects(previousRevealedLineCount, revealedLineCount);
    }

    // Advances revealedLineCount with one whole message at a time, not rows.
    private void revealNextMessageChunk() {
        if (revealedLineCount >= wrappedIconRows.size()) return;
        int currentMessageIndex = wrappedRowMessageIndex.get(revealedLineCount);
        do {
            revealedLineCount++;
        } while (revealedLineCount < wrappedIconRows.size()
                && wrappedRowMessageIndex.get(revealedLineCount) == currentMessageIndex);
    }

    // Apply any LineEvents associated with the revealed lines.
    private void applyRevealedLineEffects(int fromIndexInclusive, int toIndexExclusive) {
        for (int i = fromIndexInclusive; i < toIndexExclusive && i < wrappedLineEvents.size(); i++) {
            LineEvent event = wrappedLineEvents.get(i);
            if (event.attackSlot() >= 0) {
                animationStartMs.put(event.attackSlot(), new ImmutablePair<Long, String>(System.currentTimeMillis(), event.animationType));
            }
            if (event.snapshotToApply() != null) {
                displayedSnapshot = event.snapshotToApply();
            }
        }
    }

    private int visibleLineCount() {
        return Math.min(revealedLineCount, wrappedIconRows.size());
    }

    private boolean isCaughtUpOnLog() {
        return visibleLineCount() >= wrappedIconRows.size();
    }

    // Render the battle log to the side of the battle menu
    private void renderBattleLog(GuiGraphics graphics, BattleSyncPayload state, int mouseX, int mouseY) {
        List<List<IBattleIcon>> rows = getWrappedIconRows(state);
        int visible = visibleLineCount();
        if (visible == 0) return;

        ScrollerData data = ScrollerData.from(this, visible, imageHeight, ICON_ROW_HEIGHT);
        int iconX = battleScreenWidth + 4;

        // Scissor to the panel's actual visible rectangle so scrolled rows don't bleed
        int scissorTop = topPos + LOG_TEXT_MARGIN_Y;
        int scissorBottom = topPos + imageHeight - LOG_TEXT_MARGIN_Y;
        graphics.enableScissor(
                leftPos + battleScreenWidth, scissorTop,
                leftPos + battleScreenWidth + logPanelWidth, scissorBottom
        );

        int hoveredMessageIndex = -1;
        int y = LOG_TEXT_MARGIN_Y;
        for (int i = 0; i < visible; i++) {
            List<IBattleIcon> row = rows.get(i);
            int drawY = y - data.scrollerOffset(scrollPosition);

            // Manually shift non-header and non-card rows by 3px to align them properly.
            boolean startsWithCard = !row.isEmpty() && row.get(0) instanceof BuddycardBattleIcon;
            boolean isHeaderRow = !row.isEmpty() && (row.get(0).equals(TextureBattleIcon.spacerIcon) || row.get(0).equals(TextureBattleIcon.invisibleSpacerIcon));
            int rowShiftX = (!startsWithCard && !isHeaderRow) ? 3 : 0;

            int x = iconX + rowShiftX;
            for (IBattleIcon icon : row) {
                // Vertically center each icon within the row's height budget
                int iconY = drawY + (ICON_ROW_HEIGHT - icon.height()) / 2;
                renderBattleIcon(graphics, icon, x, iconY);
                x += icon.width();
            }

            // Hover-hit-test in absolute screen coordinates
            int absoluteTop = topPos + drawY;
            int absoluteBottom = absoluteTop + ICON_ROW_HEIGHT;
            if (mouseX >= leftPos + iconX + rowShiftX && mouseX < leftPos + x
                    && mouseY >= Math.max(absoluteTop, scissorTop) && mouseY < Math.min(absoluteBottom, scissorBottom)) {
                hoveredMessageIndex = wrappedRowMessageIndex.get(i);
            }
            y += ICON_ROW_HEIGHT;
        }

        graphics.disableScissor();

        // Not rendering a tooltip here, but saving it to be displayed in render() later.
        hoveredLogMessageIndex = (hoveredMessageIndex >= 0 && hoveredMessageIndex < state.recentLog().size()) ? hoveredMessageIndex : -1;
    }

    /** -1 when nothing's hovered - set by renderBattleLog, consumed by renderHoveredLogTooltip. */
    private int hoveredLogMessageIndex = -1;

    private void renderHoveredLogTooltip(GuiGraphics graphics, BattleSyncPayload state, int mouseX, int mouseY) {
        if (hoveredLogMessageIndex < 0 || hoveredLogMessageIndex >= state.recentLog().size()) return;
        String text = state.recentLog().get(hoveredLogMessageIndex);
        if (text == null || text.isBlank()) return;
        List<FormattedCharSequence> tooltipLines = font.split(Component.literal(text), 200);
        graphics.renderTooltip(font, tooltipLines, mouseX, mouseY);
    }

    // Render the actual battle icons, power, etc.
    private void renderBattleIcon(GuiGraphics graphics, IBattleIcon icon, int x, int y) {
        if (icon instanceof TextureBattleIcon t) {
            // We skip drawing invisibleSpacerIcon, only using it for centering
            if (!t.equals(TextureBattleIcon.invisibleSpacerIcon)) {
                graphics.blit(TextureBattleIcon.TEXTURE, x, y, t.texturePosX(), t.texturePosY(), t.width(), 12);
            }
            for (TextureBattleIcon.BattleInfo info : t.info()) {
                int textX = x + info.x();
                if (!info.isLeftAligned()) textX -= font.width(info.display()) / 2;
                graphics.drawString(font, info.display(), textX, y + info.y(), info.color(), false);
            }
        } else if (icon instanceof BuddycardBattleIcon b) {
            ItemStack stack = new ItemStack(b.getItem());
            if (stack.isEmpty()) return;
            float scale = icon.width() / 16f;
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1f);
            graphics.renderItem(stack, 0, 0);
            graphics.pose().popPose();
        }
    }

    private void renderCardPowerAndStatus(GuiGraphics graphics, BattleSyncPayload state) {
        boolean iAmP1 = state.mySide();
        ItemStack[] board = displayedSnapshot.board(); // what the player has actually seen, not the latest sync
        int[] oppSlots = iAmP1 ? new int[]{3, 4, 5} : new int[]{0, 1, 2};
        int[] mySlots = iAmP1 ? new int[]{0, 1, 2} : new int[]{3, 4, 5};

        renderRow(graphics, board, oppSlots, RED_FIELD_CENTER_Y);
        renderRow(graphics, board, mySlots, BLUE_FIELD_CENTER_Y);
    }

    private void renderRow(GuiGraphics graphics, ItemStack[] board, int[] slotsForRow, int rowCenterY) {
        for (int col = 0; col < 3; col++) {
            int absoluteSlot = slotsForRow[col];
            ItemStack stack = board[absoluteSlot];
            if (stack.isEmpty()) continue;

            // Clamped for DISPLAY only, not touching the actual value
            int power = Math.max(0, displayedSnapshot.power()[absoluteSlot]);
            Optional<CardBattleStats> stats = CardBattleStatsRegistry.get(stack);
            ChatFormatting color;
            if (power == 0) color = ChatFormatting.RED;
            else if (stats.isPresent() && power >= stats.get().power()) color = ChatFormatting.GREEN;
            else color = ChatFormatting.YELLOW;

            int cx = COLUMN_CENTER_X[col];
            MutableComponent powerText = Component.literal(Integer.toString(power)).withStyle(style -> style.withFont(SMALL_FONT));
            graphics.drawString(font, powerText, cx + CARD_SIZE / 2 - 7, rowCenterY + CARD_SIZE / 2 - 6, color.getColor() == null ? 0xFFFFFF : color.getColor());

            int statusOrdinal = displayedSnapshot.status()[absoluteSlot];
            if (statusOrdinal != BattleStatusEffect.EMPTY.ordinal()) {
                BattleStatusEffect status = BattleStatusEffect.values()[statusOrdinal];
                MutableComponent statusText = Component.literal(Character.toString('\u0ED0' + status.ordinal())).withStyle(style -> style.withFont(SMALL_FONT));
                graphics.drawString(font, statusText, cx + CARD_SIZE / 2 - 7, rowCenterY - CARD_SIZE / 2, status.getColor());
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        BattleSyncPayload state = ClientBattleState.get();
        if (state != null) {
            renderHandAndBoardItems(graphics, state);
            renderDraggedCard(graphics, state, mouseX, mouseY);
            renderTurnIndicators(graphics, state);
            renderClickableHighlights(graphics, state, mouseX, mouseY);
            renderHoveredCardTooltip(graphics, state, mouseX, mouseY);
            renderHoveredLogTooltip(graphics, state, mouseX, mouseY);
        }
        tickAutoClose(state);
        renderTooltip(graphics, mouseX, mouseY);
    }

    // The dragged card follows the cursor every frame, like vanilla item dragging.
    private void renderDraggedCard(GuiGraphics graphics, BattleSyncPayload state, int mouseX, int mouseY) {
        if (draggingHandIndex < 0) return;
        ItemStack[] hand = displayedSnapshot.myHand();
        if (draggingHandIndex >= hand.length) return;
        renderItem2x(graphics, hand[draggingHandIndex], mouseX, mouseY);
    }

    private void renderHandAndBoardItems(GuiGraphics graphics, BattleSyncPayload state) {
        boolean iAmP1 = state.mySide();
        ItemStack[] board = displayedSnapshot.board(); // what the player has actually seen, not the latest sync
        int[] oppSlots = iAmP1 ? new int[]{3, 4, 5} : new int[]{0, 1, 2};
        int[] mySlots = iAmP1 ? new int[]{0, 1, 2} : new int[]{3, 4, 5};
        boolean fullBoard = true;

        for (int col = 0; col < 3; col++) {
            renderItem2x(graphics, board[oppSlots[col]], leftPos + COLUMN_CENTER_X[col], topPos + RED_FIELD_CENTER_Y, oppSlots[col]);
            renderItem2x(graphics, board[mySlots[col]], leftPos + COLUMN_CENTER_X[col], topPos + BLUE_FIELD_CENTER_Y, mySlots[col]);
            if (board[mySlots[col]].isEmpty()){
                fullBoard = false;
            }
        }
        ItemStack[] hand = displayedSnapshot.myHand(); // what the player has actually seen, not the latest sync
        boolean myTurn = displayedSnapshot.activePlayer() == state.mySide();
        for (int i = 0; i < 3; i++) {
            if (i == draggingHandIndex) continue;
            renderItem2x(graphics, hand[i], leftPos + COLUMN_CENTER_X[i], topPos + YOUR_HAND_CENTER_Y);
            if (!hand[i].isEmpty() && (isUnaffordable(hand[i], iAmP1) || !isCaughtUpOnLog() || !myTurn || fullBoard)) {
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, 200);
                fillCardArt(graphics, leftPos + COLUMN_CENTER_X[i], topPos + YOUR_HAND_CENTER_Y, 0x99000000);
                graphics.pose().popPose();
            }
        }
        for (int i = 0; i < displayedSnapshot.opponentHandCount() && i < 3; i++) {
            renderFaceDownPlaceholder(graphics, leftPos + COLUMN_CENTER_X[i], topPos + OPP_HAND_CENTER_Y);
        }
    }

    // Render face-down placeholders for opponent cards (since we cannot transmit what they are)
    private void renderFaceDownPlaceholder(GuiGraphics graphics, int centerX, int centerY) {
        graphics.blit(TEXTURE, centerX - 8, centerY - 12, 176, 120, 16, 24);
    }

    /** No animation targeting - used for hand cards, which can't be attacking. */
    private void renderItem2x(GuiGraphics graphics, ItemStack stack, int centerX, int centerY) {
        renderItem2x(graphics, stack, centerX, centerY, -1);
    }

    // Render a card at higher scale and animate it if applicable.
    private void renderItem2x(GuiGraphics graphics, ItemStack stack, int centerX, int centerY, int absoluteSlot) {
        if (stack.isEmpty()) return;
        int direction = centerY > topPos + RED_FIELD_CENTER_Y ? -1 : 1;
        Pair<Integer, Integer> animCoords = battleAnimator.getAttackOffset(animationStartMs, absoluteSlot, direction);
        int animatedCenterX = centerX + animCoords.getLeft();
        int animatedCenterY = centerY + animCoords.getRight();
        graphics.pose().pushPose();
        graphics.pose().translate(animatedCenterX - CARD_SIZE / 2f, animatedCenterY - CARD_SIZE / 2f, 0);
        graphics.pose().scale(2f, 2f, 1f);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();
    }

    // Render the turn indicators and End Turn buttons to show who is moving
    private void renderTurnIndicators(GuiGraphics graphics, BattleSyncPayload state) {
        boolean myTurn = displayedSnapshot.activePlayer() == state.mySide();

        int oppSrcY = myTurn ? SRC_Y_NOT_TURN : SRC_Y_ENEMY_TURN;
        graphics.blit(TEXTURE, leftPos + INDICATOR_X, topPos + OPP_INDICATOR_Y, INDICATOR_SRC_X, oppSrcY, INDICATOR_SRC_W, INDICATOR_SRC_H);

        int mySrcY = myTurn ? (!isCaughtUpOnLog() ? SRC_Y_NEUTRAL_PRESSED : SRC_Y_PLAYER_TURN) : SRC_Y_NOT_TURN;
        graphics.blit(TEXTURE, leftPos + INDICATOR_X, topPos + YOUR_INDICATOR_Y, INDICATOR_SRC_X, mySrcY, INDICATOR_SRC_W, INDICATOR_SRC_H);

        int srcY = myTurn ? 100 : 80;
        graphics.blit(TEXTURE, leftPos + VS_INDICATOR_X, topPos + VS_INDICATOR_Y, battleScreenWidth, srcY, 26, 20);
    }

    private void renderClickableHighlights(GuiGraphics graphics, BattleSyncPayload state, int mouseX, int mouseY) {
        boolean iAmP1 = state.mySide();
        boolean myTurn = displayedSnapshot.activePlayer() == iAmP1;
        if (!myTurn || !isCaughtUpOnLog()) return;

        ItemStack[] board = displayedSnapshot.board(); // what the player has actually seen, not the latest sync
        int[] mySlots = iAmP1 ? new int[]{0, 1, 2} : new int[]{3, 4, 5};
        ItemStack[] hand = displayedSnapshot.myHand(); // what the player has actually seen, not the latest sync

        for (int i = 0; i < 3; i++) {
            if (!hand[i].isEmpty() && !isUnaffordable(hand[i], iAmP1) && withinCard(mouseX, mouseY, leftPos + COLUMN_CENTER_X[i], topPos + YOUR_HAND_CENTER_Y)) {
                fillCard(graphics, leftPos + COLUMN_CENTER_X[i], topPos + YOUR_HAND_CENTER_Y, 0x55FFFFFF);
            }
        }
        for (int col = 0; col < 3; col++) {
            int cx = leftPos + COLUMN_CENTER_X[col];
            int cy = topPos + BLUE_FIELD_CENTER_Y;
            if (!withinCard(mouseX, mouseY, cx, cy)) continue;
            boolean emptySlot = board[mySlots[col]].isEmpty();
            if (draggingHandIndex >= 0 && emptySlot) {
                fillCard(graphics, cx, cy, 0x77FFFF55); // valid drop target
            } else if (!emptySlot) {
                fillCard(graphics, cx, cy, 0x55FFFFFF); // activatable card
            }
        }
    }

    private void fillCard(GuiGraphics graphics, int centerX, int centerY, int argb) {
        int halfW = CARD_HITBOX_WIDTH / 2;
        int halfH = CARD_HITBOX_HEIGHT / 2;
        graphics.fill(centerX - halfW, centerY - halfH, centerX + halfW, centerY + halfH, argb);
    }

    // Greys out cards which cannot be used at the moment.
    private void fillCardArt(GuiGraphics graphics, int centerX, int centerY, int argb) {
        int halfW = (CARD_HITBOX_WIDTH - 8) / 2;
        int halfH = (CARD_HITBOX_HEIGHT - 8) / 2;
        graphics.fill(centerX - halfW, centerY - halfH, centerX + halfW, centerY + halfH, argb);
    }

    // Checks if the player can afford to use a card (from their current snapshot).
    private boolean isUnaffordable(ItemStack card, boolean iAmP1) {
        if (card.isEmpty()) return false;
        int cost = CardBattleStatsRegistry.get(card).map(CardBattleStats::cost).orElse(0);
        int myEnergy = iAmP1 ? displayedSnapshot.energy1() : displayedSnapshot.energy2();
        return cost > myEnergy;
    }

    private void renderHoveredCardTooltip(GuiGraphics graphics, BattleSyncPayload state, int mouseX, int mouseY) {
        boolean iAmP1 = state.mySide();
        ItemStack[] board = displayedSnapshot.board(); // what the player has actually seen, not the latest sync
        int[] oppSlots = iAmP1 ? new int[]{3, 4, 5} : new int[]{0, 1, 2};
        int[] mySlots = iAmP1 ? new int[]{0, 1, 2} : new int[]{3, 4, 5};

        for (int col = 0; col < 3; col++) {
            if (withinCard(mouseX, mouseY, leftPos + COLUMN_CENTER_X[col], topPos + RED_FIELD_CENTER_Y)) {
                renderCardTooltipIfPresent(graphics, board[oppSlots[col]], mouseX, mouseY);
                return;
            }
            if (withinCard(mouseX, mouseY, leftPos + COLUMN_CENTER_X[col], topPos + BLUE_FIELD_CENTER_Y)) {
                renderCardTooltipIfPresent(graphics, board[mySlots[col]], mouseX, mouseY);
                return;
            }
        }
        ItemStack[] hand = displayedSnapshot.myHand(); // what the player has actually seen, not the latest sync
        for (int i = 0; i < 3; i++) {
            if (withinCard(mouseX, mouseY, leftPos + COLUMN_CENTER_X[i], topPos + YOUR_HAND_CENTER_Y)) {
                renderCardTooltipIfPresent(graphics, hand[i], mouseX, mouseY);
                return;
            }
        }
    }

    private void renderCardTooltipIfPresent(GuiGraphics graphics, ItemStack stack, int mouseX, int mouseY) {
        if (!stack.isEmpty()) graphics.renderTooltip(font, stack, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (updateScrollPosition(mouseX, mouseY, button)) return true;

        BattleSyncPayload state = ClientBattleState.get();
        if (state != null) {
            boolean iAmP1 = state.mySide();
            boolean myTurn = displayedSnapshot.activePlayer() == iAmP1;
            int[] mySlots = iAmP1 ? new int[]{0, 1, 2} : new int[]{3, 4, 5};
            ItemStack[] hand = displayedSnapshot.myHand();

            // Skip everything unless its their turn and their log has caught up
            if (myTurn && isCaughtUpOnLog()) {
                if (withinRect(mouseX, mouseY, leftPos + INDICATOR_X, topPos + YOUR_INDICATOR_Y, INDICATOR_SIZE, INDICATOR_SIZE)) {
                    PacketDistributor.sendToServer(EndTurnPayload.INSTANCE);
                    return true;
                }

                // Can drag affordable cards
                for (int i = 0; i < 3; i++) {
                    if (!hand[i].isEmpty() && withinCard(mouseX, mouseY, leftPos + COLUMN_CENTER_X[i], topPos + YOUR_HAND_CENTER_Y)) {
                        // Can't afford it - consume the click
                        if (isUnaffordable(hand[i], iAmP1)) return true;
                        draggingHandIndex = i;
                        return true;
                    }
                }
                // Board slots only ever activate on a click
                for (int col = 0; col < 3; col++) {
                    int slotIndex = mySlots[col];
                    if (withinCard(mouseX, mouseY, leftPos + COLUMN_CENTER_X[col], topPos + BLUE_FIELD_CENTER_Y)) {
                        PacketDistributor.sendToServer(new ActivateCardPayload(slotIndex));
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingHandIndex >= 0) {
            int handIndex = draggingHandIndex;
            draggingHandIndex = -1;

            BattleSyncPayload state = ClientBattleState.get();
            if (state != null) {
                boolean iAmP1 = state.mySide();
                int[] mySlots = iAmP1 ? new int[]{0, 1, 2} : new int[]{3, 4, 5};
                for (int col = 0; col < 3; col++) {
                    if (withinCard(mouseX, mouseY, leftPos + COLUMN_CENTER_X[col], topPos + BLUE_FIELD_CENTER_Y)) {
                        PacketDistributor.sendToServer(new PlayCardPayload(handIndex, mySlots[col]));
                        break;
                    }
                }
                // Released anywhere else.
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean withinCard(double mouseX, double mouseY, int centerX, int centerY) {
        int halfW = CARD_HITBOX_WIDTH / 2;
        int halfH = CARD_HITBOX_HEIGHT / 2;
        return mouseX >= centerX - halfW && mouseX < centerX + halfW && mouseY >= centerY - halfH && mouseY < centerY + halfH;
    }

    private boolean withinRect(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dragX, double dragY) {
        // A card drag in progress "consumes" the whole gesture
        if (draggingHandIndex >= 0) return true;
        if (updateScrollPosition(x, y, button)) return true;
        return super.mouseDragged(x, y, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        BattleSyncPayload state = ClientBattleState.get();
        if (state != null) getWrappedIconRows(state);
        int logSize = visibleLineCount();
        ScrollerData data = ScrollerData.from(this, logSize, imageHeight, ICON_ROW_HEIGHT);
        if (data.needsScroller() && x >= leftPos + battleScreenWidth && x <= leftPos + battleScreenWidth + 123 && y >= topPos && y <= topPos + imageHeight) {
            float scrollRatio = (data.scrollerPosMax - data.scrollerPosMin) / ((data.requiredHeight - data.availableHeight) * .065f);
            scrollPosition = Mth.clamp(scrollPosition - (float) scrollY * scrollRatio, 0, data.scrollerPosMax - data.scrollerPosMin);
            followLatestLog = scrollPosition >= imageHeight - 20.5f;
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    private boolean updateScrollPosition(double x, double y, int button) {
        BattleSyncPayload state = ClientBattleState.get();
        if (state != null) getWrappedIconRows(state);
        int logSize = visibleLineCount();
        ScrollerData data = ScrollerData.from(this, logSize, imageHeight, ICON_ROW_HEIGHT);
        if (button == 0 && data.needsScroller() && x >= leftPos + battleScreenWidth + 103 && x <= leftPos + battleScreenWidth + 123 && y >= topPos && y <= topPos + imageHeight) {
            scrollPosition = Mth.clamp((float) y - data.scrollerPosMin, 0, data.scrollerPosMax - data.scrollerPosMin);
            followLatestLog = scrollPosition >= imageHeight - 20.5f;
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
        ClientBattleState.clear();
    }

    // Auto-closes game when its over
    private void tickAutoClose(BattleSyncPayload state) {
        if (state == null || !state.gameEnded()) return;

        if (gameEndedAtMs < 0 || !isCaughtUpOnLog()) {
            gameEndedAtMs = System.currentTimeMillis();
        } else if (System.currentTimeMillis() - gameEndedAtMs >= AUTO_CLOSE_DELAY_MS) {
            onClose();
        }
    }

    private record ScrollerData(int requiredHeight, int availableHeight, int borderFillHeight, int scrollerPosMin, int scrollerPosMax) {
        private static ScrollerData from(BattleScreen screen, int lineCount, int imageHeight, int lineHeight) {
            int required = lineCount == 0 ? 0 : LOG_TEXT_MARGIN_Y + lineCount * lineHeight;
            int borderFill = imageHeight - 10;
            return new ScrollerData(required, imageHeight - 2 * LOG_TEXT_MARGIN_Y, borderFill, screen.topPos + 10, screen.topPos + imageHeight - 10);
        }

        private int drawableHeight() {
            return Math.min(borderFillHeight, requiredHeight);
        }

        private boolean needsScroller() {
            return requiredHeight > availableHeight;
        }

        private int scrollerOffset(float scrollerPos) {
            if (!needsScroller()) return 0;
            float offset = scrollerPos / (scrollerPosMax - scrollerPosMin);
            return (int) ((requiredHeight - availableHeight) * offset);
        }
    }
}
