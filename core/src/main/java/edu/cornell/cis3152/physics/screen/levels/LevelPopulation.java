package edu.cornell.cis3152.physics.screen.levels;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.PulleyJointDef;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonWriter;
import edu.cornell.cis3152.physics.screen.WorldState;
import edu.cornell.cis3152.physics.world.*;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Builds all physics objects, terrain, and tiles for a given level number from JSON data.
 * <p>
 * Receives a {@code textureResolver} and {@code spriteAdder} callback so it can load textures
 * and add sprites to the scene without depending on the scene directly.
 */
class LevelPopulation {

    private static final int TILE_PX = 16;
    private static final int GOO_FRAME_COUNT = 3;
    /** Default if {@code constants.goo.surface_line_from_bottom} is absent (5px / 16px tile). */
    private static final float GOO_SURFACE_LINE_FROM_BOTTOM_DEFAULT = 5f / 16f;
    private static final float OBJECT_SIZE = 1.0f;
    private static final float FLOOR_TILE_SCALE = 2.0f;
    private static final float TILE_WORLD_SIZE = 1.0f;
    private static final float PULLEY_ROPE_OVERLAP_SCALE = 1.25f;

    /**
     * Holds references to the populated level's key objects (avatar, goal door, object lists, tile data).
     */
    static class Result {
        Door goalDoor;
        Zuko avatar;
        /** Extra Zuko-sprite objects placed via the editor's zukosprite tool. */
        List<Zuko> extraZukos = new ArrayList<>();
        List<GameObject> honeys = new ArrayList<>();
        List<GameObject> ices = new ArrayList<>();
        List<GameObject> clouds = new ArrayList<>();

        List<TextureRegion> tileRegions    = new ArrayList<>();
        /** [x, y] screen-space position in pixels for each tile, in the same order as tileRegions. */
        List<float[]>       tilePositions  = new ArrayList<>();
        List<BoxSprite> pulleyCarries = new ArrayList<>();
        List<BoxSprite> pulleyRopes = new ArrayList<>();
        List<Vector2> pulleyWheelCenters = new ArrayList<>();
        List<Float> pulleyWheelRadii = new ArrayList<>();
        List<Vector2> pulleyGroundAnchors = new ArrayList<>();
        List<Vector2> pulleyCarryAnchorOffsets = new ArrayList<>();
        /** Decorative goo sprites (fatal hazard art); animated in {@link LevelBaseScene}. */
        List<BoxSprite> gooDecors = new ArrayList<>();
        /** One texture region per animation frame ({@code shared/goo_0.png}, …). */
        TextureRegion[] gooFrames;
    }

    float TERRAIN_BLOCK_SIZE = 1.5f;

    private final JsonValue constants;
    private final BiFunction<String, String, Texture> textureResolver;
    private final Consumer<ObstacleSprite> spriteAdder;

    LevelPopulation(JsonValue constants,
                    BiFunction<String, String, Texture> textureResolver,
                    Consumer<ObstacleSprite> spriteAdder) {
        this.constants = constants;
        this.textureResolver = textureResolver;
        this.spriteAdder = spriteAdder;
    }

