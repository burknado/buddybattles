package com.aelux.buddybattles.engine;

import com.aelux.buddybattles.engine.log.BattleComponent;
import com.aelux.buddybattles.session.BattleParticipant;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class BattleBoard {
    public static final Random RANDOM = new Random();
    private static final int HAND_SIZE = 3;
    private static final int BOARD_SIZE = 6;

    public final BattleParticipant p1;
    public final BattleParticipant p2;

    public int health1 = 20;
    public int health2 = 20;
    public int energy1 = 0;
    public int energy2 = 0;
    public int turnEnergy = 1;
    public int turn = 1;
    public boolean isPlayer1Turn;

    public final List<LoggedEvent> log = new ArrayList<>();

    private final ItemStack[] board = new ItemStack[BOARD_SIZE];
    private final ItemStack[] hand1 = new ItemStack[HAND_SIZE];
    private final ItemStack[] hand2 = new ItemStack[HAND_SIZE];
    private final Deque<ItemStack> deck1 = new ArrayDeque<>();
    private final Deque<ItemStack> deck2 = new ArrayDeque<>();

    private final Runnable onSync;
    private final Consumer<Boolean> onGameEnd;

    public BattleBoard(BattleParticipant p1, BattleParticipant p2, Runnable onSync, Consumer<Boolean> onGameEnd) {
        this.p1 = p1;
        this.p2 = p2;
        this.onSync = onSync;
        this.onGameEnd = onGameEnd;

        java.util.Arrays.fill(board, ItemStack.EMPTY);
        java.util.Arrays.fill(hand1, ItemStack.EMPTY);
        java.util.Arrays.fill(hand2, ItemStack.EMPTY);

        Collections.addAll(deck1, p1.getDeck());
        Collections.addAll(deck2, p2.getDeck());
        shuffle(deck1);
        shuffle(deck2);

        // coin flip for who goes first
        this.isPlayer1Turn = RANDOM.nextBoolean();
    }

    private static void shuffle(Deque<ItemStack> deck) {
        List<ItemStack> asList = new ArrayList<>(deck);
        Collections.shuffle(asList, RANDOM);
        deck.clear();
        deck.addAll(asList);
    }

    // --- board ---

    public ItemStack getBoardCard(int slot) {
        return board[slot];
    }

    public void setBoardCard(int slot, ItemStack stack) {
        board[slot] = stack;
    }

    public ItemStack removeBoardCard(int slot) {
        ItemStack removed = board[slot];
        board[slot] = ItemStack.EMPTY;
        return removed;
    }

    // --- hand ---

    private ItemStack[] handFor(boolean p1) {
        return p1 ? hand1 : hand2;
    }

    public ItemStack getHandCard(boolean p1, int index) {
        return handFor(p1)[index];
    }

    public ItemStack[] getHand(boolean p1) {
        return handFor(p1);
    }

    public ItemStack removeHandCard(boolean p1, int index) {
        ItemStack[] hand = handFor(p1);
        ItemStack removed = hand[index];
        hand[index] = ItemStack.EMPTY;
        return removed;
    }

    private int firstFreeHandSlot(boolean p1) {
        ItemStack[] hand = handFor(p1);
        for (int i = 0; i < hand.length; i++) {
            if (hand[i].isEmpty()) return i;
        }
        return -1;
    }

    /** Draws a card from {@code p1}'s deck into their hand, if there's room and cards left. */
    public boolean tryDrawCard(boolean p1) {
        Deque<ItemStack> deck = p1 ? deck1 : deck2;
        int freeSlot = firstFreeHandSlot(p1);
        if (freeSlot == -1 || deck.isEmpty()) return false;
        handFor(p1)[freeSlot] = deck.pop();
        return true;
    }

    /** Puts a specific card into {@code p1}'s hand (e.g. a card returned from the board), if there's room. */
    public boolean tryPutInHand(boolean p1, ItemStack stack) {
        int freeSlot = firstFreeHandSlot(p1);
        if (freeSlot == -1) return false;
        handFor(p1)[freeSlot] = stack;
        return true;
    }

    /** Shuffles a card back into {@code p1}'s deck - e.g. a card that died in battle. */
    public void returnToDeck(boolean p1, ItemStack stack) {
        Deque<ItemStack> deck = p1 ? deck1 : deck2;
        deck.push(stack);
        shuffle(deck);
    }

    public ItemStack removeRandomFromDeck(boolean p1) {
        Deque<ItemStack> deck = p1 ? deck1 : deck2;
        if (deck.isEmpty()) return ItemStack.EMPTY;
        List<ItemStack> asList = new ArrayList<>(deck);
        ItemStack picked = asList.remove(RANDOM.nextInt(asList.size()));
        deck.clear();
        deck.addAll(asList);
        return picked;
    }

    /** Discards a random non-empty card from p1's hand back into their deck. */
    public ItemStack discardRandomFromHand(boolean p1) {
        ItemStack[] hand = handFor(p1);
        List<Integer> nonEmptyIndices = new ArrayList<>();
        for (int i = 0; i < hand.length; i++) {
            if (!hand[i].isEmpty()) nonEmptyIndices.add(i);
        }
        if (nonEmptyIndices.isEmpty()) return ItemStack.EMPTY;

        int index = nonEmptyIndices.get(RANDOM.nextInt(nonEmptyIndices.size()));
        ItemStack discarded = hand[index];
        hand[index] = ItemStack.EMPTY;
        returnToDeck(p1, discarded);
        return discarded;
    }

    public boolean tryTutorCard(boolean p1, java.util.function.Predicate<ItemStack> matcher) {
        Deque<ItemStack> deck = p1 ? deck1 : deck2;
        int freeSlot = firstFreeHandSlot(p1);
        if (freeSlot == -1) return false;
        for (java.util.Iterator<ItemStack> it = deck.iterator(); it.hasNext(); ) {
            ItemStack candidate = it.next();
            if (matcher.test(candidate)) {
                it.remove();
                handFor(p1)[freeSlot] = candidate;
                return true;
            }
        }
        return false;
    }

    public ItemStack removeMatchingFromDeck(boolean p1, java.util.function.Predicate<ItemStack> matcher) {
        Deque<ItemStack> deck = p1 ? deck1 : deck2;
        for (java.util.Iterator<ItemStack> it = deck.iterator(); it.hasNext(); ) {
            ItemStack candidate = it.next();
            if (matcher.test(candidate)) {
                it.remove();
                return candidate;
            }
        }
        return ItemStack.EMPTY;
    }

    // --- energy / turn ---

    public int energy(boolean p1) {
        return p1 ? energy1 : energy2;
    }

    /** Energy never drops below 0, so losing energy you don't have is a no-op. */
    public void spendEnergy(boolean p1, int amount) {
        if (p1) energy1 = Math.max(0, energy1 - amount);
        else energy2 = Math.max(0, energy2 - amount);
    }

    /** Inverse of spendEnergy() - e.g. Lern's "discard a card, gain 1 Energy". */
    public void gainEnergy(boolean p1, int amount) {
        if (p1) energy1 += amount;
        else energy2 += amount;
    }

    // --- identity ---

    public Component getName1() {
        return p1.getDisplayName();
    }

    public Component getName2() {
        return p2.getDisplayName();
    }

    // --- log / sync / end ---

    public void addLog(BattleComponent component) {
        ActingContext ctx = currentActingContext();
        log.add(new LoggedEvent(component, ctx.slot, ctx.abilityId, snapshot()));
    }

    /** Explicit-context variant. */
    public void addLog(BattleComponent component, int sourceSlot, String abilityId) {
        log.add(new LoggedEvent(component, sourceSlot, abilityId, snapshot()));
    }

    // --- snapshotting, for the addLog(...) overloads above ---

    /** Supplies the one piece of state BattleBoard itself doesn't own. */
    public interface StateSupplier {
        int power(int slot);
        int statusOrdinal(int slot);
    }

    private StateSupplier stateSupplier;

    public void setStateSupplier(StateSupplier supplier) {
        this.stateSupplier = supplier;
    }

    private BoardSnapshot snapshot() {
        ItemStack[] boardCopy = new ItemStack[6];
        int[] power = new int[6];
        int[] status = new int[6];
        for (int i = 0; i < 6; i++) {
            boardCopy[i] = getBoardCard(i).copy();
            power[i] = stateSupplier != null ? stateSupplier.power(i) : 0;
            status[i] = stateSupplier != null ? stateSupplier.statusOrdinal(i) : 0;
        }
        ItemStack[] hand1Copy = copyHand(true);
        ItemStack[] hand2Copy = copyHand(false);
        return new BoardSnapshot(health1, health2, energy1, energy2, boardCopy, power, status, hand1Copy, hand2Copy, this.isPlayer1Turn);
    }

    private ItemStack[] copyHand(boolean p1) {
        ItemStack[] source = getHand(p1);
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) copy[i] = source[i].copy();
        return copy;
    }

    // --- acting-context stack, for auto-tagging addLog(component) above ---

    private record ActingContext(int slot, String abilityId) {
        private static final ActingContext NONE = new ActingContext(-1, LoggedEvent.NONE_ID);
    }

    private final Deque<ActingContext> actingContextStack = new ArrayDeque<>();

    /** Push before invoking a specific ability's lambda, pop in a finally block right after. */
    public void pushActingContext(int slot, String abilityId) {
        actingContextStack.push(new ActingContext(slot, abilityId));
    }

    public void popActingContext() {
        if (!actingContextStack.isEmpty()) actingContextStack.pop();
    }

    private ActingContext currentActingContext() {
        return actingContextStack.isEmpty() ? ActingContext.NONE : actingContextStack.peek();
    }

    public void syncData() {
        p1.markDirty();
        p2.markDirty();
        onSync.run();
    }

    public void endGame(boolean p1Won) {
        addLog(new BattleComponent(Component.literal("").append(p1Won ? getName1() : getName2()).append(Component.translatable("battles.log.buddybattles.wins"))));
        onGameEnd.accept(p1Won);
    }
}
