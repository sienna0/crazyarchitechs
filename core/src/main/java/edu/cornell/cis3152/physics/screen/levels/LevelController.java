package edu.cornell.cis3152.physics.screen.levels;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.cis3152.physics.CanvasRender;
import edu.cornell.cis3152.physics.screen.PhysicsScene;
import edu.cornell.gdiac.assets.AssetDirectory;
import com.badlogic.gdx.Screen;
import edu.cornell.gdiac.graphics.SpriteBatch;

/**
 * Controls which level is currently active.
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
    }

    private static int countLevels(AssetDirectory assets) {
        JsonValue constants = assets.getEntry("platform-constants", JsonValue.class);
        int count = 0;
        while (constants.get("level" + (count + 1)) != null) {
            count++;
        }
        return Math.max(1, count);
    }

    /**
     * Loads a level by number
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
     * Go to next level
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
     * Restart current level
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
}
