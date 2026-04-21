/*
 * LoadingScene.java
 *
 * Asset loading is a really tricky problem. If you have a lot of sound or
 * images, it can take a long time to decompress them and load them into memory.
 * If you just have code at the start to load all your assets, your game will
 * look like it is hung at the start.
 *
 * The alternative is asynchronous asset loading. In asynchronous loading, you
 * load a little bit of the assets at a time, but still animate the game while
 * you are loading. This way the player knows the game is not hung, even though
 * he or she cannot do anything until loading is complete. You know those
 * loading screens with the inane tips that want to be helpful? That is
 * asynchronous loading.
 *
 * This player mode provides a basic loading screen. While you could adapt it
 * for between level loading, it is currently designed for loading all assets
 * at the start of the game.
 *
 * @author: Walker M. White
 * @date: 11/21/2024
 */
package edu.cornell.cis3152.physics.screen;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ScreenUtils;
import edu.cornell.cis3152.physics.CanvasRender;
import edu.cornell.cis3152.physics.graphics.GifFrames;
import edu.cornell.gdiac.assets.*;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.TextAlign;
import edu.cornell.gdiac.graphics.TextLayout;
import edu.cornell.gdiac.util.ScreenListener;

import java.io.IOException;

/**
 * Class that provides a loading screen for the state of the game.
 *
 * Main menu and asset loading: background, animated title {@link GifFrames}, progress bar, then PLAY / OPTIONS / QUIT.
 */
public class LoadingScene implements Screen {

    public static final int MENU_PLAY = 0;
    public static final int MENU_OPTIONS = 1;
    public static final int MENU_QUIT = 2;

    private static final String[] MENU_LABELS = { "PLAY", "OPTIONS", "QUIT" };
    private static final Color MENU_SELECTED = new Color(0.85f, 0.72f, 0.15f, 1f);
    private static final Color MENU_UNSELECTED = new Color(0.88f, 0.88f, 0.85f, 1f);
    private static final Color OPTIONS_SCRIM = new Color(0f, 0f, 0f, 0.55f);
    /** Default budget for asset loader (do nothing but load 60 fps) */
    private static int DEFAULT_BUDGET = 15;

    private static final String TITLE_GIF_INTERNAL = "loading/GameLogo_animated.gif";
    /** Fallback per-frame duration if a GIF frame omits delay metadata. */
    private static final float TITLE_DEFAULT_FRAME_SEC = 1f / 12f;
    /** Max title width as a fraction of the logical canvas (GIF frames are often huge vs 640×360). */
    private static final float TITLE_MAX_WIDTH_FRAC = 1.0f;
    /** Max title height as a fraction of the logical canvas (2.5× original 0.32 cap). */
    private static final float TITLE_MAX_HEIGHT_FRAC = 0.8f;
    /** Title position: center X as fraction of canvas width (0–1). */
    private static final float TITLE_CENTER_X_FRAC = 0.5f;
    /** Title position: center Y as fraction of canvas height (0–1); lower = more gap below inner menu frame top. */
    private static final float TITLE_CENTER_Y_FRAC = 0.55f;

    // There are TWO asset managers.
    // One to load the loading screen. The other to load the assets
    /** Internal assets for this loading screen */
    private AssetDirectory internal;
    /** The actual assets to be loaded */
    private AssetDirectory assets;

    /** The drawing camera for this scene */
    private OrthographicCamera camera;
    /** Shared sprite batch */
    private SpriteBatch batch;
    /** Shared letterboxed viewport */
    private CanvasRender viewport;
    /** Listener that will update the player mode when we are done */
    private ScreenListener listener;

    /** The width of this scene */
    private int width;
    /** The height of this scene */
    private int height;

    /** The constants for arranging images on the screen */
    JsonValue constants;

    /** Scaling factor for when the student changes the resolution. */
    private float scale;
    /** Current progress (0 to 1) of the asset manager */
    private float progress;
    /** The amount of time to devote to loading assets (as opposed to on screen hints, etc.) */
    private int   budget;

