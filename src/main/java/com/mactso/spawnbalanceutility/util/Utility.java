package com.mactso.spawnbalanceutility.util;


import java.lang.reflect.Method;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import com.mactso.spawnbalanceutility.Main;
import com.mactso.spawnbalanceutility.config.MyConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public class Utility {

	private static final Logger LOGGER = LogManager.getLogger();
	
	public static final String NONE = "none";
	public static final String BEACH = "beach";
	public static final String BADLANDS = "badlands";
	public static final String DESERT = "desert";
	public static final String EXTREME_HILLS = "extreme_hills";
	public static final String ICY = "icy";
	public static final String JUNGLE = "jungle";
	public static final String THEEND = "the_end";
	public static final String FOREST = "forest";
	public static final String MESA = "mesa";
	public static final String MUSHROOM = "mushroom";
	public static final String MOUNTAIN = "mountain";
	public static final String NETHER = "nether";
	public static final String OCEAN = "ocean";
	public static final String PLAINS = "plains";
	public static final String RIVER = "river";
	public static final String SAVANNA = "savanna";
	public static final String SWAMP = "swamp";
	public static final String TAIGA = "taiga";
	public static final String UNDERGROUND = "underground";
	
	

	public static String getBiomeCategory(Holder<Biome> biomeHolder) { 
		
		if (biomeHolder.is(BiomeTags.HAS_VILLAGE_DESERT))
			return Utility.DESERT;
		if (biomeHolder.is(BiomeTags.IS_FOREST))
			return Utility.FOREST;
		if (biomeHolder.is(BiomeTags.IS_BEACH))
			return Utility.BEACH;
		if (biomeHolder.is(BiomeTags.HAS_VILLAGE_SNOWY))
			return Utility.ICY;		
		if (biomeHolder.is(BiomeTags.IS_JUNGLE))
			return Utility.JUNGLE;		
		if (biomeHolder.is(BiomeTags.IS_OCEAN))
			return Utility.OCEAN;		
		if (biomeHolder.is(BiomeTags.IS_DEEP_OCEAN))
			return Utility.OCEAN;		
		if (biomeHolder.is(BiomeTags.HAS_VILLAGE_PLAINS))
			return Utility.PLAINS;		
		if (biomeHolder.is(BiomeTags.IS_RIVER))
			return Utility.RIVER;		
		if (biomeHolder.is(BiomeTags.IS_SAVANNA))
			return Utility.SAVANNA;		
		if (biomeHolder.is(BiomeTags.ALLOWS_SURFACE_SLIME_SPAWNS))
			return Utility.SWAMP;		
		if (biomeHolder.is(BiomeTags.IS_TAIGA))
			return Utility.TAIGA;		
		if (biomeHolder.is(BiomeTags.IS_BADLANDS))
			return Utility.BADLANDS;		
		if (biomeHolder.is(BiomeTags.IS_MOUNTAIN))
			return Utility.EXTREME_HILLS;		
		if (biomeHolder.is(BiomeTags.IS_NETHER))
			return Utility.NETHER;
		if (biomeHolder.is(BiomeTags.IS_END))
			return Utility.THEEND;			

		return NONE;
		
	}

	@SuppressWarnings("deprecation")
	public static void registerMissingSpawnPlacements() {
		Method m = null;
		try {
			m = SpawnPlacements.class.getDeclaredMethod("register", EntityType.class, SpawnPlacementType.class,
					Heightmap.Types.class, SpawnPlacements.SpawnPredicate.class);
			m.setAccessible(true);
		} catch (Exception e) {
			LOGGER.error("Error, can't reflect into spawn placement registry.  can't register spawn locations for entities with no spawn location");
			return;
		}

		// Loop over each entity resource location and register the spawn placement
		for (String rlString : MyConfig.getFixSpawnPlacementMobsSet()) {
			// Parse the entity resource location
			try {
				ResourceLocation entityResourceLocation = ResourceLocation.parse((rlString.trim()));
				@Nullable
				EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getValue(entityResourceLocation);
				if ((entityType != EntityType.PIG) && (entityType.getCategory() != MobCategory.MISC)) {
					@SuppressWarnings("unchecked")
					EntityType<? extends Mob> entity = (EntityType<? extends Mob>) entityType;
					// Register the spawn placement on the ground for the entity type
					try {
						m.invoke(null, 
								convert(entity, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Utility::genericMobSpawnRules));
					} catch (IllegalStateException e) {
						LOGGER.error(rlString
								+ " already had a SpawnPlacement Registered.  It has been replaced.  Did you mean to do that?");
					}
				} else {
					LOGGER.warn(rlString + " is a configured spawn placement entity for a mod that is not loaded.");
				}
			} catch (Exception e) {
				LOGGER.warn(rlString
						+ " for spawn placement config has a bad an illegal character (should be lower case?).");
			}
			}
		}

	private static <T extends Mob> Object[] convert(EntityType<T> et, SpawnPlacementType pt, Heightmap.Types ht, SpawnPlacements.SpawnPredicate<T> sp)
	{
		return new Object[] {et, pt, ht, sp};
	}
	
	public static boolean genericMobSpawnRules(EntityType<? extends Mob> entityType, LevelAccessor level,
			EntitySpawnReason spawnReason, BlockPos pos, RandomSource rand) {

		Utility.debugMsg(1, Main.MODID + " : " + entityType.getDescriptionId());
		if (spawnReason == EntitySpawnReason.SPAWNER)
				return true;

		if (spawnReason == EntitySpawnReason.SPAWN_ITEM_USE)
			return true;

		// TODO Fix this in 1.21.5
		
		boolean isFriendly = entityType.getCategory().isFriendly();
		
		if (!isFriendly && level.getDifficulty() == Difficulty.PEACEFUL)
			return false;
		
		BlockState bs = level.getBlockState(pos.below());

		if (!(bs.isValidSpawn(level, pos.below(), entityType))) {
			return false;
		}
		
		
		if (isFriendly) {
			return true;
		}

		if (!isFriendly && Monster.isDarkEnoughToSpawn((ServerLevelAccessor) level, pos, rand)) {
			return true;
		}
		
		return false;
	}

	public static String GetBiomeName(Biome b) {
		return b.toString();
	}

	public static void dbgChatln(ServerPlayer p, String msg, int level) {
		if (MyConfig.getDebugLevel() > level - 1) {
			sendChat(p, msg, ChatFormatting.YELLOW);
		}
	}
	
	
	public static void debugMsg(int level, String dMsg) {

		if (MyConfig.getDebugLevel() >= level) {
			LOGGER.info("L" + level + ":" + dMsg);
		}

	}

	public static void debugMsg(int level, BlockPos pos, String dMsg) {

		if (MyConfig.getDebugLevel() >= level) {
			LOGGER.info("L" + level + " (" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "): " + dMsg);
		}

	}

	public static void debugMsg(int level, LivingEntity le, String dMsg) {

		if (MyConfig.getDebugLevel() >= level) {
			LOGGER.info("L" + level + " (" + le.blockPosition().getX() + "," + le.blockPosition().getY() + ","
					+ le.blockPosition().getZ() + "): " + dMsg);
		}

	}

	public static void sendBoldChat(ServerPlayer p, String chatMessage, ChatFormatting textColor) {
		MutableComponent component = Component.literal(chatMessage);
		component.setStyle(component.getStyle().withBold(true).withColor(textColor));
	
		p.sendSystemMessage(component);

	}

	// support for any color chattext
	public static void sendChat(ServerPlayer p, String chatMessage, ChatFormatting textColor) {

		MutableComponent component = Component.literal(chatMessage);
		component.setStyle(component.getStyle().withColor(textColor));
		p.sendSystemMessage(component);

	}



	public static void warn (String dMsg) {
		LOGGER.warn(dMsg);
	}
	
//	public static String getResourceLocationString(BlockState blockState) {
//		return getResourceLocationString(blockState.getBlock());
//	}
	
//	@SuppressWarnings("deprecation")
//	public static String getResourceLocationString(Block block) {
//		return block.getRegistryEntry().registryKey().getValue().toString();
//	}
//
//	@SuppressWarnings("deprecation")
//	public static String getResourceLocationString(Item item) {
//		return item.getRegistryEntry().registryKey().getValue().toString();
//	}
//
//	@SuppressWarnings("deprecation")
//	public static String getResourceLocationString(Entity entity) {
//		return entity.getType().getRegistryEntry().registryKey().getValue().toString();
//	}
//
//	public static String getResourceLocationString(World world) {
//		return world.getRegistryKey().getValue().toString();
//	}

}
