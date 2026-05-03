package edu.cornell.cis3152.physics.world;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;

import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.SpriteSheet;

import edu.cornell.gdiac.physics2.*;

public class ZukoAnimator {
    /** The SpriteSheet for Zuko's phototaking animation */
    private SpriteSheet photoSheet;
    /** The duration of the photo animation */
    private float photoAnimationTime = 0f;
    /** The duration of each frame */
    private float photoFrameDuration = 0.07f;
    /** Whether the animation is playing or not */
    private boolean playingPhoto = false;

    /** The SpriteSheet for Zuko's jumping animation */
    private SpriteSheet jumpSheet;
    /** The duration of the animation */
    private float jumpAnimationTime = 0f;
    /** The duration of each frame */
    private float jumpFrameDuration = 0.18f;
    /** Whether the animation is playing or not */
    private boolean playingJump = false;

    /** The SpriteSheet for Zuko's walk animation */
    private SpriteSheet walkSheet;
    /** The duration of the walk animation */
    private float walkAnimationTime = 0f;
    /** The duration of each walk frame */
    private float walkFrameDuration = 0.14f;

    /** The SpriteSheet for Zuko's walk animation */
    private SpriteSheet idleSheet;
    /** The duration of the walk animation */
    private float idleAnimationTime = 0f;
    /** The duration of each walk frame */
    private float idleFrameDuration = 0.14f;

    /** The SpriteSheet for Zuko's melting death animation */
    private SpriteSheet deathMeltSheet;
    /** The duration of the melting death animation */
    private float deathMeltAnimationTime = 0f;
    /** The duration of each melting death frame */
    private float deathMeltFrameDuration = 0.065f;
    /** Whether the animation is playing or not */
    private boolean playingDeathMelt = false;
    /** Animation delay **/
    private int deathDelay = 6;

    /** The SpriteSheet for Zuko's portal animation */
    private SpriteSheet portalSheet;
    /** The duration of the portal animation */
    private float portalAnimationTime = 0f;
    /** The duration of each portal frame */
    private float portalFrameDuration = 0.06f;
    /** Whether the portal animation is actively playing */
    private boolean playingPortal = false;
    /** Whether the portal animation has reached its last frame */
    private boolean portalFinished = false;

    /** The SpriteSheet for Zuko's spawn animation */
    private SpriteSheet spawnSheet;
    /** The duration of the spawn animation */
    private float spawnAnimationTime = 0f;
    /** The duration of each spawn frame */
    private float spawnFrameDuration = 0.06f;
    /** Whether the spawn animation is actively playing */
    private boolean playingSpawn = false;
    /** Whether the spawn animation has reached its last frame */
    private boolean spawnFinished = false;

    /** The Texture of one segment of Zuko's tongue */
    private Texture tongueSegment;
    /** The progress of the tongue to the target. 0 = fully retracted, 1 = fully extended */
    private float tongueProgress = 0f;
    /** The speed of the tongue */
    private float tongueSpeed = 3.0f;
    /** The sticking target */
    private Vector2 tongueTarget = new Vector2();
//    /** The offset of the tongue on Zuko's sprite */
//    private Vector2 tongueMouthOffset = new Vector2(0.1f, 0f);
    /** The state of Zuko's tongue. 0 = idle, 1 = extending, 2 = retracting */
    private int tongueState = 0; // 0 = idle, 1 = extending, 2 = retracting
    /** The Affine2 for drawing the tongue */
    private final Affine2 tongueTransform = new Affine2();
    /** Distance from tongue to target */
    private float tongueTotalDist = 0f;

    private Texture baseTexture;

    /** Cache for the affine flip */
    private final Affine2 flipCache = new Affine2();

    public Texture getBaseTexture() { return baseTexture; }

    public void setBaseTexture(Texture texture) {
        baseTexture = texture;
    }


    /**
     * Starts the photo-taking animation
     */
    public void startPhotoAnimation() {
        playingPhoto = true;
        photoAnimationTime = 0f;
    }

    /**
     * Sets the photo animation SpriteSheet for Zuko
     * @param sheet
     * @param rows
     * @param cols
     */
    public void setPhotoAnimation(Texture sheet, int rows, int cols, int size) {
        photoSheet = new SpriteSheet(sheet, rows, cols, size);
    }

    /**
     * Starts Zuko's jump animation
     */
    public void startJumpAnimation() {
        playingJump = true;
        jumpAnimationTime = 0f;
    }

    /**
     * Sets the jump animation SpriteSheet for Zuko
     * @param sheet
     * @param rows
     * @param cols
     */
    public void setJumpAnimation(Texture sheet, int rows, int cols, int size) {
        jumpSheet = new SpriteSheet(sheet, rows, cols, size);
    }

