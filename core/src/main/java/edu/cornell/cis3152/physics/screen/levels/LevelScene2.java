/*
 * PlatformScene.java
 *
 * This is the game scene (player mode) specific to the platforming mini-game.
 * You SHOULD NOT need to modify this file. However, you may learn valuable
 * lessons for the rest of the lab by looking at it.
 *
 * Based on the original PhysicsDemo Lab by Don Holden, 2007
 *
 * Author: Walker M. White
 * Version: 2/8/2025
 */
package edu.cornell.cis3152.physics.screen.levels;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.WeldJointDef;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.cis3152.physics.InputController;
import edu.cornell.cis3152.physics.screen.PhysicsScene;
import edu.cornell.cis3152.physics.world.*;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.audio.SoundEffect;
import edu.cornell.gdiac.audio.SoundEffectManager;
import edu.cornell.gdiac.graphics.TextLayout;
import edu.cornell.gdiac.math.Path2;
import edu.cornell.gdiac.math.PathFactory;
import edu.cornell.gdiac.physics2.Obstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;

/**
 * The game scene for the platformer game.
 *
 * Look at the method {@link #populateLevel} for how we initialize the scene.
 * Beyond that, a lot of work is done in the method for the ContactListener
 * interface. That is the method that is called upon collisions, giving us a
 * chance to define a response.
 */
public class LevelScene2 extends PhysicsScene implements ContactListener {
    /** Texture asset for character avatar */
    private TextureRegion avatarTexture;
    /** Texture asset for the spinning barrier */
    private TextureRegion barrierTexture;
    /** Texture asset for the bullet */
    private TextureRegion bulletTexture;
    /** Texture asset for the bridge plank */
    private TextureRegion bridgeTexture;

    private Texture cloudTexture;
    private Texture earthTexture;

    /** The jump sound. We only want to play once. */
    private SoundEffect jumpSound;
    /** The weapon fire sound. We only want to play once. */
    private SoundEffect fireSound;
    /** The weapon pop sound. We only want to play once. */
    private SoundEffect plopSound;
    /** The default sound volume */
    private float volume;

    /** Reference to the character avatar */
    private Zuko avatar;
    /** Reference to the goalDoor (for collision detection) */
    private Door goalDoor;

    //** Picture list */
    private Array<Picture> pictures = new Array<>();
    private Picture activePicture;

    private GameObject rock;
    private GameObject cloud;
    private GameObject ice;

    /** Flic vs stick toggle: flic == true, stick == false*/
    boolean flicStick;

    // Rock lift behavior (cloud photo on rock)
    private float rockLiftCeilingY;
    private boolean rockLiftActive;

    // Cloud behavior (rock photo on cloud)
    private boolean cloudDropActive;
    private boolean cloudReturnActive;
    private float cloudLiftCeilingY;

    // Ice interaction state
    private boolean iceOnCloudActive;
    private boolean iceOnRockActive;
    private boolean rockOnIceActive;
    private boolean cloudOnIceActive;

    // Levels
    private int currentLevel = 1;

    // Tuning constants
    private static final float ROCK_DENSITY = 5.0f;
    private static final float ROCK_FRICTION = 5.0f;

    private static final float CLOUD_BASE_DENSITY = 2.0f;
    private static final float CLOUD_BASE_FRICTION = 0.0f;

    private static final float CLOUD_LIFT_GRAVITY = -0.5f;

    private static final float ICE_DENSITY = 1.0f;
    private static final float ICE_FRICTION = 0.0f;
    private static final float ICE_RESTITUTION = 0.3f;
    private static final float ICE_SLIDE_DENSITY = 0.3f;
    private static final float ICE_BOUNCE_RESTITUTION = 2.5f;

    // Range Variables
    private float STICK_PICTURE_DISTANCE = 5.0f; //I know you wanted it to be 3 times less than take picture but, if you mistakenly take a picture of the rock, you would not be able to reach the cloud unless it is 2 times less
    private float TAKE_PICTURE_DISTANCE = 10.0f;
    private Array<GameObject> highlighted = new Array<>();
    private final Affine2 highlightTransform = new Affine2();
    private boolean showRange = false;
    /** Mark set to handle more sophisticated collision callbacks */
    protected ObjectSet<Fixture> sensorFixtures;

