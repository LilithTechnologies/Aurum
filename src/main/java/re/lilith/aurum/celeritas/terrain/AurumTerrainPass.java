package re.lilith.aurum.celeritas.terrain;

public enum AurumTerrainPass {
    SHADOW("shadow"),
    GBUFFER_SOLID("gbuffers_terrain"),
    GBUFFER_TRANSLUCENT("gbuffers_water");

    private final String name;

    AurumTerrainPass(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
