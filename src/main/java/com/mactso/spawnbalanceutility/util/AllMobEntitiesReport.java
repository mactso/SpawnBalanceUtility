package com.mactso.spawnbalanceutility.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.util.registry.Registry;

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

		for (EntityType<?> a : Registry.ENTITY_TYPE) {
			if (isValidClassification(a)) {
				p.println(a.getRegistryName().toString() + ", " + a.getClassification());
			}
		}
		
		if (p != System.out) {
			p.close();
		}
		
	}

	private static boolean isValidClassification(EntityType<?> a) {
		if (a.getClassification() == EntityClassification.MISC) 
			return false;
		return true;
	}
	

}
