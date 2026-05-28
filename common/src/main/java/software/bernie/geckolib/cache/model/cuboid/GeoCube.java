package software.bernie.geckolib.cache.model.cuboid;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;
import software.bernie.geckolib.cache.model.GeoBone;
import software.bernie.geckolib.cache.model.GeoQuad;

/**
 * Baked cuboid for a {@link GeoBone}
 *
 * @param quads The quad array for this cube, pre-sorted to render in correct order
 * @param pivot The pivot point of this cube
 * @param rotation The baked rotation value of this cube
 * @param size The x/y/z dimensions of this cube
 */
public record GeoCube(@Nullable GeoQuad[] quads, Vec3 pivot, Vec3 rotation, Vec3 size) {
    /**
     * Submit this cuboid's quads to the vertex consumer
     */
    public void render(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int renderColor) {
        if (hasRotation()) {
            translateToPivotPoint(poseStack);
            rotate(poseStack);
            translateAwayFromPivotPoint(poseStack);
        }

        PoseStack.Pose currentPose = poseStack.last();
        Matrix3f normalisedPoseState = currentPose.normal();
        Matrix4f poseState = currentPose.pose();
        float normalM00 = normalisedPoseState.m00();
        float normalM01 = normalisedPoseState.m01();
        float normalM02 = normalisedPoseState.m02();
        float normalM10 = normalisedPoseState.m10();
        float normalM11 = normalisedPoseState.m11();
        float normalM12 = normalisedPoseState.m12();
        float normalM20 = normalisedPoseState.m20();
        float normalM21 = normalisedPoseState.m21();
        float normalM22 = normalisedPoseState.m22();
        boolean flat = isFlat();

        for (GeoQuad quad : this.quads) {
            if (quad == null)
                continue;

            float baseNormalX = quad.normalX();
            float baseNormalY = quad.normalY();
            float baseNormalZ = quad.normalZ();
            float normalX = org.joml.Math.fma(normalM00, baseNormalX, org.joml.Math.fma(normalM10, baseNormalY, normalM20 * baseNormalZ));
            float normalY = org.joml.Math.fma(normalM01, baseNormalX, org.joml.Math.fma(normalM11, baseNormalY, normalM21 * baseNormalZ));
            float normalZ = org.joml.Math.fma(normalM02, baseNormalX, org.joml.Math.fma(normalM12, baseNormalY, normalM22 * baseNormalZ));

            if (flat) {
                if (normalX < 0 && (size().y() == 0 || size().z() == 0))
                    normalX = -normalX;

                if (normalY < 0 && (size().x() == 0 || size().z() == 0))
                    normalY = -normalY;

                if (normalZ < 0 && (size().x() == 0 || size().y() == 0))
                    normalZ = -normalZ;
            }

            quad.render(poseState, normalX, normalY, normalZ, vertexConsumer, packedLight, packedOverlay, renderColor);
        }

    }

    public boolean hasRotation() {
        return rotation().x() != 0 || rotation().y() != 0 || rotation().z() != 0;
    }

    public boolean isFlat() {
        return size().x() == 0 || size().y() == 0 || size().z() == 0;
    }

    /**
     * Apply a rotation to the provided PoseStack by this cube's rotation values
     */
    public void rotate(PoseStack poseStack) {
        final Vec3 rotation = rotation();

        poseStack.mulPose(new Quaternionf().rotationXYZ(0, 0, (float)rotation.z()));
        poseStack.mulPose(new Quaternionf().rotationXYZ(0, (float)rotation.y(), 0));
        poseStack.mulPose(new Quaternionf().rotationXYZ((float)rotation.x(), 0, 0));
    }

    /**
     * Apply a translation to the provided PoseStack to this cube's pivot point
     */
    public void translateToPivotPoint(PoseStack poseStack) {
        poseStack.translate(pivot().x() / 16f, pivot().y() / 16f, pivot().z() / 16f);
    }

    /**
     * Apply a translation to the provided PoseStack away from this cube's pivot point
     */
    public void translateAwayFromPivotPoint(PoseStack poseStack) {
        poseStack.translate(-pivot().x() / 16f, -pivot().y() / 16f, -pivot().z() / 16f);
    }
}
