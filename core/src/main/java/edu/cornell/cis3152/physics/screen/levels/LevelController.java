package edu.cornell.cis3152.physics.screen.levels;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.cis3152.physics.CanvasRender;
import edu.cornell.cis3152.physics.screen.PhysicsScene;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;

/**
 * Manages level lifecycle: which level is active, loading, advancing, and restarting.
 *
 * <p>On construction, discovers how many levels exist by scanning the platform constants
 * JSON for {@code level1}, {@code level2}, … and stores the total count. Each successful
 * {@link #loadLevel(int)} constructs a fresh {@link LevelBaseScene}, attaches the shared
 * {@link SpriteBatch} and {@link CanvasRender} viewport, and shows/resizes it.
 *
 * @see #countLevels(AssetDirectory)
 * @see #loadLevel(int)
 * @see #nextLevel()
 * @see #restartLevel()
 */
public class LevelController {

    /** Current level index */
    private int currentLevel;

    /** Total number of levels */
    private int totalLevels;

    /** Loaded assets */
    private AssetDirectory assets;

    /** Shared sprite batch */
    private SpriteBatch batch;
    /** Shared letterboxed viewport */
    private CanvasRender viewport;

    /** Current level screen */
    private PhysicsScene currentScene;

    private final LevelProgress levelProgress;

    /**
     * Constructor. Discovers available levels by counting levelN keys in constants.
     */
    public LevelController(AssetDirectory assets, SpriteBatch batch, CanvasRender viewport) {
        this.assets = assets;
        this.batch = batch;
        this.viewport = viewport;

        currentLevel = 1;
        totalLevels = countLevels(assets);

        loadLevel(currentLevel);
        levelProgress = new LevelProgress(totalLevels, "assets/save_game.json", assets.getEntry("platform-constants", JsonValue.class));
    }

    /**
     * Scans {@code platform-constants} JSON for consecutive keys {@code level1}, {@code level2}, …
     * until a key is missing, and returns that count (at least 1).
     *
     * @param assets asset directory containing the constants entry
     * @return number of levels found
     */
    private static int countLevels(AssetDirectory assets) {
        JsonValue constants = assets.getEntry("platform-constants", JsonValue.class);
        int count = 0;
        while (constants.get("level" + (count + 1)) != null) {
            count++;
        }
        return Math.max(1, count);
    }

    /**
     * Loads the given level index: creates a new {@link LevelBaseScene}, wires the shared
     * sprite batch and viewport, shows and resizes the scene, and updates the active level index.
     *
     * <p>No-op (with a console message) if {@code level} is outside {@code 1..totalLevels}.
     *
     * @param level 1-based level index
     */
    public void loadLevel(int level) {
        if (level >= 1 && level <= totalLevels) {

            currentScene = new LevelBaseScene(assets);
        } else {
            System.out.println("no level");
            return;
        }

        currentScene.setBatch(batch);
        currentScene.setViewport(viewport);
        currentScene.show();
        currentScene.resize(com.badlogic.gdx.Gdx.graphics.getWidth(), com.badlogic.gdx.Gdx.graphics.getHeight());
        currentLevel = level;
        ((LevelBaseScene)currentScene).setLevel(level);
    }

    /**
     * Sets the screen listener for the current scene
     */
    public void setScreenListener(edu.cornell.gdiac.util.ScreenListener listener) {
        if (currentScene != null) {
            currentScene.setScreenListener(listener);
        }
    }

    /**
     * Advances to the next level; after the last level, wraps to level 1.
     */
    public void nextLevel() {
        if (currentLevel < totalLevels) {
            loadLevel(currentLevel + 1);
        }
        else
        {
            loadLevel(1);
        }
    }

    /**
     * Reloads the current level by calling {@link #loadLevel(int)} again with the same index
     * (fresh {@link LevelBaseScene} instance).
     */
    public void restartLevel() {
        loadLevel(currentLevel);
    }

    /**
     * Get current scene
     */
    public PhysicsScene getCurrentScene() {
        return currentScene;
    }

    /**
     * Get current level number
     */
    public int getCurrentLevel() {
        return currentLevel;
    }

    /**
     * Get total number of levels.
     */
    public int getTotalLevels() {
        return totalLevels;
    }

    /** Release the current level scene (e.g. when returning to the title screen). */
    public void dispose() {
        if (currentScene != null) {
            currentScene.dispose();
            currentScene = null;
        }
    }

    public void markCurrentBeaten() {
        int photosUsed = ((LevelBaseScene)currentScene).getPhotosUsed();
        int flyCount = ((LevelBaseScene)currentScene).getFlyCount();
        float timeElapsed = ((LevelBaseScene)currentScene).getTimeElapsed();
        levelProgress.beatLevel(currentLevel, photosUsed, flyCount, timeElapsed);
    }

    public boolean isBeaten(int level) {return levelProgress.isBeaten(level);}

    public LevelProgress getLevelProgress() {return levelProgress;}
    public int getLevelScore(int level) {return levelProgress.getLevelScore(level);}

}
