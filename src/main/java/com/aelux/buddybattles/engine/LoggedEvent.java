package com.aelux.buddybattles.engine;

import com.aelux.buddybattles.engine.log.BattleComponent;

public record LoggedEvent(BattleComponent component, int sourceSlot, String abilityId, BoardSnapshot snapshot) {
    public static final String ATTACK_ID = "attack";
    public static final String NONE_ID = "";
    public static final String BUFF_ID = "buff";
    public static final String SHAKE_ID = "shake";
    public static final String CIRCLE_ID = "circle";
}
