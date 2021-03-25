package com.mactso.spawnbalanceutility.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.*;
import java.util.Map.Entry;

import net.minecraft.entity.EntityClassification;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;

import java.io.*;

public class BiomeCreatureManager {

	public static Map<String,List<BiomeCreatureItem>> biomeCreaturesMap = new HashMap<>();
	public static Hashtable<String, BiomeCreatureItem> biomeCreatureHashtable = new Hashtable<>();
	static int lastgoodline = 0;
	
	public static void biomeCreatureInit() {
		int spawnWeight = 0;
		int minCount = 0;
		int maxCount = 0;
		int linecount = 0;
		String errorField = "first";
		String line;
		
		if (biomeCreaturesMap.size() > 0) {
			return;
		}
		try (InputStreamReader input = new InputStreamReader(
				new FileInputStream("config/spawnbalanceutility/BiomeMobWeight.csv"))) {
			BufferedReader br = new BufferedReader(input);
			int x = 3;
			while ((line = br.readLine()) != null) {
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
					ResourceLocation r = new ResourceLocation(modAndBiome);
					errorField = "classification";
					String classification = st.nextToken().trim();
					errorField = "modAndMob";
					String modAndMob = st.nextToken().trim();
					r = new ResourceLocation(modAndMob);					
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
					}
					
				} catch (Exception e) {
					System.out.println("SpawnBalanceUtility Error reading field "+errorField+" on "+linecount+"th line of BiomeMobWeight.csv.");
				}
			}
			input.close();
		} catch (Exception e) {
			System.out.println("BiomeMobWeight.csv not found in subdirectory SpawnBalanceUtility");
			// e.printStackTrace();
		}
		
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
