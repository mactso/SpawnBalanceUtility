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
import com.mactso.spawnbalanceutility.manager.BiomeCreatureManager.BiomeCreatureItem;
import com.mactso.spawnbalanceutility.manager.MobMassAdditionManager;
import com.mactso.spawnbalanceutility.manager.MobMassAdditionManager.MassAdditionMobItem;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraftforge.event.server.ServerStartingEvent;

public class SpawnBiomeData {
//	private static Field fieldBiomeCategory = null;
	private static final Logger LOGGER = LogManager.getLogger();

	static int reportlinenumber = 0;
	static int biomeEventNumber = 0;

	static Set<String> biomesProcessed = new HashSet<>();

	static {
		initReports();
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
		
		Registry<Biome> biomeRegistry = dynreg.lookupOrThrow(Registries.BIOME);
		Field field = null;
		// get net/minecraft/world/level/biome/MobSpawnSettings/f_48329_
		// net/minecraft/world/level/biome/MobSpawnSettings/spawners
		try {
			field = MobSpawnSettings.class.getDeclaredField("spawners");
			field.setAccessible(true);
		} catch (Exception e) {
			LOGGER.error("XXX Unexpected Reflection Failure balanceBiomeSpawnValues");
			return;
		}

		int usedTotal = 0;
		String vCl = "";

		for (Biome b : biomeRegistry) {
			String bn = biomeRegistry.getKey(b).toString();
			Holder<Biome> biomeHolder = biomeRegistry.wrapAsHolder(b);
			String bcName = Utility.getMyBC(biomeHolder);

			List<BiomeCreatureItem> modBiomeMobSpawners = BiomeCreatureManager.biomeCreaturesMap.get(bn);
			if (modBiomeMobSpawners == null) {
				LOGGER.warn("XXX Biome (" + bn + ") has no valid mobs.");
				modBiomeMobSpawners = new ArrayList<>();
				continue;
			}

			MobSpawnSettings mobSpawnSettings = b.getMobSettings();

			Map<MobCategory, WeightedList<SpawnerData>> newMap = new HashMap<>();
			int used = 0;

			for (MobCategory v : MobCategory.values()) {
				List<Weighted<SpawnerData>> newFixedList = new ArrayList<>();
				vCl = v.toString();
				for (BiomeCreatureItem biomeCreatureItem : modBiomeMobSpawners) {
					if (biomeCreatureItem.getClassification().equalsIgnoreCase(vCl)) {


						Optional<EntityType<?>> opt = BuiltInRegistries.ENTITY_TYPE
								.getOptional(ResourceLocation.parse((biomeCreatureItem.getModAndMob())));
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
							SpawnerData newSpawner = new SpawnerData(opt.get(), biomeCreatureItem.getMinCount(),
									biomeCreatureItem.getMaxCount());
							newFixedList.add(new Weighted<>(newSpawner, biomeCreatureItem.getSpawnWeight()));
						} else {
							Utility.debugMsg(0, reportlinenumber + "SpawnBalanceUtility ERROR: Mob "
									+ biomeCreatureItem.getModAndMob() + " not in Entity Type Registry");
						}
					}
				}
				newMap.put(v, WeightedList.of(newFixedList));
				used += newFixedList.size();
			}
			try {
				@SuppressWarnings("unchecked")
				Map<MobCategory, WeightedList<SpawnerData>> oldMap = (Map<MobCategory, WeightedList<SpawnerData>>) field.get(mobSpawnSettings);
				field.set(mobSpawnSettings, newMap);
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
		Registry<Biome> biomeRegistry = dynreg.lookupOrThrow(Registries.BIOME);

		Field field = null;
		try {
			field = MobSpawnSettings.class.getDeclaredField("spawners");
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
			Holder<Biome> biomeHolder = biomeRegistry.wrapAsHolder(b);
			String bcName = Utility.getMyBC(biomeHolder);

			MobSpawnSettings msi = b.getMobSettings();
			Map<MobCategory, WeightedList<SpawnerData>> map = null;
			try {
				map = (Map<MobCategory, WeightedList<SpawnerData>>) field.get(msi);
			} catch (Exception e) {
				Utility.debugMsg(0, Main.MODID + " XXX Unexpected Reflection Failure getting map");
				return;
			}

			Map<MobCategory, WeightedList<SpawnerData>> newMap = new HashMap<>();
//			boolean classificationMonster = false;
			boolean zombifiedPiglinSpawner = false;
			boolean ghastSpawner = false;
			List<EntityType<?>> usedList = new ArrayList<>();

			// given- we have the biome name- the category name.

			for (MobCategory mc : MobCategory.values()) {

				// TODO Hard Exception Here.
				WeightedList<SpawnerData> orgWListSpawners = map.get(mc);

				// and here we have the classification
				// looks like the mob name can't be part of the key however.
				// the hashtable.elements() may give an enumeration from a biome.
				List<Weighted<SpawnerData>> spawnItems = orgWListSpawners.unwrap();
				List<Weighted<SpawnerData>> newFixedList = new ArrayList<>();
				
				for (Weighted<SpawnerData> weightedEntry : orgWListSpawners.unwrap()) {
					
					int oldSpawnWeight = weightedEntry.weight();
					SpawnerData s = weightedEntry.value();
					int newSpawnWeight = oldSpawnWeight;
					if (newSpawnWeight > 0) {
						newSpawnWeight = Math.max(MyConfig.getMinSpawnWeight(), newSpawnWeight);
						newSpawnWeight = Math.min(MyConfig.getMaxSpawnWeight(), newSpawnWeight);	
					}

					Utility.debugMsg(2, Main.MODID + ":" + s.type().getDescriptionId() + " minspawn change from "
								+ weightedEntry.weight() + " to " + newSpawnWeight);

					String key = EntityType.getKey(s.type()).toString();
					int dSW = MyConfig.getDefaultSpawnWeight(key);
					if (dSW != MyConfig.NO_DEFAULT_SPAWN_WEIGHT_FOUND) {
						if (newSpawnWeight == 0) {
							LOGGER.warn("WARN Setting " + key + " to non-zero spawnweight value may cause runaway spawning.");
						}
						newSpawnWeight = dSW;
					}

					if (newSpawnWeight != oldSpawnWeight)
						fixCount++;

					
					SpawnerData newS = new SpawnerData(s.type(), s.minCount(), s.maxCount());

					
					newFixedList.add(new Weighted<>(newS, newSpawnWeight));
					
					if (Utility.getMyBC(biomeHolder) == Utility.NETHER) {
						if (s.type() == EntityType.ZOMBIFIED_PIGLIN)
							zombifiedPiglinSpawner = true;
						if (s.type() == EntityType.GHAST) {
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
						for (Weighted<SpawnerData> s : newFixedList) {
							if (s.value().type() == et) {
								mobFound = true;
								break;
							}
						}
						if (mobFound == false) {
							SpawnerData newS = new SpawnerData(et,  ma.getMinCount(), ma.getMaxCount());
							newFixedList.add(new Weighted<>(newS, ma.getSpawnWeight()));
							usedList.add(et);
						}
					}

				}

				if (Utility.getMyBC(biomeHolder) == Utility.NETHER) {
					if (mc == MobCategory.MONSTER) {
						if ((zombifiedPiglinSpawner == false) && (MyConfig.isFixEmptyNether())) {
							SpawnerData newS = new SpawnerData(EntityType.ZOMBIFIED_PIGLIN,  1, 4);
							newFixedList.add(new Weighted<>(newS, MyConfig.getMinSpawnWeight()));
							netherCount++;
						}

						if ((ghastSpawner == false) && (MyConfig.isFixEmptyNether())) {
							SpawnerData newS = new SpawnerData(EntityType.GHAST,  4, 4);
							newFixedList.add(new Weighted<>(newS, ((int) (MyConfig.getMinSpawnWeight() * 0.75f))));						
							netherCount++;
						}
					}
				}

				newMap.put(mc, WeightedList.of(newFixedList));
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
		p.println("* If you remove all lines for a Biome, the default values will be used");
		p.println("* So for an 'empty' biome, leave one line with a CREATURE or the AMBIENT line with bats.");
		p.println("* Lines starting with '*' are comments and ignored");
		p.println("* When this file is read, SBU writes summary information to the log file.");
		p.println("* ");

		int biomelineNumber = 0;
		MinecraftServer server = event.getServer();
		RegistryAccess dynreg = server.registryAccess();
		Registry<Biome> biomeRegistry = dynreg.lookupOrThrow(Registries.BIOME);

		for (Biome b : biomeRegistry) {
			String bn = biomeRegistry.getKey(b).toString();
			String cn = Utility.getMyBC(biomeRegistry.wrapAsHolder(b));
			MobSpawnSettings msi = b.getMobSettings();
			for (MobCategory v : MobCategory.values()) {
				for (Weighted<SpawnerData> s : msi.getMobs(v).unwrap()) {
					if (MyConfig.isSuppressMinecraftMobReporting()) {
						if (EntityType.getKey(s.value().type()).getNamespace().equals("minecraft")) {
							continue;
						}
					}
					String modname = EntityType.getKey(s.value().type()).getNamespace();
					if (MyConfig.isIncludedMod(modname)) {
						p.println(++biomelineNumber + ", " + cn + ", " + bn + ", " + v + ", "
								+ EntityType.getKey(s.value().type()).toString() + ", " + s.weight() + ", " + s.value().minCount() + ", "
								+ s.value().maxCount());
					}
				}
			}
		}

		if (p != System.out) {
			p.close();
		}
	}

}
