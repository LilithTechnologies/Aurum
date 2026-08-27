package re.lilith.aurum.shaderpack.program;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import org.jetbrains.annotations.Nullable;
import re.lilith.aurum.gl.blending.AlphaTestOverride;
import re.lilith.aurum.gl.blending.BlendModeOverride;
import re.lilith.aurum.gl.blending.BufferBlendInformation;
import re.lilith.aurum.shaderpack.ConstDirectiveParser;
import re.lilith.aurum.shaderpack.DispatchingDirectiveHolder;
import re.lilith.aurum.shaderpack.PackRenderTargetDirectives;
import re.lilith.aurum.shaderpack.ShaderProperties;
import re.lilith.aurum.shaderpack.comment.CommentDirective;
import re.lilith.aurum.shaderpack.comment.CommentDirectiveParser;

import java.util.*;

public class ProgramDirectives {
    private static final ImmutableList<String> LEGACY_RENDER_TARGETS = PackRenderTargetDirectives.LEGACY_RENDER_TARGETS;

    private final int[] drawBuffers;
    private final float viewportScale;
    @Nullable
    private final AlphaTestOverride alphaTestOverride;

    @Nullable
    private final BlendModeOverride blendModeOverride;
    private final List<BufferBlendInformation> bufferBlendInformations;
    private final ImmutableSet<Integer> mipmappedBuffers;
    private final ImmutableMap<Integer, Boolean> explicitFlips;

    private ProgramDirectives(int[] drawBuffers, float viewportScale, @Nullable AlphaTestOverride alphaTestOverride,
                              @Nullable BlendModeOverride blendModeOverride, List<BufferBlendInformation> bufferBlendInformations, ImmutableSet<Integer> mipmappedBuffers,
                              ImmutableMap<Integer, Boolean> explicitFlips) {
        this.drawBuffers = drawBuffers;
        this.viewportScale = viewportScale;
        this.alphaTestOverride = alphaTestOverride;
        this.blendModeOverride = blendModeOverride;
        this.bufferBlendInformations = bufferBlendInformations;
        this.mipmappedBuffers = mipmappedBuffers;
        this.explicitFlips = explicitFlips;
    }

    ProgramDirectives(ProgramSource source, ShaderProperties properties, Set<Integer> supportedRenderTargets,
                      @Nullable BlendModeOverride defaultBlendOverride) {
        // DRAWBUFFERS is only detected in the fragment shader source code (.fsh).
        // If there's no explicit declaration, then by default /* DRAWBUFFERS:0 */ is inferred.
        // For SEUS v08 and SEUS v10 to work, this will need to be set to 01234567. However, doing this causes
        // TAA to break on Sildur's Vibrant Shaders, since gbuffers_skybasic lacks a DRAWBUFFERS directive, causing
        // undefined data to be written to colortex7.
        //
        // TODO: Figure out how to infer the DRAWBUFFERS directive when it is missing.
        String fragmentSource = source.getFragmentSource().orElse(null);
        Optional<CommentDirective> optionalDrawbuffersDirective = findDrawbuffersDirective(fragmentSource);
        Optional<CommentDirective> optionalRendertargetsDirective = findRendertargetsDirective(fragmentSource);

        CommentDirective appliedDirective = getAppliedDirective(optionalDrawbuffersDirective.orElse(null), optionalRendertargetsDirective.orElse(null));
        if (appliedDirective == null) {
            drawBuffers = new int[]{0};
        } else if (appliedDirective.getType() == CommentDirective.Type.DRAWBUFFERS) {
            drawBuffers = parseDigits(appliedDirective.getDirective().toCharArray());
        } else if (appliedDirective.getType() == CommentDirective.Type.RENDERTARGETS) {
            drawBuffers = parseDigitList(appliedDirective.getDirective());
        } else {
            throw new IllegalStateException("Unhandled comment directive type!");
        }

        if (properties != null) {
            viewportScale = properties.getViewportScaleOverrides().getOrDefault(source.getName(), 1.0f);
            alphaTestOverride = properties.getAlphaTestOverrides().get(source.getName());

            BlendModeOverride blendModeOverride = properties.getBlendModeOverrides().get(source.getName());
            List<BufferBlendInformation> bufferBlendInformations = properties.getBufferBlendOverrides().get(source.getName());
            this.blendModeOverride = blendModeOverride != null ? blendModeOverride : defaultBlendOverride;
            this.bufferBlendInformations = bufferBlendInformations != null ? bufferBlendInformations : Collections.emptyList();

            explicitFlips = source.getParent().getPackDirectives().getExplicitFlips(source.getName());
        } else {
            viewportScale = 1.0f;
            alphaTestOverride = null;
            blendModeOverride = defaultBlendOverride;
            bufferBlendInformations = Collections.emptyList();
            explicitFlips = ImmutableMap.of();
        }

        HashSet<Integer> mipmappedBuffers = new HashSet<>();
        DispatchingDirectiveHolder directiveHolder = new DispatchingDirectiveHolder();

        supportedRenderTargets.forEach(index -> {
            BooleanConsumer mipmapHandler = shouldMipmap -> {
                if (shouldMipmap) {
                    mipmappedBuffers.add(index);
                } else {
                    mipmappedBuffers.remove(index);
                }
            };

            directiveHolder.acceptConstBooleanDirective("colortex" + index + "MipmapEnabled", mipmapHandler);

            if (index < LEGACY_RENDER_TARGETS.size()) {
                directiveHolder.acceptConstBooleanDirective(LEGACY_RENDER_TARGETS.get(index) + "MipmapEnabled", mipmapHandler);
            }
        });

        if (fragmentSource != null) {
            for (ConstDirectiveParser.ConstDirective directive : ConstDirectiveParser.findDirectives(fragmentSource)) {
                directiveHolder.processDirective(directive);
            }
        }

        this.mipmappedBuffers = ImmutableSet.copyOf(mipmappedBuffers);
    }

