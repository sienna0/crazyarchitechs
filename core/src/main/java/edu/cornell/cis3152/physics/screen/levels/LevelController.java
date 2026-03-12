package edu.cornell.cis3152.physics.screen.levels;
import edu.cornell.cis3152.physics.screen.PhysicsScene;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import com.badlogic.gdx.Screen;

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

    /** Shared batch */
    private SpriteBatch batch;

    /** Current level screen */
    private Screen currentScene;

    private PhysicsScene currentPhysics;

    /**
     * Constructor
     */
    public LevelController(AssetDirectory assets, SpriteBatch batch) {
        this.assets = assets;
        this.batch = batch;

        currentLevel = 1;
        totalLevels = 2;

        loadLevel(currentLevel);
    }

    /**
     * Loads a level by number
     */
    public void loadLevel(int level) {

        switch(level) {
            case 1:
                currentScene = new Level1Scene(assets); // add batch
                break;

            case 2:
                currentScene = new Level2Scene(assets); // add batch

//                controllers[1] = new Level2Scene(directory);
//
//                for(int ii = 0; ii < controllers.length; ii++) {
//                    controllers[ii].setScreenListener(this);
//                    controllers[ii].setSpriteBatch(batch);
//                }
//
                break;

            default:
                System.out.println("no level");
        }

        currentLevel = level;
    }

    /**
     * Go to next level
     */
    public void nextLevel() {
        if (currentLevel < totalLevels) {
            loadLevel(currentLevel + 1);
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
    public Screen getCurrentScene() {
        return currentScene;
    }

    /**
     * Get current level number
     */
    public int getCurrentLevel() {
        return currentLevel;
    }
}
