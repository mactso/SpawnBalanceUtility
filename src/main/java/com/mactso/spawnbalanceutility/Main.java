package com.mactso.spawnbalanceutility;

import org.jetbrains.annotations.NotNull;

import com.mactso.spawnbalanceutility.config.MyConfig;
import com.mactso.spawnbalanceutility.util.AllMobEntitiesReport;
import com.mactso.spawnbalanceutility.util.MyStructureModifier;
import com.mactso.spawnbalanceutility.util.SpawnBiomeData;
import com.mactso.spawnbalanceutility.util.SpawnStructureData;
import com.mactso.spawnbalanceutility.util.Utility;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint.DisplayTest;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkConstants;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

@Mod("spawnbalanceutility")
public class Main {

	public static final String MODID = "spawnbalanceutility";

	public Main() {

		Utility.debugMsg(0, MODID + ": Registering Mod.");
		ModLoadingContext.get().registerExtensionPoint(DisplayTest.class,
				() -> new DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
		FMLJavaModLoadingContext.get().getModEventBus().register(this);
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MyConfig.COMMON_SPEC);
		MinecraftForge.EVENT_BUS.register(SpawnBiomeData.class);
	}

	@SubscribeEvent
	public void preInit(final FMLCommonSetupEvent event) {
		Utility.debugMsg(1, MODID + ": Registering Handlers.  Version 1");
		AllMobEntitiesReport.doReport();
	}

	@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
	public static class ModEvents {
		@SubscribeEvent
		public static void onRegister(final RegisterEvent event) {
			@NotNull
			ResourceKey<? extends Registry<?>> key = event.getRegistryKey();
			if (key.equals(ForgeRegistries.Keys.STRUCTURE_MODIFIER_SERIALIZERS))
				MyStructureModifier.register(event.getForgeRegistry());
		}

	}


	@Mod.EventBusSubscriber()
	public static class ForgeEvents {
		@SubscribeEvent(priority = EventPriority.LOWEST)
		public static void onServerAboutToStart(ServerAboutToStartEvent event) {
			if (MyConfig.isBalanceBiomeSpawnValues()) {
				SpawnBiomeData.balanceBiomeSpawnValues(event.getServer());
				Utility.debugMsg(1, "SpawnBalanceUtility: Balancing Biomes with BiomeMobWeight.CSV Spawn weight Values. ");
			}
			if (MyConfig.isFixSpawnValues()) {

				SpawnBiomeData.fixBiomeSpawnValues(event.getServer());
				Utility.debugMsg(1, " SpawnBalanceUtility: Fixing biome extreme spawn values. ");
				if (MyConfig.isFixEmptyNether()) {
					Utility.debugMsg(2, " SpawnBalanceUtility: Zombified piglin and ghasts will be added to Nether Zone.");
				}

			}

//	        	SpawnData.initReports();
		}

		@SubscribeEvent
		public static void onServerStarting(ServerStartingEvent event) {

			if (MyConfig.isGenerateReport()) {
				SpawnBiomeData.generateBiomeReport(event);
				SpawnStructureData.generateStructureSpawnValuesReport(event);
			}
			Utility.registerMissingSpawnPlacements();
		}

		@SubscribeEvent
		public static void onServerStopping(ServerStoppingEvent event) {
//	        	MyConfig.debugMsg(1, "Spawn Balance Utility: Server Stopping");
		}
	}

}
