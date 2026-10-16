package com.chocohead.gravisuite.items;

import ic2.api.item.ElectricItem;
import ic2.core.item.tool.ItemElectricToolChainsaw;
import ic2.core.util.Keyboard;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

/** Advanced Chainsaw: the IC2 chainsaw at 45000 EU / tier 2 / speed 30 hitting for 13; shearing of blocks and mobs can be toggled. */
public class ItemAdvancedChainsaw extends ItemElectricToolChainsaw {
    public ItemAdvancedChainsaw(Properties p) {
        super(p.rarity(Rarity.UNCOMMON));
        maxCharge = 45000;
        transferLimit = 500;
        tier = 2;
        efficiencyOnProperMaterial = 30;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !Keyboard.isModeSwitchKeyDown(player)) return InteractionResultHolder.pass(stack);
        boolean disable = !shearDisabled(stack);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean("disableShear", disable));
        GraviPower.message(player, Component.translatable("gravisuite.advancedChainsaw.shear", GraviPower.onOff(!disable)), disable ? ChatFormatting.DARK_RED : ChatFormatting.DARK_GREEN);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        if (!ElectricItem.manager.canUse(stack, operationEnergyCost)) return ItemAttributeModifiers.EMPTY;
        return ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 13, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND).build();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> list, TooltipFlag flag) {
        boolean on = !shearDisabled(stack);
        list.add(Component.translatable("gravisuite.advancedChainsaw.shear", GraviPower.onOff(on)).withStyle(on ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED));
        super.appendHoverText(stack, ctx, list, flag);
    }
}
