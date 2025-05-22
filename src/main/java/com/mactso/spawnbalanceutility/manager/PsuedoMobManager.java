package com.mactso.spawnbalanceutility.manager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.spawnbalanceutility.config.MyConfig;
import com.mactso.spawnbalanceutility.util.Summary;
import com.mactso.spawnbalanceutility.util.Utility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

public class PsuedoMobManager {
	private static final Logger LOGGER = LogManager.getLogger();
	public static Map<String, List<PsuedoMobItem>> PsuedoMobsMap = new HashMap<>();
	public static Hashtable<String, PsuedoMobItem> PsuedoMobHashtable = new Hashtable<>();
	static int lastgoodline = 0;

	private static int minBuildHeight = -64;
	private static int maxBuildHeight = 256;

	private static boolean firstTime = true;
	
	public static void psuedoMobInit() {
		int spawnWeight = 0;
		int minCount = 0;
		int maxCount = 0;
		int linecount = 0;
		int addcount = 0;
		String errorField = "first";
		String line;

		if (PsuedoMobsMap.size() > 0) {
			return;
		}

		// this code only has an effect on linux because case doesn't matter on windows)
		File f = new File("config/spawnbalanceutility/PsuedoMobs.csv");
		if (!(f.exists())) {
			f = new File("config/spawnbalanceutility/PsuedoMobs.CSV");
		}

		try (InputStreamReader input = new InputStreamReader(new FileInputStream(f))) {
			BufferedReader br = new BufferedReader(input);
			while ((line = br.readLine()) != null) {
				if (line.charAt(0) == '*') {
					continue;
				}

				StringTokenizer st = new StringTokenizer(line, ",");
				linecount++;
				try {
					errorField = "linenumber";
					int lineNumber = Integer.parseInt(st.nextToken().trim());
					lastgoodline = lineNumber;
					errorField = "modAndBiome";
					String modAndBiome = st.nextToken().trim();
					errorField = "modAndMob";
					String modAndMob = st.nextToken().trim();
					errorField = "spawnWeight";
					spawnWeight = Integer.parseInt(st.nextToken().trim());
					errorField = "minCount";
					minCount = Integer.parseInt(st.nextToken().trim());
					errorField = "maxCount";
					maxCount = Integer.parseInt(st.nextToken().trim());

					if (minCount < 1) {
						minCount = 1;
					}
					if (maxCount > 32) {
						maxCount = 32;
					}
					if (minCount > maxCount) {
						minCount = maxCount;
					}

					if (spawnWeight > 0) {
						LOGGER.info("SpawnBalanceUtility Processing PsuedoMob: " + line);
						String key = modAndBiome;
						PsuedoMobItem pmi = new PsuedoMobItem(lineNumber, modAndBiome, modAndMob, spawnWeight, minCount,
								maxCount);
						Optional<EntityType<?>> eot = EntityType.byString(pmi.modAndMob);
						if (eot.isPresent()) {
							List<PsuedoMobItem> p = PsuedoMobsMap.get(key);
							if (p == null) {
								p = new ArrayList<>();
								PsuedoMobsMap.put(key, p);
							}
							// TODO maybe check for duplicates here later
							// for now okay as long as spawn weight > 0.
							p.add(pmi);
							addcount++;
						} else {
							LOGGER.info("Line:" + lineNumber + " PsuedoMob Configuration Error:" + pmi.modAndMob
									+ " not registered with Minecraft.");
						}
					}

				} catch (Exception e) {
					if (!(line.isEmpty())) {
						LOGGER.warn("SpawnBalanceUtility problem reading field " + errorField + " on " + linecount
								+ "th line of PsuedoMobs.csv.");
					} else if (MyConfig.getDebugLevel() > 0) {
						LOGGER.warn("SpawnBalanceUtility blank line at " + linecount + "th line of PsuedoMobs.csv.");
					}
				}
			}
			input.close();
		} catch (Exception e) {
			LOGGER.warn(
					"PsueodoMobs.csv not found in config/spawnbalanceutility/ (Remember you rename PsueodoMobs.rpt to create it). ");
			e.printStackTrace();
		}
		// Summary.setBiomeReadInfo(linecount, linecount - addcount);
	}