    public ProgramDirectives withOverriddenDrawBuffers(int[] drawBuffersOverride) {
        return new ProgramDirectives(drawBuffersOverride, viewportScale, alphaTestOverride, blendModeOverride, bufferBlendInformations,
                mipmappedBuffers, explicitFlips);
    }

    private static Optional<CommentDirective> findDrawbuffersDirective(@Nullable String stageSource) {
        if (stageSource == null) {
            return Optional.empty();
        }
        return CommentDirectiveParser.findDirective(stageSource, CommentDirective.Type.DRAWBUFFERS);
    }

    private static Optional<CommentDirective> findRendertargetsDirective(@Nullable String stageSource) {
        if (stageSource == null) {
            return Optional.empty();
        }
        return CommentDirectiveParser.findDirective(stageSource, CommentDirective.Type.RENDERTARGETS);
    }

    private static int[] parseDigits(char[] directiveChars) {
        int[] buffers = new int[directiveChars.length];
        int index = 0;

        for (char buffer : directiveChars) {
            buffers[index++] = Character.digit(buffer, 10);
        }

        return buffers;
    }

    private static int[] parseDigitList(String digitListString) {
        return Arrays.stream(digitListString.split(","))
                .mapToInt(Integer::parseInt)
                .toArray();
    }

    @Nullable
    private static CommentDirective getAppliedDirective(@Nullable CommentDirective drawbuffersDirective, @Nullable CommentDirective rendertargetsDirective) {
        if (drawbuffersDirective != null && rendertargetsDirective != null) {
            return drawbuffersDirective.getLocation() > rendertargetsDirective.getLocation() ? drawbuffersDirective : rendertargetsDirective;
        }

        return drawbuffersDirective != null ? drawbuffersDirective : rendertargetsDirective;
    }

    public int[] getDrawBuffers() {
        return drawBuffers;
    }

    public float getViewportScale() {
        return viewportScale;
    }

    public Optional<AlphaTestOverride> getAlphaTestOverride() {
        return Optional.ofNullable(alphaTestOverride);
    }

    public Optional<BlendModeOverride> getBlendModeOverride() {
        return Optional.ofNullable(blendModeOverride);
    }

    public List<BufferBlendInformation> getBufferBlendOverrides() {
        return bufferBlendInformations;
    }

    public ImmutableSet<Integer> getMipmappedBuffers() {
        return mipmappedBuffers;
    }

    public ImmutableMap<Integer, Boolean> getExplicitFlips() {
        return explicitFlips;
    }
}
