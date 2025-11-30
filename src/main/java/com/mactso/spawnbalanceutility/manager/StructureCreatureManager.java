package com.mactso.spawnbalanceutility.manager;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.spawnbalanceutility.config.MyConfig;
import com.mactso.spawnbalanceutility.util.Summary;
import com.mactso.spawnbalanceutility.util.Utility;

public class StructureCreatureManager {

	private static final Logger LOGGER = LogManager.getLogger();

	public static Map<String, List<StructureCreatureItem>> structureCreaturesMap = new HashMap<>();
	private static int lastgoodlinenumber = 0;
	private static String lastgoodline = "Start of the file.  There were no prior good lines.";

	public static void structureCreatureInit(Path csvPath) {

		int linecount = 0;
		int lineNumber = 0;
		int addcount = 0;
		int blankline = 0;
		int commentcount = 0;
		String errorField = "first";
		String line;

		if (structureCreaturesMap.size() > 0) {
			return;
		}

		// Check if the file exists
		File f = csvPath.toFile();
		if (!f.exists()) {
			// Optional: case-insensitive fallback (Linux)
			Path fallbackPath = csvPath.getParent().resolve(csvPath.getFileName().toString().toUpperCase());
			f = fallbackPath.toFile();
			if (!f.exists()) {
				LOGGER.info("CSV file not found: " + csvPath);
				return;
			} else {
				csvPath = fallbackPath; // use the fallback
			}
		}

		// Now use csvPath to read the file

		try (BufferedReader br = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
			
			int spawnWeight = 0;
			int minCount = 0;
			int maxCount = 0;
			
			while ((line = br.readLine()) != null) {

				line = line.trim();
				if (line.isEmpty()) {
					blankline++;
					continue;
				}
				if (line.charAt(0) == '*') {
					commentcount++;
					continue;
				}

				String[] parts = line.split(",", -1);
				if (parts.length < 7) {
					LOGGER.warn("Malformed line in StructMobWeight.csv at line " + linecount + ": " + line);
					continue;
				}

				linecount++;
				try {
					errorField = "linenumber";
					lineNumber = Integer.parseInt(parts[0].trim());
					lastgoodlinenumber = lineNumber;

					errorField = "modAndStructure";
					String modAndStructure = parts[1].trim();

					errorField = "mobCategory";
					String mobCategory = parts[2].trim();

					errorField = "modAndMob";
					String modAndMob = parts[3].trim();

					errorField = "spawnWeight";
					spawnWeight = Integer.parseInt(parts[4].trim());

					errorField = "minCount";
					minCount = Integer.parseInt(parts[5].trim());

					errorField = "maxCount";
					maxCount = Integer.parseInt(parts[6].trim());

					// clamp counts
					minCount = Math.max(MyConfig.MOB_MIN_COUNT, minCount);
					maxCount = Math.min(MyConfig.MOB_MAX_COUNT, maxCount);
					if (minCount > maxCount) {
						minCount = maxCount;
					}

					String key = modAndStructure;
					if (spawnWeight > 0) {
						// DEBUG LEVEL 1: medium-level summary of processed CSV lines (off by default)
						Utility.debugMsg(1,
								lineNumber + ", " + lastgoodline + ", " + modAndStructure + ", " + mobCategory + ", "
										+ modAndMob + ", " + spawnWeight + ", " + minCount + ", " + maxCount);
						StructureCreatureItem bci = new StructureCreatureItem(lineNumber, modAndStructure, mobCategory,
								modAndMob, spawnWeight, minCount, maxCount);
						List<StructureCreatureItem> structureMobList = structureCreaturesMap.get(key);
						if (structureMobList == null) {
							structureMobList = new ArrayList<>();
							structureCreaturesMap.put(key, structureMobList);
						}
						// TODO maybe check for duplicates here later
						// for now okay as long as spawn weight > 0.
						structureMobList.add(bci);
						addcount++;
					}
					lastgoodline = line;
				} catch (NumberFormatException e) {
					LOGGER.warn("Number format problem reading " + errorField + " on line " + linecount
							+ " with line number " + lineNumber + " of StructMobWeight.csv: " + line);
					LOGGER.warn("The last good line was: " + lastgoodline);

				} catch (Exception e) {
					LOGGER.warn("Unexpected problem reading line " + linecount + " with line number " + lineNumber
							+ " of StructMobWeight.csv: " + line, e);
					LOGGER.warn("The last good line was: " + lastgoodline);
				}
			}
		} catch (Exception e) {
			LOGGER.warn(
					"StructMobWeight.csv not found in config/spawnbalanceutility/ (Remember you rename StructMobWeight.rpt to create it). ");
			e.printStackTrace();
		}

		linecount -= (blankline + commentcount);
		Summary.setStructureReadInfo(linecount, linecount - addcount);
	}

	public static class StructureCreatureItem {
		int lineNumber;
		String modAndStructure;
		String mobCategory;
		String modAndMob;
		int spawnWeight;
		int minCount;
		int maxCount;

		public StructureCreatureItem(int lineNumber, String modAndStructure, String mobCategory, String modAndMob,
				int spawnWeight, int min, int max) {
			this.lineNumber = lineNumber;
			this.modAndStructure = modAndStructure;
			this.mobCategory = mobCategory;
			this.modAndMob = modAndMob;
			this.spawnWeight = spawnWeight;
			this.minCount = min;
			this.maxCount = max;
		}

		public String getModAndStructure() {
			return modAndStructure;
		}

		public String getMobCategory() {
			return mobCategory;
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
