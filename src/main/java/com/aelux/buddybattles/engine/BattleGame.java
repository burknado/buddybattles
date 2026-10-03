package com.aelux.buddybattles.engine;

import com.mojang.logging.LogUtils;
import com.aelux.buddybattles.engine.log.BattleComponent;
import com.aelux.buddybattles.engine.log.BuddycardBattleIcon;
import com.aelux.buddybattles.engine.log.TextureBattleIcon;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Function;

public class BattleGame {
    private static final Logger LOGGER = LogUtils.getLogger();

    public BattleBoard board;
    private boolean gameEnded = false;
    private List<BattleAttack> savedAttacks = new ArrayList<>();
    /** Attackers whose own power may have dropped to 0 during their fight (e.g. Bumblina). */
    private List<Integer> selfDeathChecks = new ArrayList<>();
    public final BattleCardState[] state = new BattleCardState[6];
    /** Actively changing turn power. */
    public final int[] turnPower = new int[6];
    private final Map<Integer, List<String>> runEvents = new HashMap<>();

    private int loopDetector = 0;
    private boolean sentInfLoopMsg = false;

    public BattleGame(BattleBoard board) {
        this.board = board;
        for (int i = 0; i < 6; i++) state[i] = new BattleCardState(0, BattleStatusEffect.EMPTY);
    }

    public boolean hasGameEnded() {
        return gameEnded;
    }

    /**
     * Ends the game if either player is at 0 health. Call after any action that can deal damage.
     * Returns true if the game is over; callers must not sync afterwards, since ending sends the final sync.
     */
    public boolean checkGameOver() {
        if (gameEnded) return true;
        if (board.health1 <= 0 || board.health2 <= 0) {
            gameEnded = true;
            board.endGame(board.health2 <= 0 && board.health1 > 0);
            return true;
        }
        return false;
    }

    /** Fires a card's ACTIVATED ability, then checks for game over. */
    public void activate(int slot) {
        trigger(BattleEvent.ACTIVATED, slot);
        checkGameOver();
    }

    /** begins the game. does NOT reset state, make a new BattleGame for that */
    public void beginGame() {
        LOGGER.info("Game Start");
        board.tryDrawCard(true);
        board.tryDrawCard(true);
        board.tryDrawCard(false);
        board.tryDrawCard(false);
        board.addLog(new BattleComponent(Component.translatable("battles.log.buddycards.starting_draw"), List.of(TextureBattleIcon.drawIcon, TextureBattleIcon.drawIcon, TextureBattleIcon.drawIcon, TextureBattleIcon.drawIcon)));
        for (int i = 0; i < 6; i++) state[i] = new BattleCardState(0, BattleStatusEffect.EMPTY);
        startTurn();
    }

    /** starts a turn */
    public void startTurn() {
        LOGGER.info("Player " + player() + "'s turn!");
        loopDetector = 0;
        sentInfLoopMsg = false;
        if (isP1()) board.energy1 = Math.min(board.energy1 + board.turnEnergy, 10);
        else board.energy2 = Math.min(board.energy2 + board.turnEnergy, 10);
        LOGGER.info("Player " + player() + " gained " + board.turnEnergy + " energy!");
        if (board.tryDrawCard(isP1()))
            board.addLog(new BattleComponent(Component.literal("").append(isP1() ? board.getName1() : board.getName2()).append(Component.translatable("battles.log.buddycards.turn_draw")), List.of(TextureBattleIcon.spacerIcon, TextureBattleIcon.drawIcon, TextureBattleIcon.spacerIcon)));
        else
            board.addLog(new BattleComponent(Component.literal("").append(isP1() ? board.getName1() : board.getName2()).append(Component.translatable("battles.log.buddycards.turn_fail_draw")), List.of(TextureBattleIcon.spacerIcon, TextureBattleIcon.drawIcon, TextureBattleIcon.spacerIcon)));
        board.addLog(new BattleComponent(Component.literal("").append(isP1() ? board.getName1() : board.getName2()).append(Component.translatable("battles.log.buddycards.turn_energy1")).append("" + board.turnEnergy).append(Component.translatable("battles.log.buddycards.turn_energy2")), List.of(TextureBattleIcon.energyIcon(board.turnEnergy))));
        // copy power values to turn power (must be done here before events have a chance to fire)
        for (int i = 0; i < 6; i++) turnPower[i] = state[i].power;
        // send turn event to all your cards
        for (int i = slot(0); i < slot(3); i++) {
            trigger(BattleEvent.TURN, i);
        }
        board.syncData();
    }

