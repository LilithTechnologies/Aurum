package re.lilith.aurum.celeritas.terrain;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import re.lilith.aurum.vertices.ExtendedDataHelper;

/**
 * Holds the block currently being meshed. The chunk build runs on many threads, so each thread keeps its own context.
 * {@link AurumChunkVertexEncoder} reads the context to fill the {@code mc_Entity} and {@code at_midBlock} attributes.
 */
public final class BlockContextHolder {
    private static final ThreadLocal<BlockContextHolder> CONTEXT = ThreadLocal.withInitial(BlockContextHolder::new);

    private short blockId = -1;
    private short renderType = ExtendedDataHelper.BLOCK_RENDER_TYPE;
    private byte blockEmission;
    private int localPosX;
    private int localPosY;
    private int localPosZ;

    private boolean doubleSidedQuad;

    private final IntArrayList recordedQuads = new IntArrayList();

    private int replayIndex;

    public static BlockContextHolder get() {
        return CONTEXT.get();
    }

    public void recordQuad() {
        this.recordedQuads.add((this.blockId & 0xFFFF) | (this.renderType << 16));
        this.recordedQuads.add(this.localPosX | (this.localPosY << 8) | (this.localPosZ << 16) | (this.blockEmission << 24));
    }

    /**
     * Restores the context of the next recorded quad, in the order the quads were meshed.
     */
    public void replayNextQuad() {
        if (this.replayIndex + 1 >= this.recordedQuads.size()) {
            return;
        }

        int ids = this.recordedQuads.getInt(this.replayIndex++);
        int position = this.recordedQuads.getInt(this.replayIndex++);

        this.blockId = (short) ids;
        this.renderType = (short) (ids >> 16);
        this.localPosX = position & 0xFF;
        this.localPosY = (position >> 8) & 0xFF;
        this.localPosZ = (position >> 16) & 0xFF;
        this.blockEmission = (byte) (position >>> 24);
    }

    public void clearRecordedQuads() {
        this.recordedQuads.clear();
        this.replayIndex = 0;
    }

    public void setBlock(short blockId, short renderType, byte blockEmission, int localPosX, int localPosY, int localPosZ) {
        this.blockId = blockId;
        this.renderType = renderType;
        this.blockEmission = blockEmission;
        this.localPosX = localPosX;
        this.localPosY = localPosY;
        this.localPosZ = localPosZ;
    }

    public void reset() {
        this.blockId = -1;
        this.renderType = ExtendedDataHelper.BLOCK_RENDER_TYPE;
        this.blockEmission = 0;
        this.localPosX = 0;
        this.localPosY = 0;
        this.localPosZ = 0;
        this.doubleSidedQuad = false;
    }

    public void setDoubleSidedQuad(boolean doubleSidedQuad) {
        this.doubleSidedQuad = doubleSidedQuad;
    }

    public boolean isDoubleSidedQuad() {
        return this.doubleSidedQuad;
    }

    public short getBlockId() {
        return this.blockId;
    }

    public short getRenderType() {
        return this.renderType;
    }

    public byte getBlockEmission() {
        return this.blockEmission;
    }

    public int getLocalPosX() {
        return this.localPosX;
    }

    public int getLocalPosY() {
        return this.localPosY;
    }

    public int getLocalPosZ() {
        return this.localPosZ;
    }
}