    /**
     * Entry point — reads {@code levelN} JSON, creates goal, walls, platforms, floors, tilemap,
     * Zuko, extra zukosprites, honey/ice/cloud objects, and tile colliders. Returns a {@link Result}.
     */
    Result populate(int currentLevel, float units, WorldState worldState, World world) {
        Result result = new Result();

        JsonValue level = constants.get("level" + currentLevel);
        JsonValue objectLocations = level.get("objectLocations");
        JsonValue levelPlayerSettings = level.get("playerSettings");

        Texture texture = textureResolver.apply("shared-goal", "shared/goaldoor.png");
        JsonValue goal = objectLocations.get("goal");
        result.goalDoor = new Door(units, goal);
        result.goalDoor.setTexture(texture);
        result.goalDoor.getObstacle().setName("goal");
        spriteAdder.accept(result.goalDoor);

        Texture tilesetTexture = textureResolver.apply("platform-tileset", "platform/tileset.png");
        JsonValue tilemap = level.get("tilemap");
        if (tilemap != null) {
            for (int ii = 0; ii < tilemap.size; ii++) {
                JsonValue entry = tilemap.get(ii);
                int tx  = entry.getInt("tx");
                int ty  = entry.getInt("ty");
                int col = entry.getInt("col");
                int row = entry.getInt("row");

                // 16x16 from tileset
                TextureRegion region = new TextureRegion(
                        tilesetTexture,
                        col * TILE_PX, row * TILE_PX,
                        TILE_PX, TILE_PX
                );
                result.tileRegions.add(region);

                result.tilePositions.add(new float[]{ tx * units, ty * units });
            }
        }

        Texture borderTexture = textureResolver.apply("shared-wall", "shared/treetile.png");
        JsonValue walls = level.get("walls");
        JsonValue wallPositions = walls.get("positions");
        for (int ii = 0; ii < wallPositions.size; ii++) {
            Surface wall = new Surface(wallPositions.get(ii).asFloatArray(), units, walls);
            wall.getObstacle().setName("wall" + ii);
            wall.setTexture(borderTexture);
            spriteAdder.accept(wall);
        }

        JsonValue platforms = level.get("platforms");
        JsonValue platformPositions = platforms.get("positions");
        for (int ii = 0; ii < platformPositions.size; ii++) {
            Surface platform = new Surface(platformPositions.get(ii).asFloatArray(), units, platforms);
            platform.getObstacle().setName("platform" + ii);
            platform.setTexture(borderTexture);
            spriteAdder.accept(platform);
        }

        Texture floorTexture = textureResolver.apply("shared-floor", "shared/floortile.png");
        JsonValue floors = level.get("floors");
        if (floors != null) {
            JsonValue renderedFloors = buildScaledTileSettings(floors, FLOOR_TILE_SCALE);
            JsonValue floorPositions = floors.get("positions");
            if (floorPositions != null) {
                for (int ii = 0; ii < floorPositions.size; ii++) {
                    Surface floor = new Surface(floorPositions.get(ii).asFloatArray(), units, renderedFloors);
                    floor.getObstacle().setName("floor" + ii);
                    floor.setTexture(floorTexture);
                    spriteAdder.accept(floor);
                }
            }
        }
        addTilemapColliders(level, floors, units);

        Texture zukoTexture  = textureResolver.apply("platform-traci",  "platform/traci.png");
        Texture walkSheet    = textureResolver.apply("platform-walk",   "platform/zukowalk.png");
        Texture photoSheet   = textureResolver.apply("platform-camera", "platform/cameraflash.png");
        Texture jumpSheet    = textureResolver.apply("platform-jump",   "platform/zukojump.png");
        Texture tongueTexture =  textureResolver.apply("platform-tongue",   "platform/zukotonguechunk.png");


        JsonValue posJson = level.get("objectLocations").get("zukoPos");
        result.avatar = buildZuko(units, constants.get("zuko"), posJson.get("pos").getFloat(0), posJson.get("pos").getFloat(1),
                zukoTexture, walkSheet, photoSheet, jumpSheet, tongueTexture, "avatar",levelPlayerSettings);
        spriteAdder.accept(result.avatar);
        result.avatar.createSensor();

        JsonValue zukoSprites = objectLocations.get("zukosprite");
        if (zukoSprites != null) {
            for (int ii = 0; ii < zukoSprites.size; ii++) {
                float[] pos = zukoSprites.get(ii).asFloatArray();

                JsonValue syntheticZuko = buildSyntheticZukoJson(
                        level.get("objectLocations").get("zukoPos"), pos[0], pos[1]);

                Zuko extra = buildZuko(units, syntheticZuko,pos[0], pos[1],
                        zukoTexture, walkSheet, photoSheet, jumpSheet, tongueTexture,
                        "zukosprite" + ii, levelPlayerSettings);
                spriteAdder.accept(extra);
                extra.createSensor();
                result.extraZukos.add(extra);
            }
        }

        float objectWidth = OBJECT_SIZE;

        Texture honeyTexture = textureResolver.apply("platform-honey", "platform/honey.png");
        float honeyHeight = objectWidth * ((float) honeyTexture.getHeight() / honeyTexture.getWidth());
        JsonValue honeyPositions = objectLocations.get("honey");
        for (int ii = 0; ii < honeyPositions.size; ii++) {
            float[] pos = honeyPositions.get(ii).asFloatArray();
            GameObject honey = new GameObject(
                    Obj.HONEY, constants.get("honey"), units,
                    pos[0], pos[1],
                    objectWidth, honeyHeight,
                    BodyDef.BodyType.DynamicBody,
                    false
            );
            honey.setTexture(honeyTexture);
            spriteAdder.accept(honey);
            result.honeys.add(honey);
        }

        Texture iceTexture = textureResolver.apply("platform-ice", "platform/ice.png");
        float iceHeight = objectWidth * ((float) iceTexture.getHeight() / iceTexture.getWidth());
        JsonValue icePositions = objectLocations.get("ice");
        for (int ii = 0; ii < icePositions.size; ii++) {
            float[] pos = icePositions.get(ii).asFloatArray();
            GameObject ice = new GameObject(
                    Obj.ICE, constants.get("ice"), units,
                    pos[0], pos[1],
                    objectWidth, iceHeight,
                    BodyDef.BodyType.DynamicBody,
                    false
            );
            ice.setTexture(iceTexture);
            spriteAdder.accept(ice);
            result.ices.add(ice);
        }

        float cloudSize = OBJECT_SIZE;
        Texture cloudTexture = textureResolver.apply("platform-cloud", "platform/cloud.png");
        JsonValue cloudPositions = objectLocations.get("cloud");
        for (int ii = 0; ii < cloudPositions.size; ii++) {
            float[] pos = cloudPositions.get(ii).asFloatArray();
            GameObject cloud = new GameObject(
                    Obj.CLOUD, constants.get("cloud"), units,
                    pos[0], pos[1],
                    cloudSize, cloudSize,
                    BodyDef.BodyType.DynamicBody,
                    false
            );
            cloud.setTexture(cloudTexture);
            spriteAdder.accept(cloud);
            result.clouds.add(cloud);
        }

        addPulleyAssembly(result, objectLocations, units, world);

        addGooDecorations(result, level, units);

        return result;
    }

