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

import com.mactso.spawnbalanceutility.Main;
import com.mactso.spawnbalanceutility.config.MyConfig;
import com.mactso.spawnbalanceutility.manager.BiomeCreatureManager;
import com.mactso.spawnbalanceutility.manager.MobMassAdditionManager;
import com.mactso.spawnbalanceutility.manager.BiomeCreatureManager.BiomeCreatureItem;
import com.mactso.spawnbalanceutility.manager.MobMassAdditionManager.MassAdditionMobItem;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraftforge.coremod.api.ASMAPI;
import net.minecraftforge.event.server.ServerStartingEvent;

public class SpawnBiomeData {
//	private static Field fieldBiomeCategory = null;
	private static final Logger LOGGER = LogManager.getLogger();

	static int reportlinenumber = 0;
	static int biomeEventNumber = 0;

	static Set<String> biomesProcessed = new HashSet<>();

	static {
		initReports();
//		try {dp
//			String name = ASMAPI.mapField("f_47442_");
//			fieldBiomeCategory = Biome.class.getDeclaredField(name);
//			fieldBiomeCategory.setAccessible(true);
//		} catch (Exception e) {
//			LOGGER.error("XXX Unexpected Reflection Failure trying set Biome.biomeCategory accessible");
//		}		
	}

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

