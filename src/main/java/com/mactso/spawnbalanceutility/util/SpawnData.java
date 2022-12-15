//package com.mactso.spawnbalanceutility.util;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.PrintStream;
//import java.lang.reflect.Field;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//import java.util.Set;
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
//import com.mactso.spawnbalanceutility.config.MyConfig;
//import com.mactso.spawnbalanceutility.manager.BiomeCreatureManager;
//import com.mactso.spawnbalanceutility.manager.BiomeCreatureManager.BiomeCreatureItem;
//
//import net.minecraft.entity.EntityType;
//import net.minecraft.entity.SpawnGroup;
//import net.minecraft.server.MinecraftServer;
//import net.minecraft.util.Identifier;
//import net.minecraft.util.collection.Weight;
//import net.minecraft.util.collection.WeightedList;
//import net.minecraft.util.registry.DynamicRegistryManager;
//import net.minecraft.util.registry.Registry;
//import net.minecraft.world.biome.Biome;
//import net.minecraft.world.biome.SpawnSettings;
//import net.minecraft.world.biome.SpawnSettings.SpawnEntry;
//import net.minecraft.world.spawner.Spawner;
//
////
////
////
//public class SpawnData {
//
//	static {
//		initReports();
//	}
//	private static final Logger LOGGER = LogManager.getLogger();
//	static int biomelineNumber = 0;
//	static int structureLineNumber = 0;
//	static int reportlinenumber = 0;
//	static int biomeEventNumber = 0;
//	static int structureEventNumber = 0;
//	static Set<String> biomesProcessed = new HashSet<>();
//	static Set<String> structuresProcessed = new HashSet<>();
//
//	//
//	public static void initReports() {
//		File fd = new File("config/spawnbalanceutility");
//		if (!fd.exists())
//			fd.mkdir();
//		File fb = new File("config/spawnbalanceutility/BiomeMobWeight.txt");
//		if (fb.exists())
//			fb.delete();
//		File fs = new File("config/spawnbalanceutility/StructMobWeight.txt");
//		if (fs.exists())
//			fs.delete();
//		File fma = new File("config/spawnbalanceutility/MassAdditionMobs.txt");
//		if (!(fma.exists()))
//			generateMassAdditionMobsStubReport();
//	}
//
//	private static void generateMassAdditionMobsStubReport() {
//
//		PrintStream p = null;
//		try {
//			p = new PrintStream(new FileOutputStream("config/spawnbalanceutility/MassAdditionMobs.txt", true));
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//		if (p == null) {
//			p = System.out;
//		}
//
//		p.println(
//				"Example mob mass addition file.  Add mobs with the pattern below and rename file to MassAdditionMobs.csv");
//		p.println("Line, Category*, Class**, Namespace:Mob, Weight, Mingroup , Maxgroup");
//		p.println("");
//		p.println("1, A, MONSTER, minecraft:phantom, 10, 1, 4");
//		p.println("");
//		p.println("* A, O, N, E for All, Overworld, Nether, The End");
//		p.println("** MONSTER,CREATURE,AMBIENT");
//		if (p != System.out) {
//			p.close();
//		}
//	}
//
////
////
//	public static void balanceBiomeSpawnValues(MinecraftServer server) {
//
//		DynamicRegistryManager dynreg = server.getRegistryManager();
//		Registry<Biome> biomeRegistry = dynreg.getManaged(Registry.BIOME_KEY);
//
//		Field field = null;
//		// get net/minecraft/world/level/biome/MobSpawnSettings/f_48329_
//		// net/minecraft/world/level/biome/SpawnSettings/spawners
////		try {
////			String name = ASMAPI.mapField("f_48329_");
////			field = SpawnSettings.class.getDeclaredField(name);
////			field.setAccessible(true);
////		} catch (Exception e) {
////			LOGGER.error("XXX Unexpected Reflection Failure balanceBiomeSpawnValues");
////			return;
////		}
//
////		String bCl = "";
//		String vCl = "";
//		String bc = "private";
//		for (Biome b : biomeRegistry) {
//			String bn = biomeRegistry.getKey(b).get().getValue().getPath();
//
//			List<BiomeCreatureItem> modBiomeMobSpawners = BiomeCreatureManager.biomeCreaturesMap.get(bn);
//			if (modBiomeMobSpawners == null) {
//				LOGGER.warn("XXX Balance Biomes True but BiomeMobWeight.CSV missing, empty, or has no valid mobs.");
//				modBiomeMobSpawners = new ArrayList<>();
//				continue;
//			}
//
//			SpawnSettings msi = b.getSpawnSettings();
//
//			Map<SpawnGroup, WeightedList<SpawnEntry>> newMap = new HashMap<>();
//
//			for (SpawnGroup v : SpawnGroup.values()) {
//				List<SpawnEntry> newFixedList = new ArrayList<>();
//				vCl = v.getName();
//				for (BiomeCreatureItem biomeCreatureItem : modBiomeMobSpawners) {
//					if (biomeCreatureItem.getClassification().toLowerCase().equals(vCl)) {
//						@SuppressWarnings("deprecation")
//						Optional<EntityType<?>> opt = Optional
//								.of(Registry.ENTITY_TYPE.get(new Identifier(biomeCreatureItem.getModAndMob())));
//						if (opt.isPresent()) {
//							SpawnEntry newSpawnEntry = new SpawnEntry(opt.get(),
//									Weight.of(biomeCreatureItem.getSpawnWeight()), biomeCreatureItem.getMinCount(),
//									biomeCreatureItem.getMaxCount());
//							newFixedList.add(newSpawnEntry);
//						} else {
//							System.out.println(reportlinenumber + "SpawnBalanceUtility ERROR: Mob "
//									+ biomeCreatureItem.getModAndMob() + " not in Entity Type Registry");
//						}
//					}
//				}
//
//				newMap.put(v, WeightedList.newFixedList);
//			}
//			try {
//				field.set(msi, newMap);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//
//	}
//
//	@SuppressWarnings("unchecked")
//	public static void fixBiomeSpawnValues(MinecraftServer server) {
//
//		DynamicRegistryManager dynreg = server.getRegistryManager();
//		Registry<Biome> biomeRegistry = dynreg.getManaged(Registry.BIOME_KEY);
//
////		Field field = null;
////		try {
////			String name = ASMAPI.mapField("f_48329_");
////			field = SpawnSettings.class.getDeclaredField(name);
////			field.setAccessible(true);
////		} catch (Exception e) {
////			LOGGER.error("XXX Unexpected Reflection Failure lateBalanceBiomeSpawnValues");
////			return;
////		}
//
//		for (Biome b : biomeRegistry) {
//			String bn = biomeRegistry.getKey(b).toString();
//			SpawnSettings msi = b.getSpawnSettings();
//			Map<SpawnGroup, WeightedList<Spawner>> map = null;
//		}
//
//	}
//
//	public static void generateBiomeReport(MinecraftServer server) {
//
//		PrintStream p = null;
//		try {
//			p = new PrintStream(new FileOutputStream("config/spawnbalanceutility/BiomeMobWeight.txt", true));
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//		if (p == null) {
//			p = System.out;
//		}
//
//		DynamicRegistryManager dynreg = server.getRegistryManager();
//		Registry<Biome> biomeRegistry = dynreg.getManaged(Registry.BIOME_KEY);
//
//		for (Biome b : biomeRegistry) {
//
//			String cn = "private";
//			String bn = biomeRegistry.getKey(b).get().getValue().getPath();
//			SpawnSettings msi = b.getSpawnSettings();
//			for (SpawnGroup v : SpawnGroup.values()) {
//
//				for (SpawnEntry s : msi.getSpawnEntries(v).getEntries()) {
//					String modname = s.type.getRegistryEntry().getKey().get().getValue().getNamespace();
//					if (MyConfig.isSuppressMinecraftMobReporting()) {
//						if (modname.equals("minecraft")) {
//							continue;
//						}
//					}
//					if (MyConfig.isIncludedMod(modname)) {
//						p.println(++biomelineNumber + ", " + cn + ", " + bn + ", " + v + ", " + modname + ", "
//								+ s.getWeight() + ", " + s.minGroupSize + ", " + s.maxGroupSize);
//					}
//				}
//			}
//		}
//
//		if (p != System.out) {
//			p.close();
//		}
//	}
//
//}
