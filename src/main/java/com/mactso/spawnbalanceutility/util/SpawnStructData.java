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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraftforge.event.world.StructureSpawnListGatherEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SpawnStructData {

	private static final Logger LOGGER = LogManager.getLogger();

	static int structureLineNumber = 0;
	static int structureEventNumber = 0;
	static int reportlinenumber = 0;

	static Set<String> structuresProcessed = new HashSet<>();

	static {
		initReports();
	}

	public static void initReports() {
		File fd = new File("config/spawnbalanceutility");
		if (!fd.exists())
			fd.mkdir();
		File fs = new File("config/spawnbalanceutility/StructMobWeight.txt");
		if (fs.exists())
			fs.delete();
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onStructure(StructureSpawnListGatherEvent event) {

		String threadname = Thread.currentThread().getName();

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
//			generateStructureSpawnValuesReport(event);
		}

	}

	private static void balanceStructureSpawnValues(StructureSpawnListGatherEvent event) {

//		String bCl = "";
		String vCl = "";

		structureEventNumber++;

		String key = event.getStructure().getRegistryName().toString();
		List<StructureCreatureItem> p = StructureCreatureManager.structureCreaturesMap.get(key);

		List<SpawnerData> newSpawnersList = new ArrayList<>();
		List<SpawnerData> theirSpawnersList = new ArrayList<>();

		if (p != null) {
			for (MobCategory ec : MobCategory.values()) {
				vCl = ec.getSerializedName();
				newSpawnersList.clear();
				theirSpawnersList.clear();
				for (int i = 0; i < p.size(); i++) {
					StructureCreatureItem sci = p.get(i);
//					bCl = sci.classification;
					if (sci.getClassification().toLowerCase().equals(vCl)) {
						@SuppressWarnings("deprecation")
						Optional<EntityType<?>> opt = Registry.ENTITY_TYPE
								.getOptional(new ResourceLocation(sci.getModAndMob()));
						if (opt.isPresent()) {
							SpawnerData newSpawner = new SpawnerData(opt.get(), Weight.of(sci.getSpawnWeight()),
									sci.getMinCount(), sci.getMaxCount());
							newSpawnersList.add(newSpawner);

							if (MyConfig.getDebugLevel() > 0) {
								System.out.println("Structure :" + key + " + rl#:" + structureLineNumber
										+ " SpawnBalanceUtility XXZZY: p.size() =" + p.size() + " Mob "
										+ sci.getModAndMob() + " Added to " + key + ".");
							}

						} else {
							System.out.println(reportlinenumber + "SpawnBalanceUtility ERROR: Mob " + sci.getModAndMob()
									+ " not in Entity Type Registry");
						}
					}
				}
				for (SpawnerData s : event.getEntitySpawns(ec)) {
					theirSpawnersList.add(s);
				}
				for (SpawnerData s : theirSpawnersList) {
					event.removeEntitySpawn(ec, s);
				}
				event.addEntitySpawns(ec, newSpawnersList);
			}

		}

	}

	private static void fixStructureSpawnValues(StructureSpawnListGatherEvent event) {

		List<SpawnerData> newSpawnersList = new ArrayList<>();
		List<SpawnerData> theirSpawnersList = new ArrayList<>();

		for (MobCategory ec : MobCategory.values()) {
			newSpawnersList.clear();
			theirSpawnersList.clear();
			for (SpawnerData s : event.getEntitySpawns(ec)) {
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
				theirSpawnersList.add(s);
				newSpawnersList.add(newS);
			}
			for (SpawnerData s : theirSpawnersList) {
				event.removeEntitySpawn(ec, s);
			}
			event.addEntitySpawns(ec, newSpawnersList);

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
			StructureSpawnOverride mobs = csf.spawnOverrides.get(MobCategory.MONSTER);
			if (mobs == null)
				continue;
			for (MobCategory ec : MobCategory.values()) {

				for (SpawnerData s : mobs.spawns().unwrap()) {
					if (s.type.getCategory() != ec) {
						continue;
					}
					if (MyConfig.isSuppressMinecraftMobReporting()) {
						if (s.type.getRegistryName().getNamespace().equals("minecraft")) {
							continue;
						}
					}
					if (MyConfig.isIncludedMod(s.type.getRegistryName().getNamespace())) {
						p.println(++structureLineNumber + ", " + csfName + ", " + ec + ", " + s.type.getRegistryName()
								+ ", " + s.getWeight().asInt() + ", " + s.minCount + ", " + s.maxCount);

					}

				}

			}
		}
		if (p != System.out) {
			p.close();
		}
	}

	public static void doStructureActions(MinecraftServer server) {

		RegistryAccess ra = server.registryAccess();

		@SuppressWarnings("deprecation")
		Registry<ConfiguredStructureFeature<?, ?>> csfreg = ra
				.registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);

		if (MyConfig.isBalanceStructureSpawnValues()) {
//			balanceStructureSpawnValues(csfreg);
		}
		if (MyConfig.isFixSpawnValues()) {
			fixStructureSpawnValues(csfreg);
		}
		if (MyConfig.isGenerateReport()) {
			generateStructureSpawnValuesReport(csfreg);
		}
	}

}
