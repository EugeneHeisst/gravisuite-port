package com.chocohead.gravisuite.jetpack;

import com.chocohead.gravisuite.Gravisuite;
import ic2.core.util.Keyboard;
import ic2.core.util.Util;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * ic2.core.item.armor.jetpack.JetpackLogic of IC2 2.8 with its JetpackHandler tick: while the chest piece's jetpack is
 * active, jump (or hover mode) flies, forward pushes, boost adds thrust, ALT-less mode switch + jump toggles hover.
 * Runs on both sides like the original (client prediction, server energy and anti-floating).
 */
@EventBusSubscriber(modid = Gravisuite.MODID)
public final class JetpackLogic {
    /** Whether the local player's jetpack worked this tick (client sound). */
    public static boolean lastJetpackUsedClient;
    private JetpackLogic() {}

    @SubscribeEvent
    static void tick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (stack.getItem() instanceof IJetpack jetpack) onArmorTick(player.level(), player, stack, jetpack);
        else if (player.level().isClientSide && player.isLocalPlayer()) lastJetpackUsedClient = false;
    }

    private static CompoundTag tag(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }

    public static boolean useJetpack(Player player, boolean hoverMode, IJetpack jetpack, ItemStack stack) {
        if (jetpack.getChargeLevel(stack) <= 0) return false;
        IBoostingJetpack boosting = jetpack instanceof IBoostingJetpack b ? b : null;
        float power = jetpack.getPower(stack);
        float drop = jetpack.getDropPercentage(stack);
        if (jetpack.getChargeLevel(stack) <= drop) power = (float)(power * (jetpack.getChargeLevel(stack) / drop));
        if (Keyboard.isForwardKeyDown(player)) {
            float rethruster = boosting != null ? boosting.getBaseThrust(stack, hoverMode) : hoverMode ? 1 : 0.15f;
            float boost = boosting != null ? boosting.getBoostThrust(player, stack, hoverMode) : 0;
            float forwardPower = power * rethruster * 2;
            if (forwardPower > 0) {
                player.moveRelative(0.02f + boost, new Vec3(0, 0, 0.4f * forwardPower + boost));
                if (boost != 0 && !player.onGround()) boosting.useBoostPower(stack, boost);
            }
        }
        int maxFlightHeight = (int)(player.level().getMaxBuildHeight() / jetpack.getWorldHeightDivisor(stack));
        double y = player.getY();
        if (y > maxFlightHeight - 25) {
            if (y > maxFlightHeight) y = maxFlightHeight;
            power = (float)(power * ((maxFlightHeight - y) / 25));
        }
        Vec3 m = player.getDeltaMovement();
        double prevMotion = m.y;
        double motionY = Math.min(m.y + power * 0.2f, 0.6f);
        if (hoverMode) {
            float maxHoverY = 0;
            if (Keyboard.isJumpKeyDown(player)) {
                maxHoverY += jetpack.getHoverMultiplier(stack, true);
                if (boosting != null) maxHoverY *= boosting.getHoverBoost(player, stack, true);
            }
            if (player.isShiftKeyDown()) {
                maxHoverY += -jetpack.getHoverMultiplier(stack, false);
                if (boosting != null) maxHoverY *= boosting.getHoverBoost(player, stack, false);
            }
            if (motionY > maxHoverY) {
                motionY = maxHoverY;
                if (prevMotion > motionY) motionY = prevMotion;
            }
        }
        player.setDeltaMovement(m.x, motionY, m.z);
        if (!player.onGround()) jetpack.drainEnergy(stack, hoverMode ? 1 : 2);
        player.fallDistance = 0;
        Util.resetPlayerInAirTime(player);
        return true;
    }

    public static void onArmorTick(Level level, Player player, ItemStack stack, IJetpack jetpack) {
        boolean used = false;
        if (!stack.isEmpty() && jetpack.isJetpackActive(stack)) {
            CompoundTag nbt = tag(stack);
            boolean hoverMode = nbt.getBoolean("hoverMode");
            byte toggleTimer = nbt.getByte("toggleTimer");
            if (Keyboard.isJumpKeyDown(player) && Keyboard.isModeSwitchKeyDown(player) && toggleTimer == 0) {
                toggleTimer = 10;
                hoverMode = !hoverMode;
                if (!level.isClientSide) {
                    boolean hover = hoverMode;
                    CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putBoolean("hoverMode", hover));
                    player.displayClientMessage(Component.literal(hoverMode ? "Hover Mode enabled." : "Hover Mode disabled."), false);
                }
            }
            if (Keyboard.isJumpKeyDown(player) || hoverMode) {
                used = useJetpack(player, hoverMode, jetpack, stack);
                if (player.onGround() && hoverMode && !level.isClientSide) {
                    CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putBoolean("hoverMode", false));
                    player.displayClientMessage(Component.literal("Hover Mode disabled."), false);
                }
            }
            if (!level.isClientSide && toggleTimer > 0) {
                byte next = (byte)(toggleTimer - 1);
                CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putByte("toggleTimer", next));
            }
        }
        if (level.isClientSide && player.isLocalPlayer()) lastJetpackUsedClient = used;
    }
}
