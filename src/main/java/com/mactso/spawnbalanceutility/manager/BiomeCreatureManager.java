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
import java.util.StringTokenizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.spawnbalanceutility.config.MyConfig;

public class BiomeCreatureManager {

	private static final Logger LOGGER = LogManager.getLogger();
	public static Map<String,List<BiomeCreatureItem>> biomeCreaturesMap = new HashMap<>();
	public static Hashtable<String, BiomeCreatureItem> biomeCreatureHashtable = new Hashtable<>();
	static int lastgoodline = 0;
	
	public static void biomeCreatureInit() {
		int spawnWeight = 0;
		int minCount = 0;
		int maxCount = 0;
		int linecount = 0;
		int addcount = 0;
		int commentcount = 0;
		String errorField = "first";
		String line;
		
		if (biomeCreaturesMap.size() > 0) {
			return;
		}

		
 // this code only has an effect on linux because case doesn't matter on windows)
		File f = new File("config/spawnbalanceutility/BiomeMobWeight.csv");
				if (!(f.exists())) {
			 f = new File("config/spawnbalanceutility/BiomeMobWeight.CSV");
		}
		
		try (InputStreamReader input = new InputStreamReader( new FileInputStream(f))) 
		{			
			BufferedReader br = new BufferedReader(input);
			while ((line = br.readLine()) != null) {
				
				if (line.isEmpty()) {
					continue;
				} 
		
				if (line.trim().isEmpty()) {
					continue;
				} 
				
				if (line.charAt(0)=='*') {
					commentcount++;
					continue;
				}

				StringTokenizer st = new StringTokenizer(line, ",");
				linecount++;
				try {
					errorField = "linenumber";
					int lineNumber = Integer.parseInt( st.nextToken().trim());
					lastgoodline = lineNumber;
					errorField = "category";
					String category = st.nextToken().trim();
					errorField = "modAndBiome";
					String modAndBiome = st.nextToken().trim();
					errorField = "classification";
					String classification = st.nextToken().trim();
					errorField = "modAndMob";
					String modAndMob = st.nextToken().trim();
					errorField = "spawnWeight";
					spawnWeight = Integer.parseInt(st.nextToken().trim());
					errorField = "minCount";
					minCount = Integer.parseInt(st.nextToken().trim());
					errorField = "maxCount";
					maxCount  = Integer.parseInt(st.nextToken().trim());

					if (minCount < 1) {
						minCount = 1;
					}
					if (maxCount > 32) {
						maxCount = 32;
					}
					if (minCount > maxCount) {
						minCount = maxCount;
					}		
				
					String key = modAndBiome;
					if (spawnWeight > 0){
						BiomeCreatureItem bci = new BiomeCreatureItem(lineNumber, category, modAndBiome, classification, modAndMob, spawnWeight, minCount, maxCount);
						List<BiomeCreatureItem> p = biomeCreaturesMap.get(key);
						if (p == null) {
							p = new ArrayList<>();
							biomeCreaturesMap.put(key, p);
						}
						// TODO maybe check for duplicates here later
						// for now okay as long as spawn weight > 0.
						p.add(bci);
						addcount++;
					}
					
				} catch (Exception e) {
					if (!(line.isEmpty())) {
						LOGGER.warn("SpawnBalanceUtility problem reading field "+errorField+" on "+linecount+"th line of BiomeMobWeight.csv.");
					} else if (MyConfig.getDebugLevel() > 0 ) {
						LOGGER.warn("SpawnBalanceUtility blank line at "+linecount+"th line of BiomeMobWeight.csv.");
					}
				}
			}
			input.close();
		} catch (Exception e) {
			LOGGER.warn("BiomeMobWeight.csv not found in config/spawnbalanceutility/ (Remember you rename BiomeMobWeight.rpt to create it). ");
			e.printStackTrace();
		}
		//Summary.setBiomeReadInfo(linecount, linecount - addcount);
	}
	
	
	public static class BiomeCreatureItem  {
		int lineNumber;
		String category;
		String modAndBiome;
		String classification;
		String modAndMob;
		int spawnWeight;
		int minCount;
		int maxCount;

		public BiomeCreatureItem(int lineNumber, String category, String modAndBiome, String classification, 
				String modAndMob, int spawnWeight, int min, int max) {
			this.lineNumber = lineNumber;
			this.category = category;
			this.modAndBiome = modAndBiome;
			this.classification = classification;
			this.modAndMob = modAndMob;
			this.spawnWeight = spawnWeight;
			this.minCount = min;
			this.maxCount = max;
		}
		
		public String getCategory() {
			return category;
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
