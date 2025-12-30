package com.mactso.spawnbalanceutility.utilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class AllMobEntitiesReport {
	
	public static void initReports () {
		File fd = new File("config/spawnbalanceutility");
		if (!fd.exists())
			fd.mkdir();
		File fb = new File("config/spawnbalanceutility/AllMobEntities.rpt");
		if (fb.exists())
			fb.delete();
	}
	
	public static void doReport(MinecraftServer server) {

	    initReports();

	    PrintStream p;
	    try {
	        p = new PrintStream(new FileOutputStream(
	                "config/spawnbalanceutility/AllMobEntities.rpt", true));
	    } catch (IOException e) {
	        e.printStackTrace();
	        p = System.out;
	    }

	    p.println("* This file is a dictionary of all mob identifiers (modname:mobname) to help you.");
	    p.println("* The MISC mobs can not spawn normally.");
	    p.println("* Some MISC things like Item Frames won't spawn properly.");
	    p.println("* ");

	    Registry<EntityType<?>> entityRegistry =
	            server.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);

	    for (EntityType<?> type : entityRegistry) {

	        if (!isValidClassification(type)) {
	            continue; // skip listing misc items now that PseudoMobs is gone
	        }

	        if (type.canSummon()) {
	            p.println(
	                entityRegistry.getKey(type) + ", " + type.getCategory()
	            );
	        }
	    }

	    if (p != System.out) {
	        p.close();
	    }
	}

	private static boolean isValidClassification(EntityType<?> a) {
		if (a.getCategory() == MobCategory.MISC) 
			return false;
		return true;
	}
	

}
