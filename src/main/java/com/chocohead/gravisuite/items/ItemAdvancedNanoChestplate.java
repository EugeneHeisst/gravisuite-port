package com.chocohead.gravisuite.items;

import ic2.api.item.ElectricItem;
import ic2.core.ref.Ic2Items;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

/**
 * Advanced NanoChestPlate: the advanced jetpack with nano protection (0.9, 800 EU per point). Once a second, while burning,
 * it pours a water cell from the inventory over the wearer for 50000 EU (the original tried to take the EU from the cell).
 */
public class ItemAdvancedNanoChestplate extends ItemAdvancedElectricJetpack {
    public static final double EXTINGUISH_COST = 50000;

    public ItemAdvancedNanoChestplate(Holder<ArmorMaterial> material, Properties p) { super(material, p.rarity(Rarity.UNCOMMON), 3e6, 30000, 3); }

    @Override
    public void onArmorTick(Level level, Player player, ItemStack armour) {
        super.onArmorTick(level, player, armour);
        if (level.isClientSide || player.tickCount % 20 != 0 || !player.isOnFire() || !ElectricItem.manager.canUse(armour, EXTINGUISH_COST)) return;
        var inv = player.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack stack = inv.items.get(i);
            if (stack.isEmpty() || !stack.is(Ic2Items.WATER_CELL.get())) continue;
            ItemStack empty = new ItemStack(Ic2Items.EMPTY_CELL.get());
            stack.shrink(1);
            if (!inv.add(empty)) { stack.grow(1); continue; } // no room for the empty cell: like the original, keep the water
            ElectricItem.manager.discharge(armour, EXTINGUISH_COST, Integer.MAX_VALUE, true, false, false);
            player.clearFire();
            break;
        }
    }

    @Override public int getEnergyPerDamage() { return 800; }
    @Override public double getDamageAbsorptionRatio() { return 0.9; }
}
