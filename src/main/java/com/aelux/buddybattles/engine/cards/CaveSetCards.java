package com.aelux.buddybattles.engine.cards;

import com.mojang.logging.LogUtils;
import com.aelux.buddybattles.engine.*;
import com.aelux.buddybattles.engine.log.BattleComponent;
import com.aelux.buddybattles.engine.log.BuddycardBattleIcon;
import com.aelux.buddybattles.engine.log.IBattleIcon;
import com.aelux.buddybattles.engine.log.TextureBattleIcon;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/** All 27 Cave-set cards. */
public class CaveSetCards {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register() {
        registerIfPresent(1, pikax());
        registerIfPresent(2, expire());
        registerIfPresent(3, bumblina());
        registerIfPresent(4, potter());
        registerIfPresent(5, shard());
        registerIfPresent(6, zombert());
        registerIfPresent(7, meddiPot());
        registerIfPresent(8, hurm());
        registerIfPresent(9, skelly());
        registerIfPresent(10, flicker());
        registerIfPresent(11, sord());
        registerIfPresent(12, shooter());
        registerIfPresent(13, butin());
        registerIfPresent(14, trumpet());
        registerIfPresent(15, shush());
        registerIfPresent(16, creep());
        registerIfPresent(17, booms());
        registerIfPresent(18, vinder());
        registerIfPresent(19, flag());
        registerIfPresent(20, blinky());
        registerIfPresent(21, watcher());
        registerIfPresent(22, veks());
        registerIfPresent(23, ally());
        registerIfPresent(24, lern());
        registerIfPresent(25, boomsToGo());
        registerIfPresent(26, screamer());
        registerIfPresent(27, spamsy());
    }

