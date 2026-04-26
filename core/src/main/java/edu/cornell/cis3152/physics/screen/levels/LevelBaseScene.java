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
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.cis3152.physics.world.FlyCollectible;
import java.util.ArrayList;
import edu.cornell.cis3152.physics.CanvasRender;
import edu.cornell.cis3152.physics.GameAudio;
import edu.cornell.cis3152.physics.InputController;
import edu.cornell.cis3152.physics.screen.PhysicsScene;
import edu.cornell.cis3152.physics.screen.WorldState;
import edu.cornell.cis3152.physics.world.*;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.audio.SoundEffect;
import edu.cornell.gdiac.audio.SoundEffectManager;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import java.util.List;

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
    private static final float UI = CanvasRender.layoutScale();
    private static final float TRANSITION_ENTRY_X = 1.25f;
    private static final float TRANSITION_WALK_MULTIPLIER = 0.4f;
    private static final float CAMERA_ZOOM = 0.75f;
    private static final float PULLEY_ROPE_WHEEL_INSET = 2.0f / 16.0f;

    private Texture backgroundTexture;

    /** Slot frame + overlay for inventory thumbnails ({@code shared/inventory.png}). */
    private Texture inventoryTexture;
    private Texture settingsIconTexture;
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
    /** Rows = stuck-on surface ({@link GameObject} target); cols = photographed type (subject). Filenames: {@code picture_<surface>_with_<photo>.png}. */
    private Texture[][] stuckPictureTextures;
    private LevelPopulation levelPopulation;
    private LevelPopulation.Result levelData;
    private PhotoSystem photoSystem;
    private LevelRenderer renderer;

    private float gooAnimPhaseTimer;
    private int gooAnimCycle;
    /** From {@code constants.json} {@code goo.frame_duration_sec}. */
    private float gooAnimFrameDuration = 0.26f;

    /** Flies collected so far in the current level. */
    private int flyCount = 0;
    /** Total flies placed in the current level. */
    private int flyTotal = 0;
    /** Fly waiting to be collected once Zuko's tongue finishes retracting. */
    private FlyCollectible pendingFlyCollection = null;
    /** Whether the tongue was active last frame (for edge-detection). */
    private boolean tonguePreviouslyActive = false;
    /** Collect range in physics units — matches take_picture_distance. */
    private float flyCollectRange = 9.0f;

    private boolean pendingHazardRestart = false;
    private float hazardTimer = 0f;
    private boolean hazardTriggered = false;
    private static final float HAZARD_DELAY = 0.9f;
    private boolean enteringFromPreviousLevel = false;
    private float entryTargetX = 0f;
    private float entryTargetY = 0f;

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
        if (settingsIconTexture == null) {
            settingsIconTexture = requireTexture("platform-settings", "platform/settings_icon.png");
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
        if (inventoryTexture == null) {
            inventoryTexture = requireTexture("shared-inventory", "shared/inventory.png");
        }
        if (levelPopulation == null) {
            levelPopulation = new LevelPopulation(constants, this::requireTexture, this::addSprite);
        }
        JsonValue gooJson = constants.get("goo");
        if (gooJson != null) {
            gooAnimFrameDuration = gooJson.getFloat("frame_duration_sec", 0.26f);
        }
        if (photoSystem == null) {
            JsonValue gp = constants.get("gameplay");
            float stickDist = gp != null ? gp.getFloat("stick_picture_distance", 9.0f) : 9.0f;
            float takeDist  = gp != null ? gp.getFloat("take_picture_distance", 9.0f) : 9.0f;
            float springK   = gp != null ? gp.getFloat("lift_spring_stiffness", 6.0f) : 6.0f;
            float springD   = gp != null ? gp.getFloat("lift_spring_damping", 3.5f) : 3.5f;

            flyCollectRange = takeDist;
            photoSystem = new PhotoSystem(
                    worldState, stickDist, takeDist, springK, springD,
                    volume, fireSound, plopSound, constants.get("zuko")
            );
            if (stuckPictureTextures == null) {
                stuckPictureTextures = loadStuckPictureTextures();
            }
            renderer = new LevelRenderer(
                    worldState, inventoryTexture, settingsIconTexture,
                    pauseIconTexture, markerPixel,
                    stuckPictureTextures,
                    stickDist, takeDist
            );
        }
    }

    /**
     * Loads combo sprites keyed by {@code [target surface][photographed subject]} — matches
     * {@code picture_<surface>_with_<photo>.png} (e.g. honey block + ice snapshot → honey_with_ice).
     */
    private Texture[][] loadStuckPictureTextures() {
        Obj[] vals = Obj.values();
        Texture[][] m = new Texture[vals.length][vals.length];
        m[Obj.CLOUD.ordinal()][Obj.HONEY.ordinal()] =
                requireTexture("platform-picture-cloud-with-honey", "platform/picture_cloud_with_honey.png");
        m[Obj.CLOUD.ordinal()][Obj.ICE.ordinal()] =
                requireTexture("platform-picture-cloud-with-ice", "platform/picture_cloud_with_ice.png");
        m[Obj.HONEY.ordinal()][Obj.CLOUD.ordinal()] =
                requireTexture("platform-picture-honey-with-cloud", "platform/picture_honey_with_cloud.png");
        m[Obj.HONEY.ordinal()][Obj.ICE.ordinal()] =
                requireTexture("platform-picture-honey-with-ice", "platform/picture_honey_with_ice.png");
        m[Obj.ICE.ordinal()][Obj.CLOUD.ordinal()] =
                requireTexture("platform-picture-ice-with-cloud", "platform/picture_ice_with_cloud.png");
        m[Obj.ICE.ordinal()][Obj.HONEY.ordinal()] =
                requireTexture("platform-picture-ice-with-honey", "platform/picture_ice_with_honey.png");
        return m;
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
    protected void drawFixedBackground(SpriteBatch batch) {
        if (backgroundTexture != null) {
            batch.setColor(Color.WHITE);
            batch.draw(backgroundTexture, 0, 0, viewport.getWidth(), viewport.getHeight());
        }
    }

    @Override
    protected void drawBackground(SpriteBatch batch) {
        if (renderer != null && levelData != null) {
            renderer.drawLevelTiles(batch, levelData, height / bounds.height);
        }
    }

    @Override
    protected void updateCamera() {
        if (camera == null) {
            return;
        }

        camera.zoom = CAMERA_ZOOM;
        float halfViewWidth = (camera.viewportWidth * camera.zoom) * 0.5f;
        float halfViewHeight = (camera.viewportHeight * camera.zoom) * 0.5f;
        float worldWidth = bounds.width * scale.x;
        float worldHeight = bounds.height * scale.y;

        float targetX = worldWidth * 0.5f;
        float targetY = worldHeight * 0.5f;
        if (avatar != null) {
            targetX = avatar.getPosition().x * scale.x;
            targetY = avatar.getPosition().y * scale.y;
        }

        camera.position.set(
                clampCameraAxis(targetX, halfViewWidth, worldWidth),
                clampCameraAxis(targetY, halfViewHeight, worldHeight),
                0f
        );
    }

    private float clampCameraAxis(float target, float halfView, float worldSize) {
        if (worldSize <= halfView * 2.0f) {
            return worldSize * 0.5f;
        }
        return Math.max(halfView, Math.min(worldSize - halfView, target));
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
        levelData = levelPopulation.populate(currentLevel, units, worldState, world);
        goalDoor = levelData.goalDoor;
        avatar = levelData.avatar;
        gooAnimPhaseTimer = 0f;
        gooAnimCycle = 0;
        flyCount = 0;
        flyTotal = levelData.flies.size();
        pendingFlyCollection = null;
        tonguePreviouslyActive = false;
    }

    /** Returns the number of flies collected in the current level. */
    public int getFlyCount() { return flyCount; }

    /** Returns the total number of flies placed in the current level. */
    public int getFlyTotal() { return flyTotal; }

    private void applyGooTextureRegions() {
        if (levelData == null || levelData.gooFrames == null || levelData.gooDecors.isEmpty()) {
            return;
        }
        TextureRegion[] frames = levelData.gooFrames;
        int n = frames.length;
        if (n == 0) {
            return;
        }
        List<BoxSprite> decors = levelData.gooDecors;
        for (int ii = 0; ii < decors.size(); ii++) {
            BoxSprite b = decors.get(ii);
            float x = b.getObstacle().getX();
            float y = b.getObstacle().getY();
            int off = LevelPopulation.gooPhaseOffsetForDecor(x, y, n);
            decors.get(ii).setTextureRegion(frames[(gooAnimCycle + off) % n]);
        }
    }

    private void updateGooAnimation(float dt) {
        if (levelData == null || levelData.gooFrames == null || levelData.gooDecors.isEmpty()) {
            return;
        }
        int n = levelData.gooFrames.length;
        if (n == 0) {
            return;
        }
        gooAnimPhaseTimer += dt;
        while (gooAnimPhaseTimer >= gooAnimFrameDuration) {
            gooAnimPhaseTimer -= gooAnimFrameDuration;
            gooAnimCycle = (gooAnimCycle + 1) % n;
            applyGooTextureRegions();
        }
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

    /**
     * Starts a short auto-walk from the left edge into this level's spawn point.
     */
    public void beginEntryFromPreviousLevel() {
        if (avatar == null) {
            return;
        }

        entryTargetX = avatar.getObstacle().getX();
        entryTargetY = avatar.getObstacle().getY();
        if (entryTargetX <= TRANSITION_ENTRY_X + 0.15f) {
            return;
        }

        enteringFromPreviousLevel = true;
        avatar.setFacingRight(true);
        avatar.setJumping(false);
        avatar.setGrounded(false);
        avatar.warpTo(TRANSITION_ENTRY_X, entryTargetY);
    }

    @Override
    public void draw(float dt) {
        super.draw(dt);
        renderer.draw(batch, viewport, camera, uiCamera, avatar);
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
                go.clampHoneyHorizontalVelocityUnlessIced();
            }
        }
        syncPulleyRopes();
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
        if (hazardTriggered) {
            hazardTimer += dt;
            avatar.setMovement(0f);
            avatar.setJumping(false);

            if (hazardTimer >= HAZARD_DELAY) {
                pendingHazardRestart = true;
                hazardTriggered = false;
            }
            return;
        }
        updateGooAnimation(dt);
        photoSystem.update(dt);
        viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), worldState.getPauseMouseCache());
        float pauseRight = viewport.getWidth() - LevelHud.margin();
        float pauseTop = viewport.getHeight() - LevelHud.margin();
        Vector2 pauseMouseCache = worldState.getPauseMouseCache();
        float pauseHit = LevelHud.hoverIconSize() * LevelHud.iconDrawScale();
        float pauseLeft = pauseRight - pauseHit;
        float pauseBottom = pauseTop - pauseHit;
        boolean pauseIconHovered = pauseMouseCache.x >= pauseLeft && pauseMouseCache.x <= pauseRight
                && pauseMouseCache.y >= pauseBottom && pauseMouseCache.y <= pauseTop;

        float settingsHit = LevelHud.hoverIconSize() * LevelHud.iconDrawScale();
        float settingsRight = pauseLeft - LevelHud.iconGap();
        float settingsLeft = settingsRight - settingsHit;
        float settingsBottom = pauseTop - settingsHit;
        boolean settingsIconHovered = settingsIconTexture != null
                && pauseMouseCache.x >= settingsLeft && pauseMouseCache.x <= settingsRight
                && pauseMouseCache.y >= settingsBottom && pauseMouseCache.y <= pauseTop;
        worldState.setSettingsIconHovered(settingsIconHovered);
        if (settingsIconHovered && !worldState.wasSettingsIconHovered()) {
            SoundEffectManager.getInstance().play("hover", hoverSound,
                    GameAudio.effectiveSfxVolume(volume * 0.4f));
        }
        worldState.setSettingsIconWasHovered(settingsIconHovered);

        worldState.setPauseIconHovered(pauseIconHovered);

        if (pauseIconHovered && !worldState.wasPauseIconHovered()) {
            SoundEffectManager.getInstance().play("hover", hoverSound,
                    GameAudio.effectiveSfxVolume(volume * 0.4f));
        }
        worldState.setPauseIconWasHovered(pauseIconHovered);

        if (settingsIconHovered && Gdx.input.justTouched()) {
            settingsClicked = true;
        } else if (pauseIconHovered && Gdx.input.justTouched()) {
            pauseClicked = true;
        }

        if (enteringFromPreviousLevel) {
            updateEntryTransition();
            return;
        }

        InputController input = InputController.getInstance();
        photoSystem.updateHighlights(avatar, sprites, world);
        photoSystem.handlePictureShortcuts(input, avatar);
        updateAvatarMovement(input, avatar);

        GameObject target = photoSystem.resolveCurrentTarget(input, avatar, sprites);
        float units = avatar.getObstacle().getPhysicsUnits();
        int clickedSlot = renderer.getClickedSlot(
                pauseMouseCache.x,
                pauseMouseCache.y,
                viewport,
                avatar.getPictureInventory().getSize()
        );
        photoSystem.handlePictureAction(input, target, avatar, clickedSlot, world);

        if (photoSystem.isPictureTaken()) {
            photoSystem.clearPictureTaken();
        }

        // Fly tongue-completion: collect when tongue finishes retracting
        boolean tongueActive = avatar.isTongueActive();
        if (tonguePreviouslyActive && !tongueActive && pendingFlyCollection != null) {
            flyCount++;
            pendingFlyCollection.markCollected();
            pendingFlyCollection = null;
        }
        tonguePreviouslyActive = tongueActive;

        // Fly click detection (only when no active picture and no pending collection)
        if (input.didLeftClick() && worldState.getActivePicture() == null
                && clickedSlot < 0 && pendingFlyCollection == null) {
            Vector2 crosshair = input.getCrossHair();
            FlyCollectible clickedFly = findFlyUnderCrosshair(crosshair, units);
            if (clickedFly != null && flyInRange(clickedFly) && flyHasLineOfSight(clickedFly)) {
                avatar.startTongueAnimation(clickedFly.getObstacle().getX(), clickedFly.getObstacle().getY());
                pendingFlyCollection = clickedFly;
            }
        }

        // Update in-range fly list for highlight rendering
        ArrayList<FlyCollectible> inRangeFlies = new ArrayList<>();
        if (levelData != null) {
            for (FlyCollectible fly : levelData.flies) {
                if (!fly.isCollected() && flyInRange(fly) && flyHasLineOfSight(fly)) {
                    inRangeFlies.add(fly);
                }
            }
        }
        renderer.setInRangeFlies(inRangeFlies);

        photoSystem.applyLiftSprings(sprites);
        avatar.applyForce();
        if (avatar.isJumping()) {
            SoundEffectManager.getInstance().play("jump", jumpSound, GameAudio.effectiveSfxVolume(volume));
            avatar.startJumpAnimation();
        }
    }

    private FlyCollectible findFlyUnderCrosshair(Vector2 crosshair, float units) {
        if (levelData == null) return null;
        float halfSizePx = FlyCollectible.FLY_SIZE * units / 2f;
        float mouseX = crosshair.x * units;
        float mouseY = crosshair.y * units;
        for (FlyCollectible fly : levelData.flies) {
            if (fly.isCollected()) continue;
            float cx = fly.getObstacle().getX() * units;
            float cy = fly.getObstacle().getY() * units;
            if (mouseX >= cx - halfSizePx && mouseX <= cx + halfSizePx
                    && mouseY >= cy - halfSizePx && mouseY <= cy + halfSizePx) {
                return fly;
            }
        }
        return null;
    }

    private boolean flyInRange(FlyCollectible fly) {
        float dx = fly.getObstacle().getX() - avatar.getObstacle().getX();
        float dy = fly.getObstacle().getY() - avatar.getObstacle().getY();
        return dx * dx + dy * dy <= flyCollectRange * flyCollectRange;
    }

    /**
     * Returns true if at least 3 of 5 sample points on the fly are reachable from the avatar
     * without a solid fixture blocking the ray — mirrors the photo system's LOS logic.
     * Flies are sensors so the ray can't hit them; instead we check that nothing solid
     * intercepts the path to each sample point.
     */
    private boolean flyHasLineOfSight(FlyCollectible fly) {
        float ox = avatar.getObstacle().getX();
        float oy = avatar.getObstacle().getY();
        float tx = fly.getObstacle().getX();
        float ty = fly.getObstacle().getY();
        float half = FlyCollectible.FLY_SIZE * 0.4f;
        Vector2[] samples = {
            new Vector2(tx,        ty),
            new Vector2(tx - half, ty - half),
            new Vector2(tx + half, ty - half),
            new Vector2(tx - half, ty + half),
            new Vector2(tx + half, ty + half),
        };
        int visible = 0;
        for (Vector2 sample : samples) {
            if (isPathToFlyClear(ox, oy, sample.x, sample.y)) {
                if (++visible >= 3) return true;
            }
        }
        return false;
    }

    private boolean isPathToFlyClear(float ox, float oy, float tx, float ty) {
        final boolean[] blocked = {false};
        world.rayCast((fixture, point, normal, fraction) -> {
            if (fixture.isSensor()) return -1f;
            if (fixture.getBody().getUserData() == avatar) return -1f;
            blocked[0] = true;
            return 0f;
        }, ox, oy, tx, ty);
        return !blocked[0];
    }

    /**
     * Maps horizontal input to movement force and primary action to jump for the avatar.
     */
    public void updateAvatarMovement(InputController input, Zuko avatar) {
        avatar.setMovement(input.getHorizontal() * avatar.getForce());
        avatar.setJumping(input.didPrimary());
    }

    private void updateEntryTransition() {
        // this should make Zuko walk to the right on screen
        avatar.setFacingRight(true);
        avatar.setJumping(false);
        avatar.setMovement(avatar.getForce() * TRANSITION_WALK_MULTIPLIER);
        avatar.applyForce();

        if (avatar.getObstacle().getX() >= entryTargetX) {
            avatar.warpTo(entryTargetX, entryTargetY);
            avatar.setMovement(0f);
            avatar.stopMotion();
            enteringFromPreviousLevel = false;
        }
    }

    private void syncPulleyRopes() {
        if (levelData == null) {
            return;
        }
        if (levelData.pulleyCarries.size() < 2 || levelData.pulleyGroundAnchors.size() < 2 || levelData.pulleyRopes.isEmpty()) {
            return;
        }

        Vector2 leftCarryPos = levelData.pulleyCarries.get(0).getObstacle().getPosition();
        Vector2 leftOffset = levelData.pulleyCarryAnchorOffsets.get(0);
        Vector2 leftCarryAnchor = new Vector2(leftCarryPos.x + leftOffset.x, leftCarryPos.y + leftOffset.y);

        Vector2 rightCarryPos = levelData.pulleyCarries.get(1).getObstacle().getPosition();
        Vector2 rightOffset = levelData.pulleyCarryAnchorOffsets.get(1);
        Vector2 rightCarryAnchor = new Vector2(rightCarryPos.x + rightOffset.x, rightCarryPos.y + rightOffset.y);

        Vector2 leftTopAnchor = levelData.pulleyGroundAnchors.get(0);
        Vector2 rightTopAnchor = levelData.pulleyGroundAnchors.get(1);
        if (!levelData.pulleyWheelCenters.isEmpty() && !levelData.pulleyWheelRadii.isEmpty()) {
            Vector2 wheelCenter = levelData.pulleyWheelCenters.get(0);
            float wheelRadius = levelData.pulleyWheelRadii.get(0);
            float ropeRadius = Math.max(0.0f, wheelRadius - PULLEY_ROPE_WHEEL_INSET);
            float ropeY = wheelCenter.y;
            leftTopAnchor = new Vector2(wheelCenter.x - ropeRadius, ropeY);
            rightTopAnchor = new Vector2(wheelCenter.x + ropeRadius, ropeY);
        }

        Vector2[] ropePath = new Vector2[] { leftCarryAnchor, leftTopAnchor, rightTopAnchor, rightCarryAnchor };
        float[] segmentLengths = new float[ropePath.length - 1];
        float totalLength = 0.0f;
        for (int ii = 0; ii < segmentLengths.length; ii++) {
            segmentLengths[ii] = ropePath[ii].dst(ropePath[ii + 1]);
            totalLength += segmentLengths[ii];
        }
        if (totalLength <= 0.0f) {
            return;
        }

        int count = levelData.pulleyRopes.size();
        for (int ii = 0; ii < count; ii++) {
            float distance = totalLength * ((ii + 0.5f) / count);
            float traversed = 0.0f;
            int segment = 0;
            while (segment < segmentLengths.length - 1 && distance > traversed + segmentLengths[segment]) {
                traversed += segmentLengths[segment];
                segment++;
            }

            float segmentLength = segmentLengths[segment];
            Vector2 start = ropePath[segment];
            Vector2 end = ropePath[segment + 1];
            float t = segmentLength == 0.0f ? 0.0f : (distance - traversed) / segmentLength;
            float x = start.x + (end.x - start.x) * t;
            float y = start.y + (end.y - start.y) * t;
            float ropeAngle = (float) Math.atan2(-(end.x - start.x), end.y - start.y);

            levelData.pulleyRopes.get(ii).getObstacle().setPosition(x, y);
            levelData.pulleyRopes.get(ii).getObstacle().setAngle(ropeAngle);
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

            // See if surface is fatal
            if ((bd1 == avatar && bd2 instanceof Surface surface && surface.isFatal()) ||
                    (bd2 == avatar && bd1 instanceof Surface surface2 && surface2.isFatal())) {
                if (!hazardTriggered && !pendingHazardRestart) {
                    hazardTriggered = true;
                    hazardTimer = 0f;
                    avatar.startDeathMeltAnimation();
                    avatar.setMovement(0f);
                    avatar.setJumping(false);
                    avatar.stopMotion();
                }
                return;
            }

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
    public void preSolve(Contact contact, Manifold oldManifold) {
        Fixture fixtureA = contact.getFixtureA();
        Fixture fixtureB = contact.getFixtureB();

        Object userDataA = fixtureA.getBody().getUserData();
        Object userDataB = fixtureB.getBody().getUserData();
        if (userDataA instanceof GameObject || userDataB instanceof GameObject) {
            contact.resetFriction();
            contact.ResetRestitution();
        }
    }

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

    /** Returns whether Zuko is standing on a fatal tile */
    public boolean consumeHazardRestart() {
        if (pendingHazardRestart) {
            pendingHazardRestart = false;
            return true;
        }
        return false;
    }
}
