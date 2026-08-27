package re.lilith.aurum.uniforms;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.*;
import re.lilith.aurum.expression.BiomeCategories;
import re.lilith.aurum.gl.uniform.holder.UniformHolder;

import java.util.LinkedHashMap;
import java.util.Map;

import static re.lilith.aurum.gl.uniform.UniformUpdateFrequency.ONCE;
import static re.lilith.aurum.gl.uniform.UniformUpdateFrequency.PER_TICK;

public class BiomeUniforms {
    private static Biome cachedBiome = null;
    private static long cachedWorldTime = -1;
    private static int cachedPlayerX = Integer.MIN_VALUE;
    private static int cachedPlayerZ = Integer.MIN_VALUE;

    public static void addBiomeUniforms(UniformHolder uniforms) {
        uniforms.uniform1i(PER_TICK, "biome", BiomeUniforms::getBiomeId)
                .uniform1i(PER_TICK, "biome_category", BiomeUniforms::getBiomeCategory)
                .uniform1i(PER_TICK, "biome_precipitation", BiomeUniforms::getBiomePrecipitation)
                .uniform1f(PER_TICK, "rainfall", BiomeUniforms::getBiomeRainfall)
                .uniform1f(PER_TICK, "temperature", BiomeUniforms::getBiomeTemperature);

        addModernBiomeConstants(uniforms);
    }

    private static void addModernBiomeConstants(UniformHolder uniforms) {
        uniforms.uniform1i(ONCE, "BIOME_NETHER_WASTES", () -> -1000)
                .uniform1i(ONCE, "BIOME_SOUL_SAND_VALLEY", () -> -1001)
                .uniform1i(ONCE, "BIOME_CRIMSON_FOREST", () -> -1002)
                .uniform1i(ONCE, "BIOME_WARPED_FOREST", () -> -1003)
                .uniform1i(ONCE, "BIOME_BASALT_DELTAS", () -> -1004)
                .uniform1i(ONCE, "BIOME_LUSH_CAVES", () -> -1005)
                .uniform1i(ONCE, "BIOME_PALE_GARDEN", () -> -1006);
    }

