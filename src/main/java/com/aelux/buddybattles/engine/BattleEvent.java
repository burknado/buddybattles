package com.aelux.buddybattles.engine;

import java.util.function.BiFunction;

public class BattleEvent {
    public final String name;

    public BattleEvent(String name) {
        this.name = name;
    }

    public BattleAbility ability(String name, BattleAbility.BattleAbilityFunc func) {
        return new BattleAbility(this, name, func);
    }

    /** When a card is played */
    public static final BattleEvent PLAYED = new BattleEvent("played");
    /** At the beginning of every turn, before cards are played */
    public static final BattleEvent TURN = new BattleEvent("turn");
    /** When a card attacks or is attacked */
    public static final BattleEvent FIGHT = new BattleEvent("fight");
    /** When a card kills another */
    public static final BattleEvent KILL = new BattleEvent("kill");
    /** When a card dies */
    public static final BattleEvent DEATH = new BattleEvent("death");
    /** When any card dies */
    public static final BattleEvent OBSERVE_DEATH = new BattleEvent("observe_death");
    /** When a card loses power */
    public static final BattleEvent DAMAGED = new BattleEvent("damaged");
    /** When a card is redstone powered */
    public static final BattleEvent POWERED = new BattleEvent("powered");
    /** When a card is activated manually */
    public static final BattleEvent ACTIVATED = new BattleEvent("activated");
    /** Fires on the opposing row when any card enters the board (played or spawned). target/source = the entering slot. */
    public static final BattleEvent OBSERVE_ENEMY = new BattleEvent("observe_played");
    /** Fires on the rest of your row when you play a card from hand. target/source = the played slot. */
    public static final BattleEvent OBSERVE_ALLY = new BattleEvent("observe_ally_played");

    public interface Distribution extends BiFunction<Integer, BattleGame, int[]> {
        /** All cards */
        Distribution ALL = (slot, game) -> new int[]{0, 1, 2, 3, 4, 5};
        /** Adjacent cards INCLUDING enemy cards */
        Distribution ALL_ADJACENT = (slot, game) -> game.allAdjacent(slot);
        /** Adjacent cards EXCLUDING enemy cards */
        Distribution ADJACENT = (slot, game) -> game.adjacent(slot);
        /** All enemy cards */
        Distribution ALL_ENEMY = (slot, game) -> slot < 3 ? new int[]{3, 4, 5} : new int[]{0, 1, 2};
        /** All of your cards */
        Distribution ROW = (slot, game) -> slot < 3 ? new int[]{0, 1, 2} : new int[]{3, 4, 5};
        /** All of your other cards */
        Distribution ROW_OTHER = (slot, game) -> game.allies(slot);
        /** The provided card and the opposing card */
        Distribution COLUMN = (slot, game) -> new int[]{slot, game.opposite(slot)};
        /** Only the opposing card */
        Distribution OPPOSITE = (slot, game) -> new int[]{game.opposite(slot)};
        /** Only the provided card */
        Distribution THIS = (slot, game) -> new int[]{slot};
    }
}
