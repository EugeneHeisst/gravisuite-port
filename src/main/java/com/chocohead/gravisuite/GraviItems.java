package com.chocohead.gravisuite;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.*;
import ic2.core.item.armor.ItemArmorBatpack;
import java.util.*;
public final class GraviItems {
 public static final DeferredRegister<ArmorMaterial> MATERIALS=DeferredRegister.create(Registries.ARMOR_MATERIAL,Gravisuite.MODID);
 public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(Gravisuite.MODID);
 public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,Gravisuite.MODID);
 public static final Map<String,DeferredItem<Item>> COMPONENTS=new LinkedHashMap<>();
 static{for(String id:List.of("superconductor_cover","superconductor","cooling_core","gravitation_engine","magnetron","vajra_core","engine_booster"))COMPONENTS.put(id,ITEMS.register(id,()->new Item(new Item.Properties())));}
 private static Holder<ArmorMaterial> material(String id){return MATERIALS.register(id,()->{var defense=new EnumMap<ArmorItem.Type,Integer>(ArmorItem.Type.class);for(var type:ArmorItem.Type.values())defense.put(type,0);return new ArmorMaterial(defense,0,SoundEvents.ARMOR_EQUIP_IRON,()->Ingredient.EMPTY,List.of(new ArmorMaterial.Layer(Gravisuite.id(id))),0,0);});}
 public static final Holder<ArmorMaterial> ADVANCED_MATERIAL=material("advanced_lappack"),ULTIMATE_MATERIAL=material("ultimate_lappack");
 public static final DeferredItem<ItemArmorBatpack> ADVANCED_LAPPACK=ITEMS.register("advanced_lappack",()->new ItemArmorBatpack(ADVANCED_MATERIAL,new Item.Properties().rarity(Rarity.UNCOMMON),3e6,30000,3));
 public static final DeferredItem<ItemArmorBatpack> ULTIMATE_LAPPACK=ITEMS.register("ultimate_lappack",()->new ItemArmorBatpack(ULTIMATE_MATERIAL,new Item.Properties().rarity(Rarity.RARE),6e7,100000,4));
 public static final java.util.function.Supplier<CreativeModeTab> TAB=TABS.register("main",()->CreativeModeTab.builder().title(Component.literal("Gravitation Suite PORT")).icon(()->new ItemStack(ULTIMATE_LAPPACK.get())).displayItems((parameters,out)->{for(var entry:ITEMS.getEntries()){var item=entry.get();out.accept(item);if(item instanceof ic2.api.item.IElectricItem){var stack=new ItemStack(item);ic2.api.item.ElectricItem.manager.charge(stack,Double.POSITIVE_INFINITY,Integer.MAX_VALUE,true,false);out.accept(stack);}}}).build());
}
