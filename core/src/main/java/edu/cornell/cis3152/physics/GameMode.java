package edu.cornell.cis3152.physics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.ScreenUtils;

import edu.cornell.cis3152.physics.InputController;
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
public class GameMode implements Screen {

    private AssetDirectory assets;
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private ScreenListener listener;
    private LevelController levelController;


    private int width;
    private int height;

    private boolean active;

    private final Texture background;
    private Texture zukoTexture;

//    private float playerX;
////    private float playerY;
////    private float playerSpeed;

    /**
     * Creates the game mode with loaded assets.
     *
     * @param assets the asset directory from LoadingScene
     * @param batch the shared sprite batch
     */
    public GameMode(AssetDirectory assets, SpriteBatch batch) {
        this.assets = assets;
        this.batch = batch;
        // this.levelController = new LevelController();

        camera = new OrthographicCamera();
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Pull textures from the asset directory
        // These keys must match your json asset file
        background = assets.getEntry("background", Texture.class);
        zukoTexture = assets.getEntry("zuko", Texture.class);

//        playerX = 100;
//        playerY = 100;
//        playerSpeed = 250.0f;

        active = true;
    }

    /**
     * Sets the screen listener for this mode.
     *
     * @param listener the listener for screen transitions
     */
    public void setScreenListener(ScreenListener listener) {
        this.listener = listener;
    }

    /**
     * Updates the game state.
     *
     * @param delta time since last frame
     */
    private void update(float delta) {
        InputController input = InputController.getInstance();

        // step the physics world
        // process collisions
        // update animations
    }

    /**
     * Draws the game world.
     */
    private void draw() {
        ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1.0f);

        batch.begin(camera);
        batch.setColor(Color.WHITE);

        // Draw background full-screen
        batch.draw(background, 0, 0, width, height);

        // Draw player
        // batch.draw(playerTexture, playerX, playerY);

        batch.end();
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
        this.width = width;
        this.height = height;

        camera.setToOrtho(false, width, height);
    }

    @Override
    public void show() {
        active = true;
    }

    @Override
    public void hide() {
        active = false;
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
        // Usually do not dispose shared assets here if AssetDirectory owns them
    }
}