    private static Biome getCachedBiome() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return null;
        }

        long worldTime = client.world.getTimeOfDay();
        Vec3d pos = client.player.getPos();
        int playerX = (int) Math.floor(pos.x);
        int playerZ = (int) Math.floor(pos.z);

        if (cachedBiome == null || cachedWorldTime != worldTime || cachedPlayerX != playerX || cachedPlayerZ != playerZ) {
            cachedBiome = client.world.getBiome(new BlockPos(playerX, (int) pos.y, playerZ));
            cachedWorldTime = worldTime;
            cachedPlayerX = playerX;
            cachedPlayerZ = playerZ;
        }

        return cachedBiome;
    }

    public static int getBiomePrecipitation() {
        Biome biome = getCachedBiome();
        if (biome == null || biome.downfall <= 0.0F) {
            return 0;
        }

        return biome.temperature > 0.15F ? 1 : 2;
    }

    public static float getBiomeRainfall() {
        Biome biome = getCachedBiome();
        return biome != null ? biome.downfall : 0.0F;
    }

    public static float getBiomeTemperature() {
        Biome biome = getCachedBiome();
        return biome != null ? biome.temperature : 0.0F;
    }

    public static int getBiomeId() {
        Biome biome = getCachedBiome();
        return biome != null ? biome.id : 0;
    }

    public static int getBiomeCategory() {
        Biome biome = getCachedBiome();
        if (biome == null) {
            return BiomeCategories.NONE.ordinal();
        }

        return determineBiomeCategory(biome).ordinal();
    }

    private static BiomeCategories determineBiomeCategory(Biome biome) {
        BiomeCategories category = getVanillaBiomeCategory(biome.id);
        if (category != null) {
            return category;
        }

        category = detectVanillaBiomeByClass(biome);
        if (category != null) {
            return category;
        }

        category = detectBiomeByName(biome);
        if (category != null) {
            return category;
        }

        category = detectBiomeByProperties(biome);
        if (category != null) {
            return category;
        }

        return BiomeCategories.NONE;
    }

    private static BiomeCategories getVanillaBiomeCategory(int biomeID) {
        return switch (biomeID) {
            case 0, 10, 24 -> BiomeCategories.OCEAN;
            case 1 -> BiomeCategories.PLAINS;
            case 2, 17 -> BiomeCategories.DESERT;
            case 3, 20, 34 -> BiomeCategories.EXTREME_HILLS;
            case 4, 18, 27, 28, 29 -> BiomeCategories.FOREST;
            case 5, 19, 30, 31, 32, 33 -> BiomeCategories.TAIGA;
            case 6 -> BiomeCategories.SWAMP;
            case 7, 11 -> BiomeCategories.RIVER;
            case 8 -> BiomeCategories.NETHER;
            case 9 -> BiomeCategories.THE_END;
            case 12, 13 -> BiomeCategories.ICY;
            case 14, 15 -> BiomeCategories.MUSHROOM;
            case 16, 25, 26 -> BiomeCategories.BEACH;
            case 21, 22, 23 -> BiomeCategories.JUNGLE;
            case 35, 36 -> BiomeCategories.SAVANNA;
            case 37, 38, 39 -> BiomeCategories.MESA;
            default -> null;
        };
    }

    private static final Map<Class<? extends Biome>, BiomeCategories> VANILLA_CLASS_MAP = createVanillaClassMap();

    private static Map<Class<? extends Biome>, BiomeCategories> createVanillaClassMap() {
        Map<Class<? extends Biome>, BiomeCategories> map = new LinkedHashMap<>();
        map.put(StoneBeachBiome.class, BiomeCategories.BEACH);
        map.put(BeachBiome.class, BiomeCategories.BEACH);
        map.put(MushroomBiome.class, BiomeCategories.MUSHROOM);
        map.put(OceanBiome.class, BiomeCategories.OCEAN);
        map.put(PlainsBiome.class, BiomeCategories.PLAINS);
        map.put(DesertBiome.class, BiomeCategories.DESERT);
        map.put(ExtremeHillsBiome.class, BiomeCategories.EXTREME_HILLS);
        map.put(ForestBiome.class, BiomeCategories.FOREST);
        map.put(TaigaBiome.class, BiomeCategories.TAIGA);
        map.put(SwampBiome.class, BiomeCategories.SWAMP);
        map.put(RiverBiome.class, BiomeCategories.RIVER);
        map.put(NetherBiome.class, BiomeCategories.NETHER);
        map.put(EndBiome.class, BiomeCategories.THE_END);
        map.put(JungleBiome.class, BiomeCategories.JUNGLE);
        map.put(SavannaBiome.class, BiomeCategories.SAVANNA);
        map.put(MesaBiome.class, BiomeCategories.MESA);
        return map;
    }

    private static BiomeCategories detectVanillaBiomeByClass(Biome biome) {
        BiomeCategories direct = VANILLA_CLASS_MAP.get(biome.getClass());
        if (direct != null) {
            return direct;
        }

        for (Map.Entry<Class<? extends Biome>, BiomeCategories> entry : VANILLA_CLASS_MAP.entrySet()) {
            if (entry.getKey().isInstance(biome)) {
                return entry.getValue();
            }
        }

        return null;
    }

    private static BiomeCategories detectBiomeByName(Biome biome) {
        if (biome.name == null) return null;

        String name = biome.name.toLowerCase();

        if (name.contains("nether") || name.contains("hell")) return BiomeCategories.NETHER;
        if (name.contains("end") || name.contains("sky")) return BiomeCategories.THE_END;

        if (name.contains("ocean") || name.contains("sea")) return BiomeCategories.OCEAN;
        if (name.contains("river") || name.contains("stream")) return BiomeCategories.RIVER;
        if (name.contains("beach") || name.contains("shore") || name.contains("coast")) return BiomeCategories.BEACH;

        if (name.contains("mushroom")) return BiomeCategories.MUSHROOM;
        if (name.contains("swamp") || name.contains("marsh") || name.contains("bog")) return BiomeCategories.SWAMP;

        if (name.contains("jungle")) return BiomeCategories.JUNGLE;
        if (name.contains("savanna")) return BiomeCategories.SAVANNA;
        if (name.contains("mesa") || name.contains("badlands")) return BiomeCategories.MESA;
        if (name.contains("desert")) return BiomeCategories.DESERT;

        if (name.contains("mountain") || name.contains("peak") || name.contains("alpine") ||
                name.contains("cliff") || name.contains("crag")) return BiomeCategories.MOUNTAIN;

        if (name.contains("ice") || name.contains("frozen") || name.contains("snow") ||
                name.contains("arctic") || name.contains("tundra") || name.contains("glacier"))
            return BiomeCategories.ICY;

        if (name.contains("taiga") || name.contains("boreal") || name.contains("conifer")) return BiomeCategories.TAIGA;

        if (name.contains("forest") || name.contains("wood") || name.contains("grove") ||
                name.contains("thicket")) return BiomeCategories.FOREST;

        if (name.contains("plain") || name.contains("field") || name.contains("meadow") ||
                name.contains("grassland") || name.contains("prairie")) return BiomeCategories.PLAINS;

        return null;
    }

    private static BiomeCategories detectBiomeByProperties(Biome biome) {
        float temp = biome.temperature;
        float rain = biome.downfall;

        if (temp <= 0.0F && rain > 0.0F) {
            return BiomeCategories.ICY;
        }

        if (temp >= 2.0F && rain <= 0.0F) {
            return BiomeCategories.DESERT;
        }

        if (temp >= 1.0F && rain <= 0.1F) {
            return BiomeCategories.SAVANNA;
        }

        if (rain >= 0.85F && temp >= 0.5F && temp <= 1.0F) {
            return BiomeCategories.SWAMP;
        }

        if (temp >= 0.0F && temp <= 0.4F && rain >= 0.4F) {
            return BiomeCategories.TAIGA;
        }

        if (temp >= 0.4F && temp <= 0.9F && rain >= 0.5F) {
            return BiomeCategories.FOREST;
        }

        if (temp >= 0.4F && temp <= 1.0F && rain >= 0.3F && rain <= 0.6F) {
            return BiomeCategories.PLAINS;
        }

        return null;
    }
}
