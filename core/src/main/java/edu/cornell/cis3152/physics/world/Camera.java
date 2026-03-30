package edu.cornell.cis3152.physics.world;

import com.badlogic.gdx.utils.JsonValue;

public class Camera {
    // should work via cam = new Camera(smth) cam.setType(thermal or smth) cam.takePicture
    //    Camera Variables
    /**
     * Current number of photos Zuko can take
     */
    private int filmCount;

    /**
     * Frames remaining until next photo can be taken
     */
    private int pictureCooldown;

    /**
     * Max cooldown frames until next picture can be taken
     */
    private int pictureLimit;

    /**
     * Maximum range for photos
     */
    private float maxSightDistance;


    /**
     * Whether I just took a photo - used for Sound
     */
    private boolean pictureTaken;

    /**
     * Returns true if Zuko can take a picture.
     * Zuko can take a picture if he has film remaining, picture cooldown has expired, there is a current target,
     * and he has a line of sight to the target
     *
     * @return true if Zuko can take a picture
     */
    public boolean canTakePicture(float targetX, float targetY, float zukoX, float zukoY) {
        return filmCount > 0
                && pictureCooldown <= 0
                && hasLineOfSight(targetX, targetY, zukoX, zukoY, maxSightDistance);
    }

    /**
     * Takes a picture of the current target.
     * Decrements the film count, sets the picture cooldown
     * Flags that a picture was taken for sound purposes - It seems we might not be adding this for now
     */
    public void takePicture() {
        filmCount--;
        pictureCooldown = pictureLimit;
        pictureTaken = true;
    }

    /**
     * Returns tre if a picture was just taken this frame.
     * This is used to trigger the camera shutter sound.
     *
     * @return true if a picture was just taken
     */
    public boolean isPictureTaken() {
        return pictureTaken;
    }

    /**
     * Clears the picture taken flag.
     * This should be called after the picture taken flag has been handled.
     *
     */
    public void clearPictureTaken() {
        pictureTaken = false;
    }

    /**
     * Returns the current number of photos Zuko can take.
     *
     * @return the current film count
     */
    public int getFilmCount() {
        return filmCount;
    }

    /**
     * Sets the current number of photos Zuko can take
     * Added this just in case each level has a different limit
     *
     * @param value the new film count
     */
    public void setFilmCount(int value) {
        filmCount = value;
    }

    /**
     * Returns true if Zuko has line of sight to the current target.
     * Line of sight is determined by the Euclidean distance between Zuko and target.
     * If the target is within maxSightDistance, Zuko has line of sight
     * No use for current's target x and y position, will be using mouse position
     *
     * @return true if Zuko has line of sight to the current target
     */
    public boolean hasLineOfSight(float targetX, float targetY, float zukoX, float zukoY, float maxDistance) {
        float dx = targetX - zukoX;
        float dy = targetY - zukoY;
        float d = (dx * dx) + (dy * dy);

        return d <= (maxDistance * maxDistance);
    }

    public Camera() {
        filmCount = 10;
        pictureLimit = 10;
        pictureCooldown = 0;
        pictureTaken = false;
        maxSightDistance = 9.0f;

    }

    /**
     * Updates the camera's state (NOT GAME LOGIC).
     *
     * We use this method to reset cooldowns.
     *
     * @param dt  Number of seconds since last animation frame
     */
    public void update(float dt) {
        if (pictureCooldown > 0) {
            pictureCooldown--;
        }
    }
}