    private TextLayout cameraLabel;
    private OrthographicCamera textCamera;
    private BitmapFont font;
    /**
     * Creates and initialize a new instance of the platformer game
     *
     * The game has default gravity and other settings
     */
    public LevelScene2(AssetDirectory directory) {
        super(directory,"platform");
        world.setContactListener(this);
        sensorFixtures = new ObjectSet<Fixture>();

        // Pull out sounds
        jumpSound = directory.getEntry( "platform-jump", SoundEffect.class );
        fireSound = directory.getEntry( "platform-pew", SoundEffect.class );
        plopSound = directory.getEntry( "platform-plop", SoundEffect.class );
        volume = constants.getFloat("volume", 1.0f);

        font = new BitmapFont();
        cameraLabel = new TextLayout();
        cameraLabel.setFont( font );
    }

    /**
     * Resets the status of the game so that we can play again.
     *
     * This method disposes of the world and creates a new one.
     */
    public void reset() {
        JsonValue values = constants.get("world");
        Vector2 gravity = new Vector2(0, values.getFloat( "gravity" ));

        if (world != null) {
            for (ObstacleSprite sprite : sprites) {
                sprite.getObstacle().deactivatePhysics( world );
            }
        }
        sprites.clear();
        addQueue.clear();

        if (world != null) {
            Array<Body> bodies = new Array<>();
            world.getBodies(bodies);
            for(Body b : bodies) {
                world.destroyBody( b );
            }
        }

        if (world == null) {
            world = new World( gravity, false );
            world.setContactListener( this );
        }

        setComplete(false);
        setFailure(false);

        activePicture = null;
        if (pictures != null) {
            pictures.clear();
        }

        rockLiftActive = false;
        cloudDropActive = false;
        cloudReturnActive = false;
        iceOnCloudActive = false;
        iceOnRockActive = false;
        rockOnIceActive = false;
        cloudOnIceActive = false;

        populateLevel();
    }

