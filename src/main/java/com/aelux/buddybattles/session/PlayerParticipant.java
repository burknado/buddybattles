package com.aelux.buddybattles.session;

import com.aelux.buddybattles.deck.DeckSource;
import com.aelux.buddybattles.engine.BoardSnapshot;
import com.aelux.buddybattles.engine.LoggedEvent;
import com.aelux.buddybattles.engine.log.IBattleIcon;
import com.aelux.buddybattles.menu.BattleMenu;
import com.aelux.buddybattles.network.BattleSyncPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** Wraps a real player's side of a battle. */
public class PlayerParticipant implements BattleParticipant {
    private final ServerPlayer player;
    private final ItemStack deckStack;

    /** Set via attachSession() right after BattleSession's constructor builds the board/game. */
    private BattleSession session;

    public PlayerParticipant(ServerPlayer player, ItemStack deckStack) {
        this.player = player;
        this.deckStack = deckStack;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    @Override
    public void attachSession(BattleSession session) {
        this.session = session;
    }

    /** Opens the battle screen for this player. Call once, ideally before beginGame() so the first sync isn't missed. */
    public void openBattleScreen() {
        player.openMenu(new SimpleMenuProvider(
                (windowId, inv, p) -> new BattleMenu(windowId, p),
                Component.translatable("menu.buddybattles.battle")));
    }

    @Override
    public UUID getParticipantId() {
        return player.getUUID();
    }

    @Override
    public Component getDisplayName() {
        return player.getDisplayName();
    }

    @Override
    public boolean isAI() {
        return false;
    }

    @Override
    public void markDirty() {
        if (session == null) return;
        PacketDistributor.sendToPlayer(player, buildSyncPayload(session.isEnded(), false, false));
    }

    @Override
    public void onBattleEnded(boolean won, boolean interrupted) {
        if (interrupted) {
            player.displayClientMessage(Component.translatable("misc.buddybattles.duel_interrupted"), true);
        } else {
            player.displayClientMessage(Component.translatable(won ? "misc.buddybattles.duel_won" : "misc.buddybattles.duel_lost"), true);
        }
        if (session != null) {
            PacketDistributor.sendToPlayer(player, buildSyncPayload(true, won, interrupted));
        }
    }

    private BattleSyncPayload buildSyncPayload(boolean gameEnded, boolean wonBySelf, boolean interrupted) {
        boolean mySide = session.p1 == this;
        var board = session.board;
        var game = session.game;

        ItemStack[] hand = board.getHand(mySide);
        ItemStack[] opponentHand = board.getHand(!mySide);
        int opponentHandCount = (int) java.util.Arrays.stream(opponentHand).filter(s -> !s.isEmpty()).count();
        ItemStack[] cards = new ItemStack[6];
        int[] boardPower = new int[6];
        int[] boardStatus = new int[6];
        for (int i = 0; i < 6; i++) {
            cards[i] = board.getBoardCard(i);
            boardPower[i] = game.state[i].power;
            boardStatus[i] = game.state[i].status.ordinal();
        }

        // No cap - the whole match log is sent every sync.
        List<String> recentLog = board.log.stream()
                .map(e -> e.component().getHoverText().getString())
                .collect(Collectors.toList());
        List<Integer> logSourceSlots = board.log.stream()
                .map(LoggedEvent::sourceSlot)
                .collect(Collectors.toList());
        List<String> logAbilityIds = board.log.stream()
                .map(LoggedEvent::abilityId)
                .collect(Collectors.toList());
        List<BoardSnapshot.PlayerView> logSnapshots = board.log.stream()
                .map(LoggedEvent::snapshot)
                .map(snapshot -> snapshot.toPlayerView(mySide))
                .collect(Collectors.toList());
        List<List<IBattleIcon>> logIcons = board.log.stream()
                .map(e -> e.component().getBattleIcons())
                .collect(Collectors.toList());

        return new BattleSyncPayload(
                board.health1, board.health2,
                board.energy1, board.energy2,
                board.turnEnergy, board.turn,
                board.isPlayer1Turn,
                mySide,
                hand[0], hand[1], hand[2],
                cards[0], cards[1], cards[2], cards[3], cards[4], cards[5],
                boardPower, boardStatus, opponentHandCount,
                recentLog, logSourceSlots, logAbilityIds, logSnapshots, logIcons,
                gameEnded, wonBySelf, interrupted
        );
    }

    @Override
    public ItemStack[] getDeck() {
        if (deckStack.getItem() instanceof DeckSource deckSource) {
            return deckSource.getCards(deckStack);
        }
        return new ItemStack[0];
    }
}
