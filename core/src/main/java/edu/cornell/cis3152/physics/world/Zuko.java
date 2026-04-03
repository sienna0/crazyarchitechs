package edu.cornell.cis3152.physics.world;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;

import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.SpriteSheet;
import edu.cornell.gdiac.graphics.Texture2D;
import edu.cornell.gdiac.math.Path2;
import edu.cornell.gdiac.math.PathFactory;
import edu.cornell.gdiac.physics2.*;

// TODO i do think we should refactor this file.. it's getting quite long
// TODO split animation and movement?

/**
 * Zuko's avatar for the platform game.
 *
 * An ObstacleSprite is a sprite (specifically a textured mesh) that is
 * connected to a obstacle. It is designed to be the same size as the
 * physics object, and it tracks the physics object, matching its position
 * and angle at all times.
 *
 * Note that unlike a traditional ObstacleSprite, this attaches some additional
 * information to the obstacle. In particular, we add a sensor fixture. This
  * sensor is used to prevent double-jumping. However, we only have one mesh,
 * the mesh for Zuko. The sensor is invisible and only shows up in debug mode.
 * While we could have made the fixture a separate obstacle, we want it to be a
 * simple fixture so that we can attach it to the obstacle WITHOUT using joints.
 */
public class Zuko extends ObstacleSprite {

    /** The initializing data (to avoid magic numbers) */
    private final JsonValue data;
    /** The width of Zuko's avatar */
    private float width;
    /** The height of Zuko's avatar */
    private float height;

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
    /** How long until we can shoot again */
    private int shootCooldown;
    /** Whether our feet are on the ground */
    private boolean isGrounded;
    /** The object type currently supporting Zuko's ground sensor */
    private GameObject currentPlatform;

    /** The outline of the sensor obstacle */
    private Path2 sensorOutline;
    /** The debug color for the sensor */
    private Color sensorColor;
    /** The name of the sensor fixture */
    private String sensorName;

    /** Cache for internal force calculations */
    private final Vector2 forceCache = new Vector2();
    /** Cache for the affine flip */
    private final Affine2 flipCache = new Affine2();

    /** The object currently under mouse/being aimed at */
    private GameObject currentTarget;

    /** Inventory of pictures that Zuko may use */
    private Inventory pictureInventory;

    private Camera camera;

    private static final float REDUCED_JUMP_MULTIPLIER = 0.5f;

    private boolean canJumpFull = true;
    private boolean onIce = false;

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
    private float jumpFrameDuration = 0.14f;
    /** Whether the animation is playing or not */
    private boolean playingJump = false;

    /** The SpriteSheet for Zuko's walk animation */
    private SpriteSheet walkSheet;
    /** The duration of the walk animation */
    private float walkAnimationTime = 0f;
    /** The duration of each walk frame */
    private float walkFrameDuration = 0.07f;

    private Texture baseTexture;

