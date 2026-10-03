package com.aelux.buddybattles.engine.cards;

import com.mojang.logging.LogUtils;
import com.wildcard.buddycards.item.BuddycardItem;
import com.wildcard.buddycards.registries.BuddycardsItems;
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

/** All 27 End-set cards. */
public class EndSetCards {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register() {
        registerIfPresent(1, enrok());
        registerIfPresent(2, chester());
        registerIfPresent(3, scales());
        registerIfPresent(4, vwoop());
        registerIfPresent(5, silver());
        registerIfPresent(6, libraria());
        registerIfPresent(7, airow());
        registerIfPresent(8, webster());
        registerIfPresent(9, perl());
        registerIfPresent(10, frooty());
        registerIfPresent(11, rod());
        registerIfPresent(12, rokit());
        registerIfPresent(13, wally());
        registerIfPresent(14, flouer());
        registerIfPresent(15, lingerer());
        registerIfPresent(16, chorus());
        registerIfPresent(17, shulk());
        registerIfPresent(18, menace());
        registerIfPresent(19, visionary());
        registerIfPresent(20, sleeper());
        registerIfPresent(21, spectre());
        registerIfPresent(22, card_package());
        registerIfPresent(23, brok());
        registerIfPresent(24, portz());
        registerIfPresent(25, crystal());
        registerIfPresent(26, glider());
        registerIfPresent(27, ender());
    }

