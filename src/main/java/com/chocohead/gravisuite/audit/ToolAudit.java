package com.chocohead.gravisuite.audit;

import com.chocohead.gravisuite.Config;
import com.chocohead.gravisuite.GraviItems;
import com.chocohead.gravisuite.items.ItemAdvancedDrill;
import com.chocohead.gravisuite.items.ItemGraviTool;
import com.chocohead.gravisuite.items.ItemVajra;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.core.util.Keyboard;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Advanced drill, advanced chainsaw, GraviTool and Vajra against the original's numbers and behaviour. */
@EventBusSubscriber(modid = com.chocohead.gravisuite.Gravisuite.MODID)
public final class ToolAudit {
    private ToolAudit() {}

    /** A pretend claim: breaking this position is refused like a protection mod would. */
    static BlockPos protectedPos;
    @SubscribeEvent static void protect(BlockEvent.BreakEvent event) { if (event.getPos().equals(protectedPos)) event.setCanceled(true); }

    private static int count(ServerLevel l, BlockPos p, Item item) {
        int n = 0;
        for (var e : l.getEntitiesOfClass(ItemEntity.class, new AABB(p).inflate(4))) if (e.getItem().is(item)) n += e.getItem().getCount();
        return n;
    }
    private static void clearDrops(ServerLevel l, BlockPos p) { l.getEntitiesOfClass(ItemEntity.class, new AABB(p).inflate(6)).forEach(ItemEntity::discard); }
    private static double charge(ItemStack s) { return ElectricItem.manager.getCharge(s); }
    private static ItemStack charged(Item item, double eu) { ItemStack s = new ItemStack(item); ElectricItem.manager.charge(s, eu, Integer.MAX_VALUE, true, false); return s; }
    private static void face(FakePlayer pl, BlockPos stand, BlockPos target) {
        pl.moveTo(stand.getX() + .5, stand.getY(), stand.getZ() + .5, 0, 0);
        pl.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(target));
    }
    private static UseOnContext ctx(ServerLevel l, FakePlayer pl, ItemStack s, BlockPos pos, Direction side) {
        pl.setItemInHand(InteractionHand.MAIN_HAND, s);
        return new UseOnContext(l, pl, InteractionHand.MAIN_HAND, s, new BlockHitResult(Vec3.atCenterOf(pos), side, pos, false));
    }
    private static void modeSwitch(ServerLevel l, FakePlayer pl, ItemStack s) {
        Keyboard.processKeyUpdate(pl, 1 << Keyboard.Key.modeSwitch.ordinal());
        pl.setItemInHand(InteractionHand.MAIN_HAND, s);
        s.getItem().use(l, pl, InteractionHand.MAIN_HAND);
        Keyboard.processKeyUpdate(pl, 0);
    }
    private static boolean stats(Item item, double max, int tier, double transfer) {
        var api = (IElectricItem)item;
        ItemStack s = new ItemStack(item);
        return api.getMaxCharge(s) == max && api.getTier(s) == tier && api.getTransferLimit(s) == transfer && !api.canProvideEnergy(s);
    }

    public static void run(ServerLevel l, FakePlayer pl, BiConsumer<String, Object[]> unused) {
        BlockPos o = new BlockPos(-8, 100, 24);
        for (int cx = -2; cx <= 2; cx++) for (int cz = 0; cz <= 3; cz++) l.setChunkForced(cx, cz, true);
        for (var p : BlockPos.betweenClosed(o.offset(-6, -2, -6), o.offset(26, 4, 6))) l.setBlock(p, p.getY() < o.getY() ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
        pl.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        pl.getInventory().clearContent();
        for (var slot : EquipmentSlot.values()) pl.setItemSlot(slot, ItemStack.EMPTY);

        // ---------- advanced drill
        check("advanced drill 45000 EU / tier 2 / 500 transfer", stats(GraviItems.ADVANCED_DRILL.get(), 45000, 2, 500), "drill");
        ItemStack drill = charged(GraviItems.ADVANCED_DRILL.get(), 45000), other = charged(GraviItems.ADVANCED_DRILL.get(), 45000);
        float[] speeds = new float[4];
        for (int i = 0; i < 4; i++) { speeds[i] = drill.getDestroySpeed(Blocks.STONE.defaultBlockState()); modeSwitch(l, pl, drill); }
        check("drill modes 35 / 16 / 10 / 16 speed and back to normal", speeds[0] == 35 && speeds[1] == 16 && speeds[2] == 10 && speeds[3] == 16 && ItemAdvancedDrill.readMode(drill) == ItemAdvancedDrill.DrillMode.NORMAL, java.util.Arrays.toString(speeds));
        modeSwitch(l, pl, drill);
        check("drill mode belongs to the stack", ItemAdvancedDrill.readMode(drill) == ItemAdvancedDrill.DrillMode.LOW_POWER && ItemAdvancedDrill.readMode(other) == ItemAdvancedDrill.DrillMode.NORMAL, ItemAdvancedDrill.readMode(other));
        check("uncharged drill digs at speed 1", new ItemStack(GraviItems.ADVANCED_DRILL.get()).getDestroySpeed(Blocks.STONE.defaultBlockState()) == 1, "empty");
        BlockPos s1 = o.offset(0, 0, 3);
        l.setBlock(s1, Blocks.STONE.defaultBlockState(), 3);
        pl.setItemInHand(InteractionHand.MAIN_HAND, drill);
        double before = charge(drill);
        pl.gameMode.destroyBlock(s1);
        check("low power mode mines stone for 80 EU", l.getBlockState(s1).isAir() && before - charge(drill) == 80 && count(l, s1, Items.COBBLESTONE) == 1, before - charge(drill));
        clearDrops(l, s1);
        ItemAdvancedDrill.saveMode(drill, ItemAdvancedDrill.DrillMode.BIG_HOLES);
        // 3x3 wall in the x plane at x+5: stone, dirt, obsidian, planks (axe block, stays), bedrock (stays), a protected stone
        BlockPos c = o.offset(5, 1, 0);
        for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) l.setBlock(c.offset(0, y, z), Blocks.STONE.defaultBlockState(), 3);
        l.setBlock(c.offset(0, 1, -1), Blocks.DIRT.defaultBlockState(), 3);
        l.setBlock(c.offset(0, 1, 1), Blocks.OBSIDIAN.defaultBlockState(), 3);
        l.setBlock(c.offset(0, -1, -1), Blocks.OAK_PLANKS.defaultBlockState(), 3);
        l.setBlock(c.offset(0, -1, 1), Blocks.BEDROCK.defaultBlockState(), 3);
        protectedPos = c.offset(0, 0, 1);
        face(pl, o.offset(2, 0, 0), c);
        pl.setItemInHand(InteractionHand.MAIN_HAND, drill);
        before = charge(drill);
        pl.gameMode.destroyBlock(c);
        int air = 0;
        for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) if (l.getBlockState(c.offset(0, y, z)).isAir()) air++;
        check("big holes mines the 3x3 through the normal break, skipping axe blocks, bedrock and protected blocks",
            air == 6 && l.getBlockState(c.offset(0, -1, -1)).is(Blocks.OAK_PLANKS) && l.getBlockState(c.offset(0, -1, 1)).is(Blocks.BEDROCK) && l.getBlockState(protectedPos).is(Blocks.STONE)
                && count(l, c, Items.OBSIDIAN) == 1 && count(l, c, Items.DIRT) == 1 && count(l, c, Items.COBBLESTONE) == 4 && before - charge(drill) == 6 * 160, "air=" + air + " eu=" + (before - charge(drill)));
        protectedPos = null;
        clearDrops(l, c);
        for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) l.setBlock(c.offset(0, y, z), Blocks.STONE.defaultBlockState(), 3);
        ItemStack weak = charged(GraviItems.ADVANCED_DRILL.get(), 160 * 3);
        ItemAdvancedDrill.saveMode(weak, ItemAdvancedDrill.DrillMode.BIG_HOLES);
        pl.setItemInHand(InteractionHand.MAIN_HAND, weak);
        pl.gameMode.destroyBlock(c);
        air = 0;
        for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) if (l.getBlockState(c.offset(0, y, z)).isAir()) air++;
        check("big holes stops when the energy runs out", air == 3 && charge(weak) == 0, "air=" + air + " eu=" + charge(weak));
        clearDrops(l, c);
        // a protection listener registered after the mod's own handlers (LOWEST, late) cancels only the aimed centre:
        // the whole 3x3 must stay, with no drops and no EU, because the centre break was never approved
        for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) l.setBlock(c.offset(0, y, z), Blocks.STONE.defaultBlockState(), 3);
        java.util.function.Consumer<BlockEvent.BreakEvent> late = e -> { if (e.getPos().equals(c)) e.setCanceled(true); };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, late);
        ItemStack lateDrill = charged(GraviItems.ADVANCED_DRILL.get(), 45000);
        ItemAdvancedDrill.saveMode(lateDrill, ItemAdvancedDrill.DrillMode.BIG_HOLES);
        pl.setItemInHand(InteractionHand.MAIN_HAND, lateDrill);
        boolean broke;
        try { broke = pl.gameMode.destroyBlock(c); } finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(late); }
        int stone = 0;
        for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) if (l.getBlockState(c.offset(0, y, z)).is(Blocks.STONE)) stone++;
        check("a late protection cancel of the centre keeps all nine blocks, no drops, no EU", !broke && stone == 9 && count(l, c, Items.COBBLESTONE) == 0 && charge(lateDrill) == 45000,
            "broke=" + broke + " stone=" + stone + " eu=" + charge(lateDrill));
        pl.setItemInHand(InteractionHand.MAIN_HAND, lateDrill);
        double lateBefore = charge(lateDrill);
        pl.gameMode.destroyBlock(c);
        int open = 0;
        for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) if (l.getBlockState(c.offset(0, y, z)).isAir()) open++;
        check("without the cancel the same drill still clears the 3x3 for 9 x 160 EU", open == 9 && lateBefore - charge(lateDrill) == 9 * 160 && count(l, c, Items.COBBLESTONE) == 9, "open=" + open + " eu=" + (lateBefore - charge(lateDrill)));
        clearDrops(l, c);
        for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) l.setBlock(c.offset(0, y, z), Blocks.AIR.defaultBlockState(), 3);

        // ---------- advanced chainsaw
        check("advanced chainsaw 45000 EU / tier 2 / 500 transfer / speed 30", stats(GraviItems.ADVANCED_CHAINSAW.get(), 45000, 2, 500) && charged(GraviItems.ADVANCED_CHAINSAW.get(), 45000).getDestroySpeed(Blocks.OAK_LOG.defaultBlockState()) == 30, "chainsaw");
        ItemStack saw = charged(GraviItems.ADVANCED_CHAINSAW.get(), 45000);
        double sawDamage = saw.getAttributeModifiers().modifiers().stream().filter(m -> m.attribute().is(Attributes.ATTACK_DAMAGE)).mapToDouble(m -> m.modifier().amount()).sum();
        double emptyDamage = new ItemStack(GraviItems.ADVANCED_CHAINSAW.get()).getAttributeModifiers().modifiers().stream().filter(m -> m.attribute().is(Attributes.ATTACK_DAMAGE)).mapToDouble(m -> m.modifier().amount()).sum();
        check("charged chainsaw hits for 13, empty for nothing extra", sawDamage == 13 && emptyDamage == 0, sawDamage + "/" + emptyDamage);
        var sheep = EntityType.SHEEP.create(l);
        sheep.moveTo(o.getX() + 10.5, o.getY(), o.getZ() + .5);
        sheep.setNoAi(true);
        l.addFreshEntity(sheep);
        pl.setItemInHand(InteractionHand.MAIN_HAND, saw);
        before = charge(saw);
        saw.getItem().interactLivingEntity(saw, pl, sheep, InteractionHand.MAIN_HAND);
        check("chainsaw shears sheep for 100 EU", sheep.isSheared() && before - charge(saw) == 100 && count(l, sheep.blockPosition(), Items.WHITE_WOOL) > 0, before - charge(saw));
        clearDrops(l, sheep.blockPosition());
        modeSwitch(l, pl, saw);
        var sheep2 = EntityType.SHEEP.create(l);
        sheep2.moveTo(o.getX() + 12.5, o.getY(), o.getZ() + .5);
        sheep2.setNoAi(true);
        l.addFreshEntity(sheep2);
        saw.getItem().interactLivingEntity(saw, pl, sheep2, InteractionHand.MAIN_HAND);
        BlockPos leaves = o.offset(14, 0, 0);
        l.setBlock(leaves, Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true), 3);
        pl.setItemInHand(InteractionHand.MAIN_HAND, saw);
        pl.gameMode.destroyBlock(leaves);
        check("shearing off: no sheep shearing, leaves do not drop themselves", !sheep2.isSheared() && count(l, leaves, Items.OAK_LEAVES) == 0, "off");
        clearDrops(l, leaves);
        modeSwitch(l, pl, saw);
        l.setBlock(leaves, Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true), 3);
        pl.setItemInHand(InteractionHand.MAIN_HAND, saw);
        pl.gameMode.destroyBlock(leaves);
        check("shearing on: leaves drop themselves", count(l, leaves, Items.OAK_LEAVES) == 1, count(l, leaves, Items.OAK_LEAVES));
        clearDrops(l, leaves);
        sheep.discard();
        sheep2.discard();

        // ---------- Vajra
        check("Vajra 10 MEU / tier 3 / 60000 transfer", stats(GraviItems.VAJRA.get(), 10000000, 3, 60000), "vajra");
        ItemStack vajra = charged(GraviItems.VAJRA.get(), 1e7);
        check("Vajra digs everything at 20000 but bedrock", vajra.getDestroySpeed(Blocks.OBSIDIAN.defaultBlockState()) == 20000 && vajra.getDestroySpeed(Blocks.OAK_LOG.defaultBlockState()) == 20000
            && vajra.getDestroySpeed(Blocks.BEDROCK.defaultBlockState()) == 1 && vajra.isCorrectToolForDrops(Blocks.IRON_BLOCK.defaultBlockState()) && !vajra.isCorrectToolForDrops(Blocks.BEDROCK.defaultBlockState()), "speed");
        BlockPos v = o.offset(16, 0, 0);
        l.setBlock(v, Blocks.STONE.defaultBlockState(), 3);
        pl.setItemInHand(InteractionHand.MAIN_HAND, vajra);
        before = charge(vajra);
        pl.gameMode.destroyBlock(v);
        check("Vajra breaks stone normally for 3333 EU", count(l, v, Items.COBBLESTONE) == 1 && before - charge(vajra) == 3333, (before - charge(vajra)) + " cobble=" + count(l, v, Items.COBBLESTONE) + " stone=" + count(l, v, Items.STONE));
        clearDrops(l, v);
        var enchants = l.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        vajra.enchant(enchants.getOrThrow(Enchantments.UNBREAKING), 2);
        modeSwitch(l, pl, vajra);
        l.setBlock(v, Blocks.STONE.defaultBlockState(), 3);
        pl.setItemInHand(InteractionHand.MAIN_HAND, vajra);
        pl.gameMode.destroyBlock(v);
        check("accurate Vajra harvests stone itself and puts its enchantments back", ItemVajra.isAccurate(vajra) && count(l, v, Items.STONE) == 1 && count(l, v, Items.COBBLESTONE) == 0
            && EnchantmentHelper.getTagEnchantmentLevel(enchants.getOrThrow(Enchantments.SILK_TOUCH), vajra) == 0 && EnchantmentHelper.getItemEnchantmentLevel(enchants.getOrThrow(Enchantments.UNBREAKING), vajra) == 2, vajra.getEnchantments());
        clearDrops(l, v);

        // Register after the normal subscribers so this really cancels AFTER the old temporary-silk BreakEvent hook.
        java.util.function.Consumer<BlockEvent.BreakEvent> lateProtection = e -> { if (e.getPlayer() == pl && e.getPos().equals(v)) e.setCanceled(true); };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, false, BlockEvent.BreakEvent.class, lateProtection);
        l.setBlock(v, Blocks.STONE.defaultBlockState(), 3);
        before = charge(vajra);
        boolean denied;
        try { denied = !pl.gameMode.destroyBlock(v); }
        finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(lateProtection); }
        check("late protection cancellation never persists temporary silk or spends EU", denied && l.getBlockState(v).is(Blocks.STONE) && count(l, v, Items.STONE) == 0
            && before == charge(vajra) && EnchantmentHelper.getTagEnchantmentLevel(enchants.getOrThrow(Enchantments.SILK_TOUCH), vajra) == 0
            && EnchantmentHelper.getTagEnchantmentLevel(enchants.getOrThrow(Enchantments.UNBREAKING), vajra) == 2
            && !vajra.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().contains(ItemVajra.TEMP_SILK), vajra.getEnchantments());
        modeSwitch(l, pl, vajra);
        check("turning accurate mode off after a canceled break leaves no silk effect", !ItemVajra.isAccurate(vajra)
            && EnchantmentHelper.getItemEnchantmentLevel(enchants.getOrThrow(Enchantments.SILK_TOUCH), vajra) == 0, vajra.getEnchantments());
        modeSwitch(l, pl, vajra);

        vajra.enchant(enchants.getOrThrow(Enchantments.SILK_TOUCH), 1);
        l.setBlock(v, Blocks.STONE.defaultBlockState(), 3);
        pl.gameMode.destroyBlock(v);
        check("a silk touch the player gave the Vajra stays at its level", EnchantmentHelper.getTagEnchantmentLevel(enchants.getOrThrow(Enchantments.SILK_TOUCH), vajra) == 1 && count(l, v, Items.STONE) == 1, vajra.getEnchantments());
        clearDrops(l, v);
        modeSwitch(l, pl, vajra);
        check("turning accurate mode off preserves the player's own silk touch", EnchantmentHelper.getItemEnchantmentLevel(enchants.getOrThrow(Enchantments.SILK_TOUCH), vajra) == 1, vajra.getEnchantments());

        ItemStack lastUse = charged(GraviItems.VAJRA.get(), 3333);
        modeSwitch(l, pl, lastUse);
        l.setBlock(v, Blocks.STONE.defaultBlockState(), 3);
        pl.gameMode.destroyBlock(v);
        check("the last full Vajra operation still silk-harvests and has no permanent enchantment", charge(lastUse) == 0 && count(l, v, Items.STONE) == 1
            && lastUse.getTagEnchantments().isEmpty(), lastUse.getTagEnchantments());
        clearDrops(l, v);

        ItemStack legacy = charged(GraviItems.VAJRA.get(), 10000);
        legacy.enchant(enchants.getOrThrow(Enchantments.SILK_TOUCH), 10);
        legacy.enchant(enchants.getOrThrow(Enchantments.UNBREAKING), 2);
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, legacy, t -> t.putInt(ItemVajra.TEMP_SILK, 2));
        legacy.getItem().inventoryTick(legacy, l, pl, 0, false);
        check("a saved legacy temporary silk is repaired without removing the player's own enchantments",
            EnchantmentHelper.getTagEnchantmentLevel(enchants.getOrThrow(Enchantments.SILK_TOUCH), legacy) == 1
                && EnchantmentHelper.getTagEnchantmentLevel(enchants.getOrThrow(Enchantments.UNBREAKING), legacy) == 2
                && !legacy.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().contains(ItemVajra.TEMP_SILK), legacy.getTagEnchantments());

        ItemStack flat = new ItemStack(GraviItems.VAJRA.get());
        com.chocohead.gravisuite.items.ItemVajra.isAccurate(flat);
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, flat, t -> t.putBoolean("accurate", true));
        l.setBlock(v, Blocks.STONE.defaultBlockState(), 3);
        pl.setItemInHand(InteractionHand.MAIN_HAND, flat);
        pl.gameMode.destroyBlock(v);
        check("accurate mode without energy breaks without silk touch", count(l, v, Items.COBBLESTONE) == 1 && flat.getEnchantments().isEmpty(), flat.getEnchantments());
        clearDrops(l, v);
        Config.ACCURATE.set(false);
        ItemStack locked = charged(GraviItems.VAJRA.get(), 1e7);
        modeSwitch(l, pl, locked);
        Config.ACCURATE.set(true);
        check("accurate mode cannot be enabled when the config forbids it", !ItemVajra.isAccurate(locked), "locked");
        double vd = vajra.getAttributeModifiers().modifiers().stream().filter(m -> m.attribute().is(Attributes.ATTACK_DAMAGE)).mapToDouble(m -> m.modifier().amount()).sum();
        double ed = new ItemStack(GraviItems.VAJRA.get()).getAttributeModifiers().modifiers().stream().filter(m -> m.attribute().is(Attributes.ATTACK_DAMAGE)).mapToDouble(m -> m.modifier().amount()).sum();
        var zombie = EntityType.ZOMBIE.create(l);
        before = charge(vajra);
        vajra.getItem().hurtEnemy(vajra, zombie, pl);
        check("Vajra hits for 25 with 6666 EU, 1 when empty", vd == 24 && ed == 0 && before - charge(vajra) == 6666, vd + "/" + ed);
        nativeVajraFirstHit(l, pl, o.offset(16, 0, 3));

        // ---------- GraviTool
        check("GraviTool 300000 EU / tier 2 / 10000 transfer", stats(GraviItems.GRAVITOOL.get(), 300000, 2, 10000), "gravitool");
        ItemStack tool = charged(GraviItems.GRAVITOOL.get(), 300000);
        String plain = tool.getHoverName().getString();
        modeSwitch(l, pl, tool);
        check("GraviTool mode switch and named mode", ItemGraviTool.readMode(tool) == ItemGraviTool.Mode.TREETAP && !tool.getHoverName().getString().equals(plain) && tool.getHoverName().getString().contains("("), tool.getHoverName().getString());
        ItemGraviTool.saveMode(tool, ItemGraviTool.Mode.HOE);
        BlockPos g = o.offset(18, -1, 0);
        l.setBlock(g, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        before = charge(tool);
        tool.getItem().useOn(ctx(l, pl, tool, g, Direction.UP));
        check("hoe mode tills grass for 50 EU", l.getBlockState(g).is(Blocks.FARMLAND) && before - charge(tool) == 50, l.getBlockState(g) + " eu=" + (before - charge(tool)) + " above=" + l.getBlockState(g.above()));
        BlockPos g2 = g.east();
        l.setBlock(g2, Blocks.DIRT.defaultBlockState(), 3);
        l.setBlock(g2.above(), Blocks.STONE.defaultBlockState(), 3);
        tool.getItem().useOn(ctx(l, pl, tool, g2, Direction.UP));
        check("hoe mode needs air above", l.getBlockState(g2).is(Blocks.DIRT), l.getBlockState(g2));
        l.setBlock(g2.above(), Blocks.AIR.defaultBlockState(), 3);
        ItemStack empty = new ItemStack(GraviItems.GRAVITOOL.get());
        pl.setItemSlot(EquipmentSlot.CHEST, GraviItems.ULTIMATE_LAPPACK.get().charged());
        empty.getItem().useOn(ctx(l, pl, empty, g2, Direction.UP));
        check("an empty GraviTool draws from the worn lappack", l.getBlockState(g2).is(Blocks.FARMLAND), l.getBlockState(g2));
        pl.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        ItemStack dry = new ItemStack(GraviItems.GRAVITOOL.get());
        l.setBlock(g2, Blocks.DIRT.defaultBlockState(), 3);
        dry.getItem().useOn(ctx(l, pl, dry, g2, Direction.UP));
        check("no energy, no tilling", l.getBlockState(g2).is(Blocks.DIRT), l.getBlockState(g2));

        ItemGraviTool.saveMode(tool, ItemGraviTool.Mode.TREETAP);
        BlockPos rw = o.offset(20, 0, 0);
        l.setBlock(rw, ic2.core.ref.Ic2Blocks.RUBBER_WOOD.get().defaultBlockState().setValue(ic2.core.block.rubber.BlockRubWood.HOLE, ic2.core.block.rubber.BlockRubWood.Hole.wet).setValue(ic2.core.block.rubber.BlockRubWood.FACING, Direction.NORTH), 3);
        before = charge(tool);
        tool.getItem().useOn(ctx(l, pl, tool, rw, Direction.NORTH));
        check("treetap mode takes resin from a wet hole for 50 EU", count(l, rw, ic2.core.ref.Ic2Items.get("resin")) > 0 && before - charge(tool) == 50, before - charge(tool));
        clearDrops(l, rw);
        BlockPos br = o.offset(22, 0, 0);
        l.setBlock(br, ic2.core.ref.Ic2Blocks.BARREL.get().defaultBlockState(), 3);
        tool.getItem().useOn(ctx(l, pl, tool, br, Direction.SOUTH));
        check("treetap mode fits a tap on a booze barrel and keeps the GraviTool", l.getBlockEntity(br) instanceof ic2.core.block.misc.TileEntityBarrel b && b.treetapSide == Direction.SOUTH.get3DDataValue() && !tool.isEmpty(), "barrel");

        ItemGraviTool.saveMode(tool, ItemGraviTool.Mode.WRENCH);
        BlockPos m = o.offset(24, 0, 0);
        l.setBlock(m, ic2.core.ref.Ic2Blocks.ELECTRIC_KINETIC_GENERATOR.get().defaultBlockState(), 3);
        before = charge(tool);
        tool.getItem().onItemUseFirst(tool, ctx(l, pl, tool, m, Direction.EAST));
        Direction turned = l.getBlockEntity(m) instanceof ic2.api.tile.IWrenchable w ? w.getFacing() : null;
        check("wrench mode turns a rotatable IC2 machine to the clicked side for 50 EU", turned == Direction.EAST && before - charge(tool) == 50, turned);
        tool.getItem().onItemUseFirst(tool, ctx(l, pl, tool, m, Direction.EAST));
        check("wrench mode removes it when it already faces that side", l.getBlockState(m).isAir() && (count(l, m, ic2.core.ref.Ic2Blocks.ELECTRIC_KINETIC_GENERATOR.get().asItem()) + count(l, m, ic2.core.ref.Ic2Blocks.MACHINE.get().asItem())) == 1, l.getBlockState(m));
        clearDrops(l, m);
        l.setBlock(m, ic2.core.ref.Ic2Blocks.MACERATOR.get().defaultBlockState(), 3);
        tool.getItem().onItemUseFirst(tool, ctx(l, pl, tool, m, Direction.EAST));
        check("wrench mode takes off a machine IC2 does not let turn", l.getBlockState(m).isAir(), l.getBlockState(m));
        clearDrops(l, m);
        ItemGraviTool.saveMode(tool, ItemGraviTool.Mode.SCREWDRIVER);
        l.setBlock(m, Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.NORTH), 3);
        before = charge(tool);
        tool.getItem().onItemUseFirst(tool, ctx(l, pl, tool, m, Direction.UP));
        BlockState turnedState = l.getBlockState(m);
        pl.setShiftKeyDown(true);
        tool.getItem().onItemUseFirst(tool, ctx(l, pl, tool, m, Direction.UP));
        pl.setShiftKeyDown(false);
        check("screwdriver mode turns a horizontal block (carved pumpkin) clockwise for 500 EU, sneaking turns it back", turnedState.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING) == Direction.EAST && l.getBlockState(m).getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING) == Direction.NORTH && before - charge(tool) == 1000, turnedState);
        l.setBlock(m, Blocks.FURNACE.defaultBlockState(), 3);
        before = charge(tool);
        tool.getItem().onItemUseFirst(tool, ctx(l, pl, tool, m, Direction.UP));
        check("screwdriver leaves other blocks (furnace) and the energy alone", l.getBlockState(m).getValue(FurnaceBlock.FACING) == Direction.NORTH && before == charge(tool), l.getBlockState(m));
        l.removeBlock(m, false);
        pl.getInventory().clearContent();
    }

    /** Actual Player.attack: its damage is calculated before Item.hurtEnemy refills the tool from armour. */
    private static void nativeVajraFirstHit(ServerLevel level, FakePlayer player, BlockPos pos) {
        player.getInventory().clearContent();
        for (var slot : EquipmentSlot.values()) player.setItemSlot(slot, ItemStack.EMPTY);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setOnGround(true);
        player.setSprinting(false);
        player.fallDistance = 0;
        player.setDeltaMovement(Vec3.ZERO);
        var tool = new ItemStack(GraviItems.VAJRA.get());
        var pack = GraviItems.ULTIMATE_LAPPACK.get().charged();
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        player.setItemSlot(EquipmentSlot.CHEST, pack);
        var target = EntityType.COW.create(level);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        target.setHealth(100);
        target.setNoAi(true);
        target.moveTo(Vec3.atCenterOf(pos));
        target.invulnerableTime = 0;
        level.addFreshEntity(target);
        try {
            // FakePlayer.tick is a no-op. Invoke vanilla's equipment reconciliation, then provide a full attack cooldown.
            var equipment = LivingEntity.class.getDeclaredMethod("detectEquipmentUpdates");
            equipment.setAccessible(true);
            equipment.invoke(player);
            var cooldown = LivingEntity.class.getDeclaredField("attackStrengthTicker");
            cooldown.setAccessible(true);
            cooldown.setInt(player, 20);
            check("native Vajra first-hit fixture starts with an empty tool, charged pack and full cooldown",
                charge(tool) == 0 && charge(pack) == 6e7 && player.getAttackStrengthScale(.5f) == 1
                    && player.getAttributeValue(Attributes.ATTACK_DAMAGE) == 1, "damage=" + player.getAttributeValue(Attributes.ATTACK_DAMAGE));
            double totalBefore = charge(tool) + charge(pack);
            float healthBefore = target.getHealth();
            player.attack(target);
            double spent = totalBefore - charge(tool) - charge(pack);
            float damage = healthBefore - target.getHealth();
            check("native first Vajra hit draws armour power before dealing 25 damage and spends exactly 6666 EU",
                damage == 25 && spent == 6666, "damage=" + damage + " totalEU=" + spent + " tool=" + charge(tool) + " pack=" + charge(pack));

            equipment.invoke(player); // record the first hit's refilled stack as a real equipment tick would
            tool = new ItemStack(GraviItems.VAJRA.get());
            pack = GraviItems.ULTIMATE_LAPPACK.get().charged();
            player.setItemInHand(InteractionHand.MAIN_HAND, tool);
            player.setItemSlot(EquipmentSlot.CHEST, pack);
            equipment.invoke(player);
            cooldown.setInt(player, 20);
            target.setHealth(100);
            target.invulnerableTime = 0;
            java.util.function.Consumer<net.neoforged.neoforge.event.entity.player.AttackEntityEvent> cancel = event -> {
                if (event.getEntity() == player && event.getTarget() == target) event.setCanceled(true);
            };
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
            try { player.attack(target); }
            finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel); }
            check("native canceled Vajra attack does not refill, damage or consume EU", target.getHealth() == 100 && charge(tool) == 0 && charge(pack) == 6e7
                && player.getAttributeValue(Attributes.ATTACK_DAMAGE) == 1, "tool=" + charge(tool) + " pack=" + charge(pack));

            target.setInvulnerable(true);
            cooldown.setInt(player, 20);
            totalBefore = charge(tool) + charge(pack);
            player.attack(target);
            check("native unsuccessful Vajra hit does not consume EU", target.getHealth() == 100 && charge(tool) + charge(pack) == totalBefore,
                "tool=" + charge(tool) + " pack=" + charge(pack));
            target.setInvulnerable(false);
            equipment.invoke(player);

            tool = charged(GraviItems.VAJRA.get(), 3333);
            player.setItemInHand(InteractionHand.MAIN_HAND, tool);
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            equipment.invoke(player);
            cooldown.setInt(player, 20);
            target.setHealth(100);
            target.invulnerableTime = 0;
            player.attack(target);
            check("native undercharged Vajra uses fist damage without consuming a partial payment", target.getHealth() == 99 && charge(tool) == 3333,
                "damage=" + (100 - target.getHealth()) + " EU=" + charge(tool));

            tool = charged(GraviItems.VAJRA.get(), 3666);
            pack = charged(GraviItems.ULTIMATE_LAPPACK.get(), 3000);
            player.setItemInHand(InteractionHand.MAIN_HAND, tool);
            player.setItemSlot(EquipmentSlot.CHEST, pack);
            equipment.invoke(player);
            cooldown.setInt(player, 20);
            target.setHealth(100);
            target.invulnerableTime = 0;
            player.attack(target);
            check("native Vajra combines its final charge with partial armour power exactly once", target.getHealth() == 75 && charge(tool) == 0 && charge(pack) == 0
                && player.getAttributeValue(Attributes.ATTACK_DAMAGE) == 1, "damage=" + (100 - target.getHealth()) + " tool=" + charge(tool) + " pack=" + charge(pack));

            // No equipment reconciliation between these attacks: the previous powered modifier must already have expired.
            cooldown.setInt(player, 20);
            target.setHealth(100);
            target.invulnerableTime = 0;
            player.attack(target);
            check("native second Vajra attack cannot reuse spent power before the next equipment tick", target.getHealth() == 99 && charge(tool) == 0 && charge(pack) == 0,
                "damage=" + (100 - target.getHealth()));

            tool = charged(GraviItems.VAJRA.get(), 10000);
            player.setItemInHand(InteractionHand.MAIN_HAND, tool);
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            equipment.invoke(player);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            equipment.invoke(player);
            cooldown.setInt(player, 20);
            target.setHealth(100);
            target.invulnerableTime = 0;
            player.attack(target);
            check("native Vajra unequip leaves no attack modifier on the empty hand", player.getAttributeValue(Attributes.ATTACK_DAMAGE) == 1 && target.getHealth() == 99,
                "damage=" + (100 - target.getHealth()));

            // Same NeoForge PartEntity path as a dragon head: native Player.attack unwraps the living parent for hurtEnemy.
            var part = new net.neoforged.neoforge.entity.PartEntity<net.minecraft.world.entity.animal.Cow>(target) {
                @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {}
                @Override protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {}
                @Override protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {}
                @Override public boolean isPickable() { return true; }
                @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) { return getParent().hurt(source, amount); }
            };
            part.moveTo(target.position());
            tool = new ItemStack(GraviItems.VAJRA.get());
            pack = GraviItems.ULTIMATE_LAPPACK.get().charged();
            player.setItemInHand(InteractionHand.MAIN_HAND, tool);
            player.setItemSlot(EquipmentSlot.CHEST, pack);
            equipment.invoke(player);
            cooldown.setInt(player, 20);
            target.setHealth(100);
            target.invulnerableTime = 0;
            totalBefore = charge(tool) + charge(pack);
            player.attack(part);
            spent = totalBefore - charge(tool) - charge(pack);
            check("native multipart Vajra first hit refills before parent damage and pays once", target.getHealth() == 75 && spent == 6666,
                "parentDamage=" + (100 - target.getHealth()) + " totalEU=" + spent);

            equipment.invoke(player);
            tool = new ItemStack(GraviItems.VAJRA.get());
            pack = GraviItems.ULTIMATE_LAPPACK.get().charged();
            player.setItemInHand(InteractionHand.MAIN_HAND, tool);
            player.setItemSlot(EquipmentSlot.CHEST, pack);
            equipment.invoke(player);
            cooldown.setInt(player, 20);
            target.setHealth(100);
            target.invulnerableTime = 0;
            java.util.function.Consumer<net.neoforged.neoforge.event.entity.player.AttackEntityEvent> cancelPart = event -> {
                if (event.getEntity() == player && event.getTarget() == part) event.setCanceled(true);
            };
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancelPart);
            try { player.attack(part); }
            finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancelPart); }
            check("native multipart attack cancellation leaves parent and both EU stores unchanged", target.getHealth() == 100 && charge(tool) == 0 && charge(pack) == 6e7,
                "tool=" + charge(tool) + " pack=" + charge(pack));

            tool = charged(GraviItems.VAJRA.get(), 6666);
            player.setItemInHand(InteractionHand.MAIN_HAND, tool);
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            equipment.invoke(player);
            cooldown.setInt(player, 20);
            player.attack(part);
            check("native multipart final operation deals powered parent damage and clears its modifier", target.getHealth() == 75 && charge(tool) == 0
                && player.getAttributeValue(Attributes.ATTACK_DAMAGE) == 1, "parentDamage=" + (100 - target.getHealth()) + " EU=" + charge(tool));
        } catch (ReflectiveOperationException failure) {
            throw new RuntimeException("Vajra native attack fixture", failure);
        } finally {
            target.discard();
            player.getInventory().clearContent();
            for (var slot : EquipmentSlot.values()) player.setItemSlot(slot, ItemStack.EMPTY);
            try {
                var equipment = LivingEntity.class.getDeclaredMethod("detectEquipmentUpdates");
                equipment.setAccessible(true);
                equipment.invoke(player);
            } catch (ReflectiveOperationException failure) { throw new RuntimeException(failure); }
        }
    }

    private static void check(String name, boolean ok, Object detail) { ServerAudit.check(name, ok, detail); }
}
