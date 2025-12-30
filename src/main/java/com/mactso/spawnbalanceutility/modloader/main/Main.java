package com.mactso.spawnbalanceutility.modloader.main;

import com.mactso.spawnbalanceutility.modloader.config.MyConfig;
import com.mactso.spawnbalanceutility.utilities.MyUtilities;

import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("spawnbalanceutility")
public class Main {

	public static final String MODID = "spawnbalanceutility";

    public Main(FMLJavaModLoadingContext context)
    {
		context.registerConfig(ModConfig.Type.COMMON, MyConfig.COMMON_SPEC);
    	MyUtilities.debugMsg(0,MODID + ": Registering Mod.");
	}
    

	@SubscribeEvent
	public void preInit(final FMLCommonSetupEvent event) {
		MyUtilities.debugMsg(1, MODID + ": Registering Handlers.  Version 1");

	}

}
