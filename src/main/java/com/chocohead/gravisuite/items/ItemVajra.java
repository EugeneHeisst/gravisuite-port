package com.chocohead.gravisuite.items;

import com.chocohead.gravisuite.Config;
import ic2.api.item.ElectricItem;
import ic2.core.item.tool.ItemElectricTool;
import ic2.core.util.Keyboard;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Vajra: 10 MEU tier 3, mines everything but bedrock almost instantly for 3333 EU, hits for 25 (6666 EU). Accurate mode
 * (mode switch + right click, config accurateEnabled) harvests with silk touch: the enchantment is added only for the break.
 */
public class ItemVajra extends ItemElectricTool {
    public static final String TEMP_SILK = "gravisuiteTempSilk";

    public ItemVajra(Properties p) {
        super(p.rarity(Rarity.EPIC), 3333, HarvestLevel.Iridium, EnumSet.of(ToolClass.Pickaxe, ToolClass.Shovel, ToolClass.Axe));
        maxCharge = 10000000;
        transferLimit = 60000;
        tier = 3;
        efficiencyOnProperMaterial = 20000;
    }

    public static boolean isAccurate(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("accurate"); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !Keyboard.isModeSwitchKeyDown(player)) return InteractionResultHolder.pass(stack);
        if (isAccurate(stack)) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putBoolean("accurate", false));
            GraviPower.message(player, Component.translatable("gravisuite.vajra.silkTouch", GraviPower.onOff(false)), ChatFormatting.DARK_RED);
        } else if (Config.ACCURATE.get()) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putBoolean("accurate", true));
            GraviPower.message(player, Component.translatable("gravisuite.vajra.silkTouch", GraviPower.onOff(true)), ChatFormatting.DARK_GREEN);
        } else GraviPower.message(player, Component.translatable("gravisuite.vajra.silkTouchDisabled"), ChatFormatting.DARK_RED);
        return InteractionResultHolder.success(stack);
    }

    @Override public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) { return !state.is(Blocks.BEDROCK); }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!ElectricItem.manager.canUse(stack, operationEnergyCost)) return 1;
        return state.is(Blocks.BEDROCK) ? 1 : efficiencyOnProperMaterial;
    }

    /** Called before the server copies the tool for the drops: adds silk touch X for this break if accurate mode can pay. */
    public void startAccurateBreak(ItemStack stack, Level level) {
        if (!Config.ACCURATE.get() || !isAccurate(stack) || !ElectricItem.manager.canUse(stack, operationEnergyCost)) return;
        var silk = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
        int previous = EnchantmentHelper.getItemEnchantmentLevel(silk, stack);
        if (previous >= 10) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putInt(TEMP_SILK, previous + 1)); // remembers the player's own level + 1
        EnchantmentHelper.updateEnchantments(stack, e -> e.set(silk, 10));
    }

    /** Restores the enchantments as they were before {@link #startAccurateBreak}. */
    public static void endAccurateBreak(ItemStack stack, Level level) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(TEMP_SILK)) return;
        int previous = tag.getInt(TEMP_SILK) - 1;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.remove(TEMP_SILK));
        var silk = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
        EnchantmentHelper.updateEnchantments(stack, e -> e.set(silk, previous));
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        boolean ret = super.mineBlock(stack, level, state, pos, entity);
        if (!level.isClientSide) endAccurateBreak(stack, level);
        return ret;
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        double damage = ElectricItem.manager.canUse(stack, operationEnergyCost * 2) ? 24 : 0;
        return ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, damage, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND).build();
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player) ElectricItem.manager.use(stack, operationEnergyCost * 2, attacker);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> list, TooltipFlag flag) {
        boolean on = isAccurate(stack);
        list.add(Component.translatable("gravisuite.vajra.silkTouch", GraviPower.onOff(on).copy().withStyle(on ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED)).withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, ctx, list, flag);
    }
}
