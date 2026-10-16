package com.chocohead.gravisuite;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.*;
public record ConfigCondition(String key,boolean value) implements ICondition {
    public static final MapCodec<ConfigCondition> CODEC=RecordCodecBuilder.mapCodec(i->i.group(Codec.STRING.fieldOf("key").forGetter(ConfigCondition::key),Codec.BOOL.optionalFieldOf("value",true).forGetter(ConfigCondition::value)).apply(i,ConfigCondition::new));
    public static final DeferredRegister<MapCodec<? extends ICondition>> REGISTRY=DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS,Gravisuite.MODID);
    public static final java.util.function.Supplier<MapCodec<ConfigCondition>> TYPE=REGISTRY.register("config",()->CODEC);
    public boolean test(IContext context){var flag=Config.RECIPES.get(key);return flag!=null&&flag.get()==value;}public MapCodec<? extends ICondition> codec(){return CODEC;}
}
