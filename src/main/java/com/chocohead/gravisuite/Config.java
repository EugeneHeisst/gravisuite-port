package com.chocohead.gravisuite;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.*;
public final class Config {
 public static final ModConfigSpec SPEC;
 public static final Map<String,ModConfigSpec.BooleanValue> RECIPES=new LinkedHashMap<>();
 public static final ModConfigSpec.BooleanValue ACCURATE,REPLACE_QUANTUM;
 static{var b=new ModConfigSpec.Builder();b.push("recipes");for(String key:List.of("advanced_lappack","ultimate_lappack","advanced_jetpack","advanced_nano_chestplate","gravi_chestplate","advanced_drill","advanced_chainsaw","gravitool","vajra"))RECIPES.put(key,b.define(key,true));REPLACE_QUANTUM=b.define("replaceQuantumRecipe",true);b.pop().push("vajra");ACCURATE=b.define("accurateEnabled",true);b.pop();SPEC=b.build();}
}
