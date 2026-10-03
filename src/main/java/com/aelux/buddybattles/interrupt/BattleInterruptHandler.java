package com.aelux.buddybattles.interrupt;

import com.aelux.buddybattles.session.BattleSessionManager;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public class BattleInterruptHandler {
    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0) return;

        LivingEntity entity = event.getEntity();
        BattleSessionManager.INSTANCE.get(entity.getUUID())
                .ifPresent(session -> session.interrupt());
    }
}
