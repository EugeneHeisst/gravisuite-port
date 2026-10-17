package com.chocohead.gravisuite.audit;

import com.chocohead.gravisuite.GraviItems;
import com.chocohead.gravisuite.GraviKeys;
import com.chocohead.gravisuite.items.ItemAdvancedElectricJetpack;
import com.chocohead.gravisuite.jetpack.IJetpack;
import com.chocohead.gravisuite.jetpack.JetpackLogic;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.core.item.armor.ItemArmorElectric;
import ic2.core.ref.Ic2Items;
import ic2.core.util.Keyboard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Advanced jetpack, advanced nano chestplate and GraviChestPlate: IC2 2.8 jetpack logic with the original numbers. */
public final class FlightAudit {
    private FlightAudit() {}

    private static void check(String name, boolean ok, Object detail) { ServerAudit.check(name, ok, detail); }
    private static double charge(ItemStack s) { return ElectricItem.manager.getCharge(s); }
    private static void keys(FakePlayer pl, Keyboard.Key... down) { int state = 0; for (var k : down) state |= 1 << k.ordinal(); Keyboard.processKeyUpdate(pl, state); }
    private static void wear(FakePlayer pl, ItemStack s) { pl.setItemSlot(EquipmentSlot.CHEST, s); }
    private static ItemStack charged(Item item, double eu) { ItemStack s = new ItemStack(item); ElectricItem.manager.charge(s, eu, Integer.MAX_VALUE, true, false); return s; }
    private static void flyTick(FakePlayer pl) {
        // Actual subscriber order: JetpackLogic on Pre, IC2 ArmorEvents/item.onArmorTick on Post.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre(pl));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(pl));
    }
    private static void flyPayload(FakePlayer pl, boolean down) {
        var buf = io.netty.buffer.Unpooled.buffer();
        try {
            GraviKeys.FlyKeyPayload.CODEC.encode(buf, new GraviKeys.FlyKeyPayload(down));
            var payload = GraviKeys.FlyKeyPayload.CODEC.decode(buf);
            var context = (net.neoforged.neoforge.network.handling.IPayloadContext)java.lang.reflect.Proxy.newProxyInstance(FlightAudit.class.getClassLoader(), new Class<?>[]{net.neoforged.neoforge.network.handling.IPayloadContext.class}, (proxy, method, args) -> {
                if (method.getName().equals("player")) return pl;
                throw new UnsupportedOperationException(method.getName());
            });
            GraviKeys.FlyKeyPayload.handle(payload, context);
        } finally { buf.release(); }
    }
    private static int cooldown(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getByte("toggleTimer"); }
    private static void air(FakePlayer pl, double y) { pl.moveTo(-8.5, y, 40.5, 0, 0); pl.setOnGround(false); pl.setDeltaMovement(Vec3.ZERO); }

