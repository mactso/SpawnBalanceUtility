package com.mactso.spawnbalanceutility.util;

import java.lang.reflect.Field;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.spawnbalanceutility.config.MyConfig;

public final class StructureFieldAccess {

    private static final Logger LOGGER = LogManager.getLogger();
    private static Field STRUCTURE_SETTINGS_FIELD;

    private StructureFieldAccess() {}

    /**
     * Returns the resolved Field object for Structure.settings (StructureSettings).
     * Throws RuntimeException if resolution fails.
     */
    public static Field get() {
        if (STRUCTURE_SETTINGS_FIELD == null) {
            STRUCTURE_SETTINGS_FIELD = resolve();
        }
        return STRUCTURE_SETTINGS_FIELD;
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
        LOGGER.error("SpawnBalanceUtility: failed to reflect Structure.settings (Structure$StructureSettings)");
        return null;
    }

    /** Resolves using vanilla named class in production */
    private static Field resolveVanilla() {
        int step = 1;
        try {
            if (MyConfig.getDebugLevel() > 0) LOGGER.warn("[" + step++ + "] resolveVanilla(): START");

            String className = "net.minecraft.world.level.levelgen.structure.Structure";
            Class<?> clazz = Class.forName(className);
            if (MyConfig.getDebugLevel() > 0) LOGGER.warn("[" + step++ + "] Class.forName() succeeded: " + clazz.getName());

            Field f = clazz.getDeclaredField("settings");
            f.setAccessible(true);
            if (MyConfig.getDebugLevel() > 0) LOGGER.warn("[" + step++ + "] Field 'settings' accessed successfully");

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

            Class<?> clazz = Class.forName("net.minecraft.class_3195"); // intermediary Structure class
            if (MyConfig.getDebugLevel() > 0) LOGGER.warn("[" + step++ + "] Class.forName() succeeded: " + clazz.getName());

            if (MyConfig.getDebugLevel() > 0) {
                LOGGER.warn("[" + step++ + "] Dumping declared fields for {}", clazz.getName());
                int i = 0;
                for (Field fld : clazz.getDeclaredFields()) {
                    LOGGER.warn("[{}.{}] Field name='{}' type='{}'", step, ++i, fld.getName(), fld.getType().getName());
                }
            }

            Field f = clazz.getDeclaredField("field_38429"); // intermediary field name for StructureSettings
            f.setAccessible(true);
            if (MyConfig.getDebugLevel() > 0) LOGGER.warn("[" + step++ + "] Field 'field_38429' accessed successfully");

            return f;

        } catch (Exception e) {
            if (MyConfig.getDebugLevel() > 0) LOGGER.warn("[" + step++ + "] resolveIntermediary() failed: " + e);
            return null;
        }
    }
}