    /** ends a turn */
    public boolean endTurn() {
        // Only announce the attack phase if the acting player actually has a buddy on the board to attack with.
        boolean hasBuddies = false;
        for (int i = slot(0); i < slot(3); i++) {
            if (!board.getBoardCard(i).isEmpty()) {
                hasBuddies = true;
                break;
            }
        }
        if (hasBuddies) {
            board.addLog(new BattleComponent(Component.literal("").append(isP1() ? board.getName1() : board.getName2()).append(Component.translatable("battles.log.buddycards.turn_attack")), List.of(TextureBattleIcon.invisibleSpacerIcon, TextureBattleIcon.startAttackIcon, TextureBattleIcon.invisibleSpacerIcon)));
        }
        loopDetector = 0;
        sentInfLoopMsg = false;
        // have all your cards attack
        for (int i = slot(0); i < slot(3); i++) {
            if (!board.getBoardCard(i).isEmpty()) {
                int opposite = opposite(i);
                attack(opposite, i);
                if (!board.getBoardCard(opposite).isEmpty()) {
                    // retaliate
                    attack(i, opposite);
                }
            }
        }
        // finalize turn power values but they may be changed in the kill/death events
        for (int i = 0; i < 6; i++) state[i].power = turnPower[i];
        // card killing
        int[] marked = new int[6];
        for (BattleAttack atk : savedAttacks) {
            int target = atk.target();
            int source = atk.source();
            if (board.getBoardCard(target).isEmpty() || state[target].power > 0) continue;
            // Two cards that knocked each other out are resolved together below, so both get KILL credit.
            if (target == opposite(source) && state[source].power <= 0
                    && savedAttacks.contains(new BattleAttack(source, target))) {
                if (marked[target] > 0) marked[source] = 2;
                else marked[source] = 1;
                continue;
            }
            LOGGER.info(board.getBoardCard(source) + " killed " + board.getBoardCard(target));
            if (!trigger(BattleEvent.KILL, source, target, source)) continue;
            if (!trigger(BattleEvent.DEATH, target, target, source)) continue;
            if (!trigger(BattleEvent.OBSERVE_DEATH, source, target, source, BattleEvent.Distribution.ALL)) continue;
            logAndRemove(target);
        }
        for (int i = 0; i < 3; i++) {
            if (marked[i] > 0 && marked[i + 3] > 0) {
                int target = marked[i] == 1 ? i + 3 : i;
                int source = marked[i] == 1 ? i : i + 3;

                boolean killTarget = trigger(BattleEvent.KILL, source, target, source);
                killTarget &= turnPower[target] <= 0;
                killTarget = killTarget && trigger(BattleEvent.DEATH, target, target, source) && trigger(BattleEvent.OBSERVE_DEATH, source, target, source, BattleEvent.Distribution.ALL);
                boolean killSource = trigger(BattleEvent.KILL, target, source, target);
                killSource &= turnPower[source] <= 0;
                killSource = killSource && trigger(BattleEvent.DEATH, source, source, target) && trigger(BattleEvent.OBSERVE_DEATH, source, target, source, BattleEvent.Distribution.ALL);

                if (killTarget && !board.getBoardCard(target).isEmpty()) {
                    LOGGER.info(board.getBoardCard(source) + " killed " + board.getBoardCard(target));
                    logAndRemove(target);
                }
                if (killSource && !board.getBoardCard(source).isEmpty()) {
                    LOGGER.info(board.getBoardCard(target) + " killed " + board.getBoardCard(source));
                    logAndRemove(source);
                }
            }
        }
        // After all real kills, so the killer still gets KILL credit. No KILL fires here.
        for (int slot : selfDeathChecks) {
            if (!board.getBoardCard(slot).isEmpty() && state[slot].power <= 0
                    && trigger(BattleEvent.DEATH, slot, slot, slot)
                    && trigger(BattleEvent.OBSERVE_DEATH, slot, slot, slot, BattleEvent.Distribution.ALL)) {
                logAndRemove(slot);
            }
        }
        savedAttacks = new ArrayList<>();
        selfDeathChecks = new ArrayList<>();
        for (int i = 0; i < 6; i++) state[i].power = turnPower[i];
        if (checkGameOver()) return false;
        board.syncData();
        return true;
    }