    public static void run(ServerLevel l, FakePlayer pl) {
        for (var p : BlockPos.betweenClosed(new BlockPos(-12, 99, 36), new BlockPos(-4, 140, 44))) l.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        pl.getInventory().clearContent();
        for (var slot : EquipmentSlot.values()) pl.setItemSlot(slot, ItemStack.EMPTY);
        record Stats(Item item, double max, int tier, double transfer, double ratio, int epd) {}
        for (var st : new Stats[]{new Stats(GraviItems.ADVANCED_JETPACK.get(), 3e6, 3, 30000, 0, 0), new Stats(GraviItems.ADVANCED_NANO_CHESTPLATE.get(), 3e6, 3, 30000, 0.9, 800), new Stats(GraviItems.GRAVI_CHESTPLATE.get(), 6e7, 4, 100000, 1.1, 20000)}) {
            ItemStack s = new ItemStack(st.item);
            var api = (IElectricItem)st.item;
            var armor = (ItemArmorElectric)st.item;
            check(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(st.item).getPath() + " original charge/tier/transfer/protection, provides energy",
                api.getMaxCharge(s) == st.max && api.getTier(s) == st.tier && api.getTransferLimit(s) == st.transfer && api.canProvideEnergy(s) && armor.getDamageAbsorptionRatio() == st.ratio && armor.getEnergyPerDamage() == st.epd, st);
        }

        // fly key switches the engine
        ItemStack jet = charged(GraviItems.ADVANCED_JETPACK.get(), 3e6);
        wear(pl, jet);
        air(pl, 110);
        keys(pl);
        GraviKeys.setFlyKey(pl, true);
        flyTick(pl);
        boolean on = ItemAdvancedElectricJetpack.isJetpackOn(pl.getItemBySlot(EquipmentSlot.CHEST));
        flyTick(pl);
        boolean stillOn = ItemAdvancedElectricJetpack.isJetpackOn(pl.getItemBySlot(EquipmentSlot.CHEST));
        GraviKeys.setFlyKey(pl, false);
        for (int i = 0; i < 12; i++) flyTick(pl);
        GraviKeys.setFlyKey(pl, true);
        flyTick(pl);
        boolean off = !ItemAdvancedElectricJetpack.isJetpackOn(pl.getItemBySlot(EquipmentSlot.CHEST));
        GraviKeys.setFlyKey(pl, false);
        check("Gravi Fly Key switches the jetpack engine with a 10 tick debounce", on && stillOn && off, on + "/" + stillOn + "/" + off);
        ItemAdvancedElectricJetpack.switchJetpack(pl.getItemBySlot(EquipmentSlot.CHEST));
        jet = pl.getItemBySlot(EquipmentSlot.CHEST);

        // thrust and energy
        air(pl, 110);
        double before = charge(jet);
        keys(pl, Keyboard.Key.jump);
        flyTick(pl);
        check("advanced jetpack lifts 0.2 per tick for 12 EU", Math.abs(pl.getDeltaMovement().y - 0.2) < 1e-6 && before - charge(jet) == 12 && pl.fallDistance == 0, pl.getDeltaMovement().y + " eu=" + (before - charge(jet)));
        air(pl, 110);
        before = charge(jet);
        keys(pl, Keyboard.Key.jump, Keyboard.Key.forward, Keyboard.Key.boost);
        flyTick(pl);
        Vec3 v = pl.getDeltaMovement();
        check("boost pushes forward (0.11 x 0.33 = 0.0363) for 60 EU more", Math.abs(v.horizontalDistance() - 0.0363) < 1e-4 && before - charge(jet) == 72, v + " eu=" + (before - charge(jet)));
        keys(pl);
        air(pl, 110);
        CustomData.update(DataComponents.CUSTOM_DATA, jet, t -> t.putBoolean("hoverMode", true));
        pl.setDeltaMovement(0, -0.5, 0);
        before = charge(jet);
        flyTick(pl);
        check("hover mode catches a fall for 6 EU a tick", pl.getDeltaMovement().y >= -0.5 && pl.getDeltaMovement().y <= 0 && before - charge(jet) == 6, pl.getDeltaMovement().y + " eu=" + (before - charge(jet)));
        air(pl, 110);
        keys(pl, Keyboard.Key.jump);
        flyTick(pl);
        check("hover climbs at most 0.2 a tick", Math.abs(pl.getDeltaMovement().y - 0.2) < 1e-6, pl.getDeltaMovement().y);
        keys(pl);
        pl.setOnGround(true);
        flyTick(pl);
        check("landing ends hover mode", !ItemAdvancedElectricJetpack.isHovering(pl.getItemBySlot(EquipmentSlot.CHEST)), "landed");
        ItemStack low = charged(GraviItems.ADVANCED_JETPACK.get(), 3e6 * 0.025);
        ItemAdvancedElectricJetpack.switchJetpack(low);
        wear(pl, low);
        air(pl, 110);
        keys(pl, Keyboard.Key.jump);
        flyTick(pl);
        check("below 5% the thrust fades with the charge", Math.abs(pl.getDeltaMovement().y - 0.1) < 1e-4, pl.getDeltaMovement().y);
        keys(pl);

        // GraviChestPlate
        ItemStack undercharged = charged(GraviItems.GRAVI_CHESTPLATE.get(), 9999);
        wear(pl, undercharged);
        air(pl, 110);
        keys(pl);
        flyPayload(pl, true);
        flyTick(pl);
        check("native F payload enables an undercharged Gravi engine without allowing flight", ItemAdvancedElectricJetpack.isJetpackOn(undercharged) && !((IJetpack)undercharged.getItem()).isJetpackActive(undercharged) && cooldown(undercharged) == 10 && charge(undercharged) == 9999, cooldown(undercharged));
        flyPayload(pl, false);
        flyTick(pl);
        check("inactive Gravi engine cooldown decreases exactly once per native player tick", cooldown(undercharged) == 9, cooldown(undercharged));
        flyPayload(pl, true);
        flyTick(pl);
        check("undercharged Gravi engine rejects F during the debounce", ItemAdvancedElectricJetpack.isJetpackOn(undercharged) && cooldown(undercharged) == 8, cooldown(undercharged));
        flyPayload(pl, false);
        for (int i = 0; i < 8; i++) flyTick(pl);
        check("undercharged Gravi engine cooldown expires after ten native ticks", cooldown(undercharged) == 0 && ItemAdvancedElectricJetpack.isJetpackOn(undercharged) && charge(undercharged) == 9999, cooldown(undercharged));
        flyPayload(pl, true);
        flyTick(pl);
        check("native F payload can turn the Gravi engine off without charging it", !ItemAdvancedElectricJetpack.isJetpackOn(undercharged) && cooldown(undercharged) == 10 && charge(undercharged) == 9999, charge(undercharged));
        flyPayload(pl, false);
        flyTick(pl);
        check("disabled Gravi engine cooldown also decreases exactly once per native tick", cooldown(undercharged) == 9, cooldown(undercharged));
        ItemStack gravi = charged(GraviItems.GRAVI_CHESTPLATE.get(), 9999);
        ItemAdvancedElectricJetpack.switchJetpack(gravi);
        check("GraviChestPlate needs 10000 EU to fly", !((IJetpack)gravi.getItem()).isJetpackActive(gravi), charge(gravi));
        ElectricItem.manager.charge(gravi, 6e7, Integer.MAX_VALUE, true, false);
        wear(pl, gravi);
        air(pl, 110);
        before = charge(gravi);
        keys(pl, Keyboard.Key.jump);
        flyTick(pl);
        check("GraviChestPlate lifts 0.3 a tick for 278 EU", Math.abs(pl.getDeltaMovement().y - 0.3) < 1e-6 && before - charge(gravi) == 278, pl.getDeltaMovement().y + " eu=" + (before - charge(gravi)));
        air(pl, 110);
        before = charge(gravi);
        keys(pl, Keyboard.Key.jump, Keyboard.Key.forward, Keyboard.Key.boost);
        flyTick(pl);
        check("GraviChestPlate boost costs 834 EU", before - charge(gravi) == 278 + 834, before - charge(gravi));
        keys(pl, Keyboard.Key.jump);
        int ceiling = (int)(l.getMaxBuildHeight() / 0.91071427f);
        air(pl, ceiling + 1);
        flyTick(pl);
        check("no lift above the flight ceiling (world height / 0.9107)", pl.getDeltaMovement().y <= 0, pl.getDeltaMovement().y);
        keys(pl);
        pl.setRemainingFireTicks(100);
        flyTick(pl);
        check("GraviChestPlate puts the wearer out", !pl.isOnFire(), pl.getRemainingFireTicks());

        // advanced nano chestplate puts out fire with a water cell
        ItemStack nano = charged(GraviItems.ADVANCED_NANO_CHESTPLATE.get(), 3e6);
        wear(pl, nano);
        pl.getInventory().setItem(3, new ItemStack(Ic2Items.WATER_CELL.get(), 2));
        pl.setRemainingFireTicks(100);
        pl.tickCount = 40;
        before = charge(nano);
        ((ItemArmorElectric)nano.getItem()).onArmorTick(l, pl, nano);
        check("nano chestplate pours a water cell over a burning wearer for 50000 EU", !pl.isOnFire() && pl.getInventory().countItem(Ic2Items.WATER_CELL.get()) == 1 && pl.getInventory().countItem(Ic2Items.EMPTY_CELL.get()) == 1 && before - charge(nano) == 50000, before - charge(nano));
        pl.getInventory().clearContent();
        pl.setRemainingFireTicks(100);
        ((ItemArmorElectric)nano.getItem()).onArmorTick(l, pl, nano);
        check("without a water cell it keeps burning", pl.isOnFire(), pl.getRemainingFireTicks());
        pl.clearFire();

        // dyeing and recipes
        var input = CraftingInput.of(2, 1, NonNullList.of(ItemStack.EMPTY, charged(GraviItems.GRAVI_CHESTPLATE.get(), 1000), new ItemStack(Items.RED_DYE)));
        var dyed = l.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, l).map(r -> r.value().assemble(input, l.registryAccess())).orElse(ItemStack.EMPTY);
        check("GraviChestPlate can be dyed and keeps its charge", dyed.is(GraviItems.GRAVI_CHESTPLATE.get()) && dyed.has(DataComponents.DYED_COLOR) && charge(dyed) == 1000, dyed.get(DataComponents.DYED_COLOR));
        var quantum = l.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("ic2:quantum_chestplate")).orElse(null);
        boolean replaced = quantum != null && quantum.value().getResultItem(l.registryAccess()).is(Ic2Items.get("quantum_chestplate"))
            && quantum.value().getIngredients().stream().anyMatch(i -> i.test(new ItemStack(GraviItems.ADVANCED_NANO_CHESTPLATE.get())))
            && quantum.value().getIngredients().stream().noneMatch(i -> i.test(new ItemStack(Ic2Items.get("electric_jetpack"))));
        check("IC2 quantum chestplate recipe now starts from the advanced nano chestplate (replaceQuantumRecipe)", replaced == com.chocohead.gravisuite.Config.REPLACE_QUANTUM.get(), quantum == null ? "missing" : quantum.value().getIngredients().size());
        for (var slot : EquipmentSlot.values()) pl.setItemSlot(slot, ItemStack.EMPTY);
        keys(pl);
    }
}
