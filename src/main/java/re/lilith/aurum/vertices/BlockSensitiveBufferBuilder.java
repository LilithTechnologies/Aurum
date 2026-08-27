package re.lilith.aurum.vertices;

public interface BlockSensitiveBufferBuilder {
    void aurum$beginBlock(short block, short renderType, byte blockEmission, int localPosX, int localPosY, int localPosZ);

    void aurum$endBlock();
}
