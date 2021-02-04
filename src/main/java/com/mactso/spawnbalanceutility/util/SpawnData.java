package com.mactso.spawnbalanceutility.util;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.mactso.spawnbalanceutility.config.MyConfig;

import net.minecraft.entity.EntityClassification;
import net.minecraft.world.biome.MobSpawnInfo.Spawners;
import net.minecraftforge.common.world.MobSpawnInfoBuilder;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.event.world.StructureSpawnListGatherEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SpawnData
{
	static {
		File fd = new File("config/spawnbalanceutility");
		if (!fd.exists()) fd.mkdir();
		File fb = new File("config/spawnbalanceutility/BiomeMobWeight.txt");
		if (fb.exists()) fb.delete();
		File fs = new File("config/spawnbalanceutility/StructMobWeight.txt");
		if (fb.exists()) fs.delete();
	}
	
	@SubscribeEvent (priority=EventPriority.LOWEST)
	public static void onBiome(BiomeLoadingEvent event)
	{
		

		String threadname = Thread.currentThread().getName();
		if (threadname.equals("Render thread")) {
			return;
		}
		
		// may be 'format'
		PrintStream p=null;
		try {
			p = new PrintStream(new  FileOutputStream("config/spawnbalanceutility/BiomeMobWeight.txt",true));
		} catch (IOException e) {
            e.printStackTrace();
        }
		
		if (p==null) {
			p=System.out;
		}

		String bn = event.getName().toString();
		String cn = event.getCategory().getName().toString();
		MobSpawnInfoBuilder builder = event.getSpawns();
		
		for (EntityClassification v : EntityClassification.values() ) {

			for (Spawners s : builder.getSpawner(v)) {
				p.println(cn + ", " +bn + ", " +v + ", "+ s.type.getRegistryName() + ", " + s.itemWeight + ", " + s.minCount + ", " + s.maxCount);
			}
		}
		
		if (p!=System.out) {
			p.close();
		}

	}
	
	@SubscribeEvent (priority=EventPriority.LOWEST)
	public static void onStructure(StructureSpawnListGatherEvent event)
	{
		PrintStream p=null;
		try {
			p = new PrintStream(new  FileOutputStream("config/spawnbalanceutility/StructMobWeight.txt",true));
		} catch (IOException e) {
            e.printStackTrace();
        }
		
		if (p==null) {
			p=System.out;
		}
		
		String sn = event.getStructure().getStructureName().toString();

//		p.println("Structure :" + sn +  ". ");
       		
		for (EntityClassification v : EntityClassification.values() ) {
			for (Spawners s : event.getEntitySpawns(v)) {
				p.println(sn + ", " + v + ", " + s.type.getRegistryName() + ", " + s.itemWeight + ", " + s.minCount + ", " +s.maxCount );
			}
			if (event.getEntitySpawns(v).size() == 0) {
				p.println (sn + ", " + v +", nonenone:none, 0, 0, 0");
			}
		}
		
		if (p!=System.out) {
			p.close();
		}		
	}
}

