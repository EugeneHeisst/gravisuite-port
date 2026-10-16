package com.chocohead.gravisuite.audit;

import com.chocohead.gravisuite.GraviItems;
import com.chocohead.gravisuite.Gravisuite;
import com.chocohead.gravisuite.items.ItemAdvancedElectricJetpack;
import ic2.core.util.Util;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * -Dgravisuite.audit=mpflight on the dedicated dev server (allow-flight=false): the joining player gets a charged
 * GraviChestPlate, switches the engine on with the Gravi Fly Key over the network and climbs holding jump for 240 ticks
 * (the kick comes after 80 floating ticks). Then the IC2 in-air reset is switched off and the server must kick.
 */
@EventBusSubscriber(modid = Gravisuite.MODID)
public final class MpFlightAudit {
    private static final boolean ENABLED = "mpflight".equals(System.getProperty("gravisuite.audit"));
    private static UUID flyer;
    private static int t = -1, maxInAir;
    private static double startY;
    private static boolean flightOk, kicked, engine;
    private MpFlightAudit() {}

    @SubscribeEvent
    static void join(PlayerEvent.PlayerLoggedInEvent event) {
        if (!ENABLED || !(event.getEntity() instanceof ServerPlayer sp)) return;
        flyer = sp.getUUID();
        t = 0;
        sp.setGameMode(GameType.SURVIVAL);
        sp.setItemSlot(EquipmentSlot.CHEST, GraviItems.GRAVI_CHESTPLATE.get().charged());
        sp.teleportTo(sp.serverLevel(), sp.getX(), 100, sp.getZ(), 0, 0);
        startY = 100;
        Gravisuite.LOG.info("[gravi-mp-audit] {} joined, allow-flight={}", sp.getName().getString(), sp.server.isFlightAllowed());
    }

    @SubscribeEvent
    static void leave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!ENABLED || !event.getEntity().getUUID().equals(flyer)) return;
        kicked = true;
        Gravisuite.LOG.info("[gravi-mp-audit] player left at tick {} (reset disabled: {})", t, Util.auditDisableAirReset);
    }

    @SubscribeEvent
    static void tick(ServerTickEvent.Post event) {
        if (!ENABLED || t < 0) return;
        t++;
        ServerPlayer sp = flyer == null ? null : event.getServer().getPlayerList().getPlayer(flyer);
        if (sp != null && !Util.auditDisableAirReset) {
            maxInAir = Math.max(maxInAir, Util.playerInAirTime(sp));
            engine |= ItemAdvancedElectricJetpack.isJetpackOn(sp.getItemBySlot(EquipmentSlot.CHEST));
        }
        if (t == 280) {
            flightOk = sp != null && !kicked && engine && sp.getY() > startY + 10;
            Gravisuite.LOG.info("[gravi-mp-audit] {} GraviChestPlate flight on an allow-flight=false server: engine on by the fly key={} connected={} climbed {} blocks, max floating ticks {}",
                flightOk ? "PASS" : "FAIL", engine, sp != null, sp == null ? 0 : sp.getY() - startY, maxInAir);
            Util.auditDisableAirReset = true;
        }
        if (t > 280 && kicked || t == 520) {
            Gravisuite.LOG.info("[gravi-mp-audit] {} control: without the reset the server kicks for flying (kicked={} after {} ticks)", kicked ? "PASS" : "FAIL", kicked, t - 280);
            Gravisuite.LOG.info("[gravi-mp-audit] DONE {}", flightOk && kicked ? "PASS" : "FAIL");
            t = -1;
            event.getServer().halt(false);
        }
    }
}
