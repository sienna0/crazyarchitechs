/*
 * LevelBaseScene.java — Frogtographer
 *
 * Main gameplay scene for a single level: physics world, avatar, photos, and win/lose.
 * Extends the shared physics screen stack; based on the original PhysicsDemo Lab (Don Holden, 2007).
 */
package edu.cornell.cis3152.physics.screen.levels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
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
import edu.cornell.gdiac.physics2.ObstacleSprite;

/**
 * Primary gameplay scene for Frogtographer: one level at a time inside a Box2D world.
 *
 * <p>Extends {@link PhysicsScene} and implements {@link ContactListener} so collisions drive
 * ground contact and goal-door completion. On {@link #reset()} or {@link #setLevel(int)},
 * the scene clears bodies and sprites, repopulates geometry through {@link LevelPopulation},
 * and wires {@link PhotoSystem} (photo mechanics) and {@link LevelRenderer} (HUD and tiles).
 *
 * <p>{@link #update(float)} runs each frame: pause UI, input, photo highlights/actions,
 * lift springs, and avatar forces. After the world steps, {@link #postUpdate(float)} flushes
 * deferred Box2D changes via {@link GameObject#syncPhysics()}.
 *
 * <p>Gameplay tuning (stick/take distances, lift spring stiffness/damping, etc.) is read once
 * from the {@code gameplay} section of the platform constants JSON when photo/render
 * subsystems are first created.
 *
 * @see #beginContact(Contact)
 * @see #endContact(Contact)
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

    /** Shared picture inventory, highlights, and pause UI state for this level. */
    private WorldState worldState;

    /** Which level is currently loaded (1-based). */
    private int currentLevel = 1;
    /** Mark set to handle more sophisticated collision callbacks */
    protected ObjectSet<Fixture> sensorFixtures;

    private Texture markerPixel;
    private LevelPopulation levelPopulation;
    private LevelPopulation.Result levelData;
    private PhotoSystem photoSystem;
    private LevelRenderer renderer;

    /**
     * Lazily creates sound handles, textures, {@link WorldState}, contact tracking,
     * {@link LevelPopulation}, {@link PhotoSystem}, and {@link LevelRenderer}.
     * Reads {@code gameplay} JSON fields (e.g. stick/take distances, lift spring constants)
     * when constructing the photo and render helpers.
     */
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
            JsonValue gp = constants.get("gameplay");
            float stickDist = gp != null ? gp.getFloat("stick_picture_distance", 9.0f) : 9.0f;
            float takeDist  = gp != null ? gp.getFloat("take_picture_distance", 9.0f) : 9.0f;
            float springK   = gp != null ? gp.getFloat("lift_spring_stiffness", 6.0f) : 6.0f;
            float springD   = gp != null ? gp.getFloat("lift_spring_damping", 3.5f) : 3.5f;

            photoSystem = new PhotoSystem(
                    worldState, stickDist, takeDist, springK, springD,
                    volume, fireSound, plopSound
            );
            renderer = new LevelRenderer(
                    worldState, slotTexture, pauseIconTexture, markerPixel,
                    stickDist, takeDist
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
     * Tears down the current level: deactivates and clears sprites, destroys all Box2D bodies,
     * resets {@link WorldState}, then repopulates the level (same or new geometry after
     * {@link #setLevel(int)}).
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
     * Delegates to {@link LevelPopulation#populate} to build walls, tiles, objects, avatar,
     * and goal door for {@link #currentLevel}, updating {@link #goalDoor} and {@link #avatar}.
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
     * Switches to another level number (falls back to level 1 if undefined in constants),
     * then {@link #reset()} to rebuild the world.
     *
     * @param level 1-based level index from JSON ({@code levelN} keys)
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

    /**
     * After {@code world.step()}, applies any Box2D property changes that were deferred during
     * the step (body type, filters, etc.) by calling {@link GameObject#syncPhysics()} on each sprite.
     */
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

    /**
     * Main per-frame gameplay: pause icon hit-testing and sound, then photo highlights,
     * keyboard shortcuts, movement, mouse target and inventory slot clicks, picture actions,
     * lift springs, avatar forces, and jump audio/animation.
     */
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
     * Box2D contact start: registers ground support when Zuko's foot sensor touches a surface,
     * and marks the level complete when the avatar collides with the goal door.
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
     * Box2D contact end: removes ground contacts for the foot sensor; when no supports remain,
     * clears grounded state, otherwise picks another active support via {@link #refreshCurrentSupport()}.
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
     * After a support fixture ends, reassigns {@link Zuko#setCurrentPlatform(GameObject)} from
     * the remaining entries in {@link #sensorFixtures} so movement modifiers on the standing
     * surface stay correct when overlapping several bodies.
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
     * Resolves the {@link GameObject} under a support fixture from the fixture's body user data,
     * or {@code null} if the body is not a game object.
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
