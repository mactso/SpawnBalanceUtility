package com.mactso.spawnbalanceutility.manager;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.mactso.spawnbalanceutility.Main;
import com.mactso.spawnbalanceutility.util.Summary;
import com.mactso.spawnbalanceutility.util.Utility;

public class StructureCreatureManager {

	public static Map<String,List<StructureCreatureItem>> structureCreaturesMap = new HashMap<>();
	public static Hashtable<String, StructureCreatureItem> structureCreatureHashtable = new Hashtable<>();
	static int lastgoodline = 0;
	
	public static void structureCreatureInit() {
		int spawnWeight = 0;
		int minCount = 0;
		int maxCount = 0;
		int linecount = 0;
		int addcount = 0;
		String errorField = "first";
		String line;
		
		if (structureCreaturesMap.size() > 0) {
			return;
		}
		try (InputStreamReader input = new InputStreamReader(
				new FileInputStream("config/spawnbalanceutility/StructMobWeight.csv"))) {
			BufferedReader br = new BufferedReader(input);
			while ((line = br.readLine()) != null) {
				if (line.charAt(0)=='*') {
					continue;
				}
				StringTokenizer st = new StringTokenizer(line, ",");
				linecount++;
				try {
					errorField = "linenumber";
					int lineNumber = Integer.parseInt( st.nextToken().trim());
					lastgoodline = lineNumber;
					errorField = "modAndStructure";
					String modAndStructure = st.nextToken().trim();
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
					if (maxCount > 12) {
						maxCount = 12;
					}
					if (minCount > maxCount) {
						minCount = maxCount;
					}					
					String key = modAndStructure;
					if (spawnWeight > 0){
						// TODO set this debug value to 1.
						Utility.debugMsg(1, lineNumber +", "+ lastgoodline+", "+ modAndStructure+", "+ classification+", "+ modAndMob+", "+ spawnWeight+", "+minCount+", "+ maxCount);
						StructureCreatureItem bci = new StructureCreatureItem(lineNumber, modAndStructure, classification, modAndMob, spawnWeight, minCount, maxCount);
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
					
				} catch (Exception e) {
					Utility.debugMsg(0, Main.MODID + " Error reading field "+errorField+" on "+linecount+"th line of StructureMobWeight.csv.");
				}
			}
			input.close();
		} catch (Exception e) {
			Utility.debugMsg(0, "Warning StructMobWeight.csv not found in subdirectory SpawnBalanceUtility");

		}
		Summary.setStructureReadInfo(linecount, linecount - addcount);
	}
	
	
	public static class StructureCreatureItem  {
		int lineNumber;
		String modAndStructure;
		String classification;
		String modAndMob;
		int spawnWeight;
		int minCount;
		int maxCount;

		public StructureCreatureItem(int lineNumber, String modAndStructure, String classification, 
				String modAndMob, int spawnWeight, int min, int max) {
			this.lineNumber = lineNumber;
			this.modAndStructure = modAndStructure;
			this.classification = classification;
			this.modAndMob = modAndMob;
			this.spawnWeight = spawnWeight;
			this.minCount = min;
			this.maxCount = max;
		}
		
		public String getModAndStructure() {
			return modAndStructure;
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
