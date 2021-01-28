package com.mactso.spawnbalanceutility.entities;

import java.util.List;

import net.minecraft.entity.EntityType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.Category;
import net.minecraft.world.biome.MobSpawnInfo.Spawners;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraftforge.event.world.BiomeLoadingEvent;

public class ModEntities {


	public static void getBiomeSpawnData(List<Spawners> spawns, BiomeLoadingEvent event) {
		int weight;
		int min;
		int max;

		Category biomeCategory = event.getCategory();
		
		if (biomeCategory == Biome.Category.NETHER) {

			boolean zombiePiglinSpawner = false;
			boolean ghastSpawner = false;

			// more efficient but less generic.
			for (int i = 0; i < spawns.size(); i++) {
				Spawners s = spawns.get(i);
				if (s.type == EntityType.ZOMBIFIED_PIGLIN) {
					zombiePiglinSpawner = true;
				}
				if (s.type == EntityType.GHAST) {
					ghastSpawner = true;
				}

			}

//			if (!zombiePiglinSpawner) {
//				spawns.add(new Spawners(EntityType.ZOMBIFIED_PIGLIN, weight = MyConfig.getZombifiedPiglinSpawnBoost(),
//						min = 1, max = 3));
//			}
			
		} else 	
		if (biomeCategory == Biome.Category.RIVER) {
//			spawns.add(new Spawners(EntityType.COD, weight = MyConfig.getCodSpawnBoost()/3, min = 1, max = 2));
//			spawns.add(new Spawners(EntityType.SALMON, weight = MyConfig.getSalmonSpawnBoost()/5, min = 1, max = 2));
//			spawns.add(new Spawners(EntityType.SQUID, weight = MyConfig.getSquidSpawnBoost()/2, min = 1, max = 4));
		
		} else {
//			spawns.add(new Spawners(SLIPPERY_BITER, weight = 20, min = 1, max = 2));
//			spawns.add(new Spawners(RIVER_GUARDIAN, weight = 10, min = 1, max = 1));
//			spawns.add(new Spawners(GURTY, weight = 15, min = 1, max = 1));
		}

	}

	public static void getFeatureSpawnData(List<Spawners> spawns, Structure<?> structure) {
		int weight;
		int min;
		int max;

		if (structure == Structure.OCEAN_RUIN) {
//			spawns.add(new Spawners(RIVER_GUARDIAN, weight = 20, min = 1, max = 1));
//			spawns.add(new Spawners(SLIPPERY_BITER, weight = 20, min = 1, max = 1));
		} else if (structure == Structure.SHIPWRECK) {
//			spawns.add(new Spawners(RIVER_GUARDIAN, weight = 15, min = 1, max = 1));
//			spawns.add(new Spawners(SLIPPERY_BITER, weight = 15, min = 1, max = 1));
		} 

	}
}
