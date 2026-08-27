package re.lilith.aurum.shaderpack.comment;

public record CommentDirective(Type type, String directive, int location) {
    public enum Type {
        DRAWBUFFERS,
        RENDERTARGETS
    }

    public Type getType() {
        return type;
    }

    /**
     * @return The directive without {@literal /}* or *{@literal /}
     */
    public String getDirective() {
        return directive;
    }

    /**
     * @return The starting position of the directive in a multi-line string. <br>
     * This is necessary to check if either the drawbuffer or the rendertarget directive should be applied
     * when there are multiple in the same shader file, based on which one is defined last.
     */
    public int getLocation() {
        return location;
    }
}
