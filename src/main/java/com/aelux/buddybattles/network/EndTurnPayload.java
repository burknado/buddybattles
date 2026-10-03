package com.aelux.buddybattles.network;

import com.aelux.buddybattles.BuddyBattles;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server: end my turn. No fields - a unit payload. */
public record EndTurnPayload() implements CustomPacketPayload {
    public static final EndTurnPayload INSTANCE = new EndTurnPayload();
    public static final Type<EndTurnPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BuddyBattles.MOD_ID, "end_turn"));
    public static final StreamCodec<ByteBuf, EndTurnPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
