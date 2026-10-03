package com.aelux.buddybattles.engine;

import com.aelux.buddybattles.engine.log.BattleComponent;
import com.aelux.buddybattles.engine.log.BuddycardBattleIcon;
import com.aelux.buddybattles.engine.log.TextureBattleIcon;
import net.minecraft.network.chat.Component;

import java.util.List;

public enum BattleStatusEffect {
    EMPTY,
    FIRE(new BattleAbility(BattleEvent.TURN, "status.fire", fire()), 0xFFff6430),
    SLEEP(new BattleAbility(BattleEvent.FIGHT, "status.sleep", sleep()), 0xFFbafff6),
    STRENGTH(new BattleAbility(BattleEvent.FIGHT, "status.strength", strength()), 0xFFab002b),
    RESISTANCE(new BattleAbility(BattleEvent.DAMAGED, "status.resistance", resistance()), 0xFF627886),
    REGENERATION(new BattleAbility(BattleEvent.TURN, "status.regeneration", regeneration()), 0xFFf633ad),
    WEAKNESS(new BattleAbility(BattleEvent.FIGHT, "status.weakness", weakness()), 0xFF937725),
    POISON(new BattleAbility(BattleEvent.TURN, "status.poison", poison()), 0xFF55a03c),
    WITHER(new BattleAbility(BattleEvent.TURN, "status.wither", wither()), 0xFF000000),
    AIRBORNE(new BattleAbility(BattleEvent.FIGHT, "status.airborne", airborne()), 0xFFffdc75),
    STUNNED(new BattleAbility(BattleEvent.FIGHT, "status.stunned", stunned()), 0xFF937725);

    private final BattleAbility ability;
    private final int color;

    BattleStatusEffect(BattleAbility ability, int color) {
        this.ability = ability;
        this.color = color;
    }

    BattleStatusEffect() {
        ability = null;
        color = 0xFFFFFF;
    }

    public BattleAbility getAbility() {
        return ability;
    }

    public int getColor() {
        return color;
    }

    private static BattleAbility.BattleAbilityFunc fire() {
        return (game, slot, target, source) -> {
            if (BattleBoard.RANDOM.nextBoolean()) {
                game.state[slot].status = EMPTY;
                game.board.addLog(new BattleComponent(Component.translatable(game.getCard(slot).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.status.fire.log1")), List.of(TextureBattleIcon.statusIcon(FIRE), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.statusIcon(EMPTY))));
            } else {
                game.turnPower[slot]--;
                game.board.addLog(new BattleComponent(Component.translatable(game.getCard(slot).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.status.fire.log2")), List.of(TextureBattleIcon.statusIcon(FIRE), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.subtractIcon(1))));
                game.updatePower(slot);
            }
            return true;
        };
    }

    private static BattleAbility.BattleAbilityFunc sleep() {
        return (game, slot, target, source) -> {
            game.state[slot].status = EMPTY;
            game.board.addLog(new BattleComponent(Component.translatable(game.getCard(slot).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.status.sleep.log")), List.of(TextureBattleIcon.statusIcon(SLEEP), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.statusIcon(EMPTY))));
            return false;
        };
    }

    private static BattleAbility.BattleAbilityFunc strength() {
        return (game, slot, target, source) -> {
            game.state[slot].power += 2;
            game.state[slot].status = EMPTY;
            game.board.addLog(new BattleComponent(Component.translatable(game.getCard(slot).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.status.strength.log")), List.of(TextureBattleIcon.statusIcon(STRENGTH), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.addIcon(2), TextureBattleIcon.statusIcon(EMPTY))));
            game.updatePower(slot);
            return true;
        };
    }

    private static BattleAbility.BattleAbilityFunc resistance() {
        return (game, slot, target, source) -> {
            game.state[slot].power++;
            game.board.addLog(new BattleComponent(Component.translatable(game.getCard(slot).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.status.resistance.log")), List.of(TextureBattleIcon.statusIcon(RESISTANCE), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.addIcon(1))));
            game.updatePower(slot);
            return true;
        };
    }

    private static BattleAbility.BattleAbilityFunc regeneration() {
        return (game, slot, target, source) -> {
            game.state[slot].power++;
            game.board.addLog(new BattleComponent(Component.translatable(game.getCard(slot).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.status.regeneration.log")), List.of(TextureBattleIcon.statusIcon(REGENERATION), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.addIcon(1))));
            game.updatePower(slot);
            return true;
        };
    }

    private static BattleAbility.BattleAbilityFunc weakness() {
        return (game, slot, target, source) -> {
            game.state[slot].power -= 2;
            game.state[slot].status = EMPTY;
            game.board.addLog(new BattleComponent(Component.translatable(game.getCard(slot).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.status.weakness.log")), List.of(TextureBattleIcon.statusIcon(SLEEP), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.subtractIcon(2), TextureBattleIcon.statusIcon(EMPTY))));
            game.updatePower(slot);
            return true;
        };
    }

    private static BattleAbility.BattleAbilityFunc poison() {
        return (game, slot, target, source) -> {
            if (game.state[slot].power > 1) {
                game.state[slot].power--;
                game.board.addLog(new BattleComponent(Component.translatable(game.getCard(slot).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.status.poison.log")), List.of(TextureBattleIcon.statusIcon(SLEEP), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.subtractIcon(1))));
                game.updatePower(slot);
            }
            return true;
        };
    }

    private static BattleAbility.BattleAbilityFunc wither() {
        return (game, slot, target, source) -> {
            game.state[slot].power--;
            game.board.addLog(new BattleComponent(Component.translatable(game.getCard(slot).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.status.wither.log")), List.of(TextureBattleIcon.statusIcon(WITHER), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.subtractIcon(1))));
            game.updatePower(slot);
            return true;
        };
    }

    private static BattleAbility.BattleAbilityFunc airborne() {
        return (game, slot, target, source) -> {
            int damage = game.turnPower[slot];
            game.damagePlayer(!BattleGame.getOwner(slot), damage);
            game.state[slot].status = EMPTY;
            game.board.addLog(new BattleComponent(Component.translatable(game.getCard(slot).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.status.airborne.log1")).append("" + damage).append(Component.translatable("battles.ability.buddycards.status.airborne.log2")).append(BattleGame.getOwner(slot) ? game.board.getName2() : game.board.getName1()).append(Component.translatable("battles.ability.buddycards.status.airborne.log3")), List.of(TextureBattleIcon.statusIcon(AIRBORNE), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.damageIcon(damage), TextureBattleIcon.statusIcon(EMPTY))));
            return false;
        };
    }

    private static BattleAbility.BattleAbilityFunc stunned() {
        return (game, slot, target, source) -> {
            game.state[slot].status = EMPTY;
            game.board.addLog(new BattleComponent(Component.translatable(game.getCard(slot).getDescriptionId()).append(Component.translatable("battles.ability.buddycards.status.stunned.log")), List.of(TextureBattleIcon.statusIcon(STUNNED), TextureBattleIcon.dividerIcon, BuddycardBattleIcon.create(game.getCard(slot)), TextureBattleIcon.statusIcon(EMPTY))));
            return false;
        };
    }
}