    private static Item card(int cardNumber) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("buddycards", "buddycard_end" + cardNumber));
    }

    private static void registerIfPresent(int cardNumber, CardBattleStats stats) {
        Item item = card(cardNumber);
        if (item == Items.AIR) {
            LOGGER.warn("Buddy Battles: end set card {} not found in Buddycards' item registry (version mismatch?) - skipping.", cardNumber);
            return;
        }
        CardBattleStatsRegistry.register(item, stats);
    }

    // 1: Enrok -- "On play, 50% chance to deal 1 damage to the opposing card (or player, if empty)."
    private static CardBattleStats enrok() {
        return CardBattleStats.of(3, 3, new BattleAbility(BattleEvent.PLAYED, "cheese_toss", (game, slot, target, source) -> {
            if (BattleBoard.RANDOM.nextBoolean()) {
                int opp = BattleGame.opposite(slot);
                game.directAttack(opp, slot, 1);
                List<IBattleIcon> icons = List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.damageIcon(2));
                if (!game.getCard(opp).isEmpty()) {
                    game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.cheese_toss.log1").append(Component.translatable(game.getCard(opp).getDescriptionId())).append(Component.translatable("battles.ability.buddycards.cheese_toss.log2")), icons), slot, LoggedEvent.ATTACK_ID);
                    game.updatePower(opp);
                } else {
                    game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.cheese_toss.log1").append(BattleGame.getOwner(slot) ? game.board.getName2() : game.board.getName1()).append(Component.translatable("battles.ability.buddycards.cheese_toss.log2")), icons), slot, LoggedEvent.ATTACK_ID);
                }
            }
            return true;
        }));
    }

    // 2: Chester -- "Activate: for 1 energy, draw a card."
    private static CardBattleStats chester() {
        return CardBattleStats.of(4, 2, new BattleAbility(BattleEvent.ACTIVATED, "take_inventory", (game, slot, target, source) -> {
            boolean p1 = BattleGame.getOwner(slot);
            if (game.board.energy(p1) >= 1) {
                if (!game.board.tryDrawCard(p1)) return false;
                game.board.spendEnergy(p1, 1);
                game.board.addLog(new BattleComponent(Component.literal("").append(p1 ? game.board.getName1() : game.board.getName2()).append(Component.translatable("battles.ability.buddycards.take_inventory.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.energyIcon(1), TextureBattleIcon.dividerIcon, TextureBattleIcon.drawIcon)), slot, LoggedEvent.CIRCLE_ID);
                return true;
            }
            return false;
        }));
    }

    // 3: Scales -- "On play, set Airborne."
    private static CardBattleStats scales() {
        return CardBattleStats.of(4, 2, new BattleAbility(BattleEvent.PLAYED, "floating_membrane", (game, slot, target, source) -> {
            game.state[slot].status = BattleStatusEffect.AIRBORNE;
            game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.floating_membrane.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.statusIcon(BattleStatusEffect.AIRBORNE))), slot, LoggedEvent.BUFF_ID);
            return true;
        }));
    }

    // 4: Vwoop -- "When Vwoop is attacked, Vwoop will try to move to a location without an opposing buddy."
    private static CardBattleStats vwoop() {
        return CardBattleStats.of(6, 4, new BattleAbility(BattleEvent.DAMAGED, "teleport_away", (game, slot, target, source) -> {
            if (game.getCard(source).isEmpty() || BattleGame.getOwner(source) == BattleGame.getOwner(slot)) return true;
            if (game.turnPower[slot] <= 0) return true; // a lethal hit kills it before it can teleport
            for (int i : BattleEvent.Distribution.ROW.apply(slot, game)) {
                if (i != slot && game.getCard(i).isEmpty() && game.getCard(BattleGame.opposite(i)).isEmpty()) {
                    ItemStack moved = game.moveCard(slot, i);
                    game.updatePower(i);
                    game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.teleport_away.log"),
                            List.of(BuddycardBattleIcon.create(moved), TextureBattleIcon.dividerIcon, TextureBattleIcon.returnIcon)));
                    // false: the old slot is now empty, so skip the damage bookkeeping for it
                    return false;
                }
            }
            return true;
        }));
    }

    // 5: Silver -- "When damaged, try to find another Silver in your deck and draw it."
    private static CardBattleStats silver() {
        return CardBattleStats.of(1, 1, new BattleAbility(BattleEvent.DAMAGED, "silver_swarm", (game, slot, target, source) -> {
            boolean p1 = BattleGame.getOwner(slot);
            if (game.board.tryTutorCard(p1, stack ->
                    stack.getItem() instanceof BuddycardItem bc && bc.getSet() == BuddycardsItems.END_SET && bc.getCardNumber() == 5))
                game.board.addLog(new BattleComponent(Component.literal("").append(p1 ? game.board.getName1() : game.board.getName2()).append(Component.translatable("battles.ability.buddycards.silver_swarm.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, TextureBattleIcon.drawIcon)), slot, LoggedEvent.CIRCLE_ID);
            return true;
        }));
    }

    // 6: Libraria -- "At the start of your turn, draw a card."
    private static CardBattleStats libraria() {
        return CardBattleStats.of(6, 4, new BattleAbility(BattleEvent.TURN, "endless_knowledge", (game, slot, target, source) -> {
            boolean p1 = BattleGame.getOwner(slot);
            if (game.board.tryDrawCard(p1))
                game.board.addLog(new BattleComponent(Component.literal("").append(p1 ? game.board.getName1() : game.board.getName2()).append(Component.translatable("battles.ability.buddycards.endless_knowledge.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, TextureBattleIcon.drawIcon)), slot, LoggedEvent.CIRCLE_ID);
            return true;
        }));
    }

    // 7: Airow -- "Activate: deal 1 damage to the opposing card (or player, if empty), then this card leaves play."
    private static CardBattleStats airow() {
        return CardBattleStats.of(1, 1, new BattleAbility(BattleEvent.ACTIVATED, "fire_away", (game, slot, target, source) -> {
            int opp = BattleGame.opposite(slot);
            game.directAttack(opp, slot, 1);
            List<IBattleIcon> icons = List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.xIcon, TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.damageIcon(1));
            if (!game.getCard(opp).isEmpty()) {
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.fire_away.log1").append(Component.translatable(game.getCard(opp).getDescriptionId())).append(Component.translatable("battles.ability.buddycards.fire_away.log2")), icons), slot, LoggedEvent.ATTACK_ID);
            } else {
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.fire_away.log1").append(BattleGame.getOwner(slot) ? game.board.getName2() : game.board.getName1()).append(Component.translatable("battles.ability.buddycards.fire_away.log2")), icons), slot, LoggedEvent.ATTACK_ID);
            }
            game.removeCard(slot);
            game.updatePower(opp);
            return true;
        }));
    }

    // 8: Webster -- "When damaged, Stun the attacker."
    private static CardBattleStats webster() {
        return CardBattleStats.of(2, 1, new BattleAbility(BattleEvent.DAMAGED, "sticky_situation", (game, slot, target, source) -> {
            if (source != slot && !game.getCard(source).isEmpty()) {
                game.state[source].status = BattleStatusEffect.STUNNED;
                game.board.addLog(new BattleComponent(Component.translatable(game.getCard(source).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.sticky_situation.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(source)), TextureBattleIcon.statusIcon(BattleStatusEffect.STUNNED))), slot, LoggedEvent.SHAKE_ID);
            }
            return true;
        }));
    }

    // 9: Perl -- "Activate: teleport to an empty slot elsewhere in your row."
    private static CardBattleStats perl() {
        return CardBattleStats.of(2, 1, new BattleAbility(BattleEvent.ACTIVATED, "bounce_and_swap", (game, slot, target, source) -> {
            boolean owner = BattleGame.getOwner(slot);
            if (game.board.energy(owner) < 2) return false;
            ItemStack returned = game.returnCard(slot);
            if (returned.isEmpty()) return false; // no room in hand - nothing happened, no cost paid
            game.board.spendEnergy(owner, 2);

            List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(returned), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(returned), TextureBattleIcon.returnIcon, TextureBattleIcon.energyIcon(1)));
            ItemStack[] hand = game.board.getHand(owner);
            List<Integer> nonEmpty = new ArrayList<>();
            for (int i = 0; i < hand.length; i++) if (!hand[i].isEmpty() && !hand[i].is(card(9))) nonEmpty.add(i);
            if (!nonEmpty.isEmpty()) {
                int idx = nonEmpty.get(BattleBoard.RANDOM.nextInt(nonEmpty.size()));
                ItemStack placed = game.board.removeHandCard(owner, idx);
                game.addCard(slot, placed);
                icons.add(TextureBattleIcon.playIcon);
                icons.add(BuddycardBattleIcon.create(placed));
            }
            game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddybattles.bounce_and_swap.log"), icons), slot, LoggedEvent.CIRCLE_ID);
            return true;
        }));
    }

    // 10: Frooty -- "On death, each ally gains 1 Power and is teleported to a random spot."
    private static CardBattleStats frooty() {
        return CardBattleStats.of(3, 1, new BattleAbility(BattleEvent.DEATH, "tasty_teleportation", (game, slot, target, source) -> {
            List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
            List<Integer> allies = new ArrayList<>();
            for (int i : BattleEvent.Distribution.ROW.apply(slot, game)) {
                if (i != slot && !game.getCard(i).isEmpty()) allies.add(i);
            }
            for (int i : allies) {
                game.turnPower[i]++;
                icons.add(BuddycardBattleIcon.create(game.getCard(i)));
            }
            for (int i : allies) {
                List<Integer> emptySlots = new ArrayList<>();
                for (int j : BattleEvent.Distribution.ROW.apply(slot, game)) {
                    if (game.getCard(j).isEmpty()) emptySlots.add(j);
                }
                if (!emptySlots.isEmpty()) {
                    int dest = emptySlots.get(BattleBoard.RANDOM.nextInt(emptySlots.size()));
                    game.moveCard(i, dest);
                }
            }
            if (!allies.isEmpty()) {
                icons.add(TextureBattleIcon.addIcon(1));
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddybattles.tasty_teleportation.log"), icons), slot, LoggedEvent.BUFF_ID);
                game.updatePower();
            }
            return true;
        }));
    }

    // 11: Rod -- "On death, -1 power to every enemy monster buddy."
    private static CardBattleStats rod() {
        return CardBattleStats.of(2, 1, new BattleAbility(BattleEvent.DEATH, "mystical_glow", (game, slot, target, source) -> {
            List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
            for (int i : BattleEvent.Distribution.ALL_ENEMY.apply(slot, game)) {
                if (game.getCard(i).is(CardTags.MONSTER)) {
                    game.turnPower[i]--;
                    icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                }
            }
            if (icons.size() > 2) {
                icons.add(TextureBattleIcon.subtractIcon(1));
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.mystical_glow.log"), icons), slot, LoggedEvent.SHAKE_ID);
                game.updatePower();
            }
            return true;
        }));
    }

    // 12: Rokit -- "Activate: -1 power to every other card in play, then this card leaves play."
    private static CardBattleStats rokit() {
        return CardBattleStats.of(2, 2, new BattleAbility(BattleEvent.ACTIVATED, "big_bang", (game, slot, target, source) -> {
            List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
            for (int i : BattleEvent.Distribution.ALL.apply(slot, game)) {
                if (i != slot) {
                    game.turnPower[i]--;
                    icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                }
            }
            if (icons.size() > 2) {
                icons.add(TextureBattleIcon.subtractIcon(1));
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.big_bang.log"), icons), slot, LoggedEvent.SHAKE_ID);
                game.updatePower();
                game.removeCard(slot);
            }
            return true;
        }));
    }

    // 14: Chorus Flower -- "On play, repeatedly try to find and draw every Chorus Plant (card 16) in your deck."
    private static CardBattleStats flouer() {
        return CardBattleStats.of(2, 1, new BattleAbility(BattleEvent.PLAYED, "chorus_bloom", (game, slot, target, source) -> {
            boolean p1 = BattleGame.getOwner(slot);
            List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
            while (game.board.tryTutorCard(p1, stack ->
                    stack.getItem() instanceof BuddycardItem bc && bc.getSet() == BuddycardsItems.END_SET && bc.getCardNumber() == 16))
                icons.add(TextureBattleIcon.drawIcon);
            if (icons.size() > 2)
                game.board.addLog(new BattleComponent(Component.literal("").append(p1 ? game.board.getName1() : game.board.getName2()).append(Component.translatable("battles.ability.buddycards.chorus_bloom.log")), icons), slot, LoggedEvent.CIRCLE_ID);
            return true;
        }));
    }

    // 13: Wally -- "When an ally dies, return it to your hand instead."
    private static CardBattleStats wally() {
        return CardBattleStats.of(8, 6, new BattleAbility(BattleEvent.OBSERVE_DEATH, "returned_home", (game, slot, target, source) -> {
            if (target == slot || BattleGame.getOwner(target) != BattleGame.getOwner(slot)) return true;
            ItemStack returned = game.returnCard(target);
            if (!returned.isEmpty()) {
                game.board.addLog(new BattleComponent(Component.translatable(returned.getDescriptionId()).append(Component.translatable("battles.ability.buddybattles.returned_home.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(returned), TextureBattleIcon.returnIcon)), slot, LoggedEvent.CIRCLE_ID);
                return false; // cancels the death - already handled via returnCard instead
            }
            return true;
        }));
    }

    // 15: Lingerer -- "When this dies, return it to your hand."
    private static CardBattleStats lingerer() {
        return CardBattleStats.of(2, 1, new BattleAbility(BattleEvent.DEATH, "lingering", (game, slot, target, source) -> {
            ItemStack returned = game.returnCard(slot);
            if (!returned.isEmpty()) {
                game.board.addLog(new BattleComponent(Component.translatable(returned.getDescriptionId()).append(Component.translatable("battles.ability.buddybattles.lingering.log")), List.of(BuddycardBattleIcon.create(returned), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(returned), TextureBattleIcon.returnIcon)), slot, LoggedEvent.SHAKE_ID);
                return false;
            }
            return true;
        }));
    }

    // 16: Chorus -- "When this enters, all Chorus cards gain 2 Power."
    private static CardBattleStats chorus() {
        return CardBattleStats.of(2, 1, new BattleAbility(BattleEvent.PLAYED, "chorus_harmony", (game, slot, target, source) -> {
            List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
            for (int i : BattleEvent.Distribution.ROW.apply(slot, game)) {
                if (!game.getCard(i).isEmpty() && game.getCard(i).is(card(16))) {
                    game.turnPower[i] += 2;
                    icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                }
            }
            icons.add(TextureBattleIcon.addIcon(1));
            game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddybattles.chorus_harmony.log"), icons), slot, LoggedEvent.BUFF_ID);
            game.updatePower();
            return true;
        }));
    }

    // 17: Shulk -- "When this enters, it gains 1 Power for each other card in your hand and Resistance."
    private static CardBattleStats shulk() {
        return CardBattleStats.of(4, 2, new BattleAbility(BattleEvent.PLAYED, "shulking", (game, slot, target, source) -> {
            boolean owner = BattleGame.getOwner(slot);
            int handCount = 0;
            for (ItemStack stack : game.board.getHand(owner)) if (!stack.isEmpty()) handCount++;
            game.turnPower[slot] += handCount;
            game.state[slot].status = BattleStatusEffect.RESISTANCE;
            game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddybattles.shulking.log1").append("" + handCount).append(Component.translatable("battles.ability.buddybattles.shulking.log2")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.addIcon(handCount), TextureBattleIcon.statusIcon(BattleStatusEffect.RESISTANCE))), slot, LoggedEvent.BUFF_ID);
            game.updatePower();
            return true;
        }));
    }

    // 18: Menace -- "Airborne. When this unit fights, it gains Airborne."
    private static CardBattleStats menace() {
        return new CardBattleStats(5, 3, new BattleAbility.Builder()
                .add(new BattleAbility(BattleEvent.PLAYED, "menacing", (game, slot, target, source) -> {
                    game.state[slot].status = BattleStatusEffect.AIRBORNE;
                    game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.flying_menace.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.statusIcon(BattleStatusEffect.AIRBORNE))), slot, LoggedEvent.BUFF_ID);
                    return true;
                }))
                .add(new BattleAbility(BattleEvent.FIGHT, "insomnia", (game, slot, target, source) -> {
                    game.state[slot].status = BattleStatusEffect.AIRBORNE;
                    game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.insomnia.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.statusIcon(BattleStatusEffect.AIRBORNE))), slot, LoggedEvent.BUFF_ID);
                    return true;
                })).build());
    }

    // 19: Visionary -- "When this enters, draw a card."
    private static CardBattleStats visionary() {
        return CardBattleStats.of(5, 4, new BattleAbility(BattleEvent.PLAYED, "foresight", (game, slot, target, source) -> {
            boolean owner = BattleGame.getOwner(slot);
            if (game.board.tryDrawCard(owner)) {
                game.board.addLog(new BattleComponent(Component.literal("").append(owner ? game.board.getName1() : game.board.getName2()).append(Component.translatable("battles.ability.buddybattles.foresight.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, TextureBattleIcon.drawIcon)), slot, LoggedEvent.CIRCLE_ID);
            }
            return true;
        }));
    }

    // 20: Sleeper -- "When this enters, it falls Asleep. When this dies, deal 2 damage to all creatures + opponent."
    private static CardBattleStats sleeper() {
        return new CardBattleStats(5, 6, new BattleAbility.Builder()
                .add(new BattleAbility(BattleEvent.PLAYED, "falls_asleep", (game, slot, target, source) -> {
                    game.state[slot].status = BattleStatusEffect.SLEEP;
                    game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddybattles.falls_asleep.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, TextureBattleIcon.statusIcon(BattleStatusEffect.SLEEP))), slot, LoggedEvent.CIRCLE_ID);
                    return true;
                }))
                .add(new BattleAbility(BattleEvent.DEATH, "sleeper_nova", (game, slot, target, source) -> {
                    List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
                    icons.addAll(CardEffects.dealAoeDamage(game, slot, 2));
                    icons.add(TextureBattleIcon.damageIcon(2));
                    game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddybattles.sleeper_nova.log"), icons), slot, LoggedEvent.SHAKE_ID);
                    game.updatePower();
                    return true;
                })).build());
    }

    private static CardBattleStats spectre() {
        return CardBattleStats.of(8, 5, new BattleAbility(BattleEvent.PLAYED, "haunting", (game, slot, target, source) -> {
            for (int i : BattleEvent.Distribution.ROW.apply(slot, game)) {
                if (i == slot || game.getCard(i).isEmpty()) continue;
                game.board.addLog(new BattleComponent(Component.translatable(game.getCard(i).getDescriptionId()).append(Component.translatable("battles.ability.buddybattles.haunting.log1")).append(Component.translatable(game.getCard(slot).getDescriptionId())).append(Component.translatable("battles.ability.buddybattles.haunting.log2")), List.of(BuddycardBattleIcon.create(game.getCard(i)), TextureBattleIcon.deathIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.addIcon(5))), slot, LoggedEvent.BUFF_ID);
                game.state[i].power = 0;
                game.turnPower[i] = 0;
                game.updatePower(i);
                game.turnPower[slot] += 5;
                game.updatePower(slot);
                break;
            }
            return true;
        }));
    }

    // 22: Package -- "When this card dies, put a random card from your hand onto the field."
    private static CardBattleStats card_package() {
        return new CardBattleStats(2, 2, new BattleAbility.Builder()
                .add(new BattleAbility(BattleEvent.DEATH, "package_surprise", (game, slot, target, source) -> {
                    boolean owner = BattleGame.getOwner(slot);
                    if (game.board.energy(owner) < 2) return false;
                    ItemStack returned = game.returnCard(slot);
                    if (returned.isEmpty()) return false; // no room in hand - nothing happened, no cost paid
                    game.board.spendEnergy(owner, 2);

                    List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(returned), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(returned), TextureBattleIcon.returnIcon, TextureBattleIcon.energyIcon(1)));
                    ItemStack[] hand = game.board.getHand(owner);
                    List<Integer> nonEmpty = new ArrayList<>();
                    for (int i = 0; i < hand.length; i++) if (!hand[i].isEmpty() && !hand[i].is(card(9))) nonEmpty.add(i);
                    if (!nonEmpty.isEmpty()) {
                        int idx = nonEmpty.get(BattleBoard.RANDOM.nextInt(nonEmpty.size()));
                        ItemStack placed = game.board.removeHandCard(owner, idx);
                        game.addCard(slot, placed);
                        icons.add(TextureBattleIcon.playIcon);
                        icons.add(BuddycardBattleIcon.create(placed));
                    }
                    game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddybattles.surprise_package.log"), icons), slot, LoggedEvent.CIRCLE_ID);
                    return true;
                })).build());
    }

    // 23: Brok -- "This unit's Power cannot fall below 1. When this unit would take damage, you lose 2 HP."
    private static CardBattleStats brok() {
        return CardBattleStats.of(5, 1, new BattleAbility(BattleEvent.DAMAGED, "redirected_pain", (game, slot, target, source) -> {
            if (game.turnPower[slot] < 1) game.turnPower[slot] = 1;
            boolean owner = BattleGame.getOwner(slot);
            game.damagePlayer(owner, 2);
            game.board.addLog(new BattleComponent(Component.literal("").append(owner ? game.board.getName1() : game.board.getName2()).append(Component.translatable("battles.ability.buddybattles.redirected_pain.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, TextureBattleIcon.damageIcon(2))), slot, LoggedEvent.SHAKE_ID);
            return true;
        }));
    }

    // 24: Portz -- "When this enters, a random other card from your hand is placed on the field."
    private static CardBattleStats portz() {
        return CardBattleStats.of(8, 5, new BattleAbility(BattleEvent.PLAYED, "portal", (game, slot, target, source) -> {
            boolean owner = BattleGame.getOwner(slot);
            int emptyFieldSlot = -1;
            for (int i : BattleEvent.Distribution.ROW.apply(slot, game)) {
                if (game.getCard(i).isEmpty()) {
                    emptyFieldSlot = i;
                    break;
                }
            }
            if (emptyFieldSlot == -1) return true;

            ItemStack[] hand = game.board.getHand(owner);
            List<Integer> nonEmpty = new ArrayList<>();
            for (int i = 0; i < hand.length; i++) if (!hand[i].isEmpty()) nonEmpty.add(i);
            if (nonEmpty.isEmpty()) return true;

            int idx = nonEmpty.get(BattleBoard.RANDOM.nextInt(nonEmpty.size()));
            ItemStack placed = game.board.removeHandCard(owner, idx);
            game.addCard(emptyFieldSlot, placed);
            game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddybattles.portal.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(placed), TextureBattleIcon.playIcon)), slot, LoggedEvent.CIRCLE_ID);
            return true;
        }));
    }

    // 25: Crystal -- "Activate: 2 Energy - Gain Power equal to the number of cards in your hand."
    private static CardBattleStats crystal() {
        return CardBattleStats.of(3, 2, new BattleAbility(BattleEvent.ACTIVATED, "crystal_power", (game, slot, target, source) -> {
            boolean owner = BattleGame.getOwner(slot);
            if (game.board.energy(owner) < 2) return false;
            game.board.spendEnergy(owner, 2);
            int handCount = 0;
            for (ItemStack stack : game.board.getHand(owner)) if (!stack.isEmpty()) handCount++;
            game.turnPower[slot] += handCount;
            game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddybattles.crystal_power.log1").append("" + handCount).append(Component.translatable("battles.ability.buddybattles.crystal_power.log2")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, TextureBattleIcon.energyIcon(2), BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.addIcon(handCount))), slot, LoggedEvent.BUFF_ID);
            game.updatePower();
            return true;
        }));
    }

    // 26: Glider -- "Airborne. When this enters, a random ally gain Airborne."
    private static CardBattleStats glider() {
        return CardBattleStats.of(4, 2, new BattleAbility(BattleEvent.PLAYED, "gliding", (game, slot, target, source) -> {
            game.state[slot].status = BattleStatusEffect.AIRBORNE;
            List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.statusIcon(BattleStatusEffect.AIRBORNE)));
            List<Integer> candidates = new ArrayList<>();
            for (int i : BattleEvent.Distribution.ROW.apply(slot, game)) {
                if (i != slot && !game.getCard(i).isEmpty()) candidates.add(i);
            }
            if (!candidates.isEmpty()) {
                int ally = candidates.get(BattleBoard.RANDOM.nextInt(candidates.size()));
                game.state[ally].status = BattleStatusEffect.AIRBORNE;
                icons.add(BuddycardBattleIcon.create(game.getCard(ally)));
                icons.add(TextureBattleIcon.statusIcon(BattleStatusEffect.AIRBORNE));
            }
            game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddybattles.gliding.log"), icons), slot, LoggedEvent.BUFF_ID);
            return true;
        }));
    }

    // 27: Ender -- "When this enters, destroy all other units. Airborne. When this unit dies, you lose 2 HP."
    private static CardBattleStats ender() {
        return new CardBattleStats(10, 9, new BattleAbility.Builder()
                .add(new BattleAbility(BattleEvent.PLAYED, "annihilation", (game, slot, target, source) -> {
                    game.state[slot].status = BattleStatusEffect.AIRBORNE;
                    List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
                    for (int i = 0; i < 6; i++) {
                        if (i != slot && !game.getCard(i).isEmpty()) {
                            icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                            game.state[i].power = 0;
                            game.turnPower[i] = 0;
                            game.updatePower(i);
                        }
                    }
                    icons.add(TextureBattleIcon.deathIcon);
                    game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddybattles.annihilation.log"), icons), slot, LoggedEvent.SHAKE_ID);
                    return true;
                }))
                .add(new BattleAbility(BattleEvent.DEATH, "ender_fades", (game, slot, target, source) -> {
                    boolean owner = BattleGame.getOwner(slot);
                    game.damagePlayer(owner, 4);
                    game.board.addLog(new BattleComponent(Component.literal("").append(owner ? game.board.getName1() : game.board.getName2()).append(Component.translatable("battles.ability.buddybattles.ender_fades.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, TextureBattleIcon.damageIcon(4))), slot, LoggedEvent.SHAKE_ID);
                    return true;
                })).build());
    }
}
