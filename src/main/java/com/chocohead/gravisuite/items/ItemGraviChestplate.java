package com.chocohead.gravisuite.items;

import ic2.api.item.ElectricItem;
import ic2.core.util.Keyboard;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

/**
 * GraviChestPlate: 60 MEU tier 4, the strongest boosting jetpack (power 1.5, thrust 0.5 / hover 1, boost 834 EU, 278 EU per
 * flying tick, needs 10000 EU to start), protection 1.1 at 20000 EU per point, never burns, dyeable.
 */
public class ItemGraviChestplate extends ItemAdvancedElectricJetpack {
    public static final double MIN_CHARGE = 10000, BOOST_COST = 834, FLIGHT_COST = 278;

    public ItemGraviChestplate(Holder<ArmorMaterial> material, Properties p) { super(material, p.rarity(Rarity.EPIC), 6e7, 100000, 4); }

    @Override
    public void onArmorTick(Level level, Player player, ItemStack stack) {
        super.onArmorTick(level, player, stack);
        player.clearFire();
    }

    @Override public boolean isJetpackActive(ItemStack stack) { return super.isJetpackActive(stack) && ElectricItem.manager.getCharge(stack) >= MIN_CHARGE; }
    @Override public float getPower(ItemStack stack) { return 1.5f; }
    @Override public float getDropPercentage(ItemStack stack) { return 0.01f; }
    @Override public float getBaseThrust(ItemStack stack, boolean hover) { return hover ? 1 : 0.5f; }
    @Override public float getBoostThrust(Player player, ItemStack stack, boolean hover) { return Keyboard.isBoostKeyDown(player) && ElectricItem.manager.getCharge(stack) >= BOOST_COST ? hover ? 0.1f : 0.3f : 0; }
    @Override public boolean useBoostPower(ItemStack stack, float boostAmount) { return ElectricItem.manager.discharge(stack, BOOST_COST, Integer.MAX_VALUE, true, false, false) > 0; }
    @Override public float getWorldHeightDivisor(ItemStack stack) { return 0.91071427f; }
    @Override public float getHoverMultiplier(ItemStack stack, boolean upwards) { return 0.25f; }

    @Override
    public float getHoverBoost(Player player, ItemStack stack, boolean up) {
        if (Keyboard.isBoostKeyDown(player) && ElectricItem.manager.getCharge(stack) >= BOOST_COST) {
            if (!player.onGround()) ElectricItem.manager.discharge(stack, BOOST_COST, Integer.MAX_VALUE, true, false, false);
            return 3;
        }
        return 1;
    }

    @Override public boolean drainEnergy(ItemStack pack, int amount) { return ElectricItem.manager.discharge(pack, FLIGHT_COST, Integer.MAX_VALUE, true, false, false) > 0; }
    @Override public int getEnergyPerDamage() { return 20000; }
    @Override public double getDamageAbsorptionRatio() { return 1.1; }
}
