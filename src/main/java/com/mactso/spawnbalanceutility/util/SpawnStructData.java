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

import com.mactso.spawnbalanceutility.config.MyConfig;
import com.mactso.spawnbalanceutility.manager.StructureCreatureManager;
import com.mactso.spawnbalanceutility.manager.StructureCreatureManager.StructureCreatureItem;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride.BoundingBoxType;
import net.minecraftforge.coremod.api.ASMAPI;

public class SpawnStructData {

	private static Field fieldStructSpawnOverride = null;

	private static final Logger LOGGER = LogManager.getLogger();
	public static final WeightedRandomList<MobSpawnSettings.SpawnerData> SBU_FIX_EMPTY_MOB_LIST = WeightedRandomList
			.create();

	static int structureLineNumber = 0;
	static int structureEventNumber = 0;
	static int reportlinenumber = 0;

	static Set<String> structuresProcessed = new HashSet<>();

	static {
		initReports();
	}

	// minecraft/world/level/levelgen/feature/ConfiguredStructureFeature/f_209744_
	// net/minecraft/world/level/levelgen/feature/ConfiguredStructureFeature/spawnOverrides
	static {
		try {
			String name = ASMAPI.mapField("f_209744_");
			fieldStructSpawnOverride = ConfiguredStructureFeature.class.getDeclaredField(name);
			fieldStructSpawnOverride.setAccessible(true);
		} catch (Exception e) {
			LOGGER.error("XXX Unexpected Reflection Failure trying set Biome.biomeCategory accessible");
		}
	}

	public static void initReports() {
		File fd = new File("config/spawnbalanceutility");
		if (!fd.exists())
			fd.mkdir();
		File fs = new File("config/spawnbalanceutility/StructMobWeight.txt");
		if (fs.exists())
			fs.delete();
	}

	public static void doStructureActions(MinecraftServer server) {

		RegistryAccess ra = server.registryAccess();

		Registry<ConfiguredStructureFeature<?, ?>> csfreg = ra
				.registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);

		if (MyConfig.isBalanceStructureSpawnValues()) {
			balanceStructureSpawnValues(csfreg);
		}
		if (MyConfig.isFixSpawnValues()) {
			fixStructureSpawnValues(csfreg);
		}
		if (MyConfig.isGenerateReport()) {
			generateStructureSpawnValuesReport(csfreg);
		}
	}


	private static void balanceStructureSpawnValues(Registry<ConfiguredStructureFeature<?, ?>> csfreg) {

		List<SpawnerData> newSpawnersList = new ArrayList<>();

		for (ConfiguredStructureFeature<?, ?> csf : csfreg) {

			ResourceLocation key = csfreg.getKey(csf);
			ResourceLocation key2 = csf.feature.getRegistryName();

			ResourceLocation csfKey = csfreg.getKey(csf);
			String csfName = csfKey.toString();
			List<StructureCreatureItem> p = StructureCreatureManager.structureCreaturesMap.get(csfName);
			Map<MobCategory, StructureSpawnOverride> newMap = new HashMap<>();
			if (p != null) {
				for (MobCategory mc : MobCategory.values()) {
					String vCl = mc.getSerializedName();
					for (int i = 0; i < p.size(); i++) {
						StructureCreatureItem sci = p.get(i);

						if (sci.getClassification().toLowerCase().equals(vCl)) {
							@SuppressWarnings("deprecation")
							Optional<EntityType<?>> opt = Registry.ENTITY_TYPE
									.getOptional(new ResourceLocation(sci.getModAndMob()));
							if (opt.isPresent()) {
								SpawnerData newS = new SpawnerData(opt.get(), Weight.of(sci.getSpawnWeight()),
										sci.getMinCount(), sci.getMaxCount());
								newSpawnersList.add(newS);
							}
							newMap.put(mc, new StructureSpawnOverride(BoundingBoxType.STRUCTURE,
									WeightedRandomList.create(newSpawnersList)));
						}

					}
					if (!newMap.isEmpty()) {
						try {
							fieldStructSpawnOverride.set(csf, newMap);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	private static void fixStructureSpawnValues(Registry<ConfiguredStructureFeature<?, ?>> csfreg) {

		List<SpawnerData> newSpawnersList = new ArrayList<>();

		for (ConfiguredStructureFeature<?, ?> csf : csfreg) {
			String csfName = csfreg.getKey(csf).toString();
			ResourceLocation key = csfreg.getKey(csf);
			ResourceLocation key2 = csf.feature.getRegistryName();
			Map<MobCategory, StructureSpawnOverride> newMap = new HashMap<>();

			for (MobCategory mc : MobCategory.values()) {
				StructureSpawnOverride mobs = csf.spawnOverrides.get(mc);
				if (mobs == null)
					continue;
				WeightedRandomList<SpawnerData> oldwrl = mobs.spawns();
				newSpawnersList.clear();
				for (SpawnerData s : oldwrl.unwrap()) {
					int newSpawnWeight = s.getWeight().asInt();
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
					SpawnerData newS = new SpawnerData(s.type, Weight.of(newSpawnWeight), s.minCount, s.maxCount);

					newSpawnersList.add(newS);
				}
				newMap.put(mc,
						new StructureSpawnOverride(mobs.boundingBox(), WeightedRandomList.create(newSpawnersList)));
			}
			try {
				fieldStructSpawnOverride.set(csf, newMap);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	private static void generateStructureSpawnValuesReport(Registry<ConfiguredStructureFeature<?, ?>> csfreg) {

		PrintStream p = null;
		try {
			p = new PrintStream(new FileOutputStream("config/spawnbalanceutility/StructMobWeight.txt", false));
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (p == null) {
			p = System.out;
		}
		for (ConfiguredStructureFeature<?, ?> csf : csfreg) {
			String csfName = csfreg.getKey(csf).toString();
			p.println(++structureLineNumber + ", " + csfName + ", HEADING, header:ignore, 0, 0, 0");
			ResourceLocation key = csfreg.getKey(csf);
			ResourceLocation key2 = csf.feature.getRegistryName();

			for (MobCategory mc : MobCategory.values()) {
				StructureSpawnOverride mobs = csf.spawnOverrides.get(mc);
				if (mobs == null)
					continue;
				for (SpawnerData s : mobs.spawns().unwrap()) {

					if (MyConfig.isSuppressMinecraftMobReporting()) {
						if (s.type.getRegistryName().getNamespace().equals("minecraft")) {
							continue;
						}
					}
					if (MyConfig.isIncludedMod(s.type.getRegistryName().getNamespace())) {
						p.println(++structureLineNumber + ", " + csfName + ", " + mc + ", " + s.type.getRegistryName()
								+ ", " + s.getWeight().asInt() + ", " + s.minCount + ", " + s.maxCount);

					}

				}

			}
		}
		if (p != System.out) {
			p.close();
		}
	}

}
