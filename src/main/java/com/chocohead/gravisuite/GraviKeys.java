package com.chocohead.gravisuite;

import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The Gravi Fly Key (default F) of GraviKeys: the client sends its state, both sides read it per player. */
public final class GraviKeys {
    private static final Map<Player, Boolean> FLY = new WeakHashMap<>();
    private GraviKeys() {}

    public static synchronized boolean isFlyKeyDown(Player player) { return FLY.getOrDefault(player, false); }
    public static synchronized void setFlyKey(Player player, boolean down) { FLY.put(player, down); }

    public record FlyKeyPayload(boolean down) implements CustomPacketPayload {
        public static final Type<FlyKeyPayload> TYPE = new Type<>(Gravisuite.id("fly_key"));
        public static final StreamCodec<ByteBuf, FlyKeyPayload> CODEC = ByteBufCodecs.BOOL.map(FlyKeyPayload::new, FlyKeyPayload::down);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
        public static void handle(FlyKeyPayload p, IPayloadContext ctx) { setFlyKey(ctx.player(), p.down); }
    }
}
