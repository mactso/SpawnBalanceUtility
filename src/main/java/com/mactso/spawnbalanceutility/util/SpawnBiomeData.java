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

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
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

		if (MyConfig.isBalanceBiomeSpawnValues()) {
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

		RegistryAccess dynreg = server.registryAccess();
		Optional<Registry<Biome>> optRegistry = dynreg.lookup( Registries.BIOME);		
		if (optRegistry.isEmpty()) {
			LOGGER.error("Hard Error : Biome Registry Missing");
			return;
		}
		Registry<Biome> biomeRegistry = optRegistry.get();

		Field fieldSpawners = null;
		try {
			fieldSpawners = MobSpawnSettings.class.getDeclaredField("spawners");
			fieldSpawners.setAccessible(true);
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("XXX Unexpected Reflection Failure set MobSpawnSettings.spawners accessible");
		}

		int usedTotal = 0;
		String vCl = "";

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

			MobSpawnSettings mobSpawnSettings = b.getMobSettings();

			Map<MobCategory, WeightedList<SpawnerData>> newMap = new HashMap<>();
			int used = 0;

			for (MobCategory mc : MobCategory.values()) {
				List<Weighted<SpawnerData>> newBalancedList = new ArrayList<>();
				vCl = mc.toString(); // BUG FIX for HybridAquaticFish
				for (BiomeCreatureItem biomeCreatureItem : modBiomeMobSpawners) {
					if (biomeCreatureItem.getMobCategory().equalsIgnoreCase(vCl)) {
						Optional<EntityType<?>> optEntityType = BuiltInRegistries.ENTITY_TYPE
									.getOptional(ResourceLocation.parse((biomeCreatureItem.getModAndMob())));

						if (optEntityType.isPresent()) {
														if (optEntityType.get().getCategory() == MobCategory.MISC) {
								Utility.debugMsg(0, Main.MODID + " : " + biomeCreatureItem.getModAndMob()
										+ " is MISC, minecraft is hard coded to change it to minecraft:pig in spawning data.");
							} else if (optEntityType.get().getCategory() != mc) {
								if (biomeCreatureItem.getModAndMob().equals("minecraft:ocelot")) {
									// the ocelot is a creature but it is in the MONSTER mob category- old minecraft bug.
								} else {
									Utility.debugMsg(0,
											Main.MODID + " : " + biomeCreatureItem.getModAndMob() + " Error, mob type "
													+ mc + " different than defined for the type of mob "
													+ optEntityType.get().getCategory());
								}
							}							
							SpawnerData newSpawner = new SpawnerData(optEntityType.get(), biomeCreatureItem.getMinCount(), biomeCreatureItem.getMaxCount());
							newBalancedList.add(new Weighted<>(newSpawner, biomeCreatureItem.getSpawnWeight()));
						} else {
							LOGGER.error(reportlinenumber + "SpawnBalanceUtility ERROR: Mob "
									+ biomeCreatureItem.getModAndMob() + " not in Entity Type Registry");
						}
					}
				}
				newMap.put(mc, WeightedList.of(newBalancedList));
				used += newBalancedList.size();
			}
			try {
				@SuppressWarnings("unchecked")
				Map<MobCategory, WeightedList<SpawnerData>> oldMap = (Map<MobCategory, WeightedList<SpawnerData>>) fieldSpawners.get(mobSpawnSettings);
				fieldSpawners.set(mobSpawnSettings, newMap);
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

		int fixCount = 0;
		int netherCount = 0;
		int biomeTotal = 0;
		Set<EntityType<?>> usedTotalSet = new HashSet<>();
		List<Weighted<SpawnerData>> newFixedList = new ArrayList<>();

		for (Biome biome : biomeRegistry) {			

			String biomeName = biomeRegistry.getKey(biome).toString();	
			LOGGER.warn("SBU Biomes: " + biomeName);
			
			
			MobSpawnSettings msi = biome.getMobSettings();
					
			ResourceKey<Biome> bk = biomeRegistry.getResourceKey(biome).get();
			Optional<Reference<Biome>> oRE = biomeRegistry.get(bk);
			if (!oRE.isPresent()) {
				continue;
			}
			
			Map<MobCategory, WeightedList<SpawnerData>> newMap = new HashMap<>();
			
			boolean zombifiedPiglinSpawner = false;
			boolean ghastSpawner = false;
			List<EntityType<?>> usedList = new ArrayList<>();



			for (MobCategory mc : MobCategory.values()) {

				newFixedList.clear();
				Utility.debugMsg(1, "biome:" +  biomeName  + ", " + biome.toString());
				WeightedList<SpawnerData> originalSpawnerList = biome.getMobSettings().getMobs(mc);
				for ( Weighted<SpawnerData> wsd : originalSpawnerList.unwrap()) {

					int oldSpawnWeight = wsd.weight();
					SpawnerData spawnerData = wsd.value();
					
					int newSpawnWeight = oldSpawnWeight;
					if (newSpawnWeight > 0) {
						newSpawnWeight = Math.max(MyConfig.getMinSpawnWeight(), newSpawnWeight);
						newSpawnWeight = Math.min(MyConfig.getMaxSpawnWeight(), newSpawnWeight);	
					}
					
					Utility.debugMsg(2, Main.MODID + ":" + wsd.value().type().getDescriptionId() + " minimum Spawn changed from "
								+ oldSpawnWeight + " to " + newSpawnWeight);
								

// This default spawn weight feature isn't in fabric yet.
//					String key = EntityType.getKey(spawnerData.type()).toString();
//					int dSW = MyConfig.getDefaultSpawnWeight(key);
//					if (dSW != MyConfig.NO_DEFAULT_SPAWN_WEIGHT_FOUND) {
//						if (newSpawnWeight == 0) {
//							LOGGER.warn("WARN Setting " + key + " to non-zero spawnweight value may cause runaway spawning.");
//						}
//						newSpawnWeight = dSW;
//					}
					if (newSpawnWeight != oldSpawnWeight)
						fixCount++;

					newFixedList.add(new Weighted<>(spawnerData, newSpawnWeight));

					// Biome o = oRE.get();
					
					if (Utility.getMyBC(oRE.get()) == Utility.NETHER) {
						if (spawnerData.type() == EntityType.ZOMBIFIED_PIGLIN)
							zombifiedPiglinSpawner = true;
						if (spawnerData.type() == EntityType.GHAST) {
							ghastSpawner = true;
						}
					}
				}

				List<MassAdditionMobItem> massAddMobs = MobMassAdditionManager.getFilteredList(mc, biomeName);
				EntityType<?> et;
				for (MassAdditionMobItem ma : massAddMobs) {

					Optional<EntityType<?>> oe = EntityType.byString(ma.getModAndMob());
					if (oe.isPresent()) {
						et = oe.get();
						boolean mobFound = false;
						for (Weighted<SpawnerData> wsd : newFixedList) {
							if (wsd.value().type() == et) {
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

				if (Utility.getMyBC(oRE.get()) == Utility.NETHER) {
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
				fieldSpawners.set(msi, newMap);
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

	
		int biomelineNumber = 0;
		// MinecraftServer server = event.getServer(); Forge is event driven.
		RegistryAccess dynreg = server.registryAccess();
		Optional<Registry<Biome>> optRegistry = dynreg.lookup( Registries.BIOME);		
		if (optRegistry.isEmpty()) {
			LOGGER.error("Hard Error : Biome Registry Missing");
			return;
		}
		Registry<Biome> biomeRegistry = optRegistry.get();


		
		// Standard loop below here.
		
		for (Biome biome : biomeRegistry) {
			
			ResourceKey<Biome> bk = biomeRegistry.getResourceKey(biome).get();
			Optional<Reference<Biome>> oRE = biomeRegistry.get(bk);

			String bn = biomeRegistry.getKey(biome).toString();
			// Optional<Holder.Reference<Biome>> oBH = biomeRegistry.getHolder(biomeRegistry.getId(b));
			String cn = Utility.getMyBC(oRE.get());
			MobSpawnSettings msi = biome.getMobSettings();
			for (MobCategory mc : MobCategory.values()) {
				for (Weighted<SpawnerData> wsd : msi.getMobs(mc).unwrap()) {
					SpawnerData spawnerData = wsd.value();
					if (MyConfig.isSuppressMinecraftMobReporting()) {
						if (EntityType.getKey(spawnerData.type()).getNamespace().equals("minecraft")) {
							continue;
						}
					}
					String modname = EntityType.getKey(spawnerData.type()).getNamespace();
					if (MyConfig.isIncludedMod(modname)) {
						p.println(++biomelineNumber + ", " + cn + ", " + bn + ", " + mc + ", "
								+ EntityType.getKey(spawnerData.type()).toString() + ", " + wsd.weight() + ", " + wsd.value().minCount() + ", "
								+ wsd.value().maxCount());
					}
				}
			}
			
		}

		if (p != System.out) {
			p.close();
		}
	}

}