	public static boolean checkSpawnPsuedoMob(ServerPlayer sp) {
		Utility.debugMsg(1, "PsuedoMob Spawn Attempt for " + sp.getDisplayName().getString());

		if (PsuedoMobsMap.size() == 0) {
			return false;
		}
		
		BlockPos pos = sp.blockPosition();
		ServerLevel sLevel = sp.serverLevel();
		RandomSource rand = sLevel.getRandom();

		int boxsize = 64;
		AABB box = AABB.of(BoundingBox.fromCorners(pos.above(boxsize).north(boxsize).east(boxsize),
				pos.below(boxsize).south(boxsize).west(boxsize)));
		if (firstTime) {
			minBuildHeight = sLevel.getMinBuildHeight();
			maxBuildHeight = sLevel.getMaxBuildHeight();
			firstTime = false;
		}

		// generate a random spawning spot and look downwards for
		// an area to spawn with solid ground and air above it.
		int debug = 6;
		MutableBlockPos mSpawnPos = getRandomSolidSpawnPos(sp, box);
		if (mSpawnPos == null) {
			return false;
		}

		//
		PsuedoMobItem pmi = getRandomPsuedoMob(sLevel, mSpawnPos, rand);
		int debug4 = 4;
		if (pmi == null) { // legal if no pm's configured for biome.
			return false;
		} else {
			Utility.debugMsg(2, pmi.modAndMob + " psuedo mob chosen");

		}

		// check if the mob isn't near the player
		if (isPsuedoMobNearPlayer(sLevel, pmi, box)) {
			Utility.debugMsg(2, pmi.modAndMob + " is already near player " + sp.getDisplayName().getString() + ".");
			return false;
		}

		Utility.debugMsg(2, pmi.modAndMob + " is not nearby player.");
		Optional<EntityType<?>> eot = EntityType.byString(pmi.modAndMob);
		if (eot.isEmpty())
			return false; // checked in init() above so should never happen.
		EntityType<?> et = eot.get();
		if (isDarkEnoughToSpawnPsuedoMob(sLevel, et, mSpawnPos)) {
			Utility.debugMsg(2, pmi.modAndMob + " Light is valid.  Generating it.");
			return Utility.populateXEntityType(et, sp.serverLevel(), mSpawnPos, pmi.maxCount, false);
		} else {
			Utility.debugMsg(2, pmi.modAndMob + " failed brightness spawn test (this has a random element).");
		}

		return false;
	}

	private static boolean isDarkEnoughToSpawnPsuedoMob(ServerLevel sLevel, EntityType<?> et,
			MutableBlockPos mSpawnPos) {

		RandomSource rand = sLevel.getRandom();
		int skylight = sLevel.getBrightness(LightLayer.SKY, mSpawnPos);
		int blocklight = sLevel.getBrightness(LightLayer.BLOCK, mSpawnPos);
		DimensionType dimensiontype = sLevel.dimensionType();
		int dimSpawnLight = dimensiontype.monsterSpawnBlockLightLimit();

		if ((et.create(sLevel) instanceof Monster)) {
			if (blocklight < 1) {
				return true;
			} else if (blocklight <= dimSpawnLight) {
				return true;
			}

			if (sLevel.isThundering()) {
				skylight = Math.max(0, skylight - 6);
			}
			if (skylight > rand.nextInt(16)) {
				Utility.debugMsg(2, "Too Bright for Monster Psuedo Spawn: " + blocklight );
				return false;
			}
		} else { // Not Monster
			if ((blocklight > rand.nextInt(6))) {
				return true;
			} else {
				Utility.debugMsg(2, "Too Dark for Non-Monster Psuedo Spawn: " + blocklight );
				return false;
			}
		}

		return false;
	}

