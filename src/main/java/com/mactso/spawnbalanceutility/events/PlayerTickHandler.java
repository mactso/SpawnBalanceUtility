package com.mactso.spawnbalanceutility.events;

import com.mactso.spawnbalanceutility.Main;
import com.mactso.spawnbalanceutility.manager.PsuedoMobManager;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@Mod.EventBusSubscriber(bus = Bus.FORGE, modid = Main.MODID)
public class PlayerTickHandler {
	int delayticker = 0;
	// assumes this event only raised for server worlds. TODO verify.
	@SubscribeEvent
	public static void onPLayerTickEvent(PlayerTickEvent.Pre event) {
		
		// this is always serverlevel
		if (event.player instanceof ServerPlayer p) {
			if (p.getId()%80 ==  p.level().getGameTime()%80) { // throttle to reduce CPU impact
				PsuedoMobManager.checkSpawnPsuedoMob(p);
			}
		}
	}

}