    /** updates the power of all cards */
    public void updatePower() {
        for (int i = 0; i < 6; i++) updatePower(i);
    }

    /** updates the power of a specific card */
    public void updatePower(int i) {
        state[i].power = turnPower[i];
        if (state[i].power > 0) return;
        if (!trigger(BattleEvent.DEATH, i, i, i)) return;
        if (!trigger(BattleEvent.OBSERVE_DEATH, i, i, i, BattleEvent.Distribution.ALL)) return;
        logAndRemove(i);
    }

    /** initiates a turn switch and starts the next turn */
    public void nextTurn() {
        board.isPlayer1Turn = !board.isPlayer1Turn;
        if (board.turnEnergy < 5) ++board.turnEnergy;
        ++board.turn;
        startTurn();
        runEvents.clear();
        checkGameOver();
    }

    private void reduceHP(boolean p1, int damage) {
        if (p1) {
            board.health1 = Math.max(0, board.health1 - damage);
        } else {
            board.health2 = Math.max(0, board.health2 - damage);
        }
    }

    /** Damages a player directly (health never drops below 0), bypassing the board. */
    public void damagePlayer(boolean p1, int damage) {
        reduceHP(p1, damage);
    }

    /** performs an attack. please ensure ahead of time the source card is valid */
    public void attack(int target, int source) {
        // Queued first so it still runs if the attack exits early (e.g. Vwoop dodging).
        selfDeathChecks.add(source);
        if (board.getBoardCard(target).isEmpty()) {
            if (!trigger(BattleEvent.FIGHT, source, target, source, BattleEvent.Distribution.THIS)) return;
            int damage = state[source].power;
            reduceHP(getOwner(target), damage);
            LOGGER.info(board.getBoardCard(source) + " dealt " + damage + " damage to Player " + player(!isP1()));
            board.addLog(new BattleComponent(Component.translatable(getCard(source).getDescriptionId()).append(Component.translatable("battles.log.buddycards.attack1")).append("" + damage).append(Component.translatable("battles.log.buddycards.attack2")).append(getOwner(target) ? board.getName1() : board.getName2()), List.of(BuddycardBattleIcon.create(getCard(source)), TextureBattleIcon.damageIcon(damage))), source, LoggedEvent.ATTACK_ID);
        } else {
            if (!trigger(BattleEvent.FIGHT, source, target, source)) return;
            int damage = state[source].power;
            turnPower[target] -= damage;
            LOGGER.info(board.getBoardCard(source) + " dealt " + damage + " damage to " + board.getBoardCard(target));
            board.addLog(new BattleComponent(Component.translatable(getCard(source).getDescriptionId()).append(Component.translatable("battles.log.buddycards.attack1")).append("" + damage).append(Component.translatable("battles.log.buddycards.attack2")).append(Component.translatable(getCard(target).getDescriptionId())), List.of(BuddycardBattleIcon.create(getCard(source)), TextureBattleIcon.damageIcon(damage), BuddycardBattleIcon.create(getCard(target)))), source, LoggedEvent.ATTACK_ID);
            if (!trigger(BattleEvent.DAMAGED, target, target, source)) return;
            savedAttacks.add(new BattleAttack(target, source));
        }
    }

