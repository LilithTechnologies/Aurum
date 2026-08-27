package re.lilith.aurum.celeritas.terrain;

import org.embeddedt.embeddium.impl.render.chunk.LocalSectionIndex;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import re.lilith.aurum.vertices.ExtendedDataHelper;
import re.lilith.aurum.vertices.NormalHelper;
import re.lilith.aurum.vertices.view.TriView;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

/**
 * Writes terrain vertices in the {@link AurumChunkVertexType} layout.
 */
public class AurumChunkVertexEncoder implements ChunkVertexEncoder, TriView {
    private static final int LIGHT_OFFSET = AurumChunkVertexType.VERTEX_FORMAT.getAttribute("aurum_LightCoord").getPointer();
    private static final int SECTION_OFFSET = AurumChunkVertexType.VERTEX_FORMAT.getAttribute("aurum_SectionOffset").getPointer();
    private static final int NORMAL_OFFSET = AurumChunkVertexType.VERTEX_FORMAT.getAttribute("aurum_Normal").getPointer();
    private static final int MID_TEX_OFFSET = AurumChunkVertexType.VERTEX_FORMAT.getAttribute("mc_midTexCoord").getPointer();
    private static final int TANGENT_OFFSET = AurumChunkVertexType.VERTEX_FORMAT.getAttribute("at_tangent").getPointer();
    private static final int ENTITY_OFFSET = AurumChunkVertexType.VERTEX_FORMAT.getAttribute("mc_Entity").getPointer();
    private static final int MID_BLOCK_OFFSET = AurumChunkVertexType.VERTEX_FORMAT.getAttribute("at_midBlock").getPointer();

    private static final int STRIDE = AurumChunkVertexType.STRIDE;

    private final ChunkVertexEncoder baseEncoder = AurumChunkVertexType.createBaseEncoder();

    private final float[] x = new float[4];
    private final float[] y = new float[4];
    private final float[] z = new float[4];
    private final float[] u = new float[4];
    private final float[] v = new float[4];


    private int vertexCount;
    private int normal;

    @Override
    public long write(long ptr, Material material, Vertex vertex, int sectionIndex) {

        this.baseEncoder.write(ptr, material, vertex, sectionIndex);

        BlockContextHolder context = BlockContextHolder.get();

        LWJGL.memPutShort(ptr + LIGHT_OFFSET, (short) (vertex.light & 0xFF));
        LWJGL.memPutShort(ptr + LIGHT_OFFSET + 2, (short) ((vertex.light >> 16) & 0xFF));
        LWJGL.memPutInt(ptr + SECTION_OFFSET, packSectionOffset(sectionIndex));
        LWJGL.memPutShort(ptr + ENTITY_OFFSET, context.getBlockId());
        LWJGL.memPutShort(ptr + ENTITY_OFFSET + 2, context.getRenderType());
        LWJGL.memPutInt(ptr + MID_BLOCK_OFFSET, ExtendedDataHelper.computeMidBlock(vertex.x, vertex.y, vertex.z,
                context.getLocalPosX(), context.getLocalPosY(), context.getLocalPosZ(), context.getBlockEmission()));

        int index = this.vertexCount;
        this.x[index] = vertex.x;
        this.y[index] = vertex.y;
        this.z[index] = vertex.z;
        this.u[index] = vertex.u;
        this.v[index] = vertex.v;
        this.normal = vertex.trueNormal;
        this.vertexCount = index + 1;

        if (this.vertexCount == 4) {
            this.vertexCount = 0;
            this.finishQuad(ptr);
        }

        return ptr + STRIDE;
    }

    private void finishQuad(long ptr) {
        float midU = (this.u[0] + this.u[1] + this.u[2] + this.u[3]) * 0.25F;
        float midV = (this.v[0] + this.v[1] + this.v[2] + this.v[3]) * 0.25F;

        int packedNormal = this.normal;
        int tangent = NormalHelper.computeTangent(
                NormalHelper.getPackedNormalComponent(packedNormal, 0),
                NormalHelper.getPackedNormalComponent(packedNormal, 1),
                NormalHelper.getPackedNormalComponent(packedNormal, 2),
                this);

        for (int vertex = 0; vertex < 4; vertex++) {
            long base = ptr - (long) (3 - vertex) * STRIDE;

            LWJGL.memPutFloat(base + MID_TEX_OFFSET, midU);
            LWJGL.memPutFloat(base + MID_TEX_OFFSET + 4, midV);
            LWJGL.memPutInt(base + NORMAL_OFFSET, packedNormal);
            LWJGL.memPutInt(base + TANGENT_OFFSET, tangent);
        }

    }

    private static int packSectionOffset(int sectionIndex) {
        return (LocalSectionIndex.unpackX(sectionIndex) * 16)
                | ((LocalSectionIndex.unpackY(sectionIndex) * 16) << 8)
                | ((LocalSectionIndex.unpackZ(sectionIndex) * 16) << 16);
    }

    @Override
    public float x(int index) {
        return this.x[index];
    }

    @Override
    public float y(int index) {
        return this.y[index];
    }

    @Override
    public float z(int index) {
        return this.z[index];
    }

    @Override
    public float u(int index) {
        return this.u[index];
    }

    @Override
    public float v(int index) {
        return this.v[index];
    }
}
