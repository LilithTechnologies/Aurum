package re.lilith.aurum.vertices;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayDeque;
import java.util.Deque;

public class PoseStack {
    private final Deque<Pose> poseStack = new ArrayDeque<>();

    public PoseStack() {
        Matrix4f matrix4f = new Matrix4f().identity();
        Matrix3f matrix3f = new Matrix3f().identity();
        this.poseStack.add(new Pose(matrix4f, matrix3f));
    }

    public void mulPose(Quaternionf quaternion) {
        Pose pose = this.poseStack.getLast();
        pose.pose.rotate(quaternion);
        pose.normal.rotate(quaternion);
    }

    public Pose last() {
        return this.poseStack.getLast();
    }

    public record Pose(Matrix4f pose, Matrix3f normal) {
    }
}