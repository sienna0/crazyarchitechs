package edu.cornell.cis3152.physics.screen.levels;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonWriter;
import edu.cornell.cis3152.physics.screen.WorldState;
import edu.cornell.cis3152.physics.world.*;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private static final float OBJECT_SIZE = 1.0f;
    private static final float FLOOR_TILE_SCALE = 2.0f;
    private static final float TILE_WORLD_SIZE = 1.0f;

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
    Result populate(int currentLevel, float units, WorldState worldState) {
        Result result = new Result();

        JsonValue level = constants.get("level" + currentLevel);
        JsonValue objectLocations = level.get("objectLocations");

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

        // ── Walls ────────────────────────────────────────────────────────────
        Texture earthTexture = textureResolver.apply("shared-earth", "shared/earthtile.png");
        JsonValue walls = level.get("walls");
        JsonValue wallPositions = walls.get("positions");
        for (int ii = 0; ii < wallPositions.size; ii++) {
            Surface wall = new Surface(wallPositions.get(ii).asFloatArray(), units, walls);
            wall.getObstacle().setName("wall" + ii);
            wall.setTexture(earthTexture);
            spriteAdder.accept(wall);
        }

        JsonValue platforms = level.get("platforms");
        JsonValue platformPositions = platforms.get("positions");
        for (int ii = 0; ii < platformPositions.size; ii++) {
            Surface platform = new Surface(platformPositions.get(ii).asFloatArray(), units, platforms);
            platform.getObstacle().setName("platform" + ii);
            platform.setTexture(earthTexture);
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
                zukoTexture, walkSheet, photoSheet, jumpSheet, tongueTexture, "avatar");
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
                        "zukosprite" + ii);
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

        return result;
    }

    /**
     * Factory method that creates a {@link Zuko} instance and assigns all animation sheets.
     */
    private Zuko buildZuko(float units, JsonValue zukoJson, float xStartingPos, float yStartingPos,
                           Texture zukoTexture, Texture walkSheet,
                           Texture photoSheet, Texture jumpSheet, Texture tongueTexture,
                           String name) {
        Zuko zuko = new Zuko(units, zukoJson, xStartingPos, yStartingPos);
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
     * Generates {@link InvisibleSurface} colliders for each tilemap cell.
     */
    private void addTilemapColliders(JsonValue level, JsonValue collisionSettings, float units) {
        if (collisionSettings == null) {
            return;
        }

        JsonValue tilemap = level.get("tilemap");
        if (tilemap == null || tilemap.size == 0) {
            return;
        }

        Set<String> seenTiles = new HashSet<>();
        for (int ii = 0; ii < tilemap.size; ii++) {
            JsonValue entry = tilemap.get(ii);
            int tx = entry.getInt("tx");
            int ty = entry.getInt("ty");
            String key = tx + ":" + ty;
            if (!seenTiles.add(key)) {
                continue;
            }

            InvisibleSurface tileCollider = new InvisibleSurface(new float[]{
                    tx, ty,
                    tx + TILE_WORLD_SIZE, ty,
                    tx + TILE_WORLD_SIZE, ty + TILE_WORLD_SIZE,
                    tx, ty + TILE_WORLD_SIZE
            }, units, collisionSettings);
            tileCollider.getObstacle().setName("tilecollider" + ii);
            spriteAdder.accept(tileCollider);
        }
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
