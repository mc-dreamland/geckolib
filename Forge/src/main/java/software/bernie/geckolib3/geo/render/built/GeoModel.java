package software.bernie.geckolib3.geo.render.built;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Direction;
import software.bernie.geckolib3.geo.raw.pojo.ModelProperties;

public class GeoModel {
	public List<GeoBone> topLevelBones = new ObjectArrayList<>();
	public ModelProperties properties;

	public Optional<GeoBone> getBone(String name) {
		for (GeoBone bone : topLevelBones) {
			GeoBone optionalBone = getBoneRecursively(name, bone);
			if (optionalBone != null) {
				return Optional.of(optionalBone);
			}
		}
		return Optional.empty();
	}

	private GeoBone getBoneRecursively(String name, GeoBone bone) {
		if (bone.name.equals(name)) {
			return bone;
		}
		for (GeoBone childBone : bone.childBones) {
			if (childBone.name.equals(name)) {
				return childBone;
			}
			GeoBone optionalBone = getBoneRecursively(name, childBone);
			if (optionalBone != null) {
				return optionalBone;
			}
		}
		return null;
	}

	public void optimizeCubes() {
		for (GeoBone bone : topLevelBones) {
			optimizeCubesRecursively(bone);
		}
	}

	private void optimizeCubesRecursively(GeoBone bone) {
		removeAdjacentFaces(bone.childCubes);
		for (GeoBone childBone : bone.childBones) {
			optimizeCubesRecursively(childBone);
		}
	}

	private void removeAdjacentFaces(List<GeoCube> cubes) {
		Set<GeoCube> optimizedCubes = new HashSet<>();
		for (GeoCube cube : cubes) {
			for (GeoCube otherCube : cubes) {
				if (cube != otherCube && !optimizedCubes.contains(otherCube)) {
					for (int i = 0; i < 6; i++) {
						GeoQuad face = cube.quads[i];
						Direction direction = Direction.values()[i];
						GeoQuad otherFace = otherCube.getFaceInDirection(direction.getOpposite());
						if (otherFace != null && areFacesAdjacent(face, otherFace)) {
							// Remove the adjacent faces
							cube.quads[i] = null;
							otherCube.quads[direction.getOpposite().ordinal()] = null;
						}
					}
				}
			}
			optimizedCubes.add(cube);
		}
	}

	private boolean areFacesAdjacent(GeoQuad face1, GeoQuad face2) {
		if (face1 == null || face2 == null) {
			return false;
		}

		// Check if all vertices are the same between the two faces
		for (int i = 0; i < 4; i++) {
			GeoVertex v1 = face1.vertices[i];
			GeoVertex v2 = face2.vertices[i];
			if (!v1.position.equals(v2.position)) {
				return false;
			}
		}
		return true;
	}
}
