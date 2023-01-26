package com.mactso.spawnbalanceutility.config;

import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.spawnbalanceutility.Main;
import com.mactso.spawnbalanceutility.manager.BiomeCreatureManager;
import com.mactso.spawnbalanceutility.manager.MobMassAdditionManager;
import com.mactso.spawnbalanceutility.manager.StructureCreatureManager;
import com.mojang.datafixers.util.Pair;

public class MyConfig {

	private static final Logger LOGGER = LogManager.getLogger();
	public static final int NO_DEFAULT_SPAWN_WEIGHT_FOUND = -999;
	public static SimpleConfig CONFIG;
	private static ModConfigProvider configs;
	private static final String defaultIncludedReportModsSet = "*;";
	private static final String defaultSpawnWeightList = "minecraft:enderman,5;minecraft:witch,5;";
	
	public static void registerConfigs() {
		configs = new ModConfigProvider();
		createConfigs();

		CONFIG = SimpleConfig.of(Main.MOD_ID + "config").provider(configs).request();

		assignConfigs();
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

	

	public static boolean isIncludedMod (String modName) {
		if (includedReportModsSet.contains("*")) return true;
		if (includedReportModsSet.isEmpty()) return true;
		return includedReportModsSet.contains(modName);
	}

//	public static int getDefaultSpawnWeight (String key) {
//		if (spawnWeightList.isEmpty()) return NO_DEFAULT_SPAWN_WEIGHT_FOUND;
//		String[]arrayItem = extract(defaultSpawnWeightList);
//		Utility.debugMsg(1, "Considering Default Spawn Weight 'key' : " + key);
//		for (int i = 0; i<exampleSpawnWeightList.size();i++) {
//			String s = arrayItem[i];
//			String[] ret = s.split(",");
//			Utility.debugMsg(1, "ret ='" + ret[0] + "', default spawn weight: " + ret[1]);
//			if (ret[0].equals(key)) {
//				int dSW = Integer.parseInt(ret[1]);  
//				return dSW;
//			}
//		}
//		return NO_DEFAULT_SPAWN_WEIGHT_FOUND;
//	}

	public static int debugLevel;
	private static boolean generateReport;
	private static boolean suppressMinecraftMobReporting;
	private static boolean fixEmptyNether;
	private static boolean balanceBiomeSpawnValues;
	private static boolean fixSpawnValues;
	private static boolean balanceStructureSpawnValues;
	public static int minSpawnWeight;
	public static int maxSpawnWeight;
	public static HashSet<String> includedReportModsSet;
	public static HashSet<String> spawnWeightList;

	private static void createConfigs() {
		configs.addKeyValuePair(new Pair<>("key.debugLevel", 0), "int");

		configs.addKeyValuePair(new Pair<>("key.generateReport", "true"), "String");
		configs.addKeyValuePair(new Pair<>("key.suppressMinecraftMobReporting", "false"), "String");
		configs.addKeyValuePair(new Pair<>("key.fixEmptyNether", "true"), "String");
		configs.addKeyValuePair(new Pair<>("key.balanceBiomeSpawnValues", "false"), "String");
		configs.addKeyValuePair(new Pair<>("key.fixSpawnValues", "true"), "String");
		configs.addKeyValuePair(new Pair<>("key.balanceStructureSpawnValues", "false"), "String");

		configs.addKeyValuePair(new Pair<>("key.includedReportModsSet", defaultIncludedReportModsSet), "String");
		configs.addKeyValuePair(new Pair<>("key.minSpawnWeight", 5), "int");
		configs.addKeyValuePair(new Pair<>("key.maxSpawnWeight", 80), "int");
//		configs.addKeyValuePair(new Pair<>("key.defaultSpawnWeightList", defaultSpawnWeightList), "String");
	}

	
	
	private static void assignConfigs() {
		
		debugLevel = CONFIG.getOrDefault("key.debugLevel", 0);
		generateReport = "true".equals(CONFIG.getOrDefault("key.generateReport", "true"));
		suppressMinecraftMobReporting = "true".equals(CONFIG.getOrDefault("key.suppressMinecraftMobReporting", "true"));
		fixEmptyNether = "true".equals(CONFIG.getOrDefault("key.fixEmptyNether", "true"));
		balanceBiomeSpawnValues = "true".equals(CONFIG.getOrDefault("key.balanceBiomeSpawnValues", "true"));
		fixSpawnValues = "true".equals(CONFIG.getOrDefault("key.fixSpawnValues", "true"));
		balanceStructureSpawnValues = "true".equals(CONFIG.getOrDefault("key.balanceStructureSpawnValues", "true"));
//		spawnWeightList = getSpawnWeightStringSet(extract(CONFIG.getOrDefault("key.defaultSpawnWeightList", defaultSpawnWeightList)));
		includedReportModsSet = getIncludedModStringSet(extract(CONFIG.getOrDefault("key.includedReportModsSet", defaultIncludedReportModsSet)));

		minSpawnWeight = CONFIG.getOrDefault("key.minSpawnWeight", 5);
		maxSpawnWeight = CONFIG.getOrDefault("key.maxSpawnWeight", 80);

		BiomeCreatureManager.biomeCreatureInit();
		StructureCreatureManager.structureCreatureInit();
		MobMassAdditionManager.massAdditionMobsInit();

		LOGGER.info("All " + configs.getConfigsList().size() + " have been set properly");
	}

	public static HashSet<String> getIncludedModStringSet (String[] values) {
		HashSet<String> set = new HashSet<>();
		
//		ModList modlist = ModList.get();  // Modlist.get() is forge.  Don't know how to do this in fabric yet.  
// 	    It's only used to validate the mod name is spelled right so dropping it.
		for (String s : values) {
			String s2 = s.trim().toLowerCase();
			if (!s2.isEmpty()) {
//				if (modlist.isLoaded(s2)) {
				set.add(s2);
//				} else {
//					LOGGER.warn("spawnbalanceutility includedReportModsSet entry : " +s2 + " is not a valid current loaded forge mod.");
//				} 
			}
		}
		return set;
	}

//	public static HashSet<String> getSpawnWeightStringSet(String[] values) {
//		HashSet<String> set = new HashSet<>();
//		for (String s : values) {
//			String s2 = s.trim().toLowerCase();
//			if (!s2.isEmpty()) {
//				set.add(s2);
//			}
//		}
//		return set;
//	}

	public static String[] extract(String s) {
		String[] ret = s.split(";");
		return ret;
	}

}
