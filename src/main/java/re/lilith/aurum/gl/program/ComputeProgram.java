package re.lilith.aurum.gl.program;

import org.joml.Vector2f;
import org.joml.Vector3i;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL43C;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.gl.GlResource;
import re.lilith.aurum.pipeline.WorldRenderingPipeline;
import re.lilith.aurum.shaderpack.FilledIndirectPointer;

import java.util.Objects;

public final class ComputeProgram extends GlResource {
    private final ProgramUniforms uniforms;
    private final ProgramSamplers samplers;
    private final ProgramImages images;
    private Vector3i absoluteWorkGroups;
    private Vector2f relativeWorkGroups;
    private FilledIndirectPointer indirectPointer;
    private final int[] localSize;
    private float cachedWidth;
    private float cachedHeight;
    private Vector3i cachedWorkGroups;

    ComputeProgram(int program, ProgramUniforms uniforms, ProgramSamplers samplers, ProgramImages images) {
        super(program);

        localSize = new int[3];
        AurumRenderSystem.getProgramiv(program, GL43C.GL_COMPUTE_WORK_GROUP_SIZE, localSize);
        this.uniforms = uniforms;
        this.samplers = samplers;
        this.images = images;
    }

    public void setWorkGroupInfo(Vector2f relativeWorkGroups, Vector3i absoluteWorkGroups, FilledIndirectPointer indirectPointer) {
        this.relativeWorkGroups = relativeWorkGroups;
        this.absoluteWorkGroups = absoluteWorkGroups;
        this.indirectPointer = indirectPointer;
    }

    public Vector3i getWorkGroups(float width, float height) {
        if (indirectPointer != null) return null;

        if (cachedWidth != width || cachedHeight != height || cachedWorkGroups == null) {
            this.cachedWidth = width;
            this.cachedHeight = height;
            if (this.absoluteWorkGroups != null) {
                this.cachedWorkGroups = this.absoluteWorkGroups;
            } else if (relativeWorkGroups != null) {
                // TODO: This is my best guess at what OptiFine does. Can this be confirmed?
                // Do not use actual localSize here, apparently that's not what we want.
                this.cachedWorkGroups = new Vector3i((int) Math.ceil(Math.ceil((width * relativeWorkGroups.x)) / localSize[0]), (int) Math.ceil(Math.ceil((height * relativeWorkGroups.y)) / localSize[1]), 1);
            } else {
                this.cachedWorkGroups = new Vector3i((int) Math.ceil(width / localSize[0]), (int) Math.ceil(height / localSize[1]), 1);
            }
        }

        return cachedWorkGroups;
    }

    public void dispatch(float width, float height) {
        ProgramSamplers.clearActiveSamplers();
        GL20.glUseProgram(getGlId());
        uniforms.update();
        samplers.update();
        images.update();

        if (!Aurum.getPipelineManager().getPipeline().map(WorldRenderingPipeline::allowConcurrentCompute).orElse(false)) {
            AurumRenderSystem.memoryBarrier(40);
        }

        if (indirectPointer != null) {
            AurumRenderSystem.bindBuffer(GL43C.GL_DISPATCH_INDIRECT_BUFFER, indirectPointer.buffer());
            AurumRenderSystem.dispatchComputeIndirect(indirectPointer.offset());
        } else {
            AurumRenderSystem.dispatchCompute(Objects.requireNonNull(getWorkGroups(width, height)));
        }
    }

    public void destroyInternal() {
        GL20.glDeleteProgram(getGlId());
    }

    /**
     * @return the OpenGL ID of this program.
     * @deprecated this should be encapsulated eventually
     */
    @Deprecated
    public int getProgramId() {
        return getGlId();
    }
}