    /**
     * Fatal goo uses invisible {@link Surface} colliders; art is tiled 1×1 with textures from
     * {@code shared/goo_0.png} … {@code goo_N}.
     */
    private void addGooDecorations(Result result, JsonValue level, float units) {
        JsonValue goos = level.get("goo");
        if (goos == null) {
            return;
        }
        JsonValue gooPositions = goos.get("positions");
        if (gooPositions == null || gooPositions.size == 0) {
            return;
        }

        JsonValue gooCfg = constants.get("goo");
        float surfaceLine = gooCfg != null
                ? gooCfg.getFloat("surface_line_from_bottom", GOO_SURFACE_LINE_FROM_BOTTOM_DEFAULT)
                : GOO_SURFACE_LINE_FROM_BOTTOM_DEFAULT;

        TextureRegion[] gooFrames = new TextureRegion[GOO_FRAME_COUNT];
        for (int f = 0; f < GOO_FRAME_COUNT; f++) {
            Texture t = textureResolver.apply(
                    "shared-goo-" + f,
                    "shared/goo_" + f + ".png"
            );
            gooFrames[f] = new TextureRegion(t);
        }
        result.gooFrames = gooFrames;

        JsonValue gooOrientations = goos.get("orientations");
        Texture tile0 = gooFrames[0].getTexture();

        for (int ii = 0; ii < gooPositions.size; ii++) {
            float[] pts = gooPositions.get(ii).asFloatArray();

            Surface goo = new Surface(pts, units, goos);
            goo.getObstacle().setName("goo" + ii);
            goo.setVisible(false);
            spriteAdder.accept(goo);

            float angle = (gooOrientations != null && ii < gooOrientations.size)
                    ? gooOrientations.get(ii).asFloat() : 0f;

            float minX = pts[0], maxX = pts[0], minY = pts[1], maxY = pts[1];
            for (int j = 2; j < pts.length; j += 2) {
                minX = Math.min(minX, pts[j]);
                maxX = Math.max(maxX, pts[j]);
            }
            for (int j = 1; j < pts.length; j += 2) {
                minY = Math.min(minY, pts[j]);
                maxY = Math.max(maxY, pts[j]);
            }

            float surfaceSpan;
            if (angle == 90 || angle == -90) {
                surfaceSpan = maxY - minY;
            } else {
                surfaceSpan = maxX - minX;
            }

            int nTiles = Math.max(1, (int) Math.ceil(surfaceSpan / TILE_WORLD_SIZE - 1e-5f));

            for (int ti = 0; ti < nTiles; ti++) {
                float tcx;
                float tcy;
                if (angle == 90 || angle == -90) {
                    tcx = (angle == 90) ? maxX - TILE_WORLD_SIZE / 2f : minX + TILE_WORLD_SIZE / 2f;
                    tcy = minY + TILE_WORLD_SIZE * (ti + 0.5f);
                } else {
                    tcx = minX + TILE_WORLD_SIZE * (ti + 0.5f);
                    if (angle == 180f) {
                        tcy = minY + TILE_WORLD_SIZE * (0.5f - surfaceLine);
                    } else {
                        tcy = maxY + TILE_WORLD_SIZE * (0.5f - surfaceLine);
                    }
                }

                int phaseOff = gooPhaseOffsetForDecor(tcx, tcy, GOO_FRAME_COUNT);
                BoxSprite decor = new BoxSprite(
                        units, tcx, tcy,
                        TILE_WORLD_SIZE, TILE_WORLD_SIZE,
                        BodyDef.BodyType.StaticBody, true, true,
                        0f, 0f, 0f, 0f,
                        "goo_decor" + ii + "_" + ti, tile0
                );
                decor.setTextureRegion(gooFrames[phaseOff]);
                decor.getObstacle().setAngle((float) Math.toRadians(angle));
                spriteAdder.accept(decor);
                result.gooDecors.add(decor);
            }
        }
    }

