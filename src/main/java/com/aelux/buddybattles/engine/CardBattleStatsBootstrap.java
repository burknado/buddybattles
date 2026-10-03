package com.aelux.buddybattles.engine;

import com.aelux.buddybattles.engine.cards.BaseSetCards;
import com.aelux.buddybattles.engine.cards.CaveSetCards;
import com.aelux.buddybattles.engine.cards.EndSetCards;
import com.aelux.buddybattles.engine.cards.FallbackCards;
import com.aelux.buddybattles.engine.cards.NetherSetCards;

// Registers each set of cards
public class CardBattleStatsBootstrap {
    public static void register() {
        BaseSetCards.register();
        NetherSetCards.register();
        EndSetCards.register();
        CaveSetCards.register();
        FallbackCards.fillGaps();
    }
}
