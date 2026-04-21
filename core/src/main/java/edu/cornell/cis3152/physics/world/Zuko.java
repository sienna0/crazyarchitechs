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
 * The player-controlled frog avatar. She walks, jumps, and takes photos using her
 * {@link Camera} and {@link Inventory}.
 *
 * <p>Manages animation state (walk while moving on ground, jump sheet, photo sheet),
 * horizontal movement via Box2D forces, and which {@link GameObject}—if any—the foot
 * sensor reports as the current platform. Platform type and any picture stuck on it
 * affect jump height (honey reduces unless countered) and horizontal control (ice is slippery).</p>
 *
 * <p>Like any {@link ObstacleSprite}, the visible mesh tracks the physics obstacle.
 * A separate thin sensor fixture under the feet detects ground contact for jumping;
 * it is not its own obstacle and is drawn only in debug mode.</p>
 */
public class Zuko extends ObstacleSprite {

    /** The initializing data (to avoid magic numbers) */
    private final JsonValue data;
    /** The width of Zuko's avatar */
    private float width;
    /** The height of Zuko's avatar */
    private float height;

    private float drawSize;
    private float jumpDrawHeight;

    /** The object type currently supporting Zuko's ground sensor */
    private GameObject currentPlatform;

    /** The outline of the sensor obstacle */
    private Path2 sensorOutline;
    /** The debug color for the sensor */
    private Color sensorColor;
    /** The name of the sensor fixture */
    private String sensorName;

    /** The object currently under mouse/being aimed at */
    private GameObject currentTarget;

    /** Inventory of pictures that Zuko may use */
    private Inventory pictureInventory;

    private ZukoAnimator animator;

    private ZukoMovement movement;

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
     * Returns the object type Zuko is currently standing on.
     *
     * @return the supporting object type, or null if airborne/non-object terrain
     */
    public GameObject getCurrentPlatform() {
        return currentPlatform;
    }