    /**
     * Stable per-tile animation offset from world position (no parallel arrays; same layout every load).
     */
    static int gooPhaseOffsetForDecor(float worldX, float worldY, int frameCount) {
        if (frameCount <= 1) {
            return 0;
        }
        int h = Float.floatToIntBits(worldX) ^ (Float.floatToIntBits(worldY) * 31);
        return Math.floorMod(h, frameCount);
    }

    private void addPulleyAssembly(Result result, JsonValue objectLocations, float units, World world) {
        if (objectLocations == null) {
            return;
        }

        JsonValue pulleySettings = objectLocations.get("pulley_settings");
        JsonValue groundAnchorsJson = pulleySettings == null ? null : pulleySettings.get("groundAnchors");

        Texture topTexture = textureResolver.apply("shared-pulley-top", "shared/pulley_top.png");
        JsonValue pulleyTop = objectLocations.get("pulley_top");
        addPulleyTopDecor(result, pulleyTop, units, topTexture);

        Texture stringTexture = textureResolver.apply("shared-pulley-string", "shared/pulley_string.png");
        JsonValue pulleyStrings = objectLocations.get("pulley_strings");
        if (groundAnchorsJson != null && groundAnchorsJson.size >= 2) {
            result.pulleyGroundAnchors.add(readVector(groundAnchorsJson.get(0)));
            result.pulleyGroundAnchors.add(readVector(groundAnchorsJson.get(1)));
        }
        addPulleyRopeDecor(result, pulleyStrings, units, stringTexture);

        Texture carryTexture = textureResolver.apply("shared-pulley-carry", "shared/pulley_carry.png");
        JsonValue pulleyCarries = objectLocations.get("pulley_carry");
        List<BoxSprite> carries = new ArrayList<>();
        List<Vector2> carryAnchors = new ArrayList<>();
        if (pulleyCarries != null) {
            for (int ii = 0; ii < pulleyCarries.size; ii++) {
                JsonValue entry = pulleyCarries.get(ii);
                float[] pos = entry.get("pos").asFloatArray();
                float[] size = entry.get("size").asFloatArray();
                BoxSprite carry = new BoxSprite(
                        units, pos[0], pos[1], size[0], size[1],
                        BodyDef.BodyType.DynamicBody, false, true,
                        entry.getFloat("density", 4.0f),
                        entry.getFloat("friction", 0.8f),
                        entry.getFloat("restitution", 0.0f),
                        entry.getFloat("gravityScale", 1.0f),
                        "pulley_carry" + ii,
                        carryTexture
                );
                carry.setObstacle(new BottomStripBoxObstacle(
                        pos[0], pos[1], size[0], size[1],
                        entry.getFloat("collisionWidth", size[0] * 0.55f),
                        entry.getFloat("collisionHeight", Math.min(size[1] * 0.22f, 0.2f))
                ));
                carry.getObstacle().setBodyType(BodyDef.BodyType.DynamicBody);
                carry.getObstacle().setPhysicsUnits(units);
                carry.getObstacle().setUserData(carry);
                carry.getObstacle().setSensor(false);
                carry.getObstacle().setFixedRotation(true);
                carry.getObstacle().setDensity(entry.getFloat("density", 4.0f));
                carry.getObstacle().setFriction(entry.getFloat("friction", 0.8f));
                carry.getObstacle().setRestitution(entry.getFloat("restitution", 0.0f));
                carry.getObstacle().setGravityScale(entry.getFloat("gravityScale", 1.0f));
                carry.getObstacle().setName("pulley_carry" + ii);
                spriteAdder.accept(carry);
                carry.getObstacle().setCentroid(new Vector2(0.0f, 0.0f));
                carry.getObstacle().getBody().setLinearDamping(entry.getFloat("linearDamping", 6.0f));
                carry.getObstacle().getBody().setAngularDamping(entry.getFloat("angularDamping", 10.0f));
                carries.add(carry);
                Vector2 anchor = readAnchor(entry, pos[0], pos[1] + (size[1] * 0.5f));
                carryAnchors.add(anchor);
                result.pulleyCarries.add(carry);
                result.pulleyCarryAnchorOffsets.add(new Vector2(anchor.x - pos[0], anchor.y - pos[1]));
            }
        }

        Texture blockTexture = textureResolver.apply("platform-rock", "platform/rock.png");
        JsonValue pulleyBlocks = objectLocations.get("pulley_block");
        if (pulleyBlocks != null) {
            for (int ii = 0; ii < pulleyBlocks.size; ii++) {
                JsonValue entry = pulleyBlocks.get(ii);
                float[] pos = entry.get("pos").asFloatArray();
                float[] size = entry.get("size").asFloatArray();
                BoxSprite block = new BoxSprite(
                        units, pos[0], pos[1], size[0], size[1],
                        BodyDef.BodyType.DynamicBody, false, true,
                        entry.getFloat("density", 6.0f),
                        entry.getFloat("friction", 1.1f),
                        entry.getFloat("restitution", 0.0f),
                        entry.getFloat("gravityScale", 1.0f),
                        "pulley_block" + ii,
                        blockTexture
                );
                spriteAdder.accept(block);
            }
        }

        if (world != null && pulleySettings != null && carries.size() == 2) {
            createPulleyJoint(world, carries, carryAnchors, pulleySettings);
        }
    }

