package com.mactso.spawnbalanceutility.manager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.spawnbalanceutility.config.MyConfig;
import com.mactso.spawnbalanceutility.util.Summary;
import com.mactso.spawnbalanceutility.util.Utility;

import net.minecraft.world.entity.MobCategory;

public class MobMassAdditionManager {

	private static final Logger LOGGER = LogManager.getLogger();

	public static Hashtable<String, MassAdditionMobItem> massAdditionMobs = new Hashtable<>();
	private static int lastgoodlinenumber = 0;
	private static String lastgoodline = "Start of the file.  There were no prior good lines.";

	public static String CATEGORY_ALL = "A";
	public static String CATEGORY_OVERWORLD = "O";
	public static String CATEGORY_NETHER = "N";
	public static String CATEGORY_THEEND = "E";

	// TODO category appears to be biomeName in the calling class.  
	// either fix the calling class or fix this class.
	public static List<MassAdditionMobItem> getFilteredList(MobCategory mc, String category) {
		
		List<MassAdditionMobItem> ma = new ArrayList<>();
		for (MassAdditionMobItem m : massAdditionMobs.values()) {
			if (mc.getName().equalsIgnoreCase(m.getMobCategory())) {
				if (m.getCategory().equals(CATEGORY_ALL)) {
					ma.add(m);
				} else if (category.equals(Utility.NETHER)) {
					if (m.getCategory().equals(CATEGORY_NETHER)) {
						ma.add(m);
					}
				} else if (category.equals(Utility.THEEND)) {
					if (m.getCategory().equals(CATEGORY_THEEND)) {
						ma.add(m);
					}
				} else if (!category.equals(Utility.NONE)) {
					if (m.getCategory().equals(CATEGORY_OVERWORLD)) {
						ma.add(m);
					}

				}
			}
		}
		return ma;
	}

	public static void massAdditionMobsInit(Path csvPath) {

		int linecount = 0;
		int blankline = 0;
		int commentcount = 0;
		String errorField = "first";
		String line;

		if (massAdditionMobs.size() > 0) {
			return;
		}
		
	    // Check if the file exists
	    File f = csvPath.toFile();
	    if (!f.exists()) {
	        // Optional: case-insensitive fallback (Linux)
	        Path fallbackPath = csvPath.getParent().resolve(
	                csvPath.getFileName().toString().toUpperCase()
	        );
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
			int lineNumber = 0;
            int physicalLine = 0;
            
			while ((line = br.readLine()) != null) {
                physicalLine++;

				if (line.trim().isEmpty()) {
					blankline++;
					continue;
				}
				if (line.charAt(0)=='*') {
					commentcount++;
					continue;
				}


                linecount++;
                errorField = "CSV fields";
                

                String[] parts = line.split(",", -1);
                if (parts.length < 7) {
                    LOGGER.warn("Invalid CSV line (not enough fields) at physical line " + physicalLine);
                    continue;
                }
				try {
                    errorField = "linenumber";
                    int lineNum = Integer.parseInt(parts[0].trim());
                    lastgoodlinenumber = lineNum;

                    errorField = "biomeCategory";
                    String biomeCategory = parts[1].trim();

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

                    // Clamp logic
					minCount = Math.max(MyConfig.MOB_MIN_COUNT, minCount);
					maxCount = Math.min(MyConfig.MOB_MAX_COUNT, maxCount);

                    if (minCount > maxCount) {
                        minCount = maxCount;
                    }

                    if (!validMobCategory(mobCategory)) {
                        LOGGER.warn("Invalid mobCategory '{}' at CSV record {}", mobCategory, linecount);
                        continue;
                    }
					if (spawnWeight > 0) {
						Utility.debugMsg(1,
								lineNumber + ", " + lastgoodline + ", " + modAndMob + ", " + mobCategory + ", "
										+ modAndMob + ", " + spawnWeight + ", " + minCount + ", " + maxCount);
						MassAdditionMobItem item = new MassAdditionMobItem(lineNumber, biomeCategory, mobCategory,
								modAndMob, spawnWeight, minCount, maxCount);
						massAdditionMobs.put(modAndMob, item); // uses last one in file if dupes
					}
					lastgoodline = line;
                }catch (NumberFormatException e) {
	                LOGGER.warn("Number format problem reading " + errorField + " on line " + linecount + " with line number " + lineNumber + " of BiomeMobWeight.csv: " + line);
	                LOGGER.warn("The last good line was: " + lastgoodline);

	            } catch (Exception e) {
	                LOGGER.warn("Unexpected problem reading line " + linecount + " with line number " + lineNumber + " of BiomeMobWeight.csv: " + line, e);
	                LOGGER.warn("The last good line was: " + lastgoodline);
                }
            }

        } catch (IOException e) {
            LOGGER.error("Error reading MassAdditionMobs.csv", e);
        }

        linecount -= (blankline + commentcount);
        Summary.setMassAddReadInfo(linecount, linecount - massAdditionMobs.size());
    }

	public static boolean validMobCategory(String mobCategory) {
		for (MobCategory mc : MobCategory.values()) {
			if (mobCategory.equalsIgnoreCase(mc.toString())) {
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
		p.println("* Format is. Line#, Dimension, mobCategory, mod:mob, spawnWgt, MinGroup, MaxGroup");
		p.println("*");
		p.println("* 1,   A, MONSTER, minecraft:phantom, 10           ,1         ,4");
		p.println("* will add phantoms too all biomes with a spawnweight of 10 and 1-4 group size.");
		p.println("*");
		if (p != System.out) {
			p.close();
		}
	}
	
	
	public static class MassAdditionMobItem {
		int lineNumber;
		String category;
		String mobCategory;
		String modAndMob;
		int spawnWeight;
		int minCount;
		int maxCount;

		public MassAdditionMobItem(int lineNumber, String category, String mobCategory, String modAndMob,
				int spawnWeight, int min, int max) {
			this.lineNumber = lineNumber;
			this.category = category;
			this.mobCategory = mobCategory;
			this.modAndMob = modAndMob;
			this.spawnWeight = spawnWeight;
			this.minCount = min;
			this.maxCount = max;


		}

		public String getCategory() {
			return category;
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
