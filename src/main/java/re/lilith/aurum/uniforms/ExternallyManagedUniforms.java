package re.lilith.aurum.uniforms;

import re.lilith.aurum.gl.uniform.UniformType;
import re.lilith.aurum.gl.uniform.holder.UniformHolder;

public class ExternallyManagedUniforms {
    private ExternallyManagedUniforms() {
        // no construction allowed
    }

    public static void addExternallyManagedUniforms(UniformHolder uniformHolder) {
        addMat4(uniformHolder, "aurum_ModelViewMatrix");
        addMat4(uniformHolder, "u_ModelViewProjectionMatrix");
        addMat4(uniformHolder, "aurum_NormalMatrix");
    }

    public static void addExternallyManagedUniforms116(UniformHolder uniformHolder) {
        addExternallyManagedUniforms(uniformHolder);

        uniformHolder.externallyManagedUniform("u_ModelScale", UniformType.VEC3);
        uniformHolder.externallyManagedUniform("u_TextureScale", UniformType.VEC2);
    }

    private static void addMat4(UniformHolder uniformHolder, String name) {
        uniformHolder.externallyManagedUniform(name, UniformType.MAT4);
    }
}
