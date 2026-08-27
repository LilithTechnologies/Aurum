package re.lilith.aurum.gl.program;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.Nullable;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.gl.image.ImageHolder;
import re.lilith.aurum.gl.sampler.SamplerHolder;
import re.lilith.aurum.gl.shader.GlShader;
import re.lilith.aurum.gl.shader.ProgramCreator;
import re.lilith.aurum.gl.shader.ShaderType;
import re.lilith.aurum.gl.state.ValueUpdateNotifier;
import re.lilith.aurum.gl.texture.InternalTextureFormat;
import re.lilith.aurum.pipeline.transform.ShaderTransformer;

import java.util.function.IntSupplier;

public class ProgramBuilder extends ProgramUniforms.Builder implements SamplerHolder, ImageHolder {
    private final int program;
    private final ProgramSamplers.Builder samplers;
    private final ProgramImages.Builder images;

    private ProgramBuilder(String name, int program, ImmutableSet<Integer> reservedTextureUnits) {
        super(name, program);

        this.program = program;
        this.samplers = ProgramSamplers.builder(program, reservedTextureUnits);
        this.images = ProgramImages.builder(program);
    }

    public void bindAttributeLocation(int index, String name) {
        AurumRenderSystem.bindAttributeLocation(program, index, name);
    }

    public static ProgramBuilder begin(String name, @Nullable String vertexSource, @Nullable String geometrySource,
                                       @Nullable String fragmentSource, ImmutableSet<Integer> reservedTextureUnits) {
        GlShader vertex;
        GlShader geometry;
        GlShader fragment;

        vertex = buildShader(ShaderType.VERTEX, name + ".vsh", vertexSource);

        if (geometrySource != null) {
            geometry = buildShader(ShaderType.GEOMETRY, name + ".gsh", geometrySource);
        } else {
            geometry = null;
        }

        fragment = buildShader(ShaderType.FRAGMENT, name + ".fsh", fragmentSource);

        int programId;

        if (geometry != null) {
            programId = ProgramCreator.create(name, vertex, geometry, fragment);
        } else {
            programId = ProgramCreator.create(name, vertex, fragment);
        }

        vertex.destroy();

        if (geometry != null) {
            geometry.destroy();
        }

        fragment.destroy();

        return new ProgramBuilder(name, programId, reservedTextureUnits);
    }

    public static ProgramBuilder beginCompute(String name, @Nullable String source, ImmutableSet<Integer> reservedTextureUnits) {
        if (!AurumRenderSystem.supportsCompute()) {
            throw new IllegalStateException("This PC does not support compute shaders, but it's attempting to be used???");
        }

        GlShader compute = buildShader(ShaderType.COMPUTE, name + ".csh", ShaderTransformer.patchCompute(source));

        int programId = ProgramCreator.create(name, compute);

        compute.destroy();

        return new ProgramBuilder(name, programId, reservedTextureUnits);
    }

    public Program build() {
        return new Program(program, super.buildUniforms(), this.samplers.build(), this.images.build());
    }

    public ComputeProgram buildCompute() {
        return new ComputeProgram(program, super.buildUniforms(), this.samplers.build(), this.images.build());
    }

    private static GlShader buildShader(ShaderType shaderType, String name, @Nullable String source) {
        try {
            return new GlShader(shaderType, name, source);
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to compile " + shaderType + " shader for program " + name, e);
        }
    }

    @Override
    public void addExternalSampler(int textureUnit, String... names) {
        samplers.addExternalSampler(textureUnit, names);
    }

    @Override
    public boolean hasSampler(String name) {
        return samplers.hasSampler(name);
    }

    @Override
    public boolean addDefaultSampler(IntSupplier sampler, String... names) {
        return samplers.addDefaultSampler(sampler, names);
    }

    @Override
    public boolean addDynamicSampler(IntSupplier sampler, String... names) {
        return samplers.addDynamicSampler(sampler, names);
    }

    public boolean addDynamicSampler(IntSupplier sampler, ValueUpdateNotifier notifier, String... names) {
        return samplers.addDynamicSampler(sampler, notifier, names);
    }

    @Override
    public boolean hasImage(String name) {
        return images.hasImage(name);
    }

    @Override
    public void addTextureImage(IntSupplier textureID, InternalTextureFormat internalFormat, String name) {
        images.addTextureImage(textureID, internalFormat, name);
    }
}
