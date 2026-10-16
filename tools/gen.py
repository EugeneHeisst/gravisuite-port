#!/usr/bin/env python3
from pathlib import Path
import json,shutil
ROOT=Path(__file__).resolve().parent.parent;REF=ROOT/'related/gravisuite-reference/jar/assets/gravisuite';RES=ROOT/'src/generated/resources';A=RES/'assets/gravisuite';D=RES/'data/gravisuite/recipe'
def dump(path,data):path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
for folder in ['textures','sounds']:
 for p in (REF/folder).rglob('*'):
  if p.is_file():q=A/folder/p.relative_to(REF/folder).as_posix().lower();q.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,q)
components=['superconductor_cover','superconductor','cooling_core','gravitation_engine','magnetron','vajra_core','engine_booster']
items={key:'crafting/'+key.replace('_','') for key in components};items.update(advanced_lappack='advancedlappack',ultimate_lappack='ultimatelappack')
legacy={**{k:'crafting.'+k for k in components},'advanced_lappack':'advancedLappack','ultimate_lappack':'ultimateLappack'}
dump(RES/'assets/minecraft/atlases/blocks.json',{'sources':[{'type':'minecraft:directory','source':'items','prefix':'items/'}]})
for key,texture in items.items():dump(A/'models/item'/f'{key}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'gravisuite:items/'+texture}})
for locale in ['en_US','ru_RU']:
 lang={}
 for line in (REF/'lang'/f'{locale}.lang').read_text().splitlines():
  if '=' in line and not line.startswith('#'):k,v=line.split('=',1);lang[k]=v
 for key,old in legacy.items():lang['item.gravisuite.'+key]=lang.get('gravisuite.'+old,key.replace('_',' ').title())
 dump(A/'lang'/f'{locale.lower()}.json',lang)
for key in ['advanced_lappack','ultimate_lappack']:
 p=REF/'textures/armour'/f'{key.replace("_","")}.png';q=A/'textures/models/armor'/f'{key}_layer_1.png';q.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,q)
def item(i):return {'tag':i[1:]} if i.startswith('#') else {'item':i if ':' in i else 'gravisuite:'+i}
def recipe(name,result,pattern,keys,count=1,flag=None):
 d={'type':'ic2:shaped','category':'equipment','pattern':pattern,'key':{k:item(v) for k,v in keys.items()},'result':{'id':result if ':' in result else 'gravisuite:'+result,'count':count}}
 if flag:d['neoforge:conditions']=[{'type':'gravisuite:config','key':flag}]
 dump(D/f'{name}.json',d)
recipe('superconductor_cover','superconductor_cover',['APA','CCC','APA'],{'A':'ic2:advanced_alloy','P':'ic2:iridium_plate','C':'ic2:carbon_plate'},3)
recipe('superconductor','superconductor',['SSS','CGC','SSS'],{'S':'superconductor_cover','G':'#c:ingots/gold','C':'ic2:glass_fibre_cable'},3)
recipe('cooling_core','cooling_core',['CSC','HPH','CSC'],{'C':'ic2:hex_heat_storage','S':'ic2:advanced_heat_exchanger','H':'ic2:heat_plating','P':'ic2:iridium_plate'})
recipe('gravitation_engine','gravitation_engine',['TST','CHC','TST'],{'T':'ic2:tesla_coil','S':'superconductor','C':'cooling_core','H':'ic2:hv_transformer'})
recipe('engine_booster','engine_booster',['GAG','COC','AVA'],{'G':'minecraft:glowstone_dust','A':'ic2:advanced_alloy','C':'ic2:advanced_circuit','O':'ic2:overclocker_upgrade','V':'ic2:advanced_heat_vent'},flag='advanced_jetpack')
recipe('magnetron','magnetron',['ICI','CSC','ICI'],{'I':'#c:plates/iron','C':'#c:plates/copper','S':'superconductor'},flag='vajra')
recipe('vajra_core','vajra_core',[' M ','PTP','SHS'],{'M':'magnetron','P':'ic2:iridium_plate','T':'ic2:tesla_coil','S':'superconductor','H':'ic2:hv_transformer'},flag='vajra')
recipe('advanced_lappack','advanced_lappack',['P','C','E'],{'P':'ic2:energy_pack','C':'ic2:advanced_circuit','E':'ic2:energy_crystal'},flag='advanced_lappack')
for pack in ['lappack','advanced_lappack']:recipe('ultimate_lappack_from_'+pack,'ultimate_lappack',['CPC','CLC','CSC'],{'C':'ic2:lapotron_crystal','P':'ic2:iridium_plate','L':'ic2:lappack' if pack=='lappack' else pack,'S':'superconductor'},flag='ultimate_lappack')
# ---------- tools
tools={'advanced_drill':'advanceddrill','advanced_chainsaw':'advancedchainsaw','vajra':'vajra'}
for key,tex in tools.items():dump(A/'models/item'/f'{key}.json',{'parent':'minecraft:item/handheld','textures':{'layer0':'gravisuite:items/'+tex}})
modes=['hoe','treetap','wrench','screwdriver']
for m in modes:dump(A/'models/item/gravitool'/f'{m}.json',{'parent':'minecraft:item/handheld','textures':{'layer0':'gravisuite:items/gravitool/'+m}})
dump(A/'models/item/gravitool.json',{'parent':'gravisuite:item/gravitool/hoe','overrides':[{'predicate':{'gravisuite:mode':i/4},'model':'gravisuite:item/gravitool/'+m} for i,m in enumerate(modes)]})
for locale in ['en_us','ru_ru']:
 f=A/'lang'/f'{locale}.json';lang=json.loads(f.read_text())
 for key,old in {'advanced_drill':'advancedDrill','advanced_chainsaw':'advancedChainsaw','gravitool':'graviTool','vajra':'vajra'}.items():lang['item.gravisuite.'+key]=lang.get('gravisuite.'+old,key)
 dump(f,lang)
