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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

class LevelPopulation {

    private static final int TILE_PX = 16;
    private static final float OBJECT_SIZE = 1.0f;
    private static final float FLOOR_TILE_SCALE = 2.0f;

    static class Result {
        Door goalDoor;
        Zuko avatar;
        /** Extra Zuko-sprite objects placed via the editor's zukosprite tool. */
        List<Zuko> extraZukos = new ArrayList<>();
        GameObject honey;
        GameObject ice;
        GameObject cloud;

        // Tilemap — purely visual, drawn manually each frame.
        // Parallel arrays: tileRegions[i] is drawn at tilePositions[i] (world units).
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
        if (currentLevel == 1) {
            addTilemapCollisionFallback(level, floors, units);
        }

        Texture zukoTexture  = textureResolver.apply("platform-traci",  "platform/traci.png");
        Texture walkSheet    = textureResolver.apply("platform-walk",   "platform/zukowalk.png");
        Texture photoSheet   = textureResolver.apply("platform-camera", "platform/cameraflash.png");
        Texture jumpSheet    = textureResolver.apply("platform-jump",   "platform/zukojump.png");

        result.avatar = buildZuko(units, level.get("objectLocations").get("zuko"),
                zukoTexture, walkSheet, photoSheet, jumpSheet, "avatar");
        spriteAdder.accept(result.avatar);
        result.avatar.createSensor();

        JsonValue zukoSprites = objectLocations.get("zukosprite");
        if (zukoSprites != null) {
            for (int ii = 0; ii < zukoSprites.size; ii++) {
                float[] pos = zukoSprites.get(ii).asFloatArray();

                JsonValue syntheticZuko = buildSyntheticZukoJson(
                        level.get("objectLocations").get("zuko"), pos[0], pos[1]);

                Zuko extra = buildZuko(units, syntheticZuko,
                        zukoTexture, walkSheet, photoSheet, jumpSheet,
                        "zukosprite" + ii);
                spriteAdder.accept(extra);
                extra.createSensor();
                result.extraZukos.add(extra);
            }
        }

        float objectWidth = OBJECT_SIZE;

        Texture rockTexture = textureResolver.apply("platform-rock", "platform/rock.png");
        float rockHeight = objectWidth * ((float) rockTexture.getHeight() / rockTexture.getWidth());
        JsonValue rockPositions = objectLocations.get("rock");
        for (int ii = 0; ii < rockPositions.size; ii++) {
            float[] pos = rockPositions.get(ii).asFloatArray();
            result.honey = new GameObject(
                    Obj.HONEY, constants.get("rock"), units,
                    pos[0], pos[1],
                    objectWidth, rockHeight,
                    BodyDef.BodyType.DynamicBody,
                    false
            );
            result.honey.setTexture(rockTexture);
            spriteAdder.accept(result.honey);
        }

        Texture iceTexture = textureResolver.apply("platform-ice", "platform/ice.png");
        float iceHeight = objectWidth * ((float) iceTexture.getHeight() / iceTexture.getWidth());
        JsonValue icePositions = objectLocations.get("ice");
        for (int ii = 0; ii < icePositions.size; ii++) {
            float[] pos = icePositions.get(ii).asFloatArray();
            result.ice = new GameObject(
                    Obj.ICE, constants.get("ice"), units,
                    pos[0], pos[1],
                    objectWidth, iceHeight,
                    BodyDef.BodyType.DynamicBody,
                    false
            );
            result.ice.setTexture(iceTexture);
            spriteAdder.accept(result.ice);
        }

        float cloudSize = OBJECT_SIZE;
        Texture cloudTexture = textureResolver.apply("platform-cloud", "platform/cloud.png");
        JsonValue cloudPositions = objectLocations.get("cloud");
        for (int ii = 0; ii < cloudPositions.size; ii++) {
            float[] pos = cloudPositions.get(ii).asFloatArray();
            result.cloud = new GameObject(
                    Obj.CLOUD, constants.get("cloud"), units,
                    pos[0], pos[1],
                    cloudSize, cloudSize,
                    BodyDef.BodyType.DynamicBody,
                    false
            );
            result.cloud.setTexture(cloudTexture);
            spriteAdder.accept(result.cloud);
            worldState.setCloudHomeY(result.cloud.getObstacle().getY());
        }

