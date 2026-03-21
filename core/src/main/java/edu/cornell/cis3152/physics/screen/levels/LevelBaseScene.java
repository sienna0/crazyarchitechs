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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
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
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.cis3152.physics.InputController;
import edu.cornell.cis3152.physics.screen.PhysicsScene;
import edu.cornell.cis3152.physics.world.*;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.audio.SoundEffect;
import edu.cornell.gdiac.audio.SoundEffectManager;
import edu.cornell.gdiac.graphics.SpriteBatch;
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
public class LevelBaseScene extends PhysicsScene implements ContactListener {
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
    private Texture backgroundTexture;

    private Texture slotTexture;

    private int selectedSlotIndex = -1;

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
    private float cloudHomeY;

    // Levels
    private int currentLevel = 1;
    // Range Variables
    private float STICK_PICTURE_DISTANCE = 9.0f; //I know you wanted it to be 3 times less than take picture but, if you mistakenly take a picture of the rock, you would not be able to reach the cloud unless it is 2 times less
    private float TAKE_PICTURE_DISTANCE = 9.0f;
    private Array<GameObject> highlighted = new Array<>();
    private final Affine2 highlightTransform = new Affine2();
    private boolean showRange = false;
    private static final float LIFT_SPRING_STIFFNESS = 6.0f;
    private static final float LIFT_SPRING_DAMPING = 3.5f;
    private static final float STUCK_PICTURE_SIZE = 22.0f;
    private static final float STUCK_PICTURE_INSET = 8.0f;
    private static final float STUCK_PICTURE_BORDER = 2.0f;
    private static final float STUCK_PICTURE_INNER_PADDING = 4.0f;
    /** Mark set to handle more sophisticated collision callbacks */
    protected ObjectSet<Fixture> sensorFixtures;

    private TextLayout cameraLabel;
    private OrthographicCamera textCamera;
    private BitmapFont font;
    private Texture markerPixel;

    /**
     * Resolves a texture from the asset directory, with a direct file fallback for
     * cases where the manifest key is temporarily out of sync.
     */
    private Texture requireTexture(String key, String fallbackPath) {
        Texture texture = directory.getEntry(key, Texture.class);
        if (texture == null) {
            texture = new Texture(Gdx.files.internal(fallbackPath));
        }
        return texture;
    }

    /**
     * Creates and initialize a new instance of the platformer game
     *
     * The game has default gravity and other settings
     */
    public LevelBaseScene(AssetDirectory directory) {
        super(directory,"platform");
        world.setContactListener(this);
        sensorFixtures = new ObjectSet<Fixture>();

        // Pull out sounds
        jumpSound = directory.getEntry( "platform-jump", SoundEffect.class );
        fireSound = directory.getEntry( "platform-pew", SoundEffect.class );
        plopSound = directory.getEntry( "platform-plop", SoundEffect.class );
        volume = constants.getFloat("volume", 1.0f);
        backgroundTexture = requireTexture("shared-background", "shared/background.png");

        textCamera = new OrthographicCamera();
        textCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        font = new BitmapFont();
        cameraLabel = new TextLayout();
        cameraLabel.setFont( font );

        Pixmap markerPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        markerPixmap.setColor(Color.WHITE);
        markerPixmap.fill();
        markerPixel = new Texture(markerPixmap);
        markerPixmap.dispose();
    }

    @Override
    protected void drawBackground(SpriteBatch batch) {
        if (backgroundTexture != null) {
            batch.setColor(Color.WHITE);
            batch.draw(backgroundTexture, 0, 0, viewport.getWidth(), viewport.getHeight());
        }
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

        populateLevel();
    }

