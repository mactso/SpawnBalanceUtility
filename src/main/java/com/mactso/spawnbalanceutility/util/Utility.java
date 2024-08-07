package com.mactso.spawnbalanceutility.util;

import java.util.Random;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.spawnbalanceutility.config.MyConfig;

import net.minecraft.block.BlockState;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.registries.ForgeRegistries;

public class Utility {

	private static final Logger LOGGER = LogManager.getLogger();
	
	public static void debugMsg (int level, String dMsg) {
		if (MyConfig.getDebugLevel() > level-1) {
			System.out.println("L"+level + ":" + dMsg);
		}
	}
	

	public static void registerMissingSpawnPlacements() {

		// Loop over each entity resource location and register the spawn placement
		for (String rlString : MyConfig.getFixSpawnPlacementMobsSet()) {
			// Parse the entity resource location
			try {
				ResourceLocation entityResourceLocation = new ResourceLocation(rlString.trim());
				@Nullable
				EntityType<?> entityType = ForgeRegistries.ENTITIES.getValue(entityResourceLocation);
				if ((entityType != EntityType.PIG) && (entityType.getCategory() != EntityClassification.MISC)) {

					// Register the spawn placement on the ground for the entity type
					if (entityType.getCategory() == EntityClassification.MONSTER) {
						@SuppressWarnings("unchecked")
						EntityType<? extends MonsterEntity> entityMonster = (EntityType<? extends MonsterEntity>) entityType;
						try {
							EntitySpawnPlacementRegistry.register(entityMonster, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND,
									Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, Utility::genericMonsterSpawnRules);
						} catch (IllegalStateException e) {
							LOGGER.error(rlString + " already had a SpawnPlacement Registered.  It has been replaced.  Did you mean to do that?");
						}
					} else {
						@SuppressWarnings("unchecked")
						EntityType<? extends CreatureEntity> entityCreature = (EntityType<? extends CreatureEntity>) entityType;
						try {
							EntitySpawnPlacementRegistry.register(entityCreature, EntitySpawnPlacementRegistry.PlacementType.ON_GROUND,
									Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, Utility::genericCreatureSpawnRules);
						} catch (IllegalStateException e) {
							LOGGER.error(rlString + " already had a SpawnPlacement Registered.  It has been replaced.  Did you mean to do that?");
						}
						
					}
				} else {
					LOGGER.warn(rlString + " is a configured spawn placement entity for a mod that is not loaded.");
				}
			} catch( Exception e ) {
				LOGGER.warn(rlString + " for spawn placement config has a bad an illegal character (should be lower case?).");
			}
		}
	}
	
	public static boolean genericMonsterSpawnRules(EntityType<? extends MonsterEntity> entityType, IServerWorld level,
			SpawnReason spawnReason, BlockPos pos, Random rand) {

		System.out.println(entityType.getDescriptionId());
		if (spawnReason == SpawnReason.SPAWNER)
				return true;

		if (spawnReason == SpawnReason.SPAWN_EGG)
			return true;

		if (level.getDifficulty() == Difficulty.PEACEFUL)
			return false;
		
		BlockState bs = level.getBlockState(pos.below());

		if (!(bs.isValidSpawn(level, pos.below(), entityType))) {
			return false;
		}
		
		if (MonsterEntity.isDarkEnoughToSpawn(level, pos, rand)) {
			return true;
		}

		return true;
	}

	
	public static boolean genericCreatureSpawnRules(EntityType<? extends CreatureEntity> entityType, IServerWorld level,
			SpawnReason spawnReason, BlockPos pos, Random rand) {

		System.out.println(entityType.getDescriptionId());
		if (spawnReason == SpawnReason.SPAWNER)
				return true;

		if (spawnReason == SpawnReason.SPAWN_EGG)
			return true;
	
		BlockState bs = level.getBlockState(pos.below());

		if (!(bs.isValidSpawn(level, pos.below(), entityType))) {
			return false;
		}

		return true;
	}
	
	
	public static void debugMsg (int level, BlockPos pos, String dMsg) {
		if (MyConfig.getDebugLevel() > level-1) {
			System.out.println("L"+level+" ("+pos.getX()+","+pos.getY()+","+pos.getZ()+"): " + dMsg);
		}
	}
	
	// support for any color chattext
	public static void sendChat(PlayerEntity p, String chatMessage, Color color) {
		StringTextComponent component = new StringTextComponent(chatMessage);
		component.getStyle().withColor(color);
		p.sendMessage(component, p.getUUID());
	}

	// support for any color, optionally bold text.
	public static void sendBoldChat(PlayerEntity p, String chatMessage, Color color) {
		StringTextComponent component = new StringTextComponent(chatMessage);

		component.getStyle().withBold(true);
		component.getStyle().withColor(color);

		p.sendMessage(component, p.getUUID());
	}
	
}
