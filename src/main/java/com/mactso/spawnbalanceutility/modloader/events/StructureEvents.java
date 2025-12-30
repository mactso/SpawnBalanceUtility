package com.mactso.spawnbalanceutility.modloader.events;

import org.jetbrains.annotations.NotNull;

import com.mactso.spawnbalanceutility.utilities.MyStructureModifier;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/**
 * Handles MOD bus registration events for SpawnBalanceUtility.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class StructureEvents {

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        @NotNull
        ResourceKey<? extends Registry<?>> key = event.getRegistryKey();

        if (key.equals(ForgeRegistries.Keys.STRUCTURE_MODIFIER_SERIALIZERS)) {
            MyStructureModifier.register(event.getForgeRegistry());
        }
    }
}

