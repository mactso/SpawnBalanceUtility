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
import com.mactso.spawnbalanceutility.util.BiomeCreatureManager.BiomeCreatureItem;
import com.mactso.spawnbalanceutility.util.MobMassAdditionManager.MassAdditionMobItem;
import com.mactso.spawnbalanceutility.util.StructureCreatureManager.StructureCreatureItem;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.BiomeCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraftforge.coremod.api.ASMAPI;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.world.StructureSpawnListGatherEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;



public class SpawnData {
	private static Field fieldBiomeCategory = null;
	private static final Logger LOGGER = LogManager.getLogger();
	static int biomelineNumber = 0;
	static int structureLineNumber = 0;
	static int reportlinenumber = 0;
	static int biomeEventNumber = 0;
	static int structureEventNumber = 0;
	static Set<String> biomesProcessed = new HashSet<>();
	static Set<String> structuresProcessed = new HashSet<>();

	static {
		initReports();
		try {
			String name = ASMAPI.mapField("f_47442_");
			fieldBiomeCategory = Biome.class.getDeclaredField(name);
			fieldBiomeCategory.setAccessible(true);
		} catch (Exception e) {
			LOGGER.error("XXX Unexpected Reflection Failure trying set Biome.biomeCategory accessible");
		}		
	}
	
	public static void initReports() {
		File fd = new File("config/spawnbalanceutility");
		if (!fd.exists())
			fd.mkdir();
		File fb = new File("config/spawnbalanceutility/BiomeMobWeight.txt");
		if (fb.exists())
			fb.delete();
		File fs = new File("config/spawnbalanceutility/StructMobWeight.txt");
		if (fs.exists())
			fs.delete();
		File fma = new File("config/spawnbalanceutility/MassAdditionMobs.txt");
		if (!(fma.exists()))
			generateMassAdditionMobsStubReport();
	}


