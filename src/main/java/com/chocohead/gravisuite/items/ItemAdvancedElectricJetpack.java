package com.chocohead.gravisuite.items;

import com.chocohead.gravisuite.GraviKeys;
import com.chocohead.gravisuite.jetpack.IBoostingJetpack;
import ic2.api.item.ElectricItem;
import ic2.core.item.armor.ItemArmorElectric;
import ic2.core.util.Keyboard;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * Advanced electric jetpack: 3 MEU tier 3 power source with an IC2 2.8 boosting jetpack (thrust 0.3, hover 0.65, boost
 * 60 EU), switched on and off with the Gravi Fly Key.
 */
public class ItemAdvancedElectricJetpack extends ItemArmorElectric implements IBoostingJetpack {
    public ItemAdvancedElectricJetpack(Holder<ArmorMaterial> material, Properties p) { this(material, p.rarity(Rarity.UNCOMMON), 3e6, 30000, 3); }
    protected ItemAdvancedElectricJetpack(Holder<ArmorMaterial> material, Properties p, double maxCharge, double transfer, int tier) { super(material, Type.CHESTPLATE, p, maxCharge, transfer, tier); }

    private static net.minecraft.nbt.CompoundTag tag(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }
    public static boolean isJetpackOn(ItemStack stack) { return tag(stack).getBoolean("isFlyActive"); }
    public static boolean isHovering(ItemStack stack) { return tag(stack).getBoolean("hoverMode"); }

    public static boolean switchJetpack(ItemStack stack) {
        boolean on = !isJetpackOn(stack);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putBoolean("isFlyActive", on));
        return on;
    }

    @Override
    public void onArmorTick(Level level, Player player, ItemStack stack) {
        byte toggleTimer = tag(stack).getByte("toggleTimer");
        if (GraviKeys.isFlyKeyDown(player) && toggleTimer == 0) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putByte("toggleTimer", (byte)10));
            if (!level.isClientSide) {
                boolean on = switchJetpack(stack);
                GraviPower.message(player, Component.translatable("gravisuite.message.jetpackSwitch", GraviPower.onOff(on).copy().withStyle(on ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED)), ChatFormatting.YELLOW);
            }
        }
        // JetpackLogic owns the shared hover/F cooldown, even when the engine cannot fly.
    }

    @Override public boolean isJetpackActive(ItemStack stack) { return isJetpackOn(stack); }
    @Override public double getChargeLevel(ItemStack stack) { return ElectricItem.manager.getCharge(stack) / getMaxCharge(stack); }
    @Override public float getPower(ItemStack stack) { return 1; }
    @Override public float getDropPercentage(ItemStack stack) { return 0.05f; }
    @Override public float getBaseThrust(ItemStack stack, boolean hover) { return hover ? 0.65f : 0.3f; }
    @Override public float getBoostThrust(Player player, ItemStack stack, boolean hover) { return Keyboard.isBoostKeyDown(player) && ElectricItem.manager.getCharge(stack) >= 60 ? hover ? 0.07f : 0.09f : 0; }
    @Override public boolean useBoostPower(ItemStack stack, float boostAmount) { return ElectricItem.manager.discharge(stack, 60, Integer.MAX_VALUE, true, false, false) > 0; }
    @Override public float getWorldHeightDivisor(ItemStack stack) { return 1; }
    @Override public float getHoverMultiplier(ItemStack stack, boolean upwards) { return 0.2f; }

    @Override
    public float getHoverBoost(Player player, ItemStack stack, boolean up) {
        if (Keyboard.isBoostKeyDown(player) && ElectricItem.manager.getCharge(stack) >= 60) {
            if (!player.onGround()) ElectricItem.manager.discharge(stack, 60, Integer.MAX_VALUE, true, false, false);
            return 2;
        }
        return 1;
    }

    @Override public boolean drainEnergy(ItemStack pack, int amount) { return ElectricItem.manager.discharge(pack, amount * 6, Integer.MAX_VALUE, true, false, false) > 0; }
    @Override public boolean canProvideEnergy(ItemStack stack) { return true; }
    @Override public int getEnergyPerDamage() { return 0; }
    @Override public double getDamageAbsorptionRatio() { return 0; }
}
