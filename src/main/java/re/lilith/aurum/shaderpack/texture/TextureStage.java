package re.lilith.aurum.shaderpack.texture;

import java.util.Optional;

public enum TextureStage {
    /**
     * The shadowcomp passes.
     * <p>
     * While this is not documented in shaders.txt, it is a valid stage for defining custom textures.
     */
    SHADOWCOMP,
    /**
     * The prepare passes.
     * <p>
     * While this is not documented in shaders.txt, it is a valid stage for defining custom textures.
     */
    PREPARE,
    /**
     * All of the gbuffer passes, as well as the shadow passes.
     */
    GBUFFERS_AND_SHADOW,
    /**
     * The deferred pass.
     */
    DEFERRED,
    /**
     * The composite pass and final pass.
     */
    COMPOSITE_AND_FINAL;

    public static Optional<TextureStage> parse(String name) {
        return switch (name) {
            case "shadowcomp" -> Optional.of(SHADOWCOMP);
            case "prepare" -> Optional.of(PREPARE);
            case "gbuffers" -> Optional.of(GBUFFERS_AND_SHADOW);
            case "deferred" -> Optional.of(DEFERRED);
            case "composite" -> Optional.of(COMPOSITE_AND_FINAL);
            default -> Optional.empty();
        };
    }
}
