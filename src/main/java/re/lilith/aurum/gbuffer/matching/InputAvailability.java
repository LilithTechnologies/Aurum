package re.lilith.aurum.gbuffer.matching;

public record InputAvailability(boolean texture, boolean lightmap, boolean overlay) {
    public static final int NUM_VALUES = 8;

    public static InputAvailability unpack(int packed) {
        return new InputAvailability((packed & 1) == 1, (packed & 2) == 2, (packed & 4) == 4);
    }

    public int pack() {
        int packed = 0;

        if (overlay) {
            packed |= 4;
        }

        if (lightmap) {
            packed |= 2;
        }

        if (texture) {
            packed |= 1;
        }

        return packed;
    }
}