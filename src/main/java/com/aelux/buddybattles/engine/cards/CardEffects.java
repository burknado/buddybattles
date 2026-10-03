package com.aelux.buddybattles.engine.cards;

import com.aelux.buddybattles.engine.BattleGame;
import com.aelux.buddybattles.engine.log.BuddycardBattleIcon;
import com.aelux.buddybattles.engine.log.IBattleIcon;

import java.util.ArrayList;
import java.util.List;

/** Effects shared between card sets. */
final class CardEffects {
    private CardEffects() {
    }

    /**
     * Deals damage to every other card on the board and to the source's opponent.
     * Returns icons for every card hit, for the ability's log line.
     */
    static List<IBattleIcon> dealAoeDamage(BattleGame game, int slot, int amount) {
        List<IBattleIcon> icons = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            if (i != slot && !game.getCard(i).isEmpty()) {
                icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                game.directAttack(i, slot, amount, false, true);
            }
        }
        game.damagePlayer(!BattleGame.getOwner(slot), amount);
        return icons;
    }
}