    /**
     * Sets the walk animation SpriteSheet for Zuko
     * @param sheet
     * @param rows
     * @param cols
     */
    public void setWalkAnimation(Texture sheet, int rows, int cols, int size) {
        walkSheet = new SpriteSheet(sheet, rows, cols, size);
    }

    /**
     * Starts Zuko's jump animation
     */
    public void startDeathMeltAnimation() {
        playingDeathMelt = true;
        deathMeltAnimationTime = 0f;
    }

    /**
     * Starts Zuko's portal animation
     */
    public void startPortalAnimation() {
        playingPortal = true;
        portalFinished = false;
        portalAnimationTime = 0f;
        if (portalSheet != null) {
            portalSheet.setFrame(0);
        }
    }

    /**
     * Starts Zuko's spawn animation
     */
    public void startSpawnAnimation() {
        playingSpawn = true;
        spawnFinished = false;
        spawnAnimationTime = 0f;
        if (spawnSheet != null) {
            spawnSheet.setFrame(0);
        }
    }

    /**
     * Sets the jump animation SpriteSheet for Zuko
     * @param sheet
     * @param rows
     * @param cols
     */
    public void setDeathMeltAnimation(Texture sheet, int rows, int cols, int size) {
        deathMeltSheet = new SpriteSheet(sheet, rows, cols, size);
    }

    /**
     * Sets the portal animation SpriteSheet for Zuko
     * @param sheet
     * @param rows
     * @param cols
     */
    public void setPortalAnimation(Texture sheet, int rows, int cols, int size) {
        portalSheet = new SpriteSheet(sheet, rows, cols, size);
        portalFinished = false;
    }

    /**
     * Sets the spawn animation SpriteSheet for Zuko
     * @param sheet
     * @param rows
     * @param cols
     */
    public void setSpawnAnimation(Texture sheet, int rows, int cols, int size) {
        spawnSheet = new SpriteSheet(sheet, rows, cols, size);
        spawnFinished = false;
    }

    /**
     * Sets the jump animation SpriteSheet for Zuko
     * @param sheet
     * @param rows
     * @param cols
     */
    public void setIdleAnimation(Texture sheet, int rows, int cols, int size) {
        idleSheet = new SpriteSheet(sheet, rows, cols, size);
    }

    /**
     * Sets the tongue segment texture
     */
    public void setTongueSegment(Texture texture) {
        this.tongueSegment = texture;
    }

    /**
     * Starts Zuko's tongue animation when a photo is being stuck
     */
    public void startTongueAnimation(float zukoX, float zukoY, float targetX, float targetY, boolean faceRight, float units) {
        tongueTarget.set(targetX * units, targetY * units);

        Vector2 mouth = getMouthPosition(zukoX, zukoY, faceRight, units);

        float dx = tongueTarget.x - mouth.x;
        float dy = tongueTarget.y - mouth.y;
        tongueTotalDist = (float)Math.sqrt(dx * dx + dy * dy);

        tongueProgress = 0f;
        tongueState = 1;
        System.out.println("zukoX=" + zukoX + " zukoY=" + zukoY + " targetX=" + targetX + " targetY=" + targetY + " units=" + units);
    }

    /**
     * Separate draw method for the tongue. Draws incrementally based on progress.
     */
    public void drawTongue(SpriteBatch batch, boolean faceRight, float zukoX, float zukoY, float units) {
        if (tongueState == 0 || tongueSegment == null) return;

        // Use the same mouth position as startTongueAnimation
        Vector2 mouth = getMouthPosition(zukoX, zukoY, faceRight, units);
        float mx = mouth.x;
        float my = mouth.y;

        // Current tip position based on progress
        float tipX = mx + (tongueTarget.x - mx) * tongueProgress;
        float tipY = my + (tongueTarget.y - my) * tongueProgress;

        float dx = tipX - mx;
        float dy = tipY - my;
        float len = (float) Math.sqrt(dx * dx + dy * dy);

        if (len <= 0f) return;

// Compute perpendicular ONCE for the whole tongue
        float px = -dy / len;
        float py = dx / len;

        float pixelSize = 1f;
        float step = 1f;
        float tongueWidth = 1.3f;

        int i = 0;
        for (float d = 0; d <= len; d += step, i++) {
            float t = d / len;
            float sx = (float)Math.round((mx + dx * t) / step) * step;
            float sy = (float)Math.round((my + dy * t) / step) * step;

            float halfW = tongueWidth / 2;
            for (float w = -halfW; w <= halfW; w++) {
                float wx = (float)Math.round(sx + px * w);
                float wy = (float)Math.round(sy + py * w);
                batch.draw(tongueSegment, wx - pixelSize / 2f, wy - pixelSize / 2f, pixelSize, pixelSize);
            }
        }
        System.out.println("mouth=" + mx + "," + my + " tip=" + tipX + "," + tipY);
    }

