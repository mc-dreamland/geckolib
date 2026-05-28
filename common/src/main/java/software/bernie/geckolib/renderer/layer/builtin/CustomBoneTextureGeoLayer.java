package software.bernie.geckolib.renderer.layer.builtin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import software.bernie.geckolib.GeckoLibConstants;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.model.GeoBone;
import software.bernie.geckolib.cache.model.GeoQuad;
import software.bernie.geckolib.cache.model.GeoVertex;
import software.bernie.geckolib.cache.model.cuboid.CuboidGeoBone;
import software.bernie.geckolib.cache.model.cuboid.GeoCube;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.renderer.base.GeoRenderer;
import software.bernie.geckolib.renderer.base.PerBoneRender;
import software.bernie.geckolib.renderer.base.RenderPassInfo;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtil;

import java.util.function.BiConsumer;

/**
 * Built-in GeoLayer for rendering a custom texture for a specific bone.
 * <p>
 * Due to the way Mojang handles {@link VertexConsumer buffers}, and for safety; this layer only supports one bone at a time.
 * Add multiple copies of this layer if your model has multiple bones you want to render with a custom texture
 *
 * @param <T> Animatable class type. Inherited from the renderer this layer is attached to
 * @param <O> Associated object class type, or {@link Void} if none. Inherited from the renderer this layer is attached to
 * @param <R> RenderState class type. Inherited from the renderer this layer is attached to
 */
public class CustomBoneTextureGeoLayer<T extends GeoAnimatable, O, R extends GeoRenderState> extends GeoRenderLayer<T, O, R> {
    protected final String boneName;
    protected final Identifier texture;

    public CustomBoneTextureGeoLayer(GeoRenderer<T, O, R> renderer, String boneName, Identifier texture) {
        super(renderer);

        this.boneName = boneName;
        this.texture = texture;
    }

    /**
     * Get the texture resource path for the given {@link GeoRenderState}.
     */
    @Override
    protected Identifier getTextureResource(R renderState) {
        return this.texture;
    }

    /**
     * Get the render type for the render pass
     */
    protected @Nullable RenderType getRenderType(R renderState, Identifier texture) {
        return this.renderer.getRenderType(renderState, texture);
    }

    /**
     * This method is called by the {@link GeoRenderer} before rendering, immediately after {@link GeoRenderer#preRenderPass} has been called
     * <p>
     * This allows for RenderLayers to perform pre-render manipulations such as hiding or showing bones.
     * <p>
     * <b><u>NOTE:</u></b> Changing VertexConsumers or RenderTypes must not be performed here
     */
    @ApiStatus.Internal
    @Override
    public void preRender(RenderPassInfo<R> renderPassInfo, SubmitNodeCollector renderTasks) {
        if (renderPassInfo.willRender()) {
            renderPassInfo.addBoneUpdater((renderPassInfo1, snapshots) -> snapshots.get(CustomBoneTextureGeoLayer.this.boneName)
                    .ifPresent(bone -> {
                        bone.skipRender(true);
                        bone.skipChildrenRender(false);
                    }));
        }
    }

    /**
     * Register per-bone render operations, to be rendered after the main model is done.
     * <p>
     * Even though the task is called after the main model renders, the {@link PoseStack} provided will be posed as if the bone
     * is currently rendering.
     *
     * @param renderPassInfo The collated render-related data for this render pass
     * @param consumer The registrar to accept the per-bone render tasks
     */
    @Override
    public void addPerBoneRender(RenderPassInfo<R> renderPassInfo, BiConsumer<GeoBone, PerBoneRender<R>> consumer) {
        if (renderPassInfo.willRender()) {
            renderPassInfo.model().getBone(this.boneName).filter(CuboidGeoBone.class::isInstance)
                    .ifPresentOrElse(bone -> consumer.accept(bone, this::renderBone),
                                     () -> GeckoLibConstants.LOGGER.error("Unable to find bone for CustomBoneTextureGeoLayer: {}, skipping", this.boneName));
        }
    }

