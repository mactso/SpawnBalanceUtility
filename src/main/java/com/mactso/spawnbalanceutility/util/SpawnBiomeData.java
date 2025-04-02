package com.mactso.spawnbalanceutility.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.spawnbalanceutility.config.MyConfigs;
import com.mactso.spawnbalanceutility.manager.BiomeCreatureManager;
import com.mactso.spawnbalanceutility.manager.BiomeCreatureManager.BiomeCreatureItem;
import com.mactso.spawnbalanceutility.manager.MobMassAdditionManager;
import com.mactso.spawnbalanceutility.manager.MobMassAdditionManager.MassAdditionMobItem;
import com.mactso.spawnbalanceutility.utility.Utility;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry.Reference;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.collection.Weight;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.biome.SpawnSettings.SpawnEntry;

public class SpawnBiomeData {
	private static final Logger LOGGER = LogManager.getLogger();
	static int biomelineNumber = 0;
	static int reportlinenumber = 0;
	static int biomeEventNumber = 0;
	static Set<String> biomesProcessed = new HashSet<>();
	static Set<String> structuresProcessed = new HashSet<>();

	static {
		initReports();
//		mappings.jar entry for /Biome -  
//         Fabric : 	f	Lcbr$b;	l	field_9329	category
// note- must have semicolon at end of type "Lcbr$b;"

		// don't need biome category in 1.19
//		try {
//			MappingResolver mapping = FabricLoader.getInstance().getMappingResolver();
//			String fieldName = mapping.mapFieldName("intermediary", "net.minecraft.class_1959", "field_9329",
//					"Lnet/minecraft/class_1959$class_1961;");
//			fieldBiomeCategory = Biome.class.getDeclaredField(fieldName); // fieldname makes work in dev and runtime.
//			fieldBiomeCategory.setAccessible(true);
//		} catch (Exception e) {
//			e.printStackTrace();
//			LOGGER.error("XXX Unexpected Reflection Failure set Biome.biomeCategory accessible");
//		}
	}

//	
	public static void initReports() {
		File fd = new File("config/spawnbalanceutility");
		if (!fd.exists())
			fd.mkdir();
		File fb = new File("config/spawnbalanceutility/BiomeMobWeight.rpt");
		if (fb.exists())
			fb.delete();
		File fma = new File("config/spawnbalanceutility/MassAdditionMobs.rpt");
		if (!(fma.exists()))
			generateMassAdditionMobsStubReport();
	}

	public static void doBiomeActions(MinecraftServer server) {

		initReports();

		if (MyConfigs.isBalanceBiomeSpawnValues()) {
			balanceBiomeSpawnValues(server);
		}
		if (MyConfigs.isFixSpawnValues()) {
			fixBiomeSpawnValues(server);
		}
		if (MyConfigs.isGenerateReport()) {
			generateBiomeSpawnValuesReport(server);
		}
	}

