package com.mactso.spawnbalanceutility.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import com.mactso.spawnbalanceutility.Main;
import com.mactso.spawnbalanceutility.config.MyConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

public class Utility {

	public static String NONE = "none";
	public static String BEACH = "beach";
	public static String BADLANDS = "badlands";
	public static String DESERT = "desert";
	public static String EXTREME_HILLS = "extreme_hills";
	public static String ICY = "icy";
	public static String JUNGLE = "jungle";
	public static String THEEND = "the_end";
	public static String FOREST = "forest";
	public static String MESA = "mesa";
	public static String MUSHROOM = "mushroom";
	public static String MOUNTAIN = "mountain";
	public static String NETHER = "nether";
	public static String OCEAN = "ocean";
	public static String PLAINS = "plains";
	public static String RIVER = "river";
	public static String SAVANNA = "savanna";
	public static String SWAMP = "swamp";
	public static String TAIGA = "taiga";
	public static String UNDERGROUND = "underground";

	public final static int FOUR_SECONDS = 80;
	public final static int TWO_SECONDS = 40;
	public final static float Pct00 = 0.00f;
	public final static float Pct02 = 0.02f;
	public final static float Pct05 = 0.05f;
	public final static float Pct09 = 0.09f;
	public final static float Pct16 = 0.16f;
	public final static float Pct25 = 0.25f;
	public final static float Pct50 = 0.50f;
	public final static float Pct75 = 0.75f;
	public final static float Pct84 = 0.84f;
	public final static float Pct89 = 0.89f;
	public final static float Pct91 = 0.91f;
	public final static float Pct95 = 0.95f;
	public final static float Pct99 = 0.99f;
	public final static float Pct100 = 1.0f;

	private static final Logger LOGGER = LogManager.getLogger();

