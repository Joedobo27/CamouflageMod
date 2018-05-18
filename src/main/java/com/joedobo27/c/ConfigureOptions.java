package com.joedobo27.c;

import org.jetbrains.annotations.Nullable;

import javax.json.*;
import java.io.*;
import java.util.Properties;

public class ConfigureOptions {


    private final LinearScalingFunction camouflageRecoveryScale;
    private final LinearScalingFunction spellPowerExplainsCamouflageChance;
    private final int castingEarthSeconds;
    private final int costFavor;
    private final int spellDifficulty;
    private final int requiredFaith;
    private final long coolDownEarthMilliseconds;
    private final LinearScalingFunction armorDRExplainsCamouflageChance;

    private static ConfigureOptions instance = null;

    private ConfigureOptions(LinearScalingFunction camouflageRecoveryScale, LinearScalingFunction spellPowerExplainsCamouflageChance,
                             int castingEarthSeconds, int costFavor, int spellDifficulty, int requiredFaith,
                             long coolDownEarthMilliseconds, LinearScalingFunction armorDRExplainsCamouflageChance) {

        this.camouflageRecoveryScale = camouflageRecoveryScale;
        this.spellPowerExplainsCamouflageChance = spellPowerExplainsCamouflageChance;
        this.castingEarthSeconds = castingEarthSeconds;
        this.costFavor = costFavor;
        this.spellDifficulty = spellDifficulty;
        this.requiredFaith = requiredFaith;
        this.coolDownEarthMilliseconds = coolDownEarthMilliseconds;
        this.armorDRExplainsCamouflageChance = armorDRExplainsCamouflageChance;
        instance = this;
    }

    synchronized static void setOptions(@Nullable Properties properties) {
        if (instance == null) {
            if (properties == null) {
                properties = getProperties();
            }
            if (properties == null)
                throw new RuntimeException("properties can't be null here.");
            instance = new ConfigureOptions(
                    doJsonTo(properties.getProperty("camouflageRecovery")),
                    doJsonTo(properties.getProperty("spellPowerExplainsCamouflageChance")),
                    Integer.parseInt(properties.getProperty("castingEarthSeconds", "20")),
                    Integer.parseInt(properties.getProperty("costFavor", "80")),
                    Integer.parseInt(properties.getProperty("spellDifficulty", "60")),
                    Integer.parseInt(properties.getProperty("requiredFaith", "30")),
                    Long.parseLong(properties.getProperty("coolDownEarthMilliseconds", "0")),
                    doJsonTo(properties.getProperty("armorDRExplainsCamouflageChance")));
        }
    }

    synchronized static void resetOptions() {
        instance = null;
        Properties properties = getProperties();
        if (properties == null)
            throw new RuntimeException("properties can't be null here.");
        instance = new ConfigureOptions(
                doJsonTo(properties.getProperty("camouflageRecovery")),
                doJsonTo(properties.getProperty("spellPowerExplainsCamouflageChance")),
                Integer.parseInt(properties.getProperty("castingEarthSeconds", "20")),
                Integer.parseInt(properties.getProperty("costFavor", "80")),
                Integer.parseInt(properties.getProperty("spellDifficulty", "60")),
                Integer.parseInt(properties.getProperty("requiredFaith", "30")),
                Long.parseLong(properties.getProperty("coolDownEarthMilliseconds", "0")),
                doJsonTo(properties.getProperty("armorDRExplainsCamouflageChance")));
    }

    private static LinearScalingFunction doJsonTo(String jsonString) {
        Reader reader = new StringReader(jsonString);
        JsonReader jsonReader = Json.createReader(reader);
        JsonObject jsonValues = jsonReader.readObject();
        double x1;
        double x2;
        double y1;
        double y2;
        try {
            x1 = jsonValues.getJsonNumber("x1").doubleValue();
            x2 = jsonValues.getJsonNumber("x2").doubleValue();
            y1 = jsonValues.getJsonNumber("y1").doubleValue();
            y2 = jsonValues.getJsonNumber("y2").doubleValue();
        }catch (NullPointerException | NumberFormatException e) {
            x1 = 1D;
            x2 = 1D;
            y1 = 1D;
            y2 = 1D;
            CamouflageMod.logger.warning("error setting options " + e.getMessage());
        }
        //noinspection ConstantConditions
        if (jsonReader != null) {
            jsonReader.close();
        }
        return LinearScalingFunction.make(x1, x2, y1, y2);
    }

    private static Properties getProperties() {
        try {
            File configureFile = new File("mods/MightyMattockMod.properties");
            FileInputStream configureStream = new FileInputStream(configureFile);
            Properties configureProperties = new Properties();
            configureProperties.load(configureStream);
            return configureProperties;
        }catch (IOException e) {
            CamouflageMod.logger.warning(e.getMessage());
            return null;
        }
    }

    public static ConfigureOptions getInstance() {
        return instance;
    }

    LinearScalingFunction getCamouflageRecoveryScale() {
        return camouflageRecoveryScale;
    }

    LinearScalingFunction getSpellPowerExplainsCamouflageChance() {
        return spellPowerExplainsCamouflageChance;
    }

    public int getCastingEarthSeconds() {
        return castingEarthSeconds;
    }

    public int getCostFavor() {
        return costFavor;
    }

    public int getSpellDifficulty() {
        return spellDifficulty;
    }

    public int getRequiredFaith() {
        return requiredFaith;
    }

    public long getCoolDownEarthMilliseconds() {
        return coolDownEarthMilliseconds;
    }

    LinearScalingFunction getArmorDRExplainsCamouflageChance() {
        return armorDRExplainsCamouflageChance;
    }
}
