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
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.cis3152.physics.InputController;
import edu.cornell.cis3152.physics.screen.PhysicsScene;
import edu.cornell.cis3152.physics.screen.WorldState;
import edu.cornell.cis3152.physics.world.*;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.audio.SoundEffect;
import edu.cornell.gdiac.audio.SoundEffectManager;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.TextLayout;
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
    private static final float PAUSE_ICON_SIZE = 38.0f;

    private Texture backgroundTexture;

    private Texture slotTexture;
    private Texture pauseIconTexture;
    /** The jump sound. We only want to play once. */
    private SoundEffect jumpSound;
    /** The weapon fire sound. We only want to play once. */
    private SoundEffect fireSound;
    /** The weapon pop sound. We only want to play once. */
    private SoundEffect plopSound;
    private SoundEffect hoverSound;
    /** The default sound volume */
    private float volume;

    /** Reference to the character avatar */
    private Zuko avatar;
    /** Reference to the goalDoor (for collision detection) */
    private Door goalDoor;

    //** Picture list */
    private WorldState worldState;

    // Levels
    private int currentLevel = 1;
    // Range Variables
    private float STICK_PICTURE_DISTANCE = 9.0f; //I know you wanted it to be 3 times less than take picture but, if you mistakenly take a picture of the rock, you would not be able to reach the cloud unless it is 2 times less
    private float TAKE_PICTURE_DISTANCE = 9.0f;
    private static final float LIFT_SPRING_STIFFNESS = 6.0f;
    private static final float LIFT_SPRING_DAMPING = 3.5f;
    /** Mark set to handle more sophisticated collision callbacks */
    protected ObjectSet<Fixture> sensorFixtures;

    private TextLayout cameraLabel;
    private Texture markerPixel;
    private LevelPopulation levelPopulation;
    private LevelPopulation.Result levelData;
    private PhotoSystem photoSystem;
    private LevelRenderer renderer;

    private void ensureInitialized() {
        if (worldState == null) {
            worldState = new WorldState();
        }
        if (sensorFixtures == null) {
            sensorFixtures = new ObjectSet<Fixture>();
        }
        if (jumpSound == null) {
            jumpSound = directory.getEntry("platform-jump", SoundEffect.class);
            fireSound = directory.getEntry("platform-pew", SoundEffect.class);
            plopSound = directory.getEntry("platform-plop", SoundEffect.class);
            hoverSound = directory.getEntry("platform-hover", SoundEffect.class);
            volume = constants.getFloat("volume", 1.0f);
        }
        if (backgroundTexture == null) {
            backgroundTexture = requireTexture("shared-background", "shared/background.png");
        }
        if (pauseIconTexture == null) {
            pauseIconTexture = requireTexture("platform-pause", "platform/pause_icon.png");
        }
        if (markerPixel == null) {
            Pixmap markerPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            markerPixmap.setColor(Color.WHITE);
            markerPixmap.fill();
            markerPixel = new Texture(markerPixmap);
            markerPixmap.dispose();
        }
        if (slotTexture == null) {
            slotTexture = markerPixel;
        }
        if (levelPopulation == null) {
            levelPopulation = new LevelPopulation(constants, this::requireTexture, this::addSprite);
        }
        if (photoSystem == null) {
            photoSystem = new PhotoSystem(
                    worldState,
                    STICK_PICTURE_DISTANCE,
                    TAKE_PICTURE_DISTANCE,
                    LIFT_SPRING_STIFFNESS,
                    LIFT_SPRING_DAMPING,
                    volume,
                    fireSound,
                    plopSound
            );
        }
        if (renderer == null) {
            renderer = new LevelRenderer(
                    worldState,
                    slotTexture,
                    pauseIconTexture,
                    markerPixel,
                    STICK_PICTURE_DISTANCE,
                    TAKE_PICTURE_DISTANCE
            );
        }
    }

    /**
     * Resolves a texture from the asset directory, with a direct file fallback for
     * cases where the manifest key is temporarily out of sync.
     */
    private Texture requireTexture(String key, String fallbackPath) {
        Texture texture = directory.getEntry(key, Texture.class);
        if (texture == null) {
            texture = new Texture(Gdx.files.internal(fallbackPath));
        }
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }

    /**
     * Creates and initialize a new instance of the platformer game
     *
     * The game has default gravity and other settings
     */
    public LevelBaseScene(AssetDirectory directory) {
        super(directory,"platform");
        ensureInitialized();
        world.setContactListener(this);
    }

    @Override
    protected void drawBackground(SpriteBatch batch) {
        if (backgroundTexture != null) {
            batch.setColor(Color.WHITE);
            batch.draw(backgroundTexture, 0, 0, viewport.getWidth(), viewport.getHeight());
        }
        if (renderer != null && levelData != null) {
            renderer.drawLevelTiles(batch, levelData, height / bounds.height);
        }
    }

    /**
     * Resets the status of the game so that we can play again.
     *
     * This method disposes of the world and creates a new one.
     */
    public void reset() {
        ensureInitialized();
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

        worldState.reset();

        populateLevel();
    }

    /**
     * Lays out the game geography.
     */
    private void populateLevel() {
        float units = height/(bounds.height);
        if (constants.get("level" + currentLevel) == null) {
            currentLevel = 1;
        }
        levelData = levelPopulation.populate(currentLevel, units, worldState);
        goalDoor = levelData.goalDoor;
        avatar = levelData.avatar;
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

    @Override
    public void draw(float dt) {
        super.draw(dt);
        renderer.draw(batch, viewport, camera, avatar);
    }

    @Override
    public void postUpdate(float dt) {
        for (ObstacleSprite sprite : sprites) {
            if (sprite instanceof GameObject go) {
                go.syncPhysics();
            }
        }
        super.postUpdate(dt);
    }

    @Override
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

    @Override
    public void update(float dt) {
        viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), worldState.getPauseMouseCache());
        float iconSize = PAUSE_ICON_SIZE;
        float iconX = viewport.getWidth() - iconSize - 15f;
        float iconY = viewport.getHeight() - iconSize - 15f;
        Vector2 pauseMouseCache = worldState.getPauseMouseCache();
        boolean pauseIconHovered = pauseMouseCache.x >= iconX && pauseMouseCache.x <= iconX + iconSize
                && pauseMouseCache.y >= iconY && pauseMouseCache.y <= iconY + iconSize;
        worldState.setPauseIconHovered(pauseIconHovered);

        if (pauseIconHovered && !worldState.wasPauseIconHovered()) {
            SoundEffectManager.getInstance().play("hover", hoverSound, volume * 0.4f);
        }
        worldState.setPauseIconWasHovered(pauseIconHovered);

        if (pauseIconHovered && Gdx.input.justTouched()) {
            pauseClicked = true;
        }

        InputController input = InputController.getInstance();
        photoSystem.updateHighlights(avatar, sprites);
        photoSystem.handlePictureShortcuts(input, avatar);
        photoSystem.updateAvatarMovement(input, avatar);

        GameObject target = photoSystem.resolveCurrentTarget(input, avatar, sprites);
        float units = avatar.getObstacle().getPhysicsUnits();
        int clickedSlot = renderer.getClickedSlot(
                input.getCrossHair().x * units,
                input.getCrossHair().y * units,
                viewport,
                avatar.getPictureInventory().getSize()
        );
        photoSystem.handlePictureAction(input, target, avatar, clickedSlot);

        if (avatar.getCamera().isPictureTaken()) {
            avatar.getCamera().clearPictureTaken();
        }

        photoSystem.applyLiftSprings(sprites);
        avatar.applyForce();
        if (avatar.isJumping()) {
            SoundEffectManager.getInstance().play("jump", jumpSound, volume);
            avatar.startJumpAnimation();
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
                Fixture supportFixture = avatar == bd1 ? fix2 : fix1;
                avatar.setGrounded(true);
                sensorFixtures.add(supportFixture);
                avatar.setCurrentPlatform(getSupportObj(supportFixture));
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
            Fixture supportFixture = avatar == bd1 ? fix2 : fix1;
            sensorFixtures.remove(supportFixture);
            if (sensorFixtures.size == 0) {
                avatar.setGrounded(false);
                avatar.setCurrentPlatform(null);
            } else if (avatar.getCurrentPlatform() == getSupportObj(supportFixture)) {
                refreshCurrentSupport();
            }
        }
    }

    /**
     * Chooses one of the active sensor contacts as the current supporting obstacle
     * This is for texture blocks that edit movement
     */
    private void refreshCurrentSupport() {
        for (Fixture fixture : sensorFixtures) {
            GameObject support = getSupportObj(fixture);
            if (support != null) {
                avatar.setCurrentPlatform(support);
                return;
            }
        }
        avatar.setCurrentPlatform(null);
    }

    /**
     * Returns the GameObject type associated with a support fixture
     */
    private GameObject getSupportObj(Fixture fixture) {
        Object userData = fixture.getBody().getUserData();
        return userData instanceof GameObject ? (GameObject) userData : null;
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

    @Override
    public void dispose() {
        if (markerPixel != null) {
            markerPixel.dispose();
            markerPixel = null;
        }
        super.dispose();
    }
}
