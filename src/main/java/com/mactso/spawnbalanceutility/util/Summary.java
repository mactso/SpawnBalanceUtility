//package com.mactso.spawnbalanceutility.util;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.function.Function;
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
//import net.minecraft.util.random.WeightedRandomList;
//import net.minecraft.world.entity.EntityType;
//import net.minecraft.world.entity.MobCategory;
//import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
//
//public class Summary
//{
//	private static final Logger LOGGER = LogManager.getLogger();
//	private static int biomeTotal;
//	private static int biomeSkip;
//	private static int biomeUsed;
//	private static int biomeAdd;
//	private static int biomeChange;
//	private static int biomeDelete;
//	private static int structureTotal;
//	private static int structureSkip;
//	private static int structureUsed;
//	private static int massAddTotal;
//	private static int massAddSkip;
//	private static int massAddUsed;
//	private static int massAddBiomes;
//	private static int bFixCount;
//	private static int sFixCount;
//	private static int netherCount;
//
//	public static void clear()
//	{
//		biomeUsed = -1;
//		massAddUsed = -1;
//		massAddBiomes = 0;
//		bFixCount = 0;
//		netherCount = 0;
//		biomeAdd = 0;
//		biomeChange = 0;
//		biomeDelete = 0;
//	}
//
//	public static void clearStructure()
//	{
//		structureUsed = -1;
//		sFixCount = 0;
//	}
//
//	public static void setBiomeReadInfo(int total, int skip)
//	{
//		biomeTotal = total;
//		biomeSkip = skip;
//	}
//
//	public static void setBiomeUsed(int used)
//	{
//		biomeUsed = used;
//	}
//
//	public static void biomeUpdate(Map<MobCategory, WeightedRandomList<SpawnerData>> oldMap, Map<MobCategory, WeightedRandomList<SpawnerData>> newMap)
//	{
//		Set<MobCategory> set = new HashSet<>(oldMap.keySet());
//		set.addAll(newMap.keySet());
//		for (MobCategory k : set)
//		{
//			WeightedRandomList<SpawnerData> oldWList = oldMap.get(k);
//			WeightedRandomList<SpawnerData> newWList = newMap.get(k);
//			List<SpawnerData> oldList = (oldWList != null) ? oldWList.unwrap() : new ArrayList<>();
//			List<SpawnerData> newList = (newWList != null) ? newWList.unwrap() : new ArrayList<>();
//			Map<EntityType<?>,BiomeSpawns> dataMap = new HashMap<>();
//			Function<EntityType<?>,BiomeSpawns> def = t -> new BiomeSpawns();
//			for (SpawnerData e : oldList)
//			{
//				BiomeSpawns data = dataMap.computeIfAbsent(e.type, def);
//				data.oldList.add(e);
//			}
//			for (SpawnerData e : newList)
//			{
//				BiomeSpawns data = dataMap.computeIfAbsent(e.type, def);
//				data.newList.add(e);
//			}
//			for (BiomeSpawns data : dataMap.values())
//			{
//				List<SpawnerData> oldTList = data.oldList;
//				List<SpawnerData> newTList = data.newList;
//				int delta = newTList.size() - oldTList.size();
//				if (delta > 0)
//					biomeAdd += delta;
//				else if (delta < 0)
//					biomeDelete -= delta;
//				if (newTList.size() > 0 && oldTList.size() > 0)
//				{
//					List<SpawnerData> list1, list2;
//					if (delta < 0)
//					{
//						list1 = newTList;
//						list2 = oldTList;
//					}
//					else
//					{
//						list1 = oldTList;
//						list2 = newTList;
//					}
//					for (SpawnerData e : list1)
//					{
//						boolean found = false;
//						Iterator<SpawnerData> it = list2.iterator();
//						while (it.hasNext())
//						{
//							SpawnerData e2 = it.next();
//							if (e2.getWeight().asInt() == e.getWeight().asInt() && e2.minCount == e.minCount && e2.maxCount == e.maxCount)
//							{
//								it.remove();
//								found = true;
//								break;
//							}
//						}
//						if (!found)
//							biomeChange++;
//					}
//				}
//			}
//		}
//	}
//
//	public static void setStructureReadInfo(int total, int skip)
//	{
//		structureTotal = total;
//		structureSkip = skip;
//	}
//
//	public static void setStructureUsed(int used)
//	{
//		if (structureUsed < 0)
//			structureUsed = 0;
//		structureUsed += used;
//	}
//
//	public static void setMassAddReadInfo(int total, int skip)
//	{
//		massAddTotal = total;
//		massAddSkip = skip;
//	}
//
//	public static void setMassAddUsed(int used, int biomes)
//	{
//		massAddUsed = used;
//		massAddBiomes = biomes;
//	}
//
//	public static void setBiomeFix(int count, int netherUsed)
//	{
//		bFixCount = count;
//		netherCount = netherUsed;
//	}
//
//	public static void setStructureFix(int count)
//	{
//		sFixCount += count;
//	}
//
//	public static void report()
//	{
//		LOGGER.info("SBU SUMMARY");
//		String extra = "";
//		if (biomeSkip > 0)
//			extra = extra + ", " + biomeSkip + " lines skipped";
//		if (biomeTotal - biomeSkip > 0 && biomeUsed >= 0)
//		{
//			extra = extra + ", " + biomeUsed + " lines used";
//			extra = extra + " (" + biomeAdd + "/" + biomeChange + "/" + biomeDelete + ")";
//		}
//		LOGGER.info("Biome csv: " + biomeTotal + " lines read" + extra);
//
//		extra = "";
//		if (structureSkip > 0)
//			extra = extra + ", " + structureSkip + " lines skipped";
//		if (structureTotal - structureSkip > 0 && structureUsed >= 0)
//			extra = extra + ", " + structureUsed + " lines used";
//		LOGGER.info("Structure csv: " + structureTotal + " lines read" + extra);
//
//		extra = "";
//		if (massAddSkip > 0)
//			extra = extra + ", " + massAddSkip + " lines skipped";
//		if (massAddTotal - massAddSkip > 0 && massAddUsed >= 0)
//			extra = extra + ", " + massAddUsed + " lines used (" + massAddBiomes + " biomes)";
//		LOGGER.info("Mass add csv: " + massAddTotal + " lines read" + extra);
//
//		LOGGER.info("Biome fix: " + bFixCount + " weight changed, " + netherCount + " add to nether");
//		LOGGER.info("Structure fix: " + sFixCount + " weight changed");
//	}
//
//	static class BiomeSpawns
//	{
//		public List<SpawnerData> oldList = new ArrayList<>();
//		public List<SpawnerData> newList = new ArrayList<>();
//		public BiomeSpawns()
//		{
//		}
//	}
//}
