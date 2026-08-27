package re.lilith.aurum.gbuffer.state;

import re.lilith.aurum.gbuffer.matching.InputAvailability;

public class StateTracker {
    public static final StateTracker INSTANCE = new StateTracker();

    // blocks atlas
    public boolean albedoSampler;
    // lightmap
    public boolean lightmapSampler;
    // overlay
    public boolean overlaySampler;

    public boolean compilingDisplayList;

    public InputAvailability getInputs() {
        return new InputAvailability(albedoSampler,
                lightmapSampler,
                overlaySampler);
    }
}
