package re.lilith.aurum.gl.program;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBShaderImageLoadStore;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.gl.state.ValueUpdateNotifier;
import re.lilith.aurum.gl.uniform.Uniform;
import re.lilith.aurum.gl.uniform.UniformType;
import re.lilith.aurum.gl.uniform.UniformUpdateFrequency;
import re.lilith.aurum.gl.uniform.holder.DynamicLocationalUniformHolder;
import re.lilith.aurum.gl.uniform.holder.UniformHolder;
import re.lilith.aurum.uniforms.SystemTimeUniforms;

import java.nio.IntBuffer;
import java.util.*;

public class ProgramUniforms {
    private static ProgramUniforms active;
    private final ImmutableList<Uniform> perTick;
    private final ImmutableList<Uniform> perFrame;
    private final ImmutableList<Uniform> dynamic;
    private final ImmutableList<ValueUpdateNotifier> notifiersToReset;

    private ImmutableList<Uniform> once;
    long lastTick = -1;
    int lastFrame = -1;

    public ProgramUniforms(ImmutableList<Uniform> once, ImmutableList<Uniform> perTick, ImmutableList<Uniform> perFrame,
                           ImmutableList<Uniform> dynamic, ImmutableList<ValueUpdateNotifier> notifiersToReset) {
        this.once = once;
        this.perTick = perTick;
        this.perFrame = perFrame;
        this.dynamic = dynamic;
        this.notifiersToReset = notifiersToReset;
    }

    private void updateStage(ImmutableList<Uniform> uniforms) {
        for (Uniform uniform : uniforms) {
            uniform.update();
        }
    }

    private static long getCurrentTick() {
        return Objects.requireNonNull(MinecraftClient.getInstance().world).getTimeOfDay();
    }

    public void update() {
        if (active != null) {
            active.removeListeners();
        }

        active = this;

        updateStage(dynamic);

        if (once != null) {
            updateStage(once);
            updateStage(perTick);
            updateStage(perFrame);
            lastTick = getCurrentTick();

            once = null;
            return;
        }

        long currentTick = getCurrentTick();

        if (lastTick != currentTick) {
            lastTick = currentTick;

            updateStage(perTick);
        }

        // TODO: Move the frame counter to a different place?
        int currentFrame = SystemTimeUniforms.COUNTER.getAsInt();

        if (lastFrame != currentFrame) {
            lastFrame = currentFrame;

            updateStage(perFrame);
        }
    }

    public void removeListeners() {
        active = null;

        for (ValueUpdateNotifier notifier : notifiersToReset) {
            notifier.setListener(null);
        }
    }

    public static void clearActiveUniforms() {
        if (active != null) {
            active.removeListeners();
        }
    }

    public static Builder builder(String name, int program) {
        return new Builder(name, program);
    }

    public static class Builder implements DynamicLocationalUniformHolder {
        private final String name;
        private final int program;

        private final Map<Integer, String> locations;
        private final Map<String, Uniform> once;
        private final Map<String, Uniform> perTick;
        private final Map<String, Uniform> perFrame;
        private final Map<String, Uniform> dynamic;
        private final Map<String, UniformType> uniformNames;
        private final Map<String, UniformType> externalUniformNames;
        private final List<ValueUpdateNotifier> notifiersToReset;

        protected Builder(String name, int program) {
            this.name = name;
            this.program = program;

            locations = new HashMap<>();
            once = new HashMap<>();
            perTick = new HashMap<>();
            perFrame = new HashMap<>();
            dynamic = new HashMap<>();
            uniformNames = new HashMap<>();
            externalUniformNames = new HashMap<>();
            notifiersToReset = new ArrayList<>();
        }

        @Override
        public void addUniform(UniformUpdateFrequency updateFrequency, Uniform uniform) {
            Objects.requireNonNull(uniform);

            switch (updateFrequency) {
                case ONCE:
                    once.put(locations.get(uniform.getLocation()), uniform);
                    break;
                case PER_TICK:
                    perTick.put(locations.get(uniform.getLocation()), uniform);
                    break;
                case PER_FRAME:
                    perFrame.put(locations.get(uniform.getLocation()), uniform);
                    break;
            }

        }

        @Override
        public OptionalInt location(String name, UniformType type) {
            int id = AurumRenderSystem.getUniformLocation(program, name);

            if (id == -1) {
                return OptionalInt.empty();
            }

            if (!locations.containsKey(id) && !uniformNames.containsKey(name)) {
                locations.put(id, name);
                uniformNames.put(name, type);
            } else {
//				Aurum.logger.warn("[" + this.name + "] Duplicate uniform: " + type.toString().toLowerCase() + " " + name);

                return OptionalInt.empty();
            }

            return OptionalInt.of(id);
        }