    /**
     * Lays out the game geography.
     */
    private void populateLevel() {
        float units = height/bounds.height;

        // Add level goal
        Texture texture = directory.getEntry( "shared-goal", Texture.class );
        JsonValue goal = constants.get("goal");
        goalDoor = new Door(units, goal);
        goalDoor.setTexture( texture );
        goalDoor.getObstacle().setName("goal");
        addSprite(goalDoor);

        // Create ground pieces
        texture = directory.getEntry( "shared-earth", Texture.class );
        Texture earthTexture = texture;

        Surface wall;
        String wname = "wall";
        JsonValue walls = constants.get("level" + currentLevel).get("walls");
        JsonValue walljv = walls.get("positions");
        for (int ii = 0; ii < walljv.size; ii++) {
            wall = new Surface(walljv.get(ii).asFloatArray(), units, walls);
            wall.getObstacle().setName(wname+ii);
            wall.setTexture( texture );
            addSprite(wall);
        }

        Surface platform;
        String pname = "platform";
        JsonValue plats = constants.get("level" + currentLevel).get("platforms");
        JsonValue platjv = plats.get("positions");
        for (int ii = 0; ii < platjv.size; ii++) {
            platform = new Surface(platjv.get(ii).asFloatArray(), units, walls);
            platform.getObstacle().setName(pname+ii);
            platform.setTexture( texture );
            addSprite(platform);
        }

        // Create Traci
        texture = directory.getEntry( "platform-traci", Texture.class );
        avatar = new Zuko(units, constants.get("traci"));
        avatar.setTexture(texture);
        addSprite(avatar);
        avatar.createSensor();

        float rockSize = 1.5f;
        float cloudSize = 1.5f;
        float platformLeftX = 25.0f;
        float platformTopY = 10.0f;

        // Rock
        rock = new GameObject(
                Obj.ROCK, constants.get("rock"), units,
                platformLeftX - rockSize, 4.0f + rockSize / 2.0f,
                rockSize, rockSize,
                BodyDef.BodyType.DynamicBody,
                false
        );
        rock.getObstacle().setDensity(ROCK_DENSITY);
        rock.getObstacle().setFriction(ROCK_FRICTION);
        rock.getObstacle().setRestitution(0.0f);
        rock.setTexture(earthTexture);
        addSprite(rock);

        Texture cloudTexture = directory.getEntry( "cloud", Texture.class );
        // Cloud (normal mode = "floaty platform marker": gravity off, collides)
        cloud = new GameObject(
                Obj.CLOUD, constants.get("cloud"), units, 20.0f, platformTopY + 2.0f, cloudSize,
                cloudSize, BodyDef.BodyType.DynamicBody, false
        );
        cloud.getObstacle().setDensity(CLOUD_BASE_DENSITY);
        cloud.getObstacle().setFriction(CLOUD_BASE_FRICTION);
        cloud.getObstacle().setRestitution(0.0f);
        cloud.getObstacle().setGravityScale(0.0f);
        cloud.setTexture(cloudTexture);
        addSprite(cloud);

        float iceSize = 1.5f;
        Pixmap icePixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        icePixmap.setColor(0.7f, 0.78f, 0.85f, 1.0f);
        icePixmap.fill();
        Texture iceTexture = new Texture(icePixmap);
        icePixmap.dispose();

        ice = new GameObject(
                Obj.ICE, constants.get("ice"), units,
                15.0f, 4.0f + iceSize / 2.0f,
                iceSize, iceSize,
                BodyDef.BodyType.StaticBody, false
        );
        ice.getObstacle().setDensity(ICE_DENSITY);
        ice.getObstacle().setFriction(ICE_FRICTION);
        ice.getObstacle().setRestitution(ICE_RESTITUTION);
        ice.setTexture(iceTexture);
        addSprite(ice);

        rockLiftCeilingY = bounds.height - rockSize / 2.0f;

        cloudLiftCeilingY = cloud.getObstacle().getY();
        cloudDropActive = false;
        cloudReturnActive = false;
    }

    public boolean preUpdate(float dt) {
        if (!super.preUpdate(dt)) {
            return false;
        }

        if (!isFailure() && avatar.getObstacle().getY() < -1) {
            setFailure(true);
            return false;
        }
        return true;
    }

    public void update(float dt) {

        InputController input = InputController.getInstance();
        findObjectNearZuko();

        // FIXME removed the dropped photo -> need to use inventory and scroll instead

        // Process actions in object model
        avatar.setMovement(input.getHorizontal() * avatar.getForce());
        avatar.setJumping(input.didPrimary());

        if (input.didToggleRange()) {
            showRange = !showRange;
        }
        // This should not be like this imo
        if (input.didRegCamera()) {
            avatar.getCamera().setCameraType(CameraType.REGULAR);
        }
        if (input.didTherCamera()) {
            avatar.getCamera().setCameraType(CameraType.THERMAL);
        }
        if (input.didTexCamera()) {
            avatar.getCamera().setCameraType(CameraType.TEXTURE);
        }
        if (input.didCycleCamera()) {
            avatar.getCamera().cycleCameraType();
        }

        Vector2 mouse = input.getCrossHair();
        GameObject target = findObjectUnderMouse(mouse.x, mouse.y);
        avatar.setCurrentTarget(target);

        // Move to correct spot in inventory
        // FIXME drop button is used to cycle pictures, should rename
        if (input.didDropPhoto()){
            avatar.getPictureInventory().nextCurrPic();
        }
        // Toggle flic vs stick. (flic == true, stick == false)
        if (input.didFlicStickToggle()){
            flicStick = !flicStick;
        }

        // Left click
        if (input.didLeftClick()){
            if (target != null) {
                SoundEffectManager sounds = SoundEffectManager.getInstance();
                float picVolume = Math.min(1.0f, volume * 1.75f);
                // In flic mode?
                if (flicStick) {
                    // Check avatar can take picture
                    if (avatar.getCamera().canTakePicture(target.getObstacle().getX(),
                            target.getObstacle().getY(),
                            avatar.getObstacle().getX(), avatar.getObstacle().getY())) {
                        Picture currPic = avatar.getPictureInventory().getCurrPicture();
                        // Check if this picture is unused
                        if (!currPic.hasSubject()){
                            currPic.setSubject(target,avatar.getCamera().getCameraType());
                            sounds.play("plop", plopSound, picVolume);
                        }
                    }
                } else { // stick mode (or unstick)
                    Picture currPic = avatar.getPictureInventory().getCurrPicture();
                    // If the picture is already on the target, then unstick it
                    if (currPic.getTarget() == target){
                        sprites.remove(currPic);
                    }
                    // Make sure the picture has a subject and there's not already a picture on the object
                    else if (currPic.hasSubject() && !avatar.getPictureInventory().hasPicture(target)){
                        currPic.setTarget(target);
                        sprites.add(currPic);
                    }
                }
            }

            // FIXME doesn't have an alternative to the manual rock lift stuff

            avatar.applyForce();
            if (avatar.isJumping()) {
                SoundEffectManager sounds = SoundEffectManager.getInstance();
                sounds.play("jump", jumpSound, volume);
            }


        }

    }

