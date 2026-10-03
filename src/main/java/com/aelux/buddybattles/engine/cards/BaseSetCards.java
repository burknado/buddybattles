package com.aelux.buddybattles.engine.cards;

import com.mojang.datafixers.util.Pair;
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

/** All 36 Base-set cards. */
public class BaseSetCards {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register() {
        registerIfPresent(1, saps());
        registerIfPresent(2, rok());
        registerIfPresent(3, oinke());
        registerIfPresent(4, cole());
        registerIfPresent(5, metalico());
        registerIfPresent(6, drLazuli());
        registerIfPresent(7, betsy());
        registerIfPresent(8, snowbelle());
        registerIfPresent(9, aqua());
        registerIfPresent(10, fortuna());
        registerIfPresent(11, weet());
        registerIfPresent(12, grassling());
        registerIfPresent(13, angstySaps());
        registerIfPresent(14, coppor());
        registerIfPresent(15, aimy());
        registerIfPresent(16, dropper());
        registerIfPresent(17, yang());
        registerIfPresent(18, yin());
        registerIfPresent(19, bling());
        registerIfPresent(20, emerelda());
        registerIfPresent(21, dio());
        registerIfPresent(22, treatsy());
        registerIfPresent(23, red());
        registerIfPresent(24, melony());
        registerIfPresent(25, spoopy());
        registerIfPresent(26, tick());
        registerIfPresent(27, tock());
        registerIfPresent(28, whare());
        registerIfPresent(29, knallEdgy());
        registerIfPresent(30, caster());
        registerIfPresent(31, pisty());
        registerIfPresent(32, knight());
        registerIfPresent(33, ambystoma());
        registerIfPresent(34, kart());
        registerIfPresent(35, creator());
        registerIfPresent(36, meltor());
    }

