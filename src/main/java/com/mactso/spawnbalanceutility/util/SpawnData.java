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

import com.mactso.spawnbalanceutility.config.MyConfig;
import com.mactso.spawnbalanceutility.util.BiomeCreatureManager.BiomeCreatureItem;
import com.mactso.spawnbalanceutility.util.MobMassAdditionManager.MassAdditionMobItem;
import com.mactso.spawnbalanceutility.util.StructureCreatureManager.StructureCreatureItem;

import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.MutableRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.MobSpawnInfo;
import net.minecraft.world.biome.MobSpawnInfo.Spawners;
import net.minecraftforge.coremod.api.ASMAPI;
import net.minecraftforge.event.world.StructureSpawnListGatherEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;

public class SpawnData {

	static {
		initReports();
	}

	static int biomelineNumber = 0;
	static int structureLineNumber = 0;
	static int reportlinenumber = 0;
	static int biomeEventNumber = 0;
	static int structureEventNumber = 0;
	static Set<String> biomesProcessed = new HashSet<>();
	static Set<String> structuresProcessed = new HashSet<>();

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

		DynamicRegistries dynreg = server.registryAccess();
		MutableRegistry<Biome> biomeRegistry =  dynreg.registryOrThrow(Registry.BIOME_REGISTRY);
		Field field = null;
		try {
			String name = ASMAPI.mapField("field_242554_e");
			field = MobSpawnInfo.class.getDeclaredField(name);
			field.setAccessible(true);
		} catch (Exception e) {
			System.out.println("XXX Unexpected Reflection Failure balanceBiomeSpawnValues");
			return;
		}

//		String bCl = "";
		String vCl = "";

