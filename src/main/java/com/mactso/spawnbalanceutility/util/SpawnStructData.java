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

import com.mactso.spawnbalanceutility.config.MyConfigs;
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
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedRandomList;
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
		if (fs.exists())
			fs.delete();
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
		
		if (MyConfigs.isBalanceStructureSpawnValues()) {
			balanceStructureSpawnValues(csfreg);
		}
		if (MyConfigs.isFixSpawnValues()) {
			fixStructureSpawnValues(csfreg);
		}
		if (MyConfigs.isGenerateReport()) {
			generateStructureSpawnValuesReport(csfreg);
		}
	}

	private static void balanceStructureSpawnValues(Registry<Structure> csfreg) {

		List<SpawnerData> newSpawnEntriesList = new ArrayList<>();

		
		for (Entry<ResourceKey<Structure>, Structure> csf : csfreg.entrySet()) {

			ResourceKey<Structure> csfKey = csf.getKey();
			String csfIdentifier = csfKey.location().toString();
			

			List<StructureCreatureItem> structureMobList = StructureCreatureManager.structureCreaturesMap
					.get(csfIdentifier);

			Map<MobCategory, StructureSpawnOverride> newMap = new HashMap<>();
			if (structureMobList != null) {
				for (MobCategory mc : MobCategory.values()) {
					String vCl = mc.toString();  // fixed from vCl = v.getSerializedName();
					newSpawnEntriesList.clear();
					for (int i = 0; i < structureMobList.size(); i++) {
						StructureCreatureItem sci = structureMobList.get(i);

						if (sci.getClassification().equalsIgnoreCase(vCl)) {
							
							Optional<EntityType<?>> optRef = BuiltInRegistries.ENTITY_TYPE
									.getOptional(ResourceLocation.parse((sci.getModAndMob())));

							if (optRef.isPresent()) {

								// original
								SpawnerData newS = new SpawnerData(optRef.get(), 
										Weight.of(sci.getSpawnWeight()),
										sci.getMinCount(),
										sci.getMaxCount());
								newSpawnEntriesList.add(newS);
							}

						}

					}
					
					newMap.put(mc, new StructureSpawnOverride(BoundingBoxType.STRUCTURE, WeightedRandomList.create(newSpawnEntriesList)));

				}
				if (!newMap.isEmpty()) {
					Structure workStruct = csf.getValue();
					try {
						StructureSettings cfg = (StructureSettings) fieldStructConfig.get(workStruct);
						fieldStructConfig.set(workStruct,
								new StructureSettings (cfg.biomes(), newMap, cfg.step(), cfg.terrainAdaptation()));
					} catch (Exception e) {
						if (MyConfigs.getDebugLevel() > 0) {
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

		List<SpawnerData> newSpawnersList = new ArrayList<>();

		for (Entry<ResourceKey<Structure>, Structure> csf : csfreg.entrySet()) {

			ResourceKey<Structure> csfKey = csf.getKey();
			String csfIdentifier = csfKey.location().toString();
			String csfName = csfKey.toString();

			Map<MobCategory, StructureSpawnOverride> newMap = new HashMap<>();
			Structure workStruct = csf.getValue();
			Map<MobCategory, StructureSpawnOverride> mobs = workStruct.spawnOverrides();
			if (mobs == null)
				continue;
			for (MobCategory mc : MobCategory.values()) {
				StructureSpawnOverride old = mobs.get(mc);
				if (old == null) {
					continue;
				}
				WeightedRandomList<SpawnerData> oldwrl = old.spawns();
				newSpawnersList.clear();
				for (SpawnerData s : oldwrl.unwrap()) {
					int newSpawnWeight = s.getWeight().asInt();
					if (newSpawnWeight < MyConfigs.getMinSpawnWeight()) {
						if ((newSpawnWeight > 1) && (newSpawnWeight * 10 < MyConfigs.getMaxSpawnWeight())) {
							newSpawnWeight = newSpawnWeight * 10;
						} else {
							newSpawnWeight = MyConfigs.getMinSpawnWeight();
						}
					}
					if (newSpawnWeight > MyConfigs.getMaxSpawnWeight()) {
						newSpawnWeight = MyConfigs.getMaxSpawnWeight();
					}
					SpawnerData newS = new SpawnerData(s.type, Weight.of(newSpawnWeight), s.minCount, s.maxCount);
					newSpawnersList.add(newS);
				}
				newMap.put(mc, new StructureSpawnOverride(BoundingBoxType.STRUCTURE, WeightedRandomList.create(newSpawnersList)));
			}

			try {
				StructureSettings cfg = (StructureSettings) fieldStructConfig.get(workStruct);
				fieldStructConfig.set(workStruct,
						new StructureSettings(cfg.biomes(), newMap, cfg.step(), cfg.terrainAdaptation()));
			} catch (Exception e) {
				LOGGER.error("Failed to fix " + csfName + " spawnentries map.  Set debugValue to 1 to see stacktrace.");
			}
		}

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
		p.println("* Spawn Balance Utility (SBU) will use this file ONLY if it is renamed to StructMobWeight.csv.");
		p.println("* Lines starting with '*' are comments and ignored");
		p.println("* It allows you to add and remove mobs and adjust their spawnweights for Structures");
		p.println("* like shipwrecks, nether fortresses, water monuments, etc.");
		p.println("* ");

		int structlinenumber = 0;
		for (Structure struct : structRegistry) {
			String sn = structRegistry.getKey(struct).toString();

			p.println(++structlinenumber + ", " + sn + ", HEADING, header:ignore, 0, 0, 0");
			Map<MobCategory, StructureSpawnOverride> msi = struct.spawnOverrides();
			for (MobCategory mc : MobCategory.values()) {
				if (msi.get(mc) == null)
					continue;
				for (SpawnerData s : msi.get(mc).spawns().unwrap()) {
					if (MyConfigs.isSuppressMinecraftMobReporting()) {
						if (EntityType.getKey(s.type).getNamespace().equals("minecraft")) {
							continue;
						}
					}
					String modname = EntityType.getKey(s.type).getNamespace();
					if (MyConfigs.isIncludedMod(modname)) {
						p.println(
								++structlinenumber + ", " + sn + ", " + mc + ", " + EntityType.getKey(s.type).toString()
										+ ", " + s.getWeight() + ", " + s.minCount + ", " + s.maxCount);
					}

				}
			}

		}

		if (p != System.out) {
			p.close();
		}
	}

}
