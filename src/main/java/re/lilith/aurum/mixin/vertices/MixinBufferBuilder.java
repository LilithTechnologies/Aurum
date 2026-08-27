package re.lilith.aurum.mixin.vertices;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.client.render.VertexFormats;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.lilith.aurum.Aurum;
import re.lilith.aurum.celeritas.terrain.BlockContextHolder;
import re.lilith.aurum.gbuffer.BlockRenderingSettings;
import re.lilith.aurum.vertices.*;
import re.lilith.aurum.vertices.view.BufferBuilderPolygonView;

import java.nio.ByteBuffer;

/**
 * Dynamically and transparently extends the vanilla vertex formats with additional data
 */
@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilder implements BlockSensitiveBufferBuilder, ExtendingBufferBuilder {
    @Unique
    private boolean extending;

    @Unique
    private boolean aurum$shouldNotExtend = false;

    @Unique
    private boolean aurum$isTerrain = false;

    @Unique
    private int vertexCount;

    @Unique
    private final BufferBuilderPolygonView polygon = new BufferBuilderPolygonView();

    @Unique
    private final Vector3f normal = new Vector3f();

    @Unique
    private short currentBlock;

    @Unique
    private short currentRenderType;

    @Unique
    private byte currentBlockEmission;

    @Unique
    private int currentLocalPosX;

    @Unique
    private int currentLocalPosY;

    @Unique
    private int currentLocalPosZ;


    @Shadow
    private ByteBuffer buffer;

    @Shadow
    public int drawMode;

    @Shadow
    private VertexFormat format;

    @Shadow
    private int currentElementId;

    @Shadow
    private @Nullable VertexFormatElement currentElement;

    @Shadow
    public boolean building;

    @Shadow
    public abstract void begin(int drawMode, VertexFormat vertexFormat);

    @Shadow
    public abstract void end();

    @Shadow
    protected abstract void nextElement();

    @Inject(method = "begin", at = @At("HEAD"))
    private void aurum$recoverFromStuckBuild(int drawMode, VertexFormat format, CallbackInfo ci) {
        if (this.building) {
            Aurum.LOGGER.warn("A buffer was begun when it's already building, it will be ended to prevent a crash. This is a bug.");
            this.end();
        }
    }

    @Override
    public void aurum$beginWithoutExtending(int drawMode, VertexFormat vertexFormat) {
        aurum$shouldNotExtend = true;
        begin(drawMode, vertexFormat);
        aurum$shouldNotExtend = false;
    }

    @Inject(method = "begin", at = @At("HEAD"))
    private void aurum$onBegin(int drawMode, VertexFormat format, CallbackInfo ci) {
        boolean shouldExtend = (!aurum$shouldNotExtend) && BlockRenderingSettings.INSTANCE.shouldUseExtendedVertexFormat();
        extending = shouldExtend && (format == VertexFormats.BLOCK || format == VertexFormats.ENTITY);
        vertexCount = 0;
    }


    @Inject(method = "begin", at = @At("RETURN"))
    private void aurum$afterBegin(int drawMode, VertexFormat format, CallbackInfo ci) {

        if (extending) {
            if (format == VertexFormats.ENTITY) {
                this.format = AurumVertexFormats.ENTITY;
                this.aurum$isTerrain = false;
            } else {
                this.format = AurumVertexFormats.TERRAIN;
                this.aurum$isTerrain = true;
            }
            this.currentElement = this.format.getElements().getFirst();
        }
    }

    @Inject(method = "reset", at = @At("HEAD"))
    private void aurum$onReset(CallbackInfo ci) {
        extending = false;
        vertexCount = 0;
    }

    @Unique
    private int aurum$quadVertexCount;

    @Inject(method = "next", at = @At("HEAD"))
    private void aurum$recordBlockForQuad(CallbackInfo ci) {
        // Only the chunk meshing renderers set a block, so this records nothing for immediate mode geometry.
        if (currentBlock == -1) {
            return;
        }

        if (++aurum$quadVertexCount == 4) {
            aurum$quadVertexCount = 0;
            BlockContextHolder.get().recordQuad();
        }
    }

    @Inject(method = "next", at = @At("HEAD"))
    private void aurum$beforeNext(CallbackInfo ci) {
        if (!extending) {
            return;
        }

        if (aurum$isTerrain) {
            // ENTITY_ELEMENT
            this.putShort(0, currentBlock);
            this.putShort(2, currentRenderType);
            this.nextElement();
        }
        // MID_TEXTURE_ELEMENT
        this.putFloat(0);
        this.putFloat(4);
        this.nextElement();
        // TANGENT_ELEMENT
        this.putInt(0);
        this.nextElement();
        if (aurum$isTerrain) {
            // MID_BLOCK_ELEMENT
            int posIndex = this.currentElementId - 48;
            float x = buffer.getFloat(posIndex);
            float y = buffer.getFloat(posIndex + 4);
            float z = buffer.getFloat(posIndex + 8);
            this.putInt(ExtendedDataHelper.computeMidBlock(x, y, z, currentLocalPosX, currentLocalPosY, currentLocalPosZ, currentBlockEmission));
            this.nextElement();
        }

        vertexCount++;

        if (drawMode == GL11.GL_QUADS && vertexCount == 4 || drawMode == GL11.GL_TRIANGLES && vertexCount == 3) {
            fillExtendedData(vertexCount);
        }
    }

    @Unique
    private void fillExtendedData(int vertexAmount) {
        vertexCount = 0;

        int stride = format.getVertexSize();

        polygon.setup(buffer, currentElementId, stride, vertexAmount, aurum$isTerrain ? 16 : 12);

        float midU = 0;
        float midV = 0;

        for (int vertex = 0; vertex < vertexAmount; vertex++) {
            midU += polygon.u(vertex);
            midV += polygon.v(vertex);
        }

        midU /= vertexAmount;
        midV /= vertexAmount;

        if (vertexAmount == 3) {
            NormalHelper.computeFaceNormalTri(normal, polygon);
        } else {
            NormalHelper.computeFaceNormal(normal, polygon);
        }

        if (aurum$isTerrain && BlockContextHolder.get().isDoubleSidedQuad()) {
            normal.set(0.0f, 1.0f, 0.0f);
        }

        int packedNormal = NormalHelper.packNormal(normal, 0.0f);

        int tangent = NormalHelper.computeTangent(normal.x, normal.y, normal.z, polygon);

        int midUOffset;
        int midVOffset;
        int normalOffset;
        int tangentOffset;
        if (aurum$isTerrain) {
            midUOffset = 16;
            midVOffset = 12;
            normalOffset = 24;
            tangentOffset = 8;
        } else {
            midUOffset = 12;
            midVOffset = 8;
            normalOffset = 16;
            tangentOffset = 4;
        }

        for (int vertex = 0; vertex < vertexAmount; vertex++) {
            buffer.putFloat(currentElementId - midUOffset - stride * vertex, midU);
            buffer.putFloat(currentElementId - midVOffset - stride * vertex, midV);
            buffer.putInt(currentElementId - tangentOffset - stride * vertex, tangent);

            if (aurum$isTerrain) {
                buffer.putInt(currentElementId - normalOffset - stride * vertex, packedNormal);
            }
        }
    }

    @Override
    public void aurum$beginBlock(short block, short renderType, byte blockEmission, int localPosX, int localPosY, int localPosZ) {
        this.currentBlock = block;
        this.currentRenderType = renderType;
        this.currentBlockEmission = blockEmission;
        this.currentLocalPosX = localPosX;
        this.currentLocalPosY = localPosY;
        this.currentLocalPosZ = localPosZ;
    }

    @Override
    public void aurum$endBlock() {
        this.currentBlock = -1;
        this.currentRenderType = -1;
        this.currentBlockEmission = 0;
        this.currentLocalPosX = 0;
        this.currentLocalPosY = 0;
        this.currentLocalPosZ = 0;
    }

    @Unique
    private void putInt(int value) {
        this.buffer.putInt(this.currentElementId, value);
    }

    @Unique
    private void putShort(int i, short value) {
        this.buffer.putShort(this.currentElementId + i, value);
    }

    @Unique
    private void putFloat(int i) {
        this.buffer.putFloat(this.currentElementId + i, (float) 0);
    }
}
