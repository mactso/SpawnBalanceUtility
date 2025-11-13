package com.mactso.spawnbalanceutility.manager;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.spawnbalanceutility.config.MyConfigs;
import com.mactso.spawnbalanceutility.utility.Utility;

import net.minecraft.world.entity.MobCategory;

public class MobMassAdditionManager {

	private static final Logger LOGGER = LogManager.getLogger();

	public static Hashtable<String, MassAdditionMobItem> massAdditionMobsHashtable = new Hashtable<>();
	static int lastgoodline = 0;

	public static String CATEGORY_ALL = "A";
	public static String CATEGORY_OVERWORLD = "O";
	public static String CATEGORY_NETHER = "N";
	public static String CATEGORY_THEEND = "E";

	public static List<MassAdditionMobItem> getFilteredList(MobCategory v, String category) {
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

	public static void massAdditionMobsInit() {
		int spawnWeight = 0;
		String category;
		int minCount = 0;
		int maxCount = 0;
		int linecount = 0;
		String errorField = "first";
		String line;

		if (massAdditionMobsHashtable.size() > 0) {
			return;
		}
		try (InputStreamReader input = new InputStreamReader(
				new FileInputStream("config/spawnbalanceutility/MassAdditionMobs.csv"))) {
			BufferedReader br = new BufferedReader(input);
			while ((line = br.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line, ",");
				linecount++;
				try {
					errorField = "linenumber";
					int lineNumber = Integer.parseInt(st.nextToken().trim());
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
						System.out.println("SpawnBalanceUtility invalid classification " + classification + " on "
								+ linecount + "th line of MassAdditionMobs.csv.");
					} else if (spawnWeight > 0) {
						MassAdditionMobItem bci = new MassAdditionMobItem(lineNumber, category, classification,
								modAndMob, spawnWeight, minCount, maxCount);
						massAdditionMobsHashtable.put(key, bci); // uses last one in file if dupes
					}

				} catch (Exception e) {
					if (!(line.isEmpty())) {
						LOGGER.error("SpawnBalanceUtility Error reading field " + errorField + " on " + linecount
								+ "th line of MassAdditionMobs.csv.");
					} else if (MyConfigs.getDebugLevel() > 0) {
						LOGGER.warn("SpawnBalanceUtility Warning blank line at " + linecount
								+ "th line of MassAdditionMobs.csv.");
					}
				}
			}
			input.close();
		} catch (Exception e) {
			System.out.println("MassAdditionMobs.csv not found in subdirectory SpawnBalanceUtility");
		}

	}

	public static boolean validClassification(String classification) {
		for (MobCategory mc : MobCategory.values()) {
			if (classification.equalsIgnoreCase(mc.toString())) {
				return true;
			}
		}
		return false;
	}
	
	public static void generateMassAdditionMobsStubReport() {

		PrintStream p = null;
		try {
			p = new PrintStream(new FileOutputStream("config/spawnbalanceutility/MassAdditionMobs.rpt", false));
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (p == null) {
			p = System.out;
		}

		p.println("* This is an example Mass Addition File that lets you add mobs to every biome.");
		p.println("* Lines that start with a '*' are comments and are not used.");
		p.println("* If you rename this file to MassAdditionMobs.csv, Spawn Balance Utility will use it.");
		p.println("*");
		p.println("* Parameter explainations and values.");
		p.println("* Parm Dimension  : A, O, N, E for All, Overworld, Nether, The End");
		p.println("* Parm Class      : MONSTER, CREATURE, AMBIENT, UNDERWATER, etc.");
		p.println("* Parm Resource   : modname:mobname");
		p.println(
				"* Parm Weight     : a number 1 or higher.  1 is superrare, 5 is rare, 20 is uncommon, 80 is common.");
		p.println("* Parm MinGroup   : a number 1 and less than MaxGroup");
		p.println("* Parm MaxGroup   : a number higher than MinGroup and usually 5 or less.");
		p.println("* Format is. Line, Dim,   Class, mod:mob,           spawnWeight, Mingroup, MaxGroup");
		p.println("*");
		p.println("* 1,   A, MONSTER, minecraft:phantom, 10           ,1         ,4");
		p.println("* will add phantoms too all biomes with a spawnweight of 10 and 1-4 group size.");
		p.println("*");
		if (p != System.out) {
			p.close();
		}
	}

}
