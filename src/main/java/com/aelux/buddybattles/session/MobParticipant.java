package com.aelux.buddybattles.session;

import com.aelux.buddybattles.ai.MobBattleAI;
import com.aelux.buddybattles.datapack.MobDeckLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** An AI-controlled mob without tiers (piglin, enderling) whose deck and loot come from a MobDeckLoader. */
public class MobParticipant implements BattleParticipant {
    private final Mob mob;
    private final MobDeckLoader decks;
    private final boolean wasNoAiBefore;
    private MobBattleAI ai;
    /** Picked once, so the empty-deck check and the battle use the same random deck. */
    private ItemStack[] deck;

    public MobParticipant(Mob mob, MobDeckLoader decks) {
        this.mob = mob;
        this.decks = decks;
        this.wasNoAiBefore = mob.isNoAi();
        mob.setNoAi(true);
    }

    @Override
    public void attachSession(BattleSession session) {
        this.ai = new MobBattleAI(session, this);
    }

    @Override
    public void tick() {
        if (ai != null) ai.tick();
    }

    @Override
    public UUID getParticipantId() {
        return mob.getUUID();
    }

    @Override
    public Component getDisplayName() {
        return mob.getDisplayName();
    }

    @Override
    public boolean isAI() {
        return true;
    }

    @Override
    public void markDirty() {
    }

    @Override
    public void onBattleEnded(boolean won, boolean interrupted) {
        mob.setNoAi(wasNoAiBefore);
        if (!won && !interrupted) {
            LootDrops.drop(mob.level(), mob.getX(), mob.getY(), mob.getZ(), decks.getLoot());
        }
    }

    @Override
    public ItemStack[] getDeck() {
        if (deck == null) deck = decks.getPool().stream().map(ItemStack::new).toArray(ItemStack[]::new);
        return deck;
    }
}