    private void addPulleyRopeDecor(Result result, JsonValue entries, float units, Texture texture) {
        result.pulleyRopes.clear();
        if (entries == null) {
            return;
        }

        for (int ii = 0; ii < entries.size; ii++) {
            JsonValue entry = entries.get(ii);
            float[] pos = entry.get("pos").asFloatArray();
            float[] size = entry.get("size").asFloatArray();
            BoxSprite box = new BoxSprite(
                    units, pos[0], pos[1], size[0], size[1] * PULLEY_ROPE_OVERLAP_SCALE,
                    BodyDef.BodyType.StaticBody, true, true,
                    0.0f, 0.0f, 0.0f, 0.0f,
                    "pulley_string" + ii,
                    texture
            );
            spriteAdder.accept(box);
            result.pulleyRopes.add(box);
        }
    }

    private void addPulleyTopDecor(Result result, JsonValue entries, float units, Texture texture) {
        result.pulleyWheelCenters.clear();
        result.pulleyWheelRadii.clear();
        if (entries == null) {
            return;
        }
        for (int ii = 0; ii < entries.size; ii++) {
            JsonValue entry = entries.get(ii);
            float[] pos = entry.get("pos").asFloatArray();
            float[] size = entry.get("size").asFloatArray();
            float radius = entry.getFloat("radius", Math.min(size[0], size[1]) * 0.34f);
            WheelSprite wheel = new WheelSprite(
                    units, pos[0], pos[1], radius,
                    BodyDef.BodyType.StaticBody, true, true,
                    0.0f, 0.0f, 0.0f, 0.0f,
                    "pulley_top" + ii,
                    texture
            );
            spriteAdder.accept(wheel);
            result.pulleyWheelCenters.add(new Vector2(pos[0], pos[1]));
            result.pulleyWheelRadii.add(radius);
        }
    }

