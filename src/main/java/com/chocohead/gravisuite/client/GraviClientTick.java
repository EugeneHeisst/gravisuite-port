package com.chocohead.gravisuite.client;

import com.chocohead.gravisuite.GraviKeys;
import com.chocohead.gravisuite.GraviSounds;
import com.chocohead.gravisuite.Gravisuite;
import com.chocohead.gravisuite.jetpack.JetpackLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Sends the fly key state when it changes and plays the jetpack loop while the local jetpack works. */
@EventBusSubscriber(modid = Gravisuite.MODID, value = Dist.CLIENT)
public final class GraviClientTick {
    private static boolean lastFly;
    private static JetpackSound sound;
    private GraviClientTick() {}

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) { lastFly = false; return; }
        boolean fly = GraviClient.FLY_KEY.isDown() && mc.screen == null;
        if (fly != lastFly) {
            lastFly = fly;
            GraviKeys.setFlyKey(mc.player, fly);
            PacketDistributor.sendToServer(new GraviKeys.FlyKeyPayload(fly));
        }
        if (JetpackLogic.lastJetpackUsedClient && (sound == null || sound.isStopped())) mc.getSoundManager().play(sound = new JetpackSound());
    }

    static final class JetpackSound extends AbstractTickableSoundInstance {
        JetpackSound() {
            super(GraviSounds.JETPACK_LOOP.get(), SoundSource.PLAYERS, RandomSource.create());
            looping = true;
            volume = 0.5f;
        }
        @Override
        public void tick() {
            var p = Minecraft.getInstance().player;
            if (p == null || !JetpackLogic.lastJetpackUsedClient) { stop(); return; }
            x = p.getX(); y = p.getY(); z = p.getZ();
        }
    }
}