    /**
     * Render the bone with the replacement texture
     */
    protected void renderBone(RenderPassInfo<R> renderPassInfo, GeoBone bone, SubmitNodeCollector renderTasks) {
        R renderState = renderPassInfo.renderState();
        Identifier boneTexture = getTextureResource(renderState);
        Identifier baseTexture = this.renderer.getTextureLocation(renderState);
        IntIntPair boneTextureSize = RenderUtil.getTextureDimensions(boneTexture);
        IntIntPair baseTextureSize = RenderUtil.getTextureDimensions(baseTexture);
        float widthRatio = baseTextureSize.firstInt() / (float)boneTextureSize.firstInt();
        float heightRatio = baseTextureSize.secondInt() / (float)boneTextureSize.secondInt();
        int packedLight = renderPassInfo.packedLight();
        int packedOverlay = renderPassInfo.packedOverlay();
        int renderColor = renderPassInfo.renderColor();
        RenderType renderType = getRenderType(renderState, boneTexture);

        if (renderType != null) {
            renderTasks.submitCustomGeometry(renderPassInfo.poseStack(), renderType, (pose, buffer) -> {
                PoseStack poseStack = renderPassInfo.poseStack();

                poseStack.pushPose();
                poseStack.last().set(pose);
                bone.translateAwayFromPivotPoint(poseStack);

                for (GeoCube cube : ((CuboidGeoBone)bone).cubes) {
                    if (!cube.hasRotation()) {
                        renderCube(cube, poseStack, buffer, packedLight, packedOverlay, renderColor, widthRatio, heightRatio);

                        continue;
                    }

                    poseStack.pushPose();
                    renderCube(cube, poseStack, buffer, packedLight, packedOverlay, renderColor, widthRatio, heightRatio);
                    poseStack.popPose();
                }

                poseStack.popPose();
            });
        }
    }

    /**
     * Submit a cube's modified quads to the vertex consumer
     * @see GeoCube#render(PoseStack, VertexConsumer, int, int, int)
     */
    @ApiStatus.Internal
    public void renderCube(GeoCube cube, PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int renderColor, float widthRatio, float heightRatio) {
        if (cube.hasRotation()) {
            cube.translateToPivotPoint(poseStack);
            cube.rotate(poseStack);
            cube.translateAwayFromPivotPoint(poseStack);
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
        boolean flat = cube.isFlat();

        for (GeoQuad quad : cube.quads()) {
            if (quad == null)
                continue;

            float baseNormalX = quad.normalX();
            float baseNormalY = quad.normalY();
            float baseNormalZ = quad.normalZ();
            float normalX = org.joml.Math.fma(normalM00, baseNormalX, org.joml.Math.fma(normalM10, baseNormalY, normalM20 * baseNormalZ));
            float normalY = org.joml.Math.fma(normalM01, baseNormalX, org.joml.Math.fma(normalM11, baseNormalY, normalM21 * baseNormalZ));
            float normalZ = org.joml.Math.fma(normalM02, baseNormalX, org.joml.Math.fma(normalM12, baseNormalY, normalM22 * baseNormalZ));

            if (flat) {
                if (normalX < 0 && (cube.size().y() == 0 || cube.size().z() == 0))
                    normalX = -normalX;

                if (normalY < 0 && (cube.size().x() == 0 || cube.size().z() == 0))
                    normalY = -normalY;

                if (normalZ < 0 && (cube.size().x() == 0 || cube.size().y() == 0))
                    normalZ = -normalZ;
            }

            renderQuad(quad, poseState, normalX, normalY, normalZ, vertexConsumer, packedLight, packedOverlay, renderColor, widthRatio, heightRatio);
        }
    }

    /**
     * Submit a modified texture quad to the vertex consumer
     * @see GeoQuad
     */
    @ApiStatus.Internal
    protected void renderQuad(GeoQuad quad, Matrix4f pose, float normalX, float normalY, float normalZ, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int renderColor, float widthRatio, float heightRatio) {
        float poseM00 = pose.m00();
        float poseM01 = pose.m01();
        float poseM02 = pose.m02();
        float poseM10 = pose.m10();
        float poseM11 = pose.m11();
        float poseM12 = pose.m12();
        float poseM20 = pose.m20();
        float poseM21 = pose.m21();
        float poseM22 = pose.m22();
        float poseM30 = pose.m30();
        float poseM31 = pose.m31();
        float poseM32 = pose.m32();

        for (GeoVertex vertex : quad.vertices()) {
            float positionX = vertex.posX();
            float positionY = vertex.posY();
            float positionZ = vertex.posZ();
            float transformedX = org.joml.Math.fma(poseM00, positionX, org.joml.Math.fma(poseM10, positionY, org.joml.Math.fma(poseM20, positionZ, poseM30)));
            float transformedY = org.joml.Math.fma(poseM01, positionX, org.joml.Math.fma(poseM11, positionY, org.joml.Math.fma(poseM21, positionZ, poseM31)));
            float transformedZ = org.joml.Math.fma(poseM02, positionX, org.joml.Math.fma(poseM12, positionY, org.joml.Math.fma(poseM22, positionZ, poseM32)));

            vertexConsumer.addVertex(transformedX, transformedY, transformedZ, renderColor, vertex.texU() * widthRatio,
                                     vertex.texV() * heightRatio, packedOverlay, packedLight, normalX, normalY, normalZ);
        }
    }
}