        public ProgramUniforms buildUniforms() {
            // Check for any unsupported uniforms and warn about them so that we can easily figure out what uniforms we
            // need to add.
            int activeUniforms = GL20C.glGetProgrami(program, GL20C.GL_ACTIVE_UNIFORMS);
            IntBuffer sizeBuf = BufferUtils.createIntBuffer(1);
            IntBuffer typeBuf = BufferUtils.createIntBuffer(1);

            for (int index = 0; index < activeUniforms; index++) {
                String name = AurumRenderSystem.getActiveUniform(program, index, 128, sizeBuf, typeBuf);

                if (name.isEmpty()) {
                    // No further information available.
                    continue;
                }

                int type = typeBuf.get(0);

                UniformType provided = uniformNames.get(name);
                UniformType expected = getExpectedType(type);

                if (provided == null && !name.startsWith("gl_")) {
                    if (isSampler(type) || isImage(type)) {
                        // don't print a warning, samplers and images are managed elsewhere
                        continue;
                    }

                    UniformType externalProvided = externalUniformNames.get(name);

                    if (externalProvided != null) {
                        if (externalProvided != expected) {
                            String expectedName;

                            if (expected != null) {
                                expectedName = expected.toString();
                            } else {
                                expectedName = "(unsupported type: " + getTypeName(type) + ")";
                            }

                            Aurum.LOGGER.error("[{}] Wrong uniform type for externally-managed uniform {}: {} is provided but the program expects {}.", this.name, name, externalProvided, expectedName);
                        }

                        continue;
                    }
                    continue;
                }

                if (provided != null && provided != expected) {
                    String expectedName;

                    if (expected != null) {
                        expectedName = expected.toString();
                    } else {
                        expectedName = "(unsupported type: " + getTypeName(type) + ")";
                    }

                    Aurum.LOGGER.error("[{}] Wrong uniform type for {}: Aurum is providing {} but the program expects {}. Disabling that uniform.", this.name, name, provided, expectedName);

                    once.remove(name);
                    perTick.remove(name);
                    perFrame.remove(name);
                    dynamic.remove(name);
                }
            }

            return new ProgramUniforms(ImmutableList.copyOf(once.values()), ImmutableList.copyOf(perTick.values()), ImmutableList.copyOf(perFrame.values()),
                    ImmutableList.copyOf(dynamic.values()), ImmutableList.copyOf(notifiersToReset));
        }

        @Override
        public void addDynamicUniform(Uniform uniform, ValueUpdateNotifier notifier) {
            Objects.requireNonNull(uniform);
            Objects.requireNonNull(notifier);

            dynamic.put(locations.get(uniform.getLocation()), uniform);
            notifiersToReset.add(notifier);

        }

        @Override
        public UniformHolder externallyManagedUniform(String name, UniformType type) {
            externalUniformNames.put(name, type);

            return this;
        }
    }

    private static String getTypeName(int type) {
        String typeName;

        if (type == GL20C.GL_FLOAT) {
            typeName = "float";
        } else if (type == GL20C.GL_INT) {
            typeName = "int";
        } else if (type == GL20C.GL_BOOL) {
            typeName = "bool";
        } else if (type == GL20C.GL_BOOL_VEC2) {
            typeName = "bvec2";
        } else if (type == GL20C.GL_BOOL_VEC3) {
            typeName = "bvec3";
        } else if (type == GL20C.GL_BOOL_VEC4) {
            typeName = "bvec4";
        } else if (type == GL20C.GL_FLOAT_MAT4) {
            typeName = "mat4";
        } else if (type == GL20C.GL_FLOAT_VEC4) {
            typeName = "vec4";
        } else if (type == GL20C.GL_FLOAT_MAT3) {
            typeName = "mat3";
        } else if (type == GL20C.GL_FLOAT_VEC3) {
            typeName = "vec3";
        } else if (type == GL20C.GL_FLOAT_MAT2) {
            typeName = "mat2";
        } else if (type == GL20C.GL_FLOAT_VEC2) {
            typeName = "vec2";
        } else if (type == GL20C.GL_INT_VEC2) {
            typeName = "ivec2";
        } else if (type == GL20C.GL_INT_VEC4) {
            typeName = "ivec4";
        } else if (type == GL20C.GL_SAMPLER_3D) {
            typeName = "sampler3D";
        } else if (type == GL20C.GL_SAMPLER_2D) {
            typeName = "sampler2D";
        } else if (type == GL30C.GL_UNSIGNED_INT_SAMPLER_2D) {
            typeName = "usampler2D";
        } else if (type == GL30C.GL_UNSIGNED_INT_SAMPLER_3D) {
            typeName = "usampler3D";
        } else if (type == GL20C.GL_SAMPLER_1D) {
            typeName = "sampler1D";
        } else if (type == GL20C.GL_SAMPLER_2D_SHADOW) {
            typeName = "sampler2DShadow";
        } else if (type == GL20C.GL_SAMPLER_1D_SHADOW) {
            typeName = "sampler1DShadow";
        } else if (type == ARBShaderImageLoadStore.GL_IMAGE_2D) {
            typeName = "image2D";
        } else if (type == ARBShaderImageLoadStore.GL_IMAGE_3D) {
            typeName = "image3D";
        } else {
            typeName = "(unknown:" + type + ")";
        }

        return typeName;
    }

