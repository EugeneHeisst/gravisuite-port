package com.chocohead.gravisuite.items;

import ic2.api.item.ElectricItem;
import ic2.core.item.tool.ItemDrill;
import ic2.core.util.Keyboard;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Advanced Diamond Drill: four modes per stack (normal 35/160 EU, low power 16/80, fine 10/50, big holes 16/160 breaking the
 * 3x3 around the aimed block). Mode switch key + right click cycles the mode.
 */
public class ItemAdvancedDrill extends ItemDrill {
    public enum DrillMode {
        NORMAL(ChatFormatting.DARK_GREEN, 35, 160), LOW_POWER(ChatFormatting.GOLD, 16, 80), FINE(ChatFormatting.AQUA, 10, 50), BIG_HOLES(ChatFormatting.LIGHT_PURPLE, 16, 160);
        public final ChatFormatting colour;
        public final float drillSpeed;
        public final double energyCost;
        public final String translationName;
        DrillMode(ChatFormatting colour, float speed, double cost) {
            this.colour = colour; this.drillSpeed = speed; this.energyCost = cost;
            this.translationName = "gravisuite.advancedDrill." + toCamel(name());
        }
        private static String toCamel(String s) {
            StringBuilder b = new StringBuilder();
            boolean up = false;
            for (char c : s.toLowerCase(java.util.Locale.ROOT).toCharArray()) { if (c == '_') { up = true; continue; } b.append(up ? Character.toUpperCase(c) : c); up = false; }
            return b.toString();
        }
        public static DrillMode fromId(int id) { return values()[Math.floorMod(id, values().length)]; }
    }

    public ItemAdvancedDrill(Properties p) { super(p.rarity(Rarity.UNCOMMON), 160, HarvestLevel.Iridium, 45000, 500, 2, 35, false); }

    public static DrillMode readMode(ItemStack stack) { return DrillMode.fromId(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("toolMode")); }
    public static void saveMode(ItemStack stack, DrillMode mode) { CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putInt("toolMode", mode.ordinal())); }

    /** The current mode's cost (the original kept it in the shared item; here it follows each stack). */
    @Override public double getEnergyCost(ItemStack stack) { return readMode(stack).energyCost; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!Keyboard.isModeSwitchKeyDown(player)) return super.use(level, player, hand);
        if (!level.isClientSide) {
            DrillMode mode = DrillMode.fromId(readMode(stack).ordinal() + 1);
            saveMode(stack, mode);
            GraviPower.message(player, Component.translatable("gravisuite.advancedDrill.mode", Component.translatable(mode.translationName)), mode.colour);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        DrillMode mode = readMode(stack);
        if (!ElectricItem.manager.canUse(stack, mode.energyCost)) return 1;
        return isEffectiveOn(state) ? mode.drillSpeed : 1;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        // onBlockStartBreak of the original: ServerPlayerGameMode.destroyBlock calls mineBlock only after the centre's BreakEvent
        // (every listener, including late protection ones) and its other checks have passed, before the centre is removed and paid.
        if (!level.isClientSide && entity instanceof ServerPlayer player && readMode(stack) == DrillMode.BIG_HOLES) breakBigHole(player, stack, pos);
        if (state.getDestroySpeed(level, pos) != 0) {
            double cost = getEnergyCost(stack);
            if (entity != null) ElectricItem.manager.use(stack, cost, entity);
            else ElectricItem.manager.discharge(stack, cost, tier, true, false, false);
        }
        return true;
    }

    /** The 3x3 (in the plane of the aimed side) around the block the player looks at, mineable blocks only. */
    public static List<BlockPos> getBrokenBlocks(Player player, BlockPos pos, Direction side) {
        int xm = side.getAxis() == Direction.Axis.X ? 0 : 1, ym = side.getAxis() == Direction.Axis.Y ? 0 : 1, zm = side.getAxis() == Direction.Axis.Z ? 0 : 1;
        List<BlockPos> list = new ArrayList<>(9);
        for (int x = -xm; x <= xm; x++) for (int y = -ym; y <= ym; y++) for (int z = -zm; z <= zm; z++) {
            BlockPos p = pos.offset(x, y, z);
            if (canBlockBeMined(player.level(), p, player, false)) list.add(p);
        }
        return list;
    }

    /** Harvestable, a drill material (stone, dirt, sand...: the pickaxe/shovel tags) and not unbreakable. */
    public static boolean canBlockBeMined(Level level, BlockPos pos, Player player, boolean skipEffectivity) {
        if (!level.isLoaded(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0 || state.getDestroyProgress(player, level, pos) <= 0) return false;
        if (!state.canHarvestBlock(level, pos, player)) return false;
        return skipEffectivity || player.getMainHandItem().getItem() instanceof ItemAdvancedDrill d && d.isEffectiveOn(state);
    }

    public static BlockHitResult aim(Player player) {
        double reach = player.blockInteractionRange();
        var eye = player.getEyePosition();
        var hit = player.level().clip(new ClipContext(eye, eye.add(player.getViewVector(1).scale(reach)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }

    private static boolean breaking;

    /** Big holes: called on the server when the drill breaks pos; breaks the rest of the 3x3 through the normal player break path. */
    public static void breakBigHole(ServerPlayer player, ItemStack stack, BlockPos pos) {
        if (breaking) return;
        BlockHitResult hit = aim(player);
        Direction side = hit != null && hit.getBlockPos().equals(pos) ? hit.getDirection() : Direction.orderedByNearest(player)[0].getOpposite();
        List<BlockPos> blocks = getBrokenBlocks(player, pos, side);
        blocks.remove(pos); // the block being broken goes through the normal path
        breaking = true;
        try {
            for (BlockPos p : blocks) {
                // like the original's single loop, the aimed block (broken after this, by the normal path) must stay payable
                if (!GraviPower.hasNecessaryPower(stack, getCost(stack) * 2, player)) {
                    GraviPower.message(player, Component.translatable("gravisuite.advancedDrill.ranOut"), ChatFormatting.WHITE);
                    break;
                }
                if (player.getMainHandItem() != stack) break;
                player.gameMode.destroyBlock(p);
            }
        } finally {
            breaking = false;
        }
    }

    private static double getCost(ItemStack stack) { return ((ItemAdvancedDrill)stack.getItem()).getEnergyCost(stack); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> list, TooltipFlag flag) {
        list.add(Component.translatable("gravisuite.advancedDrill.mode", Component.translatable(readMode(stack).translationName).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, ctx, list, flag);
    }
}
