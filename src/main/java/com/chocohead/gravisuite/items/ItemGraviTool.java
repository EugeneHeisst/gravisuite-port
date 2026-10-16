package com.chocohead.gravisuite.items;

import com.chocohead.gravisuite.GraviSounds;
import ic2.api.tile.IWrenchable;
import ic2.core.block.misc.TileEntityBarrel;
import ic2.core.item.BaseElectricItem;
import ic2.core.item.tool.ItemTreetap;
import ic2.core.ref.Ic2Blocks;
import ic2.core.util.Keyboard;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.ItemAbilities;

/**
 * GraviTool: 300000 EU tier 2 multitool. Hoe (50 EU), treetap (50 EU), IC2 wrench (rotate/remove, 50 EU; ALT rotates
 * around the clicked axis) and screwdriver (turns horizontal blocks, 500 EU). Mode switch + right click changes the tool.
 */
public class ItemGraviTool extends BaseElectricItem {
    public static final double ROTATE = 50, HOE = 50, TAP = 50, SCREW = 500;

    public enum Mode {
        HOE(ChatFormatting.DARK_GREEN), TREETAP(ChatFormatting.GOLD), WRENCH(ChatFormatting.AQUA), SCREWDRIVER(ChatFormatting.LIGHT_PURPLE);
        public final ChatFormatting colour;
        public final String translationName = "gravisuite.graviTool." + name().toLowerCase(java.util.Locale.ROOT);
        Mode(ChatFormatting colour) { this.colour = colour; }
        public static Mode fromId(int id) { return values()[Math.floorMod(id, values().length)]; }
    }

    public ItemGraviTool(Properties p) { super(p.stacksTo(1).rarity(Rarity.UNCOMMON), 300000, 10000, 2); }

