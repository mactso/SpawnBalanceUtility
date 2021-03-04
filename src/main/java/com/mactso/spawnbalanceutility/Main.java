package com.mactso.spawnbalanceutility;

import org.apache.commons.lang3.tuple.Pair;

import com.mactso.spawnbalanceutility.config.MyConfig;
import com.mactso.spawnbalanceutility.util.SpawnData;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
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
				System.out.println(MODID + ": Registering Handlers");
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
//	        @SubscribeEvent
//	        public static void onServerStarting(FMLServerStartingEvent event)
//	        {
//	        	ModEntities.addSpawnData();
//	        }
//
//	        @SubscribeEvent
//	        public static void onServerStopping(FMLServerStoppingEvent event)
//	        {
//	        	ModEntities.removeSpawnData();
//	        }
	    }

}