    private static Item card(int cardNumber) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("buddycards", "buddycard_cave" + cardNumber));
    }

    private static void registerIfPresent(int cardNumber, CardBattleStats stats) {
        Item item = card(cardNumber);
        if (item == Items.AIR) {
            LOGGER.warn("Buddy Battles: cave set card {} not found in Buddycards' item registry (version mismatch?) - skipping.", cardNumber);
            return;
        }
        CardBattleStatsRegistry.register(item, stats);
    }

    /** Picks a random OTHER non-empty ally slot (same side, excluding `slot` itself). */
    private static int randomOtherAlly(BattleGame game, int slot) {
        List<Integer> candidates = new ArrayList<>();
        for (int i : BattleEvent.Distribution.ROW.apply(slot, game)) {
            if (i != slot && !game.getCard(i).isEmpty()) candidates.add(i);
        }
        if (candidates.isEmpty()) return -1;
        return candidates.get(BattleBoard.RANDOM.nextInt(candidates.size()));
    }

    // 1: Pikax -- "When this unit fights, gain 1 Energy. When this unit takes damage, lose 1 Energy."
    private static CardBattleStats pikax() {
        return new CardBattleStats(1, 1, new BattleAbility.Builder()
                .add(new BattleAbility(BattleEvent.FIGHT, "diggy_diggy_hole", (game, slot, target, source) -> {
                    boolean owner = BattleGame.getOwner(slot);
                    game.board.gainEnergy(owner, 1);
                    game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.diggy_diggy_hole.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, TextureBattleIcon.energyIcon(1))), slot, LoggedEvent.BUFF_ID);
                    return true;
                }))
                .add(new BattleAbility(BattleEvent.DAMAGED, "diggy_diggy_ouch", (game, slot, target, source) -> {
                    boolean owner = BattleGame.getOwner(slot);
                    game.board.spendEnergy(owner, 1);
                    game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.diggy_diggy_ouch.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, TextureBattleIcon.subtractIcon(1))), slot, LoggedEvent.SHAKE_ID);
                    return true;
                })).build());
    }

    // 2: Expire -- "When an enemy enters, add an Expire to your hand from your deck and take 1 damage."
    private static CardBattleStats expire() {
        return CardBattleStats.of(1, 1, new BattleAbility(BattleEvent.OBSERVE_ENEMY, "what_doesnt_grow_dies", (game, slot, target, source) -> {
            boolean owner = BattleGame.getOwner(slot);
            if (game.board.tryTutorCard(owner, stack -> stack.is(card(2)))) {
                game.board.addLog(new BattleComponent(Component.literal("").append(owner ? game.board.getName1() : game.board.getName2()).append(Component.translatable("battles.ability.buddycards.what_doesnt_grow_dies.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(new ItemStack(card(2))), TextureBattleIcon.drawIcon, TextureBattleIcon.damageIcon(1))), slot, LoggedEvent.SHAKE_ID);
                game.damagePlayer(owner, 1);
            }
            return true;
        }));
    }

    // 3: Bumblina -- "When this card fights, it dies."
    private static CardBattleStats bumblina() {
        return CardBattleStats.of(2, 4, new BattleAbility(BattleEvent.FIGHT, "shes_a_bumblebee", (game, slot, target, source) -> {
            // turnPower, not state[slot].power, so Bumblina still deals its own damage before dying.
            game.turnPower[slot] = 0;
            game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.shes_a_bumblebee.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, TextureBattleIcon.deathIcon)), slot, LoggedEvent.SHAKE_ID);
            return true;
        }));
    }

    // 4: Potter -- "When this enters, add a Shard to your hand from your deck."
    private static CardBattleStats potter() {
        return CardBattleStats.of(4, 3, new BattleAbility(BattleEvent.PLAYED, "yer_a_relic", (game, slot, target, source) -> {
            boolean owner = BattleGame.getOwner(slot);
            if (game.board.tryTutorCard(owner, stack -> stack.is(card(5)))) {
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.yer_a_relic.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(new ItemStack(card(5))), TextureBattleIcon.drawIcon)), slot, LoggedEvent.CIRCLE_ID);
            }
            return true;
        }));
    }

    // 5: Shard -- "When this enters, each of your non-Shard cards gain 1 Power."
    private static CardBattleStats shard() {
        return CardBattleStats.of(2, 1, new BattleAbility(BattleEvent.PLAYED, "tricolored", (game, slot, target, source) -> {
            List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
            for (int i : BattleEvent.Distribution.ROW.apply(slot, game)) {
                if (!game.getCard(i).isEmpty() && !game.getCard(i).is(card(5))) {
                    game.turnPower[i]++;
                    icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                }
            }
            if (icons.size() > 2) {
                icons.add(TextureBattleIcon.addIcon(1));
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.tricolored.log"), icons), slot, LoggedEvent.BUFF_ID);
                game.updatePower();
            }
            return true;
        }));
    }

    // 6: Zombert -- "When this dies, 50% chance to not die instead and remain at 1 Power."
    private static CardBattleStats zombert() {
        return CardBattleStats.of(3, 2, new BattleAbility(BattleEvent.DEATH, "dont_dead", (game, slot, target, source) -> {
            if (BattleBoard.RANDOM.nextBoolean()) {
                game.turnPower[slot] = 1;
                game.state[slot].power = 1;
                game.board.addLog(new BattleComponent(Component.translatable(game.getCard(slot).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.dont_dead.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.equalsIcon(1))), slot, LoggedEvent.SHAKE_ID);
                return false; // cancels the death
            }
            return true;
        }));
    }

    // 7: Meddi Pot -- "When this enters, each of your other units gains Regeneration."
    private static CardBattleStats meddiPot() {
        return CardBattleStats.of(3, 2, new BattleAbility(BattleEvent.PLAYED, "just_a_sip", (game, slot, target, source) -> {
            List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
            for (int i : BattleEvent.Distribution.ROW.apply(slot, game)) {
                if (i != slot && !game.getCard(i).isEmpty()) {
                    game.state[i].status = BattleStatusEffect.REGENERATION;
                    icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                }
            }
            if (icons.size() > 2) {
                icons.add(TextureBattleIcon.statusIcon(BattleStatusEffect.REGENERATION));
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.just_a_sip.log"), icons), slot, LoggedEvent.CIRCLE_ID);
            }
            return true;
        }));
    }

    // 8: Hurm -- "Activate: 3 Energy - Draw two cards."
    private static CardBattleStats hurm() {
        return CardBattleStats.of(3, 2, new BattleAbility(BattleEvent.ACTIVATED, "knowledge", (game, slot, target, source) -> {
            boolean owner = BattleGame.getOwner(slot);
            if (game.board.energy(owner) < 3) return false;
            game.board.spendEnergy(owner, 3);
            List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, TextureBattleIcon.energyIcon(3)));
            if (game.board.tryDrawCard(owner)) icons.add(TextureBattleIcon.drawIcon);
            if (game.board.tryDrawCard(owner)) icons.add(TextureBattleIcon.drawIcon);
            game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.knowledge.log"), icons), slot, LoggedEvent.CIRCLE_ID);
            return true;
        }));
    }

    // 9: Skelly -- "When this dies, deal 1 damage to the opponent."
    private static CardBattleStats skelly() {
        return CardBattleStats.of(2, 2, new BattleAbility(BattleEvent.DEATH, "bowmanship", (game, slot, target, source) -> {
            game.damagePlayer(!BattleGame.getOwner(slot), 1);
            game.board.addLog(new BattleComponent(Component.translatable(game.getCard(slot).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.bowmanship.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, TextureBattleIcon.damageIcon(1))), slot, LoggedEvent.ATTACK_ID);
            return true;
        }));
    }

    // 10: Flicker -- "When this enters, inflict each opposing enemy with Fire."
    private static CardBattleStats flicker() {
        return CardBattleStats.of(2, 1, new BattleAbility(BattleEvent.PLAYED, "light_up_my_world", (game, slot, target, source) -> {
            List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
            for (int i = 0; i < 6; i++) {
                boolean sameSide = Math.floor(i / 3.0) == Math.floor(slot / 3.0);
                if (!sameSide && !game.getCard(i).isEmpty()) {
                    game.state[i].status = BattleStatusEffect.FIRE;
                    icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                }
            }
            if (icons.size() > 2) {
                icons.add(TextureBattleIcon.statusIcon(BattleStatusEffect.FIRE));
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.light_up_my_world.log"), icons), slot, LoggedEvent.SHAKE_ID);
            }
            return true;
        }));
    }

    // 11: Sord -- "When this enters, give an ally Strength."
    private static CardBattleStats sord() {
        return CardBattleStats.of(2, 1, new BattleAbility(BattleEvent.PLAYED, "so_sord", (game, slot, target, source) -> {
            int ally = randomOtherAlly(game, slot);
            if (ally != -1) {
                game.state[ally].status = BattleStatusEffect.STRENGTH;
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.so_sord.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(ally)), TextureBattleIcon.statusIcon(BattleStatusEffect.STRENGTH))), slot, LoggedEvent.BUFF_ID);
            }
            return true;
        }));
    }

    // 12: Shooter -- "When this enters, inflict an enemy with Poison."
    private static CardBattleStats shooter() {
        return CardBattleStats.of(2, 2, new BattleAbility(BattleEvent.PLAYED, "aim_and_fire", (game, slot, target, source) -> {
            int opp = BattleGame.opposite(slot);
            if (!game.getCard(opp).isEmpty()) {
                game.state[opp].status = BattleStatusEffect.POISON;
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.aim_and_fire.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(opp)), TextureBattleIcon.statusIcon(BattleStatusEffect.POISON))), slot, LoggedEvent.ATTACK_ID);
            }
            return true;
        }));
    }

    // 13: Butin -- "Activate - 1 energy: Give Powered to each adjacent Redstone unit."
    private static CardBattleStats butin() {
        return new CardBattleStats(2, 2, new BattleAbility.Builder()
            .add(new BattleAbility(BattleEvent.ACTIVATED, "treasure_hunt", (game, slot, target, source) -> {
                boolean owner = BattleGame.getOwner(slot);
                if (game.board.energy(owner) < 1) return false;
                game.board.spendEnergy(owner, 1);
                List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
                List<Integer> cards = new ArrayList<>();
                for (int i : BattleEvent.Distribution.ADJACENT.apply(slot, game)) {
                    if (game.getCard(i).is(CardTags.REDSTONE)) {
                        cards.add(i);
                        icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                    }
                }
                if (!cards.isEmpty()) {
                    icons.add(TextureBattleIcon.powerIcon);
                    game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.treasure_hunt.log"), icons), slot, LoggedEvent.BUFF_ID);
                    for (int i : cards) game.trigger(BattleEvent.POWERED, i, i, slot);
                }
                return true;
            })).build());
    }

    private static void butinRoll(BattleGame game, int slot) {
        boolean owner = BattleGame.getOwner(slot);
        if (BattleBoard.RANDOM.nextBoolean()) {
            if (game.board.tryDrawCard(owner)) {
                game.board.addLog(new BattleComponent(Component.literal("").append(owner ? game.board.getName1() : game.board.getName2()).append(Component.translatable("battles.ability.buddycards.treasure_hunt.log1")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, TextureBattleIcon.drawIcon)), slot, LoggedEvent.CIRCLE_ID);
            }
        } else {
            game.damagePlayer(owner, 2);
            game.board.addLog(new BattleComponent(Component.literal("").append(owner ? game.board.getName1() : game.board.getName2()).append(Component.translatable("battles.ability.buddycards.treasure_hunt.log2")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, TextureBattleIcon.damageIcon(2))), slot, LoggedEvent.SHAKE_ID);
        }
    }

    // 14: Trumpet -- "When this enters, give an ally Resistance."
    private static CardBattleStats trumpet() {
        return CardBattleStats.of(6, 4, new BattleAbility(BattleEvent.PLAYED, "musical_talent", (game, slot, target, source) -> {
            int ally = randomOtherAlly(game, slot);
            if (ally != -1) {
                game.state[ally].status = BattleStatusEffect.RESISTANCE;
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.musical_talent.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(ally)), TextureBattleIcon.statusIcon(BattleStatusEffect.RESISTANCE))), slot, LoggedEvent.BUFF_ID);
            }
            return true;
        }));
    }

    // 15: Shush -- "When an enemy enters, it takes 1 damage."
    private static CardBattleStats shush() {
        return CardBattleStats.of(4, 2, new BattleAbility(BattleEvent.OBSERVE_ENEMY, "be_quiet", (game, slot, target, source) -> {
            game.directAttack(target, slot, 1, false, true);
            game.board.addLog(new BattleComponent(Component.translatable(game.getCard(target).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.be_quiet.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(target)), TextureBattleIcon.subtractIcon(1))), slot, LoggedEvent.ATTACK_ID);
            return true;
        }));
    }

    // 16: Creep -- "Activate - Deal 1 damage to all creatures and your opponent, then kill this unit."
    private static CardBattleStats creep() {
        return CardBattleStats.of(6, 4, new BattleAbility(BattleEvent.ACTIVATED, "nice_everything", (game, slot, target, source) -> {
            List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
            icons.addAll(CardEffects.dealAoeDamage(game, slot, 1));
            icons.add(TextureBattleIcon.damageIcon(1));
            game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.nice_everything.log"), icons), slot, LoggedEvent.SHAKE_ID);
            game.updatePower();
            game.state[slot].power = 0;
            game.updatePower(slot);
            return true;
        }));
    }

    // 17: Booms -- "0 power, when this dies deal 1 damage to all creatures and your opponent."
    private static CardBattleStats booms() {
        return CardBattleStats.of(2, 1, new BattleAbility(BattleEvent.DEATH, "splode", (game, slot, target, source) -> {
            List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
            icons.addAll(CardEffects.dealAoeDamage(game, slot, 1));
            icons.add(TextureBattleIcon.damageIcon(1));
            game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.splode.log"), icons), slot, LoggedEvent.SHAKE_ID);
            game.updatePower();
            return true;
        }));
    }

    // 18: Vinder -- "When this enters, kills an allied Equipment or Hurm. If you do, gain 4 Power."
    private static CardBattleStats vinder() {
        return CardBattleStats.of(7, 5, new BattleAbility(BattleEvent.PLAYED, "axe_you_a_question", (game, slot, target, source) -> {
            for (int i : BattleEvent.Distribution.ROW.apply(slot, game)) {
                if (i == slot || game.getCard(i).isEmpty()) continue;
                if (game.getCard(i).is(CardTags.EQUIPMENT) || game.getCard(i).is(card(8))) {
                    game.board.addLog(new BattleComponent(Component.translatable(game.getCard(i).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.axe_you_a_question.log1")).append(Component.translatable(game.getCard(slot).getDescriptionId())).append(Component.translatable("battles.ability.buddycards.axe_you_a_question.log2")), List.of(BuddycardBattleIcon.create(game.getCard(i)), TextureBattleIcon.deathIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.addIcon(4))), slot, LoggedEvent.BUFF_ID);
                    game.state[i].power = 0;
                    game.turnPower[i] = 0;
                    game.updatePower(i);
                    game.turnPower[slot] += 4;
                    game.updatePower(slot);
                    break;
                }
            }
            return true;
        }));
    }

    // 19: Flag -- "When this enters, give an ally Regeneration."
    private static CardBattleStats flag() {
        return CardBattleStats.of(4, 3, new BattleAbility(BattleEvent.PLAYED, "already_surrendering", (game, slot, target, source) -> {
            int ally = randomOtherAlly(game, slot);
            if (ally != -1) {
                game.state[ally].status = BattleStatusEffect.REGENERATION;
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.already_surrendering.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(ally)), TextureBattleIcon.statusIcon(BattleStatusEffect.REGENERATION))), slot, LoggedEvent.CIRCLE_ID);
            }
            return true;
        }));
    }

    // 20: Blinky -- "At the start of each turn, alternates between gaining 1 Power and giving Powered to
    // adjacent Redstone buddies."
    private static CardBattleStats blinky() {
        return CardBattleStats.of(3, 2, new BattleAbility(BattleEvent.TURN, "inverting_and_powering", (game, slot, target, source) -> {
            if ((game.board.turn / 2) % 2 == 1) {
                List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
                List<Integer> cards = new ArrayList<>();
                for (int i : BattleEvent.Distribution.ADJACENT.apply(slot, game)) {
                    if (game.getCard(i).is(CardTags.REDSTONE)) {
                        cards.add(i);
                        icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                    }
                }
                if (!cards.isEmpty()) {
                    icons.add(TextureBattleIcon.powerIcon);
                    game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.blinky_energize.log"), icons), slot, LoggedEvent.BUFF_ID);
                    for (int i : cards) game.trigger(BattleEvent.POWERED, i, i, slot);
                }
                return true;
            } else {
                game.turnPower[slot]++;
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.blinky_powerize.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.addIcon(1))), slot, LoggedEvent.BUFF_ID);
                game.updatePower(slot);
                return true;
            }
        }));
    }

    // 21: Watcher -- "When an enemy enters, this unit gains 1 Power. When Powered, gain 1 Energy."
    private static CardBattleStats watcher() {
        return new CardBattleStats(5, 3, new BattleAbility.Builder()
                .add(new BattleAbility(BattleEvent.OBSERVE_ENEMY, "eyes_on_the_prize", (game, slot, target, source) -> {
                    game.turnPower[slot]++;
                    game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.eyes_on_the_prize.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.addIcon(1))), slot, LoggedEvent.BUFF_ID);
                    game.updatePower(slot);
                    return true;
                }))
                .add(new BattleAbility(BattleEvent.POWERED, "eyes_energized", (game, slot, target, source) -> {
                    boolean owner = BattleGame.getOwner(slot);
                    game.board.gainEnergy(owner, 1);
                    game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.eyes_energized.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, TextureBattleIcon.energyIcon(1))), slot, LoggedEvent.CIRCLE_ID);
                    return true;
                })).build());
    }

    // 22: Veks -- "When this enters, fill the rest of your open positions with copies of it."
    private static CardBattleStats veks() {
        return CardBattleStats.of(4, 2, new BattleAbility(BattleEvent.PLAYED, "super_hostile", (game, slot, target, source) -> {
            List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
            boolean any = false;
            for (int i : BattleEvent.Distribution.ROW.apply(slot, game)) {
                // Checked live each iteration, not a stale snapshot.
                if (game.getCard(i).isEmpty()) {
                    game.addCard(i, new ItemStack(card(22)));
                    icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                    any = true;
                }
            }
            if (any) {
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.super_hostile.log"), icons), slot, LoggedEvent.BUFF_ID);
            }
            return true;
        }));
    }

    // 23: Ally -- "When this enters, add an Equipment from your deck to your hand."
    private static CardBattleStats ally() {
        return CardBattleStats.of(5, 4, new BattleAbility(BattleEvent.PLAYED, "supportive", (game, slot, target, source) -> {
            boolean owner = BattleGame.getOwner(slot);
            if (game.board.tryTutorCard(owner, stack -> stack.is(CardTags.EQUIPMENT))) {
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.supportive.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, TextureBattleIcon.drawIcon)), slot, LoggedEvent.CIRCLE_ID);
            }
            return true;
        }));
    }

    // 24: Lern -- "At the start of your turn, draw a card. Activate - Discard a card, gain 1 Energy."
    private static CardBattleStats lern() {
        return new CardBattleStats(5, 3, new BattleAbility.Builder()
                .add(new BattleAbility(BattleEvent.TURN, "take_a_look", (game, slot, target, source) -> {
                    boolean owner = BattleGame.getOwner(slot);
                    if (game.board.tryDrawCard(owner)) {
                        game.board.addLog(new BattleComponent(Component.literal("").append(owner ? game.board.getName1() : game.board.getName2()).append(Component.translatable("battles.ability.buddycards.take_a_look.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, TextureBattleIcon.drawIcon)), slot, LoggedEvent.CIRCLE_ID);
                    }
                    return true;
                }))
                .add(new BattleAbility(BattleEvent.ACTIVATED, "knowledge_is_power", (game, slot, target, source) -> {
                    boolean owner = BattleGame.getOwner(slot);
                    ItemStack discarded = game.board.discardRandomFromHand(owner);
                    if (!discarded.isEmpty()) {
                        game.board.gainEnergy(owner, 1);
                        game.board.addLog(new BattleComponent(Component.translatable(discarded.getDescriptionId()).append(Component.translatable("battles.ability.buddycards.knowledge_is_power.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(discarded), TextureBattleIcon.energyIcon(1))), slot, LoggedEvent.CIRCLE_ID);
                        return true;
                    }
                    return false;
                })).build());
    }

    // 25: Booms-To-Go -- "At the start of your turn, move into the first empty adjacent slot and gain 2
    // power. When powered at 10, deals 4 damage to each creature and opponent."
    private static CardBattleStats boomsToGo() {
        return new CardBattleStats(5, 4, new BattleAbility.Builder()
                .add(new BattleAbility(BattleEvent.TURN, "just_a_ride", (game, slot, target, source) -> {
                    for (int i : BattleEvent.Distribution.ADJACENT.apply(slot, game)) {
                        if (game.getCard(i).isEmpty()) {
                            ItemStack moved = game.moveCard(slot, i);
                            game.turnPower[i] += 2;
                            game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.just_a_ride.log"), List.of(BuddycardBattleIcon.create(moved), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(moved), TextureBattleIcon.returnIcon, TextureBattleIcon.addIcon(2))), slot, LoggedEvent.BUFF_ID);
                            game.updatePower(i);
                            break;
                        }
                    }
                    return true;
                }))
                .add(new BattleAbility(BattleEvent.POWERED, "just_a_ride_boom", (game, slot, target, source) -> {
                    if (game.state[slot].power >= 10) {
                        List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
                        icons.addAll(CardEffects.dealAoeDamage(game, slot, 4));
                        icons.add(TextureBattleIcon.damageIcon(4));
                        game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.just_a_ride_boom.log"), icons), slot, LoggedEvent.SHAKE_ID);
                        game.updatePower();
                    }
                    return true;
                })).build());
    }

    // 26: Screamer -- "When an enemy enters, it is inflicted with Weakness. When you play a card, you and
    // your opponent each draw a card."
    private static CardBattleStats screamer() {
        return new CardBattleStats(9, 6, new BattleAbility.Builder()
                .add(new BattleAbility(BattleEvent.OBSERVE_ENEMY, "constantly_screaming", (game, slot, target, source) -> {
                    game.state[target].status = BattleStatusEffect.WEAKNESS;
                    game.board.addLog(new BattleComponent(Component.translatable(game.getCard(target).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.constantly_screaming.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(target)), TextureBattleIcon.statusIcon(BattleStatusEffect.WEAKNESS))), slot, LoggedEvent.SHAKE_ID);
                    return true;
                }))
                .add(new BattleAbility(BattleEvent.OBSERVE_ALLY, "screaming_draw", (game, slot, target, source) -> {
                    boolean owner = BattleGame.getOwner(slot);
                    List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
                    if (game.board.tryDrawCard(owner)) icons.add(TextureBattleIcon.drawIcon);
                    if (game.board.tryDrawCard(!owner)) icons.add(TextureBattleIcon.drawIcon);
                    if (icons.size() > 2) {
                        game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.screaming_draw.log"), icons), slot, LoggedEvent.CIRCLE_ID);
                    }
                    return true;
                })).build());
    }

    // 27: Spamsy -- "When Powered, Deal 1 damage to the enemy card (or opponent if no card). Activate:
    // Deal 1 damage to the enemy card (or opponent if no card)."
    private static CardBattleStats spamsy() {
        return new CardBattleStats(7, 4, new BattleAbility.Builder()
                .add(new BattleAbility(BattleEvent.POWERED, "spitting_shots", (game, slot, target, source) -> {
                    List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
                    icons.addAll(CardEffects.dealAoeDamage(game, slot, 1));
                    icons.add(TextureBattleIcon.damageIcon(1));
                    game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddybattles.spamsy_nova.log"), icons), slot, LoggedEvent.SHAKE_ID);
                    game.updatePower();
                    return true;
                }))
                .add(new BattleAbility(BattleEvent.ACTIVATED, "spitting_shots_manual", (game, slot, target, source) -> {
                    spamsyShot(game, slot);
                    return true;
                })).build());
    }

    private static void spamsyShot(BattleGame game, int slot) {
        int opp = BattleGame.opposite(slot);
        game.directAttack(opp, slot, 1);
        if (!game.getCard(opp).isEmpty()) {
            game.board.addLog(new BattleComponent(Component.translatable(game.getCard(opp).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.spitting_shots.log1")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(opp)), TextureBattleIcon.subtractIcon(1))), slot, LoggedEvent.ATTACK_ID);
        } else {
            game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.spitting_shots.log2"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, TextureBattleIcon.damageIcon(1))), slot, LoggedEvent.ATTACK_ID);
        }
    }
}
