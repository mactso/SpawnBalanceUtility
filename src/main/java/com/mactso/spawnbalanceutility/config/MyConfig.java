package com.mactso.spawnbalanceutility.config;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.spawnbalanceutility.Main;
import com.mactso.spawnbalanceutility.util.BiomeCreatureManager;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MyConfig {
	private static final Logger LOGGER = LogManager.getLogger();
	public static final Common COMMON;
	public static final ForgeConfigSpec COMMON_SPEC;
	static {
		final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
		COMMON_SPEC = specPair.getRight();
		COMMON = specPair.getLeft();
	}

	public static int getDebugLevel() {
		return debugLevel;
	}

	public static void setDebugLevel(int debugLevel) {
		MyConfig.debugLevel = debugLevel;
	}

	public static boolean isGenerateReport() {
		return generateReport;
	}

	public static boolean isFixEmptyNether() {
		return fixEmptyNether;
	}

	public static boolean isBalanceSpawnValues() {
		return balanceSpawnValues;
	}

	public static boolean isFixSpawnValues() {
		return fixSpawnValues;
	}

	public static int getMinSpawnWeight() {
		return minSpawnWeight;
	}

	public static int getMaxSpawnWeight() {
		return maxSpawnWeight;
	}

	public static int debugLevel;
	private static boolean generateReport;
	private static boolean fixEmptyNether;
	private static boolean balanceSpawnValues;
	private static boolean fixSpawnValues;
	public static int minSpawnWeight;
	public static int maxSpawnWeight;
	
	@SubscribeEvent
	public static void onModConfigEvent(final ModConfig.ModConfigEvent configEvent) {
		System.out.println("Spawn Balance Config Event");
		if (configEvent.getConfig().getSpec() == MyConfig.COMMON_SPEC) {
			bakeConfig();
		}
	}

	public static void pushDebugValue() {
		if (debugLevel > 0) {
			System.out.println("hardernaturalhealing debugLevel:" + MyConfig.debugLevel);
		}
		COMMON.debugLevel.set(MyConfig.debugLevel);
	}

	public static void bakeConfig() {

		debugLevel = COMMON.debugLevel.get();
		generateReport = COMMON.generateReport.get();
		fixEmptyNether  = COMMON.fixEmptyNether.get();
		balanceSpawnValues = COMMON.balanceSpawnValues.get();
		fixSpawnValues = COMMON.fixSpawnValues.get();
		minSpawnWeight = COMMON.minSpawnWeight.get();
		maxSpawnWeight = COMMON.maxSpawnWeight.get();
		
		if (debugLevel > 0) {
			System.out.println("HarderNaturalHealing Debug: " + debugLevel);
		}
		
		BiomeCreatureManager.biomeCreatureInit();
	}
	
	public static class Common {



		public final IntValue debugLevel;
		public final BooleanValue generateReport;
		public final BooleanValue fixSpawnValues;
		public final BooleanValue fixEmptyNether;
		public final BooleanValue balanceSpawnValues;
		public final IntValue minSpawnWeight;
		public final IntValue maxSpawnWeight;
		
		public Common(ForgeConfigSpec.Builder builder) {
			builder.push("Harder Natural Healing Control Values");

			debugLevel = builder.comment("Debug Level: 0 = Off, 1 = Log, 2 = Chat+Log")
					.translation(Main.MODID + ".config." + "debugLevel").defineInRange("debugLevel", () -> 0, 0, 2);
			
			minSpawnWeight = builder.comment("minimum Spawn Weight")
					.translation(Main.MODID + ".config." + "minSpawnWeight").defineInRange("minSpawnWeight", () -> 10, 1, 1000);

			maxSpawnWeight = builder.comment("maximum Spawn Weight")
					.translation(Main.MODID + ".config." + "maxSpawnWeight").defineInRange("maxSpawnWeight", () -> 80, 1, 1000);

			generateReport = builder.comment("generateReport")
					.translation(Main.MODID + ".config." + "generateReport")
					.define("generateReport", true);

			fixEmptyNether = builder.comment("fixEmptyNether")
					.translation(Main.MODID + ".config." + "fixEmptyNether")
					.define("fixEmptyNether", true);

			balanceSpawnValues = builder.comment("Use the CSV file to balance spawn values")
					.translation(Main.MODID + ".config." + "balanceSpawnValues")
					.define("balanceSpawnValues", true);

			fixSpawnValues = builder.comment("Fix min, max values and add nether creatures")
					.translation(Main.MODID + ".config." + "fixSpawnValues")
					.define("fixSpawnValues", true);

			builder.pop();
		}
	}

	// support for any color chattext
	public static void sendChat(PlayerEntity p, String chatMessage, Color color) {
		StringTextComponent component = new StringTextComponent(chatMessage);
		component.getStyle().setColor(color);
		p.sendMessage(component, p.getUniqueID());
	}

	// support for any color, optionally bold text.
	public static void sendBoldChat(PlayerEntity p, String chatMessage, Color color) {
		StringTextComponent component = new StringTextComponent(chatMessage);

		component.getStyle().setBold(true);
		component.getStyle().setColor(color);

		p.sendMessage(component, p.getUniqueID());
	}
}