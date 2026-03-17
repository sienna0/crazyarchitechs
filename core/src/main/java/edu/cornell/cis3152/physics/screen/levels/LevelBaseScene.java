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
import edu.cornell.gdiac.graphics.TextAlign;
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
    /** Mark set to handle more sophisticated collision callbacks */
    protected ObjectSet<Fixture> sensorFixtures;

    private TextLayout cameraLabel;
    private TextLayout pictureMarkerLabel;
    private OrthographicCamera textCamera;
    private BitmapFont font;
    private Texture markerPixel;
    private Path2 markerOutline;
    private static final float PICTURE_MARKER_WIDTH = 28.0f;
    private static final float PICTURE_MARKER_HEIGHT = 22.0f;
    private static final float PICTURE_MARKER_OFFSET_Y = 6.0f;
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

        textCamera = new OrthographicCamera();
        textCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        font = new BitmapFont();
        cameraLabel = new TextLayout();
        cameraLabel.setFont( font );

        pictureMarkerLabel = new TextLayout();
        pictureMarkerLabel.setFont(font);
        pictureMarkerLabel.setAlignment(TextAlign.middleCenter);
        pictureMarkerLabel.setColor(Color.BLACK);

        Pixmap markerPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        markerPixmap.setColor(Color.WHITE);
        markerPixmap.fill();
        markerPixel = new Texture(markerPixmap);
        markerPixmap.dispose();

        markerOutline = new Path2();
        new PathFactory().makeRect(0, 0, PICTURE_MARKER_WIDTH, PICTURE_MARKER_HEIGHT, markerOutline);
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
        Texture texture = directory.getEntry( "shared-goal", Texture.class );

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

        // Create Zuko
        texture = directory.getEntry( "platform-traci", Texture.class );
        avatar = new Zuko(units, constants.get("level" + currentLevel).get("objectLocations").get("zuko"));
        avatar.setTexture(texture);
        avatar.setBaseTexture(texture);
        addSprite(avatar);
        avatar.createSensor();
        Texture photoSheet = directory.getEntry( "platform-camera", Texture.class );
        avatar.setPhotoAnimation(photoSheet, 1, 17, 17);
        Texture jumpSheet = directory.getEntry( "platform-jump", Texture.class );
        avatar.setJumpAnimation(jumpSheet, 1, 7, 7);

        float rockSize = 1.5f;
        float cloudSize = 1.5f;
//        float platformLeftX = 25.0f;
//        float platformTopY = 10.0f;



        JsonValue rocks = constants.get("level"+currentLevel).get("objectLocations");
        JsonValue rockjv = rocks.get("rock");
        for (int ii = 0;  ii < rockjv.size; ii++){
            float [] rockPositions = rockjv.get(ii).asFloatArray();
            rock = new GameObject(
                    Obj.ROCK, constants.get("rock"), units,
                    rockPositions[0], rockPositions[1],
                    rockSize, rockSize,
                    BodyDef.BodyType.DynamicBody,
                    false
            );
            rock.setTexture(earthTexture);
            addSprite(rock);
        }

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

        // Rock
//        float[] rockPositions = constants.get("level" + currentLevel).get("objectLocations").get("rock").asFloatArray();
//        rock = new GameObject(
//                Obj.ROCK, constants.get("rock"), units,
//                rockPositions[0], rockPositions[1],
//                rockSize, rockSize,
//                BodyDef.BodyType.DynamicBody,
//                false
//        );
//        rock.setTexture(earthTexture);
//        addSprite(rock);

//        Texture cloudTexture = directory.getEntry( "cloud", Texture.class );
//        // Cloud (normal mode = "floaty platform marker": gravity off, collides)
//        float[] cloudPositions = constants.get("level" + currentLevel).get("objectLocations").get("cloud").asFloatArray();
//        cloud = new GameObject(
//                Obj.CLOUD, constants.get("cloud"), units, cloudPositions[0], cloudPositions[1], cloudSize,
//                cloudSize, BodyDef.BodyType.DynamicBody, false
//        );
//        cloud.setTexture(cloudTexture);
//        addSprite(cloud);
//        cloudHomeY = cloud.getObstacle().getY();

        float iceSize = 1.5f;
        Pixmap icePixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        icePixmap.setColor(0.7f, 0.78f, 0.85f, 1.0f);
        icePixmap.fill();
        Texture iceTexture = new Texture(icePixmap);
        icePixmap.dispose();