    public void directAttack(int target, int source, int amount) {
        directAttack(target, source, amount, true, true);
    }

    /** performs an attack dealing a specific amount of damage */
    public void directAttack(int target, int source, int amount, boolean doAttackTrigger, boolean doDamageTrigger) {
        if (board.getBoardCard(target).isEmpty()) {
            if (doAttackTrigger && !trigger(BattleEvent.FIGHT, source, target, source)) return;
            reduceHP(getOwner(target), amount);
            LOGGER.info(board.getBoardCard(source) + " dealt " + amount + " damage to Player " + player(!isP1()));
        } else {
            if (doAttackTrigger) if (!trigger(BattleEvent.FIGHT, source, target, source)) return;
            turnPower[target] -= amount;
            LOGGER.info(board.getBoardCard(source) + " dealt " + amount + " damage to " + board.getBoardCard(target));
            if (doDamageTrigger) if (!trigger(BattleEvent.DAMAGED, target, target, source)) return;
            state[target].power = turnPower[target];
            updatePower(target);
        }
    }

    // Events/Triggers slot.

    public boolean trigger(BattleEvent event, int slot, int target, int source, BattleEvent.Distribution targets, Function<Integer, Boolean> condition) {
        int[] slots = targets.apply(slot, this);
        for (int s : slots) {
            if (condition.apply(s) && !trigger(event, s, target, source)) return false;
        }
        return true;
    }

    public boolean trigger(BattleEvent event, int slot, int target, int source, BattleEvent.Distribution targets) {
        int[] slots = targets.apply(slot, this);
        for (int s : slots) {
            if (!trigger(event, s, target, source)) return false;
        }
        return true;
    }

    public boolean trigger(BattleEvent event, int slot, int target, int source) {
        if (runEvents.containsKey(slot)){
            if (runEvents.get(slot).contains(event.name)){
                return true;
            } else {
                runEvents.get(slot).add(event.name);
            }
        } else {
            ArrayList<String> newList = new ArrayList<>();
            newList.add(event.name);
            runEvents.put(slot, newList);
        }

        if (infLoopFail()) return false;
        ItemStack card = board.getBoardCard(slot);
        if (state[slot].status != BattleStatusEffect.EMPTY) {
            BattleAbility effect = state[slot].getStatusEffect();
            if (effect != null && effect.event.equals(event)) {
                board.pushActingContext(slot, effect.name);
                boolean result;
                try {
                    result = effect.ability.trigger(this, slot, target, source);
                } finally {
                    board.popActingContext();
                }
                if (!result) return false;
                ++loopDetector; // count triggers for infinite loops
                if (board.getBoardCard(slot) != card) return true;
            }
        }
        if (!card.isEmpty()) {
            Optional<CardBattleStats> stats = CardBattleStatsRegistry.get(card);
            if (stats.isPresent() && stats.get().abilities().containsKey(event)) {
                for (BattleAbility ability : stats.get().abilities().get(event)) {
                    board.pushActingContext(slot, ability.name);
                    boolean result;
                    try {
                        result = ability.ability.trigger(this, slot, target, source);
                    } finally {
                        board.popActingContext();
                    }
                    if (!result) return false;
                    ++loopDetector; // count triggers for infinite loops
                    if (board.getBoardCard(slot) != card) return true;
                }
            }
        }
        return true;
    }

    public boolean trigger(BattleEvent event, int slot) {
        return trigger(event, slot, slot, slot);
    }

