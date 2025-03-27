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
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.spawnbalanceutility.config.MyConfig;
import com.mactso.spawnbalanceutility.manager.StructureCreatureManager;
import com.mactso.spawnbalanceutility.manager.StructureCreatureManager.StructureCreatureItem;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.collection.Weight;
import net.minecraft.world.StructureSpawns;
import net.minecraft.world.StructureSpawns.BoundingBox;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.biome.SpawnSettings.SpawnEntry;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.Structure.Config;

public class SpawnStructData {

	private static Field fieldStructConfig = null;

	private static final Logger LOGGER = LogManager.getLogger();
	public static final Pool<SpawnSettings.SpawnEntry> SBU_FIX_EMPTY_MOB_LIST = Pool.of();

	static int structureLineNumber = 0;
	static Set<String> structuresProcessed = new HashSet<>();
	static int reportlinenumber = 0;

	static int structureEventNumber = 0;

	static {
		initReports();

		// minecraft/world/level/levelgen/feature/ConfiguredStructureFeature/f_209744_
		// net/minecraft/world/level/levelgen/feature/Structure/config/
		// from there, I'll get the spawnOverrides

		// mappings.jar entry for /Biome -
		// Fabric : f Lcbr$b; l field_9329 category
		// note- must have semicolon at end of type "Lcbr$b;"

		try {
			MappingResolver mapping = FabricLoader.getInstance().getMappingResolver();
			String fieldName = mapping.mapFieldName("intermediary", "net.minecraft.class_3195", "field_38429",
					"Lnet/minecraft/class_3195$class_7302;");
			fieldStructConfig = Structure.class.getDeclaredField(fieldName);
			fieldStructConfig.setAccessible(true);
		} catch (Exception e) {
			LOGGER.error("XXX Unexpected Reflection Failure trying set Structure.Config record accessible");
		}
	}

//
	public static void initReports() {
		File fd = new File("config/spawnbalanceutility");
		if (!fd.exists())
			fd.mkdir();
		File fs = new File("config/spawnbalanceutility/StructMobWeight.txt");
		if (fs.exists())
			fs.delete();
	}

