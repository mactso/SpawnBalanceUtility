package com.mactso.spawnbalanceutility;

import com.mactso.spawnbalanceutility.config.MyConfig;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStarted;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;


public class Main implements ModInitializer, ServerStarted {

	    public static final String MODID = "spawnbalanceutility"; 
	    // Highest-sorting ResourceLocation using only a–z, 0–9
	    private static final ResourceLocation PROCESS_LATE =
	            ResourceLocation.fromNamespaceAndPath("zzzzzzzzz", "zzzzzzzzz");
    
		@Override
		public void onInitialize() {

			MyConfig.registerConfigs();
			
			// the value zzz,zzz will ensure SBU goes last after all other mods add their mobs.
	        ServerLifecycleEvents.SERVER_STARTED.register(PROCESS_LATE, this);
		}


		@Override
		public void onServerStarted(MinecraftServer server) {
		    CommonSBU.onServerStarted(server);
		}

}






