package com.mactso.spawnbalanceutility.utility;


import java.lang.reflect.Field;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.spawnbalanceutility.config.MyConfigs;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;


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

	public static String getMyBC(RegistryEntry<Biome> registryEntry) {
		//BiomeTags.DESERT_PYRAMID_HAS_STRUCTURE.

		if (registryEntry.isIn(BiomeTags.DESERT_PYRAMID_HAS_STRUCTURE))
			return Utility.DESERT;
		if (registryEntry.isIn(BiomeTags.IS_FOREST))
			return Utility.FOREST;
		if (registryEntry.isIn(BiomeTags.IS_BEACH))
			return Utility.BEACH;
		if (registryEntry.isIn(BiomeTags.IGLOO_HAS_STRUCTURE))
			return Utility.ICY;		
		if (registryEntry.isIn(BiomeTags.IS_JUNGLE))
			return Utility.JUNGLE;		
		if (registryEntry.isIn(BiomeTags.IS_OCEAN))
			return Utility.OCEAN;		
		if (registryEntry.isIn(BiomeTags.IS_DEEP_OCEAN))
			return Utility.OCEAN;		
		if (registryEntry.isIn(BiomeTags.VILLAGE_PLAINS_HAS_STRUCTURE))
			return Utility.PLAINS;		
		if (registryEntry.isIn(BiomeTags.IS_RIVER))
			return Utility.RIVER;		
		if (registryEntry.isIn(BiomeTags.VILLAGE_SAVANNA_HAS_STRUCTURE))
			return Utility.SAVANNA;		
		if (registryEntry.isIn(BiomeTags.SWAMP_HUT_HAS_STRUCTURE))
			return Utility.SWAMP;		
		if (registryEntry.isIn(BiomeTags.IS_TAIGA))
			return Utility.TAIGA;		
		if (registryEntry.isIn(BiomeTags.IS_BADLANDS))
			return Utility.BADLANDS;		
		if (registryEntry.isIn(BiomeTags.IS_MOUNTAIN))
			return Utility.EXTREME_HILLS;		
		if (registryEntry.isIn(BiomeTags.IS_NETHER))
			return Utility.NETHER;
		return "private";
	}
	public static String GetBiomeName(Biome b) {
		return b.toString();
	}

	// support for any color chattext
	public static void sendChat(PlayerEntity p, String chatMessage, TextColor color) {
		MutableText component = Text.literal(chatMessage);
		component.getStyle().withColor(color);
		p.sendMessage(component,false);
	}

	// support for any color, optionally bold text.
	public static void sendBoldChat(PlayerEntity p, String chatMessage, TextColor color) {
		MutableText component = Text.literal(chatMessage);
		component.setStyle(component.getStyle().withBold(true));
		component.setStyle(component.getStyle().withColor(color));
		p.sendMessage(component,false); // if this doesn't work, then check it
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
	
	public static String getResourceLocationString(BlockState blockState) {
		return getResourceLocationString(blockState.getBlock());
	}
	
	@SuppressWarnings("deprecation")
	public static String getResourceLocationString(Block block) {
		return block.getRegistryEntry().registryKey().getValue().toString();
	}

	@SuppressWarnings("deprecation")
	public static String getResourceLocationString(Item item) {
		return item.getRegistryEntry().registryKey().getValue().toString();
	}

	@SuppressWarnings("deprecation")
	public static String getResourceLocationString(Entity entity) {
		return entity.getType().getRegistryEntry().registryKey().getValue().toString();
	}

	public static String getResourceLocationString(World world) {
		return world.getRegistryKey().getValue().toString();
	}

}