    /**
     * Lays out the game geography.
     */
    private void populateLevel() {

        float units = height/bounds.height;

        // Add level goal
        Texture texture = requireTexture("shared-goal", "shared/goaldoor.png");

        if (constants.get("level" + currentLevel) == null)
        {
            currentLevel = 1;
        }

        JsonValue level = constants.get("level" + currentLevel);
        System.out.println(currentLevel);
        System.out.println("bing");
//        System.out.println("Level: " + level);
//
//        JsonValue objects = level.get("objectLocations");
//        System.out.println("Objects: " + objects);
//
//        JsonValue goal1 = objects.get("goal");
//        System.out.println("Goal: " + goal1);

        System.out.println("Level:" + constants.get("level" + currentLevel) + "; Constants: " + constants);
        JsonValue goal = constants.get("level" + currentLevel).get("objectLocations").get("goal");
        goalDoor = new Door(units, goal);
        goalDoor.setTexture( texture );
        goalDoor.getObstacle().setName("goal");
        addSprite(goalDoor);

        // Create ground pieces
        texture = requireTexture("shared-earth", "shared/earthtile.png");
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

        // Create Zuko
        texture = requireTexture("platform-traci", "platform/traci.png");
        avatar = new Zuko(units, constants.get("level" + currentLevel).get("objectLocations").get("zuko"));
        avatar.setTexture(texture);
        avatar.setBaseTexture(texture);
        addSprite(avatar);
        avatar.createSensor();
        Texture photoSheet = requireTexture("platform-camera", "platform/cameraflash.png");
        avatar.setPhotoAnimation(photoSheet, 1, 17, 17);
        Texture jumpSheet = requireTexture("platform-jump", "platform/zukojump.png");
        avatar.setJumpAnimation(jumpSheet, 1, 7, 7);

        float rockSize = 1.5f;
        float cloudSize = 1.5f;
//        float platformLeftX = 25.0f;
//        float platformTopY = 10.0f;

        float objWidth = 1.5f;

        Texture rockTexture = requireTexture("platform-rock", "platform/rock.png");
        float rockHeight = objWidth * ((float) rockTexture.getHeight() / rockTexture.getWidth());

        JsonValue rocks = constants.get("level"+currentLevel).get("objectLocations");
        JsonValue rockjv = rocks.get("rock");
        for (int ii = 0;  ii < rockjv.size; ii++){
            float [] rockPositions = rockjv.get(ii).asFloatArray();
            rock = new GameObject(
                    Obj.ROCK, constants.get("rock"), units,
                    rockPositions[0], rockPositions[1],
                    objWidth, rockHeight,
                    BodyDef.BodyType.DynamicBody,
                    false
            );
            rock.setTexture(rockTexture);
            addSprite(rock);
        }
        Texture iceTexture = requireTexture("platform-ice", "platform/ice.png");
        float iceHeight = objWidth * ((float) iceTexture.getHeight() / iceTexture.getWidth());

        JsonValue ices = constants.get("level"+currentLevel).get("objectLocations");
        JsonValue icejv = ices.get("ice");
        for (int ii = 0;  ii < icejv.size; ii++){
            float [] icePosition = icejv.get(ii).asFloatArray();
            ice = new GameObject(
                    Obj.ICE, constants.get("ice"), units,
                    icePosition[0], icePosition[1],
                    objWidth, iceHeight,
                    BodyDef.BodyType.DynamicBody,
                    false
            );
            ice.setTexture(iceTexture);
            addSprite(ice);
        }

        Texture cloudTexture = requireTexture("platform-cloud", "platform/cloud.png");

        JsonValue clouds = constants.get("level"+currentLevel).get("objectLocations");
        JsonValue cloudjv = clouds.get("cloud");
        for (int ii = 0;  ii < cloudjv.size; ii++){
            float [] cloudPositions = cloudjv.get(ii).asFloatArray();
            cloud = new GameObject(
                    Obj.CLOUD, constants.get("cloud"), units,
                    cloudPositions[0], cloudPositions[1],
                    cloudSize, cloudSize,
                    BodyDef.BodyType.DynamicBody,
                    false
            );
            cloud.setTexture(cloudTexture);
            addSprite(cloud);
            // FIXME I'm assuming setting cloudHomeY over and over will break this
            cloudHomeY = cloud.getObstacle().getY();
        }

    }

