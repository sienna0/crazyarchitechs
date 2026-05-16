package edu.cornell.cis3152.physics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.OrthographicCamera;
import edu.cornell.cis3152.physics.screen.*;
import edu.cornell.cis3152.physics.screen.levels.LevelBaseScene;
import edu.cornell.cis3152.physics.screen.levels.LevelController;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.audio.SoundEffect;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.util.ScreenListener;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

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
    private HowToPlayScene howToPlayScene;
    private GameplayOptionsOverlay gameplayOptionsOverlay;
    private WinScene winScene;

    private boolean transitioning = false;
    private float transitionTimer = 0f;
    private static final float TRANSITION_DURATION = 0.5f;
    private boolean transitionSwapped = false;
    private int pendingLevel = -1;
    private boolean transitioningToLevelSelect = false;

    private boolean deathTransitioning = false;
    private float deathTransitionTimer = 0f;
    private boolean deathRestarted = false;
    private static final float DEATH_TRANSITION_DURATION = 0.7f;

    private Texture pixel;

    private Music levelIntro;
    private Music levelLoopA;
    private Music levelLoopB;
    private boolean usingA = true;
    private static final float LOOP_RESTART_TIME = 72.0f; // start crossfade 2s before end
    private float crossfadeTimer = -1f; // -1 means not crossfading
    private boolean introPlayed = false;
    private static final float INTRO_TO_LOOP_TIME = 18.0f;
    private boolean loopStarted = false;
    private boolean musicWasOn = true;
    private float loopPlayTime = 0f;
    private static final float LOOP_MIN_PLAY_TIME = 5.0f; // ignore "finished" in first 5s

    private boolean fadingTitleToLevel = false;
    private float musicFadeTimer = 0f;
    private static final float MUSIC_FADE_DURATION = 0.2f;

    private int width;
    private int height;

    private boolean active;
    private boolean showingLevelSelect;
    private boolean paused;
    private boolean showingWin;
    /** Level select Menu/Esc: defer {@link ScreenListener#exitScreen} until after {@link #draw()} (avoid disposing mid-render). */
    private boolean pendingReturnToTitle;
    private boolean showingHowToPlay;

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
        howToPlayScene = new HowToPlayScene(assets, batch, viewport);

        camera = new OrthographicCamera();
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        active = true;
        showingLevelSelect = true;
        stopLevelMusic();
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixel = new Texture(pixmap);
        pixmap.dispose();
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
            stopLevelMusic();
            GameAudio.startTitleMusic(assets);
            levelSelectScene.show();
        } else if (exitCode == PhysicsScene.EXIT_WIN) {
            //levelController.markCurrentBeaten();
            levelController.nextLevel();
            levelController.setScreenListener(this);
            showingLevelSelect = false;
        } else if (exitCode == PhysicsScene.EXIT_LOSE) {
            startDeathTransition();
        }

    }

    /**
     * Updates the game state.
     *
     * @param delta time since last frame
     */
    private void update(float delta) {
        boolean musicNowOn = GameAudio.isMusicOn();
        if (fadingTitleToLevel) {
            musicFadeTimer += delta;
            float t = Math.min(musicFadeTimer / MUSIC_FADE_DURATION, 1f);

            GameAudio.setTitleMusicVolume(GameAudio.getMusicVolume() * (1f - t));
            setLevelMusicVolume(GameAudio.getMusicVolume() * t);

            if (t >= 1f) {
                fadingTitleToLevel = false;
                GameAudio.stopTitleMusic();
                setLevelMusicVolume(GameAudio.getMusicVolume());
            }
        }
        if (musicNowOn != musicWasOn) {
            musicWasOn = musicNowOn;

            if (!musicNowOn) {
                stopLevelMusic();
            } else if (!showingLevelSelect) {
                startLevelMusic();
                fadingTitleToLevel = true;
                musicFadeTimer = 0f;
            }
        }
        if (GameAudio.isMusicOn() && !showingLevelSelect && levelLoopA != null && loopStarted) {
            Music outgoing = usingA ? levelLoopA : levelLoopB;
            Music incoming = usingA ? levelLoopB : levelLoopA;

            if (outgoing.isPlaying()) {
                loopPlayTime += delta;
            }

            if (loopPlayTime >= LOOP_RESTART_TIME) {
                incoming.setVolume(outgoing.getVolume());
                incoming.play();
                outgoing.stop();
                usingA = !usingA;
                loopPlayTime = 0f;
            }
        }
        if (!showingLevelSelect
                && levelIntro != null
                && levelLoopA != null
                && levelIntro.isPlaying()
                && !loopStarted
                && levelIntro.getPosition() >= INTRO_TO_LOOP_TIME) {

            levelIntro.stop();
            levelLoopA.setVolume(GameAudio.getMusicVolume());
            levelLoopA.setLooping(false);
            levelLoopA.play();

            loopStarted = true;
            loopPlayTime = 0f;
        }
        if (deathTransitioning) {
            deathTransitionTimer += delta;
            float progress = deathTransitionTimer / DEATH_TRANSITION_DURATION;

            if (!deathRestarted && progress >= 0.5f) {
                deathRestarted = true;
                levelController.restartCurrentLevel();

                if (levelController.getCurrentScene() != null) {
                    levelController.getCurrentScene().show();
                    levelController.getCurrentScene().setGamePaused(true);
                }
            }

            if (progress >= 1f) {
                deathTransitioning = false;

                if (levelController.getCurrentScene() != null) {
                    levelController.getCurrentScene().setGamePaused(false);
                }
            }

            return;
        }

        if (transitioning) {
            transitionTimer += delta;
            float progress = transitionTimer / TRANSITION_DURATION;

            if (!transitionSwapped && progress >= 0.5f) {
                transitionSwapped = true;

                SoundEffect shutter = assets.getEntry("platform-plop", SoundEffect.class);
                if (shutter != null) {
                    shutter.play(GameAudio.effectiveSfxVolume(0.3f));
                }

                if (transitioningToLevelSelect) {
                    stopLevelMusic();
                    GameAudio.startTitleMusic(assets);
                    showingLevelSelect = true;
                    levelSelectScene.show();
                } else {
                    levelController.loadLevel(pendingLevel);
                    levelController.setScreenListener(this);
                    showingLevelSelect = false;
                    startLevelMusic();
                    fadingTitleToLevel = true;
                    musicFadeTimer = 0f;

                    if (levelController.getCurrentScene() != null) {
                        levelController.getCurrentScene().show();
                    }
                }

                if (levelController.getCurrentScene() != null) {
                    levelController.getCurrentScene().show();
                }
            }

            if (progress >= 1f) {
                transitioning = false;
            }

            return;
        }
        if (showingLevelSelect) {
            GameAudio.updateTitleMusic(); // add this
            int selectedLevel = levelSelectScene.consumeChosenLevel();
            if (selectedLevel > 0) {
                transitioning = true;
                transitionTimer = 0f;
                transitionSwapped = false;
                transitioningToLevelSelect = false;
                pendingLevel = selectedLevel;
            } else if (levelSelectScene.consumeExitRequested() && listener != null) {
                pendingReturnToTitle = true;
            } else if (levelSelectScene.consumeHowToPlayRequested()) {
                showingHowToPlay = true;
                levelSelectScene.hide();
                howToPlayScene.show(HowToPlayScene.Origin.LEVEL_SELECT);
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
                    paused = false;
                    pauseMenuScene.hide();
                    startDeathTransition();
                }
                if (choice == PauseMenuScene.QUIT)    {
                    paused = false;
                    pauseMenuScene.hide();

                    transitioning = true;
                    transitionTimer = 0f;
                    transitionSwapped = false;
                    transitioningToLevelSelect = true;
                }
                if (choice == PauseMenuScene.HOW_TO_PLAY)    {
                    pauseMenuScene.hide();
                    showingHowToPlay = true;
                    howToPlayScene.show(HowToPlayScene.Origin.PAUSE_MENU);
                }
            }
            PhysicsScene currentScene = levelController.getCurrentScene();
            if (currentScene instanceof LevelBaseScene levelScene) {
                if (levelScene.consumeWinTriggered()) {
                    showingWin = true;
                    winScene.setScore(levelScene.getCurrentScore());
                    winScene.setStats(levelScene.getFlyCount(), levelScene.getPhotosUsed());
                    winScene.show();
                    levelScene.setGamePaused(true);
                }
            }
            if (showingWin) {
                int choice = winScene.consumeChoice();
                if (choice == WinScene.NEXT_LEVEL) {
                    showingWin = false;
                    winScene.hide();
                    levelController.nextLevel();
                    levelController.setScreenListener(this);
                }
                else if (choice == WinScene.QUIT) {
                    showingWin = false;
                    winScene.hide();

                    transitioning = true;
                    transitionTimer = 0f;
                    transitionSwapped = false;
                    transitioningToLevelSelect = true;
                }
                if (choice == WinScene.RESTART) {
                    showingWin = false;
                    winScene.hide();
                    startDeathTransition();
                }
            }
            if (!blockPauseForOptions && currentScene instanceof LevelBaseScene levelScene && levelScene.consumeHazardRestart()) {
                startDeathTransition();
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
                currentScene.setGamePaused(true);
                winScene.render(Gdx.graphics.getDeltaTime());
                return;
            }

        }
        if (transitioning) {
            drawShutterTransition();
        }
        if (deathTransitioning) {
            drawDeathWashTransition();
        }
    }

    private void drawShutterTransition() {
        float t = transitionTimer / TRANSITION_DURATION;

        float bladeT;
        if (t < 0.5f) {
            bladeT = t / 0.5f;
        } else {
            bladeT = 1f - ((t - 0.5f) / 0.5f);
        }

        bladeT = bladeT * bladeT;

        float travel = (height / 2f) * bladeT;

        batch.begin(camera);

        batch.setColor(0.08f, 0.12f, 0.10f, 1f);
        batch.draw(pixel, 0, height - travel, width, travel);
        batch.draw(pixel, 0, 0, width, travel);

        batch.setColor(1, 1, 1, 1);
        batch.end();
    }

    private void drawDeathWashTransition() {
        float t = Math.min(deathTransitionTimer / DEATH_TRANSITION_DURATION, 1f);

        float wipeWidth = width * 1.25f;
        float x = -wipeWidth + t * (width + wipeWidth);

        batch.begin(camera);

        batch.setColor(0f, 0f, 0f, 1f);
        batch.draw(pixel, x, 0, wipeWidth, height);

        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();
    }

    private void startDeathTransition() {
        if (deathTransitioning) {
            return;
        }

        deathTransitioning = true;
        deathTransitionTimer = 0f;
        deathRestarted = false;

        if (levelController.getCurrentScene() != null) {
            levelController.getCurrentScene().setGamePaused(true);
        }
    }

    private void startLevelMusic() {
        if (!GameAudio.isMusicOn()) {
            return;
        }
        if (levelIntro == null) {
            levelIntro = assets.getEntry("platform-backgroundintroost", Music.class);
            levelLoopA = assets.getEntry("platform-backgroundost", Music.class);
            levelLoopB = assets.getEntry("platform-backgroundost2", Music.class);

            levelLoopA.setLooping(false);
            levelLoopB.setLooping(false);  // was missing
            GameAudio.registerMusic(levelIntro);
            GameAudio.registerMusic(levelLoopA);
            GameAudio.registerMusic(levelLoopB);
        }

        levelIntro.stop();
        levelLoopA.stop();
        levelLoopB.stop();   // was missing
        levelIntro.setVolume(GameAudio.getMusicVolume());
        levelLoopA.setVolume(GameAudio.getMusicVolume());
        levelLoopB.setVolume(GameAudio.getMusicVolume());

        levelIntro.play();

        loopStarted = false;
        loopPlayTime = 0f;       // was missing
        crossfadeTimer = -1f;    // was missing
        usingA = true;           // was missing
    }

    private void stopLevelMusic() {
        if (levelIntro != null) levelIntro.stop();
        if (levelLoopA != null) { levelLoopA.stop(); }
        if (levelLoopB != null) { levelLoopB.stop(); }
        introPlayed = false;
        loopStarted = false;
        loopPlayTime = 0f;
        crossfadeTimer = -1f;
        usingA = true;
    }

    private void setLevelMusicVolume(float volume) {
        if (levelIntro != null) levelIntro.setVolume(volume);
        if (levelLoopA != null) levelLoopA.setVolume(volume);
        if (levelLoopB != null) levelLoopB.setVolume(volume);
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

            if (showingHowToPlay) {
                if (showingLevelSelect) {
                    levelSelectScene.render(0f);
                } else {
                    draw();
                }
                howToPlayScene.render(delta);
                if (howToPlayScene.isExiting()) {
                    howToPlayScene.consumeExit();
                    showingHowToPlay = false;
                    if (howToPlayScene.getOrigin() == HowToPlayScene.Origin.PAUSE_MENU) {
                        paused = true;
                        pauseMenuScene.show();

                    }
                    else if (howToPlayScene.getOrigin() == HowToPlayScene.Origin.LEVEL_SELECT)  {
                        showingLevelSelect = true;
                        levelSelectScene.show();
                    }

                }
            } else {
                draw();
            }
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
        if (winScene != null) {
            winScene.resize(width, height);
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
        if (levelIntro != null) {
            levelIntro.stop();
            levelIntro = null;
        }
        if (levelLoopA != null) {
            levelLoopA.stop();
            levelLoopA = null;
        }
        if (pixel != null) {
            pixel.dispose();
            pixel = null;
        }
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
