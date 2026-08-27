package re.lilith.aurum.shaderpack.option;

import re.lilith.aurum.shaderpack.include.AbsolutePackPath;

/**
 * Encapsulates a single location of an option.
 */
public record OptionLocation(AbsolutePackPath filePath, int lineIndex) {
    public AbsolutePackPath getFilePath() {
        return filePath;
    }

    /**
     * Gets the index of the line this option is on.
     * Note that this is the index - so the first line is
     * 0, the second is 1, etc.
     */
    public int getLineIndex() {
        return lineIndex;
    }
}
