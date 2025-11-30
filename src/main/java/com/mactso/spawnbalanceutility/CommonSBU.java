package com.mactso.spawnbalanceutility;

import com.mactso.spawnbalanceutility.util.AllMobEntitiesReport;
import com.mactso.spawnbalanceutility.util.SpawnBiomeData;
import com.mactso.spawnbalanceutility.util.SpawnStructData;

import net.minecraft.server.MinecraftServer;

public final class CommonSBU {
    public static void onServerStarted(MinecraftServer server) {
        SpawnBiomeData.doBiomeActions(server);
        SpawnStructData.doStructureActions(server);
        AllMobEntitiesReport.doReport();
    }
}