	private static PsuedoMobItem getRandomPsuedoMob(ServerLevel sLevel, MutableBlockPos mSpawnPos, RandomSource rand) {
		Holder<Biome> biomeHolder = sLevel.getBiome(mSpawnPos);
		Optional<ResourceKey<Biome>> biomeKey = biomeHolder.unwrapKey();
		if (biomeKey.isEmpty()) {
			return null;
		}

		String key = biomeKey.get().location().toString();
		List<PsuedoMobItem> p = PsuedoMobsMap.get(key);
		if (p == null) {
			Utility.debugMsg(2, "No Psuedo Mobs Configured for Biome " + key);
			return null;
		}
		Utility.debugMsg(2, "Randomly picking a Psuedo Mob for Biome " + key);
		int spawnTableTotalForBiome = 0;
		for (PsuedoMobItem pmTemp : p) {
			spawnTableTotalForBiome += pmTemp.spawnWeight;
		}

		int spawnRoll = rand.nextInt(spawnTableTotalForBiome);
		// TODO remove this when live.
		// spawnRoll = 127;
		int spawnweightvalue = 0;
		for (PsuedoMobItem pmTemp : p) {
			spawnweightvalue += pmTemp.spawnWeight;
			if (spawnRoll < spawnweightvalue) {
				return pmTemp;
			}
		}
		return null;
	}

	private static MutableBlockPos getRandomSolidSpawnPos(ServerPlayer sp, AABB box) {

		ServerLevel sLevel = sp.serverLevel();
		RandomSource rand = sLevel.getRandom();
		BlockPos pos = sp.blockPosition();
		int distAway = 24; // TODO 24 in production
		int randDist = 32; // TODO 32 in production

		int spX = distAway + rand.nextInt(randDist);
		if (rand.nextBoolean())
			spX *= -1;

		int spY = rand.nextInt(distAway + randDist);
		if (rand.nextBoolean())
			spY *= -1;
		spY = pos.getY() + spY;
		spY = Math.max(minBuildHeight + 5, spY);
		spY = Math.min(maxBuildHeight - 5, spY);

		int spZ = distAway + rand.nextInt(randDist);
		if (rand.nextBoolean())
			spZ *= -1;

		MutableBlockPos mSpawnPos = new MutableBlockPos((double) pos.getX() + spX, (double) spY,
				(double) pos.getZ() + spZ);
		spY = sLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, mSpawnPos).getY();
		mSpawnPos.setY(spY);

		// isSolid() might be changing to "isSolidRender"
		while (!sp.serverLevel().getBlockState(mSpawnPos.below()).isSolid()) {
			mSpawnPos.setY(mSpawnPos.getY() - 1);
			if (mSpawnPos.getY() < minBuildHeight + 5) {
				return null;
			}
		}

		if (!box.contains(mSpawnPos.getCenter())) {
			return null;
		}

		// isSolid() might be changing to "isSolidRender"
		if (sp.serverLevel().getBlockState(mSpawnPos.below()).isSolid()) {
			if (sp.serverLevel().getBlockState(mSpawnPos).isAir()) {
				if (sp.serverLevel().getBlockState(mSpawnPos.above(1)).isAir()) {
					Utility.debugMsg(2, "Generate Random Spawn Pos " + mSpawnPos);
					return mSpawnPos;
				}
			}
		}

		Utility.debugMsg(2, "Psuedo Mob Random Spawn Position " + mSpawnPos + " was invalid.");
		return null;
	}

	private static boolean isPsuedoMobNearPlayer(ServerLevel sLevel, PsuedoMobItem pmi, AABB box) {
		Optional<EntityType<?>> eot = EntityType.byString(pmi.modAndMob);
		if (eot.isEmpty()) {
			return false;
		}
		EntityType<?> et = eot.get();
		List<?> elt = sLevel.getEntities(et, box, Entity::isAlive);

		if (elt.isEmpty()) {
			return false;
		}

		return true;
	}

	//
	//
	//
	public static class PsuedoMobItem {
		int lineNumber;
		String modAndBiome;
		String classification;
		String modAndMob;
		int spawnWeight;
		int minCount;
		int maxCount;

		public PsuedoMobItem(int lineNumber, String modAndBiome, String modAndMob, int spawnWeight, int min, int max) {
			this.lineNumber = lineNumber;
			this.modAndBiome = modAndBiome;
			this.classification = classification;
			this.modAndMob = modAndMob;
			this.spawnWeight = spawnWeight;
			this.minCount = min;
			this.maxCount = max;
		}

		public String getModAndBiome() {
			return modAndBiome;
		}

		public String getClassification() {
			return classification;
		}

		public String getModAndMob() {
			return modAndMob;
		}

		public int getSpawnWeight() {
			return spawnWeight;
		}

		public int getMinCount() {
			return minCount;
		}

		public int getMaxCount() {
			return maxCount;
		}

	}

}
