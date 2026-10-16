package com.chocohead.gravisuite;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.resources.ResourceLocation;
@Mod(Gravisuite.MODID)
public final class Gravisuite {
 public static final String MODID="gravisuite";
 public static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("GravitationSuite");
 public static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(MODID,path);}
 public Gravisuite(IEventBus bus,ModContainer container){container.registerConfig(ModConfig.Type.COMMON,Config.SPEC);GraviItems.MATERIALS.register(bus);GraviItems.ITEMS.register(bus);GraviItems.TABS.register(bus);GraviSounds.REGISTRY.register(bus);ConfigCondition.REGISTRY.register(bus);}
}
