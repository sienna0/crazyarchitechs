package edu.cornell.cis3152.physics.screen.levels;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.cis3152.physics.screen.WorldState;
import edu.cornell.cis3152.physics.world.*;
import edu.cornell.gdiac.physics2.ObstacleSprite;

import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Builds the level sprites from JSON data.
 */
class LevelPopulation {
    static class Result {
        Door goalDoor;
        Zuko avatar;
        GameObject honey;
        GameObject ice;
        GameObject cloud;
    }

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

        Texture texture = textureResolver.apply("shared-goal", "shared/goaldoor.png");
        JsonValue level = constants.get("level" + currentLevel);
        JsonValue goal = level.get("objectLocations").get("goal");
        result.goalDoor = new Door(units, goal);
        result.goalDoor.setTexture(texture);
        result.goalDoor.getObstacle().setName("goal");
        spriteAdder.accept(result.goalDoor);

        texture = textureResolver.apply("shared-earth", "shared/earthtile.png");
        JsonValue walls = level.get("walls");
        JsonValue wallPositions = walls.get("positions");
        for (int ii = 0; ii < wallPositions.size; ii++) {
            Surface wall = new Surface(wallPositions.get(ii).asFloatArray(), units, walls);
            wall.getObstacle().setName("wall" + ii);
            wall.setTexture(texture);
            spriteAdder.accept(wall);
        }

        JsonValue platforms = level.get("platforms");
        JsonValue platformPositions = platforms.get("positions");
        for (int ii = 0; ii < platformPositions.size; ii++) {
            Surface platform = new Surface(platformPositions.get(ii).asFloatArray(), units, walls);
            platform.getObstacle().setName("platform" + ii);
            platform.setTexture(texture);
            spriteAdder.accept(platform);
        }

        texture = textureResolver.apply("platform-traci", "platform/traci.png");
        result.avatar = new Zuko(units, level.get("objectLocations").get("zuko"));
        result.avatar.setTexture(texture);
        result.avatar.setBaseTexture(texture);
        spriteAdder.accept(result.avatar);
        result.avatar.createSensor();
        Texture photoSheet = textureResolver.apply("platform-camera", "platform/cameraflash.png");
        result.avatar.setPhotoAnimation(photoSheet, 1, 17, 17);
        Texture jumpSheet = textureResolver.apply("platform-jump", "platform/zukojump.png");
        result.avatar.setJumpAnimation(jumpSheet, 1, 7, 7);

        float cloudSize = 1.5f;
        float objectWidth = 1.5f;

        Texture rockTexture = textureResolver.apply("platform-rock", "platform/rock.png");
        float rockHeight = objectWidth * ((float) rockTexture.getHeight() / rockTexture.getWidth());
        JsonValue objectLocations = level.get("objectLocations");
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
}
