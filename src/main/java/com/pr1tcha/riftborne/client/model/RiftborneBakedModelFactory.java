package com.pr1tcha.riftborne.client.model;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.loading.json.raw.Cube;
import software.bernie.geckolib.loading.json.raw.ModelProperties;
import software.bernie.geckolib.loading.json.raw.PolyMesh;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.BoneStructure;
import software.bernie.geckolib.loading.object.GeometryTree;
import org.slf4j.Logger;

/**
 * Extends GeckoLib's built-in model baker with Bedrock {@code poly_mesh} support.
 *
 * <p>Cube geometry is delegated to GeckoLib unchanged. Explicit polygon arrays are converted
 * into synthetic {@link GeoCube}s so the regular GeckoLib bone animation and render pipeline
 * can process them.</p>
 */
public final class RiftborneBakedModelFactory implements BakedModelFactory {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final BakedModelFactory DELEGATE = BakedModelFactory.DEFAULT_FACTORY;

    @Override
    public BakedGeoModel constructGeoModel(GeometryTree geometryTree) {
        BakedGeoModel model = DELEGATE.constructGeoModel(geometryTree);
        for (BoneStructure structure : geometryTree.topLevelBones().values()) {
            GeoBone bone = model.getBone(structure.self().name())
                    .orElseThrow(() -> new IllegalStateException(
                            "GeckoLib baked model lost bone " + structure.self().name()
                    ));
            injectPolyMeshes(structure, bone, geometryTree.properties());
        }
        int[] counts = countGeometry(model.topLevelBones());
        LOGGER.info("Baked Riftborne GeckoLib model {}: {} bones, {} cubes, {} quads",
                geometryTree.properties().identifier(), counts[0], counts[1], counts[2]);
        return model;
    }

    @Override
    public GeoBone constructBone(BoneStructure structure, ModelProperties properties, GeoBone parent) {
        return DELEGATE.constructBone(structure, properties, parent);
    }

    @Override
    public GeoCube constructCube(Cube cube, ModelProperties properties, GeoBone bone) {
        return DELEGATE.constructCube(cube, properties, bone);
    }

    private static void injectPolyMeshes(BoneStructure structure, GeoBone bone, ModelProperties properties) {
        if (structure.self().polyMesh() != null) {
            bone.getCubes().add(constructPolyMesh(structure.self().polyMesh(), properties));
        }

        for (BoneStructure childStructure : structure.children().values()) {
            GeoBone childBone = bone.getChildBones().stream()
                    .filter(candidate -> candidate.getName().equals(childStructure.self().name()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "GeckoLib baked model lost child bone " + childStructure.self().name()
                    ));
            injectPolyMeshes(childStructure, childBone, properties);
        }
    }

    private static GeoCube constructPolyMesh(PolyMesh mesh, ModelProperties properties) {
        double[][][] polygons = mesh.polysUnion().union();
        if (polygons == null || polygons.length == 0) {
            throw new IllegalArgumentException(
                    "Riftborne poly_mesh requires explicit polygon arrays; shorthand TRI/QUAD is unsupported"
            );
        }

        List<GeoQuad> quads = new ArrayList<>(polygons.length);
        for (double[][] polygon : polygons) {
            if (polygon.length < 3 || polygon.length > 4) {
                throw new IllegalArgumentException(
                        "Riftborne poly_mesh supports triangles and quads, got " + polygon.length + " vertices"
                );
            }

            // GeckoLib's render buffer is QUADS. Preserve triangular mesh faces by
            // repeating their final vertex so every emitted face contributes exactly
            // four vertices and cannot corrupt the following faces in the buffer.
            GeoVertex[] vertices = new GeoVertex[4];
            for (int outputIndex = 0; outputIndex < 4; outputIndex++) {
                // Mirroring the X axis changes winding, so consume the polygon in reverse order.
                int polygonIndex = Math.min(outputIndex, polygon.length - 1);
                double[] indices = polygon[polygon.length - 1 - polygonIndex];
                int positionIndex = checkedIndex(indices, 0, mesh.positions().length / 3, "position");
                int uvIndex = checkedIndex(indices, 2, mesh.uvs().length / 2, "uv");

                int positionOffset = positionIndex * 3;
                int uvOffset = uvIndex * 2;
                float u = (float) mesh.uvs()[uvOffset];
                float v = (float) mesh.uvs()[uvOffset + 1];
                if (!Boolean.TRUE.equals(mesh.normalizedUVs())) {
                    u /= (float) properties.textureWidth();
                    v /= (float) properties.textureHeight();
                }

                vertices[outputIndex] = new GeoVertex(
                        new Vector3f(
                                (float) (-mesh.positions()[positionOffset] / 16.0),
                                (float) (mesh.positions()[positionOffset + 1] / 16.0),
                                (float) (mesh.positions()[positionOffset + 2] / 16.0)
                        ),
                        u,
                        v
                );
            }

            Vector3f normal = faceNormal(vertices);
            quads.add(new GeoQuad(vertices, normal, Direction.getNearest(normal.x, normal.y, normal.z)));
        }

        return new GeoCube(
                quads.toArray(GeoQuad[]::new),
                net.minecraft.world.phys.Vec3.ZERO,
                net.minecraft.world.phys.Vec3.ZERO,
                net.minecraft.world.phys.Vec3.ZERO,
                0,
                false
        );
    }

    private static int checkedIndex(double[] indices, int component, int count, String label) {
        if (indices.length <= component) {
            throw new IllegalArgumentException("poly_mesh vertex is missing its " + label + " index");
        }
        int index = (int) indices[component];
        if (index < 0 || index >= count) {
            throw new IllegalArgumentException("poly_mesh " + label + " index " + index + " is out of bounds");
        }
        return index;
    }

    private static Vector3f faceNormal(GeoVertex[] vertices) {
        Vector3f edgeA = new Vector3f(vertices[1].position()).sub(vertices[0].position());
        Vector3f edgeB = new Vector3f(vertices[2].position()).sub(vertices[0].position());
        Vector3f normal = edgeA.cross(edgeB);
        if (normal.lengthSquared() < 1.0E-8F) {
            return new Vector3f(0, 1, 0);
        }
        return normal.normalize();
    }

    private static int[] countGeometry(List<GeoBone> bones) {
        int[] counts = new int[3];
        for (GeoBone bone : bones) {
            counts[0]++;
            counts[1] += bone.getCubes().size();
            for (GeoCube cube : bone.getCubes()) {
                counts[2] += cube.quads().length;
            }
            int[] children = countGeometry(bone.getChildBones());
            counts[0] += children[0];
            counts[1] += children[1];
            counts[2] += children[2];
        }
        return counts;
    }
}
