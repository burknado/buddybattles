package com.aelux.buddybattles.network;

import com.aelux.buddybattles.BuddyBattles;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server: play the card in hand slot {@code handIndex} onto board slot {@code boardSlot}. */
public record PlayCardPayload(int handIndex, int boardSlot) implements CustomPacketPayload {
    public static final Type<PlayCardPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BuddyBattles.MOD_ID, "play_card"));

    public static final StreamCodec<ByteBuf, PlayCardPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PlayCardPayload::handIndex,
            ByteBufCodecs.VAR_INT, PlayCardPayload::boardSlot,
            PlayCardPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