    private boolean infLoopFail() {
        if (loopDetector > 200) {
            if (!sentInfLoopMsg) {
                sentInfLoopMsg = true;
                LOGGER.error("Infinite ability loop detected!", new Throwable());
                for (int i = 0; i < 10; i++) {
                    board.addLog(new BattleComponent(Component.translatable("battles.log.buddycards.infinite_loop"), List.of(TextureBattleIcon.deathIcon, TextureBattleIcon.deathIcon, TextureBattleIcon.deathIcon, TextureBattleIcon.deathIcon, TextureBattleIcon.deathIcon)));
                }
            }
            return true;
        } else return false;
    }

    /** Plays a card from hand onto the board. */
    public boolean playCard(int slot, ItemStack stack, boolean p1) {
        if (!board.getBoardCard(slot).isEmpty()) return false;
        Optional<CardBattleStats> stats = CardBattleStatsRegistry.get(stack);
        if (stats.isEmpty()) return false;
        int cost = stats.get().cost();
        if (board.energy(p1) < cost) return false;
        if (p1) board.energy1 -= cost;
        else board.energy2 -= cost;
        if (!addCard(slot, stack)) return false;
        // OBSERVE_ALLY means "you played a card": only cards played from hand, never the card itself.
        int rowStart = slot < 3 ? 0 : 3;
        for (int i = rowStart; i < rowStart + 3; i++) {
            if (i != slot) trigger(BattleEvent.OBSERVE_ALLY, i, slot, slot);
        }
        checkGameOver();
        return true;
    }

    /** Adds a card into play. */
    public boolean addCard(int slot, ItemStack stack) {
        if (!board.getBoardCard(slot).isEmpty()) return false;
        int power = CardBattleStatsRegistry.get(stack).map(CardBattleStats::power).orElse(0);
        board.setBoardCard(slot, stack);
        state[slot] = new BattleCardState(power, BattleStatusEffect.EMPTY);
        turnPower[slot] = power;
        board.addLog(new BattleComponent(Component.literal("").append(isP1() ? board.getName1() : board.getName2()).append(Component.translatable("battles.log.buddycards.card_play")).append(Component.translatable(stack.getDescriptionId())), List.of(BuddycardBattleIcon.create(stack), TextureBattleIcon.playIcon)));
        trigger(BattleEvent.PLAYED, slot);
        // OBSERVE_ENEMY means "an enemy entered": fires for any card entering, including spawned ones.
        int enemyRowStart = slot < 3 ? 3 : 0;
        for (int i = enemyRowStart; i < enemyRowStart + 3; i++) {
            trigger(BattleEvent.OBSERVE_ENEMY, i, slot, slot);
        }
        return true;
    }

    /** moves a card if you need that for whatever reason */
    public ItemStack moveCard(int target, int destination) {
        if (board.getBoardCard(destination).isEmpty()) {
            ItemStack stack = board.removeBoardCard(target);
            if (!stack.isEmpty()) {
                board.setBoardCard(destination, stack);
                state[destination] = state[target];
                state[target] = new BattleCardState(0, BattleStatusEffect.EMPTY);
                turnPower[destination] = turnPower[target];
                turnPower[target] = 0;
            }
            return stack;
        }
        return ItemStack.EMPTY;
    }

    public void swapCards(int a, int b) {
        ItemStack stackA = board.getBoardCard(a);
        ItemStack stackB = board.getBoardCard(b);
        board.setBoardCard(a, stackB);
        board.setBoardCard(b, stackA);

        BattleCardState stateA = state[a];
        state[a] = state[b];
        state[b] = stateA;

        int powerA = turnPower[a];
        turnPower[a] = turnPower[b];
        turnPower[b] = powerA;
    }