    private void createPulleyJoint(World world, List<BoxSprite> carries, List<Vector2> carryAnchors,
                                   JsonValue pulleySettings) {
        JsonValue groundAnchorsJson = pulleySettings.get("groundAnchors");
        if (groundAnchorsJson == null || groundAnchorsJson.size < 2) {
            return;
        }

        PulleyJointDef joint = new PulleyJointDef();
        joint.initialize(
                carries.get(0).getObstacle().getBody(),
                carries.get(1).getObstacle().getBody(),
                readVector(groundAnchorsJson.get(0)),
                readVector(groundAnchorsJson.get(1)),
                carryAnchors.get(0),
                carryAnchors.get(1),
                pulleySettings.getFloat("ratio", 1.0f)
        );
        world.createJoint(joint);
    }

    private Vector2 readAnchor(JsonValue entry, float defaultX, float defaultY) {
        JsonValue anchor = entry.get("anchor");
        if (anchor == null) {
            return new Vector2(defaultX, defaultY);
        }
        return readVector(anchor);
    }

    private Vector2 readVector(JsonValue value) {
        float[] xy = value.asFloatArray();
        return new Vector2(xy[0], xy[1]);
    }

    /**
     * Factory method that creates a {@link Zuko} instance and assigns all animation sheets.
     */
    private Zuko buildZuko(float units, JsonValue zukoJson, float xStartingPos, float yStartingPos,
                           Texture zukoTexture, Texture walkSheet,
                           Texture photoSheet, Texture jumpSheet, Texture tongueTexture,
                           String name, JsonValue levelPlayerSettings) {
        Zuko zuko = new Zuko(units, zukoJson, xStartingPos, yStartingPos, levelPlayerSettings);
        zuko.setTexture(zukoTexture);
        zuko.setBaseTexture(zukoTexture);
        zuko.getObstacle().setName(name);
        zuko.setWalkAnimation(walkSheet,  1, 6, 6);
        zuko.setPhotoAnimation(photoSheet, 1, 13, 13);
        zuko.setJumpAnimation(jumpSheet,  1, 7, 7);
        zuko.setTongueSegment(tongueTexture);

        return zuko;
    }

