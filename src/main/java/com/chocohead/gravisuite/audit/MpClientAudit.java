package com.chocohead.gravisuite.audit;

import com.chocohead.gravisuite.Gravisuite;
import com.chocohead.gravisuite.client.GraviClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** -Dgravisuite.audit=mpclient: joins the dev server, taps the Gravi Fly Key, holds jump and reports how the connection ends. */
@EventBusSubscriber(modid = Gravisuite.MODID, value = Dist.CLIENT)
public final class MpClientAudit {
    private static final boolean ENABLED = "mpclient".equals(System.getProperty("gravisuite.audit"));
    private static int ticks, inGame;
    private MpClientAudit() {}

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        if (!ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        ticks++;
        if (mc.player != null && mc.getConnection() != null) {
            inGame++;
            GraviClient.FLY_KEY.setDown(inGame >= 20 && inGame < 24);
            mc.options.keyJump.setDown(inGame >= 30);
        }
        if (mc.screen instanceof DisconnectedScreen ds) {
            Gravisuite.LOG.info("[gravi-mp-audit] client disconnected: {}", ds.getNarrationMessage().getString());
            mc.stop();
        }
        if (ticks > 6000) mc.stop();
    }
}
