package com.aelux.buddybattles.network;

import com.aelux.buddybattles.BuddyBattles;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server: manually trigger the ACTIVATED ability of the card on {@code boardSlot}. */
public record ActivateCardPayload(int boardSlot) implements CustomPacketPayload {
    public static final Type<ActivateCardPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BuddyBattles.MOD_ID, "activate_card"));

    public static final StreamCodec<ByteBuf, ActivateCardPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ActivateCardPayload::boardSlot,
            ActivateCardPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
