//package com.mactso.spawnbalanceutility.entity;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//import javax.annotation.Nullable;
//
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.RegistryAccess;
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.network.chat.Component;
//import net.minecraft.server.level.ServerLevel;
//import net.minecraft.world.entity.EntityType;
//import net.minecraft.world.entity.MobSpawnType;
//import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
//import net.minecraft.world.entity.ai.sensing.GolemSensor;
//import net.minecraft.world.entity.animal.IronGolem;
//import net.minecraft.world.entity.monster.Zombie;
//import net.minecraft.world.entity.npc.Villager;
//import net.minecraft.world.entity.npc.VillagerProfession;
//import net.minecraft.world.entity.npc.VillagerType;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.phys.AABB;
//
//public class PsuedoMob extends Zombie {
//	String actualEntityRL = "minecraft:phantom";
//	int    lightLevel = 15;
//	boolean fireResist  = true;
//	Villager v;
//	
//	   public PsuedoMob(EntityType<? extends PsuedoMob> entity, Level level) {
//		   super(entity, level);
//
//	   }
//
//
//	
//	   public void spawnActualPsuedoMob(ServerLevel level, long p_35399_, int p_35400_) {
//		      if (this.wantsToSpawnGolem(p_35399_)) {
//		         AABB aabb = this.getBoundingBox().inflate(10.0D, 10.0D, 10.0D);
//		         List<Villager> list = level.getEntitiesOfClass(Villager.class, aabb);
//		         List<Villager> list1 = list.stream().filter((p_186293_) -> {
//		            return p_186293_.wantsToSpawnGolem(p_35399_);
//		         }).limit(5L).collect(Collectors.toList());
//		         if (list1.size() >= p_35400_) {
//		            IronGolem irongolem = this.trySpawnGolem(level);
//		            if (irongolem != null) {
//		               list.forEach(GolemSensor::golemDetected);
//		            }
//		         }
//		      }
//		   }
//	   
//	   @Nullable
//	   private IronGolem trySpawnGolem(ServerLevel p_35491_) {
//	      BlockPos blockpos = this.blockPosition();
//
//	      for(int i = 0; i < 10; ++i) {
//	         double d0 = (double)(p_35491_.random.nextInt(16) - 8);
//	         double d1 = (double)(p_35491_.random.nextInt(16) - 8);
//	         BlockPos blockpos1 = this.findSpawnPositionForGolemInColumn(blockpos, d0, d1);
//	         if (blockpos1 != null) {
//	            IronGolem irongolem = EntityType.IRON_GOLEM.create(p_35491_, (CompoundTag)null, (Component)null, (Player)null, blockpos1, MobSpawnType.MOB_SUMMONED, false, false);
//	            if (irongolem != null) {
//	               if (irongolem.checkSpawnRules(p_35491_, MobSpawnType.MOB_SUMMONED) && irongolem.checkSpawnObstruction(p_35491_)) {
//	                  p_35491_.addFreshEntityWithPassengers(irongolem);
//	                  return irongolem;
//	               }
//
//	               irongolem.discard();
//	            }
//	         }
//	      }
//
//	      return null;
//	   }
//}