    /**
     * Switches level geometry from one level to the next
     */
    public void setLevel(int level)
    {
        if (constants.get("level" + level) != null)
        {
            currentLevel = level;
        }
        else
        {
            currentLevel = 1;
        }
        reset();
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
        handlePictureShortcuts(input);
        updateCameraSelection(input);
        updateAvatarMovement(input);

        GameObject target = resolveCurrentTarget(input);
        handlePictureAction(input, target);

        if (avatar.getCamera().isPictureTaken()) {
            avatar.getCamera().clearPictureTaken();
        }

        applyLiftSprings();
        avatar.applyForce();
        if (avatar.isJumping()) {
            SoundEffectManager sounds = SoundEffectManager.getInstance();
            sounds.play("jump", jumpSound, volume);
            avatar.startJumpAnimation();

        }
    }

    private void handlePictureShortcuts(InputController input) {
        if (input.didDropPhoto()) {
            if (activePicture != null)
            {
                Picture p = avatar.getPictureInventory().getPicture(selectedSlotIndex);
                if (p != null) {
                    p.clearSubject();
                }

                pictures.removeValue(p, true);

                selectedSlotIndex = -1;
                activePicture = null;
            }

//            pictures.clear();
//            avatar.getPictureInventory().reset();

//            for (int i = 0; i < avatar.getPictureInventory().getSize(); i++) {
//                Picture p = avatar.getPictureInventory().getPicture(i);
//                if (p != null) {
//                    p.clearSubject();
//                }
//            }
        }
        if (input.didToggleRange()) {
            showRange = !showRange;
        }
    }

    private void updateCameraSelection(InputController input) {
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
    }

    private void updateAvatarMovement(InputController input) {
        avatar.setMovement(input.getHorizontal() * avatar.getForce());
        avatar.setJumping(input.didPrimary());
    }

    private GameObject resolveCurrentTarget(InputController input) {
        Vector2 mouse = input.getCrossHair();
        GameObject target = findObjectUnderMouse(mouse.x, mouse.y);
        avatar.setCurrentTarget(target);
        return target;
    }

    private void handlePictureAction(InputController input, GameObject target) {
        if (input.didLeftClick()) {
            Vector2 mouse = input.getCrossHair();
            float u = avatar.getObstacle().getPhysicsUnits();
            int clickedSlot = getClickedSlot(mouse.x * u, mouse.y * u);

            if (clickedSlot >= 0) {
                Picture slotPicture = avatar.getPictureInventory().getPicture(clickedSlot);
                if (slotPicture != null && slotPicture.hasSubject())
                {
                    // If we have no picture selected, select a picture.
                    if (activePicture == null) {
                        selectedSlotIndex = clickedSlot;
                        activePicture = slotPicture;
                    }
                    // If we have a picture selected, select a new picture or deselect the old one.
                    else
                    {
                        if (clickedSlot != selectedSlotIndex)
                        {
                            selectedSlotIndex = clickedSlot;
                            activePicture = slotPicture;
                        }
                        else
                        {
                            selectedSlotIndex = -1;
                            activePicture = null;
                        }
                    }
                }

                return;
            }
        }

        if (target == null) {
            return;
        }

        if (input.didRightClick()) {
            removePictureFromTarget(target);
            return;
        }

        if (!input.didLeftClick()) {
            return;
        }

        if (activePicture == null || selectedSlotIndex == -1) {
            takePictureOfTarget(input, target);
            return;
        }

        applyPictureToTarget(target);
    }

    private void takePictureOfTarget(InputController input, GameObject target) {
        if (!avatar.getCamera().canTakePicture(
                target.getObstacle().getX(),
                target.getObstacle().getY(),
                avatar.getObstacle().getX(),
                avatar.getObstacle().getY())) {
            return;
        }

        if (avatar.getPictureInventory().getUnusedPicture() == null) {
            return;
        }

        avatar.getCamera().takePicture();
        Vector2 mousePosition = input.getCrossHair();
        Vector2 avatarPosition = avatar.getPosition();
        boolean shouldFaceRight = 0 < mousePosition.x - avatarPosition.x;
        avatar.startTakingPhoto(shouldFaceRight);
        Picture picture = new Picture(target, avatar.getCamera().getCameraType());
//        pictures.clear();
        pictures.add(picture);
//        activePicture = picture;
        avatar.getPictureInventory().addPicture(picture);

        SoundEffectManager sounds = SoundEffectManager.getInstance();
        float picVolume = Math.min(1.0f, volume * 1.75f);
        sounds.play("plop", plopSound, picVolume);
    }