    /** Whether or not this player mode is still active */
    private boolean active;

    private GifFrames titleGif;
    private float titleAnimTime;

    private BitmapFont menuFont;
    private Texture pixel;
    private final TextLayout menuOptionLayout = new TextLayout();
    private final TextLayout optionsStubLayout = new TextLayout();
    private final Vector2 pointer = new Vector2();
    private int menuSelectedIndex;
    private int menuChosenOption = -1;
    private boolean optionsOpen;
    private boolean upPrev;
    private boolean downPrev;
    private boolean confirmPrev;
    private boolean clickPrev;

    /**
     * Returns the budget for the asset loader.
     *
     * The budget is the number of milliseconds to spend loading assets each
     * animation frame. This allows you to do something other than load assets.
     * An animation frame is ~16 milliseconds. So if the budget is 10, you have
     * 6 milliseconds to do something else. This is how game companies animate
     * their loading screens.
     *
     * @return the budget in milliseconds
     */
    public int getBudget() {
        return budget;
    }

    /**
     * Sets the budget for the asset loader.
     *
     * The budget is the number of milliseconds to spend loading assets each
     * animation frame. This allows you to do something other than load assets.
     * An animation frame is ~16 milliseconds. So if the budget is 10, you have
     * 6 milliseconds to do something else. This is how game companies animate
     * their loading screens.
     *
     * @param millis the budget in milliseconds
     */
    public void setBudget(int millis) {
        budget = millis;
    }

    /**
     * Transitions into the main game after {@link #getAssets()} has finished loading.
     * Invoke from the main menu Play button (or equivalent) once that UI exists.
     */
    public void notifyPlayPressed() {
        if (progress < 1.0f || listener == null) {
            return;
        }
        listener.exitScreen(this, 0);
    }

    /**
     * Returns the asset directory produced by this loading screen
     *
     * This asset loader is NOT owned by this loading scene, so it persists even
     * after the scene is disposed. It is your responsbility to unload the
     * assets in this directory.
     *
     * @return the asset directory produced by this loading screen
     */
    public AssetDirectory getAssets() {
        return assets;
    }

    /**
     * Creates a LoadingScene with the default budget, size and position.
     *
     * @param file      The asset directory to load in the background
     * @param batch     The shared sprite batch
     * @param viewport  The shared letterboxed viewport
     */
    public LoadingScene(String file, SpriteBatch batch, CanvasRender viewport) {
        this(file, batch, viewport, DEFAULT_BUDGET);
    }

    /**
     * Creates a LoadingScene with the default size and position.
     *
     * The budget is the number of milliseconds to spend loading assets each animation
     * frame. This allows you to do something other than load assets. An animation
     * frame is ~16 milliseconds. So if the budget is 10, you have 6 milliseconds to
     * do something else. This is how game companies animate their loading screens.
     *
     * @param file      The asset directory to load in the background
     * @param batch The shared sprite batch
     * @param viewport The shared letterboxed viewport
     * @param millis The loading budget in milliseconds
     */
    public LoadingScene(String file, SpriteBatch batch, CanvasRender viewport, int millis) {
        this.batch = batch;
        this.viewport = viewport;
        budget = millis;

        // We need these files loaded immediately
        internal = new AssetDirectory( "loading/boot.json" );
        internal.loadAssets();
        internal.finishLoading();

        constants = internal.getEntry( "constants", JsonValue.class );
        resize(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());

        try {
            titleGif = GifFrames.load(
                    Gdx.files.internal(TITLE_GIF_INTERNAL),
                    TITLE_DEFAULT_FRAME_SEC,
                    Texture.TextureFilter.Nearest);
        } catch (IOException e) {
            Gdx.app.error("LoadingScene", "Could not load title GIF: " + TITLE_GIF_INTERNAL, e);
        }

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();

        menuFont = internal.getEntry("menu", BitmapFont.class);
        menuFont.getData().setScale(0.45f * CanvasRender.layoutScale());
        menuOptionLayout.setFont(menuFont);
        menuOptionLayout.setAlignment(TextAlign.middleCenter);
        optionsStubLayout.setFont(menuFont);
        optionsStubLayout.setAlignment(TextAlign.middleCenter);
        optionsStubLayout.setColor(Color.WHITE);

        progress = 0;

        // Start loading the REAL assets
        assets = new AssetDirectory( file );
        assets.loadAssets();
        active = true;
    }

