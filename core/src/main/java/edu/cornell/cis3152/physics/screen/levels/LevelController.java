package edu.cornell.cis3152.physics.screen.levels;
import edu.cornell.cis3152.physics.GameCanvas;
import edu.cornell.cis3152.physics.screen.PhysicsScene;
import edu.cornell.gdiac.assets.AssetDirectory;
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

    /** Shared game canvas */
    private GameCanvas canvas;

    /** Current level screen */
    private PhysicsScene currentScene;

    /** Base level screen */
    private LevelBaseScene baseScene;

    /**
     * Constructor
     */
    public LevelController(AssetDirectory assets, GameCanvas canvas) {
        this.assets = assets;
        this.canvas = canvas;

        currentLevel = 1;
        totalLevels = 3;

        baseScene = new LevelBaseScene(assets);

        loadLevel(currentLevel);
    }

    /**
     * Loads a level by number
     */
    public void loadLevel(int level) {

//        switch(level) {
//            case 1:
//                currentScene = new Level1Scene(assets);
//                break;
//
//            case 2:
//                currentScene = new Level2Scene(assets);
//                break;
//
//            default:
//                System.out.println("no level");
//                return;
//        }
//        if (level <= totalLevels)
//        {
//            baseScene.setLevel(level);
//        }
//        else
//        {
//            System.out.println("no level");
//            return;
//        }

        if (level <= totalLevels)
        {
            currentScene = new LevelBaseScene(assets);
        }
        else
        {
            System.out.println("no level");
            return;
        }

        currentScene.setCanvas(canvas);
        currentScene.show();
        currentScene.resize(com.badlogic.gdx.Gdx.graphics.getWidth(), com.badlogic.gdx.Gdx.graphics.getHeight());
//        baseScene.setCanvas(canvas);
//        baseScene.show();
//        baseScene.resize(com.badlogic.gdx.Gdx.graphics.getWidth(), com.badlogic.gdx.Gdx.graphics.getHeight());
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
}
