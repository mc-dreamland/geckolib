package software.bernie.geckolib.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.*;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.renderer.base.GeoRenderer;
import software.bernie.geckolib.renderer.base.PerBoneRender;
import software.bernie.geckolib.util.RenderUtil;

import java.util.function.BiConsumer;

/**
 * Built-in GeoLayer for rendering a custom texture for a specific bone.
 * <p>
 * Due to the way Mojang handles {@link VertexConsumer buffers}, and for safety; this layer only supports one bone at a time.
 * Add multiple copies of this layer if your model has multiple bones you want to render with a custom texture
 */
public class CustomBoneTextureGeoLayer<T extends GeoAnimatable, O, R extends GeoRenderState> extends GeoRenderLayer<T, O, R> {
    protected final String boneName;
    protected final ResourceLocation texture;

    public CustomBoneTextureGeoLayer(GeoRenderer<T, O, R> renderer, String boneName, ResourceLocation texture) {
        super(renderer);

        this.boneName = boneName;
        this.texture = texture;
    }

    /**
     * Get the texture resource path for the given {@link GeoRenderState}.
     */
    @Override
    protected ResourceLocation getTextureResource(R renderState) {
        return this.texture;
    }

    /**
     * Get the render type for the render pass
     */
    protected RenderType getRenderType(R renderState, ResourceLocation texture) {
        return this.renderer.getRenderType(renderState, texture);
    }

    /**
     * Register per-bone render operations, to be rendered after the main model is done.
     * <p>
     * Even though the task is called after the main model renders, the {@link PoseStack} provided will be posed as if the bone
     * is currently rendering.
     *
     * @param consumer The registrar to accept the per-bone render tasks
     */
    @Override
    public void addPerBoneRender(R renderState, BakedGeoModel model, BiConsumer<GeoBone, PerBoneRender<R>> consumer) {
        model.getBone(this.boneName).ifPresent(bone -> consumer.accept(bone, this::renderBone));
    }

    /**
     * Render the bone with the replacement texture
     */
    protected void renderBone(R renderState, PoseStack poseStack, GeoBone bone, @Nullable RenderType renderType, MultiBufferSource bufferSource,
                              int packedLight, int packedOverlay, int renderColor) {
        if (renderType == null)
            return;

        ResourceLocation boneTexture = getTextureResource(renderState);
        ResourceLocation baseTexture = this.renderer.getTextureLocation(renderState);
        IntIntPair boneTextureSize = RenderUtil.getTextureDimensions(boneTexture);
        IntIntPair baseTextureSize = RenderUtil.getTextureDimensions(baseTexture);
        float widthRatio = baseTextureSize.firstInt() / (float)boneTextureSize.firstInt();
        float heightRatio = baseTextureSize.secondInt() / (float)boneTextureSize.secondInt();
        VertexConsumer buffer = bufferSource.getBuffer(getRenderType(renderState, boneTexture));
        bone.setHidden(false);
        bone.setChildrenHidden(true);

        for (GeoCube cube : bone.getCubes()) {
            if (!RenderUtil.hasCubeRotation(cube)) {
                renderCube(renderState, cube, poseStack, buffer, widthRatio, heightRatio, packedLight, packedOverlay, renderColor);

                continue;
            }

            poseStack.pushPose();
            renderCube(renderState, cube, poseStack, buffer, widthRatio, heightRatio, packedLight, packedOverlay, renderColor);
            poseStack.popPose();
        }

        bone.setHidden(false);
    }

