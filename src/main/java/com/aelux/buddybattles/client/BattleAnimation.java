package com.aelux.buddybattles.client;

import com.aelux.buddybattles.engine.*;
import com.aelux.buddybattles.engine.LoggedEvent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class BattleAnimation  {
    public static final long ANIMATION_DURATION_MS = 500;
    private static final int ATTACK_LUNGE_DISTANCE = 10;
    private static final int BUFF_SHAKE_DISTANCE = 6;
    private static final int SELF_SHAKE_DISTANCE = 6;
    private static final int CIRCLE_ROUND_DISTANCE = 6;

    public Pair<Integer, Integer> getAttackOffset(Map<Integer, Pair<Long, String>> animationStartMs, int absoluteSlot, int direction) {
        if (animationStartMs == null || animationStartMs.get(absoluteSlot) == null || absoluteSlot < 0) return new ImmutablePair<>(0, 0);
        Long startMs = animationStartMs.get(absoluteSlot).getLeft();
        String animIndex = animationStartMs.get(absoluteSlot).getRight();
        if (startMs == null || animIndex == null) return new ImmutablePair<>(0, 0);

        long elapsed = System.currentTimeMillis() - startMs;
        if (elapsed >= ANIMATION_DURATION_MS) {
            animationStartMs.remove(absoluteSlot); // finished - clean up
            return new ImmutablePair<>(0, 0);
        }

        float progress = elapsed / (float) ANIMATION_DURATION_MS;

        if (animIndex.equals(LoggedEvent.ATTACK_ID)){
            //Attack lunge
            return attackLunge(progress, direction);
        } else if (animIndex.equals(LoggedEvent.BUFF_ID)){
            //Buff wiggle
            return buffSelf(progress, direction);
        } else if (animIndex.equals(LoggedEvent.SHAKE_ID)){
            //Shaking
            return selfShake(progress, direction);
        } else if (animIndex.equals(LoggedEvent.CIRCLE_ID)){
            //Circling
            return circleRound(progress, direction);
        }

        return new ImmutablePair<>(0, 0);
    }

    private Pair<Integer, Integer> attackLunge(float progress, int direction) {
        float eased = (float) Math.sin(progress * Math.PI);
        return new ImmutablePair<>(0, (int) (eased * ATTACK_LUNGE_DISTANCE * direction));
    }

    private Pair<Integer, Integer> buffSelf(float progress, int direction) {
        float eased = (float) Math.sin(progress * 2 * Math.PI);
        return new ImmutablePair<>((int) (eased * BUFF_SHAKE_DISTANCE * direction), 0);
    }

    private Pair<Integer, Integer> selfShake(float progress, int direction) {
        double rand = Math.random() * Math.PI * 2;
        float eased = (float) Math.sin(progress * Math.PI);
        int diffX = (int) (Math.cos(rand) * eased * SELF_SHAKE_DISTANCE) * direction;
        int diffY = (int) (Math.sin(rand) * eased * SELF_SHAKE_DISTANCE) * direction;
        return new ImmutablePair<>(diffX, diffY);
    }

    private Pair<Integer, Integer> circleRound(float progress, int direction) {
        float eased = (float) Math.sin(progress * Math.PI);
        float angle = (float) (progress * Math.PI * 2 * direction);
        int diffX = (int) (Math.cos(angle) * CIRCLE_ROUND_DISTANCE * eased);
        int diffY = (int) (Math.sin(angle) * CIRCLE_ROUND_DISTANCE * eased);
        return new ImmutablePair<>(diffX, diffY);
    }
}
