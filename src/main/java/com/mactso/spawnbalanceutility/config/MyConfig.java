package com.mactso.spawnbalanceutility.config;

import java.util.HashSet;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.spawnbalanceutility.Main;
import com.mactso.spawnbalanceutility.manager.BiomeCreatureManager;
import com.mactso.spawnbalanceutility.manager.MobMassAdditionManager;
import com.mactso.spawnbalanceutility.manager.PsuedoMobManager;
import com.mactso.spawnbalanceutility.manager.StructureCreatureManager;
import com.mactso.spawnbalanceutility.util.Utility;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MyConfig {
	private static final Logger LOGGER = LogManager.getLogger();
	public static final Common COMMON;
	public static final ForgeConfigSpec COMMON_SPEC;
	public static final int NO_DEFAULT_SPAWN_WEIGHT_FOUND = -999;
	

	static {
		final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
		COMMON_SPEC = specPair.getRight();
		COMMON = specPair.getLeft();
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

	public static int getDefaultSpawnWeight (String key) {
		if (defaultSpawnWeightList.isEmpty()) return -999;
		String[]arrayItem = defaultSpawnWeightList.toArray(new String[0]);
		Utility.debugMsg(1, "Considering Default Spawn Weight 'key' : " + key);
		for (int i = 0; i<defaultSpawnWeightList.size();i++) {
			String s = arrayItem[i];
			String[] ret = s.split(",");
			Utility.debugMsg(1, "ret ='" + ret[0] + "', default spawn weight: " + ret[1]);
			if (ret[0].equals(key)) {
				int dSW = Integer.parseInt(ret[1]);  
				return dSW;
			}
		}
		return NO_DEFAULT_SPAWN_WEIGHT_FOUND;
	}
	

	
	public static HashSet<String> getFixSpawnPlacementMobsSet() {
		return fixSpawnPlacementMobsSet;
	}



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
	public static HashSet<String> defaultSpawnWeightList;
	public static HashSet<String> fixSpawnPlacementMobsSet;
	
	@SubscribeEvent
	public static void onModConfigEvent(final ModConfigEvent configEvent) {
		Utility.debugMsg(0, Main.MODID + " Reload Configuration");
		if (configEvent.getConfig().getSpec() == MyConfig.COMMON_SPEC) {
			bakeConfig();
		}
	}

	public static void pushDebugValue() {
		Utility.debugMsg(0, " Change DebugLevel:" + MyConfig.debugLevel);
		COMMON.debugLevel.set(MyConfig.debugLevel);
	}

	public static void bakeConfig() {
		
		debugLevel = COMMON.debugLevel.get();

		generateReport = COMMON.generateReport.get();
		suppressMinecraftMobReporting = COMMON.suppressMinecraftMobReporting.get();
		fixEmptyNether  = COMMON.fixEmptyNether.get();
		balanceBiomeSpawnValues = COMMON.balanceBiomeSpawnValues.get();
		fixSpawnValues = COMMON.fixSpawnValues.get();
		balanceStructureSpawnValues = COMMON.balanceStructureSpawnValues.get();
		includedReportModsSet = getModStringSet (extract(COMMON.includedReportModsSet.get()));
		fixSpawnPlacementMobsSet = getFixSpawnPlacementMobs (extract(COMMON.fixSpawnPlacementMobs.get()));
		
		minSpawnWeight = COMMON.minSpawnWeight.get();
		maxSpawnWeight = COMMON.maxSpawnWeight.get();
		defaultSpawnWeightList = getSpawnWeightStringSet(extract(COMMON.defaultSpawnWeightList.get()));
		

		PsuedoMobManager.psuedoMobInit();
		BiomeCreatureManager.biomeCreatureInit();
		StructureCreatureManager.structureCreatureInit();
		MobMassAdditionManager.massAdditionMobsInit();
	}
	


	public static String[] extract(String s) {
		String[] ret = s.split(";");
		return ret;
	}

	private static HashSet<String> getFixSpawnPlacementMobs(String[] values) {
		HashSet<String> set = new HashSet<>();
		for (String s : values) {
			String s2 = s.trim().toLowerCase();
			if (!s2.isEmpty()) {
				set.add(s2);
			}
		}
		return set;
	}
	
	public static HashSet<String> getModStringSet (String[] values) {
		HashSet<String> set = new HashSet<>();
		ModList modlist = ModList.get();
		for (String s : values) {
			String s2 = s.trim().toLowerCase();
			if (!s2.isEmpty()) {
				if (modlist.isLoaded(s2)) {
					set.add(s2);
				} else {
					LOGGER.warn("spawnbalanceutility includedReportModsSet entry : " +s2 + " is not a valid current loaded forge mod.");
				} 
			}
		}
		return set;
	}

	public static HashSet<String> getSpawnWeightStringSet (String[] values) {
		HashSet<String> set = new HashSet<>();
		for (String s : values) {
			String s2 = s.trim().toLowerCase();
			if (!s2.isEmpty()) {
				set.add(s2);
			}
		}
		return set;
	}
	
	public static class Common {

		public final IntValue debugLevel;
		public final BooleanValue generateReport;
		public final BooleanValue suppressMinecraftMobReporting;
		public final BooleanValue fixEmptyNether;
		public final BooleanValue balanceBiomeSpawnValues;
		public final BooleanValue fixSpawnValues;
		public final BooleanValue balanceStructureSpawnValues;
		public final ConfigValue<String> includedReportModsSet;
		public final IntValue minSpawnWeight;
		public final IntValue maxSpawnWeight;
		public final ConfigValue<String> defaultSpawnWeightList;
		public final ConfigValue<String> fixSpawnPlacementMobs;
		
		
		
		public Common(ForgeConfigSpec.Builder builder) {
			builder.push("Spawn Biome Utility Control Values");

			debugLevel = builder.comment("Debug Level: 0 = Off, 1 = Log, 2 = Chat+Log")
					.translation(Main.MODID + ".config." + "debugLevel").defineInRange("debugLevel", () -> 0, 0, 2);
			
			generateReport = builder.comment("generateReport")
					.translation(Main.MODID + ".config." + "generateReport")
					.define("generateReport", true);

			suppressMinecraftMobReporting = builder.comment("suppressMinecraftMobReporting")
					.translation(Main.MODID + ".config." + "suppressMinecraftMobReporting")
					.define("suppressMinecraftMobReporting", false);

			fixEmptyNether = builder.comment("fixEmptyNether")
					.translation(Main.MODID + ".config." + "fixEmptyNether")
					.define("fixEmptyNether", true);

			balanceBiomeSpawnValues = builder.comment("Use the 'BiomeMobWeight.csv' file to balance Biome spawn values")
					.translation(Main.MODID + ".config." + "balanceBiomeSpawnValues")
					.define("balanceBiomeSpawnValues", true);

			fixSpawnValues = builder.comment("Fix min, max values and add nether creatures")
					.translation(Main.MODID + ".config." + "fixSpawnValues")
					.define("fixSpawnValues", true);

			balanceStructureSpawnValues = builder.comment("Use the 'StructMobWeight.csv' file to balance structure spawn values")
					.translation(Main.MODID + ".config." + "balanceStructureSpawnValues")
					.define("balanceStructureSpawnValues", true);
			
			includedReportModsSet = builder.comment("Mod Name set separated by SemiColons of mods to report.")
					.translation(Main.MODID + ".config" + "includeReportModsSet")
					.define("includedReportModsSet", "exampleModName;");

			builder.pop();
			builder.push("Spawn Placement Fixes");
			
			fixSpawnPlacementMobs = builder.comment("Add mobs spawning in the air and falling to this list .")
					.translation(Main.MODID + ".config" + "fixSpawnPlacementMobs")
					.define("defaultSpawnWeightList", "minecraft:piglin_brute;goblinanddungeon:gob;goblinanddungeon:hobgob;");
			
			builder.pop();			

			builder.push("Spawn Weight Values");
			minSpawnWeight = builder.comment("minimum Spawn Weight")
					.translation(Main.MODID + ".config." + "minSpawnWeight").defineInRange("minSpawnWeight", () -> 10, 1, 1000);

			maxSpawnWeight = builder.comment("maximum Spawn Weight")
					.translation(Main.MODID + ".config." + "maxSpawnWeight").defineInRange("maxSpawnWeight", () -> 80, 1, 1000);

			defaultSpawnWeightList = builder.comment("list of Mod:MobName,DefaultSpawnweight;")
					.translation(Main.MODID + ".config" + "defaultSpawnWeightList")
					.define("defaultSpawnWeightList", "minecraft:enderman,5;minecraft:witch,5;");
			
			builder.pop();	

		}
	}

}