    private static Item card(int cardNumber) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("buddycards", "buddycard_base" + cardNumber));
    }

    private static void registerIfPresent(int cardNumber, CardBattleStats stats) {
        Item item = card(cardNumber);
        if (item == Items.AIR) {
            LOGGER.warn("Buddy Battles: base set card {} not found in Buddycards' item registry (version mismatch?) - skipping.", cardNumber);
            return;
        }
        CardBattleStatsRegistry.register(item, stats);
    }

    // 1: Saps -- "At the start of your turn, this card gains 1 power."
    private static CardBattleStats saps() {
        return CardBattleStats.of(2, 2, new BattleAbility(BattleEvent.TURN, "growing_up", (game, slot, target, source) -> {
            game.turnPower[slot]++;
            game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.growing_up.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.addIcon(1))), slot, LoggedEvent.BUFF_ID);
            game.updatePower(slot);
            return true;
        }));
    }

    // 2: Rok -- "On play, deal 1 damage to the opposing card (or player, if empty)."
    private static CardBattleStats rok() {
        return CardBattleStats.of(2, 1, new BattleAbility(BattleEvent.PLAYED, "stone_toss", (game, slot, target, source) -> {
            int opp = BattleGame.opposite(slot);
            game.directAttack(opp, slot, 1);
            if (!game.getCard(opp).isEmpty()) {
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.stone_toss.log1").append(Component.translatable(game.getCard(opp).getDescriptionId())).append(Component.translatable("battles.ability.buddycards.stone_toss.log2")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.damageIcon(1), BuddycardBattleIcon.create(game.getCard(opp)))), slot, LoggedEvent.ATTACK_ID);
                game.updatePower(opp);
            } else {
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.stone_toss.log1").append(BattleGame.getOwner(slot) ? game.board.getName2() : game.board.getName1()).append(Component.translatable("battles.ability.buddycards.stone_toss.log2")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.damageIcon(1))), slot, LoggedEvent.ATTACK_ID);
            }
            return true;
        }));
    }

    // 3: Oinke -- "On death, owner gains 2 health."
    private static CardBattleStats oinke() {
        return CardBattleStats.of(3, 3, new BattleAbility(BattleEvent.DEATH, "tasty_bacon", (game, slot, target, source) -> {
            if (BattleGame.getOwner(slot)) game.board.health1 += 2;
            else game.board.health2 += 2;
            game.board.addLog(new BattleComponent(Component.literal("").append(BattleGame.getOwner(slot) ? game.board.getName1() : game.board.getName2()).append(Component.translatable("battles.ability.buddycards.tasty_bacon.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, TextureBattleIcon.healIcon(2))), slot, LoggedEvent.CIRCLE_ID);
            return true;
        }));
    }

    // 4: Cole -- "On death, fire buddies in your row gain 1 power."
    private static CardBattleStats cole() {
        return CardBattleStats.of(3, 2, new BattleAbility(BattleEvent.DEATH, "stoke_flames", (game, slot, target, source) -> {
            List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
            for (int i : BattleEvent.Distribution.ROW.apply(slot, game)) {
                if (game.getCard(i).is(CardTags.FIRE)) {
                    game.turnPower[i]++;
                    icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                }
            }
            if (icons.size() > 2) {
                icons.add(TextureBattleIcon.addIcon(1));
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.stoke_flames.log"), icons), slot, LoggedEvent.BUFF_ID);
                game.updatePower();
            }
            return true;
        }));
    }

    // 5: Metalico -- "On kill, gain 1 power."
    private static CardBattleStats metalico() {
        return CardBattleStats.of(3, 3, new BattleAbility(BattleEvent.KILL, "iron_temper", (game, slot, target, source) -> {
            game.turnPower[slot]++;
            game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.iron_temper.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.addIcon(1))), slot, LoggedEvent.BUFF_ID);
            game.updatePower(slot);
            return true;
        }));
    }

    // 6: Dr. Lazuli -- "On play, enchantable buddies in your row gain 1 power."
    private static CardBattleStats drLazuli() {
        return CardBattleStats.of(4, 3, new BattleAbility(BattleEvent.PLAYED, "simple_enchant", (game, slot, target, source) -> {
            List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
            for (int i : BattleEvent.Distribution.ROW.apply(slot, game)) {
                if (game.getCard(i).is(CardTags.ENCHANTABLE)) {
                    game.turnPower[i]++;
                    icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                }
            }
            if (icons.size() > 2) {
                icons.add(TextureBattleIcon.addIcon(1));
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.simple_enchant.log"), icons), slot, LoggedEvent.BUFF_ID);
                game.updatePower();
            }
            return true;
        }));
    }

    // 7: Betsy -- "Activate: clear status effects from your row."
    private static CardBattleStats betsy() {
        return CardBattleStats.of(4, 2, new BattleAbility(BattleEvent.ACTIVATED, "milk_maid", (game, slot, target, source) -> {
            List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
            for (int i : BattleEvent.Distribution.ROW.apply(slot, game)) {
                if (!game.state[i].status.equals(BattleStatusEffect.EMPTY)) {
                    game.state[i].status = BattleStatusEffect.EMPTY;
                    icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                }
            }
            if (icons.size() > 2) {
                icons.add(TextureBattleIcon.statusIcon(BattleStatusEffect.EMPTY));
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.milk_maid.log"), icons), slot, LoggedEvent.BUFF_ID);
                return true;
            }
            return false;
        }));
    }

    // 8: Snowbelle -- "On play, deal 1 direct damage to the opponent."
    private static CardBattleStats snowbelle() {
        return CardBattleStats.of(1, 1, new BattleAbility(BattleEvent.PLAYED, "snowball_fight", (game, slot, target, source) -> {
            game.damagePlayer(!BattleGame.getOwner(slot), 1);
            game.board.addLog(new BattleComponent(Component.literal("").append(BattleGame.getOwner(slot) ? game.board.getName2() : game.board.getName1()).append(Component.translatable("battles.ability.buddycards.snowball_fight.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.damageIcon(1))), slot, LoggedEvent.ATTACK_ID);
            return true;
        }));
    }

    // 9: Aqua -- "When you play Aqua, all fire buddies lose 1 power and fires are put out."
    private static CardBattleStats aqua() {
        return CardBattleStats.of(1, 1, new BattleAbility(BattleEvent.PLAYED, "wash_out", (game, slot, target, source) -> {
            List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
            List<Integer> changedCards = new ArrayList<>();
            boolean a = false, b = false;
            for (int i : BattleEvent.Distribution.ALL.apply(slot, game)) {
                if (game.getCard(i).is(CardTags.FIRE)) {
                    icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                    game.directAttack(i, slot, 1, false, false);
                    changedCards.add(i);
                    a = true;
                }
            }
            if (a) icons.add(TextureBattleIcon.subtractIcon(1));
            for (int i : BattleEvent.Distribution.ALL.apply(slot, game)) {
                if (game.state[i].status == BattleStatusEffect.FIRE) {
                    icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                    game.state[i].status = BattleStatusEffect.EMPTY;
                    b = true;
                }
            }
            if (a && b) {
                icons.add(TextureBattleIcon.statusIcon(BattleStatusEffect.EMPTY));
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.wash_out.log"), icons), slot, LoggedEvent.SHAKE_ID);
                for (int i : changedCards) game.updatePower(i);
            } else if (b) {
                icons.add(TextureBattleIcon.statusIcon(BattleStatusEffect.EMPTY));
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.wash_out.logb"), icons), slot, LoggedEvent.SHAKE_ID);
                for (int i : changedCards) game.updatePower(i);
            } else if (a) {
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.wash_out.loga"), icons), slot, LoggedEvent.SHAKE_ID);
            }
            return true;
        }));
    }

    // 10: Fortuna -- "On death, owner gains 1 health."
    private static CardBattleStats fortuna() {
        return CardBattleStats.of(1, 1, new BattleAbility(BattleEvent.DEATH, "tasty_treat", (game, slot, target, source) -> {
            if (BattleGame.getOwner(slot)) game.board.health1++;
            else game.board.health2++;
            game.board.addLog(new BattleComponent(Component.literal("").append(BattleGame.getOwner(slot) ? game.board.getName1() : game.board.getName2()).append(Component.translatable("battles.ability.buddycards.tasty_treat.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, TextureBattleIcon.healIcon(1))), slot, LoggedEvent.CIRCLE_ID);
            return true;
        }));
    }

    // 11: Weet -- "On death, animal buddies in your row gain 2 power."
    private static CardBattleStats weet() {
        return CardBattleStats.of(3, 1, new BattleAbility(BattleEvent.DEATH, "fresh_harvest", (game, slot, target, source) -> {
            List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
            for (int i : BattleEvent.Distribution.ROW.apply(slot, game)) {
                if (game.getCard(i).is(CardTags.ANIMAL)) {
                    game.turnPower[i] += 2;
                    icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                }
            }
            if (icons.size() > 2) {
                icons.add(TextureBattleIcon.addIcon(2));
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.fresh_harvest.log"), icons), slot, LoggedEvent.BUFF_ID);
            }
            return true;
        }));
    }

    // 12: Grassling -- "On play, animal buddies in your row gain 1 power."
    private static CardBattleStats grassling() {
        return CardBattleStats.of(2, 2, new BattleAbility(BattleEvent.PLAYED, "feeding_time", (game, slot, target, source) -> {
            List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
            for (int i : BattleEvent.Distribution.ROW.apply(slot, game)) {
                if (game.getCard(i).is(CardTags.ANIMAL)) {
                    game.turnPower[i]++;
                    icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                }
            }
            if (icons.size() > 2) {
                icons.add(TextureBattleIcon.addIcon(1));
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.feeding_time.log"), icons), slot, LoggedEvent.BUFF_ID);
                game.updatePower();
            }
            return true;
        }));
    }

    // 13: Yang -- "On kill, draw a card."
    private static CardBattleStats yang() {
        return CardBattleStats.of(4, 3, new BattleAbility(BattleEvent.KILL, "play_fetch", (game, slot, target, source) -> {
            if (game.board.tryDrawCard(BattleGame.getOwner(slot))) {
                game.board.addLog(new BattleComponent(Component.literal("").append(BattleGame.getOwner(slot) ? game.board.getName1() : game.board.getName2()).append(Component.translatable("battles.ability.buddycards.play_fetch.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, TextureBattleIcon.drawIcon)), slot, LoggedEvent.BUFF_ID);
            }
            return true;
        }));
    }

    // 14: Yin -- "Activate (once): gain 2 power and fall Asleep."
    private static CardBattleStats yin() {
        return CardBattleStats.of(3, 3, new BattleAbility(BattleEvent.ACTIVATED, "lazy_cat", (game, slot, target, source) -> {
            if (game.state[slot].status.equals(BattleStatusEffect.EMPTY)) {
                game.turnPower[slot] += 2;
                game.state[slot].status = BattleStatusEffect.SLEEP;
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.lazy_cat.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.addIcon(2), TextureBattleIcon.statusIcon(BattleStatusEffect.SLEEP))), slot, LoggedEvent.BUFF_ID);
                game.updatePower(slot);
            }
            return true;
        }));
    }

    // 15: Bling -- "On play, food buddies in your row gain 1 power."
    private static CardBattleStats bling() {
        return CardBattleStats.of(3, 2, new BattleAbility(BattleEvent.PLAYED, "gilded_gift", (game, slot, target, source) -> {
            List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
            for (int i : BattleEvent.Distribution.ROW.apply(slot, game)) {
                if (game.getCard(i).is(CardTags.FOOD)) {
                    game.turnPower[i]++;
                    icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                }
            }
            if (icons.size() > 2) {
                icons.add(TextureBattleIcon.addIcon(1));
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.gilded_gift.log"), icons), slot, LoggedEvent.BUFF_ID);
                game.updatePower();
            }
            return true;
        }));
    }

    // 16: Emerelda -- "On play, draw cards until your hand is full."
    private static CardBattleStats emerelda() {
        return CardBattleStats.of(3, 1, new BattleAbility(BattleEvent.PLAYED, "rich_trade", (game, slot, target, source) -> {
            List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
            while (game.board.tryDrawCard(BattleGame.getOwner(slot))) icons.add(TextureBattleIcon.drawIcon);
            if (icons.size() > 2) {
                game.board.addLog(new BattleComponent(Component.literal("").append(BattleGame.getOwner(slot) ? game.board.getName1() : game.board.getName2()).append(Component.translatable("battles.ability.buddycards.rich_trade.log")), icons), slot, LoggedEvent.BUFF_ID);
            }
            return true;
        }));
    }

    // 17: Dio -- "When damaged, gain 1 power."
    private static CardBattleStats dio() {
        return CardBattleStats.of(7, 5, new BattleAbility(BattleEvent.DAMAGED, "unbreakable", (game, slot, target, source) -> {
            game.state[slot].power++;
            game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.unbreakable.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.addIcon(1))), slot, LoggedEvent.BUFF_ID);
            game.updatePower(slot);
            return true;
        }));
    }

    // 18: Treatsy -- "On death, owner gains 3 health."
    private static CardBattleStats treatsy() {
        return CardBattleStats.of(4, 3, new BattleAbility(BattleEvent.DEATH, "delicious_desert", (game, slot, target, source) -> {
            if (BattleGame.getOwner(slot)) game.board.health1 += 3;
            else game.board.health2 += 3;
            game.board.addLog(new BattleComponent(Component.literal("").append(BattleGame.getOwner(slot) ? game.board.getName1() : game.board.getName2()).append(Component.translatable("battles.ability.buddycards.delicious_desert.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, TextureBattleIcon.healIcon(3))), slot, LoggedEvent.CIRCLE_ID);
            return true;
        }));
    }

    // 19: Red -- "At the start of your turn, trigger POWERED on adjacent redstone buddies."
    private static CardBattleStats red() {
        return CardBattleStats.of(4, 3, new BattleAbility(BattleEvent.TURN, "energize", (game, slot, target, source) -> {
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
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.energize.log"), icons), slot, LoggedEvent.BUFF_ID);
                for (int i : cards) game.trigger(BattleEvent.POWERED, i, i, slot);
            }
            return true;
        }));
    }

    // 20: Melony -- "On death, owner gains 2 health."
    private static CardBattleStats melony() {
        return CardBattleStats.of(2, 2, new BattleAbility(BattleEvent.DEATH, "sweet_treat", (game, slot, target, source) -> {
            if (BattleGame.getOwner(slot)) game.board.health1 += 2;
            else game.board.health2 += 2;
            game.board.addLog(new BattleComponent(Component.literal("").append(BattleGame.getOwner(slot) ? game.board.getName1() : game.board.getName2()).append(Component.translatable("battles.ability.buddycards.sweet_treat.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, TextureBattleIcon.healIcon(2))), slot, LoggedEvent.CIRCLE_ID);
            return true;
        }));
    }

    // 21: Spoopy -- "When one of your other cards fights, deal it 1 bonus damage against its target."
    private static CardBattleStats spoopy() {
        return CardBattleStats.of(3, 2, new BattleAbility(BattleEvent.FIGHT, "spook_em", (game, slot, target, source) -> {
            if (target != slot && !game.getCard(target).isEmpty() && game.isP1() == BattleGame.getOwner(slot)) {
                game.directAttack(target, slot, 1, false, false);
                game.board.addLog(new BattleComponent(Component.translatable(game.getCard(target).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.spook_em.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(target)), TextureBattleIcon.subtractIcon(1))), slot, LoggedEvent.SHAKE_ID);
                game.updatePower(target);
            }
            return true;
        }));
    }

    // 22: Knall-Edgy -- "On play, enchantable buddies in your row +2 power. On kill, enchantable buddies
    // in your row +1 power."
    private static CardBattleStats knallEdgy() {
        return new CardBattleStats(8, 4, new BattleAbility.Builder()
                .add(new BattleAbility(BattleEvent.PLAYED, "premium_enchant", (game, slot, target, source) -> {
                    List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
                    for (int i : BattleEvent.Distribution.ROW.apply(slot, game)) {
                        if (game.getCard(i).is(CardTags.ENCHANTABLE)) {
                            game.turnPower[i] += 2;
                            icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                        }
                    }
                    if (icons.size() > 2) {
                        icons.add(TextureBattleIcon.addIcon(2));
                        game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.premium_enchant.log"), icons), slot, LoggedEvent.BUFF_ID);
                        game.updatePower();
                    }
                    return true;
                }))
                .add(new BattleAbility(BattleEvent.KILL, "xp_farm", (game, slot, target, source) -> {
                    List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
                    for (int i : BattleEvent.Distribution.ROW.apply(slot, game)) {
                        if (game.getCard(i).is(CardTags.ENCHANTABLE)) {
                            game.turnPower[i]++;
                            icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                        }
                    }
                    if (icons.size() > 2) {
                        icons.add(TextureBattleIcon.addIcon(1));
                        game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.premium_enchant.log"), icons), slot, LoggedEvent.BUFF_ID);
                        game.updatePower();
                    }
                    return true;
                })).build());
    }

    // 23: Caster -- "On play, deal 4 damage to the opposing card. At the start of your turn, adjacent
    // metal buddies +1 power."
    private static CardBattleStats caster() {
        return new CardBattleStats(9, 5, new BattleAbility.Builder()
                .add(new BattleAbility(BattleEvent.PLAYED, "anvil_drop", (game, slot, target, source) -> {
                    int opp = BattleGame.opposite(slot);
                    if (!game.getCard(opp).isEmpty()) {
                        game.directAttack(opp, slot, 4);
                        game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.anvil_drop.log1").append(Component.translatable(game.getCard(opp).getDescriptionId())).append(Component.translatable("battles.ability.buddycards.anvil_drop.log2")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.damageIcon(4), BuddycardBattleIcon.create(game.getCard(opp)))), slot, LoggedEvent.ATTACK_ID);
                        game.updatePower(opp);
                    }
                    return true;
                }))
                .add(new BattleAbility(BattleEvent.TURN, "reforge", (game, slot, target, source) -> {
                    List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
                    for (int i : BattleEvent.Distribution.ADJACENT.apply(slot, game)) {
                        if (game.getCard(i).is(CardTags.METAL)) {
                            game.turnPower[i]++;
                            icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                        }
                    }
                    if (icons.size() > 2) {
                        icons.add(TextureBattleIcon.addIcon(1));
                        game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.reforge.log"), icons), slot, LoggedEvent.BUFF_ID);
                        game.updatePower();
                    }
                    return true;
                })).build());
    }

    // 24: Pisty -- "On play, and when powered, return the opposing card to its owner's hand."
    private static CardBattleStats pisty() {
        return new CardBattleStats(5, 3, new BattleAbility.Builder()
                .add(new BattleAbility(BattleEvent.PLAYED, "push_back", (game, slot, target, source) -> {
                    int opp = BattleGame.opposite(slot);
                    ItemStack returnedCard = game.returnCard(opp);
                    if (!returnedCard.isEmpty()) {
                        game.board.addLog(new BattleComponent(Component.translatable(returnedCard.getDescriptionId()).append(Component.translatable("battles.ability.buddycards.push_back.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(returnedCard), TextureBattleIcon.returnIcon)), slot, LoggedEvent.CIRCLE_ID);
                    }
                    return true;
                }))
                .add(new BattleAbility(BattleEvent.POWERED, "empowered_push", (game, slot, target, source) -> {
                    int opp = BattleGame.opposite(slot);
                    ItemStack returnedCard = game.returnCard(opp);
                    if (!returnedCard.isEmpty()) {
                        game.board.addLog(new BattleComponent(Component.translatable(returnedCard.getDescriptionId()).append(Component.translatable("battles.ability.buddycards.empowered_push.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(returnedCard), TextureBattleIcon.returnIcon)), slot, LoggedEvent.CIRCLE_ID);
                    }
                    return true;
                })).build());
    }

    // 25: Knight -- "When damaged, retaliate 1 damage to the source. Every turn, deal 2 damage to the opposing card."
    private static CardBattleStats knight() {
        return new CardBattleStats(7, 5, new BattleAbility.Builder()
                .add(new BattleAbility(BattleEvent.DAMAGED, "prismarine_spines", (game, slot, target, source) -> {
                    if (!game.getCard(source).isEmpty()) {
                        game.directAttack(source, slot, 1, false, false);
                        game.board.addLog(new BattleComponent(Component.translatable(game.getCard(source).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.prismarine_spines.log")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(source)), TextureBattleIcon.subtractIcon(1))), slot, LoggedEvent.SHAKE_ID);
                        game.updatePower(source);
                    }
                    return true;
                }))
                .add(new BattleAbility(BattleEvent.TURN, "laser_gaze", (game, slot, target, source) -> {
                    int opp = BattleGame.opposite(slot);
                    if (!game.getCard(opp).isEmpty() && (game.board.turn / 2) % 2 == 1) {
                        game.directAttack(opp, slot, 2);
                        game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.laser_gaze.log1").append(Component.translatable(game.getCard(opp).getDescriptionId())).append(Component.translatable("battles.ability.buddycards.laser_gaze.log2")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.damageIcon(2), BuddycardBattleIcon.create(game.getCard(opp)))), slot, LoggedEvent.ATTACK_ID);
                        game.updatePower(opp);
                    }
                    return true;
                })).build());
    }

    // 26: Creator -- "On play, summon random cards from your deck into empty row slots. On death, consume
    // adjacent cards and gain their power instead of dying (once)."
    private static CardBattleStats creator() {
        return new CardBattleStats(8, 4, new BattleAbility.Builder()
                .add(new BattleAbility(BattleEvent.PLAYED, "grand_creation", (game, slot, target, source) -> {
                    List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
                    List<Pair<Integer, ItemStack>> cards = new ArrayList<>();
                    for (int i : BattleEvent.Distribution.ROW.apply(slot, game)) {
                        if (game.getCard(i).isEmpty()) {
                            ItemStack drawn = game.board.removeRandomFromDeck(BattleGame.getOwner(slot));
                            if (drawn.isEmpty()) continue; // deck exhausted, nothing left to summon
                            cards.add(Pair.of(i, drawn));
                            icons.add(BuddycardBattleIcon.create(drawn));
                        }
                    }
                    if (!cards.isEmpty()) {
                        icons.add(TextureBattleIcon.playIcon);
                        game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.grand_creation.log"), icons), slot, LoggedEvent.CIRCLE_ID);
                        game.updatePower();
                        for (Pair<Integer, ItemStack> p : cards) {
                            game.addCard(p.getFirst(), p.getSecond());
                        }
                    }
                    return true;
                }))
                .add(new BattleAbility(BattleEvent.DEATH, "craft_together", (game, slot, target, source) -> {
                    List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
                    int power = 0;
                    for (int i : BattleEvent.Distribution.ADJACENT.apply(slot, game)) {
                        if (!game.getCard(i).isEmpty()) {
                            power += game.turnPower[i];
                            icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                            game.removeCard(i);
                        }
                    }
                    if (power > 0) {
                        game.turnPower[slot] += power;
                        icons.add(TextureBattleIcon.xIcon);
                        icons.add(BuddycardBattleIcon.create(game.getCard(slot)));
                        icons.add(TextureBattleIcon.addIcon(power));
                        game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.craft_together.log1").append("" + power).append(Component.translatable("battles.ability.buddycards.craft_together.log2")), icons), slot, LoggedEvent.SHAKE_ID);
                        game.updatePower();
                        return false;
                    }
                    return true;
                })).build());
    }

    // 27: Meltor -- "On play, sacrifice non-metal row buddies for +2 power each. When any fire-tagged card
    // dies, self and metal row buddies gain 2 power."
    private static CardBattleStats meltor() {
        return new CardBattleStats(6, 4, new BattleAbility.Builder()
                .add(new BattleAbility(BattleEvent.PLAYED, "smeltdown", (game, slot, target, source) -> {
                    List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
                    List<Integer> cards = new ArrayList<>();
                    int power = 0;
                    for (int i : BattleEvent.Distribution.ROW.apply(slot, game)) {
                        if (slot != i && !game.getCard(i).isEmpty() && !game.getCard(i).is(CardTags.METAL)) {
                            cards.add(i);
                            power += 2;
                            icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                        }
                    }
                    if (!cards.isEmpty()) {
                        icons.add(TextureBattleIcon.deathIcon);
                        icons.add(BuddycardBattleIcon.create(game.getCard(slot)));
                        icons.add(TextureBattleIcon.addIcon(power));
                        game.turnPower[slot] += power;
                        game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.smeltdown.log1").append("" + power).append(Component.translatable("battles.ability.buddycards.smeltdown.log2")), icons), slot, LoggedEvent.SHAKE_ID);
                        game.updatePower();
                        for (int i : cards) {
                            if (game.trigger(BattleEvent.DEATH, i)) game.removeCard(i);
                        }
                    }
                    return true;
                }))
                .add(new BattleAbility(BattleEvent.OBSERVE_DEATH, "flaming_forge", (game, slot, target, source) -> {
                    if (!game.getCard(target).is(CardTags.FIRE)) return true;
                    List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
                    for (int i : BattleEvent.Distribution.ROW.apply(slot, game)) {
                        if (i == slot || game.getCard(i).is(CardTags.METAL)) {
                            game.turnPower[i] += 2;
                            icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                        }
                    }
                    if (icons.size() > 2) {
                        icons.add(TextureBattleIcon.addIcon(2));
                        game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.flaming_forge.log"), icons), slot, LoggedEvent.BUFF_ID);
                        game.updatePower();
                    }
                    return true;
                })).build());
    }

    // 28: Angsty Saps -- "At the start of your turn, if both adjacent slots are occupied, gain 2 power."
    private static CardBattleStats angstySaps() {
        return CardBattleStats.of(3, 3, new BattleAbility(BattleEvent.TURN, "thick_thicket", (game, slot, target, source) -> {
            for (int i : BattleEvent.Distribution.ADJACENT.apply(slot, game))
                if (game.getCard(i).isEmpty()) return true;
            game.turnPower[slot] += 2;
            game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.thick_thicket.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.addIcon(2))), slot, LoggedEvent.BUFF_ID);
            game.updatePower(slot);
            return true;
        }));
    }

    // 29: Coppor -- "When powered, gain 1 power."
    private static CardBattleStats coppor() {
        return CardBattleStats.of(2, 2, new BattleAbility(BattleEvent.POWERED, "conduction", (game, slot, target, source) -> {
            game.turnPower[slot]++;
            game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.conduction.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.addIcon(1))), slot, LoggedEvent.BUFF_ID);
            game.updatePower(slot);
            return true;
        }));
    }

    // 30: Aimy -- "On death, other cards in your row gain 1 power."
    private static CardBattleStats aimy() {
        return CardBattleStats.of(1, 1, new BattleAbility(BattleEvent.DEATH, "courage_chime", (game, slot, target, source) -> {
            List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
            List<Integer> cards = new ArrayList<>();
            for (int i : BattleEvent.Distribution.ROW.apply(slot, game)) {
                if (i != slot && !game.getCard(i).isEmpty()) {
                    game.turnPower[i]++;
                    cards.add(i);
                    icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                }
            }
            if (icons.size() > 2) {
                icons.add(TextureBattleIcon.addIcon(1));
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.courage_chime.log"), icons), slot, LoggedEvent.BUFF_ID);
                for (int i : cards) game.updatePower(i);
            }
            return true;
        }));
    }

    // 31: Dropper -- "At the start of your turn, return this card to your hand."
    private static CardBattleStats dropper() {
        return CardBattleStats.of(2, 3, new BattleAbility(BattleEvent.TURN, "drip_drop", (game, slot, target, source) -> {
            ItemStack returnedCard = game.returnCard(slot);
            if (!returnedCard.isEmpty()) {
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.drip_drop.log"), List.of(BuddycardBattleIcon.create(returnedCard), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(returnedCard), TextureBattleIcon.returnIcon)), slot, LoggedEvent.CIRCLE_ID);
            }
            return true;
        }));
    }

    // 32: Tick -- "When powered: below 8 power, gain 1 power and double-trigger POWERED on adjacent
    // redstone buddies; at 8+ power, this card dies instead."
    private static CardBattleStats tick() {
        return CardBattleStats.of(3, 2, new BattleAbility(BattleEvent.POWERED, "pulse_extender", (game, slot, target, source) -> {
            List<IBattleIcon> icons = new ArrayList<>(List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon));
            List<Integer> cards = new ArrayList<>();
            if (game.turnPower[slot] < 8) {
                for (int i : BattleEvent.Distribution.ADJACENT.apply(slot, game)) {
                    if (game.getCard(i).is(CardTags.REDSTONE)) {
                        cards.add(i);
                        icons.add(BuddycardBattleIcon.create(game.getCard(i)));
                        icons.add(TextureBattleIcon.doublePowerIcon);
                    }
                }
                icons.add(BuddycardBattleIcon.create(game.getCard(slot)));
                icons.add(TextureBattleIcon.addIcon(1));
                game.turnPower[slot]++;
                game.updatePower(slot);
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.pulse_extender.log"), icons), slot, LoggedEvent.BUFF_ID);
                for (int i : cards) {
                    game.trigger(BattleEvent.POWERED, i, i, slot);
                    game.trigger(BattleEvent.POWERED, i, i, slot);
                }
                return true;
            }
            icons.add(BuddycardBattleIcon.create(game.getCard(slot)));
            icons.add(TextureBattleIcon.deathIcon);
            game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.pulse_extender.log_2"), icons), slot, LoggedEvent.SHAKE_ID);
            game.trigger(BattleEvent.DEATH, slot, slot, slot);
            game.trigger(BattleEvent.OBSERVE_DEATH, slot, slot, slot, BattleEvent.Distribution.ALL);
            game.removeCard(slot);
            return true;
        }));
    }

    // 33: Tock -- "On play, gain power equal to half the current turn number."
    private static CardBattleStats tock() {
        return CardBattleStats.of(4, 1, new BattleAbility(BattleEvent.PLAYED, "waiting_game", (game, slot, target, source) -> {
            int power = game.board.turn / 2;
            if (power > 0) {
                game.turnPower[slot] += power;
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.waiting_game.log1").append("" + power).append(Component.translatable("battles.ability.buddycards.waiting_game.log2")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.addIcon(power))), slot, LoggedEvent.BUFF_ID);
                game.updatePower();
            }
            return true;
        }));
    }

    // 34: Whare -- "On play, gain power equal to the number of cards currently on the field."
    private static CardBattleStats whare() {
        return CardBattleStats.of(4, 1, new BattleAbility(BattleEvent.PLAYED, "travel_game", (game, slot, target, source) -> {
            int power = 0;
            for (int i : BattleEvent.Distribution.ALL.apply(slot, game))
                if (!game.getCard(i).isEmpty()) power++;
            if (power > 0) {
                game.turnPower[slot] += power;
                game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.travel_game.log1").append("" + power).append(Component.translatable("battles.ability.buddycards.travel_game.log2")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.addIcon(power))), slot, LoggedEvent.BUFF_ID);
                game.updatePower();
            }
            return true;
        }));
    }

    // 35: Ambystoma -- "On kill, gain Regeneration. On play, 10% chance to gain 6 power."
    private static CardBattleStats ambystoma() {
        return new CardBattleStats(3, 3, new BattleAbility.Builder()
                .add(new BattleAbility(BattleEvent.KILL, "axo-regeneration", (game, slot, target, source) -> {
                    game.state[slot].status = BattleStatusEffect.REGENERATION;
                    game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.axo-regeneration.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.statusIcon(BattleStatusEffect.REGENERATION))), slot, LoggedEvent.BUFF_ID);
                    game.updatePower();
                    return true;
                }))
                .add(new BattleAbility(BattleEvent.PLAYED, "rare_blue", (game, slot, target, source) -> {
                    if (BattleBoard.RANDOM.nextInt(10) == 0) {
                        game.turnPower[slot] += 6;
                        game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.rare_blue.log"), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.addIcon(6))), slot, LoggedEvent.SHAKE_ID);
                        game.updatePower();
                    }
                    return true;
                })).build());
    }

    // 36: Kart -- "At the start of your turn, move into the first empty adjacent slot and gain 2 power.
    // When powered at 10+ power, announce a (cosmetic, no-op) crash."
    private static CardBattleStats kart() {
        return new CardBattleStats(5, 4, new BattleAbility.Builder()
                .add(new BattleAbility(BattleEvent.TURN, "railway", (game, slot, target, source) -> {
                    for (int i : BattleEvent.Distribution.ADJACENT.apply(slot, game)) {
                        if (game.getCard(i).isEmpty()) {
                            ItemStack moved = game.moveCard(slot, i);
                            game.turnPower[i] += 2;
                            game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.railway.log"), List.of(BuddycardBattleIcon.create(moved), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(moved), TextureBattleIcon.returnIcon, TextureBattleIcon.addIcon(2))), slot, LoggedEvent.BUFF_ID);
                            game.updatePower(i);
                            break;
                        }
                    }
                    return true;
                }))
                .add(new BattleAbility(BattleEvent.POWERED, "mine_crash", (game, slot, target, source) -> {
                    if (game.turnPower[slot] >= 10) {
                        game.board.addLog(new BattleComponent(Component.translatable("battles.ability.buddycards.mine_crash.log1").append(BattleGame.getOwner(slot) ? game.board.getName2() : game.board.getName1()).append(Component.translatable("battles.ability.buddycards.mine_crash.log2")), List.of(BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.damageIcon(10))), slot, LoggedEvent.ATTACK_ID);
                        game.damagePlayer(!BattleGame.getOwner(slot), 10);
                    }
                    return true;
                })).build());
    }
}