    /**
     * Returns the position of this character's obstacle
     *
     * @return the position of this character's obstacle.
     */
    public Vector2 getPosition() {
        return getObstacle().getPosition();
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
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
     * Returns the object type Zuko is currently standing on.
     *
     * @return the supporting object type, or null if airborne/non-object terrain
     */
    public GameObject getCurrentPlatform() {
        return currentPlatform;
    }

    /**
     * Sets the object type Zuko is currently standing on.
     *
     * @param platform the supporting object type, or null if airborne/non-object terrain
     */
    public void setCurrentPlatform(GameObject platform) {
        currentPlatform = platform;
        if (platform == null) {
            canJumpFull = true;
            onIce = false;
            return;
        }

        Quality effectiveTexture = platform.getEffectiveSurfaceQuality();
        // this controls whether or not we can jump on the current block
        // onIce is because i think we need to make ice MORE slippery which involves increasing velocity on ice but
        // TBD TODO
        switch (effectiveTexture) {
            case STICKY:
                canJumpFull = false;
                onIce = false;
                // System.out.println("honey");
                break;
            case SLIPPERY:
                canJumpFull = true;
                onIce = true;
                // System.out.println("ice");
                break;
            default:
                canJumpFull = true;
                onIce = false;
                // System.out.println("nothing");
                break;
        }
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
     * Returns the name of the ground sensor
     *
     * This is used by the ContactListener. Because we do not associate the
     * sensor with its own obstacle,
     *
     * @return the name of the ground sensor
     */
    public String getSensorName() {
        return sensorName;
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
     * returns the object currently under the mouse
     *
     * @return the object currently under the mouse
     */
    public GameObject getCurrentTarget() {
        return currentTarget;
    }

    public Camera getCamera() {
        return camera;
    }

    public Inventory getPictureInventory() {
        return pictureInventory;
    }
    /**
     * Sets the object currently under the mouse
     *
     * @param target the GameObject currently being targeted or null if none
     */
    public void setCurrentTarget(GameObject target) {
        this.currentTarget = target;
    }

    public void setBaseTexture(Texture texture) {
        baseTexture = texture;
        setTexture(texture);
    }


    /**
     * Starts the photo-taking process
     */
    public void startTakingPhoto(boolean shouldFaceRight) {
        faceRight = shouldFaceRight;
        startPhotoAnimation();
    }


    /**
     * Starts the photo-taking animation
     */
    private void startPhotoAnimation() {
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
     * Creates a new Zuko avatar with the given physics data
     *
     * The physics units are used to size the mesh relative to the physics
     * body. All other attributes are defined by the JSON file. Because of
     * transparency around the image file, the physics object will be slightly
     * thinner than the mesh in order to give a tighter hitbox.
     *
     * @param units     The physics units
     * @param data      The physics constants for Zuko
     */
    public Zuko(float units, JsonValue data) {
        this.data = data;
        JsonValue debugInfo = data.get("debug");

        float x = data.get("pos").getFloat(0);
        float y = data.get("pos").getFloat(1);
        float s = data.getFloat( "size" );
        float size = s*units;

        // The capsule is smaller than the image
        // "inner" is the fraction of the original size for the capsule
        width = s*data.get("inner").getFloat(0);
        height = s*data.get("inner").getFloat(1);
        obstacle = new CapsuleObstacle(x, y, width, height);
        ((CapsuleObstacle)obstacle).setTolerance( debugInfo.getFloat("tolerance", 0.5f) );

        obstacle.setDensity( data.getFloat( "density", 0 ) );
        obstacle.setFriction( data.getFloat( "friction", 0 ) );
        obstacle.setRestitution( data.getFloat( "restitution", 0 ) );
        obstacle.setFixedRotation(true);
        obstacle.setPhysicsUnits( units );
        obstacle.setUserData( this );
        obstacle.setName("Zuko");

        debug = ParserUtils.parseColor( debugInfo.get("avatar"),  Color.WHITE);
        sensorColor = ParserUtils.parseColor( debugInfo.get("sensor"),  Color.WHITE);

        maxspeed = data.getFloat("maxspeed", 0);
        damping = data.getFloat("damping", 0);
        force = data.getFloat("force", 0);
        jumpForce = data.getFloat( "jump_force", 0 );
        currentJumpForce = jumpForce;
        jumpLimit = data.getInt( "jump_cool", 0 );

        // Gameplay attributes
        isGrounded = false;
        isJumping = false;
        faceRight = true;
        currentPlatform = null;

        shootCooldown = 0;
        jumpCooldown = 0;

        //Camera attributes - you can put all of this in constants.json but I am scared of a merge conflict so O put it directly for now

        currentTarget = null;

        // Inventory
        pictureInventory = new Inventory(data);

        // Create a rectangular mesh for Zuko. Note that the capsule is
        // actually smaller than the image, making a tighter hitbox. You can
        // see this when you enable debug mode.
        mesh.set(-size/2.0f,-size/2.0f,size,size);

        camera = new Camera();
    }

    /**
     * Creates the sensor for Zuko.
     *
     * We only allow the Zuko to jump when she's on the ground. Double jumping
     * is not allowed.
     *
     * To determine whether Zuko is on the ground we create a thin sensor under
     * her feet, which reports collisions with the world but has no collision
     * response. This sensor is just a FIXTURE, it is not an obstacle. We will
     * talk about the different between these later.
     *
     * Note this method is not part of the constructor. It can only be called
     * once the physics obstacle has been activated.
     */
    public void createSensor() {
        Vector2 sensorCenter = new Vector2(0, -height / 2);
        FixtureDef sensorDef = new FixtureDef();
        sensorDef.density = data.getFloat("density",0);
        sensorDef.isSensor = true;

        JsonValue sensorjv = data.get("sensor");
        float w = sensorjv.getFloat("shrink",0)*width/2.0f;
        float h = sensorjv.getFloat("height",0);
        PolygonShape sensorShape = new PolygonShape();
        sensorShape.setAsBox(w, h, sensorCenter, 0.0f);
        sensorDef.shape = sensorShape;

        // Ground sensor to represent our feet
        Body body = obstacle.getBody();
        Fixture sensorFixture = body.createFixture( sensorDef );
        sensorName = "zuko_sensor";
        sensorFixture.setUserData(sensorName);

        // Finally, we need a debug outline
        float u = obstacle.getPhysicsUnits();
        PathFactory factory = new PathFactory();
        sensorOutline = new Path2();
        factory.makeRect( (sensorCenter.x-w/2)*u,(sensorCenter.y-h/2)*u, w*u, h*u,  sensorOutline);
    }

    /**
     * Applies the force to the body of Zuko
     *
     * This method should be called after the force attribute is set.
     */
    public void applyForce() {
        if (!obstacle.isActive()) {
            return;
        }

        Vector2 pos = obstacle.getPosition();
        float vx = obstacle.getVX();
        Body body = obstacle.getBody();

        // Don't want to be moving. Damp out player motion
        if (getMovement() == 0f) {
            forceCache.set(-getDamping()*vx,0);
            body.applyForce(forceCache,pos,true);
        }

        // TODO add an even higher velocity for ice maybe?
        // Velocity too high, clamp it
        if (!onIce && Math.abs(vx) >= getMaxSpeed()) {
            obstacle.setVX(Math.signum(vx)*getMaxSpeed());
        } else {
            forceCache.set(getMovement(),0);
            body.applyForce(forceCache,pos,true);
        }

        if (isJumping()) {
            forceCache.set(0, currentJumpForce);
            body.applyLinearImpulse(forceCache,pos,true);
        }
    }



    /**
     * Updates the object's physics state (NOT GAME LOGIC).
     *
     * We use this method to reset cooldowns.
     *
     * @param dt    Number of seconds since last animation frame
     */
    @Override
    public void update(float dt) {
        // Apply cooldowns
        if (isJumping()) {
            jumpCooldown = jumpLimit;
        } else {
            jumpCooldown = Math.max(0, jumpCooldown - 1);
        }

        if (playingPhoto && photoSheet != null) {
            photoAnimationTime += dt;
            int frame = (int)(photoAnimationTime / photoFrameDuration);
            if (frame >= photoSheet.getSize()) {
                playingPhoto = false;
                photoSheet.setFrame(0);
                if (baseTexture != null) {
                    setTexture(baseTexture);
                }
            } else {
                photoSheet.setFrame(frame);
            }
        } else if (playingJump && jumpSheet != null) {
            jumpAnimationTime += dt;
            int frame = (int)(jumpAnimationTime / jumpFrameDuration);
            if (frame >= jumpSheet.getSize()) {
                playingJump = false;
                jumpSheet.setFrame(0);
                if (baseTexture != null) {
                    setTexture(baseTexture);
                }
            } else {
                jumpSheet.setFrame(frame);
            }
        } else if (walkSheet != null && isGrounded && Math.abs(obstacle.getVX()) > 0.1f) {
            walkAnimationTime += dt;
            int frame = ((int)(walkAnimationTime / walkFrameDuration)) % walkSheet.getSize();
            walkSheet.setFrame(frame);
        } else if (walkSheet != null) {
            walkAnimationTime = 0f;
            walkSheet.setFrame(0);
        }

        camera.update(dt);

        super.update(dt);
    }

    /**
     * Draws the physics object.
     *
     * This method is overridden from ObstacleSprite. We need to flip the
     * texture back-and-forth depending on her facing. We do that by creating
     * a reflection affine transform.
     *
     * @param batch The game canvas to draw to
     */
    @Override
    public void draw(SpriteBatch batch) {
        if (faceRight) {
            flipCache.setToScaling( 1,1 );
        } else {
            flipCache.setToScaling( -1,1 );
        }
        if (playingPhoto && photoSheet != null) {
            setSpriteSheet(photoSheet);
        } else if (playingJump && jumpSheet != null) {
            setSpriteSheet(jumpSheet);
        } else if (walkSheet != null && isGrounded && Math.abs(obstacle.getVX()) > 0.1f) {
            setSpriteSheet(walkSheet);
        } else if (baseTexture != null) {
            setTexture(baseTexture);
        }

        super.draw(batch,flipCache);
    }

    /**
     * Draws the outline of the physics object.
     *
     * This method is overridden from ObstacleSprite. By default, that method
     * only draws the outline of the main physics obstacle. We also want to
     * draw the outline of the sensor, and in a different color. Since it
     * is not an obstacle, we have to draw that by hand.
     *
     * @param batch The sprite batch to draw to
     */
    public void drawDebug(SpriteBatch batch) {
         super.drawDebug(batch);

         if (sensorOutline != null) {
             batch.setTexture( Texture2D.getBlank() );
             batch.setColor( sensorColor );

            Vector2 p = obstacle.getPosition();
            float a = obstacle.getAngle();
            float u = obstacle.getPhysicsUnits();

            // transform is an inherited cache variable
            transform.idt();
            transform.preRotate( (float) (a * 180.0f / Math.PI) );
            transform.preTranslate( p.x * u, p.y * u );

            //
            batch.outline( sensorOutline, transform );
        }
    }
}