    private Vector2 getMouthPosition(float zukoX, float zukoY, boolean faceRight, float units) {
        float mouthOffsetX = 0.2f;  // was 0.3f
        float mouthOffsetY = -0.1f;  // was -0.2f, pushed it too low

        return new Vector2(
                zukoX * units + (faceRight ? mouthOffsetX : -mouthOffsetX) * units,
                zukoY * units + mouthOffsetY * units
        );
    }

    public void update(float dt, boolean isGrounded, float velocityX) {
        if (playingDeathMelt && deathMeltSheet != null) {
            deathMeltAnimationTime += dt;
            int frame = (int)(deathMeltAnimationTime / deathMeltFrameDuration);
            if (frame >= deathMeltSheet.getSize()) {
                if (deathDelay > 0) {
                    deathDelay--;
                } else {
                    playingDeathMelt = false;
                    deathMeltSheet.setFrame(0);
                }
            } else {
                deathMeltSheet.setFrame(frame);
            }
        }
        if (playingSpawn && spawnSheet != null) {
            playingJump = false;
            playingPhoto = false;
            spawnAnimationTime += dt;
            int frame = (int)(spawnAnimationTime / spawnFrameDuration);
            if (frame >= spawnSheet.getSize()) {
                playingSpawn = false;
                spawnFinished = true;
                spawnSheet.setFrame(spawnSheet.getSize() - 1);
            } else {
                spawnSheet.setFrame(frame);
            }
        } else if (playingPortal && portalSheet != null) {
            playingJump = false;
            playingPhoto = false;
            portalAnimationTime += dt;
            int frame = (int)(portalAnimationTime / portalFrameDuration);
            if (frame >= portalSheet.getSize()) {
                playingPortal = false;
                portalFinished = true;
                portalSheet.setFrame(portalSheet.getSize() - 1);
            } else {
                portalSheet.setFrame(frame);
            }
        } else if (playingPhoto && photoSheet != null) {
            playingJump = false;
            photoAnimationTime += dt;
            int frame = (int)(photoAnimationTime / photoFrameDuration);
            if (frame >= photoSheet.getSize()) {
                playingPhoto = false;
                photoSheet.setFrame(0);
            } else {
                photoSheet.setFrame(frame);
            }
        } else if (playingJump && jumpSheet != null) {
            jumpAnimationTime += dt;
            int frame = (int)(jumpAnimationTime / jumpFrameDuration);
            if (frame >= jumpSheet.getSize()) {
                playingJump = false;
                jumpSheet.setFrame(0);
            } else {
                jumpSheet.setFrame(frame);
            }
        } else if (walkSheet != null && isGrounded && Math.abs(velocityX) > 0.1f) {
            walkAnimationTime += dt;
            int frame = ((int)(walkAnimationTime / walkFrameDuration)) % walkSheet.getSize();
            walkSheet.setFrame(frame);
        } else if (idleSheet != null && isGrounded) {
            idleAnimationTime += dt;
            int frame = ((int)(idleAnimationTime / idleFrameDuration)) % idleSheet.getSize();
            idleSheet.setFrame(frame);
        } else if (walkSheet != null) {
            walkAnimationTime = 0f;
            walkSheet.setFrame(0);
        }

        if (tongueState == 1) {
            tongueProgress += dt * tongueSpeed;
            if (tongueProgress >= 1f) {
                tongueProgress = 1f;
                tongueState = 2;
            }
        } else if (tongueState == 2) {
            tongueProgress -= dt * tongueSpeed;
            if (tongueProgress <= 0f) {
                tongueProgress = 0f;
                tongueState = 0;
            }
        }
    }

    public Affine2 getFlip(boolean faceRight) {
        if (faceRight) {
            flipCache.setToScaling(1, 1);
        } else {
            flipCache.setToScaling(-1, 1);
        }
        return flipCache;
    }

    public SpriteSheet getActiveSheet(boolean isGrounded, float velocityX) {
        if (playingSpawn && spawnSheet != null) return spawnSheet;
        if ((playingPortal || portalFinished) && portalSheet != null) return portalSheet;
        if (playingPhoto && photoSheet != null) return photoSheet;
        if (playingJump && jumpSheet != null) return jumpSheet;
        if (playingDeathMelt && deathMeltSheet != null) return deathMeltSheet;
        if (walkSheet != null && isGrounded && Math.abs(velocityX) > 0.1f) return walkSheet;
        if (idleSheet != null) return idleSheet;
        return null;
    }

    public boolean isPlayingJump() { return playingJump; }

    public boolean isPlayingDeathMelt() { return playingDeathMelt; }

    public boolean isPlayingPhoto() { return playingPhoto; }

    public boolean isPlayingPortal() { return playingPortal; }

    public boolean hasFinishedPortalAnimation() { return portalFinished; }

    public boolean isPlayingSpawn() { return playingSpawn; }

    public boolean hasFinishedSpawnAnimation() { return spawnFinished; }

    public boolean isTongueActive() { return tongueState != 0; }

}