sounds={'tool_change':'toolchange','wrench':'wrench','treetap':'treetap','jetpack_loop':'jetpackloop','jetpack_fire':'jetpackfire'}
dump(A/'sounds.json',{k:{'category':'player','sounds':['gravisuite:'+v]} for k,v in sounds.items()})
recipe('advanced_drill','advanced_drill',['ODO','COC'],{'O':'ic2:overclocker_upgrade','D':'ic2:diamond_drill','C':'ic2:advanced_circuit'},flag='advanced_drill')
recipe('advanced_chainsaw','advanced_chainsaw',[' D ','OCO','AOA'],{'D':'#c:gems/diamond','O':'ic2:overclocker_upgrade','C':'ic2:chainsaw','A':'ic2:advanced_circuit'},flag='advanced_chainsaw')
recipe('gravitool','gravitool',['PHP','AEA','WCT'],{'P':'ic2:carbon_plate','H':'ic2:electric_hoe','A':'ic2:advanced_alloy','E':'ic2:energy_crystal','W':'ic2:electric_wrench','C':'ic2:advanced_circuit','T':'ic2:electric_treetap'},flag='gravitool')
recipe('vajra','vajra',['PEP','CVC','ALA'],{'P':'#c:plates/iron','E':'ic2:energy_crystal','C':'ic2:carbon_plate','V':'vajra_core','A':'ic2:advanced_alloy','L':'ic2:lapotron_crystal'},flag='vajra')
# ---------- flight chests
chests={'advanced_jetpack':'advancedjetpack','advanced_nano_chestplate':'advancednanochestplate','gravi_chestplate':'gravichestplate'}
for key,tex in chests.items():
 dump(A/'models/item'/f'{key}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'gravisuite:items/'+tex}})
 q=A/'textures/models/armor'/f'{key}_layer_1.png';q.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(REF/'textures/armour'/f'{tex}.png',q)
shutil.copy2(REF/'textures/armour/gravichestplateoverlay.png',A/'textures/models/armor/gravi_chestplate_layer_1_overlay.png')
for locale in ['en_us','ru_ru']:
 f=A/'lang'/f'{locale}.json';lang=json.loads(f.read_text())
 for key,old in {'advanced_jetpack':'advancedJetpack','advanced_nano_chestplate':'advancedNanoChestplate','gravi_chestplate':'graviChestplate'}.items():lang['item.gravisuite.'+key]=lang.get('gravisuite.'+old,key)
 lang['key.categories.gravisuite']='Gravisuite';lang['Gravi Fly Key']=lang.get('Gravi Fly Key','Gravi Fly Key')
 dump(f,lang)
dump(RES/'data/minecraft/tags/item/dyeable.json',{'replace':False,'values':['gravisuite:gravi_chestplate']})
recipe('advanced_jetpack','advanced_jetpack',['CJC','BLB','GAG'],{'C':'ic2:carbon_plate','J':'ic2:electric_jetpack','B':'engine_booster','L':'advanced_lappack','G':'ic2:glass_fibre_cable','A':'ic2:advanced_circuit'},flag='advanced_jetpack')
recipe('advanced_nano_chestplate','advanced_nano_chestplate',['CJC','CNC','GAG'],{'C':'ic2:carbon_plate','J':'advanced_jetpack','N':'ic2:nano_chestplate','G':'ic2:glass_fibre_cable','A':'ic2:advanced_circuit'},flag='advanced_nano_chestplate')
recipe('gravi_chestplate','gravi_chestplate',['SAS','DBD','SCS'],{'S':'superconductor','A':'ic2:quantum_chestplate','D':'gravitation_engine','B':'ic2:hv_transformer','C':'ultimate_lappack'},flag='gravi_chestplate')
# Recipes.changeQuantumRecipe: IC2's quantum chestplate from the advanced nano chestplate, or IC2's own when switched off
ic2q=json.loads((ROOT.parent/'industrialcraft2-modern/src/generated/resources/data/ic2/recipe/quantum_chestplate.json').read_text())
grav={'type':'ic2:shaped','category':'equipment','pattern':['ANA','ILI','IAI'],'key':{'N':{'item':'gravisuite:advanced_nano_chestplate'},'A':{'item':'ic2:advanced_alloy'},'I':{'item':'ic2:iridium_plate'},'L':{'item':'ic2:lapotron_crystal'}},'result':{'id':'ic2:quantum_chestplate','count':1}}
dump(RES/'data/ic2/recipe/quantum_chestplate.json',{'type':'gravisuite:config_choice','key':'replaceQuantumRecipe','if_true':grav,'if_false':ic2q})
print('Generated7components,2electricpacks,10nativeIC2charge-preserving recipes')
