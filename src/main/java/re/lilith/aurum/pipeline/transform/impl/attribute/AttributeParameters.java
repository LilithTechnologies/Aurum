package re.lilith.aurum.pipeline.transform.impl.attribute;

import re.lilith.aurum.gbuffer.matching.InputAvailability;
import re.lilith.aurum.pipeline.transform.patch.Parameters;
import re.lilith.aurum.pipeline.transform.patch.Patch;

public class AttributeParameters extends Parameters {
    public final boolean hasGeometry;
    public final InputAvailability inputs;
    public final boolean scrollGlint;

    public AttributeParameters(Patch patch, boolean hasGeometry, InputAvailability inputs, boolean scrollGlint) {
        super(patch);
        this.hasGeometry = hasGeometry;
        this.inputs = inputs;
        this.scrollGlint = scrollGlint;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (hasGeometry ? 1231 : 1237);
        result = prime * result + ((inputs == null) ? 0 : inputs.hashCode());
        result = prime * result + (scrollGlint ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        AttributeParameters other = (AttributeParameters) obj;
        if (hasGeometry != other.hasGeometry)
            return false;
        if (scrollGlint != other.scrollGlint)
            return false;
        if (inputs == null) {
            return other.inputs == null;
        } else return inputs.equals(other.inputs);
    }
}
