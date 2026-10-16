package com.chocohead.gravisuite.audit;
import com.chocohead.gravisuite.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.nio.file.*;
@EventBusSubscriber(modid=Gravisuite.MODID,value=Dist.CLIENT)
public final class ClientAudit {
 private static int tick;
 private static void shot(Minecraft mc,String name){try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("audit",name+".png"));}catch(Exception e){throw new RuntimeException(e);}}
 @SubscribeEvent static void tick(ClientTickEvent.Post event){if(!"client".equals(System.getProperty("gravisuite.audit")))return;var mc=Minecraft.getInstance();if(mc.player==null||mc.getSingleplayerServer()==null)return;tick++;var server=mc.getSingleplayerServer();
  if(tick==20)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.getInventory().clearContent();for(var slot:EquipmentSlot.values())p.setItemSlot(slot,ItemStack.EMPTY);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(server.overworld(),.5,120,.5,0,0);int index=0;for(var entry:GraviItems.ITEMS.getEntries()){var s=new ItemStack(entry.get());if(s.getItem() instanceof ic2.api.item.IElectricItem)ic2.api.item.ElectricItem.manager.charge(s,2500000,4,true,false);p.getInventory().setItem(index++,s);}});
  if(tick==40){int count=0;for(var entry:GraviItems.ITEMS.getEntries()){var s=new ItemStack(entry.get());var id=BuiltInRegistries.ITEM.getKey(entry.get());var model=mc.getItemRenderer().getModel(s,mc.level,mc.player,0);if(model.getParticleIcon(net.neoforged.neoforge.client.model.data.ModelData.EMPTY).contents().name().getPath().equals("missingno"))throw new IllegalStateException("Missing item model "+id);if(Component.translatable("item.gravisuite."+id.getPath()).getString().startsWith("item.gravisuite."))throw new IllegalStateException("Missing translation "+id);count++;}Gravisuite.LOG.info("[gravisuite-client-audit] PASS all{}item models and translations",count);mc.setScreen(new InventoryScreen(mc.player));}
  if(tick==60)shot(mc,"components_packs_jei");
  if(tick==65)server.execute(()->server.getPlayerList().getPlayers().getFirst().setItemSlot(EquipmentSlot.CHEST,GraviItems.ADVANCED_LAPPACK.get().charged()));
  if(tick==85)shot(mc,"advanced_lappack_worn");
  if(tick==90)server.execute(()->server.getPlayerList().getPlayers().getFirst().setItemSlot(EquipmentSlot.CHEST,GraviItems.ULTIMATE_LAPPACK.get().charged()));
  if(tick==110){shot(mc,"ultimate_lappack_worn");Gravisuite.LOG.info("[gravisuite-client-audit] DONE");}
  if(tick>=115)mc.stop();
 }
}
