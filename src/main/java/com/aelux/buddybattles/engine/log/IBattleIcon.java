package com.aelux.buddybattles.engine.log;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;

public sealed interface IBattleIcon permits TextureBattleIcon, BuddycardBattleIcon {
    Codec<IBattleIcon> ALL_ICON_CODEC = Codec.either(BuddycardBattleIcon.CODEC, TextureBattleIcon.CODEC)
            .xmap(IBattleIcon::unwrap, IBattleIcon::wrap);

    int width();

    default int height() {
        return 12;
    }

    static IBattleIcon unwrap(Either<BuddycardBattleIcon, TextureBattleIcon> either) {
        return either.left().map(IBattleIcon.class::cast).orElseGet(() -> either.right().get());
    }

    static Either<BuddycardBattleIcon, TextureBattleIcon> wrap(IBattleIcon icon) {
        return switch (icon) {
            case BuddycardBattleIcon b -> Either.left(b);
            case TextureBattleIcon t -> Either.right(t);
        };
    }
}
