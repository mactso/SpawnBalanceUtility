package com.mactso.spawnbalanceutility;

import com.mactso.spawnbalanceutility.config.MyConfig;
import com.mactso.spawnbalanceutility.util.AllMobEntitiesReport;
import com.mactso.spawnbalanceutility.util.SpawnBiomeData;
import com.mactso.spawnbalanceutility.util.SpawnStructData;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStarted;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;


public class Main implements ModInitializer, ServerStarted {

	    public static final String MODID = "spawnbalanceutility"; 
    
		@Override
		public void onInitialize() {

			MyConfig.registerConfigs();
			// zzz to go last.

			ServerLifecycleEvents.SERVER_STARTED.register(ResourceLocation.fromNamespaceAndPath("zzzzzzzz","zzzzzzzz"),this);

		}


		@Override
		public void onServerStarted(MinecraftServer server) {

			handleServerAboutToStart(server);

		}
		
        public static void handleServerAboutToStart(MinecraftServer server)
        {

    		SpawnBiomeData.doBiomeActions(server);
    		SpawnStructData.doStructureActions(server);
    		AllMobEntitiesReport.doReport();

        }

}






