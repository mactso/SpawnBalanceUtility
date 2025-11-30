package com.mactso.spawnbalanceutility.config;

import java.nio.file.Path;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.spawnbalanceutility.Main;
import com.mactso.spawnbalanceutility.manager.BiomeCreatureManager;
import com.mactso.spawnbalanceutility.manager.MobMassAdditionManager;
import com.mactso.spawnbalanceutility.manager.StructureCreatureManager;
import com.mactso.spawnbalanceutility.util.Utility;
import com.mojang.datafixers.util.Pair;

import net.fabricmc.loader.api.FabricLoader;


public class MyConfig {
	

	private static final Logger LOGGER = LogManager.getLogger();

	public static SimpleConfig CONFIG;
	private static ModConfigProvider configs;
	
	public static int debugLevel;
	private static boolean generateReport;
	private static boolean suppressMinecraftMobReporting;
	private static boolean fixEmptyNether;
	private static boolean balanceBiomeSpawnValues;
	private static boolean fixSpawnValues;
	private static boolean balanceStructureSpawnValues;
	public static int minSpawnWeight;
	public static int maxSpawnWeight;

	public static final int DEFAULT_MIN_SPAWN_WEIGHT = 6;
	public static final int DEFAULT_MAX_SPAWN_WEIGHT = 79;
	public static final int MOB_MIN_COUNT = 1;
	public static final int MOB_MAX_COUNT = 32;
	
	public static HashSet<String> includedReportModsSet;
	public static HashSet<String> spawnWeightOverrideSet;
	public static HashSet<String> fixSpawnPlacementMobsSet;
	
	private static final String defaultIncludedReportModsSet = "*;";
	private static final String defaultSpawnWeightOverrideList = "minecraft:enderman,5;minecraft:witch,5;";
	private static final String defaultFixSpawnPlacementMobs = "minecraft:piglin_brute;goblinanddungeon:gob;goblinanddungeon:hobgob;";

	
	public static final int NO_DEFAULT_SPAWN_WEIGHT_FOUND = -999;
	
	
	
	public static void registerConfigs() {
		configs = new ModConfigProvider();
		createConfigs();

		CONFIG = SimpleConfig.of(Main.MODID + "config").provider(configs).request();

		initializeConfigs();
	}

	public static int getDebugLevel() {
		return debugLevel;
	}

	public static void setDebugLevel(int debugLevel) {
		MyConfig.debugLevel = debugLevel;
	}

	public static boolean isGenerateReport() {
		return generateReport;
	}

	public static boolean isSuppressMinecraftMobReporting() {
		return suppressMinecraftMobReporting;
	}

	public static boolean isFixEmptyNether() {
		return fixEmptyNether;
	}

	public static boolean isBalanceBiomeSpawnValues() {
		return balanceBiomeSpawnValues;
	}

	public static boolean isFixSpawnValues() {
		return fixSpawnValues;
	}


	public static boolean isBalanceStructureSpawnValues() {
		return balanceStructureSpawnValues;
	}

	public static int getMinSpawnWeight() {
		return minSpawnWeight;
	}

	public static int getMaxSpawnWeight() {
		return maxSpawnWeight;
	}

	public static HashSet<String> getFixSpawnPlacementMobsSet() {
		return fixSpawnPlacementMobsSet;
	}	

	public static boolean isIncludedMod (String modName) {
		if (includedReportModsSet.contains("*")) return true;
		if (includedReportModsSet.isEmpty()) return true;
	    // Exact case-insensitive match
	    for (String s : includedReportModsSet) {
	        if (s.equalsIgnoreCase(modName)) {
	            return true;
	        }
	    }

	    return false;
	}


	public static int getSpawnWeightOverride (String key) {
		if (spawnWeightOverrideSet.isEmpty()) return NO_DEFAULT_SPAWN_WEIGHT_FOUND;
		Utility.debugMsg(1, "Considering Default Spawn Weight 'key' : " + key);
	    for (String entry : spawnWeightOverrideSet) {
	        String[] parts = entry.split(",");
	        if (parts.length != 2) {
				Utility.debugMsg(0, "Bad Default Spawn Weight Override Value (key,integer) : " + entry);
	        	continue;
	        }
	        if (parts[0].equalsIgnoreCase(key)) {
	            try {
	                return Integer.parseInt(parts[1]);
	            } catch (NumberFormatException ignored) { 
					Utility.debugMsg(0, "Bad Default Spawn Weight Override Integer Value (key,integer) : " + entry);
	            }
	        }
	    }
	    return NO_DEFAULT_SPAWN_WEIGHT_FOUND;

	}