    /**
     * Called when this screen should release all resources.
     */
    public void dispose() {
        if (titleGif != null) {
            titleGif.dispose();
            titleGif = null;
        }
        if (pixel != null) {
            pixel.dispose();
            pixel = null;
        }
        internal.unloadAssets();
        internal.dispose();
    }

    /**
     * Updates the status of this scene
     *
     * We prefer to separate update and draw from one another as separate
     * methods, instead of using the single render() method that LibGDX does.
     * We will talk about why we prefer this in lecture.
     *
     * @param delta Number of seconds since last animation frame
     */
    private void update(float delta) {
        if (progress < 1.0f) {
            assets.update(budget);
            this.progress = assets.getProgress();
            if (progress >= 1.0f) {
                this.progress = 1.0f;
            }
        }
        titleAnimTime += delta;
        updateMainMenu();
    }

    private void updateMainMenu() {
        if (optionsOpen) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                    || Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                optionsOpen = false;
            }
            return;
        }

        if (progress < 1.0f) {
            return;
        }

        boolean upPressed = Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean downPressed = Gdx.input.isKeyPressed(Input.Keys.DOWN);
        boolean confirmPressed = Gdx.input.isKeyPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyPressed(Input.Keys.SPACE);
        boolean clickPressed = Gdx.input.isButtonPressed(Input.Buttons.LEFT);

        if (upPressed && !upPrev) {
            menuSelectedIndex = (menuSelectedIndex + MENU_LABELS.length - 1) % MENU_LABELS.length;
        }
        if (downPressed && !downPrev) {
            menuSelectedIndex = (menuSelectedIndex + 1) % MENU_LABELS.length;
        }

        if (confirmPressed && !confirmPrev) {
            menuChosenOption = menuSelectedIndex;
        }

        if (clickPressed && !clickPrev) {
            int clicked = getHoveredMenuIndex();
            if (clicked >= 0) {
                menuChosenOption = clicked;
            }
        } else {
            int hovered = getHoveredMenuIndex();
            if (hovered >= 0) {
                menuSelectedIndex = hovered;
            }
        }

        upPrev = upPressed;
        downPrev = downPressed;
        confirmPrev = confirmPressed;
        clickPrev = clickPressed;

        if (menuChosenOption >= 0) {
            int choice = menuChosenOption;
            menuChosenOption = -1;
            switch (choice) {
                case MENU_PLAY -> notifyPlayPressed();
                case MENU_OPTIONS -> optionsOpen = true;
                case MENU_QUIT -> Gdx.app.exit();
                default -> { /* ignore */ }
            }
        }
    }

    /**
     * Draws the status of this player mode.
     *
     * We prefer to separate update and draw from one another as separate
     * methods, instead of using the single render() method that LibGDX does.
     * We will talk about why we prefer this in lecture.
     */
    private void draw() {
        // Cornell colors
        ScreenUtils.clear( 0.0f, 0.0f, 0.0f,1.0f );

        if (camera != null) {
            camera.update();
        }

        viewport.apply();
        batch.begin(camera);
        batch.setColor( Color.WHITE );

        Texture texture = internal.getEntry( "mainMenuBackground", Texture.class );
        batch.draw(texture, 0, 0, width, height);

        drawTitle();

        if (progress < 1.0f) {
            drawProgress();
        } else {
            drawMainMenu();
        }

        if (optionsOpen) {
            drawOptionsStub();
        }

        batch.end();
        viewport.reset();
    }

    /**
     * Updates the progress bar according to loading progress
     *
     * The progress bar is composed of parts: two rounded caps on the end, and
     * a rectangle in a middle. We adjust the size of the rectangle in the
     * middle to represent the amount of progress.
     */
    private void drawTitle() {
        if (titleGif == null || titleGif.getFrameCount() == 0) {
            return;
        }
        TextureRegion tr = titleGif.getKeyFrame(titleAnimTime);
        // GIF frames can differ in size; scale from the *current* frame, not frame 0.
        float rw = Math.max(1, tr.getRegionWidth());
        float rh = Math.max(1, tr.getRegionHeight());
        float maxW = width * TITLE_MAX_WIDTH_FRAC;
        float maxH = height * TITLE_MAX_HEIGHT_FRAC;
        float s = Math.min(maxW / rw, maxH / rh);
        s = MathUtils.clamp(s, 0.01f, 100f);
        float drawW = rw * s;
        float drawH = rh * s;
        float cx = width * TITLE_CENTER_X_FRAC;
        float cy = height * TITLE_CENTER_Y_FRAC;
        float x = cx - drawW / 2f;
        float y = cy - drawH / 2f;
        if (drawW <= width) {
            x = MathUtils.clamp(x, 0f, width - drawW);
        }
        if (drawH <= height) {
            y = MathUtils.clamp(y, 0f, height - drawH);
        }
        batch.setColor(Color.WHITE);
        batch.draw(tr, x, y, drawW, drawH);
    }

    private Rectangle getMenuButtonBounds(int index) {
        float layout = CanvasRender.layoutScale();
        float bw = Math.min(width * 0.42f, 280f * layout);
        float bh = 48f * layout;
        float gap = 10f * layout;
        float totalH = MENU_LABELS.length * bh + (MENU_LABELS.length - 1) * gap;
        float centerY = height * 0.28f;
        float startY = centerY + totalH / 2f;
        float x = (width - bw) / 2f;
        float y = startY - index * (bh + gap) - bh;
        return new Rectangle(x, y, bw, bh);
    }

    private int getHoveredMenuIndex() {
        viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), pointer);
        for (int i = 0; i < MENU_LABELS.length; i++) {
            if (getMenuButtonBounds(i).contains(pointer.x, pointer.y)) {
                return i;
            }
        }
        return -1;
    }

    private void drawMainMenu() {
        if (pixel == null || menuFont == null) {
            return;
        }
        menuFont.getData().setScale(0.45f * CanvasRender.layoutScale());
        float layout = CanvasRender.layoutScale();
        for (int i = 0; i < MENU_LABELS.length; i++) {
            Rectangle b = getMenuButtonBounds(i);
            boolean sel = i == menuSelectedIndex;
            batch.setColor(sel ? MENU_SELECTED : Color.CLEAR);
            batch.draw(pixel, b.x, b.y, b.width, b.height);

            menuOptionLayout.setColor(sel ? Color.WHITE : MENU_UNSELECTED);
            menuOptionLayout.setText(MENU_LABELS[i]);
            menuOptionLayout.layout();
            batch.drawText(menuOptionLayout, width / 2f, b.y + b.height / 2f + 14f * layout);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawOptionsStub() {
        batch.setColor(OPTIONS_SCRIM);
        batch.draw(pixel, 0, 0, width, height);
        batch.setColor(Color.WHITE);
        optionsStubLayout.setText("OPTIONS — COMING SOON   (ESC OR CLICK TO CLOSE)");
        optionsStubLayout.layout();
        batch.drawText(optionsStubLayout, width / 2f, height / 2f);
    }

    private void drawProgress() {
        float w = (int)(constants.getFloat( "bar.width" )*width);
        float cx = width/2;
        float cy = (int)(constants.getFloat( "bar.height" )*height);
        TextureRegion region1, region2, region3;

        // "3-patch" the background
        batch.setColor( Color.WHITE );
        region1 = internal.getEntry( "progress.backleft", TextureRegion.class );
        batch.draw(region1,cx-w/2, cy, scale*region1.getRegionWidth(), scale*region1.getRegionHeight());

        region2 = internal.getEntry( "progress.backright", TextureRegion.class );
        batch.draw(region2,cx+w/2-scale*region2.getRegionWidth(), cy,
                scale*region2.getRegionWidth(), scale*region2.getRegionHeight());

        region3 = internal.getEntry( "progress.background", TextureRegion.class );
        batch.draw(region3, cx-w/2+scale*region1.getRegionWidth(), cy,
                w-scale*(region2.getRegionWidth()+region1.getRegionWidth()),
                scale*region3.getRegionHeight());

        // "3-patch" the foreground
        region1 = internal.getEntry( "progress.foreleft", TextureRegion.class );
        batch.draw(region1,cx-w/2, cy,scale*region1.getRegionWidth(), scale*region1.getRegionHeight());

        if (progress > 0) {
            region2 = internal.getEntry( "progress.foreright", TextureRegion.class );
            float span = progress*(w-scale*(region1.getRegionWidth()+region2.getRegionWidth()));

            batch.draw( region2,cx-w/2+scale*region1.getRegionWidth()+span, cy,
                    scale*region2.getRegionWidth(), scale*region2.getRegionHeight());

            region3 = internal.getEntry( "progress.foreground", TextureRegion.class );
            batch.draw(region3, cx-w/2+scale*region1.getRegionWidth(), cy,
                        span, scale*region3.getRegionHeight());
        } else {
            region2 = internal.getEntry( "progress.foreright", TextureRegion.class );
            batch.draw(region2, cx-w/2+scale*region1.getRegionWidth(), cy,
                    scale*region2.getRegionWidth(), scale*region2.getRegionHeight());
        }

    }

    // ADDITIONAL SCREEN METHODS
    /**
     * Called when the Screen should render itself.
     *
     * We defer to the other methods update() and draw(). However, it is VERY
     * important that we only quit AFTER a draw.
     *
     * @param delta Number of seconds since last animation frame
     */
    public void render(float delta) {
        if (active) {
            update(delta);
            draw();
        }
    }

    /**
     * Called when the Screen is resized.
     *
     * This can happen at any point during a non-paused state but will never
     * happen before a call to show().
     *
     * @param width  The new width in pixels
     * @param height The new height in pixels
     */
    public void resize(int width, int height) {
        // Compute the drawing scale
        float canvasHeight = viewport.getHeight();
        scale = canvasHeight/constants.getFloat( "height" );

        this.width  = (int)viewport.getWidth();
        this.height = (int)viewport.getHeight();
        if (camera == null) {
            camera = new OrthographicCamera(this.width,this.height);
         } else {
            camera.setToOrtho( false, this.width, this.height  );
        }
    }

    /**
     * Called when the Screen is paused.
     *
     * This is usually when it's not active or visible on screen. An Application
     * is also paused before it is destroyed.
     */
    public void pause() {
        // TODO Auto-generated method stub

    }

    /**
     * Called when the Screen is resumed from a paused state.
     *
     * This is usually when it regains focus.
     */
    public void resume() {
        // TODO Auto-generated method stub

    }

    /**
     * Called when this screen becomes the current screen for a Game.
     */
    public void show() {
        // Useless if called in outside animation loop
        active = true;
    }

    /**
     * Called when this screen is no longer the current screen for a Game.
     */
    public void hide() {
        // Useless if called in outside animation loop
        active = false;
    }

    /**
     * Sets the ScreenListener for this mode
     *
     * The ScreenListener will respond to requests to quit.
     */
    public void setScreenListener(ScreenListener listener) {
        this.listener = listener;
    }

}
