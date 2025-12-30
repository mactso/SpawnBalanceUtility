package com.mactso.spawnbalanceutility.modloader.events;

import com.mactso.spawnbalanceutility.managers.ManagersInitialize;
import com.mactso.spawnbalanceutility.modloader.config.MyConfig;
import com.mactso.spawnbalanceutility.utilities.AllMobEntitiesReport;
import com.mactso.spawnbalanceutility.utilities.SpawnBiomeData;
import com.mactso.spawnbalanceutility.utilities.SpawnStructureData;
import com.mactso.spawnbalanceutility.utilities.Summary;
import com.mactso.spawnbalanceutility.utilities.MyUtilities;

import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles all server lifecycle events for SpawnBalanceUtility.
 */
@Mod.EventBusSubscriber
public class ServerEvents {

    @SubscribeEvent(priority = Priority.LOWEST)
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        ManagersInitialize.init();

        Summary.clear();

        if (MyConfig.isBalanceBiomeSpawnValues()) {
            SpawnBiomeData.balanceBiomeSpawnValues(event.getServer());
            MyUtilities.debugMsg(1, "SpawnBalanceUtility: Balancing Biomes with BiomeMobWeight.CSV Spawn weight Values.");
        }

        if (MyConfig.isFixSpawnValues()) {
            SpawnBiomeData.fixBiomeSpawnValues(event.getServer());
            MyUtilities.debugMsg(1, "SpawnBalanceUtility: Fixing biome extreme spawn values.");
            if (MyConfig.isFixEmptyNether()) {
                MyUtilities.debugMsg(2, "SpawnBalanceUtility: Zombified piglin and ghasts will be added to Nether Zone.");
            }
        }

        AllMobEntitiesReport.doReport(event.getServer());
        Summary.report();
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        if (MyConfig.isGenerateReport()) {
            SpawnBiomeData.generateBiomeReport(event);
            SpawnStructureData.generateStructureSpawnValuesReport(event);
        }
        MyUtilities.registerMissingSpawnPlacements();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        // Optional debug on shutdown
        // Utility.debugMsg(1, "SpawnBalanceUtility: Server Stopping");
    }
}
