package re.lilith.aurum.celeritas.terrain;

import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeFormat;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexFormat;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkMeshFormats;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;

public class AurumChunkVertexType implements ChunkVertexType {
    public static final AurumChunkVertexType INSTANCE = new AurumChunkVertexType();

    private static final ChunkVertexType BASE_TYPE = ChunkMeshFormats.VANILLA_LIKE;

    public static final int STRIDE = BASE_TYPE.getVertexFormat().getStride() + 32;

    public static final GlVertexFormat VERTEX_FORMAT = GlVertexFormat.builder(STRIDE)
            .addAllElements(BASE_TYPE.getVertexFormat())
            .addElement("aurum_LightCoord", GlVertexFormat.NEXT_ALIGNED_POINTER, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, false, false)
            .addElement("aurum_SectionOffset", GlVertexFormat.NEXT_ALIGNED_POINTER, GlVertexAttributeFormat.UNSIGNED_BYTE, 4, false, false)
            .addElement("aurum_Normal", GlVertexFormat.NEXT_ALIGNED_POINTER, GlVertexAttributeFormat.BYTE, 4, true, false)
            .addElement("mc_midTexCoord", GlVertexFormat.NEXT_ALIGNED_POINTER, GlVertexAttributeFormat.FLOAT, 2, false, false)
            .addElement("at_tangent", GlVertexFormat.NEXT_ALIGNED_POINTER, GlVertexAttributeFormat.BYTE, 4, true, false)
            .addElement("mc_Entity", GlVertexFormat.NEXT_ALIGNED_POINTER, GlVertexAttributeFormat.SHORT, 2, false, false)
            .addElement("at_midBlock", GlVertexFormat.NEXT_ALIGNED_POINTER, GlVertexAttributeFormat.BYTE, 4, false, false)
            .build();

    static ChunkVertexEncoder createBaseEncoder() {
        return BASE_TYPE.createEncoder();
    }

    @Override
    public float getPositionScale() {
        return BASE_TYPE.getPositionScale();
    }

    @Override
    public float getPositionOffset() {
        return BASE_TYPE.getPositionOffset();
    }

    @Override
    public float getTextureScale() {
        return BASE_TYPE.getTextureScale();
    }

    @Override
    public GlVertexFormat getVertexFormat() {
        return VERTEX_FORMAT;
    }

    @Override
    public ChunkVertexEncoder createEncoder() {
        return new AurumChunkVertexEncoder();
    }
}
