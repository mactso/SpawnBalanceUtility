package com.mactso.spawnbalanceutility;

import org.apache.commons.lang3.tuple.Pair;

import com.mactso.spawnbalanceutility.config.MyConfig;
import com.mactso.spawnbalanceutility.util.AllMobEntitiesReport;
import com.mactso.spawnbalanceutility.util.SpawnData;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.FMLNetworkConstants;

@Mod("spawnbalanceutility")
public class Main {

	    public static final String MODID = "spawnbalanceutility"; 
	    
	    public Main()
	    {
	    	System.out.println(MODID + ": Registering Mod.");
			ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST,
					() -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a,b) -> true));
	    	FMLJavaModLoadingContext.get().getModEventBus().register(this);
 	        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON,MyConfig.COMMON_SPEC );
 			MinecraftForge.EVENT_BUS.register(SpawnData.class);
 	        //   	        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
	    }


		@SubscribeEvent 
		public void preInit (final FMLCommonSetupEvent event) {
				System.out.println(MODID + ": Registering Handlers.  Version 1");
				AllMobEntitiesReport.doReport();
//				MinecraftForge.EVENT_BUS.register(new SpawnerBreakEvent ());
//				MinecraftForge.EVENT_BUS.register(new SpawnEventHandler());
//				MinecraftForge.EVENT_BUS.register(new MonsterDropEventHandler());
//				MinecraftForge.EVENT_BUS.register(new ExperienceDropEventHandler());
//				MinecraftForge.EVENT_BUS.register(new ChunkEvent());
//				CapabilityChunkLastMobDeathTime.register();
				//				MinecraftForge.EVENT_BUS.register(new MyEntityPlaceEvent());
		}   

		@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
	    public static class ModEvents
	    {

	    }	
		
	    @Mod.EventBusSubscriber()
	    public static class ForgeEvents
	    {
	        @SubscribeEvent(priority = EventPriority.LOWEST)
	        public static void onServerAboutToStart(FMLServerAboutToStartEvent event)
	        {
	    		if (MyConfig.isBalanceBiomeSpawnValues()) {
		        	SpawnData.balanceBiomeSpawnValues(event.getServer());
    				System.out.println("SpawnBalanceUtility: Balancing Biomes with BiomeMobWeight.CSV Spawn weight Values. ");
	    		}
	    		if (MyConfig.isFixSpawnValues()) {

	    			SpawnData.fixBiomeSpawnValues(event.getServer());
    				System.out.println(" SpawnBalanceUtility: Fixing biome extreme spawn values. ");
    				if (MyConfig.isFixEmptyNether() ) {
    					System.out.print(" SpawnBalanceUtility: Zombified piglin and ghasts will be added to Nether Zone.");
    				}
	    			
	    		}

//	        	SpawnData.initReports();
	        }
	        
	    	@SubscribeEvent
	        public static void onServerStarting(FMLServerStartingEvent event)
	        {

	    		if (MyConfig.isGenerateReport()) {
		        	SpawnData.generateBiomeReport(event);
	    		}
	        }

	        @SubscribeEvent
	        public static void onServerStopping(FMLServerStoppingEvent event)
	        {
//	        	MyConfig.debugMsg(1, "Spawn Balance Utility: Server Stopping");
	        }
	    }

}
