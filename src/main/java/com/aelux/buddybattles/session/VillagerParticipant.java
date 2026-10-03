package com.aelux.buddybattles.session;

import com.aelux.buddybattles.ai.MobBattleAI;
import com.aelux.buddybattles.datapack.VillagerDeckLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Wraps a villager being challenged to a duel. */
public class VillagerParticipant implements BattleParticipant {
    private final Villager villager;
    private final boolean wasNoAiBefore;

    /** Created via attachSession() once BattleSession's constructor builds the board/game. */
    private MobBattleAI ai;
    /** Picked once, so the empty-deck check and the battle use the same random deck. */
    private ItemStack[] deck;

    public VillagerParticipant(Villager villager) {
        this.villager = villager;
        this.wasNoAiBefore = villager.isNoAi();
        villager.setNoAi(true);
    }

    public Villager getVillager() {
        return villager;
    }

    @Override
    public void attachSession(BattleSession session) {
        this.ai = new MobBattleAI(session, this);
    }

    /** Called every server tick by BattleSessionManager - no-ops unless it's actually this side's turn. */
    @Override
    public void tick() {
        if (ai != null) ai.tick();
    }

    @Override
    public UUID getParticipantId() {
        return villager.getUUID();
    }

    @Override
    public Component getDisplayName() {
        return villager.getDisplayName();
    }

    @Override
    public boolean isAI() {
        return true;
    }

    @Override
    public void markDirty() {
        // No GUI to refresh on this side.
    }

    @Override
    public void onBattleEnded(boolean won, boolean interrupted) {
        villager.setNoAi(wasNoAiBefore);
        // won is from THIS participant's own perspective (see BattleSession.end).
        if (!won && !interrupted) {
            dropLootReward();
        }
    }

    /** Drops the loot reward in the world at the villager's position. */
    private void dropLootReward() {
        int level = clampLevel(villager.getVillagerData().getLevel());
        String profession = professionPath(villager);
        Map<Item, Integer> loot = VillagerDeckLoader.getLootFor(profession, level);
        LootDrops.drop(villager.level(), villager.getX(), villager.getY(), villager.getZ(), loot);
    }

    /** Reads the datapack-assigned deck for this villager's profession+trade level VERBATIM. */
    @Override
    public ItemStack[] getDeck() {
        if (deck == null) {
            int level = clampLevel(villager.getVillagerData().getLevel());
            List<Item> pool = VillagerDeckLoader.getPoolFor(professionPath(villager), level);
            deck = pool.stream().map(ItemStack::new).toArray(ItemStack[]::new);
        }
        return deck;
    }

    private static int clampLevel(int rawLevel) {
        return Math.max(1, Math.min(5, rawLevel));
    }

    private static String professionPath(Villager villager) {
        var profession = villager.getVillagerData().getProfession();
        ResourceLocation id = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
        return id != null ? id.getPath() : VillagerDeckLoader.DEFAULT_PROFESSION;
    }
}
