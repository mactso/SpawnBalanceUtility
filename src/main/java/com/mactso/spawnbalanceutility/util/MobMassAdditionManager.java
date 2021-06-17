package com.mactso.spawnbalanceutility.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import com.mactso.spawnbalanceutility.config.MyConfig;

import net.minecraft.entity.EntityClassification;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome.Category;

public class MobMassAdditionManager {
	
	public static Hashtable<String, MassAdditionMobItem> massAdditionMobsHashtable = new Hashtable<>();
	static int lastgoodline = 0;
	
	public static String CATEGORY_ALL = "A";
	public static String CATEGORY_OVERWORLD = "O";
	public static String CATEGORY_NETHER = "N";
	public static String CATEGORY_THEEND = "E";

	public static List<MassAdditionMobItem> getFilteredList(EntityClassification v, Category category) {
		List<MassAdditionMobItem> ma = new ArrayList<>();
		for (MassAdditionMobItem m : massAdditionMobsHashtable.values()) {
			if (v.getName().equalsIgnoreCase(m.getClassification())) {
				if (m.getCategory().equals(CATEGORY_ALL)) {
					ma.add(m);
				} else 	if (category == Category.NETHER) {
					if (m.getCategory().equals(CATEGORY_NETHER)) {
						ma.add(m);	
					}
				}
				else if (category == Category.THEEND) {
					if (m.getCategory().equals(CATEGORY_THEEND)) {
						ma.add(m);
					}
				}
				else if (category != Category.NONE) {
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
					int lineNumber = Integer.parseInt( st.nextToken().trim());
					lastgoodline = lineNumber;
					errorField = "category";
					category =  st.nextToken().trim();
					errorField = "classification";
					String classification = st.nextToken().trim();
					errorField = "modAndMob";
					String modAndMob = st.nextToken().trim();
					ResourceLocation r = new ResourceLocation(modAndMob);					
					errorField = "spawnWeight";
					spawnWeight = Integer.parseInt(st.nextToken().trim());
					errorField = "minCount";
					minCount = Integer.parseInt(st.nextToken().trim());
					errorField = "maxCount";
					maxCount  = Integer.parseInt(st.nextToken().trim());

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
						System.out.println("SpawnBalanceUtility invalid classification "+classification+" on "+linecount+"th line of MassAdditionMobs.csv.");
					} else if (spawnWeight > 0){
						MassAdditionMobItem bci = new MassAdditionMobItem(lineNumber, category, classification, modAndMob, spawnWeight, minCount, maxCount);
						massAdditionMobsHashtable.put(key, bci);  // uses last one in file if dupes
					}
					
				} catch (Exception e) {
					if (!(line.isEmpty())) {
						System.out.println("SpawnBalanceUtility Error reading field "+errorField+" on "+linecount+"th line of MassAdditionMobs.csv.");
					} else if (MyConfig.getDebugLevel() > 0 ) {
						System.out.println("SpawnBalanceUtility Warning blank line at "+linecount+"th line of MassAdditionMobs.csv.");
					}
				}
			}
			input.close();
		} catch (Exception e) {
			System.out.println("MassAdditionMobs.csv not found in subdirectory SpawnBalanceUtility");
			// e.printStackTrace();
		}
		

	}

	public static boolean validClassification (String classification) {
		for (EntityClassification e : EntityClassification.values()) {
			if (classification.equalsIgnoreCase(e.getName())) {
				return true;
			}
		}
		return false;
	}
	public static class MassAdditionMobItem  {
		int lineNumber;
		String category;
		String classification;
		String modAndMob;
		int spawnWeight;
		int minCount;
		int maxCount;



		public MassAdditionMobItem(int lineNumber, String category, String classification, 
				String modAndMob, int spawnWeight, int min, int max ) {
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