		for (Biome b : biomeRegistry) {
			String bn = biomeRegistry.getKey(b).toString();

			List<BiomeCreatureItem> modBiomeMobSpawners = BiomeCreatureManager.biomeCreaturesMap.get(bn);
			if (modBiomeMobSpawners == null) {
				System.out.println("XXX Balance Biomes True but BiomeMobWeight.CSV missing, empty, or has no valid mobs.");
				continue;
			}

			MobSpawnInfo msi = b.getMobSettings ();
			
//			Map<EntityClassification, List<MobSpawnInfo.Spawners>> map = null;
//			try {
//				map = (Map<EntityClassification, List<MobSpawnInfo.Spawners>>) field.get(msi);
//			} catch (Exception e) {
//				System.out.println("XXX Unexpected Reflection Failure getting map");
//				return;
//			}

			Map<EntityClassification, List<MobSpawnInfo.Spawners>> newMap = new HashMap<>();

			for (EntityClassification v : EntityClassification.values()) {
				List<Spawners> newFixedList = new ArrayList<>();
				vCl = v.getSerializedName ();
				for (BiomeCreatureItem biomeCreatureItem : modBiomeMobSpawners) {
					if (biomeCreatureItem.classification.toLowerCase().equals(vCl)) {
						@SuppressWarnings("deprecation")
						Optional<EntityType<?>> opt = Registry.ENTITY_TYPE
								.getOptional(new ResourceLocation(biomeCreatureItem.modAndMob));
						if (opt.isPresent()) {
							Spawners newSpawner = new Spawners(opt.get(), biomeCreatureItem.spawnWeight,
									biomeCreatureItem.minCount, biomeCreatureItem.maxCount);
							newFixedList.add(newSpawner);
							if (MyConfig.getDebugLevel() > 0) {
								System.out.println("Biome :" + bn + " + r:" + reportlinenumber
										+ " SpawnBalanceUtility XXZZY: p.size() =" + modBiomeMobSpawners.size()
										+ " Mob " + biomeCreatureItem.modAndMob + " Added to "
										+ b.getBiomeCategory().getName());
							}

						} else {
							System.out.println(reportlinenumber + "SpawnBalanceUtility ERROR: Mob "
									+ biomeCreatureItem.modAndMob + " not in Entity Type Registry");
						}
					}
				}
				newMap.put(v, newFixedList);
			}
			try {
				field.set(msi, newMap);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	@SuppressWarnings("unchecked")
	public static void fixBiomeSpawnValues(MinecraftServer server) {

		DynamicRegistries dynreg = server.registryAccess();
		MutableRegistry<Biome> biomeRegistry = dynreg.registryOrThrow(Registry.BIOME_REGISTRY);
		Field field = null;
		try {
			String name = ASMAPI.mapField("field_242554_e");
			field = MobSpawnInfo.class.getDeclaredField(name);
			field.setAccessible(true);
		} catch (Exception e) {
			System.out.println("XXX Unexpected Reflection Failure lateBalanceBiomeSpawnValues");
			return;
		}

		for (Biome b : biomeRegistry) {

//			String bn = biomeRegistry.getKey(b).toString();

			MobSpawnInfo msi = b.getMobSettings ();
			Map<EntityClassification, List<MobSpawnInfo.Spawners>> map = null;
			try {
				map = (Map<EntityClassification, List<MobSpawnInfo.Spawners>>) field.get(msi);
			} catch (Exception e) {
				System.out.println("XXX Unexpected Reflection Failure getting map");
				return;
			}

			Map<EntityClassification, List<MobSpawnInfo.Spawners>> newMap = new HashMap<>();
//			boolean classificationMonster = false;
			boolean zombifiedPiglinSpawner = false;
			boolean ghastSpawner = false;

			// given- we have the biome name- the category name.

			for (EntityClassification v : EntityClassification.values()) {
				List<Spawners> originalSpawnerList = map.get(v);

				// and here we have the classification
				// looks like the mob name can't be part of the key however.
				// the hashtable.elements() may give an enumeration from a biome.

				List<Spawners> newFixedList = new ArrayList<>();
				for (Spawners s : originalSpawnerList) {

//					ResourceLocation modMob = s.type.getRegistryName();
//					String key = modMob.toString();
					int newSpawnWeight = s.weight;
					if (newSpawnWeight > MyConfig.getMaxSpawnWeight()) {
						newSpawnWeight = MyConfig.getMaxSpawnWeight();
					}
					if (newSpawnWeight < MyConfig.getMinSpawnWeight()) {
						newSpawnWeight = MyConfig.getMinSpawnWeight();
						System.out.println(s.type.getRegistryName() + " minspawn change from " + s.weight + " to "
								+ newSpawnWeight);
					}
					String key = s.type.getRegistryName().toString();
					int dSW = MyConfig.getDefaultSpawnWeight(key);
					if (dSW != MyConfig.NO_DEFAULT_SPAWN_WEIGHT_FOUND) {
						newSpawnWeight = dSW;
					}
					Spawners newS = new Spawners(s.type, newSpawnWeight, s.minCount, s.maxCount);
					newFixedList.add(newS);

					if (b.getBiomeCategory() == Biome.Category.NETHER) {
						if (s.type == EntityType.ZOMBIFIED_PIGLIN)
							zombifiedPiglinSpawner = true;
						if (s.type == EntityType.GHAST) {
							ghastSpawner = true;
						}
					}
				}

				List<MassAdditionMobItem> massAddMobs = MobMassAdditionManager.getFilteredList(v, b.getBiomeCategory());
				EntityType<?> et;
				for (MassAdditionMobItem ma : massAddMobs) {

					Optional<EntityType<?>> oe = EntityType.byString(ma.getModAndMob());
					if (oe.isPresent()) {
						et = oe.get();
						boolean mobFound = false;
						for (Spawners s : newFixedList) {
							if (s.type == et) {
								mobFound = true;
								break;
							}
						}
						if (mobFound == false) {
							Spawners newS = new Spawners(et, ma.spawnWeight, ma.minCount, ma.maxCount);
							newFixedList.add(newS);
						}
					}

				}

				if (b.getBiomeCategory() == Biome.Category.NETHER) {
					if (v == EntityClassification.MONSTER) {
						if ((zombifiedPiglinSpawner == false) && (MyConfig.isFixEmptyNether())) {
							Spawners newS = new Spawners(EntityType.ZOMBIFIED_PIGLIN, MyConfig.getMinSpawnWeight(), 1,
									4);
							newFixedList.add(newS);
						}

						if ((ghastSpawner == false) && (MyConfig.isFixEmptyNether())) {
							Spawners newS = new Spawners(EntityType.GHAST, (int) (MyConfig.getMinSpawnWeight() * 0.75f),
									4, 4);
							newFixedList.add(newS);
						}
					}
				}

				newMap.put(v, newFixedList);
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

		List<Spawners> newSpawnersList = new ArrayList<>();
		List<Spawners> theirSpawnersList = new ArrayList<>();

		if (p != null) {
			for (EntityClassification ec : EntityClassification.values()) {
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
							Spawners newSpawner = new Spawners(opt.get(), sci.spawnWeight, sci.minCount, sci.maxCount);
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
				for (Spawners s : event.getEntitySpawns(ec)) {
					theirSpawnersList.add(s);
				}
				for (Spawners s : theirSpawnersList) {
					event.removeEntitySpawn(ec, s);
				}
				event.addEntitySpawns(ec, newSpawnersList);
			}

		}

	}

	private static void fixStructureSpawnValues(StructureSpawnListGatherEvent event) {

		List<Spawners> newSpawnersList = new ArrayList<>();
		List<Spawners> theirSpawnersList = new ArrayList<>();

		for (EntityClassification ec : EntityClassification.values()) {
			newSpawnersList.clear();
			theirSpawnersList.clear();
			for (Spawners s : event.getEntitySpawns(ec)) {
				int newSpawnWeight = s.weight;
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
				Spawners newS = new Spawners(s.type, newSpawnWeight, s.minCount, s.maxCount);
				theirSpawnersList.add(s);
				newSpawnersList.add(newS);
			}
			for (Spawners s : theirSpawnersList) {
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

		List<Spawners> spawners = new ArrayList<>();
		p.println(++structureLineNumber + ", " + sn + ", HEADING, header:ignore, 0, 0, 0");

		for (EntityClassification ec : EntityClassification.values()) {
			spawners = event.getEntitySpawns(ec);
			for (Spawners s : spawners) {
				if (MyConfig.isSuppressMinecraftMobReporting()) {
					if (s.type.getRegistryName().getNamespace().equals("minecraft")) {
						continue;
					}
				}
				if (MyConfig.isIncludedMod(s.type.getRegistryName().getNamespace())) {
					p.println(++structureLineNumber + ", " + sn + ", " + ec + ", " + s.type.getRegistryName() + ", "
							+ s.weight + ", " + s.minCount + ", " + s.maxCount);
					if (MyConfig.debugLevel > 0) {
						System.out.println(++structureLineNumber + ", " + sn + ", " + s.type.getRegistryName() + ", "
								+ s.weight + ", " + s.minCount + ", " + s.maxCount);
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

		p.println(
				"Example mob mass addition file.  Add mobs with the pattern below and rename file to MassAdditionMobs.csv");
		p.println("Line, Category*, Class**, Namespace:Mob, Weight, Mingroup , Maxgroup");
		p.println("");
		p.println("1, A, MONSTER, minecraft:phantom, 10, 1, 4");
		p.println("");
		p.println("* A, O, N, E for All, Overworld, Nether, The End");
		p.println("** MONSTER,CREATURE,AMBIENT");
		if (p != System.out) {
			p.close();
		}
	}

	public static void generateBiomeReport(FMLServerStartingEvent event) {

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
		DynamicRegistries dynreg = server.registryAccess();
		MutableRegistry<Biome> biomeRegistry = dynreg.registryOrThrow(Registry.BIOME_REGISTRY);

		for (Biome b : biomeRegistry) {
			String cn = b.getBiomeCategory().getName();
			String bn = biomeRegistry.getKey(b).toString();
			MobSpawnInfo msi = b.getMobSettings();
			for (EntityClassification v : EntityClassification.values()) {

				for (Spawners s : msi.getMobs(v)) {
					if (MyConfig.isSuppressMinecraftMobReporting()) {
						if (s.type.getRegistryName().getNamespace().equals("minecraft")) {
							continue;
						}
					}
					String modname = s.type.getRegistryName().getNamespace();
					if (MyConfig.isIncludedMod(modname)) {
						p.println(++biomelineNumber + ", " + cn + ", " + bn + ", " + v + ", " + s.type.getRegistryName()
								+ ", " + s.weight + ", " + s.minCount + ", " + s.maxCount);
					}
				}
			}
		}

		if (p != System.out) {
			p.close();
		}
	}

}