	public static void balanceBiomeSpawnValues(MinecraftServer server) {

		RegistryAccess dynreg = server.registryAccess();

		Registry<Biome> biomeRegistry =  dynreg.registryOrThrow(Registry.BIOME_REGISTRY);
		Field field = null;
		// get net/minecraft/world/level/biome/MobSpawnSettings/f_48329_ net/minecraft/world/level/biome/MobSpawnSettings/spawners
		try {
			String name = ASMAPI.mapField("f_48329_");
			field = MobSpawnSettings.class.getDeclaredField(name);
			field.setAccessible(true);
		} catch (Exception e) {
			LOGGER.error("XXX Unexpected Reflection Failure balanceBiomeSpawnValues");
			return;
		}

		Field fieldBiomeCategory = null;
		try {
			String name = ASMAPI.mapField("f_47442_");
			fieldBiomeCategory = Biome.class.getDeclaredField(name);
			fieldBiomeCategory.setAccessible(true);
		} catch (Exception e) {
			LOGGER.error("XXX Unexpected Reflection Failure set Biome.biomeCategory accessible");
			return;
		}
		
//		String bCl = "";
		String vCl = "";

		for (Biome b : biomeRegistry) {
			String bn = biomeRegistry.getKey(b).toString();

			List<BiomeCreatureItem> modBiomeMobSpawners = BiomeCreatureManager.biomeCreaturesMap.get(bn);
			if (modBiomeMobSpawners == null) {
				LOGGER.warn("XXX Balance Biomes True but BiomeMobWeight.CSV missing, empty, or has no valid mobs.");
				modBiomeMobSpawners = new ArrayList<>();
				continue;
			}

			MobSpawnSettings msi = b.getMobSettings ();

			Map<MobCategory, WeightedRandomList<SpawnerData>> newMap = new HashMap<>();

			for (MobCategory v : MobCategory.values()) {
				List<SpawnerData> newFixedList = new ArrayList<>();
				vCl = v.getSerializedName ();
				for (BiomeCreatureItem biomeCreatureItem : modBiomeMobSpawners) {
					if (biomeCreatureItem.classification.toLowerCase().equals(vCl)) {
						@SuppressWarnings("deprecation")
						Optional<EntityType<?>> opt = Registry.ENTITY_TYPE
								.getOptional(new ResourceLocation(biomeCreatureItem.modAndMob));
						if (opt.isPresent()) {
							SpawnerData newSpawner = new SpawnerData(opt.get(), Weight.of(biomeCreatureItem.spawnWeight),
									biomeCreatureItem.minCount, biomeCreatureItem.maxCount);
							newFixedList.add(newSpawner);
							if (MyConfig.getDebugLevel() > 0) {
								BiomeCategory bc = getBiomeCategory(b);
								System.out.println("Biome :" + bn + " + r:" + reportlinenumber
										+ " SpawnBalanceUtility XXZZY: p.size() =" + modBiomeMobSpawners.size()
										+ " Mob " + biomeCreatureItem.modAndMob + " Added to "
										+ bc.getName());
							}

						} else {
							System.out.println(reportlinenumber + "SpawnBalanceUtility ERROR: Mob "
									+ biomeCreatureItem.modAndMob + " not in Entity Type Registry");
						}
					}
				}
				newMap.put(v, WeightedRandomList.create(newFixedList));
			}
			try {
				field.set(msi, newMap);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}


	private static BiomeCategory getBiomeCategory(Biome b) {
		BiomeCategory bc = BiomeCategory.PLAINS;
		try {
			bc = (BiomeCategory) fieldBiomeCategory.get(b);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return bc;
	}

	@SuppressWarnings("unchecked")
	public static void fixBiomeSpawnValues(MinecraftServer server) {

		RegistryAccess dynreg = server.registryAccess();
		Registry<Biome> biomeRegistry = dynreg.registryOrThrow(Registry.BIOME_REGISTRY);
		Field field = null;
		try {
			String name = ASMAPI.mapField("f_48329_");
			field = MobSpawnSettings.class.getDeclaredField(name);
			field.setAccessible(true);
		} catch (Exception e) {
			LOGGER.error("XXX Unexpected Reflection Failure lateBalanceBiomeSpawnValues");
			return;
		}

		for (Biome b : biomeRegistry) {

//			String bn = biomeRegistry.getKey(b).toString();

			MobSpawnSettings msi = b.getMobSettings ();
			Map<MobCategory, WeightedRandomList<SpawnerData>> map = null;
			try {
				map = (Map<MobCategory, WeightedRandomList<SpawnerData>>) field.get(msi);
			} catch (Exception e) {
				System.out.println("XXX Unexpected Reflection Failure getting map");
				return;
			}

			Map<MobCategory, WeightedRandomList<SpawnerData>> newMap = new HashMap<>();
//			boolean classificationMonster = false;
			boolean zombifiedPiglinSpawner = false;
			boolean ghastSpawner = false;

			// given- we have the biome name- the category name.

			for (MobCategory v : MobCategory.values()) {
				
				// TODO Hard Exception Here.
				// TODO remove print statement.
				System.out.println ("biome:" + b.getRegistryName() + ", " + b.toString());
				WeightedRandomList<SpawnerData> originalSpawnerList = map.get(v);

				// and here we have the classification
				// looks like the mob name can't be part of the key however.
				// the hashtable.elements() may give an enumeration from a biome.

				List<SpawnerData> newFixedList = new ArrayList<>();
				for (SpawnerData s : originalSpawnerList.unwrap()) {

//					ResourceLocation modMob = s.type.getRegistryName();
//					String key = modMob.toString();

					int newSpawnWeight = s.getWeight().asInt();
					if (newSpawnWeight > MyConfig.getMaxSpawnWeight()) {
						newSpawnWeight = MyConfig.getMaxSpawnWeight();
					}
					if (newSpawnWeight < MyConfig.getMinSpawnWeight()) {
						newSpawnWeight = MyConfig.getMinSpawnWeight();
						System.out.println(s.type.getRegistryName() + " minspawn change from " + s.getWeight().asInt() + " to "
								+ newSpawnWeight);
					}
					String key = s.type.getRegistryName().toString();
					int dSW = MyConfig.getDefaultSpawnWeight(key);
					if (dSW != MyConfig.NO_DEFAULT_SPAWN_WEIGHT_FOUND) {
						newSpawnWeight = dSW;
					}

					SpawnerData newS = new SpawnerData(s.type, Weight.of(newSpawnWeight), s.minCount, s.maxCount);
					newFixedList.add(newS);

					if (getBiomeCategory(b) == Biome.BiomeCategory.NETHER) {
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

					Optional<EntityType<?>> oe = EntityType.byString(ma.getModAndMob());
					if (oe.isPresent()) {
						et = oe.get();
						boolean mobFound = false;
						for (SpawnerData s : newFixedList) {
							if (s.type == et) {
								mobFound = true;
								break;
							}
						}
						if (mobFound == false) {
							SpawnerData newS = new SpawnerData(et, Weight.of(ma.spawnWeight), ma.minCount, ma.maxCount);
							newFixedList.add(newS);
						}
					}

				}

				if (getBiomeCategory(b) == Biome.BiomeCategory.NETHER) {
					if (v == MobCategory.MONSTER) {
						if ((zombifiedPiglinSpawner == false) && (MyConfig.isFixEmptyNether())) {
							SpawnerData newS = new SpawnerData(EntityType.ZOMBIFIED_PIGLIN, Weight.of(MyConfig.getMinSpawnWeight()), 1,
									4);
							newFixedList.add(newS);
						}

						if ((ghastSpawner == false) && (MyConfig.isFixEmptyNether())) {
							SpawnerData newS = new SpawnerData(EntityType.GHAST, Weight.of((int) (MyConfig.getMinSpawnWeight() * 0.75f)),
									4, 4);
							newFixedList.add(newS);
						}
					}
				}

				newMap.put(v, WeightedRandomList.create( newFixedList) );
			}

			try {
				field.set(msi, newMap);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onStructure(StructureSpawnListGatherEvent event) {

		String threadname = Thread.currentThread().getName();
//		String structurename = event.getStructure().getStructureName();

//		if (MyConfig.isBalanceStructureSpawnValues()) {
//			balanceStructureSpawnValues(event);
//			if (MyConfig.getDebugLevel() > 0) {
//				System.out.println("SpawnBalanceUtility: Balancing "+ structurename +" with StructMobWeight.CSV Spawn weight values. ");
//			}
//		}

		if (MyConfig.isBalanceStructureSpawnValues()) {
			balanceStructureSpawnValues(event);
		}

		if (MyConfig.isFixSpawnValues()) {
			fixStructureSpawnValues(event);
		}

		if (MyConfig.isGenerateReport()) {
			if (threadname.equals("Render thread")) {
				return;
			}
			generateStructureSpawnValuesReport(event);
		}

	}

	private static void balanceStructureSpawnValues(StructureSpawnListGatherEvent event) {

//		String bCl = "";
		String vCl = "";

		structureEventNumber++;

		String key = event.getStructure().getRegistryName().toString();
		List<StructureCreatureItem> p = StructureCreatureManager.structureCreaturesMap.get(key);

		List<SpawnerData> newSpawnersList = new ArrayList<>();
		List<SpawnerData> theirSpawnersList = new ArrayList<>();

		if (p != null) {
			for (MobCategory ec : MobCategory.values()) {
				vCl = ec.getSerializedName ();
				newSpawnersList.clear();
				theirSpawnersList.clear();
				for (int i = 0; i < p.size(); i++) {
					StructureCreatureItem sci = p.get(i);
//					bCl = sci.classification;
					if (sci.classification.toLowerCase().equals(vCl)) {
						@SuppressWarnings("deprecation")
						Optional<EntityType<?>> opt = Registry.ENTITY_TYPE
								.getOptional(new ResourceLocation(sci.modAndMob));
						if (opt.isPresent()) {
							SpawnerData newSpawner = new SpawnerData(opt.get(), Weight.of(sci.spawnWeight), sci.minCount, sci.maxCount);
							newSpawnersList.add(newSpawner);

							if (MyConfig.getDebugLevel() > 0) {
								System.out.println("Structure :" + key + " + rl#:" + structureLineNumber
										+ " SpawnBalanceUtility XXZZY: p.size() =" + p.size() + " Mob " + sci.modAndMob
										+ " Added to " + key + ".");
							}

						} else {
							System.out.println(reportlinenumber + "SpawnBalanceUtility ERROR: Mob " + sci.modAndMob
									+ " not in Entity Type Registry");
						}
					}
				}
				for (SpawnerData s : event.getEntitySpawns(ec)) {
					theirSpawnersList.add(s);
				}
				for (SpawnerData s : theirSpawnersList) {
					event.removeEntitySpawn(ec, s);
				}
				event.addEntitySpawns(ec, newSpawnersList);
			}

		}

	}

	private static void fixStructureSpawnValues(StructureSpawnListGatherEvent event) {

		List<SpawnerData> newSpawnersList = new ArrayList<>();
		List<SpawnerData> theirSpawnersList = new ArrayList<>();

		for (MobCategory ec : MobCategory.values()) {
			newSpawnersList.clear();
			theirSpawnersList.clear();
			for (SpawnerData s : event.getEntitySpawns(ec)) {
				int newSpawnWeight = s.getWeight().asInt();
				if (newSpawnWeight < MyConfig.getMinSpawnWeight()) {
					if ((newSpawnWeight > 1) && (newSpawnWeight * 10 < MyConfig.getMaxSpawnWeight())) {
						newSpawnWeight = newSpawnWeight * 10;
					} else {
						newSpawnWeight = MyConfig.getMinSpawnWeight();
					}
				}
				if (newSpawnWeight > MyConfig.getMaxSpawnWeight()) {
					newSpawnWeight = MyConfig.getMaxSpawnWeight();
				}
				SpawnerData newS = new SpawnerData(s.type, Weight.of(newSpawnWeight), s.minCount, s.maxCount);
				theirSpawnersList.add(s);
				newSpawnersList.add(newS);
			}
			for (SpawnerData s : theirSpawnersList) {
				event.removeEntitySpawn(ec, s);
			}
			event.addEntitySpawns(ec, newSpawnersList);

		}

	}

	private static void generateStructureSpawnValuesReport(StructureSpawnListGatherEvent event) {

		String sn = event.getStructure().getRegistryName().toString();
		synchronized (structuresProcessed) {
			if (structuresProcessed.contains(sn)) {
				return;
			}
			structuresProcessed.add(sn);
		}

		PrintStream p = null;
		try {
			p = new PrintStream(new FileOutputStream("config/spawnbalanceutility/StructMobWeight.txt", true));
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (p == null) {
			p = System.out;
		}

		List<SpawnerData> spawners = new ArrayList<>();
		p.println(++structureLineNumber + ", " + sn + ", HEADING, header:ignore, 0, 0, 0");

		for (MobCategory ec : MobCategory.values()) {
			spawners = event.getEntitySpawns(ec);
			for (SpawnerData s : spawners) {
				if (MyConfig.isSuppressMinecraftMobReporting()) {
					if (s.type.getRegistryName().getNamespace().equals("minecraft")) {
						continue;
					}
				}
				if (MyConfig.isIncludedMod(s.type.getRegistryName().getNamespace())) {
					p.println(++structureLineNumber + ", " + sn + ", " + ec + ", " + s.type.getRegistryName() + ", "
							+ s.getWeight().asInt() + ", " + s.minCount + ", " + s.maxCount);
					if (MyConfig.debugLevel > 0) {
						System.out.println(++structureLineNumber + ", " + sn + ", " + s.type.getRegistryName() + ", "
								+ s.getWeight().asInt() + ", " + s.minCount + ", " + s.maxCount);
					}
				}
			}
		}

		if (p != System.out) {
			p.close();
		}
	}

	private static void generateMassAdditionMobsStubReport() {

		PrintStream p = null;
		try {
			p = new PrintStream(new FileOutputStream("config/spawnbalanceutility/MassAdditionMobs.txt", true));
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (p == null) {
			p = System.out;
		}

		p.println("* Example mob mass addition file.  Add mobs with the pattern below and rename file to MassAdditionMobs.csv");
		p.println("* Line, Dimension , Class**, Namespace:Mob, Weight, Mingroup , Maxgroup");
		p.println("*");
		p.println("* Example... 1, A, MONSTER, minecraft:phantom, 10, 1, 4");
		p.println("*");
		p.println("* Parm Dimension  : A, O, N, E for All, Overworld, Nether, The End");
		p.println("* Parm Class      : MONSTER, CREATURE, AMBIENT, UNDERWATER, etc.");
		p.println("* Parm Resource   : modname:mobname");
		p.println("* Parm Weight     : a number 1 or higher.  1 is superrare, 5 is rare, 20 is uncommon, 80 is common.");
		p.println("* Parm MinGroup   : a number 1 and less than MaxGroup");
		p.println("* Parm MaxGroup   : a number higher than MinGroup and usually 5 or less.");
		p.println("*");
		if (p != System.out) {
			p.close();
		}
	}

	public static void generateBiomeReport(ServerStartingEvent event) {

		PrintStream p = null;
		try {
			p = new PrintStream(new FileOutputStream("config/spawnbalanceutility/BiomeMobWeight.txt", true));
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (p == null) {
			p = System.out;
		}

		MinecraftServer server = event.getServer();
		RegistryAccess dynreg = server.registryAccess();
		Registry<Biome> biomeRegistry = dynreg.registryOrThrow(Registry.BIOME_REGISTRY);

		for (Biome b : biomeRegistry) {
			String cn = getBiomeCategory(b).getName();
			String bn = biomeRegistry.getKey(b).toString();
			MobSpawnSettings msi = b.getMobSettings();
			for (MobCategory v : MobCategory.values()) {

				for (SpawnerData s : msi.getMobs(v).unwrap()) {
					if (MyConfig.isSuppressMinecraftMobReporting()) {
						if (s.type.getRegistryName().getNamespace().equals("minecraft")) {
							continue;
						}
					}
					String modname = s.type.getRegistryName().getNamespace();
					if (MyConfig.isIncludedMod(modname)) {
						p.println(++biomelineNumber + ", " + cn + ", " + bn + ", " + v + ", " + s.type.getRegistryName()
								+ ", " + s.getWeight() + ", " + s.minCount + ", " + s.maxCount);
					}
				}
			}
		}

		if (p != System.out) {
			p.close();
		}
	}

}
