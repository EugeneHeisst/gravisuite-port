package com.chocohead.gravisuite.client;

import com.chocohead.gravisuite.GraviItems;
import com.chocohead.gravisuite.Gravisuite;
import com.chocohead.gravisuite.items.ItemGraviTool;
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = Gravisuite.MODID, value = Dist.CLIENT)
public final class GraviClient {
    public static final net.minecraft.client.KeyMapping FLY_KEY = new net.minecraft.client.KeyMapping("Gravi Fly Key", org.lwjgl.glfw.GLFW.GLFW_KEY_F, "Gravisuite");
    private GraviClient() {}

    /** Undyed, the GraviChestPlate is white like the original (colour -1), not leather brown. */
    @SubscribeEvent
    static void extensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            @Override public int getDefaultDyeColor(net.minecraft.world.item.ItemStack stack) {
                return net.minecraft.util.FastColor.ARGB32.opaque(net.minecraft.world.item.component.DyedItemColor.getOrDefault(stack, 0xFFFFFF));
            }
        }, GraviItems.GRAVI_CHESTPLATE.get());
    }

    @SubscribeEvent static void keys(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) { event.register(FLY_KEY); }

    /** The GraviChestPlate item takes its dye colour like the original colour handler. */
    @SubscribeEvent
    static void colours(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> tint > 0 ? -1 : net.minecraft.util.FastColor.ARGB32.opaque(stack.getOrDefault(net.minecraft.core.component.DataComponents.DYED_COLOR, new net.minecraft.world.item.component.DyedItemColor(0xFFFFFF, false)).rgb()), GraviItems.GRAVI_CHESTPLATE.get());
    }

    /** The GraviTool model follows its mode (hoe when no mode was chosen yet). */
    @SubscribeEvent
    static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(GraviItems.GRAVITOOL.get(), Gravisuite.id("mode"), (stack, level, entity, seed) -> ItemGraviTool.hasMode(stack) ? ItemGraviTool.readMode(stack).ordinal() / 4f : 0));
    }
}
