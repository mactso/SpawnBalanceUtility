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
import net.minecraft.core.RegistryAccess;
import com.mactso.spawnbalanceutility.config.MyConfigs;
import com.mactso.spawnbalanceutility.manager.BiomeCreatureManager;
import com.mactso.spawnbalanceutility.manager.BiomeCreatureManager.BiomeCreatureItem;
import com.mactso.spawnbalanceutility.manager.MobMassAdditionManager;
import com.mactso.spawnbalanceutility.manager.MobMassAdditionManager.MassAdditionMobItem;
import com.mactso.spawnbalanceutility.utility.Utility;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;


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
			MobMassAdditionManager.generateMassAdditionMobsStubReport();


	
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

		RegistryAccess dynreg = server.registryAccess();
		Optional<Registry<Biome>> optRegistry = dynreg.lookup( Registries.BIOME);		
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
			fieldSpawners = MobSpawnSettings.class.getDeclaredField(fieldName);
			fieldSpawners.setAccessible(true);
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("XXX Unexpected Reflection Failure set SpawnSettings.spawners accessible");
		}

		String vCl = "";
		List<SpawnerData> newBalancedList = new ArrayList<>();

		for (Biome b : biomeRegistry) {
			ResourceLocation bk = biomeRegistry.getKey(b);

			Optional<Biome> oRE = biomeRegistry.getOptional(bk);
			if (!oRE.isPresent()) {
				continue;
			}

			String bn = biomeRegistry.getKey(b).toString();
			List<BiomeCreatureItem> modBiomeMobSpawners = BiomeCreatureManager.biomeCreaturesMap.get(bn);
			if (modBiomeMobSpawners == null) {
				LOGGER.warn("No spawn values found for biome: " + bn + " when balance flag is true.");
				modBiomeMobSpawners = new ArrayList<>();
				continue;
			}

			MobSpawnSettings msi = b.getMobSettings();

			Map<MobCategory, WeightedRandomList<SpawnerData>> newMap = new HashMap<>();

			for (MobCategory mc : MobCategory.values()) {
				newBalancedList.clear();
				vCl = mc.toString(); // BUG FIX for HybridAquaticFish
				for (BiomeCreatureItem biomeCreatureItem : modBiomeMobSpawners) {
					if (biomeCreatureItem.getClassification().equalsIgnoreCase(vCl)) {
						Optional<EntityType<?>> optRef = BuiltInRegistries.ENTITY_TYPE
								.getOptional(ResourceLocation.parse((biomeCreatureItem.getModAndMob())));

						if (optRef.isPresent()) {
							SpawnerData newSpawner = new SpawnerData(optRef.get(),
									Weight.of(biomeCreatureItem.getSpawnWeight()), biomeCreatureItem.getMinCount(),
									biomeCreatureItem.getMaxCount());
							newBalancedList.add(newSpawner);
						} else {
							LOGGER.error(reportlinenumber + "SpawnBalanceUtility ERROR: Mob "
									+ biomeCreatureItem.getModAndMob() + " not in Entity Type Registry");
						}
					}
				}
				newMap.put(mc, WeightedRandomList.create(newBalancedList));
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

		RegistryAccess dynreg = server.registryAccess();
		Optional<Registry<Biome>> optRegistry = dynreg.lookup( Registries.BIOME);		
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
			fieldSpawners = MobSpawnSettings.class.getDeclaredField(fieldName);
			fieldSpawners.setAccessible(true);
		} catch (Exception e) {
			LOGGER.error("XXX Unexpected Reflection Failure lateBalanceBiomeSpawnValues");
			return;
		}

		List<SpawnerData> newFixedList = new ArrayList<>();

		for (Biome biome : biomeRegistry) {
			
			String bn = biomeRegistry.getKey(biome).toString();	
			LOGGER.warn("SBU Biomes: " + bn);
			// Optional<Holder.Reference<Biome>> oBH = biomeRegistry.get(biomeRegistry.getId(biome));
			
			String biomeName = biomeRegistry.getKey(biome).toString();
			
			ResourceKey<Biome> bk = biomeRegistry.getResourceKey(biome).get();
			Optional<Reference<Biome>> oRE = biomeRegistry.get(bk);
			if (!oRE.isPresent()) {
				continue;
			}
			
			String bcName = biomeRegistry.getKey(biome).toString();

			boolean zombifiedPiglinSpawner = false;
			boolean ghastSpawner = false;

			MobSpawnSettings msi = biome.getMobSettings();
			Map<MobCategory, WeightedRandomList<SpawnerData>> map = null;

			Map<MobCategory, WeightedRandomList<SpawnerData>> newMap = new HashMap<>();

			for (MobCategory mc : MobCategory.values()) {

				newFixedList.clear();
				Utility.debugMsg(1, "biome:" +  biomeName  + ", " + biome.toString());
				WeightedRandomList<SpawnerData> originalSpawnerList = biome.getMobSettings().getMobs(mc);
				for (SpawnerData s : originalSpawnerList.unwrap()) {

					int oldSpawnWeight = s.getWeight().asInt();
					int newSpawnWeight = oldSpawnWeight;
					if (newSpawnWeight > 0) {
						newSpawnWeight = Math.max(MyConfigs.getMinSpawnWeight(), newSpawnWeight);
						newSpawnWeight = Math.min(MyConfigs.getMaxSpawnWeight(), newSpawnWeight);	
					}
					
					String key = s.type.toString();

					// FORGE int dSW = MyConfig.getDefaultSpawnWeight(key);
// FORGE			if (dSW != MyConfig.NO_DEFAULT_SPAWN_WEIGHT_FOUND) {
// FORGE				newSpawnWeight = dSW;
// FORGE			}

					SpawnerData newS = new SpawnerData(s.type, Weight.of(newSpawnWeight), s.minCount, s.maxCount);
					newFixedList.add(newS);

					// Biome o = oRE.get();
					
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
						}
					}

				}

				if (Utility.getMyBC(oRE.get()) == Utility.NETHER) {
					if (mc == MobCategory.MONSTER) {
						if ((zombifiedPiglinSpawner == false) && (MyConfigs.isFixEmptyNether())) {
							SpawnerData newS = new SpawnerData(EntityType.ZOMBIFIED_PIGLIN,
									Weight.of(MyConfigs.getMinSpawnWeight()), 1, 4);
							newFixedList.add(newS);
						}

						if ((ghastSpawner == false) && (MyConfigs.isFixEmptyNether())) {
							SpawnerData newS = new SpawnerData(EntityType.GHAST,
									Weight.of((int) (MyConfigs.getMinSpawnWeight() * 0.75f)), 4, 4);
							newFixedList.add(newS);
						}
					}
				}

				// newMap.put(mc, Pool.of(newFixedList)); // original fabric
				newMap.put(mc, WeightedRandomList.create(newFixedList));  // copied this one line from forge.
			}

			try {
				fieldSpawners.set(msi, newMap);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}


	public static void generateBiomeSpawnValuesReport(MinecraftServer server) {

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

	
		// copied from routine above in this class
		RegistryAccess dynreg = server.registryAccess();
		Optional<Registry<Biome>> optRegistry = dynreg.lookup( Registries.BIOME);		
		if (optRegistry.isEmpty()) {
			LOGGER.error("Hard Error : Biome Registry Missing");
			return;
		}
		Registry<Biome> biomeRegistry = optRegistry.get();


		
		// Standard loop below here.
		
		for (Biome b : biomeRegistry) {
			
			ResourceKey<Biome> bk = biomeRegistry.getResourceKey(b).get();
			Optional<Reference<Biome>> oRE = biomeRegistry.get(bk);

			String bn = biomeRegistry.getKey(b).toString();
			// Optional<Holder.Reference<Biome>> oBH = biomeRegistry.getHolder(biomeRegistry.getId(b));
			String cn = Utility.getMyBC(oRE.get());
			MobSpawnSettings msi = b.getMobSettings();
			for (MobCategory mc : MobCategory.values()) {
				for (SpawnerData s : msi.getMobs(mc).unwrap()) {
					if (MyConfigs.isSuppressMinecraftMobReporting()) {
						if (EntityType.getKey(s.type).getNamespace().equals("minecraft")) {
							continue;
						}
					}
					String modname = EntityType.getKey(s.type).getNamespace();
					if (MyConfigs.isIncludedMod(modname)) {
						p.println(++biomelineNumber + ", " + cn + ", " + bn + ", " + mc.toString() + ", "
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
