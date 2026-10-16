package com.chocohead.gravisuite.client;

import com.chocohead.gravisuite.GraviItems;
import com.chocohead.gravisuite.Gravisuite;
import com.chocohead.gravisuite.items.ItemAdvancedElectricJetpack;
import com.chocohead.gravisuite.jetpack.IJetpack;
import ic2.api.item.ElectricItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.ModConfigSpec;

/** GravisuiteOverlay: energy of the worn Gravi chest piece and the jetpack engine/hover state in a screen corner. */
@EventBusSubscriber(modid = Gravisuite.MODID, value = Dist.CLIENT)
public final class GravisuiteOverlay {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue POSITION;
    static {
        var b = new ModConfigSpec.Builder().push("hud");
        ENABLED = b.define("enableHud", true);
        POSITION = b.comment("1 top left, 2 top right, 3 bottom left, 4 bottom right").defineInRange("hudPosition", 1, 1, 4);
        SPEC = b.pop().build();
    }
    private GravisuiteOverlay() {}

    @SubscribeEvent
    static void layers(RegisterGuiLayersEvent event) { event.registerAbove(VanillaGuiLayers.CHAT, Gravisuite.id("hud"), GravisuiteOverlay::render); }

    public static Component energyStatus(float percent) {
        MutableComponent c = Component.literal(Math.round(percent) + "%");
        return percent <= 5 ? c.withStyle(ChatFormatting.RED) : percent <= 10 ? c.withStyle(ChatFormatting.GOLD) : c;
    }

    /** The two lines for the worn chest piece, or null when the HUD has nothing to show. */
    public static Component[] lines(ItemStack stack) {
        if (stack.isEmpty()) return null;
        float charge;
        Component status = null;
        if (stack.is(GraviItems.ADVANCED_LAPPACK.get()) || stack.is(GraviItems.ULTIMATE_LAPPACK.get())) charge = (float)(ElectricItem.manager.getCharge(stack) / ((ic2.api.item.IElectricItem)stack.getItem()).getMaxCharge(stack) * 100);
        else if (stack.getItem() instanceof ItemAdvancedElectricJetpack jetpack) {
            charge = (float)(((IJetpack)jetpack).getChargeLevel(stack) * 100);
            if (ItemAdvancedElectricJetpack.isJetpackOn(stack)) {
                Component hover = ItemAdvancedElectricJetpack.isHovering(stack) ? Component.translatable("gravisuite.message.hover") : Component.empty();
                status = Component.translatable("gravisuite.message.jetpackEngine", hover.copy().withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN);
            }
        } else return null;
        return new Component[]{status, Component.translatable("gravisuite.message.energy", energyStatus(charge))};
    }

    static void render(GuiGraphics g, DeltaTracker delta) {
        var mc = Minecraft.getInstance();
        if (!ENABLED.get() || mc.player == null || mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) return;
        Component[] l = lines(mc.player.getItemBySlot(EquipmentSlot.CHEST));
        if (l == null) return;
        var font = mc.font;
        int h = font.lineHeight, w = g.guiWidth(), hh = g.guiHeight();
        Component status = l[0], energy = l[1];
        int pos = POSITION.get();
        int x1, y1, x2, y2;
        boolean right = pos == 2 || pos == 4, bottom = pos >= 3;
        x1 = right ? w - (status == null ? 0 : font.width(status)) - 2 : 2;
        x2 = right ? w - font.width(energy) - 2 : 2;
        if (!bottom) { y1 = 2; y2 = 5 + h; } else { y1 = hh - 2 - h; y2 = y1 - 3 - h; }
        if (status != null) { g.drawString(font, status, x1, y1, 0xFFFFFF); g.drawString(font, energy, x2, y2, 0xFFFFFF); }
        else g.drawString(font, energy, x2, y1, 0xFFFFFF);
    }
}
