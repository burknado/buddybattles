package com.aelux.buddybattles.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class BuddyBattlesNetworking {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(PlayCardPayload.TYPE, PlayCardPayload.STREAM_CODEC, BattleNetworkHandlers::handlePlayCard);
        registrar.playToServer(EndTurnPayload.TYPE, EndTurnPayload.STREAM_CODEC, BattleNetworkHandlers::handleEndTurn);
        registrar.playToServer(ActivateCardPayload.TYPE, ActivateCardPayload.STREAM_CODEC, BattleNetworkHandlers::handleActivateCard);
        registrar.playToClient(BattleSyncPayload.TYPE, BattleSyncPayload.STREAM_CODEC, BattleNetworkHandlers::handleBattleSync);
    }
}
