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
 private static double flightStartY;
 private static void shot(Minecraft mc,String name){try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("audit",name+".png"));}catch(Exception e){throw new RuntimeException(e);}}
 @SubscribeEvent static void tick(ClientTickEvent.Post event){if(!"client".equals(System.getProperty("gravisuite.audit")))return;var mc=Minecraft.getInstance();if(mc.player==null||mc.getSingleplayerServer()==null)return;tick++;var server=mc.getSingleplayerServer();
  if(tick==20)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.getInventory().clearContent();for(var slot:EquipmentSlot.values())p.setItemSlot(slot,ItemStack.EMPTY);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(server.overworld(),.5,120,.5,0,0);int index=0;for(var entry:GraviItems.ITEMS.getEntries()){var s=new ItemStack(entry.get());if(s.getItem() instanceof ic2.api.item.IElectricItem)ic2.api.item.ElectricItem.manager.charge(s,2500000,4,true,false);p.getInventory().setItem(index++,s);}});
  if(tick==40){int count=0;for(var entry:GraviItems.ITEMS.getEntries()){var s=new ItemStack(entry.get());var id=BuiltInRegistries.ITEM.getKey(entry.get());var model=mc.getItemRenderer().getModel(s,mc.level,mc.player,0);if(model.getParticleIcon(net.neoforged.neoforge.client.model.data.ModelData.EMPTY).contents().name().getPath().equals("missingno"))throw new IllegalStateException("Missing item model "+id);if(Component.translatable("item.gravisuite."+id.getPath()).getString().startsWith("item.gravisuite."))throw new IllegalStateException("Missing translation "+id);count++;}Gravisuite.LOG.info("[gravisuite-client-audit] PASS all{}item models and translations",count);mc.setScreen(new InventoryScreen(mc.player));}
  if(tick==60)shot(mc,"components_packs_jei");
  if(tick==65)server.execute(()->server.getPlayerList().getPlayers().getFirst().setItemSlot(EquipmentSlot.CHEST,GraviItems.ADVANCED_LAPPACK.get().charged()));
  if(tick==85)shot(mc,"advanced_lappack_worn");
  if(tick==90)server.execute(()->server.getPlayerList().getPlayers().getFirst().setItemSlot(EquipmentSlot.CHEST,GraviItems.ULTIMATE_LAPPACK.get().charged()));
  if(tick==110){shot(mc,"ultimate_lappack_worn");Gravisuite.LOG.info("[gravisuite-client-audit] DONE");}
  if(tick==112)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);p.getInventory().clearContent();p.getInventory().selected=0;
   p.getInventory().setItem(0,GraviItems.ADVANCED_DRILL.get().getItemStack(45000));p.getInventory().setItem(1,GraviItems.ADVANCED_CHAINSAW.get().getItemStack(45000));p.getInventory().setItem(2,GraviItems.VAJRA.get().getItemStack(1e7));
   for(int i=0;i<4;i++){var t=new ItemStack(GraviItems.GRAVITOOL.get());com.chocohead.gravisuite.items.ItemGraviTool.saveMode(t,com.chocohead.gravisuite.items.ItemGraviTool.Mode.values()[i]);p.getInventory().setItem(3+i,t);}
   p.getInventory().setItem(7,new ItemStack(GraviItems.GRAVITOOL.get()));});
  if(tick==118){for(int i=3;i<7;i++){var s=mc.player.getInventory().getItem(i);var sprite=mc.getItemRenderer().getModel(s,mc.level,mc.player,0).getParticleIcon(net.neoforged.neoforge.client.model.data.ModelData.EMPTY).contents().name().getPath();String want="items/gravitool/"+com.chocohead.gravisuite.items.ItemGraviTool.Mode.values()[i-3].name().toLowerCase(java.util.Locale.ROOT);Gravisuite.LOG.info("[gravisuite-client-audit] {} GraviTool mode model {} -> {}",sprite.equals(want)?"PASS":"FAIL",want,sprite);}}
  if(tick==120)mc.setScreen(new InventoryScreen(mc.player));
  if(tick==130){shot(mc,"tools");mc.setScreen(null);}
  // worn chest pieces (survival so the inventory screen shows the player)
  String[] worn={"advanced_jetpack","advanced_nano_chestplate","gravi_chestplate","gravi_chestplate_red"};
  if(tick>=140&&tick<180){int w=(tick-140)/10;int k=(tick-140)%10;
   if(k==0)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();var item=w==0?GraviItems.ADVANCED_JETPACK.get():w==1?GraviItems.ADVANCED_NANO_CHESTPLATE.get():GraviItems.GRAVI_CHESTPLATE.get();var s=new ItemStack(item);if(w==3)s.set(DataComponents.DYED_COLOR,new net.minecraft.world.item.component.DyedItemColor(0xB02E26,true));p.setItemSlot(EquipmentSlot.CHEST,s);});
   if(k==2)mc.setScreen(new InventoryScreen(mc.player));
   if(k==8){shot(mc,worn[w]+"_worn");mc.setScreen(null);}}
  // real flight in singleplayer: Gravi Fly Key turns the engine on, jump climbs
  if(tick==182)server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.getAbilities().flying=false;p.getAbilities().mayfly=false;p.onUpdateAbilities();p.setItemSlot(EquipmentSlot.CHEST,GraviItems.GRAVI_CHESTPLATE.get().charged());p.teleportTo(server.overworld(),.5,120,.5,0,0);});
  if(tick>=186&&tick<190)com.chocohead.gravisuite.client.GraviClient.FLY_KEY.setDown(true);
  if(tick==190)com.chocohead.gravisuite.client.GraviClient.FLY_KEY.setDown(false);
  if(tick==192)flightStartY=mc.player.getY();
  if(tick>=192&&tick<232)mc.options.keyJump.setDown(true);
  if(tick==228)shot(mc,"gravi_flight");
  if(tick==232){mc.options.keyJump.setDown(false);var chest=mc.player.getItemBySlot(EquipmentSlot.CHEST);double climbed=mc.player.getY()-flightStartY;Gravisuite.LOG.info("[gravisuite-client-audit] {} GraviChestPlate flight: engine {} climbed {} blocks",com.chocohead.gravisuite.items.ItemAdvancedElectricJetpack.isJetpackOn(chest)&&climbed>5?"PASS":"FAIL",com.chocohead.gravisuite.items.ItemAdvancedElectricJetpack.isJetpackOn(chest),climbed);}
  if(tick>=236)mc.stop();
 }
}