    private static UniformType getExpectedType(int type) {
        if (type == GL20C.GL_FLOAT) {
            return UniformType.FLOAT;
        } else if (type == GL20C.GL_INT || type == GL20C.GL_BOOL) {
            return UniformType.INT;
        } else if (type == GL20C.GL_BOOL_VEC2) {
            return UniformType.VEC2I;
        } else if (type == GL20C.GL_BOOL_VEC3) {
            return UniformType.VEC3I;
        } else if (type == GL20C.GL_BOOL_VEC4) {
            return UniformType.VEC4I;
        } else if (type == GL20C.GL_FLOAT_MAT4) {
            return UniformType.MAT4;
        } else if (type == GL20C.GL_FLOAT_VEC4) {
            return UniformType.VEC4;
        } else if (type == GL20C.GL_INT_VEC4) {
            return UniformType.VEC4I;
        } else if (type == GL20C.GL_FLOAT_MAT3) {
            return UniformType.MAT3;
        } else if (type == GL20C.GL_FLOAT_VEC3) {
            return UniformType.VEC3;
        } else if (type == GL20C.GL_INT_VEC3) {
            return UniformType.VEC3I;
        } else if (type == GL20C.GL_FLOAT_MAT2) {
            return null;
        } else if (type == GL20C.GL_FLOAT_VEC2) {
            return UniformType.VEC2;
        } else if (type == GL20C.GL_INT_VEC2) {
            return UniformType.VEC2I;
        } else if (type == GL20C.GL_SAMPLER_3D) {
            return UniformType.INT;
        } else if (type == GL20C.GL_SAMPLER_2D) {
            return UniformType.INT;
        } else if (type == GL30C.GL_UNSIGNED_INT_SAMPLER_2D) {
            return UniformType.INT;
        } else if (type == GL30C.GL_UNSIGNED_INT_SAMPLER_3D) {
            return UniformType.INT;
        } else if (type == GL20C.GL_SAMPLER_1D) {
            return UniformType.INT;
        } else if (type == GL20C.GL_SAMPLER_2D_SHADOW) {
            return UniformType.INT;
        } else if (type == GL20C.GL_SAMPLER_1D_SHADOW) {
            return UniformType.INT;
        } else {
            return null;
        }
    }

    private static boolean isSampler(int type) {
        return type == GL20C.GL_SAMPLER_1D
                || type == GL20C.GL_SAMPLER_2D
                || type == GL30C.GL_UNSIGNED_INT_SAMPLER_2D
                || type == GL30C.GL_UNSIGNED_INT_SAMPLER_3D
                || type == GL20C.GL_SAMPLER_3D
                || type == GL20C.GL_SAMPLER_1D_SHADOW
                || type == GL20C.GL_SAMPLER_2D_SHADOW;
    }

    private static boolean isImage(int type) {
        return type == ARBShaderImageLoadStore.GL_IMAGE_1D
                || type == ARBShaderImageLoadStore.GL_IMAGE_2D
                || type == ARBShaderImageLoadStore.GL_UNSIGNED_INT_IMAGE_2D
                || type == ARBShaderImageLoadStore.GL_IMAGE_3D
                || type == ARBShaderImageLoadStore.GL_IMAGE_1D_ARRAY
                || type == ARBShaderImageLoadStore.GL_IMAGE_2D_ARRAY;
    }
}
