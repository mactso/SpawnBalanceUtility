package com.mactso.spawnbalanceutility;

import com.mactso.spawnbalanceutility.config.MyConfig;
import com.mactso.spawnbalanceutility.util.AllMobEntitiesReport;
import com.mactso.spawnbalanceutility.util.SpawnBiomeData;
import com.mactso.spawnbalanceutility.util.SpawnStructData;

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



@Mod("spawnbalanceutility")
public class Main {

	    public static final String MODID = "spawnbalanceutility"; 
	    
	    public Main()
	    {

	    	System.out.println(MODID + ": Registering Mod.");
	        ModLoadingContext.get().registerExtensionPoint(DisplayTest.class,
	        		() -> new DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
	    	FMLJavaModLoadingContext.get().getModEventBus().register(this);
 	        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON,MyConfig.COMMON_SPEC );
 			MinecraftForge.EVENT_BUS.register(SpawnBiomeData.class);
 			MinecraftForge.EVENT_BUS.register(SpawnStructData.class);

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
	        public static void onServerAboutToStart(ServerAboutToStartEvent event)
	        {
	    		if (MyConfig.isBalanceBiomeSpawnValues()) {
		        	SpawnBiomeData.balanceBiomeSpawnValues(event.getServer());
    				System.out.println("SpawnBalanceUtility: Balancing Biomes with BiomeMobWeight.CSV Spawn weight Values. ");
	    		}

	    		SpawnStructData.doStructureActions(event.getServer());
	    		
	    		if (MyConfig.isFixSpawnValues()) {
	    			SpawnBiomeData.fixBiomeSpawnValues(event.getServer());
    				System.out.println(" SpawnBalanceUtility: Fixing biome extreme spawn values. ");
    				if (MyConfig.isFixEmptyNether() ) {
    					System.out.println(" SpawnBalanceUtility: Zombified piglin and ghasts will be added to Nether Zone.");
    				}
	    			
	    		}

//	        	SpawnData.initReports();
	        }
	        
	    	@SubscribeEvent
	        public static void onServerStarting(ServerStartingEvent event)
	        {

	    		if (MyConfig.isGenerateReport()) {
		        	SpawnBiomeData.generateBiomeReport(event);
	    		}
	        }

	        @SubscribeEvent
	        public static void onServerStopping(ServerStoppingEvent event)
	        {
//	        	MyConfig.debugMsg(1, "Spawn Balance Utility: Server Stopping");
	        }
	    }

}
