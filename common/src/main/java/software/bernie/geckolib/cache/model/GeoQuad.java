package software.bernie.geckolib.cache.model;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.loading.json.raw.FaceUV;

/**
 * Quad data holder
 *
 * @param vertices The vertex array for this quad
 * @param normalX The x-axis normal value for this quad
 * @param normalY The y-axis normal value for this quad
 * @param normalZ The z-axis normal value for this quad
 * @param direction The cube-face of this quad. This does not necessarily represent the actual direction the quad is facing
 */
public record GeoQuad(GeoVertex[] vertices, float normalX, float normalY, float normalZ, Direction direction) {
    public Vector3f normalVec() {
        return new Vector3f(this.normalX, this.normalY, this.normalZ);
    }

	public static GeoQuad build(GeoVertex[] vertices, double[] uvCoords, double[] uvSize, FaceUV.Rotation uvRotation, float texWidth, float texHeight, boolean mirror, Direction direction) {
		return build(vertices, (float)uvCoords[0], (float)uvCoords[1], (float)uvSize[0], (float)uvSize[1], uvRotation, texWidth, texHeight, mirror, direction);
	}

	public static GeoQuad build(GeoVertex[] vertices, float u, float v, float uSize, float vSize, FaceUV.Rotation uvRotation, float texWidth, float texHeight, boolean mirror, Direction direction) {
		float uWidth = (u + uSize) / texWidth;
		float vHeight = (v + vSize) / texHeight;
		u /= texWidth;
		v /= texHeight;
		Vector3f normal = direction.step();

		if (!mirror) {
			float tempWidth = uWidth;
			uWidth = u;
			u = tempWidth;
		}
		else {
			normal.mul(-1, 1, 1);
		}

		float[] uvs = uvRotation.rotateUvs(u, v, uWidth, vHeight);
		vertices[0] = vertices[0].withUVs(uvs[0], uvs[1]);
		vertices[1] = vertices[1].withUVs(uvs[2], uvs[3]);
		vertices[2] = vertices[2].withUVs(uvs[4], uvs[5]);
		vertices[3] = vertices[3].withUVs(uvs[6], uvs[7]);

		return new GeoQuad(vertices, normal.x, normal.y, normal.z, direction);
	}

    /**
     * Submit this texture quad to the vertex consumer
     */
    public void render(Matrix4f pose, float normalX, float normalY, float normalZ, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int renderColor) {
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

        for (GeoVertex vertex : vertices()) {
            float positionX = vertex.posX();
            float positionY = vertex.posY();
            float positionZ = vertex.posZ();
            float transformedX = org.joml.Math.fma(poseM00, positionX, org.joml.Math.fma(poseM10, positionY, org.joml.Math.fma(poseM20, positionZ, poseM30)));
            float transformedY = org.joml.Math.fma(poseM01, positionX, org.joml.Math.fma(poseM11, positionY, org.joml.Math.fma(poseM21, positionZ, poseM31)));
            float transformedZ = org.joml.Math.fma(poseM02, positionX, org.joml.Math.fma(poseM12, positionY, org.joml.Math.fma(poseM22, positionZ, poseM32)));

            vertexConsumer.addVertex(transformedX, transformedY, transformedZ, renderColor, vertex.texU(),
                                     vertex.texV(), packedOverlay, packedLight, normalX, normalY, normalZ);
        }
    }
}
