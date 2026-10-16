package com.chocohead.gravisuite;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Recipe file that is one of two recipes depending on a Gravi config switch. Used to replace (or keep) IC2's quantum
 * chestplate recipe by overriding its id, like Recipes.changeQuantumRecipe of the original. The loaded recipe is the chosen
 * inner one, with its own serializer for the network.
 */
public final class ConfigChoiceRecipe {
    private ConfigChoiceRecipe() {}

    private record Choice(String key, Recipe<?> ifTrue, Recipe<?> ifFalse) {
        Recipe<?> chosen() { return switch (key) { case "replaceQuantumRecipe" -> Config.REPLACE_QUANTUM.get(); default -> { var flag = Config.RECIPES.get(key); yield flag != null && flag.get(); } } ? ifTrue : ifFalse; }
    }

    private static final MapCodec<Recipe<?>> CODEC = RecordCodecBuilder.<Choice>mapCodec(i -> i.group(Codec.STRING.fieldOf("key").forGetter(Choice::key),
            Recipe.CODEC.fieldOf("if_true").forGetter(Choice::ifTrue), Recipe.CODEC.fieldOf("if_false").forGetter(Choice::ifFalse)).apply(i, Choice::new))
        .xmap(Choice::chosen, r -> new Choice("", r, r));

    public static final DeferredRegister<RecipeSerializer<?>> REGISTRY = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Gravisuite.MODID);
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final Supplier<RecipeSerializer<?>> SERIALIZER = REGISTRY.register("config_choice", () -> new RecipeSerializer() {
        @Override public MapCodec codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, Recipe<?>> streamCodec() { return (StreamCodec)Recipe.STREAM_CODEC; }
    });
}