    /**
     * Renders an individual {@link GeoCube}
     * <p>
     * This tends to be called recursively from something like {@link GeoRenderer#renderCubesOfBone}
     */
    @ApiStatus.Internal
    protected void renderCube(R renderState, GeoCube cube, PoseStack poseStack, VertexConsumer buffer, float widthRatio, float heightRatio,
                              int packedLight, int packedOverlay, int renderColor) {
        boolean hasRotation = RenderUtil.hasCubeRotation(cube);
        boolean isFlatCube = RenderUtil.isFlatCube(cube);

        if (hasRotation) {
            RenderUtil.translateToPivotPoint(poseStack, cube);
            RenderUtil.rotateMatrixAroundCube(poseStack, cube);
            RenderUtil.translateAwayFromPivotPoint(poseStack, cube);
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
        Vec3 cubeSize = cube.size();

        for (GeoQuad quad : cube.quads()) {
            if (quad == null)
                continue;

            Vector3f normal = quad.normal();
            float baseNormalX = normal.x();
            float baseNormalY = normal.y();
            float baseNormalZ = normal.z();
            float normalX = org.joml.Math.fma(normalM00, baseNormalX, org.joml.Math.fma(normalM10, baseNormalY, normalM20 * baseNormalZ));
            float normalY = org.joml.Math.fma(normalM01, baseNormalX, org.joml.Math.fma(normalM11, baseNormalY, normalM21 * baseNormalZ));
            float normalZ = org.joml.Math.fma(normalM02, baseNormalX, org.joml.Math.fma(normalM12, baseNormalY, normalM22 * baseNormalZ));

            if (isFlatCube) {
                if (normalX < 0 && (cubeSize.y() == 0 || cubeSize.z() == 0))
                    normalX = -normalX;

                if (normalY < 0 && (cubeSize.x() == 0 || cubeSize.z() == 0))
                    normalY = -normalY;

                if (normalZ < 0 && (cubeSize.x() == 0 || cubeSize.y() == 0))
                    normalZ = -normalZ;
            }

            createVerticesOfQuad(renderState, quad, poseState, normalX, normalY, normalZ, buffer, widthRatio, heightRatio, packedOverlay, packedLight, renderColor);
        }
    }

    /**
     * Applies the {@link GeoQuad Quad's} {@link GeoVertex vertices} to the given {@link VertexConsumer buffer} for rendering
     */
    @ApiStatus.Internal
    protected void createVerticesOfQuad(R renderState, GeoQuad quad, Matrix4f poseState, float normalX, float normalY, float normalZ, VertexConsumer buffer,
                                        float widthRatio, float heightRatio, int packedOverlay, int packedLight, int renderColor) {
        float poseM00 = poseState.m00();
        float poseM01 = poseState.m01();
        float poseM02 = poseState.m02();
        float poseM10 = poseState.m10();
        float poseM11 = poseState.m11();
        float poseM12 = poseState.m12();
        float poseM20 = poseState.m20();
        float poseM21 = poseState.m21();
        float poseM22 = poseState.m22();
        float poseM30 = poseState.m30();
        float poseM31 = poseState.m31();
        float poseM32 = poseState.m32();

        for (GeoVertex vertex : quad.vertices()) {
            Vector3f position = vertex.position();
            float positionX = position.x();
            float positionY = position.y();
            float positionZ = position.z();
            float transformedX = org.joml.Math.fma(poseM00, positionX, org.joml.Math.fma(poseM10, positionY, org.joml.Math.fma(poseM20, positionZ, poseM30)));
            float transformedY = org.joml.Math.fma(poseM01, positionX, org.joml.Math.fma(poseM11, positionY, org.joml.Math.fma(poseM21, positionZ, poseM31)));
            float transformedZ = org.joml.Math.fma(poseM02, positionX, org.joml.Math.fma(poseM12, positionY, org.joml.Math.fma(poseM22, positionZ, poseM32)));

            buffer.addVertex(transformedX, transformedY, transformedZ, renderColor, vertex.texU() * widthRatio, vertex.texV() * heightRatio,
                             packedOverlay, packedLight, normalX, normalY, normalZ);
        }
    }

    /**
     * This method is called by the {@link GeoRenderer} before rendering, immediately after {@link GeoRenderer#preRender} has been called
     * <p>
     * This allows for RenderLayers to perform pre-render manipulations such as hiding or showing bones.
     * <p>
     * <b><u>NOTE:</u></b> Changing VertexConsumers or RenderTypes must not be performed here<br>
     * <b><u>NOTE:</u></b> If the passed {@link VertexConsumer buffer} is null, then the animatable was not actually rendered (invisible, etc)
     * and you may need to factor this in to your design
     */
    @ApiStatus.Internal
    @Override
    public void preRender(R renderState, PoseStack poseStack, BakedGeoModel bakedModel, @Nullable RenderType renderType, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer,
                          int packedLight, int packedOverlay, int renderColor) {
        if (buffer == null)
            return;

        bakedModel.getBone(this.boneName).ifPresent(bone -> {
            bone.setHidden(true);
            bone.setChildrenHidden(false);
        });
    }
}