    /**
     * Callback method for the start of a collision
     */
    public void beginContact(Contact contact) {
        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();

        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        Object fd1 = fix1.getUserData();
        Object fd2 = fix2.getUserData();

        try {
            ObstacleSprite bd1 = (ObstacleSprite)body1.getUserData();
            ObstacleSprite bd2 = (ObstacleSprite)body2.getUserData();

            // Test bullet collision with world
            if (bd1.getName().equals("bullet") && bd2 != avatar && !bd2.getName().equals( "goal" )) {
                removeBullet(bd1);
            }
            if (bd2.getName().equals("bullet") && bd1 != avatar && !bd1.getName().equals( "goal" )) {
                removeBullet(bd2);
            }

            // See if we have landed on the ground.
            if ((avatar.getSensorName().equals(fd2) && avatar != bd1) ||
                    (avatar.getSensorName().equals(fd1) && avatar != bd2)) {
                avatar.setGrounded(true);
                sensorFixtures.add(avatar == bd1 ? fix2 : fix1);
            }

            // Check for win condition
            if ((bd1 == avatar && bd2.getName().equals( "goal" )) ||
                    (bd1.getName().equals("goal") && bd2 == avatar)) {
                setComplete(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Callback method for when two objects cease to touch.
     */
    public void endContact(Contact contact) {
        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();

        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        Object fd1 = fix1.getUserData();
        Object fd2 = fix2.getUserData();

        Object bd1 = body1.getUserData();
        Object bd2 = body2.getUserData();

        if ((avatar.getSensorName().equals(fd2) && avatar != bd1) ||
                (avatar.getSensorName().equals(fd1) && avatar != bd2)) {
            sensorFixtures.remove(avatar == bd1 ? fix2 : fix1);
            if (sensorFixtures.size == 0) {
                avatar.setGrounded(false);
            }
        }
    }

    /** Unused ContactListener method */
    public void postSolve(Contact contact, ContactImpulse impulse) {}
    /** Unused ContactListener method */
    public void preSolve(Contact contact, Manifold oldManifold) {}

    public void pause() {
        SoundEffectManager sounds = SoundEffectManager.getInstance();
        sounds.stop("plop");
        sounds.stop("fire");
        sounds.stop("jump");
    }

    /**
     * Removes a bullet from the world.
     */
    public void removeBullet(ObstacleSprite bullet) {
        bullet.getObstacle().markRemoved(true);
        SoundEffectManager sounds = SoundEffectManager.getInstance();
        sounds.play("plop", plopSound, volume);
    }

    /**
     * Returns the GameObject under the mouse position, or null if there is none
     */
    private GameObject findObjectUnderMouse(float mouseX, float mouseY) {
        for (ObstacleSprite sprite : sprites ) {
            if (sprite == avatar) continue;
            if (!(sprite instanceof GameObject)) continue;

            GameObject go = (GameObject) sprite;
            Obstacle obj = go.getObstacle();
            float u = obj.getPhysicsUnits();

            Rectangle bounds = sprite.getMesh().computeBounds();
            float centerX = obj.getX() * u;
            float centerY = obj.getY() * u;

            float minX = centerX + bounds.x;
            float minY = centerY + bounds.y;
            float maxX = minX + bounds.width;
            float maxY = minY + bounds.height;

            float posX = mouseX * u;
            float posY = mouseY * u;

            if (posX >= minX && posX <= maxX && posY >= minY && posY <= maxY) {
                return go;
            }
        }
        return null;
    }

    private void findObjectNearZuko() {
        highlighted.clear();
        float range = (activePicture != null) ? STICK_PICTURE_DISTANCE: TAKE_PICTURE_DISTANCE;
        for (ObstacleSprite sprite : sprites) {
            if (sprite == avatar) continue;
            if (!(sprite instanceof GameObject go)) continue;
            float x = go.getObstacle().getX();
            float y = go.getObstacle().getY();
            if (avatar.getCamera().hasLineOfSight(x, y, avatar.getObstacle().getX(), avatar.getObstacle().getY(), range)) {
                highlighted.add(go);
            }

        }
    }
    @Override
    public void draw(float dt) {
        super.draw(dt);

        viewport.apply();
        batch.begin(camera);
        Color highlighter = (activePicture != null) ? Color.LIME : Color.CORAL;
        batch.setColor(highlighter);

        for (GameObject go : highlighted) {
            Obstacle obj = go.getObstacle();
            float u = obj.getPhysicsUnits();
            float a = obj.getAngle();
            Vector2 p = obj.getPosition();

            highlightTransform.idt();
            highlightTransform.preRotate((float)(a * 180.0f/ Math.PI));
            highlightTransform.preTranslate(p.x * u, p.y * u);

            batch.outline(obj.getOutline(), highlightTransform);
        }
        if (showRange) {
            Obstacle obj = avatar.getObstacle();
            Vector2 p = obj.getPosition();
            float u = obj.getPhysicsUnits();
            float cx = p.x * u;
            float cy = p.y * u;
            float dashSize = 20f;
            float gapSize = 10f;
            float total = dashSize + gapSize;
            PathFactory factory = new PathFactory();

            highlightTransform.idt();
            batch.setColor(Color.LIME);
            for (float angle = 0; angle < 360; angle += total) {
                Path2 stickArc = factory.makeArc(cx,cy, (STICK_PICTURE_DISTANCE * u * 2) - 1, angle, dashSize, false);
                batch.outline(stickArc, highlightTransform);
                Path2 stickArc2 = factory.makeArc(cx,cy, (STICK_PICTURE_DISTANCE * u * 2) , angle, dashSize, false);
                batch.outline(stickArc2, highlightTransform);
                Path2 stickArc3 = factory.makeArc(cx,cy, (STICK_PICTURE_DISTANCE * u * 2) -2, angle, dashSize, false);
                batch.outline(stickArc3, highlightTransform);
            }
            batch.setColor(Color.CORAL);
            for (float angle = 0; angle < 360; angle += total) {
                Path2 takeArc = factory.makeArc(cx,cy, (TAKE_PICTURE_DISTANCE * u * 2) - 1, angle, dashSize, false);
                batch.outline(takeArc, highlightTransform);
                Path2 takeArc2 = factory.makeArc(cx,cy, (TAKE_PICTURE_DISTANCE * u  * 2), angle, dashSize, false);
                batch.outline(takeArc2, highlightTransform);
                Path2 takeArc3 = factory.makeArc(cx,cy, (TAKE_PICTURE_DISTANCE * u * 2) - 2, angle, dashSize, false);
                batch.outline(takeArc3, highlightTransform);

            }
        }

        batch.end();
        viewport.reset();

        String label = avatar.getCamera().getCameraType().getLabel();

        batch.end();
        viewport.reset();

        cameraLabel.setText(label);

        viewport.apply();
        batch.begin(textCamera);
        batch.setColor(Color.WHITE);
        batch.drawText(cameraLabel, 50, viewport.getHeight()-20);
        batch.end();
        viewport.reset();
    }
}
