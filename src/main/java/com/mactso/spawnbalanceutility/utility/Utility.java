package com.mactso.spawnbalanceutility.utility;


import java.lang.reflect.Field;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.spawnbalanceutility.config.MyConfigs;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;

public class Utility {
	private static Field fieldBiomeCategory = null;
	private static final Logger LOGGER = LogManager.getLogger();
	
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
	
	
	static {
//		try {
//			MappingResolver mapping = FabricLoader.getInstance().getMappingResolver();
//			String fieldName = mapping.mapFieldName("intermediary", "net.minecraft.class_1959", "field_9329",
//					"Lnet/minecraft/class_1959$class_1961;");
//			fieldBiomeCategory = Biome.class.getDeclaredField(fieldName);
//			fieldBiomeCategory.setAccessible(true);
//		} catch (Exception e) {
//			LOGGER.error("Unexpected Reflection Failure set Biome.category accessible");
//		}
//		if (fieldBiomeCategory == null) {
//			try {
//				String name = "category";  // see mappings.jar
//				fieldBiomeCategory = Biome.class.getDeclaredField(name);
//				fieldBiomeCategory.setAccessible(true);
//			} catch (Exception e) {
//				LOGGER.error("Development Biome field 'category' not found.");
//			}
//			
//		}
	}

	public static String getMyBC(Holder<Biome> biomeHolder) { 
		
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
		return "private";
	}
	public static String GetBiomeName(Biome b) {
		return b.toString();
	}

	// support for any color chattext
	public static void sendChat(ServerPlayer p, String chatMessage, ChatFormatting textColor) {
		MutableComponent component = Component.literal(chatMessage);
		component.setStyle(component.getStyle().withColor(textColor));
		p.sendSystemMessage(component);
	}

	public static void sendBoldChat(ServerPlayer p, String chatMessage, ChatFormatting textColor) {

		MutableComponent component = Component.literal(chatMessage);
		component.setStyle(component.getStyle().withBold(true));
		component.setStyle(component.getStyle().withColor(textColor));
		p.sendSystemMessage(component);
	}

	public static void warn (String dMsg) {
		LOGGER.warn(dMsg);
	}
	
	public static void debugMsg (int level, BlockPos pos, String dMsg) {
		debugMsg(level, " ("+pos.getX()+","+pos.getY()+","+pos.getZ()+"): " + dMsg);
	}
	
	public static void debugMsg(int level, String dMsg) {
		if (MyConfigs.getDebugLevel() > level - 1) {
			LOGGER.warn("L" + level + ":" + dMsg);
		}
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
