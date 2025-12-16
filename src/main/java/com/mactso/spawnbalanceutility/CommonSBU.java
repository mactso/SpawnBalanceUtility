package com.mactso.spawnbalanceutility;

import com.mactso.spawnbalanceutility.util.AllMobEntitiesReport;
import com.mactso.spawnbalanceutility.util.SpawnBiomeData;
import com.mactso.spawnbalanceutility.util.SpawnStructData;
import com.mactso.spawnbalanceutility.util.Summary;

import net.minecraft.server.MinecraftServer;

public final class CommonSBU {
    public static void onServerStarted(MinecraftServer server) {
    	
        Summary.clear();
        SpawnBiomeData.doBiomeActions(server); // MyConfig checks moved down inside this method.
        SpawnStructData.doStructureActions(server);
        Summary.report();
        AllMobEntitiesReport.doReport();
    }
}
