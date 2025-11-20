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
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.Structure.StructureSettings;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride.BoundingBoxType;

public class SpawnStructData {

	private static Field fieldStructConfig = null;

	private static final Logger LOGGER = LogManager.getLogger();

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
			// fabric is still intermediate even with official mapping.
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
		File fs = new File("config/spawnbalanceutility/StructMobWeight.rpt");
		if (fs.exists()) {
			if (!fs.delete()) {
				LOGGER.warn("config/spawnbalanceutility/StructMobWeight.rpt was being edited and couldn't be deleted.");
			}
			
		}
	}

	public static void doStructureActions(MinecraftServer server) {

		RegistryAccess dynreg = server.registryAccess();
		Optional<Registry<Structure>> optStructReg = dynreg.lookup(Registries.STRUCTURE);

		initReports();

		if (optStructReg.isEmpty()) {
			LOGGER.error("Hard Error : Structure Registry Missing");
			return;
		}

		Registry<Structure> csfreg = optStructReg.get();

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

	//
	// For all Structures, replace the default SpawnerData with the configured
	// SpawnerData in StructMobWeight.csv
	//

	private static void balanceStructureSpawnValues(Registry<Structure> structureRegistry) {

		for (Entry<ResourceKey<Structure>, Structure> structureEntry : structureRegistry.entrySet()) {
			List<StructureCreatureItem> structureMobList = StructureCreatureManager.structureCreaturesMap
					.get(structureEntry.getKey().location().toString());
			if (structureMobList == null)
				continue;
			balanceOneStructureEntrySpawnValues(structureEntry, structureMobList);
		}
	}

	//
	// For one Structure, replace the default SpawnerData with the configured
	// SpawnerData in StructMobWeight.csv
	//
	private static void balanceOneStructureEntrySpawnValues(Entry<ResourceKey<Structure>, Structure> structureEntry,
			List<StructureCreatureItem> structureMobList) {

		Map<MobCategory, StructureSpawnOverride> newMap = new HashMap<>();

		for (MobCategory mc : MobCategory.values()) {
			List<Weighted<SpawnerData>> newSpawnersList = new ArrayList<>();
			for (int i = 0; i < structureMobList.size(); i++) {

				StructureCreatureItem sci = structureMobList.get(i);
				if (sci.getMobCategory().equalsIgnoreCase(mc.toString())) {

					Optional<EntityType<?>> optEntityType = BuiltInRegistries.ENTITY_TYPE
							.getOptional(ResourceLocation.parse((sci.getModAndMob())));
					if (optEntityType.isPresent()) {

						Weighted<SpawnerData> newSpawner = new Weighted<SpawnerData>(
								new SpawnerData(optEntityType.get(), sci.getMinCount(), sci.getMaxCount()),
								sci.getSpawnWeight());
						newSpawnersList.add(newSpawner);
					} else {
						LOGGER.warn("Could not find " + sci.getModAndMob() + " In the BuiltInRegistries.ENTITY_TYPE.");
					}
				}
			}

			if (!newSpawnersList.isEmpty()) {
				StructureSpawnOverride override = new StructureSpawnOverride(BoundingBoxType.STRUCTURE,
						WeightedList.of(newSpawnersList));
				newMap.put(mc, override);
			}

		}

		if (newMap.isEmpty())
			return;

		Structure workStruct = structureEntry.getValue();
		try {
			StructureSettings cfg = (StructureSettings) fieldStructConfig.get(workStruct);
			fieldStructConfig.set(workStruct,
					new StructureSettings(cfg.biomes(), newMap, cfg.step(), cfg.terrainAdaptation()));
		} catch (Exception e) {
			if (MyConfig.getDebugLevel() > 0) {
				e.printStackTrace();
			} else {
				LOGGER.error("Failed to balance " + structureEntry.getKey().location().toString()
						+ " spawnentries map.  Set debugValue to 1 to see stacktrace.");
			}
		}
		return;
	}

	private static void fixStructureSpawnValues(Registry<Structure> csfreg) {

		for (Entry<ResourceKey<Structure>, Structure> csf : csfreg.entrySet()) {
			structureEventNumber++;
			ResourceKey<Structure> csfKey = csf.getKey();
			String csfIdentifier = csfKey.location().toString();
			String csfName = csfKey.toString();

			Structure workStruct = csf.getValue();
			Map<MobCategory, StructureSpawnOverride> mobs = workStruct.spawnOverrides();
			if (mobs == null)
				continue;
			fixOneStructureSpawnValues(csfName, workStruct, mobs);
		}

		return;

	}

	private static void fixOneStructureSpawnValues(String csfName, Structure workStruct,
			Map<MobCategory, StructureSpawnOverride> mobs) {

		Map<MobCategory, StructureSpawnOverride> newMap = new HashMap<>();

		for (MobCategory mc : MobCategory.values()) {

			StructureSpawnOverride old = mobs.get(mc);
			if (old == null) {
				continue;
			}
			WeightedList<SpawnerData> oldwrl = old.spawns();

			List<Weighted<SpawnerData>> newSpawnersList = new ArrayList<>();
			for (Weighted<SpawnerData> s : oldwrl.unwrap()) {
				int oldSpawnWeight = s.weight();
				int newSpawnWeight = Math.max(MyConfig.getMinSpawnWeight(), oldSpawnWeight);
				if (newSpawnWeight > 0)
					newSpawnWeight = Math.min(MyConfig.getMaxSpawnWeight(), newSpawnWeight);

				newSpawnersList.add(new Weighted<SpawnerData>(s.value(), newSpawnWeight));

			}
			if (!newSpawnersList.isEmpty()) {
				StructureSpawnOverride override = new StructureSpawnOverride(BoundingBoxType.STRUCTURE,
						WeightedList.of(newSpawnersList));
				newMap.put(mc, override);
			}
		}

		try {
			StructureSettings cfg = (StructureSettings) fieldStructConfig.get(workStruct);
			fieldStructConfig.set(workStruct,
					new StructureSettings(cfg.biomes(), newMap, cfg.step(), cfg.terrainAdaptation()));
		} catch (Exception e) {
			LOGGER.error("Failed to fix " + csfName + " spawnentries map.  Set debugValue to 1 to see stacktrace.");
		}

		return;

	}

	private static void generateStructureSpawnValuesReport(Registry<Structure> structRegistry) {

		PrintStream p = null;
		try {
			p = new PrintStream(new FileOutputStream("config/spawnbalanceutility/StructMobWeight.rpt", false));
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (p == null) {
			p = System.out;
		}
		p.println("* This is the StructMobWeight report file that is output every time the server starts.");
		p.println("* ");
		p.println("* documentation, structure, rowtype, mod:mobname, spwt, min, max");

		p.println("* Spawn Balance Utility (SBU) will use this file ONLY if it is renamed to StructMobWeight.csv.");
		p.println("* Lines starting with '*' are comments and ignored");
		p.println("* It allows you to add and remove mobs and adjust their spawnweights for Structures");
		p.println("* like shipwrecks- nether fortresses- water monuments- etc.");
		p.println("* ");

		int structlinenumber = 0;
		for (Structure struct : structRegistry) {
			String sn = structRegistry.getKey(struct).toString();

			p.println(++structlinenumber + ", " + sn + ", HEADING, header:ignore, 0, 0, 0");
			Map<MobCategory, StructureSpawnOverride> msi = struct.spawnOverrides();
			for (MobCategory mc : MobCategory.values()) {
				if (msi.get(mc) == null)
					continue;
				for (Weighted<SpawnerData> s : msi.get(mc).spawns().unwrap()) {
					if (MyConfig.isSuppressMinecraftMobReporting()) {
						if (EntityType.getKey(s.value().type()).getNamespace().equals("minecraft")) {
							continue;
						}
					}
					String modname = EntityType.getKey(s.value().type()).getNamespace();
					if (MyConfig.isIncludedMod(modname)) {
						p.println(++structlinenumber + ", " + sn + ", " + mc + ", "
								+ EntityType.getKey(s.value().type()).toString() + ", " + s.weight() + ", "
								+ s.value().minCount() + ", " + s.value().maxCount());
					}

				}
			}

		}

		if (p != System.out) {
			p.close();
		}
	}

}
