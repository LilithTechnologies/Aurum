package re.lilith.aurum.vertices.view;

import java.nio.ByteBuffer;

public class BufferBuilderPolygonView implements QuadView {
    private ByteBuffer buffer;
    private int writePointer;
    private int stride = 48;
    private int vertexAmount;

    private int uvOffset = 16;

    public void setup(ByteBuffer buffer, int writePointer, int stride, int vertexAmount, int uvOffset) {
        this.buffer = buffer;
        this.writePointer = writePointer;
        this.stride = stride;
        this.vertexAmount = vertexAmount;
        this.uvOffset = uvOffset;
    }

    @Override
    public float x(int index) {
        return buffer.getFloat(writePointer - stride * (vertexAmount - index));
    }

    @Override
    public float y(int index) {
        return buffer.getFloat(writePointer + 4 - stride * (vertexAmount - index));
    }

    @Override
    public float z(int index) {
        return buffer.getFloat(writePointer + 8 - stride * (vertexAmount - index));
    }

    @Override
    public float u(int index) {
        return buffer.getFloat(writePointer + uvOffset - stride * (vertexAmount - index));
    }

    @Override
    public float v(int index) {
        return buffer.getFloat(writePointer + uvOffset + 4 - stride * (vertexAmount - index));
    }
}
