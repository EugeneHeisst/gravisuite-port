package com.chocohead.gravisuite.jetpack;

import net.minecraft.world.item.ItemStack;

/** ic2.core.item.armor.jetpack.IJetpack of IC2 2.8 (the jetpack API Gravitation Suite 3.1.1 is built on). */
public interface IJetpack {
    int EU_ENERGY_INCREASE = 6;
    boolean drainEnergy(ItemStack stack, int amount);
    float getPower(ItemStack stack);
    float getDropPercentage(ItemStack stack);
    double getChargeLevel(ItemStack stack);
    boolean isJetpackActive(ItemStack stack);
    float getHoverMultiplier(ItemStack stack, boolean upwards);
    float getWorldHeightDivisor(ItemStack stack);
}
