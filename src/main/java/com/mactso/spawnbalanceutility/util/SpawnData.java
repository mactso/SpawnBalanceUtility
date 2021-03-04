package com.mactso.spawnbalanceutility.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mactso.spawnbalanceutility.config.MyConfig;
import com.mactso.spawnbalanceutility.util.BiomeCreatureManager.BiomeCreatureItem;

import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.Category;
import net.minecraft.world.biome.MobSpawnInfo.Spawners;
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
		if (fb.exists())
			fs.delete();
	}

	static int biomelineNumber = 0;
	static int structureLineNumber = 0;
	static int reportlinenumber = 0;
	static int biomeEventNumber = 0;
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onBiome(BiomeLoadingEvent event) {

		String threadname = Thread.currentThread().getName();
		String biomename = event.getName().toString();
		
		if (MyConfig.isBalanceSpawnValues()) {
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
			if (threadname.equals("Render thread")) {
				return;
			}
			generateBiomeSpawnValuesReport(event);
		}

	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onStructure(StructureSpawnListGatherEvent event) {
		if (MyConfig.isGenerateReport()) {
			generateStructureSpawnValueReport(event);
		}

	}

	private static void generateBiomeSpawnValuesReport(BiomeLoadingEvent event) {
		PrintStream p = null;
		try {
			p = new PrintStream(new FileOutputStream("config/spawnbalanceutility/BiomeMobWeight.txt", true));
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (p == null) {
			p = System.out;
		}

		String bn = event.getName().toString();
		String cn = event.getCategory().getName().toString();
		MobSpawnInfoBuilder builder = event.getSpawns();

		for (EntityClassification v : EntityClassification.values()) {

			for (Spawners s : builder.getSpawner(v)) {

				p.println(++biomelineNumber + ", " + cn + ", " + bn + ", " + v + ", " + s.type.getRegistryName()
						+ ", " + s.itemWeight + ", " + s.minCount + ", " + s.maxCount);
			}
		}

		if (p != System.out) {
			p.close();
		}
	}

	private static void balanceBiomeSpawnValues(BiomeLoadingEvent event) {
		String bCl = "";
		String vCl = "";

		MobSpawnInfoBuilder builder = event.getSpawns();
		biomeEventNumber++;

		String key = event.getName().toString();
		List<BiomeCreatureItem> p = BiomeCreatureManager.biomeCreaturesMap.get(key);
		
		if (p != null) {
			for (EntityClassification v : EntityClassification.values()) {
				builder.getSpawner(v).clear();
				vCl = v.getString();
				for (int i = 0; i < p.size(); i++) {
					BiomeCreatureItem b = p.get(i);
					bCl = b.classification;
					if (b.classification.toLowerCase().equals(vCl)) {
						Optional<EntityType<?>> opt = Registry.ENTITY_TYPE
								.getOptional(new ResourceLocation(b.modAndMob));
						if (opt.isPresent()) {
							Spawners newSpawner = new Spawners(opt.get(), b.spawnWeight, b.minCount, b.maxCount);
							builder.withSpawner(v, newSpawner);
							if (MyConfig.getDebugLevel() < 0) {
								System.out.println(
										"Biome :"+ key + " + r:"+reportlinenumber + " SpawnBalanceUtility XXZZY: p.size() ="+p.size()+" Mob " + b.modAndMob + " Added to " + event.getCategory().toString());
							}

						} else {
							System.out.println(
									reportlinenumber + "SpawnBalanceUtility ERROR: Mob " + b.modAndMob + " not in Entity Type Registry");
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

			// XXZZY reenable this code before release.
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

//	private static void fixStructureSpawnValues(StructureSpawnListGatherEvent event) {
//		List<Spawners> lS = new ArrayList<>();	
//		MobSpawnInfoBuilder builder = event.getEntitySpawns();
//		
//		for (EntityClassification v : EntityClassification.values() ) {
//			lS.clear();
//			for (Spawners s : event.getEntitySpawns(v)) {
//				int newSpawnWeight = s.itemWeight;
//				if (newSpawnWeight > MyConfig.getMaxSpawnWeight()) {
//					newSpawnWeight = MyConfig.getMaxSpawnWeight();
//				}
//				if (newSpawnWeight < MyConfig.getMinSpawnWeight()) {
//					newSpawnWeight = MyConfig.getMinSpawnWeight();
//				}
//				Spawners newS = new Spawners(s.type, newSpawnWeight, s.minCount, s.maxCount);
//				lS.add(newS);
//
//				p.println(structureLineNumber+ ", "+ sn + ", " + v + ", " + s.type.getRegistryName() + ", " + s.itemWeight + ", " + s.minCount + ", " +s.maxCount );
//			}
//			if (event.getEntitySpawns(v).size() == 0) {
//				p.println (sn + ", " + v +", nonenone:none, 0, 0, 0");
//			}
//		}
//	}

	private static void generateStructureSpawnValueReport(StructureSpawnListGatherEvent event) {
		PrintStream p = null;
		try {
			p = new PrintStream(new FileOutputStream("config/spawnbalanceutility/StructMobWeight.txt", true));
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (p == null) {
			p = System.out;
		}

		String sn = event.getStructure().getStructureName().toString();

//		p.println("Structure :" + sn +  ". ");

		for (EntityClassification v : EntityClassification.values()) {
			for (Spawners s : event.getEntitySpawns(v)) {
				p.println(++structureLineNumber + ", " + sn + ", " + v + ", " + s.type.getRegistryName() + ", "
						+ s.itemWeight + ", " + s.minCount + ", " + s.maxCount);

			}
			if (event.getEntitySpawns(v).size() == 0) {
				p.println(sn + ", " + v + ", nonenone:none, 0, 0, 0");
			}
		}

		if (p != System.out) {
			p.close();
		}
	}
}
