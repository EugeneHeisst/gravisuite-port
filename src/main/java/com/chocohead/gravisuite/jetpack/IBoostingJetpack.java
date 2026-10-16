package com.chocohead.gravisuite.jetpack;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** ic2.core.item.armor.jetpack.IBoostingJetpack of IC2 2.8. */
public interface IBoostingJetpack extends IJetpack {
    float getBaseThrust(ItemStack stack, boolean hover);
    float getBoostThrust(Player player, ItemStack stack, boolean hover);
    boolean useBoostPower(ItemStack stack, float boostAmount);
    float getHoverBoost(Player player, ItemStack stack, boolean up);
}
