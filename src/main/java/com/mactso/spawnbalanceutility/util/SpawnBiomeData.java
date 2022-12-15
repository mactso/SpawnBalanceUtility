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

import com.mactso.spawnbalanceutility.config.MyConfig;
import com.mactso.spawnbalanceutility.manager.BiomeCreatureManager;
import com.mactso.spawnbalanceutility.manager.BiomeCreatureManager.BiomeCreatureItem;
import com.mactso.spawnbalanceutility.manager.MobMassAdditionManager;
import com.mactso.spawnbalanceutility.manager.MobMassAdditionManager.MassAdditionMobItem;
import com.mactso.spawnbalanceutility.utility.Utility;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.collection.Weight;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.Category;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.biome.SpawnSettings.SpawnEntry;

public class SpawnBiomeData {
	private static Field fieldBiomeCategory = null;
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

		try {
			MappingResolver mapping = FabricLoader.getInstance().getMappingResolver();
			String fieldName = mapping.mapFieldName("intermediary", "net.minecraft.class_1959", "field_9329",
					"Lnet/minecraft/class_1959$class_1961;");
			fieldBiomeCategory = Biome.class.getDeclaredField(fieldName); // fieldname makes work in dev and runtime.
			fieldBiomeCategory.setAccessible(true);
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("XXX Unexpected Reflection Failure set Biome.biomeCategory accessible");
		}
	}