//        float[] icePositions = constants.get("level" + currentLevel).get("objectLocations").get("ice").asFloatArray();
//        ice = new GameObject(
//                Obj.ICE, constants.get("ice"), units,
//                icePositions[0], icePositions[1],
//                iceSize, iceSize,
//                BodyDef.BodyType.StaticBody, false
//        );
//        ice.setTexture(iceTexture);
//        addSprite(ice);

        JsonValue ices = constants.get("level"+currentLevel).get("objectLocations");
        JsonValue icejv = ices.get("ice");
        for (int ii = 0;  ii < icejv.size; ii++){
            float [] icePosition = icejv.get(ii).asFloatArray();
            ice = new GameObject(
                    Obj.ICE, constants.get("ice"), units,
                    icePosition[0], icePosition[1],
                    iceSize, iceSize,
                    BodyDef.BodyType.DynamicBody,
                    false
            );
            ice.setTexture(iceTexture);
            addSprite(ice);
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
            activePicture = null;
            pictures.clear();
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
        avatar.setShooting(input.didSecondary());
    }

    private GameObject resolveCurrentTarget(InputController input) {
        Vector2 mouse = input.getCrossHair();
        GameObject target = findObjectUnderMouse(mouse.x, mouse.y);
        avatar.setCurrentTarget(target);
        return target;
    }

    private void handlePictureAction(InputController input, GameObject target) {
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

        if (activePicture == null) {
            takePictureOfTarget(target);
            return;
        }

        applyPictureToTarget(target);
    }

    private void takePictureOfTarget(GameObject target) {
        if (!avatar.getCamera().canTakePicture(
                target.getObstacle().getX(),
                target.getObstacle().getY(),
                avatar.getObstacle().getX(),
                avatar.getObstacle().getY())) {
            return;
        }

        avatar.getCamera().takePicture();
        avatar.startPhotoAnimation();
        Picture picture = new Picture(target, avatar.getCamera().getCameraType());
        pictures.clear();
        pictures.add(picture);
        activePicture = picture;

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

    private void drawPlacedPictureMarkers() {
        for (Picture picture : pictures) {
            if (picture.getTarget() == null) {
                continue;
            }

            GameObject target = picture.getTarget();
            Obstacle obstacle = target.getObstacle();
            Rectangle targetBounds = target.getMesh().computeBounds();
            float units = obstacle.getPhysicsUnits();
            float centerX = obstacle.getX() * units;
            float centerY = obstacle.getY() * units;

            float markerX = centerX + targetBounds.x + targetBounds.width - PICTURE_MARKER_WIDTH;
            float markerY = centerY + targetBounds.y + targetBounds.height + PICTURE_MARKER_OFFSET_Y;
            markerX = MathUtils.clamp(markerX, 0.0f, canvas.getWidth() - PICTURE_MARKER_WIDTH);
            markerY = MathUtils.clamp(markerY, 0.0f, canvas.getHeight() - PICTURE_MARKER_HEIGHT);

            canvas.setColor(getMarkerColor(picture.getCameraType()));
            canvas.draw(markerPixel, markerX, markerY, PICTURE_MARKER_WIDTH, PICTURE_MARKER_HEIGHT);

            highlightTransform.idt();
            highlightTransform.preTranslate(markerX, markerY);
            canvas.setColor(Color.BLACK);
            canvas.outline(markerOutline, highlightTransform);

            pictureMarkerLabel.setText(Integer.toString(getMarkerNumber(picture.getCameraType())));
            pictureMarkerLabel.layout();
            canvas.drawText(
                    pictureMarkerLabel,
                    markerX + (PICTURE_MARKER_WIDTH / 2.0f),
                    markerY + (PICTURE_MARKER_HEIGHT / 2.0f) + 7.0f
            );
        }
    }

    private int getMarkerNumber(CameraType cameraType) {
        return switch (cameraType) {
            case THERMAL -> 1;
            case REGULAR -> 2;
            case TEXTURE -> 3;
        };
    }

    private Color getMarkerColor(CameraType cameraType) {
        return switch (cameraType) {
            case THERMAL -> new Color(0.95f, 0.62f, 0.29f, 0.95f);
            case REGULAR -> new Color(0.92f, 0.92f, 0.92f, 0.95f);
            case TEXTURE -> new Color(0.45f, 0.82f, 0.55f, 0.95f);
        };
    }

    @Override
    public void draw(float dt) {
        super.draw(dt);

        canvas.begin(camera);
        Color highlighter = (activePicture != null) ? Color.LIME : Color.CORAL;
        canvas.setColor(highlighter);

        for (GameObject go : highlighted) {
            Obstacle obj = go.getObstacle();
            float u = obj.getPhysicsUnits();
            float a = obj.getAngle();
            Vector2 p = obj.getPosition();

            highlightTransform.idt();
            highlightTransform.preRotate((float)(a * 180.0f/ Math.PI));
            highlightTransform.preTranslate(p.x * u, p.y * u);

            canvas.outline(obj.getOutline(), highlightTransform);
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
            canvas.setColor(Color.LIME);
            for (float angle = 0; angle < 360; angle += total) {
                Path2 stickArc = factory.makeArc(cx,cy, (STICK_PICTURE_DISTANCE * u * 2) - 1, angle, dashSize, false);
                canvas.outline(stickArc, highlightTransform);
                Path2 stickArc2 = factory.makeArc(cx,cy, (STICK_PICTURE_DISTANCE * u * 2) , angle, dashSize, false);
                canvas.outline(stickArc2, highlightTransform);
                Path2 stickArc3 = factory.makeArc(cx,cy, (STICK_PICTURE_DISTANCE * u * 2) -2, angle, dashSize, false);
                canvas.outline(stickArc3, highlightTransform);
            }
            canvas.setColor(Color.CORAL);
            for (float angle = 0; angle < 360; angle += total) {
                Path2 takeArc = factory.makeArc(cx,cy, (TAKE_PICTURE_DISTANCE * u * 2) - 1, angle, dashSize, false);
                canvas.outline(takeArc, highlightTransform);
                Path2 takeArc2 = factory.makeArc(cx,cy, (TAKE_PICTURE_DISTANCE * u  * 2), angle, dashSize, false);
                canvas.outline(takeArc2, highlightTransform);
                Path2 takeArc3 = factory.makeArc(cx,cy, (TAKE_PICTURE_DISTANCE * u * 2) - 2, angle, dashSize, false);
                canvas.outline(takeArc3, highlightTransform);

            }
        }

        drawPlacedPictureMarkers();

        canvas.end();

        String label = avatar.getCamera().getCameraType().getLabel();



        cameraLabel.setText(label);

        canvas.begin(textCamera);
        canvas.setColor(Color.WHITE);
        canvas.drawText(cameraLabel, 50, canvas.getHeight()-20);
        canvas.end();
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
