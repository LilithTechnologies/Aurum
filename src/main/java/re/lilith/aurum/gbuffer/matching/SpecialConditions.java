package re.lilith.aurum.gbuffer.matching;

import re.lilith.aurum.Aurum;
import re.lilith.aurum.pipeline.WorldRenderingPipeline;

public final class SpecialConditions {
    private SpecialConditions() {
    }

    public static void set(SpecialCondition condition) {
        WorldRenderingPipeline pipeline = Aurum.getPipelineManager().getPipelineNullable();

        if (pipeline != null) {
            pipeline.setSpecialCondition(condition);
        }
    }
}