//	
	public static void initReports() {
		File fd = new File("config/spawnbalanceutility");
		if (!fd.exists())
			fd.mkdir();
		File fb = new File("config/spawnbalanceutility/BiomeMobWeight.txt");
		if (fb.exists())
			fb.delete();
		File fma = new File("config/spawnbalanceutility/MassAdditionMobs.txt");
		if (!(fma.exists()))
			generateMassAdditionMobsStubReport();
	}


	public static void doBiomeActions(MinecraftServer server) {

		DynamicRegistryManager dynreg = server.getRegistryManager();
		Registry<Biome> csfreg = dynreg
				.getManaged(Registry.BIOME_KEY);
    	initReports();

		if (MyConfig.isBalanceStructureSpawnValues()) {
			balanceBiomeSpawnValues(csfreg);
		}
		if (MyConfig.isFixSpawnValues()) {
			fixBiomeSpawnValues(csfreg);
		}
		if (MyConfig.isGenerateReport()) {
			generateBiomeSpawnValuesReport(csfreg);
		}
	}
	
	public static void balanceBiomeSpawnValues(Registry<Biome> registry) {

		LOGGER.warn("SpawnBalanceUtility: Balancing Biomes with BiomeMobWeight.CSV Spawn weight Values. ");

		Field fieldSpawners = null;

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
		
		for (Biome b : registry) {
			RegistryKey<Biome> bk = registry.getKey(b).get();
			Identifier bi = bk.getValue();
			String bn = bi.toString();
			int debug = 7;
			List<BiomeCreatureItem> modBiomeMobSpawners = BiomeCreatureManager.biomeCreaturesMap.get(bn);
			if (modBiomeMobSpawners == null) {
				LOGGER.warn("No spawn values found for biome: "+ bn + " when balance flag is true.");
				modBiomeMobSpawners = new ArrayList<>();
				continue;
			}

			SpawnSettings msi = b.getSpawnSettings();

			Map<SpawnGroup, Pool<SpawnEntry>> newMap = new HashMap<>();

			for (SpawnGroup v : SpawnGroup.values()) {
				vCl = v.getName();
				newBalancedList.clear();
				for (BiomeCreatureItem biomeCreatureItem : modBiomeMobSpawners) {
					if (biomeCreatureItem.getClassification().toLowerCase().equals(vCl)) {

						Optional<EntityType<?>> opt = Registry.ENTITY_TYPE
								.getOrEmpty(new Identifier(biomeCreatureItem.getModAndMob()));
						if (opt.isPresent()) {
							SpawnEntry newSpawner = new SpawnEntry(opt.get(),
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

//
//
	private static Category getBiomeCategory(Biome b) {
		Category bc = Category.PLAINS;
		try {
			bc = (Category) fieldBiomeCategory.get(b);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return bc;
	}


	public static void fixBiomeSpawnValues(Registry<Biome> registry) {

		LOGGER.warn(" SpawnBalanceUtility: Fixing biome extreme spawn values. ");
		if (MyConfig.isFixEmptyNether() ) {
			LOGGER.warn(" SpawnBalanceUtility: Adding zombified piglin and ghasts to all Nether Zones.");
		}	
		
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

		for (Biome b : registry) {
			boolean zombifiedPiglinSpawner = false;
			boolean ghastSpawner = false;
			RegistryKey<Biome> brk = registry.getKey(b).get();
			Identifier bi = brk.getValue();
			String bn = bi.toString();

			SpawnSettings msi = b.getSpawnSettings();
			Map<SpawnGroup, Pool<SpawnEntry>> map = null;

			Map<SpawnGroup, Pool<SpawnEntry>> newMap = new HashMap<>();

			for (SpawnGroup v : SpawnGroup.values()) {

				newFixedList.clear();
				Utility.debugMsg(1, "biome:" + b.REGISTRY_CODEC.simple() + ", " + b.toString());
				Pool<SpawnEntry> originalSpawnerEntryList = b.getSpawnSettings().getSpawnEntries(v);
				for (SpawnEntry s : originalSpawnerEntryList.getEntries()) {

					int newSpawnWeight = s.getWeight().getValue();
					if (newSpawnWeight > MyConfig.getMaxSpawnWeight()) {
						newSpawnWeight = MyConfig.getMaxSpawnWeight();
					}
					if (newSpawnWeight < MyConfig.getMinSpawnWeight()) {
						newSpawnWeight = MyConfig.getMinSpawnWeight();
					}
					String key = s.type.toString();
					SpawnEntry newS = new SpawnEntry(s.type, Weight.of(newSpawnWeight), s.minGroupSize, s.maxGroupSize);
					newFixedList.add(newS);
					if (getBiomeCategory(b) == Biome.Category.NETHER) {
						if (s.type == EntityType.ZOMBIFIED_PIGLIN)
							zombifiedPiglinSpawner = true;
						if (s.type == EntityType.GHAST) {
							ghastSpawner = true;
						}
					}
				}

				List<MassAdditionMobItem> massAddMobs = MobMassAdditionManager.getFilteredList(v, getBiomeCategory(b));
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

				if (getBiomeCategory(b) == Biome.Category.NETHER) {
					if (v == SpawnGroup.MONSTER) {
						if ((zombifiedPiglinSpawner == false) && (MyConfig.isFixEmptyNether())) {
							SpawnEntry newS = new SpawnEntry(EntityType.ZOMBIFIED_PIGLIN,
									Weight.of(MyConfig.getMinSpawnWeight()), 1, 4);
							newFixedList.add(newS);
						}

						if ((ghastSpawner == false) && (MyConfig.isFixEmptyNether())) {
							SpawnEntry newS = new SpawnEntry(EntityType.GHAST,
									Weight.of((int) (MyConfig.getMinSpawnWeight() * 0.75f)), 4, 4);
							newFixedList.add(newS);
						}
					}
				}

				newMap.put(v, Pool.of(newFixedList));
			}
			int debug = 3;
			try {
				fieldSpawners.set(msi, newMap);
			} catch (Exception e) {
				// TODO Auto-generated catch block
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
			p = new PrintStream(new FileOutputStream("config/spawnbalanceutility/MassAdditionMobs.txt", false));
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (p == null) {
			p = System.out;
		}

		p.println(
				"* Example mob mass addition file.  Add mobs with the pattern below and rename file to MassAdditionMobs.csv");
		p.println("* Line, Dimension , Class**, Namespace:Mob, Weight, Mingroup , Maxgroup");
		p.println("*");
		p.println("* Example... 1, A, MONSTER, minecraft:phantom, 10, 1, 4");
		p.println("*");
		p.println("* Parm Dimension  : A, O, N, E for All, Overworld, Nether, The End");
		p.println("* Parm Class      : MONSTER, CREATURE, AMBIENT, UNDERWATER, etc.");
		p.println("* Parm Resource   : modname:mobname");
		p.println(
				"* Parm Weight     : a number 1 or higher.  1 is superrare, 5 is rare, 20 is uncommon, 80 is common.");
		p.println("* Parm MinGroup   : a number 1 and less than MaxGroup");
		p.println("* Parm MaxGroup   : a number higher than MinGroup and usually 5 or less.");
		p.println("*");
		if (p != System.out) {
			p.close();
		}
	}

//
	public static void generateBiomeSpawnValuesReport(Registry<Biome> registry) {

		PrintStream p = null;
		try {
			p = new PrintStream(new FileOutputStream("config/spawnbalanceutility/BiomeMobWeight.txt", false));
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (p == null) {
			p = System.out;
		}


		for (Biome b : registry) {
			String categoryName = getBiomeCategory(b).asString();
			RegistryKey<Biome> bk = registry.getKey(b).get();
			Identifier bi = bk.getValue();
			String biomeIdAsString = bi.toString();

			

			
			SpawnSettings msi = b.getSpawnSettings();
			for (SpawnGroup v : SpawnGroup.values()) {

				for (SpawnEntry s : msi.getSpawnEntries(v).getEntries()) {
					String creatureName = s.type.getUntranslatedName();
					String creatureIdAsString = s.type.getRegistryEntry().getKey().get().getValue().toString();
					String modname = s.type.getRegistryEntry().getKey().get().getValue().getNamespace() ;
					if (MyConfig.isSuppressMinecraftMobReporting()) {
						if (modname.equals("minecraft")) {
							continue;
						}
					}
					if (MyConfig.isIncludedMod(modname)) {
						p.println(++biomelineNumber + ", " + categoryName + ", " + biomeIdAsString + ", " + v + ", " + creatureIdAsString
								+ ", " + s.getWeight() + ", " + s.minGroupSize + ", " + s.maxGroupSize);
					}
				}
			}
		}

		if (p != System.out) {
			p.close();
		}
	}

}
