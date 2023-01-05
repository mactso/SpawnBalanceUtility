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
//		try {
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
		File fb = new File("config/spawnbalanceutility/BiomeMobWeight.txt");
		if (fb.exists())
			fb.delete();
		File fma = new File("config/spawnbalanceutility/MassAdditionMobs.txt");
		if (!(fma.exists()))
			generateMassAdditionMobsStubReport();
	}


	public static void balanceBiomeSpawnValues(MinecraftServer server) {

		RegistryAccess dynreg = server.registryAccess();
		Registry<Biome> biomeRegistry =  dynreg.registryOrThrow(Registries.BIOME);
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
				LOGGER.warn("XXX Balance Biomes True but BiomeMobWeight.CSV missing, empty, or has no valid mobs.");
				modBiomeMobSpawners = new ArrayList<>();
				continue;
			}
			int x = 3;
			
			MobSpawnSettings msi = b.getMobSettings ();

			Map<MobCategory, WeightedRandomList<SpawnerData>> newMap = new HashMap<>();

			for (MobCategory v : MobCategory.values()) {
				List<SpawnerData> newFixedList = new ArrayList<>();
				vCl = v.getSerializedName ();
				for (BiomeCreatureItem biomeCreatureItem : modBiomeMobSpawners) {
					if (biomeCreatureItem.getClassification().toLowerCase().equals(vCl)) {
						@SuppressWarnings("deprecation")
						Optional<EntityType<?>> opt = BuiltInRegistries.ENTITY_TYPE
								.getOptional(new ResourceLocation(biomeCreatureItem.getModAndMob()));
						if (opt.isPresent()) {
							SpawnerData newSpawner = new SpawnerData(opt.get(), Weight.of(biomeCreatureItem.getSpawnWeight()),
									biomeCreatureItem.getMinCount(), biomeCreatureItem.getMaxCount());
							newFixedList.add(newSpawner);
							if (MyConfig.getDebugLevel() > 0) {
								System.out.println("Biome :" + bn + " + r:" + reportlinenumber
										+ " SpawnBalanceUtility XXZZY: p.size() =" + modBiomeMobSpawners.size()
										+ " Mob " + biomeCreatureItem.getModAndMob() + " Added to "
										+ bcName);
							}

						} else {
							System.out.println(reportlinenumber + "SpawnBalanceUtility ERROR: Mob "
									+ biomeCreatureItem.getModAndMob() + " not in Entity Type Registry");
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


	@SuppressWarnings("unchecked")
	public static void fixBiomeSpawnValues(MinecraftServer server) {

		RegistryAccess dynreg = server.registryAccess();
		Registry<Biome> biomeRegistry =  dynreg.registryOrThrow(Registries.BIOME);
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

			String bn = biomeRegistry.getKey(b).toString();
			Optional<Holder.Reference<Biome>> oBH = biomeRegistry.getHolder(biomeRegistry.getId(b));
			if (!oBH.isPresent()) {
				continue;
			}
			String bcName = Utility.getMyBC(oBH.get());
			
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
				System.out.println ("biome:" + bn + ", " + b.toString());
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
						System.out.println(s.type.getDescriptionId() + " minspawn change from " + s.getWeight().asInt() + " to "
								+ newSpawnWeight);
					}
					String key = EntityType.getKey(s.type).toString();
					int dSW = MyConfig.getDefaultSpawnWeight(key);
					if (dSW != MyConfig.NO_DEFAULT_SPAWN_WEIGHT_FOUND) {
						newSpawnWeight = dSW;
					}

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

				List<MassAdditionMobItem> massAddMobs = MobMassAdditionManager.getFilteredList(v, bcName);
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
							SpawnerData newS = new SpawnerData(et, Weight.of(ma.getSpawnWeight()), ma.getMinCount(), ma.getMaxCount());
							newFixedList.add(newS);
						}
					}

				}

				if (Utility.getMyBC(oBH.get()) == Utility.NETHER) {
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
			p = new PrintStream(new FileOutputStream("config/spawnbalanceutility/BiomeMobWeight.txt", false));
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (p == null) {
			p = System.out;
		}
		int biomelineNumber = 0;
		MinecraftServer server = event.getServer();
		RegistryAccess dynreg = server.registryAccess();
		Registry<Biome> biomeRegistry =  dynreg.registryOrThrow(Registries.BIOME);

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
						p.println(++biomelineNumber + ", " + cn + ", " + bn + ", " + v + ", " + EntityType.getKey(s.type).toString()
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
