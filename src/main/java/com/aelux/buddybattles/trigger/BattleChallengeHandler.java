package com.aelux.buddybattles.trigger;

import com.wildcard.buddycards.entity.EnderlingEntity;
import com.aelux.buddybattles.config.BuddyBattlesConfig;
import com.aelux.buddybattles.datapack.MobDeckLoader;
import com.aelux.buddybattles.deck.DeckSource;
import com.aelux.buddybattles.session.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.function.Supplier;

/**
 * Right-clicking an entity while holding a deck:
 * <ul>
 *   <li>Target already in a duel - start spectating.</li>
 *   <li>Villager, piglin or enderling - instant duel, subject to the rebattle cooldown.</li>
 *   <li>Player who challenged you - accept and start the duel.</li>
 *   <li>Any other player - send them a challenge.</li>
 * </ul>
 * TODO: busy check for mobs that are trading/bartering; spectate screen.
 */
public class BattleChallengeHandler {
    @SubscribeEvent
    public void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer initiator)) return;

        ItemStack heldStack = event.getItemStack();
        if (!(heldStack.getItem() instanceof DeckSource deckSource)) return;

        var existingSession = BattleSessionManager.INSTANCE.get(event.getTarget().getUUID());
        if (existingSession.isPresent()) {
            existingSession.get().addSpectator(initiator);
            cancel(event);
            return;
        }

        if (deckSource.isEmpty(heldStack)) return;
        if (BattleSessionManager.INSTANCE.isInBattle(initiator.getUUID())) return;

        if (event.getTarget() instanceof Villager villager && !villager.isBaby()) {
            if (!BuddyBattlesConfig.enableVillagerDuels.get()) return;
            startMobDuel(event, initiator, heldStack, villager, () -> new VillagerParticipant(villager));
            return;
        }
        if (event.getTarget() instanceof Piglin piglin && !piglin.isBaby()) {
            if (!BuddyBattlesConfig.enablePiglinDuels.get()) return;
            startMobDuel(event, initiator, heldStack, piglin, () -> new MobParticipant(piglin, MobDeckLoader.PIGLIN));
            return;
        }
        if (event.getTarget() instanceof EnderlingEntity enderling) {
            if (!BuddyBattlesConfig.enableEnderlingDuels.get()) return;
            startMobDuel(event, initiator, heldStack, enderling, () -> new MobParticipant(enderling, MobDeckLoader.ENDERLING));
            return;
        }

        if (event.getTarget() instanceof Player targetPlayer && targetPlayer instanceof ServerPlayer targetServerPlayer) {
            var accepted = PendingChallengeManager.INSTANCE.tryAccept(
                    initiator.getUUID(), targetPlayer.getUUID(), BuddyBattlesConfig.challengeExpirySeconds.get() * 1000L);

            if (accepted.isPresent()) {
                PlayerParticipant p1 = new PlayerParticipant(targetServerPlayer, accepted.get().challengerDeck());
                PlayerParticipant p2 = new PlayerParticipant(initiator, heldStack);
                BattleSession session = BattleSessionManager.INSTANCE.start(p1, p2);
                p1.openBattleScreen();
                p2.openBattleScreen();
                session.beginGame();
            } else {
                PendingChallengeManager.INSTANCE.challenge(targetPlayer.getUUID(), initiator.getUUID(), heldStack);
                initiator.displayClientMessage(Component.translatable("misc.buddybattles.challenge_sent", targetPlayer.getName()), true);
                targetServerPlayer.displayClientMessage(
                        Component.translatable("misc.buddybattles.challenge_received", initiator.getName()), false);
            }
            cancel(event);
        }
    }

    /** Starts a duel against an AI mob. */
    private void startMobDuel(PlayerInteractEvent.EntityInteract event, ServerPlayer initiator, ItemStack heldStack,
                              Mob mob, Supplier<BattleParticipant> participantFactory) {
        cancel(event);

        long cooldownRemaining = RebattleCooldownManager.INSTANCE.secondsRemaining(
                initiator.getUUID(), mob.getUUID(), BuddyBattlesConfig.rebattleCooldownSeconds.get());
        if (cooldownRemaining > 0) {
            initiator.displayClientMessage(Component.translatable("misc.buddybattles.rebattle_cooldown", cooldownRemaining), true);
            return;
        }

        BattleParticipant mobSide = participantFactory.get();
        if (mobSide.getDeck().length == 0) {
            mobSide.onBattleEnded(false, true);
            initiator.displayClientMessage(Component.translatable("misc.buddybattles.no_deck_configured"), true);
            return;
        }

        PlayerParticipant playerSide = new PlayerParticipant(initiator, heldStack);
        BattleSession session = BattleSessionManager.INSTANCE.start(playerSide, mobSide);
        playerSide.openBattleScreen(); // before beginGame() so the opening sync isn't missed
        RebattleCooldownManager.INSTANCE.recordBattleStart(
                initiator.getUUID(), mob.getUUID(), BuddyBattlesConfig.rebattleCooldownSeconds.get());
        session.beginGame();
    }

    private void cancel(PlayerInteractEvent.EntityInteract event) {
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