    /**
     * Updates ground-contact context: {@code null} restores full jumps and clears ice.
     * Otherwise recomputes canJumpFull and onIce from the platform's
     * base {@link Obj} type and whether a honey/ice picture is applied.
     *
     * @param platform the object under the foot sensor, or null if not on a {@link GameObject}
     */
    public void setCurrentPlatform(GameObject platform) {
        currentPlatform = platform;
        movement.setCurrentPlatform(platform);
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
     * returns the object currently under the mouse
     *
     * @return the object currently under the mouse
     */
    public GameObject getCurrentTarget() {
        return currentTarget;
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

    /**
     * Starts Zuko's jump animation
     */
    public void startJumpAnimation() {
        animator.startJumpAnimation();
    }

    /**
     * Starts Zuko's death melt animation
     */
    public void startDeathMeltAnimation() {
        animator.startDeathMeltAnimation();
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
    public Zuko(float units, JsonValue data, float xStartingPos, float yStartingPos, JsonValue levelSettings) {
        this.data = data;
        JsonValue debugInfo = data.get("debug");

        float x = xStartingPos;
        float y = yStartingPos;
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
        obstacle.setName("zuko");

        debug = ParserUtils.parseColor( debugInfo.get("avatar"),  Color.WHITE);
        sensorColor = ParserUtils.parseColor( debugInfo.get("sensor"),  Color.WHITE);


        // Gameplay attributes

        currentPlatform = null;

        //Camera attributes - you can put all of this in constants.json but I am scared of a merge conflict so O put it directly for now

        currentTarget = null;

        // Inventory
        pictureInventory = new Inventory(data, levelSettings);

        // Create a rectangular mesh for Zuko. Note that the capsule is
        // actually smaller than the image, making a tighter hitbox. You can
        // see this when you enable debug mode.
        //mesh.set(-size/2.0f,-size/2.0f,size,size);
        drawSize = size;
        jumpDrawHeight = size * (20f / 16f);
        animator = new ZukoAnimator();
        movement = new ZukoMovement(data);
        mesh.set(-drawSize/2.0f, -drawSize/2.0f, drawSize, drawSize);

    }

    /**
     * After the capsule body exists in the world, adds a thin sensor fixture under the feet,
     * named {@link #sensorName}, so contact callbacks can set grounded state without physical
     * response. Also builds {@link #sensorOutline} for debug drawing. Must not run until
     * {@link #obstacle} is activated.
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
     * Integrates one movement step on the Box2D body: horizontal damping when input is idle,
     * otherwise drive with getMovement; caps horizontal speed unless onIce
     * (ice allows exceeding maxSpeed); applies currentJumpForce as an upward
     * impulse when {@link #isJumping()} is true. No-op if the obstacle is not active.
     */
    public void applyForce() {
        movement.applyForce(obstacle);
    }

    /**
     * Repositions the avatar without preserving any previous momentum.
     *
     * @param x target x-position in physics units
     * @param y target y-position in physics units
     */
    public void warpTo(float x, float y) {
        movement.warpTo(x,y, obstacle);
    }

    /**
     * Clears current linear and angular motion.
     */
    public void stopMotion() {
        movement.stopMotion(obstacle);
    }



    /**
     * Per-frame tick: adjusts jump cooldown, advances exactly one animation channel with
     * priority photo &gt; jump &gt; walk (walk only when grounded and moving), resets finished
     * sheets to frame 0 and restores  when appropriate, then updates
     * the {@link Camera}.
     *
     * @param dt seconds since the last frame
     */
    @Override
    public void update(float dt) {
        // Apply cooldowns
        movement.updateCooldown();
        animator.update(dt, movement.isGrounded(), obstacle.getVX());
        super.update(dt);
    }

    /**
     * Chooses the active sprite sheet (photo, jump, walk, or static )
     * consistent with {@link #update(float)}, applies a horizontal flip affine when
     * faceright is false, then delegates to {@code ObstacleSprite.draw}.
     *
     * @param batch destination batch
     */
    @Override
    public void draw(SpriteBatch batch) {
        SpriteSheet activeSheet = animator.getActiveSheet(movement.isGrounded(), obstacle.getVX());
        if (activeSheet != null) {
            setSpriteSheet(activeSheet);
        } else if (animator.getBaseTexture() != null) {
            setTexture(animator.getBaseTexture());
        }

        if (animator.isPlayingJump() && !animator.isPlayingPhoto() && !animator.isPlayingDeathMelt()) {
            float yOffset = (jumpDrawHeight - drawSize) / 2.0f;
            mesh.set(-drawSize/2.0f, -jumpDrawHeight/2.0f + yOffset, drawSize, jumpDrawHeight);
        } else {
            mesh.set(-drawSize/2.0f, -drawSize/2.0f, drawSize, drawSize);
        }
        super.draw(batch, animator.getFlip(movement.isFacingRight()));

        animator.drawTongue(batch, movement.isFacingRight(), obstacle.getX(), obstacle.getY(), obstacle.getPhysicsUnits());
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

    public void setBaseTexture(Texture texture) {
        animator.setBaseTexture(texture);
    }

    public void startTakingPhoto(boolean shouldFaceRight) {
        setFacingRight(shouldFaceRight);
        animator.startPhotoAnimation();
    }

    public void startTongueAnimation(float targetX, float targetY) {
        boolean facingRight = targetX > obstacle.getX();
        setFacingRight(facingRight);
        animator.startTongueAnimation(obstacle.getX(), obstacle.getY(), targetX, targetY, obstacle.getPhysicsUnits());
    }

    public void setPhotoAnimation(Texture sheet, int rows, int cols, int size) {
        animator.setPhotoAnimation(sheet, rows, cols, size);
    }

    public void setJumpAnimation(Texture sheet, int rows, int cols, int size) {
        animator.setJumpAnimation(sheet, rows, cols, size);
    }

    public void setWalkAnimation(Texture sheet, int rows, int cols, int size) {
        animator.setWalkAnimation(sheet, rows, cols, size);
    }

    public void setDeathMeltAnimation(Texture sheet, int rows, int cols, int size) {
        animator.setDeathMeltAnimation(sheet, rows, cols, size);
    }

    public void setTongueSegment(Texture texture) {
        animator.setTongueSegment(texture);
    }
    public void setMovement(float value) {
        movement.setMovement(value);
    }
    public boolean isJumping() {
        return movement.isJumping();
    }
    public void setJumping(boolean value) {
        movement.setJumping(value);
    }
    public void setGrounded(boolean value) {
        movement.setGrounded(value);
    }
    public float getForce() {
        return movement.getForce();
    }
    public void setFacingRight(boolean value) {
        movement.setFacingRight(value);
    }

}