		File fpm = new File("config/spawnbalanceutility/PsuedoMobs.rpt");
		if (!(fpm.exists()))
			generatePsuedoMobStubReport();

	}

	public static void balanceBiomeSpawnValues(MinecraftServer server) {

		RegistryAccess dynreg = server.registryAccess();
		Registry<Biome> biomeRegistry = dynreg.registryOrThrow(Registries.BIOME);
		Field field = null;
		// get net/minecraft/world/level/biome/MobSpawnSettings/f_48329_
		// net/minecraft/world/level/biome/MobSpawnSettings/spawners
		try {
			String name = ASMAPI.mapField("f_48329_");
			field = MobSpawnSettings.class.getDeclaredField(name);
			field.setAccessible(true);
		} catch (Exception e) {
			LOGGER.error("XXX Unexpected Reflection Failure balanceBiomeSpawnValues");
			return;
		}

		int usedTotal = 0;
		String vCl = "";

		for (Biome b : biomeRegistry) {
			String bn = biomeRegistry.getKey(b).toString();
			Optional<Holder.Reference<Biome>> oBH = biomeRegistry.getHolder(biomeRegistry.getId(b));
			if (!oBH.isPresent()) {
				continue;
			}
			String bcName = Utility.getMyBC(oBH.get());

			List<BiomeCreatureItem> modBiomeMobSpawners = BiomeCreatureManager.biomeCreaturesMap.get(bn);
			if (modBiomeMobSpawners == null) {
				LOGGER.warn("XXX Biome (" + bn + ") has no valid mobs.");
				modBiomeMobSpawners = new ArrayList<>();
				continue;
			}

			MobSpawnSettings msi = b.getMobSettings();

			Map<MobCategory, WeightedRandomList<SpawnerData>> newMap = new HashMap<>();
			int used = 0;

			for (MobCategory v : MobCategory.values()) {
				List<SpawnerData> newFixedList = new ArrayList<>();
				vCl = v.toString();
				for (BiomeCreatureItem biomeCreatureItem : modBiomeMobSpawners) {
					if (biomeCreatureItem.getClassification().equalsIgnoreCase(vCl)) {

						@SuppressWarnings("deprecation")
						Optional<EntityType<?>> opt = BuiltInRegistries.ENTITY_TYPE
								.getOptional(new ResourceLocation(biomeCreatureItem.getModAndMob()));
						int i = 3;
						if (opt.isPresent()) {
							if (opt.get().getCategory() == MobCategory.MISC) {
								Utility.debugMsg(0, Main.MODID + " : " + biomeCreatureItem.getModAndMob()
										+ " is MISC, minecraft is hard coded to change it to minecraft:pig in spawning data.");
							} else if (opt.get().getCategory() != v) {
								if (biomeCreatureItem.getModAndMob().equals("minecraft:ocelot")) {

								} else {
									Utility.debugMsg(0,
											Main.MODID + " : " + biomeCreatureItem.getModAndMob() + " Error, mob type "
													+ v + " different than defined for the type of mob "
													+ opt.get().getCategory());
								}
							}
							SpawnerData newSpawner = new SpawnerData(opt.get(),
									Weight.of(biomeCreatureItem.getSpawnWeight()), biomeCreatureItem.getMinCount(),
									biomeCreatureItem.getMaxCount());
							newFixedList.add(newSpawner);
						} else {
							Utility.debugMsg(0, reportlinenumber + "SpawnBalanceUtility ERROR: Mob "
									+ biomeCreatureItem.getModAndMob() + " not in Entity Type Registry");
						}
					}
				}
				newMap.put(v, WeightedRandomList.create(newFixedList));
				used += newFixedList.size();
			}
			try {
				@SuppressWarnings("unchecked")
				Map<MobCategory, WeightedRandomList<SpawnerData>> oldMap = (Map<MobCategory, WeightedRandomList<SpawnerData>>) field.get(msi);
				field.set(msi, newMap);
				usedTotal += used;
				Summary.biomeUpdate(oldMap, newMap);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		Summary.setBiomeUsed(usedTotal);
	}

	@SuppressWarnings("unchecked")
	public static void fixBiomeSpawnValues(MinecraftServer server) {

		LOGGER.warn(" SpawnBalanceUtility: Fixing biome extreme spawn values. ");

		RegistryAccess dynreg = server.registryAccess();
		Registry<Biome> biomeRegistry = dynreg.registryOrThrow(Registries.BIOME);

		Field field = null;
		try {
			String name = ASMAPI.mapField("f_48329_");
			field = MobSpawnSettings.class.getDeclaredField(name);
			field.setAccessible(true);
		} catch (Exception e) {
			LOGGER.error("XXX Unexpected Reflection Failure lateBalanceBiomeSpawnValues");
			return;
		}

		int fixCount = 0;
		int netherCount = 0;
		int biomeTotal = 0;
		Set<EntityType<?>> usedTotalSet = new HashSet<>();

		for (Biome b : biomeRegistry) {

			String bn = biomeRegistry.getKey(b).toString();
			LOGGER.warn("SBU Biomes: " + bn);
			Optional<Holder.Reference<Biome>> oBH = biomeRegistry.getHolder(biomeRegistry.getId(b));
			if (!oBH.isPresent()) {
				continue;
			}
			String bcName = Utility.getMyBC(oBH.get());

			MobSpawnSettings msi = b.getMobSettings();
			Map<MobCategory, WeightedRandomList<SpawnerData>> map = null;
			try {
				map = (Map<MobCategory, WeightedRandomList<SpawnerData>>) field.get(msi);
			} catch (Exception e) {
				Utility.debugMsg(0, Main.MODID + " XXX Unexpected Reflection Failure getting map");
				return;
			}

			Map<MobCategory, WeightedRandomList<SpawnerData>> newMap = new HashMap<>();
//			boolean classificationMonster = false;
			boolean zombifiedPiglinSpawner = false;
			boolean ghastSpawner = false;
			List<EntityType<?>> usedList = new ArrayList<>();

			// given- we have the biome name- the category name.

			for (MobCategory mc : MobCategory.values()) {

				// TODO Hard Exception Here.
				WeightedRandomList<SpawnerData> originalSpawnerList = map.get(mc);

				// and here we have the classification
				// looks like the mob name can't be part of the key however.
				// the hashtable.elements() may give an enumeration from a biome.

				List<SpawnerData> newFixedList = new ArrayList<>();
				for (SpawnerData s : originalSpawnerList.unwrap()) {

					int oldSpawnWeight = s.getWeight().asInt();
					int newSpawnWeight = oldSpawnWeight;
					if (newSpawnWeight > 0) {
						newSpawnWeight = Math.max(MyConfig.getMinSpawnWeight(), newSpawnWeight);
						newSpawnWeight = Math.min(MyConfig.getMaxSpawnWeight(), newSpawnWeight);	
					}

					Utility.debugMsg(2, Main.MODID + ":" + s.type.getDescriptionId() + " minspawn change from "
								+ s.getWeight().asInt() + " to " + newSpawnWeight);

					String key = EntityType.getKey(s.type).toString();
					int dSW = MyConfig.getDefaultSpawnWeight(key);
					if (dSW != MyConfig.NO_DEFAULT_SPAWN_WEIGHT_FOUND) {
						if (newSpawnWeight == 0) {
							LOGGER.warn("WARN Setting " + key + " to non-zero spawnweight value may cause runaway spawning.");
						}
						newSpawnWeight = dSW;
					}

					if (newSpawnWeight != oldSpawnWeight)
						fixCount++;

					SpawnerData newS = new SpawnerData(s.type, Weight.of(newSpawnWeight), s.minCount, s.maxCount);
					newFixedList.add(newS);
					
					if (Utility.getMyBC(oBH.get()) == Utility.NETHER) {
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
							SpawnerData newS = new SpawnerData(et, Weight.of(ma.getSpawnWeight()), ma.getMinCount(),
									ma.getMaxCount());
							newFixedList.add(newS);
							usedList.add(et);
						}
					}

				}

				if (Utility.getMyBC(oBH.get()) == Utility.NETHER) {
					if (mc == MobCategory.MONSTER) {
						if ((zombifiedPiglinSpawner == false) && (MyConfig.isFixEmptyNether())) {
							SpawnerData newS = new SpawnerData(EntityType.ZOMBIFIED_PIGLIN,
									Weight.of(MyConfig.getMinSpawnWeight()), 1, 4);
							newFixedList.add(newS);
							netherCount++;
						}

						if ((ghastSpawner == false) && (MyConfig.isFixEmptyNether())) {
							SpawnerData newS = new SpawnerData(EntityType.GHAST,
									Weight.of((int) (MyConfig.getMinSpawnWeight() * 0.75f)), 4, 4);
							newFixedList.add(newS);
							netherCount++;
						}
					}
				}

				newMap.put(mc, WeightedRandomList.create(newFixedList));
			}

			try {
				field.set(msi, newMap);
				usedTotalSet.addAll(usedList);
				biomeTotal += usedList.size();
			} catch (Exception e) {
				// this catch block was Auto-generated  
				e.printStackTrace();
			}
		}
		Summary.setMassAddUsed(usedTotalSet.size(), biomeTotal);
		Summary.setBiomeFix(fixCount, netherCount);
	}

	private static void generateMassAdditionMobsStubReport() {

		PrintStream p = null;
		try {
			p = new PrintStream(new FileOutputStream("config/spawnbalanceutility/MassAdditionMobs.rpt", true));
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
		p.println("* Parm Weight     : a number 1 or higher.  1 is superrare, 5 is rare, 20 is uncommon, 80 is common.");
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

	private static void generatePsuedoMobStubReport() {

		PrintStream p = null;
		try {
			p = new PrintStream(new FileOutputStream("config/spawnbalanceutility/PsuedoMobs.rpt", true));
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (p == null) {
			p = System.out;
		}

		p.println(
				"* Example PsuedoMob file.  Add mobs with the pattern below and rename file to MassAdditionMobs.csv");
		p.println("* Mobs in this file will generate in any specified biome despite their own spawning rules.");
		p.println("* For now, Psuedo Mobs of one type, won't respawn until the prior one dies or despawns.");
		p.println("* SBU will read this file ONLY if it is renamed PsuedoMobs.csv.");
		p.println("*");
		p.println("* NOTICE: This file has a unique format differs from BiomeMobWeight file");
		p.println("* Line starting with '*' are comments and ignored. ");
		p.println("*");
		p.println("* Line, biome, mod:mob, psuedoWeight, mingroup , maxgroup");
		p.println("* 1, minecraft:plains, minecraft:husk, 80, 4, 4");
		p.println("* 2, minecraft:plains, minecraft:blaze, 80, 1, 1");
		p.println("* 4, minecraft:desert, minecraft:iron_golem, 80, 1, 1");
		p.println("* 5, minecraft:snowyplains, minecraft:snow_golem, 80, 1, 1");

		if (p != System.out) {
			p.close();
		}
	}

	public static void generateBiomeReport(ServerStartingEvent event) {

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
		p.println("* Lines starting with '*' are comments and ignored");
		p.println("* When this file is read, SBU writes summary information to the log file.");
		p.println("* ");

		int biomelineNumber = 0;
		MinecraftServer server = event.getServer();
		RegistryAccess dynreg = server.registryAccess();
		Registry<Biome> biomeRegistry = dynreg.registryOrThrow(Registries.BIOME);

		for (Biome b : biomeRegistry) {
			String bn = biomeRegistry.getKey(b).toString();
			Optional<Holder.Reference<Biome>> oBH = biomeRegistry.getHolder(biomeRegistry.getId(b));
			String cn = Utility.getMyBC(oBH.get());
			MobSpawnSettings msi = b.getMobSettings();
			for (MobCategory v : MobCategory.values()) {
				for (SpawnerData s : msi.getMobs(v).unwrap()) {
					if (MyConfig.isSuppressMinecraftMobReporting()) {
						if (EntityType.getKey(s.type).getNamespace().equals("minecraft")) {
							continue;
						}
					}
					String modname = EntityType.getKey(s.type).getNamespace();
					if (MyConfig.isIncludedMod(modname)) {
						p.println(++biomelineNumber + ", " + cn + ", " + bn + ", " + v + ", "
								+ EntityType.getKey(s.type).toString() + ", " + s.getWeight() + ", " + s.minCount + ", "
								+ s.maxCount);
					}
				}
			}
		}

		if (p != System.out) {
			p.close();
		}
	}

}
