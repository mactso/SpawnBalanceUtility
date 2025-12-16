package com.mactso.spawnbalanceutility.util;

import java.lang.reflect.Field;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.spawnbalanceutility.config.MyConfig;

public final class SpawnerFieldAccess {

    private static final Logger LOGGER = LogManager.getLogger();
    private static Field SPAWNERS_FIELD;

    private SpawnerFieldAccess() {}

    /**
     * Returns the resolved Field object for MobSpawnSettings.spawners.
     * Throws RuntimeException if resolution failed.
     */
    public static Field get() {
        if (SPAWNERS_FIELD == null) {
            SPAWNERS_FIELD = resolve();
        }
        return SPAWNERS_FIELD;
    }

    private static Field resolve() {
        Field f;

        // Try vanilla production mapping first
        f = resolveVanilla();
        if (f != null) {
            return f;
        }

        // Try development / intermediary fallback
        f = resolveIntermediary();
        if (f != null) {
            return f;
        }

        // Terminal failure
        LOGGER.error(
            "SpawnBalanceUtility: failed to reflect MobSpawnSettings.spawners and net.minecraft.class_5483.field_26405");
        return null;
    }

    /** Resolves using vanilla named class in production */
    private static Field resolveVanilla() {
        int step = 1;
        try {
            if (MyConfig.getDebugLevel() > 0) LOGGER.warn("[" + step++ + "] resolveVanilla(): START");

            String className = "net.minecraft.world.level.biome.MobSpawnSettings";
            Class<?> clazz = Class.forName(className);
            if (MyConfig.getDebugLevel() > 0) LOGGER.warn("[" + step++ + "] Class.forName() succeeded: " + clazz.getName());

            Field f = clazz.getDeclaredField("spawners");
            f.setAccessible(true);
            if (MyConfig.getDebugLevel() > 0) LOGGER.warn("[" + step++ + "] Field 'spawners' accessed successfully");

            return f;

        } catch (Exception e) {
            if (MyConfig.getDebugLevel() > 0) LOGGER.warn("[" + step++ + "] resolveVanilla() failed: " + e);
            return null;
        }
    }

    /** Resolves using intermediary/development class */
    private static Field resolveIntermediary() {
        int step = 1;
        try {
            if (MyConfig.getDebugLevel() > 0) LOGGER.warn("[" + step++ + "] resolveIntermediary(): START");

            Class<?> clazz = Class.forName("net.minecraft.class_5483"); // intermediary MobSpawnSettings
            if (MyConfig.getDebugLevel() > 0) LOGGER.warn("[" + step++ + "] Class.forName() succeeded: " + clazz.getName());

            Field f = clazz.getDeclaredField("field_26405"); // intermediary field
            f.setAccessible(true);
            if (MyConfig.getDebugLevel() > 0) LOGGER.warn("[" + step++ + "] Field 'field_26405' accessed successfully");

            return f;

        } catch (Exception e) {
            if (MyConfig.getDebugLevel() > 0) LOGGER.warn("[" + step++ + "] resolveIntermediary() failed: " + e);
            return null;
        }
    }
}
