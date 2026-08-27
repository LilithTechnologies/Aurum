package re.lilith.aurum.celeritas.terrain;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.Texture;
import org.embeddedt.embeddium.impl.gl.shader.ShaderBindingContext;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloat3v;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformMatrix4f;
import org.embeddedt.embeddium.impl.gl.tessellation.GlPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.QuadPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderInterface;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderTextureSlot;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import re.lilith.aurum.gl.AurumRenderSystem;
import re.lilith.aurum.gl.program.ProgramImages;
import re.lilith.aurum.gl.program.ProgramSamplers;
import re.lilith.aurum.gl.program.ProgramUniforms;
import re.lilith.aurum.mixin.access.GameRendererAccessor;
import re.lilith.aurum.pipeline.samplers.AurumSamplers;

/**
 * Drives an Aurum terrain program from Celeritas' chunk renderer.
 *
 * <p>Celeritas supplies the projection matrix, the model view matrix and the region offset. Everything else that the
 * shader pack asks for comes from the Aurum uniform, sampler and image holders.</p>
 */
public class AurumChunkShaderInterface implements ChunkShaderInterface {
    private final @Nullable GlUniformMatrix4f uniformModelViewMatrix;
    private final @Nullable GlUniformMatrix4f uniformProjectionMatrix;
    private final @Nullable GlUniformMatrix4f uniformModelViewProjectionMatrix;
    private final @Nullable GlUniformMatrix4f uniformNormalMatrix;
    private final @Nullable GlUniformFloat3v uniformRegionOffset;

    private final int uniformModelScaleLocation;
    private final int uniformTextureScaleLocation;

    private final @Nullable ProgramUniforms aurumProgramUniforms;
    private final @Nullable ProgramSamplers aurumProgramSamplers;
    private final @Nullable ProgramImages aurumProgramImages;

    private final Matrix4f modelView = new Matrix4f();
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f modelViewProjection = new Matrix4f();
    private final Matrix4f normalMatrix = new Matrix4f();

    private final AurumTerrainPass pass;

    private GlPrimitiveType primitiveType = GlPrimitiveType.TRIANGLES;

    public AurumChunkShaderInterface(ShaderBindingContext context, int programId, AurumTerrainPass pass,
                                     @Nullable ProgramUniforms aurumProgramUniforms,
                                     @Nullable ProgramSamplers aurumProgramSamplers, @Nullable ProgramImages aurumProgramImages) {
        this.pass = pass;
        this.uniformModelViewMatrix = context.bindUniformIfPresent("aurum_ModelViewMatrix", GlUniformMatrix4f::new);
        this.uniformProjectionMatrix = context.bindUniformIfPresent("aurum_ProjectionMatrix", GlUniformMatrix4f::new);
        this.uniformModelViewProjectionMatrix = context.bindUniformIfPresent("u_ModelViewProjectionMatrix", GlUniformMatrix4f::new);
        this.uniformNormalMatrix = context.bindUniformIfPresent("aurum_NormalMatrix", GlUniformMatrix4f::new);
        this.uniformRegionOffset = context.bindUniformIfPresent("u_RegionOffset", GlUniformFloat3v::new);

        // Celeritas has no vec2 uniform holder, so these two are set directly.
        this.uniformModelScaleLocation = AurumRenderSystem.getUniformLocation(programId, "u_ModelScale");
        this.uniformTextureScaleLocation = AurumRenderSystem.getUniformLocation(programId, "u_TextureScale");

        this.aurumProgramUniforms = aurumProgramUniforms;
        this.aurumProgramSamplers = aurumProgramSamplers;
        this.aurumProgramImages = aurumProgramImages;
    }

    @Override
    public void setupState(TerrainRenderPass pass) {

        this.primitiveType = pass.primitiveType() == QuadPrimitiveType.DIRECT
                ? GlPrimitiveType.QUADS
                : GlPrimitiveType.TRIANGLES;

        ChunkVertexType vertexType = pass.vertexType();

        if (this.uniformModelScaleLocation != -1) {
            float scale = vertexType.getPositionScale();
            AurumRenderSystem.uniform3f(this.uniformModelScaleLocation, scale, scale, scale);
        }

        if (this.uniformTextureScaleLocation != -1) {
            float scale = vertexType.getTextureScale();
            AurumRenderSystem.uniform2f(this.uniformTextureScaleLocation, scale, scale);
        }

        MinecraftClient client = MinecraftClient.getInstance();
        Texture atlas = client.getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
        NativeImageBackedTexture lightmap = ((GameRendererAccessor) client.gameRenderer).getLightTexture();

        if (atlas != null) {
            GlStateManager.activeTexture(GL13.GL_TEXTURE0 + AurumSamplers.ALBEDO_TEXTURE_UNIT);
            GlStateManager.bindTexture(atlas.getGlId());
        }

        if (lightmap != null) {
            GlStateManager.activeTexture(GL13.GL_TEXTURE0 + AurumSamplers.LIGHTMAP_TEXTURE_UNIT);
            GlStateManager.bindTexture(lightmap.getGlId());
        }

        GlStateManager.activeTexture(GL13.GL_TEXTURE0);

        if (this.aurumProgramUniforms != null) {
            this.aurumProgramUniforms.update();
        }

        if ("cutout_mipped".equals(pass.name())) {
            GlStateManager.enableAlphaTest();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        } else {
            GlStateManager.disableAlphaTest();
        }

        if (this.aurumProgramSamplers != null) {
            this.aurumProgramSamplers.update();
        }

        if (this.aurumProgramImages != null) {
            this.aurumProgramImages.update();
        }
    }

    @Override
    public void restoreState() {
        ProgramUniforms.clearActiveUniforms();
        ProgramSamplers.clearActiveSamplers();
    }

    @Override
    public GlPrimitiveType getPrimitiveType() {
        return this.primitiveType;
    }

    @Override
    public void setProjectionMatrix(Matrix4fc matrix) {
        this.projection.set(matrix);

        if (this.uniformProjectionMatrix != null) {
            this.uniformProjectionMatrix.set(this.projection);
        }

        this.updateDerivedMatrices();
    }

    @Override
    public void setModelViewMatrix(Matrix4fc matrix) {
        this.modelView.set(matrix);

        if (this.uniformModelViewMatrix != null) {
            this.uniformModelViewMatrix.set(this.modelView);
        }

        if (this.uniformNormalMatrix != null) {
            this.normalMatrix.set(this.modelView);
            this.normalMatrix.setTranslation(0.0F, 0.0F, 0.0F);
            this.normalMatrix.invert();
            this.normalMatrix.transpose();
            this.uniformNormalMatrix.set(this.normalMatrix);
        }

        this.updateDerivedMatrices();
    }

    private void updateDerivedMatrices() {
        if (this.uniformModelViewProjectionMatrix != null) {
            this.projection.mul(this.modelView, this.modelViewProjection);
            this.uniformModelViewProjectionMatrix.set(this.modelViewProjection);
        }
    }

    @Override
    public void setRegionOffset(float x, float y, float z) {
        if (this.uniformRegionOffset != null) {
            this.uniformRegionOffset.set(x, y, z);
        }
    }

    @Override
    public void setTextureSlot(ChunkShaderTextureSlot slot, int val) {
        // Aurum binds every sampler that the shader pack asks for through ProgramSamplers.
    }
}
