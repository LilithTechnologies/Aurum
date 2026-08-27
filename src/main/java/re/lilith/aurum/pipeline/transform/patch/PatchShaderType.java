package re.lilith.aurum.pipeline.transform.patch;

import re.lilith.aurum.gl.shader.ShaderType;

public enum PatchShaderType {
    VERTEX(ShaderType.VERTEX),
    GEOMETRY(ShaderType.GEOMETRY),
    FRAGMENT(ShaderType.FRAGMENT);

    public final ShaderType glShaderType;

    PatchShaderType(ShaderType glShaderType) {
        this.glShaderType = glShaderType;
    }
}
