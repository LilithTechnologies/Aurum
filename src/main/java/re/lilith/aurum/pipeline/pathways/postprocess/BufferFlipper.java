package re.lilith.aurum.pipeline.pathways.postprocess;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

public class BufferFlipper {
    private final IntSet flippedBuffers;

    public BufferFlipper() {
        this.flippedBuffers = new IntOpenHashSet();
    }

    public void flip(int target) {
        if (!flippedBuffers.remove(target)) {
            // If the target wasn't in the set, add it to the set.
            flippedBuffers.add(target);
        }
    }

    public ImmutableSet<Integer> snapshot() {
        return ImmutableSet.copyOf(flippedBuffers);
    }
}
