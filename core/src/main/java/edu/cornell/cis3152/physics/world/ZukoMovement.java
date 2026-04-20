package edu.cornell.cis3152.physics.world;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;

import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.physics2.*;

public class ZukoMovement {

    /** The factor to multiply by the input */
    private float force;
    /** The amount to slow the character down */
    private float damping;
    /** The maximum character speed */
    private float maxspeed;
    /** The impulse for a full-strength jump */
    private float jumpForce;
    /** The impulse for the current jump request */
    private float currentJumpForce;
    /** Cooldown (in animation frames) for jumping */
    private int jumpLimit;


    /** The current horizontal movement of the character */
    private float   movement;
    /** Which direction is the character facing */
    private boolean faceRight;
    /** How long until we can jump again */
    private int jumpCooldown;
    /** Whether we are actively jumping */
    private boolean isJumping;
    /** Whether our feet are on the ground */
    private boolean isGrounded;


    private boolean canJumpFull = true;
    private boolean onIce = false;

    private static final float REDUCED_JUMP_MULTIPLIER = 0.5f;

    /** Cache for internal force calculations */
    private final Vector2 forceCache = new Vector2();

    /**
     * Integrates one movement step on the Box2D body: horizontal damping when input is idle,
     * otherwise drive with {@link #getMovement()}; caps horizontal speed unless {@link #onIce}
     * (ice allows exceeding {@link #maxspeed}); applies {@link #currentJumpForce} as an upward
     * impulse when {@link #isJumping()} is true. No-op if the obstacle is not active.
     */
    public void applyForce(Obstacle obstacle) {
        if (!obstacle.isActive()) {
            return;
        }

        float maxspeed = canJumpFull ? getMaxSpeed() : getMaxSpeed()/2;

        Vector2 pos = obstacle.getPosition();
        float vx = obstacle.getVX();
        Body body = obstacle.getBody();

        // Preserve momentum on ice so slippery surfaces feel distinct.
        if (getMovement() == 0f && !onIce) {
            forceCache.set(-getDamping()*vx,0);
            body.applyForce(forceCache,pos,true);
        }

        float movement = getMovement();
        // At max speed, still apply horizontal force when braking or reversing; otherwise the
        // clamp below would skip input entirely and air / ground direction changes feel sluggish.
        boolean atSpeedCap = !onIce && Math.abs(vx) >= maxspeed;
        boolean inputOpposesVelocity =
                movement != 0f && Math.abs(vx) > 1e-4f && Math.signum(movement) != Math.signum(vx);

        if (atSpeedCap && !inputOpposesVelocity) {
            obstacle.setVX(Math.signum(vx) * maxspeed);
        } else {
            forceCache.set(movement, 0f);
            body.applyForce(forceCache, pos, true);
        }

        if (isJumping()) {
            forceCache.set(0, currentJumpForce);
            body.applyLinearImpulse(forceCache,pos,true);
        }
    }

    /**
     * Repositions the avatar without preserving any previous momentum.
     *
     * @param x target x-position in physics units
     * @param y target y-position in physics units
     */
    public void warpTo(float x, float y, Obstacle obstacle) {
        if (!obstacle.isActive()) {
            return;
        }

        Body body = obstacle.getBody();
        if (body != null) {
            body.setTransform(x, y, body.getAngle());
            body.setLinearVelocity(0f, 0f);
            body.setAngularVelocity(0f);
        }
    }

    /**
     * Clears current linear and angular motion.
     */
    public void stopMotion(Obstacle obstacle) {
        if (!obstacle.isActive()) {
            return;
        }

        Body body = obstacle.getBody();
        if (body != null) {
            body.setLinearVelocity(0f, 0f);
            body.setAngularVelocity(0f);
        }
    }

    /**
     * Returns the left/right movement of this character.
     *
     * This is the result of input times force.
     *
     * @return the left/right movement of this character.
     */
    public float getMovement() {
        return movement;
    }