    /** removes a card from play. NO DAMAGE/DEATH EVENTS ARE FIRED */
    public ItemStack removeCard(int target) {
        ItemStack stack = board.getBoardCard(target);
        if (!stack.isEmpty()) {
            board.returnToDeck(getOwner(target), board.removeBoardCard(target));
            turnPower[target] = 0;
            state[target].power = 0;
            state[target].status = BattleStatusEffect.EMPTY;
            return stack;
        }
        return ItemStack.EMPTY;
    }

    private void logAndRemove(int slot) {
        ItemStack removedCard = getCard(slot);
        if (removedCard.getItem() == Items.AIR){
            return;
        }
        removeCard(slot);
        board.addLog(new BattleComponent(Component.translatable(removedCard.getDescriptionId()).append(Component.translatable("battles.log.buddycards.death")), List.of(BuddycardBattleIcon.create(removedCard), TextureBattleIcon.deathIcon)));
    }

    /** returns a card to your hand, if possible. */
    public ItemStack returnCard(int target) {
        ItemStack stack = board.getBoardCard(target);
        if (!stack.isEmpty() && board.tryPutInHand(getOwner(target), stack)) {
            board.removeBoardCard(target);
            turnPower[target] = 0;
            state[target].power = 0;
            state[target].status = BattleStatusEffect.EMPTY;
            return stack;
        }
        return ItemStack.EMPTY;
    }

    public ItemStack getCard(int slot) {
        return board.getBoardCard(slot);
    }

    public boolean canPlay(boolean p1, ItemStack card) {
        return p1 == isP1() && CardBattleStatsRegistry.get(card).map(s -> board.energy(p1) >= s.cost()).orElse(false);
    }

    public boolean isP1() {
        return board.isPlayer1Turn;
    }

    public char player() {
        return player(isP1());
    }

    public static char player(boolean p1) {
        return p1 ? '1' : '2';
    }

    /** Automatic slot calculation based off of current turn. Only pass 0-2 */
    public int slot(int slot) {
        return slot(slot, isP1());
    }

    public static int slot(int slot, boolean p1) {
        return p1 ? slot : slot + 3;
    }

    public static boolean isOpposite(int slot, int opposite) {
        return slot % 3 == opposite % 3;
    }

    public static int opposite(int slot) {
        return slot < 3 ? slot + 3 : slot - 3;
    }

    /** Adjacent slots L->R */
    public static int[] adjacent(int slot) {
        return switch (slot) {
            default -> new int[]{-1};
            case 0 -> new int[]{1};   //X1____
            case 1 -> new int[]{0, 2};//0X2___
            case 2 -> new int[]{1};   //_1X___
            case 3 -> new int[]{4};   //___X4_
            case 4 -> new int[]{3, 5};//___3X5
            case 5 -> new int[]{4};   //____4X
        };
    }

    /** Row neighbours plus the opposite slot. */
    public static int[] allAdjacent(int slot) {
        return switch (slot) {
            default -> new int[]{-1};
            case 0 -> new int[]{3, 1};   //X1_3__
            case 1 -> new int[]{4, 0, 2};//0X2_4_
            case 2 -> new int[]{5, 1};   //_1X__5
            case 3 -> new int[]{0, 4};   //0__X4_
            case 4 -> new int[]{1, 3, 5};//_1_3X5
            case 5 -> new int[]{2, 4};   //__2_4X
        };
    }

    public static int[] allies(int slot) {
        return switch (slot) {
            default -> new int[]{-1};
            case 0 -> new int[]{1, 2};//X12___
            case 1 -> new int[]{0, 2};//0X2___
            case 2 -> new int[]{0, 1};//01X___
            case 3 -> new int[]{4, 5};//___X45
            case 4 -> new int[]{3, 5};//___3X5
            case 5 -> new int[]{3, 4};//___34X
        };
    }

    /** true = p1, false = p2 */
    public static boolean getOwner(int slot) {
        return switch (slot) {
            default -> true;
            case 0, 1, 2 -> true;
            case 3, 4, 5 -> false;
        };
    }

    private record BattleAttack(int target, int source) {}
}