	public static String getMyBC(Holder<Biome> testBiome) {

		if (testBiome.is(BiomeTags.HAS_VILLAGE_DESERT))
			return Utility.DESERT;
		if (testBiome.is(BiomeTags.IS_FOREST))
			return Utility.FOREST;
		if (testBiome.is(BiomeTags.IS_BEACH))
			return Utility.BEACH;
		if (testBiome.is(BiomeTags.HAS_VILLAGE_SNOWY))
			return Utility.ICY;
		if (testBiome.is(BiomeTags.IS_JUNGLE))
			return Utility.JUNGLE;
		if (testBiome.is(BiomeTags.IS_OCEAN))
			return Utility.OCEAN;
		if (testBiome.is(BiomeTags.IS_DEEP_OCEAN))
			return Utility.OCEAN;
		if (testBiome.is(BiomeTags.HAS_VILLAGE_PLAINS))
			return Utility.PLAINS;
		if (testBiome.is(BiomeTags.IS_RIVER))
			return Utility.RIVER;
		if (testBiome.is(BiomeTags.IS_SAVANNA))
			return Utility.SAVANNA;
		if (testBiome.is(BiomeTags.ALLOWS_SURFACE_SLIME_SPAWNS))
			return Utility.SWAMP;
		if (testBiome.is(BiomeTags.IS_TAIGA))
			return Utility.TAIGA;
		if (testBiome.is(BiomeTags.IS_BADLANDS))
			return Utility.BADLANDS;
		if (testBiome.is(BiomeTags.IS_MOUNTAIN))
			return Utility.EXTREME_HILLS;
		if (testBiome.is(BiomeTags.IS_NETHER))
			return Utility.NETHER;
		if (testBiome.is(BiomeTags.IS_END))
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
				EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entityResourceLocation);
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
			MobSpawnType spawnReason, BlockPos pos, RandomSource rand) {

		Utility.debugMsg(1, Main.MODID + " : " + entityType.getDescriptionId());
		if (spawnReason == MobSpawnType.SPAWNER)
				return true;

		if (spawnReason == MobSpawnType.SPAWN_EGG)
			return true;

		if (level.getDifficulty() == Difficulty.PEACEFUL)
			return false;
		
		BlockState bs = level.getBlockState(pos.below());

		if (!(bs.isValidSpawn(level, pos.below(), entityType))) {
			return false;
		}
		
		if (Monster.isDarkEnoughToSpawn((ServerLevelAccessor) level, pos, rand)) {
			return true;
		}

		return true;
	}

	public static String GetBiomeName(Biome b) {
		return b.toString();
	}

	public static void dbgChatln(Player p, String msg, int level) {
		if (MyConfig.getDebugLevel() > level - 1) {
			sendChat(p, msg, ChatFormatting.YELLOW);
		}
	}

	public static void debugMsg(int level, String dMsg) {

		if (MyConfig.getDebugLevel() > level - 1) {
			LOGGER.info("L" + level + ":" + dMsg);
		}

	}

	public static void debugMsg(int level, BlockPos pos, String dMsg) {

		if (MyConfig.getDebugLevel() > level - 1) {
			LOGGER.info("L" + level + " (" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "): " + dMsg);
		}

	}

	public static void debugMsg(int level, LivingEntity le, String dMsg) {

		if (MyConfig.getDebugLevel() > level - 1) {
			LOGGER.info("L" + level + " (" + le.blockPosition().getX() + "," + le.blockPosition().getY() + ","
					+ le.blockPosition().getZ() + "): " + dMsg);
		}

	}

	public static void sendBoldChat(Player p, String chatMessage, ChatFormatting textColor) {

		MutableComponent component = Component.literal(chatMessage);
		component.setStyle(component.getStyle().withBold(true));
		component.setStyle(component.getStyle().withColor(textColor));
		p.sendSystemMessage(component);

	}

	public static void sendChat(Player p, String chatMessage, ChatFormatting textColor) {

		MutableComponent component = Component.literal(chatMessage);
		component.setStyle(component.getStyle().withColor(textColor));
		p.sendSystemMessage(component);

	}

	public static void updateEffect(LivingEntity e, int amplifier, Holder<MobEffect> mobEffect, int duration) {
		MobEffectInstance ei = e.getEffect(mobEffect);
		if (amplifier == 10) {
			amplifier = 20; // player "plaid" speed.
		}
		if (ei != null) {
			if (amplifier > ei.getAmplifier()) {
				e.removeEffect(mobEffect);
			}
			if (amplifier == ei.getAmplifier() && ei.getDuration() > 10) {
				return;
			}
			if (ei.getDuration() > 10) {
				return;
			}
			e.removeEffect(mobEffect);
		}
		e.addEffect(new MobEffectInstance(mobEffect, duration, amplifier, true, true));
		return;
	}

	public static boolean populateEntityType(EntityType<?> et, ServerLevel level, BlockPos savePos, int maximum,
			int minimum) {
		boolean isBaby = false;
		return populateEntityType(et, level, savePos, maximum, minimum, isBaby);
	}

	public static boolean populateEntityType(EntityType<?> et, ServerLevel level, BlockPos savePos, int maximum,
			int minimum, boolean isBaby) {
		boolean persistant = false;
		return populateEntityType(et, level, savePos, maximum, minimum, persistant, isBaby);
	}

	public static boolean populateEntityType(EntityType<?> et, ServerLevel level, BlockPos savePos, int maximum,
			int minimum, boolean persistant, boolean isBaby) {
		int numZP;
		Mob e;
		numZP = level.random.nextInt(maximum) - minimum;
		if (numZP < 0)
			return false;
		for (int i = 0; i <= numZP; i++) {

			e = (Mob) et.spawn(level, savePos.north(2).west(2), MobSpawnType.NATURAL);
			if (persistant)
				e.setPersistenceRequired();
			e.setBaby(isBaby);
		}
		return true;
	}

	public static boolean populateXEntityType(EntityType<?> et, ServerLevel level, BlockPos savePos, int X,
			boolean isBaby) {

		if (level.isDay() && level.getBrightness(LightLayer.SKY, savePos) > 0) {
			if (et == EntityType.ZOMBIE)
				return false;
			if (et == EntityType.ZOMBIE_VILLAGER)
				return false;
			if (et == EntityType.SKELETON)
				return false;
			if (et == EntityType.STRAY)
				return false;
			if (et == EntityType.PHANTOM)
				return false;
		}
		
		Entity e;		
		for (int i = 0; i < X; i++) {
			System.out.println("populate " + (i + 1) + " of " + X + " " + et.toShortString() + "at" + savePos);
			debugMsg(2, "populate " + (i + 1) + " of " + X + " " + et.toShortString());
			e = et.spawn(level, savePos, MobSpawnType.NATURAL);
			if (e instanceof Mob em) {
				em.setBaby(isBaby);
			}
		}
		return true;
	}

	public static void setName(ItemStack stack, String inString) {
		if (StringUtil.isBlank(inString)) {
			stack.remove(DataComponents.CUSTOM_NAME);
		} else {
			stack.set(DataComponents.CUSTOM_NAME, Component.literal(inString));
		}
	}

	public static void setLore(ItemStack stack, String inString) {
		List<Component> list = new ArrayList<>();
		list.add(Component.literal(inString));
		stack.set(DataComponents.LORE, new ItemLore(list));

	}

	public static boolean isNotNearWebs(BlockPos pos, ServerLevel serverLevel) {

		if (serverLevel.getBlockState(pos).getBlock() == Blocks.COBWEB)
			return true;
		if (serverLevel.getBlockState(pos.above()).getBlock() == Blocks.COBWEB)
			return true;
		if (serverLevel.getBlockState(pos.below()).getBlock() == Blocks.COBWEB)
			return true;
		if (serverLevel.getBlockState(pos.north()).getBlock() == Blocks.COBWEB)
			return true;
		if (serverLevel.getBlockState(pos.south()).getBlock() == Blocks.COBWEB)
			return true;
		if (serverLevel.getBlockState(pos.east()).getBlock() == Blocks.COBWEB)
			return true;
		if (serverLevel.getBlockState(pos.west()).getBlock() == Blocks.COBWEB)
			return true;

		return false;
	}

	public static boolean isOutside(BlockPos pos, ServerLevel serverLevel) {
		return serverLevel.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, pos) == pos;
	}

	public static void slowFlyingMotion(LivingEntity le) {

		if ((le instanceof Player) && (le.isFallFlying())) {
			Player cp = (Player) le;
			Vec3 vec = cp.getDeltaMovement();
			Vec3 slowedVec;
			if (vec.y > 0) {
				slowedVec = vec.multiply(0.17, -0.75, 0.17);
			} else {
				slowedVec = vec.multiply(0.17, 1.001, 0.17);
			}
			cp.setDeltaMovement(slowedVec);
		}
	}

	public static void sendChat(ServerPlayer serverPlayerEntity, String chatMessage) {
		sendChat(serverPlayerEntity, chatMessage, ChatFormatting.GOLD);

	}

}
