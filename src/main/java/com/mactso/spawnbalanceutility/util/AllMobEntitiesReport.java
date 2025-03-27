package com.mactso.spawnbalanceutility.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;

public class AllMobEntitiesReport {
	
	public static void initReports () {
		File fd = new File("config/spawnbalanceutility");
		if (!fd.exists())
			fd.mkdir();
		File fb = new File("config/spawnbalanceutility/AllMobEntities.rpt");
		if (fb.exists())
			fb.delete();
	}
	
	@SuppressWarnings("deprecation")
	public static void doReport() {
		
		PrintStream p = null;
		try {
			p = new PrintStream(new FileOutputStream("config/spawnbalanceutility/AllMobEntities.rpt", true));
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (p == null) {
			p = System.out;
		}
		
		p.println ("* This file is a dictionary of all mob identifiers (modname:mobname) to help you.");
		p.println ("* The MISC mobs can only be spawned as Psuedo Mobs.  They do not spawn normally.");
		p.println ("* Some MISC things like Item Frames won't spawn propery.");
		p.println ("* ");

		for (EntityType<?> a : Registries.ENTITY_TYPE) {
			if (isValidClassification(a)) {
				p.println(a.getRegistryEntry().toString() + ", " + a.getSpawnGroup().asString());
			}
		}
		
		if (p != System.out) {
			p.close();
		}
		
	}

	private static boolean isValidClassification(EntityType<?> a) {
		if (a.getSpawnGroup() == SpawnGroup.MISC) {
			return false;
		}

		return true;
	}
	

}
