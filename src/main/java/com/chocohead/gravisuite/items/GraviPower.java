package com.chocohead.gravisuite.items;

import com.chocohead.gravisuite.GraviSounds;
import ic2.api.item.ElectricItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** ItemGraviTool.hasNecessaryPower/checkNecessaryPower and Gravisuite.messagePlayer of the original. */
public final class GraviPower {
    private GraviPower() {}

    /** Tops the tool up from the armour (lappacks...) and tells whether it could pay usage EU now. */
    public static boolean hasNecessaryPower(ItemStack stack, double usage, Player player) {
        ElectricItem.manager.chargeFromArmor(stack, player);
        return Math.abs(ElectricItem.manager.discharge(stack, usage, Integer.MAX_VALUE, true, false, true) - usage) < 1e-6;
    }

    /** Pays usage EU; says so when it cannot. */
    public static boolean checkNecessaryPower(ItemStack stack, double usage, Player player, boolean suppressSound) {
        if (ElectricItem.manager.use(stack, usage, player)) {
            if (!suppressSound) player.level().playSound(null, player.getX(), player.getY(), player.getZ(), GraviSounds.WRENCH.get(), SoundSource.PLAYERS, 1, 1);
            return true;
        }
        message(player, Component.translatable("gravisuite.graviTool.noEnergy"), ChatFormatting.WHITE);
        return false;
    }

    public static void message(Player player, Component message, ChatFormatting colour) {
        if (!player.level().isClientSide) player.displayClientMessage(message.copy().withStyle(colour), false);
    }

    public static Component onOff(boolean on) { return Component.translatable(on ? "gravisuite.message.on" : "gravisuite.message.off"); }
}