    /**
     * Sets the left/right movement of this character.
     *
     * This is the result of input times force.
     *
     * @param value the left/right movement of this character.
     */
    public void setMovement(float value) {
        movement = value;
        // Change facing if appropriate
        if (movement < 0) {
            faceRight = false;
        } else if (movement > 0) {
            faceRight = true;
        }
    }

    /**
     * Returns true if Zuko is actively jumping.
     *
     * @return true if Zuko is actively jumping.
     */
    public boolean isJumping() {
        return isJumping && isGrounded && jumpCooldown <= 0;
    }

    /**
     * Sets whether Zuko is actively jumping.
     *
     * @param value whether Zuko is actively jumping.
     */
    public void setJumping(boolean value) {
        if (value) {
            isJumping = true;
            currentJumpForce = canJumpFull ? jumpForce : jumpForce * REDUCED_JUMP_MULTIPLIER;
            return;
        }
        isJumping = false;
    }

    /**
     * Returns true if Zuko is on the ground.
     *
     * @return true if Zuko is on the ground.
     */
    public boolean isGrounded() {
        return isGrounded;
    }

    /**
     * Sets whether Zuko is on the ground.
     *
     * @param value whether Zuko is on the ground.
     */
    public void setGrounded(boolean value) {
        isGrounded = value;
    }

    /**
     * Returns how much force to apply to get Zuko moving
     *
     * Multiply this by the input to get the movement value.
     *
     * @return how much force to apply to get Zuko moving
     */
    public float getForce() {
        return force;
    }

    /**
     * Returns how hard the brakes are applied to stop Zuko moving
     *
     * @return how hard the brakes are applied to stop Zuko moving
     */
    public float getDamping() {
        return damping;
    }

    public void setDamping(float value) {
        damping = value;
    }

    /**
     * Returns the upper limit on Zuko's left-right movement.
     *
     * This does NOT apply to vertical movement.
     *
     * @return the upper limit on Zuko's left-right movement.
     */
    public float getMaxSpeed() {
        return maxspeed;
    }

    /**
     * Returns true if this character is facing right
     *
     * @return true if this character is facing right
     */
    public boolean isFacingRight() {
        return faceRight;
    }

    /**
     * Forces the avatar to face the requested direction.
     *
     * @param value whether the avatar should face right
     */
    public void setFacingRight(boolean value) {
        faceRight = value;
    }

    public ZukoMovement(JsonValue data) {
        force = data.getFloat("force", 0);
        damping = data.getFloat("damping", 0);
        maxspeed = data.getFloat("maxspeed", 0);
        jumpForce = data.getFloat("jump_force", 0);
        currentJumpForce = jumpForce;
        jumpLimit = data.getInt("jump_cool", 0);
        isGrounded = false;
        isJumping = false;
        faceRight = true;
        jumpCooldown = 0;
    }

    /**
     * Updates ground-contact context: {@code null} restores full jumps and clears ice.
     * Otherwise recomputes {@link #canJumpFull} and {@link #onIce} from the platform's
     * base {@link Obj} type and whether a honey/ice picture is applied.
     *
     * @param platform the object under the foot sensor, or null if not on a {@link GameObject}
     */
    public void setCurrentPlatform(GameObject platform) {
        if (platform == null) {
            canJumpFull = true;
            onIce = false;
            return;
        }

        boolean baseIsHoney = platform.getObjectType() == Obj.HONEY;
        boolean baseIsIce = platform.getObjectType() == Obj.ICE;
        Quality pictureQuality = platform.getPictureQuality();
        boolean hasHoneyPicture = pictureQuality == Quality.STICKY && platform.hasPicture();
        boolean hasIcePicture = pictureQuality == Quality.SLIPPERY && platform.hasPicture();

        canJumpFull = (!baseIsHoney || hasIcePicture) && !hasHoneyPicture;
        onIce = (baseIsIce && !hasHoneyPicture) || hasIcePicture;
    }

    public void updateCooldown() {
        if (isJumping()) {
            jumpCooldown = jumpLimit;
        } else {
            jumpCooldown = Math.max(0, jumpCooldown - 1);
        }
    }
}
