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

/** All 27 Nether-set cards. */
public class NetherSetCards {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register() {
        registerIfPresent(1, brik());
        registerIfPresent(2, obby());
        registerIfPresent(3, brite());
        registerIfPresent(4, slinky());
        registerIfPresent(5, bigPig());
        registerIfPresent(6, sol());
        registerIfPresent(7, baisal());
        registerIfPresent(8, vinnie());
        registerIfPresent(9, shroom());
        registerIfPresent(10, warpNye());
        registerIfPresent(11, fya());
        registerIfPresent(12, magma());
        registerIfPresent(13, skully());
        registerIfPresent(14, blazt());
        registerIfPresent(15, quazi());
        registerIfPresent(16, hotPot());
        registerIfPresent(17, crybaby());
        registerIfPresent(18, fyo());
        registerIfPresent(19, lamp());
        registerIfPresent(20, ghost());
        registerIfPresent(21, stride());
        registerIfPresent(22, bartender());
        registerIfPresent(23, port());
        registerIfPresent(24, triple());
        registerIfPresent(25, anker());
        registerIfPresent(26, beaker());
        registerIfPresent(27, netherite());
    }

    private static Item card(int cardNumber) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("buddycards", "buddycard_nether" + cardNumber));
    }

    private static void registerIfPresent(int cardNumber, CardBattleStats stats) {
        Item item = card(cardNumber);
        if (item == Items.AIR) {
            LOGGER.warn("Buddy Battles: nether set card {} not found in Buddycards' item registry (version mismatch?) - skipping.", cardNumber);
            return;
        }
        CardBattleStatsRegistry.register(item, stats);
    }

    // 1: Brik -- "On play, deal 2 damage to the opposing card (or player, if empty)."
    private static CardBattleStats brik() {
        return CardBattleStats.of(4, 2, new BattleAbility(BattleEvent.PLAYED, "brick_toss", (game, slot, target, source) -> {
            int opp = BattleGame.opposite(slot);
            game.directAttack(opp, slot, 2);
            List<IBattleIcon> icons = List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.damageIcon(2));
            if (!game.getCard(opp).isEmpty()) {
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.brick_toss.log1").append(Component.translatable(game.getCard(opp).getDescriptionId())).append(Component.translatable("battles.ability.buddycards.brick_toss.log2")), icons), slot, LoggedEvent.ATTACK_ID);
                game.updatePower(opp);
            } else {
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.brick_toss.log1").append(BattleGame.getOwner(slot) ? game.board.getName2() : game.board.getName1()).append(Component.translatable("battles.ability.buddycards.brick_toss.log2")), icons), slot, LoggedEvent.ATTACK_ID);
            }
            return true;
        }));
    }

    // 2: Obby -- "At the start of your turn, if this card's power this turn is below 3, raise it to 3."
    private static CardBattleStats obby() {
        return CardBattleStats.of(5, 3, new BattleAbility(BattleEvent.TURN, "tough_stuff", (game, slot, target, source) -> {
            int power = 3 - game.turnPower[slot];
            if (power > 0) {
                game.turnPower[slot] += power;
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.tough_stuff.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.addIcon(power))), slot, LoggedEvent.BUFF_ID);
                game.updatePower();
            }
            return true;
        }));
    }

    // 3: Brite -- "At the start of your turn, monster buddies lose 1 power."
    private static CardBattleStats brite() {
        return CardBattleStats.of(2, 2, new BattleAbility(BattleEvent.TURN, "radiant_glow", (game, slot, target, source) -> {
            List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
            for (int i : BattleEvent.Distribution.ALL_ENEMY.apply(slot, game)) {
                if (game.getCard(i).is(CardTags.MONSTER)) {
                    icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                    game.directAttack(i, slot, 1, false, false);
                }
            }
            if (icons.size() > 2) {
                icons.add(TextureBattleIcon.subtractIcon(1));
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.radiant_glow.log"), icons), slot, LoggedEvent.SHAKE_ID);
                game.updatePower();
            }
            return true;
        }));
    }

    // 4: Slinky -- "On death, return this card to your hand instead."
    private static CardBattleStats slinky() {
        return CardBattleStats.of(3, 2, new BattleAbility(BattleEvent.DEATH, "bounce_back", (game, slot, target, source) -> {
            ItemStack returned = game.returnCard(slot);
            if (!returned.isEmpty()) {
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.bounce_back"), List.of(BuddycardBattleIcon.create(returned), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(returned), TextureBattleIcon.returnIcon)), slot, LoggedEvent.CIRCLE_ID);
                return false;
            }
            return true;
        }));
    }

    // 5: Big Pig -- "On death, heal your health by 2 and give your row Strength."
    private static CardBattleStats bigPig() {
        return CardBattleStats.of(5, 3, new BattleAbility(BattleEvent.DEATH, "hearty_bacon", (game, slot, target, source) -> {
            if (BattleGame.getOwner(slot)) game.board.health1 += 2;
            else game.board.health2 += 2;
            List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, TextureBattleIcon.healIcon(2)));
            for (int i : BattleEvent.Distribution.ROW.apply(slot, game)) {
                game.state[i].status = BattleStatusEffect.STRENGTH;
                icons.add(BuddycardBattleIcon.create(game.getCard(i)));
            }
            if (icons.size() > 3) icons.add(TextureBattleIcon.statusIcon(BattleStatusEffect.STRENGTH));
            game.board.addLog(new BattleComponent(Component.literal("").append(BattleGame.getOwner(slot) ? game.board.getName1() : game.board.getName2()).append(Component.translatable("battles.ability.buddycards.hearty_bacon.log")), icons), slot, LoggedEvent.BUFF_ID);
            return true;
        }));
    }

    // 6: Sol -- "On kill, heal your health by 1."
    private static CardBattleStats sol() {
        return CardBattleStats.of(3, 3, new BattleAbility(BattleEvent.KILL, "soul_steal", (game, slot, target, source) -> {
            if (BattleGame.getOwner(slot)) game.board.health1++;
            else game.board.health2++;
            game.board.addLog(new BattleComponent(Component.literal("").append(BattleGame.getOwner(slot) ? game.board.getName1() : game.board.getName2()).append(Component.translatable("battles.ability.buddycards.soul_steal.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, TextureBattleIcon.healIcon(1))), slot, LoggedEvent.CIRCLE_ID);
            return true;
        }));
    }

    // 7: Baisal -- "While burning, +2 power in a fight."
    private static CardBattleStats baisal() {
        return CardBattleStats.of(3, 2, new BattleAbility(BattleEvent.FIGHT, "igneous_extrusion", (game, slot, target, source) -> {
            if (game.state[slot].status.equals(BattleStatusEffect.FIRE)) {
                game.turnPower[slot] += 2;
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.igneous_extrusion.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.addIcon(2))), slot, LoggedEvent.SHAKE_ID);
                game.updatePower(slot);
            }
            return true;
        }));
    }

    // 8: Vinnie -- "In a fight, if you have a fungal buddy elsewhere in your row, +1 power (once per turn)."
    private static CardBattleStats vinnie() {
        return CardBattleStats.of(3, 3, new BattleAbility(BattleEvent.FIGHT, "warped_growth", (game, slot, target, source) -> {
            if (game.isP1() != BattleGame.getOwner(slot)) return true;
            for (int i : BattleEvent.Distribution.ROW_OTHER.apply(slot, game)) {
                if (game.getCard(i).is(CardTags.FUNGAL)) {
                    game.turnPower[slot]++;
                    game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.warped_growth.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.addIcon(1))), slot, LoggedEvent.BUFF_ID);
                    game.updatePower(slot);
                    break;
                }
            }
            return true;
        }));
    }

    // 9: Shroom -- "On death, put the card that killed it to Sleep."
    private static CardBattleStats shroom() {
        return CardBattleStats.of(3, 2, new BattleAbility(BattleEvent.DEATH, "warping_spores", (game, slot, target, source) -> {
            if (source != slot && !game.getCard(source).isEmpty()) {
                game.state[source].status = BattleStatusEffect.SLEEP;
                game.board.addLog(new BattleComponent(Component.translatable(game.getCard(source).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.warping_spores.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(source)), TextureBattleIcon.statusIcon(BattleStatusEffect.SLEEP))), slot, LoggedEvent.SHAKE_ID);
            }
            return true;
        }));
    }

    // 10: Warped Nylium -- "On play, if you have a fungal buddy elsewhere in your row, draw a card."
    private static CardBattleStats warpNye() {
        return CardBattleStats.of(2, 1, new BattleAbility(BattleEvent.PLAYED, "spreading_spores", (game, slot, target, source) -> {
            for (int i : BattleEvent.Distribution.ROW_OTHER.apply(slot, game)) {
                if (game.getCard(i).is(CardTags.FUNGAL)) {
                    if (game.board.tryDrawCard(game.isP1()))
                        game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.warped_growth.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, TextureBattleIcon.drawIcon)), slot, LoggedEvent.CIRCLE_ID);
                    break;
                }
            }
            return true;
        }));
    }

    // 11: Fya -- "When damaged, set the attacker on Fire."
    private static CardBattleStats fya() {
        return CardBattleStats.of(1, 1, new BattleAbility(BattleEvent.DAMAGED, "spicy_flames", (game, slot, target, source) -> {
            if (source != slot && !game.getCard(source).isEmpty()) {
                game.state[source].status = BattleStatusEffect.FIRE;
                game.board.addLog(new BattleComponent(Component.translatable(game.getCard(source).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.spicy_flames.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(source)), TextureBattleIcon.statusIcon(BattleStatusEffect.FIRE))), slot, LoggedEvent.SHAKE_ID);
            }
            return true;
        }));
    }

    // 12: Magma Cube -- "On play, set every card that isn't already a fire buddy on Fire."
    private static CardBattleStats magma() {
        return CardBattleStats.of(6, 3, new BattleAbility(BattleEvent.PLAYED, "meltdown", (game, slot, target, source) -> {
            List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
            for (int i : BattleEvent.Distribution.ALL.apply(slot, game)) {
                if (!game.getCard(i).is(CardTags.FIRE)) {
                    game.state[i].status = BattleStatusEffect.FIRE;
                    icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                }
            }
            if (icons.size() > 2) {
                icons.add(TextureBattleIcon.statusIcon(BattleStatusEffect.FIRE));
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.meltdown.log"), icons), slot, LoggedEvent.SHAKE_ID);
                game.updatePower();
            }
            return true;
        }));
    }

    // 13: Skully -- "In a fight, Wither the opposing card."
    private static CardBattleStats skully() {
        return CardBattleStats.of(6, 4, new BattleAbility(BattleEvent.FIGHT, "withering_strike", (game, slot, target, source) -> {
            int opp = BattleGame.opposite(slot);
            if (!game.getCard(opp).isEmpty()) {
                game.state[opp].status = BattleStatusEffect.WITHER;
                game.board.addLog(new BattleComponent(Component.translatable(game.getCard(opp).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.withering_strike.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(opp)), TextureBattleIcon.statusIcon(BattleStatusEffect.WITHER))), slot, LoggedEvent.SHAKE_ID);
            }
            return true;
        }));
    }

    // 14: Blazt -- "Activate: for 1 energy, set Fire and deal 1 damage to the opposing card."
    private static CardBattleStats blazt() {
        return CardBattleStats.of(5, 3, new BattleAbility(BattleEvent.ACTIVATED, "flaming_shot", (game, slot, target, source) -> {
            boolean p1 = BattleGame.getOwner(slot);
            int opp = BattleGame.opposite(slot);
            if (game.board.energy(p1) >= 1 && !game.getCard(opp).isEmpty()) {
                game.board.spendEnergy(p1, 1);
                game.state[opp].status = BattleStatusEffect.FIRE;
                game.directAttack(opp, slot, 1, false, false);
                game.board.addLog(new BattleComponent(Component.translatable(game.getCard(opp).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.flaming_shot.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.energyIcon(1), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(opp)), TextureBattleIcon.statusIcon(BattleStatusEffect.FIRE), TextureBattleIcon.subtractIcon(1))), slot, LoggedEvent.ATTACK_ID);
                game.updatePower(opp);
                return true;
            }
            return false;
        }));
    }

    // 15: Quazi -- "Whenever the opposing card is powered up, deal 1 damage to it."
    private static CardBattleStats quazi() {
        return CardBattleStats.of(3, 3, new BattleAbility(BattleEvent.POWERED, "subtraction", (game, slot, target, source) -> {
            int opp = BattleGame.opposite(slot);
            if (!game.getCard(opp).isEmpty()) {
                game.directAttack(target, slot, 1, false, false);
                game.board.addLog(new BattleComponent(Component.translatable(game.getCard(target).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.subtraction.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(opp)), TextureBattleIcon.subtractIcon(1))), slot, LoggedEvent.ATTACK_ID);
                game.updatePower(target);
            }
            return true;
        }));
    }

    // 16: Hot Pot -- "On death, cure Fire from your row and give those buddies +2 power."
    private static CardBattleStats hotPot() {
        return CardBattleStats.of(1, 1, new BattleAbility(BattleEvent.DEATH, "fire_resistance", (game, slot, target, source) -> {
            List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
            List<Integer> changed = new ArrayList<>();
            for (int i : BattleEvent.Distribution.ROW_OTHER.apply(slot, game)) {
                if (!game.getCard(i).isEmpty() && game.state[i].status.equals(BattleStatusEffect.FIRE)) {
                    game.state[i].status = BattleStatusEffect.EMPTY;
                    game.turnPower[i] += 2;
                    icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                    changed.add(i);
                }
            }
            if (icons.size() > 2) {
                icons.add(TextureBattleIcon.statusIcon(BattleStatusEffect.EMPTY));
                icons.add(TextureBattleIcon.addIcon(2));
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.fire_resistance.log"), icons), slot, LoggedEvent.BUFF_ID);
                for (int i : changed) game.updatePower(i);
            }
            return true;
        }));
    }

    // 17: Crybaby -- "When damaged, +1 power."
    private static CardBattleStats crybaby() {
        return CardBattleStats.of(6, 4, new BattleAbility(BattleEvent.DAMAGED, "tough_tears", (game, slot, target, source) -> {
            game.turnPower[slot]++;
            game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.tough_tears.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.addIcon(1))), slot, LoggedEvent.BUFF_ID);
            game.updatePower(slot);
            return true;
        }));
    }

    // 18: Fyo -- "When damaged, set the attacker on Fire and -1 power to it."
    private static CardBattleStats fyo() {
        return CardBattleStats.of(4, 3, new BattleAbility(BattleEvent.DAMAGED, "spirit_flames", (game, slot, target, source) -> {
            if (source != slot && !game.getCard(source).isEmpty()) {
                game.state[source].status = BattleStatusEffect.FIRE;
                game.turnPower[source]--;
                game.board.addLog(new BattleComponent(Component.translatable(game.getCard(source).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.spirit_flames.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(source)), TextureBattleIcon.statusIcon(BattleStatusEffect.FIRE), TextureBattleIcon.subtractIcon(1))), slot, LoggedEvent.SHAKE_ID);
                game.updatePower(source);
            }
            return true;
        }));
    }

    // 19: Lamp -- "On play, -1 power to every enemy monster buddy."
    private static CardBattleStats lamp() {
        return CardBattleStats.of(4, 3, new BattleAbility(BattleEvent.PLAYED, "soul_light", (game, slot, target, source) -> {
            List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
            for (int i : BattleEvent.Distribution.ALL_ENEMY.apply(slot, game)) {
                if (game.getCard(i).is(CardTags.MONSTER)) {
                    game.turnPower[i]--;
                    icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                }
            }
            if (icons.size() > 2) {
                icons.add(TextureBattleIcon.subtractIcon(2));
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.soul_light.log"), icons), slot, LoggedEvent.SHAKE_ID);
                game.updatePower();
            }
            return true;
        }));
    }

    // 20: Ghost -- "Activate: for 2 energy, set Fire and deal 2 damage to the opposing card."
    private static CardBattleStats ghost() {
        return CardBattleStats.of(7, 5, new BattleAbility(BattleEvent.ACTIVATED, "fire_spitball", (game, slot, target, source) -> {
            boolean p1 = BattleGame.getOwner(slot);
            int opp = BattleGame.opposite(slot);
            if (game.board.energy(p1) >= 2 && !game.getCard(opp).isEmpty()) {
                game.board.spendEnergy(p1, 2);
                game.state[opp].status = BattleStatusEffect.FIRE;
                game.directAttack(opp, slot, 2, false, false);
                game.board.addLog(new BattleComponent(Component.translatable(game.getCard(opp).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.fire_spitball.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.energyIcon(2), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(opp)), TextureBattleIcon.statusIcon(BattleStatusEffect.FIRE), TextureBattleIcon.subtractIcon(2))), slot, LoggedEvent.ATTACK_ID);
                game.updatePower(opp);
                return true;
            }
            return false;
        }));
    }

    // 21: Strider -- "In a fight, if you have a fire buddy elsewhere in your row, deal this card's power
    // in damage directly to the opponent instead."
    private static CardBattleStats stride() {
        return CardBattleStats.of(4, 3, new BattleAbility(BattleEvent.FIGHT, "magmawalk", (game, slot, target, source) -> {
            for (int i : BattleEvent.Distribution.ROW_OTHER.apply(slot, game)) {
                if (game.getCard(i).is(CardTags.FIRE)) {
                    int damage = game.turnPower[slot];
                    game.damagePlayer(!BattleGame.getOwner(slot), damage);
                    game.board.addLog(new BattleComponent(Component.translatable(game.getCard(slot).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.magmawalk.log1")).append("" + damage).append(Component.translatable("battles.ability.buddycards.magmawalk.log2")).append(BattleGame.getOwner(slot) ? game.board.getName2() : game.board.getName1()).append(Component.translatable("battles.ability.buddycards.magmawalk.log3")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.damageIcon(damage))), slot, LoggedEvent.CIRCLE_ID);
                    return false;
                }
            }
            return true;
        }));
    }

    // 22: Bartender -- "On play, give your row a random buff. Activate: for 5 energy, give the opposing
    // row a random debuff."
    private static CardBattleStats bartender() {
        return new CardBattleStats(7, 4, new BattleAbility.Builder()
                .add(new BattleAbility(BattleEvent.PLAYED, "furious_cocktail", (game, slot, target, source) -> {
                    List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
                    for (int i : BattleEvent.Distribution.ROW.apply(slot, game)) {
                        if (!game.getCard(i).isEmpty()) {
                            BattleStatusEffect effect = switch (BattleBoard.RANDOM.nextInt(3)) {
                                case 0 -> BattleStatusEffect.STRENGTH;
                                case 1 -> BattleStatusEffect.REGENERATION;
                                default -> BattleStatusEffect.RESISTANCE;
                            };
                            game.state[i].status = effect;
                            icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                            icons.add(TextureBattleIcon.statusIcon(effect));
                        }
                    }
                    game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.furious_cocktail.log"), icons), slot, LoggedEvent.BUFF_ID);
                    return true;
                }))
                .add(new BattleAbility(BattleEvent.ACTIVATED, "splashy_potion", (game, slot, target, source) -> {
                    boolean p1 = BattleGame.getOwner(slot);
                    if (game.board.energy(p1) >= 5) {
                        List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.energyIcon(5), TextureBattleIcon.dividerIcon));
                        for (int i : BattleEvent.Distribution.ROW_OTHER.apply(slot, game)) {
                            if (!game.getCard(i).isEmpty()) {
                                BattleStatusEffect effect = switch (BattleBoard.RANDOM.nextInt(3)) {
                                    case 0 -> BattleStatusEffect.POISON;
                                    case 1 -> BattleStatusEffect.WEAKNESS;
                                    default -> BattleStatusEffect.WITHER;
                                };
                                game.state[i].status = effect;
                                icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                                icons.add(TextureBattleIcon.statusIcon(effect));
                            }
                        }
                        if (icons.size() > 2) {
                            game.board.spendEnergy(p1, 5);
                            game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.splashy_potion.log"), icons), slot, LoggedEvent.SHAKE_ID);
                            return true;
                        }
                    }
                    return false;
                }))
                .build());
    }

    // 23: Port -- "On play, +1 power to nether buddies in your row. On death, replace itself with a random
    // Nether-set card from your deck."
    private static CardBattleStats port() {
        return new CardBattleStats(9, 6, new BattleAbility.Builder()
                .add(new BattleAbility(BattleEvent.PLAYED, "hellish_welcome", (game, slot, target, source) -> {
                    List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
                    for (int i : BattleEvent.Distribution.ROW.apply(slot, game)) {
                        if (game.getCard(i).getItem() instanceof BuddycardItem bc && bc.getSet() == BuddycardsItems.NETHER_SET) {
                            game.turnPower[i]++;
                            icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                        }
                    }
                    if (icons.size() > 2) {
                        icons.add(TextureBattleIcon.addIcon(1));
                        game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.hellish_welcome.log"), icons), slot, LoggedEvent.BUFF_ID);
                        game.updatePower();
                    }
                    return true;
                }))
                .add(new BattleAbility(BattleEvent.DEATH, "unwelcome_guest", (game, slot, target, source) -> {
                    boolean p1 = BattleGame.getOwner(slot);
                    ItemStack replacement = game.board.removeMatchingFromDeck(p1, stack ->
                            stack.getItem() instanceof BuddycardItem bc && bc.getSet() == BuddycardsItems.NETHER_SET);
                    if (!replacement.isEmpty()) {
                        game.board.addLog(new BattleComponent(Component.translatable(game.getCard(slot).getDescriptionId()).append(Component.translatable("battles.log.buddycards.death")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.deathIcon)), slot, LoggedEvent.CIRCLE_ID);
                        game.removeCard(slot);
                        game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.grand_creation.log"), List.of(BuddycardBattleIcon.create(replacement), TextureBattleIcon.playIcon)), slot, LoggedEvent.CIRCLE_ID);
                        game.updatePower();
                        game.addCard(slot, replacement);
                        return false;
                    }
                    return true;
                }))
                .build());
    }

    // 24: Triple -- "On play, set Airborne. Activate: for 2 energy, Wither and deal 1 damage to the opposing card."
    private static CardBattleStats triple() {
        return new CardBattleStats(6, 4, new BattleAbility.Builder()
                .add(new BattleAbility(BattleEvent.PLAYED, "withering_heights", (game, slot, target, source) -> {
                    game.state[slot].status = BattleStatusEffect.AIRBORNE;
                    game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.withering_heights.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.statusIcon(BattleStatusEffect.AIRBORNE))), slot, LoggedEvent.BUFF_ID);
                    return true;
                }))
                .add(new BattleAbility(BattleEvent.ACTIVATED, "withering_shot", (game, slot, target, source) -> {
                    boolean p1 = BattleGame.getOwner(slot);
                    int opp = BattleGame.opposite(slot);
                    if (game.board.energy(p1) >= 2 && !game.getCard(opp).isEmpty()) {
                        game.board.spendEnergy(p1, 2);
                        game.state[opp].status = BattleStatusEffect.WITHER;
                        game.directAttack(opp, slot, 1, false, false);
                        game.board.addLog(new BattleComponent(Component.translatable(game.getCard(opp).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.withering_shot.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.energyIcon(2), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(opp)), TextureBattleIcon.statusIcon(BattleStatusEffect.WITHER), TextureBattleIcon.subtractIcon(2))), slot, LoggedEvent.ATTACK_ID);
                        game.updatePower(opp);
                        return true;
                    }
                    return false;
                }))
                .build());
    }

    // 25: Anker -- "When an ally dies with power remaining, this card loses 1 power and the dead card
    // revives at 1 power instead. Activate: for 2 energy, +1 power."
    private static CardBattleStats anker() {
        return new CardBattleStats(7, 5, new BattleAbility.Builder()
                .add(new BattleAbility(BattleEvent.OBSERVE_DEATH, "charged_respawn", (game, slot, target, source) -> {
                    if (game.turnPower[slot] > 0) {
                        game.turnPower[slot]--;
                        game.turnPower[target] = 1;
                        game.board.addLog(new BattleComponent(Component.translatable(game.getCard(target).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.charged_respawn.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.subtractIcon(1), BuddycardBattleIcon.create(game.getCard(target)), TextureBattleIcon.equalsIcon(1))), slot, LoggedEvent.CIRCLE_ID);
                        game.updatePower(slot);
                        game.updatePower(target);
                        return false;
                    }
                    return true;
                }))
                .add(new BattleAbility(BattleEvent.ACTIVATED, "respawn_recharge", (game, slot, target, source) -> {
                    boolean p1 = BattleGame.getOwner(slot);
                    if (game.board.energy(p1) >= 2) {
                        game.board.spendEnergy(p1, 2);
                        game.turnPower[slot]++;
                        game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.respawn_recharge.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.energyIcon(2), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.addIcon(1))), slot, LoggedEvent.BUFF_ID);
                        game.updatePower(slot);
                        return true;
                    }
                    return false;
                }))
                .build());
    }

    // 26: Beaker -- "On play, +2 power to allies elsewhere in your row. Activate: for 4 energy, give your
    // row a random buff."
    private static CardBattleStats beaker() {
        return new CardBattleStats(9, 5, new BattleAbility.Builder()
                .add(new BattleAbility(BattleEvent.PLAYED, "beacon_buff", (game, slot, target, source) -> {
                    List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
                    for (int i : BattleEvent.Distribution.ROW_OTHER.apply(slot, game)) {
                        if (!game.getCard(i).isEmpty()) {
                            game.turnPower[i] += 2;
                            icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                        }
                    }
                    if (icons.size() > 2) {
                        icons.add(TextureBattleIcon.addIcon(2));
                        game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.beacon_buff.log"), icons), slot, LoggedEvent.BUFF_ID);
                        game.updatePower();
                    }
                    return true;
                }))
                .add(new BattleAbility(BattleEvent.ACTIVATED, "color_shift", (game, slot, target, source) -> {
                    boolean p1 = BattleGame.getOwner(slot);
                    if (game.board.energy(p1) >= 4) {
                        List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.energyIcon(4), TextureBattleIcon.dividerIcon));
                        BattleStatusEffect effect = switch (BattleBoard.RANDOM.nextInt(3)) {
                            case 0 -> BattleStatusEffect.STRENGTH;
                            case 1 -> BattleStatusEffect.REGENERATION;
                            default -> BattleStatusEffect.RESISTANCE;
                        };
                        for (int i : BattleEvent.Distribution.ROW.apply(slot, game)) {
                            if (!game.getCard(i).isEmpty()) {
                                game.state[i].status = effect;
                                icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                            }
                        }
                        if (icons.size() > 3) {
                            icons.add(TextureBattleIcon.statusIcon(effect));
                            game.board.spendEnergy(p1, 4);
                            game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.color_shift.log"), icons), slot, LoggedEvent.BUFF_ID);
                            return true;
                        }
                    }
                    return false;
                }))
                .build());
    }

    // 27: Netherite -- "On play, buff your row - more if this card is burning or facing a fire buddy.
    // Activate: buff a row buddy, more if it's enchantable, then this card leaves play."
    private static CardBattleStats netherite() {
        return new CardBattleStats(9, 4, new BattleAbility.Builder()
                .add(new BattleAbility(BattleEvent.PLAYED, "ancient_coating", (game, slot, target, source) -> {
                    List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
                    List<Integer> cards = new ArrayList<>();
                    for (int i : BattleEvent.Distribution.ROW.apply(slot, game)) {
                        if (!game.getCard(i).isEmpty()) {
                            cards.add(i);
                            icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                            if (game.state[slot].status.equals(BattleStatusEffect.FIRE) || game.getCard(BattleGame.opposite(i)).is(CardTags.FIRE)) {
                                game.turnPower[i] += 4;
                                icons.add(TextureBattleIcon.addIcon(4));
                            } else {
                                game.turnPower[i] += 2;
                                icons.add(TextureBattleIcon.addIcon(2));
                            }
                        }
                    }
                    if (!cards.isEmpty()) {
                        game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.ancient_coating.log"), icons), slot, LoggedEvent.BUFF_ID);
                        for (int i : cards) game.updatePower(i);
                    }
                    return true;
                }))
                .add(new BattleAbility(BattleEvent.ACTIVATED, "gear_upgrade", (game, slot, target, source) -> {
                    List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.xIcon, TextureBattleIcon.dividerIcon));
                    List<Integer> cards = new ArrayList<>();
                    for (int i : BattleEvent.Distribution.ROW_OTHER.apply(slot, game)) {
                        if (!game.getCard(i).isEmpty()) {
                            cards.add(i);
                            icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                            if (game.getCard(i).is(CardTags.ENCHANTABLE)) {
                                game.turnPower[i] += game.turnPower[slot];
                                icons.add(TextureBattleIcon.addIcon(game.turnPower[slot]));
                            } else {
                                game.turnPower[i] += 1;
                                icons.add(TextureBattleIcon.addIcon(1));
                            }
                        }
                    }
                    if (!cards.isEmpty()) {
                        game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.gear_upgrade"), icons), slot, LoggedEvent.BUFF_ID);
                        game.removeCard(slot);
                        for (int i : cards) game.updatePower(i);
                    }
                    return true;
                }))
                .build());
    }
}
