package re.lilith.aurum.gbuffer.state;

public interface RenderTargetStateListener {
    RenderTargetStateListener NOP = _ -> {
    };

    void setIsMainBound(boolean bound);
}
