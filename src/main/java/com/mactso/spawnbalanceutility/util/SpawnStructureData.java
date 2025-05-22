package com.mactso.spawnbalanceutility.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import com.mactso.spawnbalanceutility.Main;
import com.mactso.spawnbalanceutility.config.MyConfig;
import com.mactso.spawnbalanceutility.manager.StructureCreatureManager;
import com.mactso.spawnbalanceutility.manager.StructureCreatureManager.StructureCreatureItem;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.random.Weight;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraftforge.common.world.ModifiableStructureInfo.StructureInfo.Builder;
import net.minecraftforge.common.world.StructureSettingsBuilder.StructureSpawnOverrideBuilder;
import net.minecraftforge.event.server.ServerStartingEvent;

public class SpawnStructureData {

	private static final Logger LOGGER = LogManager.getLogger();

	static int structureEventNumber = 0;
	static Set<String> structuresProcessed = new HashSet<>();
	static int reportlinenumber = 0;

	static {
		Summary.clearStructure();
	}

	public static void initReports() {
		File fd = new File("config/spawnbalanceutility");
		if (!fd.exists())
			fd.mkdir();
		File fs = new File("config/spawnbalanceutility/StructMobWeight.rpt");
		if (fs.exists())
			fs.delete();
	}

	public static void onStructure(Holder<Structure> struct, Builder builder) {

		String threadname = Thread.currentThread().getName();

		if (MyConfig.isBalanceStructureSpawnValues()) {
			balanceStructureSpawnValues(struct, builder);
		}

		if (MyConfig.isFixSpawnValues()) {
			fixStructureSpawnValues(struct, builder);
		}

	}

	private static void fixStructureSpawnValues(Holder<Structure> struct, Builder builder) {

		structureEventNumber++;

		List<SpawnerData> newSpawnersList = new ArrayList<>();
		List<SpawnerData> theirSpawnersList = new ArrayList<>();
		int fixCount = 0;

		for (MobCategory ec : MobCategory.values()) {
			@Nullable
			StructureSpawnOverrideBuilder spob = builder.getStructureSettings().getSpawnOverrides(ec);
			if (spob == null)
				continue;

			newSpawnersList.clear();
			theirSpawnersList.clear();

			for (SpawnerData s : spob.getSpawns()) {
				int oldSpawnWeight = s.getWeight().asInt();
				int newSpawnWeight = Math.max(MyConfig.getMinSpawnWeight(), oldSpawnWeight);
				if (newSpawnWeight > 0) newSpawnWeight = Math.min(MyConfig.getMaxSpawnWeight(), newSpawnWeight);
				if (newSpawnWeight != oldSpawnWeight)
					fixCount++;
				SpawnerData newSpawner = new SpawnerData(s.type, Weight.of(newSpawnWeight), s.minCount,
						s.maxCount);
				newSpawnersList.add(newSpawner);

			}

			builder.getStructureSettings().removeSpawnOverrides(ec);
			
			StructureSpawnOverrideBuilder so = builder.getStructureSettings().getOrAddSpawnOverrides(ec);
			for (SpawnerData s : newSpawnersList) {
				so.addSpawn(s);
			}

		}
		Summary.setStructureFix(fixCount);

	}

	public static void generateStructureSpawnValuesReport(ServerStartingEvent event) {

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
		MinecraftServer server = event.getServer();
		RegistryAccess dynreg = server.registryAccess();
		Registry<Structure> structRegistry = dynreg.registryOrThrow(Registries.STRUCTURE);

		for (Structure struct : structRegistry) {
			String sn = structRegistry.getKey(struct).toString();

			p.println(++structlinenumber + ", " + sn + ", HEADING, header:ignore, 0, 0, 0");
			Map<MobCategory, StructureSpawnOverride> msi = struct.getModifiedStructureSettings().spawnOverrides();
			for (MobCategory mc : MobCategory.values()) {
				if (msi.get(mc) == null)
					continue;
				for (SpawnerData s : msi.get(mc).spawns().unwrap()) {
					if (MyConfig.isSuppressMinecraftMobReporting()) {
						if (EntityType.getKey(s.type).getNamespace().equals("minecraft")) {
							continue;
						}
					}
					String modname = EntityType.getKey(s.type).getNamespace();
					if (MyConfig.isIncludedMod(modname)) {
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

	private static void balanceStructureSpawnValues(Holder<Structure> struct, Builder builder) {

		String vCl = "";
		String key = null;
		structureEventNumber++;

		Optional<ResourceKey<Structure>> opKey = struct.unwrapKey(); // event.getStructure().getRegistryName().toString();
		if (opKey.isPresent()) {
			key = opKey.get().location().toString();
		}

		List<StructureCreatureItem> structureMobList = StructureCreatureManager.structureCreaturesMap.get(key);

		List<SpawnerData> newSpawnersList = new ArrayList<>();
		List<SpawnerData> theirSpawnersList = new ArrayList<>();
		int used = 0;

		if (structureMobList != null) {
			for (MobCategory v : MobCategory.values()) {

				vCl = v.getSerializedName();
				newSpawnersList.clear();
				theirSpawnersList.clear();

				for (int i = 0; i < structureMobList.size(); i++) {
					StructureCreatureItem sci = structureMobList.get(i);

					if (sci.getClassification().toLowerCase().equals(vCl)) {
						@SuppressWarnings("deprecation")
						Optional<EntityType<?>> opt = BuiltInRegistries.ENTITY_TYPE
								.getOptional(ResourceLocation.parse(sci.getModAndMob()));
						if (opt.isPresent()) {
							if (opt.get().getCategory() == MobCategory.MISC) {
								Utility.debugMsg(0, Main.MODID + " : " + sci.getModAndMob()
										+ " is MISC, minecraft is hard coded to change it to minecraft:pig in spawning data.");
							} else if (opt.get().getCategory() != v) {
								if (sci.getModAndMob().equals("minecraft:ocelot")) {

								} else {
									Utility.debugMsg(0,
											Main.MODID + " : " + sci.getModAndMob() + " Error, mob type "
													+ v + " different than defined for the type of mob "
													+ opt.get().getCategory());
								}
							}
							SpawnerData newSpawner = new SpawnerData(opt.get(), Weight.of(sci.getSpawnWeight()),
									sci.getMinCount(), sci.getMaxCount());
							newSpawnersList.add(newSpawner);

						} else {
							LOGGER.error(reportlinenumber + "SpawnBalanceUtility ERROR: Mob " + sci.getModAndMob()
									+ " not in Entity Type Registry");
						}
					}
				}

				@Nullable
				StructureSpawnOverrideBuilder spob = builder.getStructureSettings().getSpawnOverrides(v);
				if (spob != null) {
					builder.getStructureSettings().removeSpawnOverrides(v);
				}
				
				if (!newSpawnersList.isEmpty()) {
					StructureSpawnOverrideBuilder so = builder.getStructureSettings().getOrAddSpawnOverrides(v);
					for (SpawnerData s : newSpawnersList) {
						so.addSpawn(s);
					}
					used += newSpawnersList.size();
				}

			}

		}
		Summary.setStructureUsed(used);
	}

}
