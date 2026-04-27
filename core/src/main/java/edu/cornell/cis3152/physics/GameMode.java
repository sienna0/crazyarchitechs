package edu.cornell.cis3152.physics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import edu.cornell.cis3152.physics.screen.*;
import edu.cornell.cis3152.physics.screen.levels.LevelBaseScene;
import edu.cornell.cis3152.physics.screen.levels.LevelController;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.util.ScreenListener;

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
    private SpriteBatch batch;
    private CanvasRender viewport;
    private OrthographicCamera camera;
    private ScreenListener listener;
    private LevelController levelController;
    private LevelSelectScene levelSelectScene;
    private PauseMenuScene pauseMenuScene;
    private GameplayOptionsOverlay gameplayOptionsOverlay;
    private WinScene winScene;

    private int width;
    private int height;

    private boolean active;
    private boolean showingLevelSelect;
    private boolean paused;
    private boolean showingWin;
    /** Level select Menu/Esc: defer {@link ScreenListener#exitScreen} until after {@link #draw()} (avoid disposing mid-render). */
    private boolean pendingReturnToTitle;

//    private float playerX;
////    private float playerY;
////    private float playerSpeed;

    /**
     * Creates the game mode with loaded assets.
     *
     * @param assets the asset directory from LoadingScene
     * @param batch the shared sprite batch
     * @param viewport the shared letterboxed viewport
     */
    public GameMode(AssetDirectory assets, SpriteBatch batch, CanvasRender viewport) {
        this.assets = assets;
        this.batch = batch;
        this.viewport = viewport;
        this.levelController = new LevelController(assets, batch, viewport);
        this.levelSelectScene = new LevelSelectScene(assets, batch, viewport, levelController.getTotalLevels(), levelController);
        pauseMenuScene = new PauseMenuScene(assets, batch, viewport);
        gameplayOptionsOverlay = new GameplayOptionsOverlay(assets, batch, viewport);
        winScene = new WinScene(assets, batch, viewport);

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
        } else if (exitCode == PhysicsScene.EXIT_WIN) {
            levelController.markCurrentBeaten();
            levelController.nextLevel();
            levelController.setScreenListener(this);
            showingLevelSelect = false;
            PhysicsScene currentScene = levelController.getCurrentScene();
            if (currentScene instanceof LevelBaseScene levelScene) {
                levelScene.beginEntryFromPreviousLevel();
            }
        } else if (exitCode == PhysicsScene.EXIT_LOSE) {
            levelController.loadLevel(levelController.getCurrentLevel());
            levelController.setScreenListener(this);
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
                pendingReturnToTitle = true;
            }
        } else {
            if (gameplayOptionsOverlay.isOpen()) {
                gameplayOptionsOverlay.update();
                if (!gameplayOptionsOverlay.isOpen() && levelController.getCurrentScene() != null) {
                    levelController.getCurrentScene().setGamePaused(false);
                }
            } else if (levelController.getCurrentScene() != null
                    && levelController.getCurrentScene().consumeSettingsClick()) {
                gameplayOptionsOverlay.show();
                levelController.getCurrentScene().setGamePaused(true);
            }

            boolean blockPauseForOptions = gameplayOptionsOverlay.isOpen();
            boolean pauseToggle = !blockPauseForOptions && Gdx.input.isKeyJustPressed(Input.Keys.P);
            if (!pauseToggle && !blockPauseForOptions) {
                pauseToggle = Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE);
            }
            if (!pauseToggle && !blockPauseForOptions && levelController.getCurrentScene() != null) {
                pauseToggle = levelController.getCurrentScene().consumePauseClick();
            }
            if (pauseToggle) {
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
            if (showingWin) {
                int choice = winScene.consumeChoice();
                if (choice == WinScene.NEXT_LEVEL) {
                    showingWin = false;
                    winScene.hide();
                    levelController.nextLevel();
                    levelController.setScreenListener(this);
                } else if (choice == WinScene.QUIT) {
                    showingWin = false;
                    winScene.hide();
                    showingLevelSelect = true;
                    levelSelectScene.show();
                }
            }
            PhysicsScene currentScene = levelController.getCurrentScene();
            if (!blockPauseForOptions && currentScene instanceof LevelBaseScene levelScene && levelScene.consumeHazardRestart()) {
                levelController.loadLevel(levelController.getCurrentLevel());
                levelController.setScreenListener(this);
                return;
            }
        }
        if (Gdx.input.isKeyPressed(Input.Keys.J)){levelController.getLevelProgress().resetSaveGame();}
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
            if (gameplayOptionsOverlay.isOpen()) {
                gameplayOptionsOverlay.draw();
            }
            if (paused) {
                pauseMenuScene.render(Gdx.graphics.getDeltaTime());
            }
            if (showingWin) {
                winScene.render(Gdx.graphics.getDeltaTime());
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
        if (pendingReturnToTitle && listener != null) {
            pendingReturnToTitle = false;
            listener.exitScreen(this, 0);
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
        this.width = (int)viewport.getWidth();
        this.height = (int)viewport.getHeight();

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
        if (gameplayOptionsOverlay != null) {
            gameplayOptionsOverlay.resize(width, height);
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
        if (levelController != null) {
            levelController.dispose();
            levelController = null;
        }
        if (levelSelectScene != null) {
            levelSelectScene.dispose();
            levelSelectScene = null;
        }
        if (pauseMenuScene != null) {
            pauseMenuScene.dispose();
            pauseMenuScene = null;
        }
        if (gameplayOptionsOverlay != null) {
            gameplayOptionsOverlay.dispose();
            gameplayOptionsOverlay = null;
        }
        if (winScene != null) {
            winScene.dispose();
            winScene = null;
        }
    }
}