    private void applyPictureToTarget(GameObject target) {
        if (activePicture.getSubject() == null) {
            activePicture = null;
            return;
        }
        if (activePicture.getSubject() == target) {
            return;
        }
        if (!avatar.getCamera().hasLineOfSight(
                target.getObstacle().getX(),
                target.getObstacle().getY(),
                avatar.getObstacle().getX(),
                avatar.getObstacle().getY(),
                STICK_PICTURE_DISTANCE)) {
            return;
        }

        if (activePicture.getTarget() != null) {
            activePicture.getTarget().resetAttributes();
        }

        activePicture.setTarget(target, height / bounds.height);

        for (int i = 0; i < avatar.getPictureInventory().getSize(); i++) {
            Picture picture = avatar.getPictureInventory().getPicture(i);
            if (picture != null && picture.hasSubject() && picture.getSubject() == activePicture.getSubject()) {
                picture.clearSubject();
                activePicture = null;
                selectedSlotIndex = -1;
                break;
            }
        }

        SoundEffectManager sounds = SoundEffectManager.getInstance();
        sounds.play("fire", fireSound, volume);
    }

    private void removePictureFromTarget(GameObject target) {
        Picture attachedPicture = findPictureOnTarget(target);
        if (attachedPicture == null) {
            return;
        }

        target.resetAttributes();
        attachedPicture.clearTarget();
        if (activePicture == attachedPicture) {
            activePicture = attachedPicture;
        }

        SoundEffectManager sounds = SoundEffectManager.getInstance();
        sounds.play("plop", plopSound, volume);
    }

    private Picture findPictureOnTarget(GameObject target) {
        for (Picture picture : pictures) {
            if (picture.getTarget() == target) {
                return picture;
            }
        }
        return null;
    }