	public static void balanceBiomeSpawnValues(MinecraftServer server) {

		DynamicRegistryManager dynreg = server.getRegistryManager();
		Optional<Registry<Biome>> optRegistry = dynreg.getOptional(RegistryKeys.BIOME);
		if (optRegistry.isEmpty()) {
			LOGGER.error("Hard Error : Biome Registry Missing");
			return;
		}
		Registry<Biome> biomeRegistry = optRegistry.get();
		Field fieldSpawners = null;
		// get net/minecraft/world/level/biome/MobSpawnSettings/field_26405_
		// net/minecraft/world/level/biome/MobSpawnSettings/spawners
		try {
			MappingResolver mapping = FabricLoader.getInstance().getMappingResolver();
			String fieldName = mapping.mapFieldName("intermediary", "net.minecraft.class_5483", "field_26405",
					"Ljava/util/Map;");
			fieldSpawners = SpawnSettings.class.getDeclaredField(fieldName);
			fieldSpawners.setAccessible(true);
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("XXX Unexpected Reflection Failure set SpawnSettings.spawners accessible");
		}

		String vCl = "";
		List<SpawnEntry> newBalancedList = new ArrayList<>();

		for (Biome b : biomeRegistry) {
			RegistryKey<Biome> bk = biomeRegistry.getKey(b).get();

			Optional<Reference<Biome>> oRE = biomeRegistry.getOptional(bk);
			if (!oRE.isPresent()) {
				continue;
			}

			String bn = biomeRegistry.getId(b).toString();
			List<BiomeCreatureItem> modBiomeMobSpawners = BiomeCreatureManager.biomeCreaturesMap.get(bn);
			if (modBiomeMobSpawners == null) {
				LOGGER.warn("No spawn values found for biome: " + bn + " when balance flag is true.");
				modBiomeMobSpawners = new ArrayList<>();
				continue;
			}

			SpawnSettings msi = b.getSpawnSettings();

			Map<SpawnGroup, Pool<SpawnEntry>> newMap = new HashMap<>();

			for (SpawnGroup v : SpawnGroup.values()) {
				newBalancedList.clear();
				vCl = v.getName();
				for (BiomeCreatureItem biomeCreatureItem : modBiomeMobSpawners) {
					if (biomeCreatureItem.getClassification().toLowerCase().equals(vCl)) {

						Optional<Reference<EntityType<?>>> optRef = Registries.ENTITY_TYPE.getEntry(Identifier.of(biomeCreatureItem.getModAndMob()));

						if (optRef.isPresent()) {
							SpawnEntry newSpawner = new SpawnEntry(optRef.get().value(),
									Weight.of(biomeCreatureItem.getSpawnWeight()), biomeCreatureItem.getMinCount(),
									biomeCreatureItem.getMaxCount());
							newBalancedList.add(newSpawner);
						} else {
							LOGGER.error(reportlinenumber + "SpawnBalanceUtility ERROR: Mob "
									+ biomeCreatureItem.getModAndMob() + " not in Entity Type Registry");
						}
					}
				}
				newMap.put(v, Pool.of(newBalancedList));
			}
			try {
				fieldSpawners.set(msi, newMap);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	public static void fixBiomeSpawnValues(MinecraftServer server) {

		LOGGER.warn(" SpawnBalanceUtility: Fixing biome extreme spawn values. ");

		DynamicRegistryManager dynreg = server.getRegistryManager();
		Optional<Registry<Biome>> optRegistry = dynreg.getOptional(RegistryKeys.BIOME);
		if (optRegistry.isEmpty()) {
			LOGGER.error("Hard Error : Biome Registry Missing");
			return;
		}
		Registry<Biome> biomeRegistry = optRegistry.get();

		Field fieldSpawners = null;
		try {
			MappingResolver mapping = FabricLoader.getInstance().getMappingResolver();
			String fieldName = mapping.mapFieldName("intermediary", "net.minecraft.class_5483", "field_26405",
					"Ljava/util/Map;");
			fieldSpawners = SpawnSettings.class.getDeclaredField(fieldName);
			fieldSpawners.setAccessible(true);
		} catch (Exception e) {
			LOGGER.error("XXX Unexpected Reflection Failure lateBalanceBiomeSpawnValues");
			return;
		}

		List<SpawnEntry> newFixedList = new ArrayList<>();

		for (Biome b : biomeRegistry) {

			RegistryKey<Biome> bk = biomeRegistry.getKey(b).get();

			Optional<Reference<Biome>> oRE = biomeRegistry.getOptional(bk);
			if (!oRE.isPresent()) {
				continue;
			}
			String bcName = biomeRegistry.getKey(b).toString();

			boolean zombifiedPiglinSpawner = false;
			boolean ghastSpawner = false;

			SpawnSettings msi = b.getSpawnSettings();
			Map<SpawnGroup, Pool<SpawnEntry>> map = null;

			Map<SpawnGroup, Pool<SpawnEntry>> newMap = new HashMap<>();

			for (SpawnGroup mc : SpawnGroup.values()) {

				newFixedList.clear();
				Utility.debugMsg(1, "biome:" + Biome.REGISTRY_CODEC.simple() + ", " + b.toString());
				Pool<SpawnEntry> originalSpawnerEntryList = b.getSpawnSettings().getSpawnEntries(mc);
				for (SpawnEntry s : originalSpawnerEntryList.getEntries()) {

					int newSpawnWeight = s.getWeight().getValue();
					if (newSpawnWeight > MyConfigs.getMaxSpawnWeight()) {
						newSpawnWeight = MyConfigs.getMaxSpawnWeight();
					}
					if (newSpawnWeight < MyConfigs.getMinSpawnWeight()) {
						newSpawnWeight = MyConfigs.getMinSpawnWeight();
					}
					String key = s.type.toString();

					// FORGE int dSW = MyConfig.getDefaultSpawnWeight(key);
// FORGE			if (dSW != MyConfig.NO_DEFAULT_SPAWN_WEIGHT_FOUND) {
// FORGE				newSpawnWeight = dSW;
// FORGE			}

					SpawnEntry newS = new SpawnEntry(s.type, Weight.of(newSpawnWeight), s.minGroupSize, s.maxGroupSize);
					newFixedList.add(newS);

					if (Utility.getMyBC(oRE.get()) == Utility.NETHER) {
						if (s.type == EntityType.ZOMBIFIED_PIGLIN)
							zombifiedPiglinSpawner = true;
						if (s.type == EntityType.GHAST) {
							ghastSpawner = true;
						}
					}

				}

				List<MassAdditionMobItem> massAddMobs = MobMassAdditionManager.getFilteredList(mc, bcName);
				EntityType<?> et;
				for (MassAdditionMobItem ma : massAddMobs) {

					Optional<EntityType<?>> oe = EntityType.get(ma.getModAndMob());
					if (oe.isPresent()) {
						et = oe.get();
						boolean mobFound = false;
						for (SpawnEntry s : newFixedList) {
							if (s.type == et) {
								mobFound = true;
								break;
							}
						}
						if (mobFound == false) {
							SpawnEntry newS = new SpawnEntry(et, Weight.of(ma.getSpawnWeight()), ma.getMinCount(),
									ma.getMaxCount());
							newFixedList.add(newS);
						}
					}

				}

				if (Utility.getMyBC(oRE.get()) == Utility.NETHER) {
					if (mc == SpawnGroup.MONSTER) {
						if ((zombifiedPiglinSpawner == false) && (MyConfigs.isFixEmptyNether())) {
							SpawnEntry newS = new SpawnEntry(EntityType.ZOMBIFIED_PIGLIN,
									Weight.of(MyConfigs.getMinSpawnWeight()), 1, 4);
							newFixedList.add(newS);
						}

						if ((ghastSpawner == false) && (MyConfigs.isFixEmptyNether())) {
							SpawnEntry newS = new SpawnEntry(EntityType.GHAST,
									Weight.of((int) (MyConfigs.getMinSpawnWeight() * 0.75f)), 4, 4);
							newFixedList.add(newS);
						}
					}
				}

				newMap.put(mc, Pool.of(newFixedList));
			}

			try {
				fieldSpawners.set(msi, newMap);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

//
//
//
	private static void generateMassAdditionMobsStubReport() {

		PrintStream p = null;
		try {
			p = new PrintStream(new FileOutputStream("config/spawnbalanceutility/MassAdditionMobs.rpt", false));
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (p == null) {
			p = System.out;
		}

		p.println("* This is an example Mass Addition File that lets you add mobs to every biome.");
		p.println("* Lines that start with a '*' are comments and are not used.");
		p.println("* If you rename this file to MassAdditionMobs.csv, Spawn Balance Utility will use it.");
		p.println("*");
		p.println("* Parameter explainations and values.");
		p.println("* Parm Dimension  : A, O, N, E for All, Overworld, Nether, The End");
		p.println("* Parm Class      : MONSTER, CREATURE, AMBIENT, UNDERWATER, etc.");
		p.println("* Parm Resource   : modname:mobname");
		p.println(
				"* Parm Weight     : a number 1 or higher.  1 is superrare, 5 is rare, 20 is uncommon, 80 is common.");
		p.println("* Parm MinGroup   : a number 1 and less than MaxGroup");
		p.println("* Parm MaxGroup   : a number higher than MinGroup and usually 5 or less.");
		p.println("* Format is. Line, Dim,   Class, mod:mob,           spawnWeight, Mingroup, MaxGroup");
		p.println("*");
		p.println("* 1,   A, MONSTER, minecraft:phantom, 10           ,1         ,4");
		p.println("* will add phantoms too all biomes with a spawnweight of 10 and 1-4 group size.");
		p.println("*");
		if (p != System.out) {
			p.close();
		}
	}

//
	public static void generateBiomeSpawnValuesReport(MinecraftServer server) {

		DynamicRegistryManager dynreg = server.getRegistryManager();
		Optional<Registry<Biome>> optRegistry = dynreg.getOptional(RegistryKeys.BIOME);
		if (optRegistry.isEmpty()) {
			LOGGER.error("Hard Error : Biome Registry Missing");
			return;
		}
		Registry<Biome> biomeRegistry = optRegistry.get();

		PrintStream p = null;
		try {
			p = new PrintStream(new FileOutputStream("config/spawnbalanceutility/BiomeMobWeight.rpt", false));
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (p == null) {
			p = System.out;
		}

		p.println("* This is the BiomeMobWeight report file that is output every time the server starts.");
		p.println("* ");
		p.println("* Spawn Balance Utility (SBU) will use this file ONLY if it is renamed to BiomeMobWeight.csv.");
		p.println("* If you remove all lines for a Biome, the default values will be used");
		p.println("* So for an 'empty' biome, leave one line with a CREATURE or the AMBIENT line with bats.");
		p.println("* Lines starting with '*' are comments and ignored");
		p.println("* When this file is read, SBU writes summary information to the log file.");
		p.println("* ");

		for (Biome b : biomeRegistry) {

			RegistryKey<Biome> bk = biomeRegistry.getKey(b).get();

			Optional<Reference<Biome>> oRE = biomeRegistry.getOptional(bk);
			if (!oRE.isPresent()) {
				continue;
			}

			String categoryName = Utility.getMyBC(oRE.get());
			Identifier bi = bk.getValue();
			String biomeIdAsString = bi.toString();

			SpawnSettings msi = b.getSpawnSettings();
			for (SpawnGroup v : SpawnGroup.values()) {

				for (SpawnEntry s : msi.getSpawnEntries(v).getEntries()) {
					String creatureName = s.type.getUntranslatedName();
					@SuppressWarnings("deprecation")
					String creatureIdAsString = s.type.getRegistryEntry().getKey().get().getValue().toString();
					@SuppressWarnings("deprecation")
					String modname = s.type.getRegistryEntry().getKey().get().getValue().getNamespace();
					if (MyConfigs.isSuppressMinecraftMobReporting()) {
						if (modname.equals("minecraft")) {
							continue;
						}
					}
					if (MyConfigs.isIncludedMod(modname)) {
						p.println(++biomelineNumber + ", " + categoryName + ", " + biomeIdAsString + ", " + v + ", "
								+ creatureIdAsString + ", " + s.getWeight() + ", " + s.minGroupSize + ", "
								+ s.maxGroupSize);
					}
				}
			}
		}

		if (p != System.out) {
			p.close();
		}
	}

}