	private static void createConfigs() {
		configs.addKeyValuePair(new Pair<>("key.debugLevel", 0), "int");

		configs.addKeyValuePair(new Pair<>("key.generateReport", "true"), "String");
		configs.addKeyValuePair(new Pair<>("key.suppressMinecraftMobReporting", "false"), "String");
		configs.addKeyValuePair(new Pair<>("key.fixEmptyNether", "true"), "String");
		configs.addKeyValuePair(new Pair<>("key.balanceBiomeSpawnValues", "true"), "String");
		configs.addKeyValuePair(new Pair<>("key.fixSpawnValues", "true"), "String");
		configs.addKeyValuePair(new Pair<>("key.balanceStructureSpawnValues", "true"), "String");
		configs.addKeyValuePair(new Pair<>("key.minSpawnWeight", DEFAULT_MIN_SPAWN_WEIGHT), "int");
		configs.addKeyValuePair(new Pair<>("key.maxSpawnWeight", DEFAULT_MAX_SPAWN_WEIGHT), "int");
		configs.addKeyValuePair(new Pair<>("key.defaultSpawnWeightList", defaultSpawnWeightOverrideList), "String");
		configs.addKeyValuePair(new Pair<>("key.includedReportModsSet", defaultIncludedReportModsSet), "String");
		configs.addKeyValuePair(new Pair<>("key.fixSpawnPlacementMobs", defaultFixSpawnPlacementMobs), "String");
	}

	
	private static void initializeConfigs() {
		
		debugLevel = CONFIG.getOrDefault("key.debugLevel", 0);
		generateReport = "true".equals(CONFIG.getOrDefault("key.generateReport", "true"));
		suppressMinecraftMobReporting = "true".equals(CONFIG.getOrDefault("key.suppressMinecraftMobReporting", "true"));
		fixEmptyNether = "true".equals(CONFIG.getOrDefault("key.fixEmptyNether", "true"));
		balanceBiomeSpawnValues = "true".equals(CONFIG.getOrDefault("key.balanceBiomeSpawnValues", "true"));
		fixSpawnValues = "true".equals(CONFIG.getOrDefault("key.fixSpawnValues", "true"));
		balanceStructureSpawnValues = "true".equals(CONFIG.getOrDefault("key.balanceStructureSpawnValues", "true"));
		spawnWeightOverrideSet = toLowercaseStringSet(extract(CONFIG.getOrDefault("key.defaultSpawnWeightList", defaultSpawnWeightOverrideList)));
		includedReportModsSet = toLowercaseStringSet(extract(CONFIG.getOrDefault("key.includedReportModsSet", defaultIncludedReportModsSet)));
		fixSpawnPlacementMobsSet = toLowercaseStringSet(extract(CONFIG.getOrDefault("key.fixSpawnPlacementMobs", defaultFixSpawnPlacementMobs)));
		minSpawnWeight = CONFIG.getOrDefault("key.minSpawnWeight", DEFAULT_MIN_SPAWN_WEIGHT);
		maxSpawnWeight = CONFIG.getOrDefault("key.maxSpawnWeight", DEFAULT_MAX_SPAWN_WEIGHT);

		Path p1 = FabricLoader.getInstance().getConfigDir()
                .resolve("spawnbalanceutility/BiomeMobWeight.csv");
        BiomeCreatureManager.biomeCreatureInit(p1);
        
		Path p2 = FabricLoader.getInstance().getConfigDir()
                .resolve("spawnbalanceutility/StructMobWeight.csv");
		StructureCreatureManager.structureCreatureInit(p2);

		Path p3 = FabricLoader.getInstance().getConfigDir()
                .resolve("spawnbalanceutility/MassAdditionMobs.csv");
		MobMassAdditionManager.massAdditionMobsInit(p3);

		LOGGER.info(Main.MODID + " : All " + configs.getConfigsList().size() + "configuration values have been set properly.");
	}


	public static HashSet<String> toLowercaseStringSet(String[] values) {
	    HashSet<String> set = new HashSet<>();
	    for (String s : values) {
	        if (s == null) continue;
	        String s2 = s.trim().toLowerCase();
	        if (!s2.isEmpty()) {
	            set.add(s2);
	        }
	    }
	    return set;
	}

	public static String[] extract(String s) {
		String[] ret = s.split(";");
		return ret;
	}

}