    private void applyLiftSprings() {
        for (ObstacleSprite sprite : sprites) {
            if (!(sprite instanceof GameObject gameObject)) {
                continue;
            }
            boolean springActive = gameObject == cloud
                    ? gameObject.getGravityScale() <= 0.0f
                    : gameObject.hasLiftPicture();
            if (!springActive) {
                continue;
            }

            Body body = gameObject.getObstacle().getBody();
            if (body == null) {
                continue;
            }

            float displacement = cloudHomeY - body.getPosition().y;
            float damping = -LIFT_SPRING_DAMPING * body.getLinearVelocity().y;
            float springForce = (LIFT_SPRING_STIFFNESS * displacement) + damping;
            body.applyForceToCenter(0.0f, body.getMass() * springForce, true);
            body.setAngularVelocity(0.0f);
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

        if (avatar.getPictureInventory().getUnusedPicture() == null && selectedSlotIndex == -1) {
            return;
        }
        
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

    private void drawPlacedPictures() {
        for (Picture picture : pictures) {
            if (picture.getTarget() == null) {
                continue;
            }

            GameObject target = picture.getTarget();
            Obstacle obstacle = target.getObstacle();
            float units = obstacle.getPhysicsUnits();
            float centerX = obstacle.getX() * units;
            float centerY = obstacle.getY() * units;

            float angle = obstacle.getAngle();
            float rotation = angle * MathUtils.radiansToDegrees;

            drawStuckPictureLayer(centerX, centerY, STUCK_PICTURE_SIZE + (STUCK_PICTURE_BORDER * 2.0f), Color.BLACK, rotation);
            drawStuckPictureLayer(centerX, centerY, STUCK_PICTURE_SIZE, new Color(0.96f, 0.95f, 0.91f, 1.0f), rotation);
            drawStuckPictureLayer(centerX, centerY, STUCK_PICTURE_SIZE - (STUCK_PICTURE_INNER_PADDING * 2.0f), picture.getColor(), rotation);

            Texture subjectTex = picture.getTexture();
            if (subjectTex != null) {
                float texSize = STUCK_PICTURE_SIZE - (STUCK_PICTURE_INNER_PADDING * 2.0f) - 4.0f;
                batch.setColor(Color.WHITE);
                batch.draw(
                        subjectTex,
                        centerX - (texSize * 0.5f),
                        centerY - (texSize * 0.5f),
                        texSize * 0.5f,
                        texSize * 0.5f,
                        texSize,
                        texSize,
                        1.0f,
                        1.0f,
                        rotation,
                        0, 0,
                        subjectTex.getWidth(),
                        subjectTex.getHeight(),
                        false, false
                );
            }
        }
    }

    private void drawStuckPictureLayer(float centerX, float centerY, float size, Color color, float rotation) {
        batch.setColor(color);
        batch.draw(
                markerPixel,
                centerX - (size * 0.5f),
                centerY - (size * 0.5f),
                size * 0.5f,
                size * 0.5f,
                size,
                size,
                1.0f,
                1.0f,
                rotation,
                0,
                0,
                1,
                1,
                false,
                false
        );
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
            for (int t = -2; t <= 2; t++) {
                highlightTransform.idt();
                highlightTransform.preRotate((float)(a * 180.0f/ Math.PI));
                highlightTransform.preTranslate(p.x * u + t, p.y * u);
                batch.outline(obj.getOutline(), highlightTransform);
            }
            for (int t = -2; t <= 2; t++) {
                highlightTransform.idt();
                highlightTransform.preRotate((float)(a * 180.0f/ Math.PI));
                highlightTransform.preTranslate(p.x * u, p.y * u + t);
                batch.outline(obj.getOutline(), highlightTransform);
            }
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

        drawPlacedPictures();

        batch.end();
        viewport.reset();

        String label = avatar.getCamera().getCameraType().getLabel();



        cameraLabel.setText(label);

        viewport.apply();
        batch.begin(textCamera);
        batch.setColor(Color.WHITE);
        batch.drawText(cameraLabel, 50, viewport.getHeight()-20);
        batch.end();
        viewport.reset();

        viewport.apply();
        batch.begin(camera);
        batch.setColor(new Color(0.3f, 0.3f, 0.3f, 0.8f));
        batch.draw(slotTexture, viewport.getWidth()/2 - 200, 0, 400, 80);
        drawInventory();
        batch.setColor(Color.WHITE);
        batch.end();
        viewport.reset();
    }

    private void drawInventory() {
        float barWidth = 400f;
        float barHeight = 80f;
        float barX = viewport.getWidth() / 2 - barWidth / 2;
        float barY = 0f;
        float padding = 10f;
        int size = avatar.getPictureInventory().getSize();

        float slotSize = (barWidth - padding * (size + 1)) / size;
        float startX = barX + padding;
        float startY = barY + (barHeight - slotSize) / 2f;

        float selectedRaise = 12f;

        for (int i = 0; i < size; i++) {
            float slotX = startX + i * (slotSize + padding);
            float slotY = (i == selectedSlotIndex) ? startY + selectedRaise : startY;
            Picture picture = avatar.getPictureInventory().getPicture(i);

            batch.setColor(Color.GRAY);
            batch.draw(slotTexture, slotX, slotY, slotSize, slotSize);

            if (picture != null && picture.hasSubject()) {
                batch.setColor(picture.getColor());
                batch.draw(slotTexture, slotX, slotY, slotSize, slotSize);
                batch.setColor(Color.WHITE);
                batch.draw(picture.getSubject().getTexture(), slotX + 5f, slotY + 5f, slotSize - 10f, slotSize - 10f);
            }
        }
        batch.setColor(Color.WHITE);
    }

    private int getClickedSlot(float mouseX, float mouseY) {
        float barWidth = 400f;
        float barHeight = 80f;
        float barX = viewport.getWidth() / 2 - barWidth / 2;
        float barY = 0f;
        float padding = 10f;
        int size = avatar.getPictureInventory().getSize();

        float slotSize = (barWidth - padding * (size + 1)) / size;
        float startX = barX + padding;

        for (int i = 0; i < size; i++) {
            float slotX = startX + i * (slotSize + padding);
            if (mouseX >= slotX && mouseX <= slotX + slotSize &&
            mouseY >= barY && mouseY <= barY + barHeight) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void dispose() {
        if (markerPixel != null) {
            markerPixel.dispose();
            markerPixel = null;
        }
        super.dispose();
    }
}
