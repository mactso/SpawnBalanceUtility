package com.mactso.spawnbalanceutility.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.mactso.spawnbalanceutility.config.MyConfig;
import com.mactso.spawnbalanceutility.entities.ModEntities;
import com.mactso.spawnbalanceutility.util.BiomeCreatureManager.*;
import com.mactso.spawnbalanceutility.util.StructureCreatureManager;
import com.mactso.spawnbalanceutility.util.StructureCreatureManager.StructureCreatureItem;

import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.Category;
import net.minecraft.world.biome.MobSpawnInfo.Spawners;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraftforge.common.world.MobSpawnInfoBuilder;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.event.world.StructureSpawnListGatherEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SpawnData {
	static {
		File fd = new File("config/spawnbalanceutility");
		if (!fd.exists())
			fd.mkdir();
		File fb = new File("config/spawnbalanceutility/BiomeMobWeight.txt");
		if (fb.exists())
			fb.delete();
		File fs = new File("config/spawnbalanceutility/StructMobWeight.txt");
		if (fs.exists())
			fs.delete();
	}

	static int biomelineNumber = 0;
	static int structureLineNumber = 0;
	static int reportlinenumber = 0;
	static int biomeEventNumber = 0;
	static int structureEventNumber = 0;
	static Set<String> biomesProcessed = new HashSet<>();
	static Set<String> structuresProcessed = new HashSet<>();
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onBiome(BiomeLoadingEvent event) {

		String threadname = Thread.currentThread().getName();
		String biomename = event.getName().toString();
		
		if (MyConfig.isBalanceBiomeSpawnValues()) {
			balanceBiomeSpawnValues(event);
			if (MyConfig.getDebugLevel() > 0) {
				System.out.println("SpawnBalanceUtility: Balancing "+ biomename +" with BiomeMobWeight.CSV Spawn weight Values. ");
			}
		}

		if (MyConfig.isFixSpawnValues()) {
			fixBiomeSpawnValues(event);
			if (MyConfig.getDebugLevel() > 0) {
				System.out.println("SpawnBalanceUtility: Fixing "+ biomename+ " extreme spawn values. ");
				if (MyConfig.isFixEmptyNether() && (event.getCategory() == Category.NETHER)) {
					System.out.print("  Adding zombified piglin and ghasts to Nether Zone.");
				}
			}
		}

		
		// may be 'format'
		if (MyConfig.isGenerateReport()) {
			generateBiomeSpawnValuesReport(event);
		}

	}


	
	
	
	
	private static void generateBiomeSpawnValuesReport(BiomeLoadingEvent event) {
		
		String bn = event.getName().toString();	

		synchronized (biomesProcessed) {
			if (biomesProcessed.contains(bn)) {
				return;
			}
			biomesProcessed.add(bn);
		}
		
		PrintStream p = null;
		try {
			p = new PrintStream(new FileOutputStream("config/spawnbalanceutility/BiomeMobWeight.txt", true));
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (p == null) {
			p = System.out;
		}

		String cn = event.getCategory().getName().toString();
		MobSpawnInfoBuilder builder = event.getSpawns();

		for (EntityClassification v : EntityClassification.values()) {

			for (Spawners s : builder.getSpawner(v)) {
				if (MyConfig.isSuppressMinecraftMobReporting()) {
					if (s.type.getRegistryName().getNamespace().equals("minecraft")) {
						continue;
					}
				}
				String modname = s.type.getRegistryName().getNamespace();
				if (MyConfig.isIncludedMod(modname)) {
					p.println(++biomelineNumber + ", " + cn + ", " + bn + ", " + v + ", " + s.type.getRegistryName()
					+ ", " + s.itemWeight + ", " + s.minCount + ", " + s.maxCount);
				}
			}
		}

		if (p != System.out) {
			p.close();
		}
	}


	
	
	
	
	private static void balanceBiomeSpawnValues(BiomeLoadingEvent event) {
		String bCl = "";
		String vCl = "";

		MobSpawnInfoBuilder eventSpawnsBuilder = event.getSpawns();
		biomeEventNumber++;

		String modBiomeKey = event.getName().toString();
		List<BiomeCreatureItem> modBiomeMobSpawners = BiomeCreatureManager.biomeCreaturesMap.get(modBiomeKey);
		
		if (modBiomeMobSpawners != null) {
			for (EntityClassification v : EntityClassification.values()) {
				eventSpawnsBuilder.getSpawner(v).clear();
				vCl = v.getString();
				for (int i = 0; i < modBiomeMobSpawners.size(); i++) {
					BiomeCreatureItem biomeCreatureItem = modBiomeMobSpawners.get(i);
					bCl = biomeCreatureItem.classification;
					if (biomeCreatureItem.classification.toLowerCase().equals(vCl)) {
						Optional<EntityType<?>> opt = Registry.ENTITY_TYPE
								.getOptional(new ResourceLocation(biomeCreatureItem.modAndMob));
						if (opt.isPresent()) {
							Spawners newSpawner = new Spawners(opt.get(), biomeCreatureItem.spawnWeight, biomeCreatureItem.minCount, biomeCreatureItem.maxCount);
							eventSpawnsBuilder.withSpawner(v, newSpawner);
							if (MyConfig.getDebugLevel() > 0) {
								System.out.println(
										"Biome :"+ modBiomeKey + " + r:"+reportlinenumber + " SpawnBalanceUtility XXZZY: p.size() ="+modBiomeMobSpawners.size()+" Mob " + biomeCreatureItem.modAndMob + " Added to " + event.getCategory().toString());
							}

						} else {
							System.out.println(
									reportlinenumber + "SpawnBalanceUtility ERROR: Mob " + biomeCreatureItem.modAndMob + " not in Entity Type Registry");
						}
					}
				}
			}
		}
	}

	
	
	
	
	
	private static void fixBiomeSpawnValues(BiomeLoadingEvent event) {

		List<Spawners> lS = new ArrayList<>();

//		String bn = event.getName().toString();
//		String cn = event.getCategory().getName().toString();
		MobSpawnInfoBuilder builder = event.getSpawns();
		boolean classificationMonster = false;
		boolean zombifiedPiglinSpawner = false;
		boolean ghastSpawner = false;
		// given- we have the biome name- the category name.

		for (EntityClassification v : EntityClassification.values()) {
			// and here we have the classification
			// looks like the mob name can't be part of the key however.
			// the hashtable.elements() may give an enumeration from a biome.

			lS.clear();
			for (Spawners s : builder.getSpawner(v)) {
				int newSpawnWeight = s.itemWeight;
				if (newSpawnWeight > MyConfig.getMaxSpawnWeight()) {
					newSpawnWeight = MyConfig.getMaxSpawnWeight();
				}
				if (newSpawnWeight < MyConfig.getMinSpawnWeight()) {
					newSpawnWeight = MyConfig.getMinSpawnWeight();
				}
				Spawners newS = new Spawners(s.type, newSpawnWeight, s.minCount, s.maxCount);
				lS.add(newS);
				if (event.getCategory() == Biome.Category.NETHER) {
					if (s.type == EntityType.ZOMBIFIED_PIGLIN)
						zombifiedPiglinSpawner = true;
					if (s.type == EntityType.GHAST) {
						ghastSpawner = true;
					}
				}

			}

 
			if (event.getCategory() == Biome.Category.NETHER) {
				if (v == EntityClassification.MONSTER) {
					if ((zombifiedPiglinSpawner == false) && (MyConfig.isFixEmptyNether())) {
						Spawners newS = new Spawners(EntityType.ZOMBIFIED_PIGLIN, MyConfig.getMinSpawnWeight(), 1, 4);
						lS.add(newS);
					}

					if ((ghastSpawner == false) && (MyConfig.isFixEmptyNether())) {
						Spawners newS = new Spawners(EntityType.GHAST, (int) (MyConfig.getMinSpawnWeight() * 0.75f), 4,
								4);
						lS.add(newS);
					}
				}
			}

			builder.getSpawner(v).clear();
			for (Spawners s : lS) {
				builder.withSpawner(v, s);
			}
		}

	}



	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onStructure(StructureSpawnListGatherEvent event) {
		
		String threadname = Thread.currentThread().getName();
		String structurename = event.getStructure().getStructureName();
		
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

		String bCl = "";
		String vCl = "";

		structureEventNumber++;

		String key = event.getStructure().getRegistryName().toString();
		List<StructureCreatureItem> p = StructureCreatureManager.structureCreaturesMap.get(key);

		List<Spawners> newSpawnersList = new ArrayList<>();
		List<Spawners> theirSpawnersList = new ArrayList<>();

		if (p != null) {
			for (EntityClassification ec : EntityClassification.values()) {
				vCl = ec.getString();
				newSpawnersList.clear();
				theirSpawnersList.clear();
				for (int i = 0; i < p.size(); i++) {
					StructureCreatureItem sci = p.get(i);
					bCl = sci.classification;
					if (sci.classification.toLowerCase().equals(vCl)) {
						Optional<EntityType<?>> opt = Registry.ENTITY_TYPE
								.getOptional(new ResourceLocation(sci.modAndMob));
						if (opt.isPresent()) {
							Spawners newSpawner = new Spawners(opt.get(), sci.spawnWeight, sci.minCount, sci.maxCount);
							newSpawnersList.add(newSpawner);
							MyConfig.setDebugLevel(0);
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
				int newSpawnWeight = s.itemWeight;
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
			for (Spawners s: theirSpawnersList) {
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
		p.println(++structureLineNumber+ ", " + sn + ", HEADING, header:ignore, 0, 0, 0");

		for (EntityClassification ec : EntityClassification.values()) {
			spawners = event.getEntitySpawns(ec);
			for (Spawners s : spawners) {
				if (MyConfig.isSuppressMinecraftMobReporting()) {
					if (s.type.getRegistryName().getNamespace().equals("minecraft")) {
						continue;
					}
				}
				if (MyConfig.isIncludedMod(s.type.getRegistryName().getNamespace())) {
					p.println(++structureLineNumber+ ", " + sn + ", " + ec +", "+ s.type.getRegistryName() + ", " + s.itemWeight
							+ ", " + s.minCount + ", " + s.maxCount);
					if (MyConfig.debugLevel > 0) {
						System.out.println(++structureLineNumber + ", " + sn + ", " + s.type.getRegistryName() + ", "
								+ s.itemWeight + ", " + s.minCount + ", " + s.maxCount);
					}
				}
			}
		}

		if (p != System.out) {
			p.close();
		}
	}	
	
}
	

