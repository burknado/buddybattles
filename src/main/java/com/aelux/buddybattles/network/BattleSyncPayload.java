package com.aelux.buddybattles.network;

import com.aelux.buddybattles.BuddyBattles;
import com.aelux.buddybattles.engine.BoardSnapshot;
import com.aelux.buddybattles.engine.log.BuddycardBattleIcon;
import com.aelux.buddybattles.engine.log.IBattleIcon;
import com.aelux.buddybattles.engine.log.TextureBattleIcon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Server -> client: a full snapshot of the battle from the receiving player's point of view. */
public record BattleSyncPayload(
        int health1, int health2,
        int energy1, int energy2,
        int turnEnergy, int turn,
        boolean isPlayer1Turn,
        boolean mySide,
        ItemStack hand0, ItemStack hand1, ItemStack hand2,
        ItemStack board0, ItemStack board1, ItemStack board2,
        ItemStack board3, ItemStack board4, ItemStack board5,
        int[] boardPower,
        int[] boardStatus,
        int opponentHandCount,
        List<String> recentLog,
        List<Integer> logSourceSlots,
        List<String> logAbilityIds,
        List<BoardSnapshot.PlayerView> logSnapshots,
        List<List<IBattleIcon>> logIcons,
        boolean gameEnded,
        boolean wonBySelf,
        boolean interrupted
) implements CustomPacketPayload {
    public static final Type<BattleSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BuddyBattles.MOD_ID, "battle_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BattleSyncPayload> STREAM_CODEC =
            StreamCodec.of(BattleSyncPayload::write, BattleSyncPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, BattleSyncPayload p) {
        buf.writeVarInt(p.health1);
        buf.writeVarInt(p.health2);
        buf.writeVarInt(p.energy1);
        buf.writeVarInt(p.energy2);
        buf.writeVarInt(p.turnEnergy);
        buf.writeVarInt(p.turn);
        buf.writeBoolean(p.isPlayer1Turn);
        buf.writeBoolean(p.mySide);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, p.hand0);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, p.hand1);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, p.hand2);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, p.board0);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, p.board1);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, p.board2);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, p.board3);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, p.board4);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, p.board5);
        for (int i = 0; i < 6; i++) buf.writeVarInt(p.boardPower[i]);
        for (int i = 0; i < 6; i++) buf.writeVarInt(p.boardStatus[i]);
        buf.writeVarInt(p.opponentHandCount);
        buf.writeVarInt(p.recentLog.size());
        for (int i = 0; i < p.recentLog.size(); i++) {
            buf.writeUtf(p.recentLog.get(i), 512);
            buf.writeVarInt(p.logSourceSlots.get(i));
            buf.writeUtf(p.logAbilityIds.get(i), 64);
            writeSnapshot(buf, p.logSnapshots.get(i));
            writeIconList(buf, p.logIcons.get(i));
        }
        buf.writeBoolean(p.gameEnded);
        buf.writeBoolean(p.wonBySelf);
        buf.writeBoolean(p.interrupted);
    }

    private static void writeSnapshot(RegistryFriendlyByteBuf buf, BoardSnapshot.PlayerView snapshot) {
        buf.writeVarInt(snapshot.health1());
        buf.writeVarInt(snapshot.health2());
        buf.writeVarInt(snapshot.energy1());
        buf.writeVarInt(snapshot.energy2());
        for (int i = 0; i < 6; i++) ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, snapshot.board()[i]);
        for (int i = 0; i < 6; i++) buf.writeVarInt(snapshot.power()[i]);
        for (int i = 0; i < 6; i++) buf.writeVarInt(snapshot.status()[i]);
        for (int i = 0; i < 3; i++) ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, snapshot.myHand()[i]);
        buf.writeVarInt(snapshot.opponentHandCount());
        buf.writeBoolean(snapshot.activePlayer());
    }

    private static BoardSnapshot.PlayerView readSnapshot(RegistryFriendlyByteBuf buf) {
        int health1 = buf.readVarInt();
        int health2 = buf.readVarInt();
        int energy1 = buf.readVarInt();
        int energy2 = buf.readVarInt();
        ItemStack[] board = new ItemStack[6];
        for (int i = 0; i < 6; i++) board[i] = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        int[] power = new int[6];
        for (int i = 0; i < 6; i++) power[i] = buf.readVarInt();
        int[] status = new int[6];
        for (int i = 0; i < 6; i++) status[i] = buf.readVarInt();
        ItemStack[] myHand = new ItemStack[3];
        for (int i = 0; i < 3; i++) myHand[i] = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        int opponentHandCount = buf.readVarInt();
        boolean activePlayer = buf.readBoolean();
        return new BoardSnapshot.PlayerView(health1, health2, energy1, energy2, board, power, status, myHand, opponentHandCount, activePlayer);
    }

    /** type byte: 0 = TextureBattleIcon, 1 = BuddycardBattleIcon. */
    private static void writeIcon(RegistryFriendlyByteBuf buf, IBattleIcon icon) {
        if (icon instanceof TextureBattleIcon t) {
            buf.writeBoolean(false);
            buf.writeVarInt(t.texturePosX());
            buf.writeVarInt(t.texturePosY());
            buf.writeVarInt(t.width());
            buf.writeVarInt(t.info().size());
            for (TextureBattleIcon.BattleInfo info : t.info()) {
                buf.writeUtf(info.display(), 32);
                buf.writeVarInt(info.x());
                buf.writeVarInt(info.y());
                buf.writeVarInt(info.color());
                buf.writeBoolean(info.isLeftAligned());
            }
        } else if (icon instanceof BuddycardBattleIcon b) {
            buf.writeBoolean(true);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, new ItemStack(b.getItem()));
        }
    }

    private static IBattleIcon readIcon(RegistryFriendlyByteBuf buf) {
        boolean isCard = buf.readBoolean();
        if (isCard) {
            ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            return BuddycardBattleIcon.create(stack);
        }
        int x = buf.readVarInt();
        int y = buf.readVarInt();
        int width = buf.readVarInt();
        int infoCount = buf.readVarInt();
        List<TextureBattleIcon.BattleInfo> info = new ArrayList<>(infoCount);
        for (int i = 0; i < infoCount; i++) {
            String display = buf.readUtf(32);
            int ix = buf.readVarInt();
            int iy = buf.readVarInt();
            int color = buf.readVarInt();
            boolean leftAligned = buf.readBoolean();
            info.add(new TextureBattleIcon.BattleInfo(display, ix, iy, color, leftAligned));
        }
        return new TextureBattleIcon(x, y, TextureBattleIcon.TEXTURE, width, info);
    }

    private static void writeIconList(RegistryFriendlyByteBuf buf, List<IBattleIcon> icons) {
        buf.writeVarInt(icons.size());
        for (IBattleIcon icon : icons) writeIcon(buf, icon);
    }

    private static List<IBattleIcon> readIconList(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<IBattleIcon> icons = new ArrayList<>(count);
        for (int i = 0; i < count; i++) icons.add(readIcon(buf));
        return icons;
    }

    private static BattleSyncPayload read(RegistryFriendlyByteBuf buf) {
        int health1 = buf.readVarInt();
        int health2 = buf.readVarInt();
        int energy1 = buf.readVarInt();
        int energy2 = buf.readVarInt();
        int turnEnergy = buf.readVarInt();
        int turn = buf.readVarInt();
        boolean isPlayer1Turn = buf.readBoolean();
        boolean mySide = buf.readBoolean();
        ItemStack hand0 = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        ItemStack hand1 = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        ItemStack hand2 = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        ItemStack board0 = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        ItemStack board1 = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        ItemStack board2 = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        ItemStack board3 = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        ItemStack board4 = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        ItemStack board5 = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        int[] boardPower = new int[6];
        for (int i = 0; i < 6; i++) boardPower[i] = buf.readVarInt();
        int[] boardStatus = new int[6];
        for (int i = 0; i < 6; i++) boardStatus[i] = buf.readVarInt();
        int opponentHandCount = buf.readVarInt();
        int logSize = buf.readVarInt();
        List<String> recentLog = new ArrayList<>(logSize);
        List<Integer> logSourceSlots = new ArrayList<>(logSize);
        List<String> logAbilityIds = new ArrayList<>(logSize);
        List<BoardSnapshot.PlayerView> logSnapshots = new ArrayList<>(logSize);
        List<List<IBattleIcon>> logIcons = new ArrayList<>(logSize);
        for (int i = 0; i < logSize; i++) {
            recentLog.add(buf.readUtf(512));
            logSourceSlots.add(buf.readVarInt());
            logAbilityIds.add(buf.readUtf(64));
            logSnapshots.add(readSnapshot(buf));
            logIcons.add(readIconList(buf));
        }
        boolean gameEnded = buf.readBoolean();
        boolean wonBySelf = buf.readBoolean();
        boolean interrupted = buf.readBoolean();
        return new BattleSyncPayload(health1, health2, energy1, energy2, turnEnergy, turn, isPlayer1Turn, mySide,
                hand0, hand1, hand2, board0, board1, board2, board3, board4, board5,
                boardPower, boardStatus, opponentHandCount, recentLog, logSourceSlots, logAbilityIds, logSnapshots, logIcons,
                gameEnded, wonBySelf, interrupted);
    }

    /** All 6 board slots as an array, absolute indices (see class docs). */
    public ItemStack[] boardArray() {
        return new ItemStack[]{board0, board1, board2, board3, board4, board5};
    }

    /** My 3 hand slots as an array. */
    public ItemStack[] handArray() {
        return new ItemStack[]{hand0, hand1, hand2};
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