    public static boolean hasMode(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains("toolMode"); }
    public static Mode readMode(ItemStack stack) { return Mode.fromId(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("toolMode")); }
    public static void saveMode(ItemStack stack, Mode mode) { CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putInt("toolMode", mode.ordinal())); }

    @Override
    public Component getName(ItemStack stack) {
        return hasMode(stack) ? Component.translatable("gravisuite.graviTool.set", Component.translatable(getDescriptionId()), Component.translatable(readMode(stack).translationName)) : super.getName(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!Keyboard.isModeSwitchKeyDown(player)) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            Mode mode = Mode.fromId(readMode(stack).ordinal() + 1);
            saveMode(stack, mode);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), GraviSounds.TOOL_CHANGE.get(), SoundSource.PLAYERS, 1, 1);
            GraviPower.message(player, Component.translatable("gravisuite.graviTool.changeTool", Component.translatable(mode.translationName)), mode.colour);
        }
        return InteractionResultHolder.success(stack);
    }

    /** Wrench and screwdriver act before the block's own right click (onItemUseFirst of the original). */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        return switch (readMode(stack)) {
            case WRENCH -> onWrenchUse(stack, player, ctx.getLevel(), ctx.getClickedPos(), ctx.getClickedFace()) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            case SCREWDRIVER -> onScrewdriverUse(stack, player, ctx.getLevel(), ctx.getClickedPos()) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            default -> InteractionResult.PASS;
        };
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        ItemStack stack = ctx.getItemInHand();
        return switch (readMode(stack)) {
            case HOE -> onHoeUse(stack, player, ctx) ? InteractionResult.sidedSuccess(ctx.getLevel().isClientSide) : InteractionResult.FAIL;
            case TREETAP -> onTreeTapUse(stack, player, ctx.getLevel(), ctx.getClickedPos(), ctx.getClickedFace()) ? InteractionResult.sidedSuccess(ctx.getLevel().isClientSide) : InteractionResult.FAIL;
            default -> InteractionResult.PASS;
        };
    }

    protected boolean onHoeUse(ItemStack stack, Player player, UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        if (!player.mayUseItemAt(pos.relative(ctx.getClickedFace()), ctx.getClickedFace(), stack) || !GraviPower.hasNecessaryPower(stack, HOE, player)) return false;
        if (ctx.getClickedFace() == Direction.DOWN || !level.getBlockState(pos.above()).isAir()) return false;
        BlockState tilled = level.getBlockState(pos).getToolModifiedState(ctx, ItemAbilities.HOE_TILL, false);
        if (tilled == null) return false;
        if (level.isClientSide) return true;
        if (!GraviPower.checkNecessaryPower(stack, HOE, player, true)) return false;
        level.playSound(null, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1, 1);
        level.setBlock(pos, tilled, 11);
        return true;
    }

    protected boolean onTreeTapUse(ItemStack stack, Player player, Level level, BlockPos pos, Direction side) {
        if (side.getAxis() != Direction.Axis.Y && level.getBlockEntity(pos) instanceof TileEntityBarrel barrel) {
            if (barrel.treetapSide >= 2) return false;
            if (level.isClientSide) return true;
            if (!GraviPower.checkNecessaryPower(stack, TAP, player, true)) return false;
            barrel.setTreetapSide(side.get3DDataValue()); // the GraviTool keeps its tap, unlike a plain treetap
            return true;
        }
        if (!level.getBlockState(pos).is(Ic2Blocks.RUBBER_WOOD.get()) || !GraviPower.hasNecessaryPower(stack, TAP, player)) return false;
        if (level.isClientSide) return true;
        if (!ItemTreetap.attemptExtract(player, level, pos, side, null)) return false;
        level.playSound(null, pos, GraviSounds.TREETAP.get(), SoundSource.PLAYERS, 1, 1);
        return GraviPower.checkNecessaryPower(stack, TAP, player, true);
    }

    protected boolean onWrenchUse(ItemStack stack, Player player, Level level, BlockPos pos, Direction side) {
        if (!(level.getBlockEntity(pos) instanceof IWrenchable wrenchable)) return false;
        Direction current = wrenchable.getFacing(), newFacing;
        if (!Keyboard.isAltKeyDown(player)) newFacing = player.isShiftKeyDown() ? side.getOpposite() : side;
        else {
            Direction.Axis axis = side.getAxis();
            boolean forward = !player.isShiftKeyDown() && side.getAxisDirection() == Direction.AxisDirection.POSITIVE || player.isShiftKeyDown() && side.getAxisDirection() == Direction.AxisDirection.NEGATIVE;
            newFacing = forward ? current.getClockWise(axis) : current.getCounterClockWise(axis);
        }
        if (current != newFacing && wrenchable.wrenchCanSetFacing(player, newFacing)) {
            if (!GraviPower.hasNecessaryPower(stack, ROTATE, player)) return false;
            if (level.isClientSide) return true;
            wrenchable.setFacing(newFacing);
            return GraviPower.checkNecessaryPower(stack, ROTATE, player, false);
        }
        if (!wrenchable.wrenchCanRemove(player) || !GraviPower.hasNecessaryPower(stack, ROTATE, player)) return false;
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) return true;
        BlockState state = level.getBlockState(pos);
        if (CommonHooks.fireBlockBreak(level, sp.gameMode.getGameModeForPlayer(), sp, pos, state).isCanceled()) return false;
        var be = level.getBlockEntity(pos);
        List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, server, pos, be, player, stack));
        if (level.random.nextFloat() <= wrenchable.getWrenchDropRate()) {
            ItemStack drop = wrenchable.getWrenchDrop(player);
            if (!drop.isEmpty()) { if (drops.isEmpty()) drops.add(drop); else drops.set(0, drop); }
        }
        state.getBlock().playerWillDestroy(level, pos, state, player);
        if (!level.removeBlock(pos, false)) return false;
        for (ItemStack drop : drops) Block.popResource(level, pos, drop);
        return GraviPower.checkNecessaryPower(stack, ROTATE, player, false);
    }

    protected boolean onScrewdriverUse(ItemStack stack, Player player, Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !(state.getBlock() instanceof HorizontalDirectionalBlock) || !state.hasProperty(HorizontalDirectionalBlock.FACING)) return false;
        if (level.isClientSide) return GraviPower.hasNecessaryPower(stack, SCREW, player);
        if (!GraviPower.checkNecessaryPower(stack, SCREW, player, false)) return false;
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        level.setBlockAndUpdate(pos, state.setValue(HorizontalDirectionalBlock.FACING, player.isShiftKeyDown() ? facing.getCounterClockWise() : facing.getClockWise()));
        return true;
    }

    /** Tilling goes through the NeoForge tool action, which asks the item whether it can hoe. */
    @Override public boolean canPerformAction(ItemStack stack, net.neoforged.neoforge.common.ItemAbility ability) { return readMode(stack) == Mode.HOE && ItemAbilities.DEFAULT_HOE_ACTIONS.contains(ability); }

    @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) { return false; }
    @Override public boolean doesSneakBypassUse(ItemStack stack, net.minecraft.world.level.LevelReader level, BlockPos pos, Player player) { return true; }
}
