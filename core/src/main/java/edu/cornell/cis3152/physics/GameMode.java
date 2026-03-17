package edu.cornell.cis3152.physics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import edu.cornell.cis3152.physics.screen.PhysicsScene;
import edu.cornell.cis3152.physics.screen.LevelSelectScene;
import edu.cornell.cis3152.physics.screen.levels.LevelController;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.util.ScreenListener;
import edu.cornell.cis3152.physics.screen.PauseMenuScene;

/**
 * A simple example gameplay screen.
 *
 * This class shows the general structure of a GameMode:
 * - store loaded assets
 * - process input
 * - update game state
 * - draw game state
 */
public class GameMode implements Screen, ScreenListener {

    private AssetDirectory assets;
    private GameCanvas canvas;
    private OrthographicCamera camera;
    private ScreenListener listener;
    private LevelController levelController;
    private LevelSelectScene levelSelectScene;
    private PauseMenuScene pauseMenuScene;


    private int width;
    private int height;

    private boolean active;
    private boolean showingLevelSelect;
    private boolean paused;

//    private float playerX;
////    private float playerY;
////    private float playerSpeed;

    /**
     * Creates the game mode with loaded assets.
     *
     * @param assets the asset directory from LoadingScene
     * @param canvas the shared game canvas
     */
    public GameMode(AssetDirectory assets, GameCanvas canvas) {
        this.assets = assets;
        this.canvas = canvas;
        this.levelController = new LevelController(assets, canvas);
        this.levelSelectScene = new LevelSelectScene(assets, canvas, levelController.getTotalLevels());
        pauseMenuScene = new PauseMenuScene(assets, canvas);

        camera = new OrthographicCamera();
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        active = true;
        showingLevelSelect = true;
    }

    /**
     * Sets the screen listener for this mode.
     *
     * @param listener the listener for screen transitions
     */
    public void setScreenListener(ScreenListener listener) {
        this.listener = listener;
        if (levelController != null) {
            levelController.setScreenListener(this);
        }
    }

    /**
     * Responds to a request from a child scene.
     *
     * @param screen   The screen requesting to exit
     * @param exitCode The state of the screen upon exit
     */
    public void exitScreen(Screen screen, int exitCode) {
        if (exitCode == PhysicsScene.EXIT_NEXT) {
            levelController.nextLevel();
            levelController.setScreenListener(this);
            showingLevelSelect = false;
        } else if (exitCode == PhysicsScene.EXIT_PREV) {
            levelController.loadLevel(Math.max(1, levelController.getCurrentLevel()-1));
            levelController.setScreenListener(this);
            showingLevelSelect = false;
        } else if (exitCode == PhysicsScene.EXIT_QUIT) {
            showingLevelSelect = true;
            levelSelectScene.show();
        }
    }

    /**
     * Updates the game state.
     *
     * @param delta time since last frame
     */
    private void update(float delta) {
        if (showingLevelSelect) {
            int selectedLevel = levelSelectScene.consumeChosenLevel();
            if (selectedLevel > 0) {
                levelController.loadLevel(selectedLevel);
                levelController.setScreenListener(this);
                showingLevelSelect = false;
                if (levelController.getCurrentScene() != null) {
                    levelController.getCurrentScene().show();
                }
            } else if (levelSelectScene.consumeExitRequested() && listener != null) {
                listener.exitScreen(this, 0);
            }
        } else {
            if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
                paused = !paused;
                if (paused) {
                    pauseMenuScene.show();
                    levelController.getCurrentScene().setGamePaused(true);
                }
                else {
                    pauseMenuScene.hide();
                    levelController.getCurrentScene().setGamePaused(false);

                }
            }
            if (paused) {
                int choice = pauseMenuScene.consumeChoice();
                if (choice == PauseMenuScene.RESUME)  {
                    paused = false;
                    pauseMenuScene.hide();
                    levelController.getCurrentScene().setGamePaused(false);

                }
                if (choice == PauseMenuScene.RESTART) {
                    levelController.loadLevel(levelController.getCurrentLevel());
                    levelController.getCurrentScene().setGamePaused(false);
                    paused = false;
                }
                if (choice == PauseMenuScene.QUIT)    {
                    paused = false;
                    showingLevelSelect = true;
                    levelSelectScene.show();
                }
            }
        }
    }

    /**
     * Draws the game world.
     */
    private void draw() {
        if (showingLevelSelect) {
            levelSelectScene.render(Gdx.graphics.getDeltaTime());
        } else {
            PhysicsScene currentScene = levelController.getCurrentScene();
            if (currentScene != null) {
                currentScene.render(Gdx.graphics.getDeltaTime());
            }
            if (paused) {
                pauseMenuScene.render(Gdx.graphics.getDeltaTime());
            }
        }
    }

    /**
     * Called every frame.
     *
     * @param delta seconds since last frame
     */
    @Override
    public void render(float delta) {
        if (active) {
            update(delta);
            draw();
        }
    }

    /**
     * Called when the screen is resized.
     *
     * @param width new width
     * @param height new height
     */
    @Override
    public void resize(int width, int height) {
        this.width = (int)canvas.getWidth();
        this.height = (int)canvas.getHeight();

        camera.setToOrtho(false, this.width, this.height);
        if (levelSelectScene != null) {
            levelSelectScene.resize(width, height);
        }
        if (pauseMenuScene != null) {
            pauseMenuScene.resize(width, height);
        }
        if (levelController != null && levelController.getCurrentScene() != null) {
            levelController.getCurrentScene().resize(width, height);
        }
    }

    @Override
    public void show() {
        active = true;
        if (showingLevelSelect && levelSelectScene != null) {
            levelSelectScene.show();
        } else if (levelController != null && levelController.getCurrentScene() != null) {
            levelController.getCurrentScene().show();
        }
    }

    @Override
    public void hide() {
        active = false;
        if (levelSelectScene != null) {
            levelSelectScene.hide();
        }
        if (levelController != null && levelController.getCurrentScene() != null) {
            levelController.getCurrentScene().hide();
        }
    }

    @Override
    public void pause() {
        // Optional pause logic
    }

    @Override
    public void resume() {
        // Optional resume logic
    }

    @Override
    public void dispose() {
        if (levelSelectScene != null) {
            levelSelectScene.dispose();
            levelSelectScene = null;
        }
    }
}
