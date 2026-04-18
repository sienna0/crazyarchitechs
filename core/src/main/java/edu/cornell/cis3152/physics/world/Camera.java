package edu.cornell.cis3152.physics.world;

import com.badlogic.gdx.utils.JsonValue;

/**
 * Zuko's Polaroid camera. Manages film count, shot cooldown, and
 * line-of-sight range for taking photos of interactive objects.
 *
 * All tuning values are read from the Zuko JSON block so that each
 * level can define its own camera constraints.
 */
public class Camera {

    /** Remaining photos Zuko can take this level. */
    private int filmCount;
    /** Frames remaining until the next photo is allowed. */
    private int pictureCooldown;
    /** Cooldown duration in frames after each photo. */
    private final int pictureLimit;
    /** Maximum Euclidean distance for photographing an object. */
    private final float maxSightDistance;
    /** One-frame flag consumed by the scene to play the shutter sound. */
    private boolean pictureTaken;

    /**
     * Creates a camera with settings from the Zuko JSON node.
     *
     * @param data the Zuko JSON node (expects "film_count", "picture_cooldown",
     *             "max_sight_distance")
     */
    public Camera(JsonValue data) {
        this.filmCount = data.getInt("film_count", 10);
        this.pictureLimit = data.getInt("picture_cooldown", 10);
        this.maxSightDistance = data.getFloat("max_sight_distance", 9.0f);
        this.pictureCooldown = 0;
        this.pictureTaken = false;
    }

    /**
     * Returns true if Zuko can currently take a photo of the target.
     * Requires film remaining, cooldown expired, and target within range.
     */
    public boolean canTakePicture(float targetX, float targetY, float zukoX, float zukoY) {
        return filmCount > 0
                && pictureCooldown <= 0
                && hasLineOfSight(targetX, targetY, zukoX, zukoY, maxSightDistance);
    }

    /** Consumes one film, starts the cooldown, and flags the shutter sound. */
    public void takePicture() {
        filmCount--;
        pictureCooldown = pictureLimit;
        pictureTaken = true;
    }

    /** Returns true during the single frame after a photo was taken (for sound). */
    public boolean isPictureTaken() {
        return pictureTaken;
    }

    /** Clears the one-frame picture-taken flag after the scene has handled it. */
    public void clearPictureTaken() {
        pictureTaken = false;
    }

    public int getFilmCount() { return filmCount; }

    public void setFilmCount(int value) { filmCount = value; }

    /**
     * Checks whether the target position is within range of Zuko.
     * Currently uses Euclidean distance only.
     *
     * @return true if the target is within maxDistance units of Zuko
     */
    // TODO: implement world.rayCast() for true LOS
    public boolean hasLineOfSight(float targetX, float targetY,
                                  float zukoX, float zukoY, float maxDistance) {
        float dx = targetX - zukoX;
        float dy = targetY - zukoY;
        return (dx * dx + dy * dy) <= (maxDistance * maxDistance);
    }

    //FIXME this is never called? Then what's the point of picture cooldown?
    /** Ticks down the cooldown timer each frame. */
    public void update(float dt) {
        if (pictureCooldown > 0) {
            pictureCooldown--;
        }
    }
}
