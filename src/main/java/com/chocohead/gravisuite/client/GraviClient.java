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
    private GraviClient() {}

    /** The GraviTool model follows its mode (hoe when no mode was chosen yet). */
    @SubscribeEvent
    static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(GraviItems.GRAVITOOL.get(), Gravisuite.id("mode"), (stack, level, entity, seed) -> ItemGraviTool.hasMode(stack) ? ItemGraviTool.readMode(stack).ordinal() / 4f : 0));
    }
}