        return result;
    }


    private Zuko buildZuko(float units, JsonValue zukoJson,
                           Texture zukoTexture, Texture walkSheet,
                           Texture photoSheet, Texture jumpSheet,
                           String name) {
        Zuko zuko = new Zuko(units, zukoJson);
        zuko.setTexture(zukoTexture);
        zuko.setBaseTexture(zukoTexture);
        zuko.getObstacle().setName(name);
        zuko.setWalkAnimation(walkSheet,  1, 6, 6);
        zuko.setPhotoAnimation(photoSheet, 1, 13, 13);
        zuko.setJumpAnimation(jumpSheet,  1, 7, 7);
        return zuko;
    }

    private void addTilemapCollisionFallback(JsonValue level, JsonValue floorSettings, float units) {
        if (floorSettings == null) {
            return;
        }

        JsonValue floorPositions = floorSettings.get("positions");
        if (floorPositions != null && floorPositions.size > 0) {
            return;
        }

        JsonValue tilemap = level.get("tilemap");
        if (tilemap == null || tilemap.size == 0) {
            return;
        }

        Map<Integer, List<Integer>> tilesByRow = new HashMap<>();
        for (int ii = 0; ii < tilemap.size; ii++) {
            JsonValue entry = tilemap.get(ii);
            int tx = entry.getInt("tx");
            int ty = entry.getInt("ty");
            tilesByRow.computeIfAbsent(ty, ignored -> new ArrayList<>()).add(tx);
        }

        int colliderIndex = 0;
        List<Integer> rows = new ArrayList<>(tilesByRow.keySet());
        Collections.sort(rows);
        for (int row : rows) {
            List<Integer> cols = tilesByRow.get(row);
            Collections.sort(cols);
            int start = cols.get(0);
            int previous = start;

            for (int ii = 1; ii <= cols.size(); ii++) {
                boolean contiguous = ii < cols.size() && cols.get(ii) == previous + 1;
                if (contiguous) {
                    previous = cols.get(ii);
                    continue;
                }

                InvisibleSurface floor = new InvisibleSurface(new float[]{
                        start, row,
                        previous + 1.0f, row,
                        previous + 1.0f, row + 1.0f,
                        start, row + 1.0f
                }, units, floorSettings);
                floor.getObstacle().setName("tilefloor" + colliderIndex++);
                spriteAdder.accept(floor);

                if (ii < cols.size()) {
                    start = cols.get(ii);
                    previous = start;
                }
            }
        }
    }

    /**
     * Builds a synthetic JsonValue for an extra zukosprite by copying all
     * physics constants from the canonical zuko node but overriding pos.
     *
     * The Zuko constructor reads: pos, inner, size, force, damping, density,
     * friction, maxspeed, jump_force, jump_cool, shot_cool, sensor, debug.
     * We clone the canonical node and patch pos so Zuko spawns at [x, y].
     */
    private JsonValue buildSyntheticZukoJson(JsonValue canonical, float x, float y) {
        // Deep-copy by round-tripping through the JSON string representation.
        JsonValue copy = new JsonReader().parse(canonical.toJson(JsonWriter.OutputType.json));

        JsonValue pos = copy.get("pos");
        pos.get(0).set(x, null);
        pos.get(1).set(y, null);

        return copy;
    }

    private JsonValue buildScaledTileSettings(JsonValue canonical, float scale) {
        JsonValue copy = new JsonReader().parse(canonical.toJson(JsonWriter.OutputType.json));
        copy.get("tile").set(copy.getFloat("tile") * scale, null);
        return copy;
    }
}