    /**
     * Generates physics-only colliders for contiguous horizontal tile runs.
     * Merging adjacent cells avoids Box2D seam catches that can snag low-friction objects.
     */
    private void addTilemapColliders(JsonValue level, JsonValue collisionSettings, float units) {
        if (collisionSettings == null) {
            return;
        }

        JsonValue tilemap = level.get("tilemap");
        if (tilemap == null || tilemap.size == 0) {
            return;
        }

        Map<Integer, TreeSet<Integer>> rows = new HashMap<>();
        Set<String> seenTiles = new HashSet<>();
        for (int ii = 0; ii < tilemap.size; ii++) {
            JsonValue entry = tilemap.get(ii);
            int tx = entry.getInt("tx");
            int ty = entry.getInt("ty");
            String key = tx + ":" + ty;
            if (!seenTiles.add(key)) {
                continue;
            }
            rows.computeIfAbsent(ty, ignored -> new TreeSet<>()).add(tx);
        }

        int colliderIndex = 0;
        for (Map.Entry<Integer, TreeSet<Integer>> rowEntry : rows.entrySet()) {
            int ty = rowEntry.getKey();
            TreeSet<Integer> xs = rowEntry.getValue();
            Integer runStart = null;
            Integer previous = null;
            for (Integer tx : xs) {
                if (runStart == null) {
                    runStart = tx;
                } else if (previous != null && tx != previous + 1) {
                    createTileCollider(runStart, previous + 1, ty, units, collisionSettings, colliderIndex++);
                    runStart = tx;
                }
                previous = tx;
            }
            if (runStart != null && previous != null) {
                createTileCollider(runStart, previous + 1, ty, units, collisionSettings, colliderIndex++);
            }
        }
    }

    private void createTileCollider(int startX, int endExclusiveX, int y, float units,
                                    JsonValue collisionSettings, int colliderIndex) {
        Surface tileCollider = new Surface(new float[]{
                startX, y,
                endExclusiveX, y,
                endExclusiveX, y + TILE_WORLD_SIZE,
                startX, y + TILE_WORLD_SIZE
        }, units, buildInvisibleCollisionSettings(collisionSettings));
        tileCollider.getObstacle().setName("tilecollider" + colliderIndex);
        spriteAdder.accept(tileCollider);
    }

    private JsonValue buildInvisibleCollisionSettings(JsonValue collisionSettings) {
        JsonValue copy = new JsonReader().parse(collisionSettings.toJson(JsonWriter.OutputType.json));
        copy.addChild("invisible", new JsonValue(true));
        return copy;
    }

    /**
     * Deep-copies the canonical Zuko JSON and overrides {@code pos} for extra zukosprites.
     * <p>
     * The Zuko constructor reads: pos, inner, size, force, damping, density,
     * friction, maxspeed, jump_force, jump_cool, shot_cool, sensor, debug.
     * The canonical node is cloned and {@code pos} is patched so Zuko spawns at {@code (x, y)}.
     */
    private JsonValue buildSyntheticZukoJson(JsonValue canonical, float x, float y) {
        // Deep-copy by round-tripping through the JSON string representation.
        JsonValue copy = new JsonReader().parse(canonical.toJson(JsonWriter.OutputType.json));

        JsonValue pos = copy.get("pos");
        pos.get(0).set(x, null);
        pos.get(1).set(y, null);

        return copy;
    }

    /**
     * Deep-copies surface settings and scales the {@code tile} value.
     */
    private JsonValue buildScaledTileSettings(JsonValue canonical, float scale) {
        JsonValue copy = new JsonReader().parse(canonical.toJson(JsonWriter.OutputType.json));
        copy.get("tile").set(copy.getFloat("tile") * scale, null);
        return copy;
    }
}
