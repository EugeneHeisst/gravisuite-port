package com.chocohead.gravisuite;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The original's gravisuite:*.ogg sounds (assets/gravisuite/sounds.json). */
public final class GraviSounds {
    public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(Registries.SOUND_EVENT, Gravisuite.MODID);
    public static final Supplier<SoundEvent> TOOL_CHANGE = reg("tool_change"), WRENCH = reg("wrench"), TREETAP = reg("treetap"), JETPACK_LOOP = reg("jetpack_loop"), JETPACK_FIRE = reg("jetpack_fire");
    private static Supplier<SoundEvent> reg(String name) { return REGISTRY.register(name, () -> SoundEvent.createVariableRangeEvent(Gravisuite.id(name))); }
    private GraviSounds() {}
}
