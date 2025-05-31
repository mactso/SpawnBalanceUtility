package com.mactso.spawnbalanceutility.manager;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.spawnbalanceutility.Main;
import com.mactso.spawnbalanceutility.config.MyConfig;
import com.mactso.spawnbalanceutility.util.Utility;

import net.minecraft.entity.SpawnGroup;

public class MobMassAdditionManager {

	public static Hashtable<String, MassAdditionMobItem> massAdditionMobsHashtable = new Hashtable<>();
	static int lastgoodline = 0;
	private static final Logger LOGGER = LogManager.getLogger();

	public static String CATEGORY_ALL = "A";
	public static String CATEGORY_OVERWORLD = "O";
	public static String CATEGORY_NETHER = "N";
	public static String CATEGORY_THEEND = "E";

	public static List<MassAdditionMobItem> getFilteredList(SpawnGroup v, String category) {
		List<MassAdditionMobItem> ma = new ArrayList<>();
		for (MassAdditionMobItem m : massAdditionMobsHashtable.values()) {
			if (v.getName().equalsIgnoreCase(m.getClassification())) {
				if (m.getCategory().equals(CATEGORY_ALL)) {
					ma.add(m);
				} else if (category == Utility.NETHER) {
					if (m.getCategory().equals(CATEGORY_NETHER)) {
						ma.add(m);
					}
				} else if (category == Utility.THEEND) {
					if (m.getCategory().equals(CATEGORY_THEEND)) {
						ma.add(m);
					}
				} else if (category != Utility.NONE) {
					if (m.getCategory().equals(CATEGORY_OVERWORLD)) {
						ma.add(m);
					}

				}
			}
		}
		return ma;
	}

	public static void massAdditionMobsInit() {
		int spawnWeight = 0;
		String category;
		int minCount = 0;
		int maxCount = 0;
		int linecount = 0;
		int commentcount = 0;
		String errorField = "first";
		String line;

		if (massAdditionMobsHashtable.size() > 0) {
			return;
		}
		try (InputStreamReader input = new InputStreamReader(
				new FileInputStream("config/spawnbalanceutility/MassAdditionMobs.csv"))) {
			BufferedReader br = new BufferedReader(input);
			int lineNumber = 0;
			while ((line = br.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line, ",");
				linecount++;
				
				if (line.isEmpty()) {
					continue;
				} 
		
				if (line.trim().isEmpty()) {
					continue;
				} 
				
				if (line.charAt(0)=='*') {
					continue;
				}
				if (line.charAt(0)=='*') {
					commentcount++;
					continue;
				}
				
				try {
					errorField = "comment line test";
					String token = st.nextToken().trim();
						errorField = "linenumber";
						lineNumber = Integer.parseInt(token);
						lastgoodline = lineNumber;
						errorField = "category";
						category = st.nextToken().trim();
						errorField = "classification";
						String classification = st.nextToken().trim();
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
						if (maxCount > 12) {
							maxCount = 12;
						}
						if (minCount > maxCount) {
							minCount = maxCount;
						}
						String key = modAndMob;
						if (!(validClassification(classification))) {
							Utility.debugMsg(0, Main.MOD_ID + " Invalid classification " + classification + " on "
									+ linecount + "th line of MassAdditionMobs.csv.");
						} else if (spawnWeight > 0) {
							MassAdditionMobItem bci = new MassAdditionMobItem(lineNumber, category, classification,
									modAndMob, spawnWeight, minCount, maxCount);
							massAdditionMobsHashtable.put(key, bci); // uses last one in file if dupes
						}


				} catch (Exception e) {
					if (!(line.isEmpty())) {
						LOGGER.warn("SpawnBalanceUtility Error reading field " + errorField + " on " + linecount
								+ "th line of MassAdditionMobs.csv.");
					} else if (MyConfig.getDebugLevel() > 0) {
						LOGGER.warn("SpawnBalanceUtility Warning blank line at " + linecount
								+ "th line of MassAdditionMobs.csv.");
					}
				}
			}
			input.close();
		} catch (Exception e) {
			LOGGER.info(
					"SpawnBalanceUtility: Mass Addition Not Configured.  File config/spawnbalanceutility/MassAdditionMobs.csv not found.");
			// e.printStackTrace();
		}
		linecount -= commentcount;
//		Summary.setMassAddReadInfo(linecount, linecount - massAdditionMobsHashtable.size());

	}

	public static boolean validClassification(String classification) {
		for (SpawnGroup e : SpawnGroup.values()) {
			if (classification.equalsIgnoreCase(e.getName())) {
				return true;
			}
		}
		return false;
	}

	public static class MassAdditionMobItem {
		int lineNumber;
		String category;
		String classification;
		String modAndMob;
		int spawnWeight;
		int minCount;
		int maxCount;

		public MassAdditionMobItem(int lineNumber, String category, String classification, String modAndMob,
				int spawnWeight, int min, int max) {
			this.lineNumber = lineNumber;
			this.category = category;
			this.classification = classification;
			this.modAndMob = modAndMob;
			this.spawnWeight = spawnWeight;
			this.minCount = min;
			this.maxCount = max;

		}

		public String getCategory() {
			return category;
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