	public static void doStructureActions(MinecraftServer server) {

		DynamicRegistryManager dynreg = server.getRegistryManager();
		Registry<Structure> csfreg = dynreg.get(RegistryKeys.STRUCTURE);
		initReports();

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

	private static void balanceStructureSpawnValues(Registry<Structure> csfreg) {

		List<SpawnEntry> newSpawnEntriesList = new ArrayList<>();

		for (Entry<RegistryKey<Structure>, Structure> csf : csfreg.getEntrySet()) {

			RegistryKey<Structure> csfKey = csf.getKey();

			String csfIdentifier = csfKey.getValue().toString();

			List<StructureCreatureItem> creaturesInStructure = StructureCreatureManager.structureCreaturesMap
					.get(csfIdentifier);

			Map<SpawnGroup, StructureSpawns> newMap = new HashMap<>();
			if (creaturesInStructure != null) {
				for (SpawnGroup mc : SpawnGroup.values()) {
					String vCl = mc.getName();
					newSpawnEntriesList.clear();
					for (int i = 0; i < creaturesInStructure.size(); i++) {
						StructureCreatureItem sci = creaturesInStructure.get(i);

						if (sci.getClassification().toLowerCase().equals(vCl)) {
							Optional<EntityType<?>> opt = Registries.ENTITY_TYPE
									.getOrEmpty(Identifier.of(sci.getModAndMob()));
							if (opt.isPresent()) {
								SpawnEntry newS = new SpawnEntry(opt.get(), Weight.of(sci.getSpawnWeight()),
										sci.getMinCount(), sci.getMaxCount());
								newSpawnEntriesList.add(newS);
							}

						}

					}
					newMap.put(mc, new StructureSpawns(BoundingBox.STRUCTURE, Pool.of(newSpawnEntriesList)));

				}
				if (!newMap.isEmpty()) {
					Structure workStruct = csf.getValue();
					try {
						Config cfg = (Config) fieldStructConfig.get(workStruct);
						fieldStructConfig.set(workStruct,
								new Config(cfg.biomes(), newMap, cfg.step(), cfg.terrainAdaptation()));
					} catch (Exception e) {
						if (MyConfig.getDebugLevel() > 0) {
							e.printStackTrace();
						} else {
							LOGGER.error("Failed to balance " + csfIdentifier
									+ " spawnentries map.  Set debugValue to 1 to see stacktrace.");
						}
					}
				}
			}
		}
	}

	private static void fixStructureSpawnValues(Registry<Structure> csfreg) {

		List<SpawnEntry> newSpawnersList = new ArrayList<>();

		for (Entry<RegistryKey<Structure>, Structure> csf : csfreg.getEntrySet()) {

			RegistryKey<Structure> csfKey = csf.getKey();
			String csfIdentifier = csfKey.getValue().toString();
			String csfName = csfKey.toString();

			Map<SpawnGroup, StructureSpawns> newMap = new HashMap<>();
			Structure workStruct = csf.getValue();
			Map<SpawnGroup, StructureSpawns> mobs = workStruct.getStructureSpawns();
			if (mobs == null)
				continue;
			for (SpawnGroup mc : SpawnGroup.values()) {
				StructureSpawns old = mobs.get(mc);
				if (old == null) {
					continue;
				}
				Pool<SpawnEntry> oldwrl = old.spawns();
				newSpawnersList.clear();
				for (SpawnEntry s : oldwrl.getEntries()) {
					int newSpawnWeight = s.getWeight().getValue();
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
					SpawnEntry newS = new SpawnEntry(s.type, Weight.of(newSpawnWeight), s.minGroupSize, s.maxGroupSize);
					newSpawnersList.add(newS);
				}
				newMap.put(mc, new StructureSpawns(BoundingBox.STRUCTURE, Pool.of(newSpawnersList)));
			}

			try {
				Config cfg = (Config) fieldStructConfig.get(workStruct);
				fieldStructConfig.set(workStruct,
						new Config(cfg.biomes(), newMap, cfg.step(), cfg.terrainAdaptation()));
			} catch (Exception e) {
				LOGGER.error("Failed to fix " + csfName + " spawnentries map.  Set debugValue to 1 to see stacktrace.");
			}
		}

	}

	private static void generateStructureSpawnValuesReport(Registry<Structure> csfreg) {

		PrintStream p = null;
		try {
			p = new PrintStream(new FileOutputStream("config/spawnbalanceutility/StructMobWeight.txt", false));
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (p == null) {
			p = System.out;
		}

		for (Entry<RegistryKey<Structure>, Structure> csf : csfreg.getEntrySet()) {
			RegistryKey<Structure> csfKey = csf.getKey();
			String csfIdentifier = csfKey.getValue().toString();
			Structure workStruct = csf.getValue();
			Config cfg = null;
			try {
				cfg = (Config) fieldStructConfig.get(workStruct);
			} catch (Exception e) {
				if (MyConfig.getDebugLevel() > 0) {
					e.printStackTrace();
				} else {
					LOGGER.error("Failed to load " + csfIdentifier
							+ " Config/SpawnGroup map.  Set debugValue to 1 to see stacktrace.");
				}
			}
			if (cfg == null) {
				LOGGER.error("error:" + csfIdentifier + " Config/SpawnGroup map is null.");
				continue;
			}
			p.println(++structureLineNumber + ", " + csfIdentifier + ", HEADING, header:ignore, 0, 0, 0");

			// mob category ( "MONSTER", "AMBIENT", etc.)
			for (SpawnGroup spawnGroup : SpawnGroup.values()) {
				StructureSpawns mobs = cfg.spawnOverrides().get(spawnGroup);
				if (mobs == null)
					continue;
				for (SpawnEntry s : mobs.spawns().getEntries()) {
					@SuppressWarnings("deprecation")
					String modName = s.type.getRegistryEntry().getKey().get().getValue().getNamespace();
					if (MyConfig.isSuppressMinecraftMobReporting()) {
						if (modName.equals("minecraft")) {
							continue;
						}
					}

					@SuppressWarnings("deprecation")
					String mobIdentifier = s.type.getRegistryEntry().getKey().get().getValue().toString();
// note this relies on the forge "modslist" feature which I don't know how to do in fabric if possible at all.
//					if (MyConfig.isIncludedMod(modName)) {
					p.println(++structureLineNumber + ", " + csfIdentifier + ", " + spawnGroup + ", " + mobIdentifier
							+ ", " + s.getWeight().getValue() + ", " + s.minGroupSize + ", " + s.maxGroupSize);

//					}

				}

			}
		}
		if (p != System.out) {
			p.close();
		}
	}

}
