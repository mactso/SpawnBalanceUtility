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
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
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
		File fb = new File("config/spawnbalanceutility/BiomeMobWeight.txt");
		if (fb.exists())
			fb.delete();
		File fma = new File("config/spawnbalanceutility/MassAdditionMobs.txt");
		if (!(fma.exists()))
			generateMassAdditionMobsStubReport();
	}


	public static void doBiomeActions(MinecraftServer server) {


    	initReports();

		if (MyConfig.isBalanceStructureSpawnValues()) {
			balanceBiomeSpawnValues(server);
		}
		if (MyConfig.isFixSpawnValues()) {
			fixBiomeSpawnValues(server);
		}
		if (MyConfig.isGenerateReport()) {
			generateBiomeSpawnValuesReport(server);
		}
	}
	
	public static void balanceBiomeSpawnValues(MinecraftServer server) {

		DynamicRegistryManager dynreg = server.getRegistryManager();
		Registry<Biome> biomeRegistry = dynreg.getManaged(Registry.BIOME_KEY);
		Field fieldSpawners = null;
		// get net/minecraft/world/level/biome/MobSpawnSettings/field_26405_ net/minecraft/world/level/biome/MobSpawnSettings/spawners
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
			
			Optional<RegistryEntry<Biome>> oRE = biomeRegistry.getEntry(bk);
			if (!oRE.isPresent()) {
				continue;
			}

			String bn = biomeRegistry.getId(b).toString();
			List<BiomeCreatureItem> modBiomeMobSpawners = BiomeCreatureManager.biomeCreaturesMap.get(bn);
			if (modBiomeMobSpawners == null) {
				LOGGER.warn("No spawn values found for biome: "+ bn + " when balance flag is true.");
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

	public static void fixBiomeSpawnValues(MinecraftServer server) {

		LOGGER.warn(" SpawnBalanceUtility: Fixing biome extreme spawn values. ");

		DynamicRegistryManager dynreg = server.getRegistryManager();
		Registry<Biome> biomeRegistry = dynreg.getManaged(Registry.BIOME_KEY);

//		Biome r = null;
//		for (Biome b : biomeRegistry) {
//			r = b;
//			break;
//		}

//		Class obj = r.getClass();
//		System.out.println(obj.descriptorString());
//	      // using object of Class to
//	      // get all the declared methods of Dog
//	      Method[] methods = obj.getDeclaredMethods();
//
//	      // create an object of the Method class
//          System.out.println("Methods ----------------------------------");
//
//	      for (Method m : methods) {
//
//	        // get names of methods
//	        System.out.println("Method Name: " + m.getName());
//
//	        // get the access modifier of methods
//	        int modifier = m.getModifiers();
//	        System.out.println("Modifier: " + Modifier.toString(modifier));
//
//	        // get the return types of method
//	        System.out.println("Return Types: " + m.getReturnType());
//	        System.out.println(" ");
//	      }
//          System.out.println("Fields----------------------------------");
//
//	      Field[] fields = obj.getDeclaredFields();
//
//	      // create an object of the Method class
//	      for (Field f : fields) {
//
//	        // get names of methods
//	        System.out.println("Field Name: " + f.getName());
//
//	        // get the access modifier of methods
//	        int modifier = f.getModifiers();
//	        System.out.println("Modifier: " + Modifier.toString(modifier));
//	      }	      
//          System.out.println("----------------------------------");

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
			
			Optional<RegistryEntry<Biome>> oRE = biomeRegistry.getEntry(bk);
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
				Utility.debugMsg(1, "biome:" + b.REGISTRY_CODEC.simple() + ", " + b.toString());
				Pool<SpawnEntry> originalSpawnerEntryList = b.getSpawnSettings().getSpawnEntries(mc);
				for (SpawnEntry s : originalSpawnerEntryList.getEntries()) {

					int newSpawnWeight = s.getWeight().getValue();
					if (newSpawnWeight > MyConfig.getMaxSpawnWeight()) {
						newSpawnWeight = MyConfig.getMaxSpawnWeight();
					}
					if (newSpawnWeight < MyConfig.getMinSpawnWeight()) {
						newSpawnWeight = MyConfig.getMinSpawnWeight();
					}
					String key = s.type.toString();

					// FORGE			int dSW = MyConfig.getDefaultSpawnWeight(key);
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
	public static void generateBiomeSpawnValuesReport(MinecraftServer server) {

		DynamicRegistryManager dynreg = server.getRegistryManager();
		Registry<Biome> biomeRegistry = dynreg.getManaged(Registry.BIOME_KEY);
		
		PrintStream p = null;
		try {
			p = new PrintStream(new FileOutputStream("config/spawnbalanceutility/BiomeMobWeight.txt", false));
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (p == null) {
			p = System.out;
		}


		for (Biome b : biomeRegistry) {

			RegistryKey<Biome> bk = biomeRegistry.getKey(b).get();
			
			Optional<RegistryEntry<Biome>> oRE = biomeRegistry.getEntry(bk);
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
