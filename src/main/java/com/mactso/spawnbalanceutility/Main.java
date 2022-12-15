package com.mactso.spawnbalanceutility;

import com.mactso.spawnbalanceutility.config.MyConfig;
import com.mactso.spawnbalanceutility.util.SpawnBiomeData;
import com.mactso.spawnbalanceutility.util.SpawnStructData;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStarted;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStarting;
import net.minecraft.server.MinecraftServer;

public class Main implements ModInitializer, ServerStarted, ServerStarting {

	    public static final String MOD_ID = "spawnbalanceutility"; 
	    
		@Override
		public void onInitialize() {
			MyConfig.registerConfigs();
			ServerLifecycleEvents.SERVER_STARTING.register(this);
			ServerLifecycleEvents.SERVER_STARTED.register(this);

		}


		@Override
		public void onServerStarted(MinecraftServer server) {
			int debug = 5;
			handleServerAboutToStart(server);
			// TODO Auto-generated method stub
 		}

		@Override
		public void onServerStarting(MinecraftServer server) {
			int debug = 5;
			handleServerStarting(server);
			// TODO Auto-generated method stub
		
		}
		
//    	@SubscribeEvent(priority = EventPriority.LOWEST)  // TODO
        public static void handleServerAboutToStart(MinecraftServer server)
        {
        	

    		SpawnBiomeData.doBiomeActions(server);
    		SpawnStructData.doStructureActions(server);

        }
        
//    	@SubscribeEvent
        public static void handleServerStarting(MinecraftServer server)
        {


        }

}






