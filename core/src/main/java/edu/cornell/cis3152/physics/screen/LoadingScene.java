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
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ScreenUtils;
import edu.cornell.cis3152.physics.CanvasRender;
import edu.cornell.cis3152.physics.GameAudio;
import edu.cornell.cis3152.physics.graphics.GifFrames;
import edu.cornell.gdiac.assets.*;
import edu.cornell.gdiac.audio.SoundEffect;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.TextAlign;
import edu.cornell.gdiac.graphics.TextLayout;
import edu.cornell.gdiac.util.ScreenListener;

import java.io.IOException;

/**
 * Class that provides a loading screen for the state of the game.
 *
 * Main menu and asset loading: background, animated title {@link GifFrames}, progress bar, then sprite PLAY / OPTIONS.
 */
public class LoadingScene implements Screen {

    public static final int MENU_PLAY = 0;
    public static final int MENU_OPTIONS = 1;
    private static final int MENU_BUTTON_COUNT = 2;

    /** Hard cap: max button width vs logical canvas (inner menu panel scale). */
    private static final float MENU_BUTTON_PANEL_MAX_WIDTH_FRAC = 0.34f;
    /** Extra linear scale on button size (1 = use cap as-is; 0.6 = 60% width/height). */
    private static final float MENU_BUTTON_SCALE = 0.6f;
    /** Also never wider than this fraction of the title’s fitted bbox (when a title exists). */
    private static final float MENU_BUTTON_WIDTH_VS_TITLE = 0.98f;
    /** If no title GIF, same as panel cap. */
    private static final float MENU_BUTTON_FALLBACK_MAX_WIDTH_FRAC = MENU_BUTTON_PANEL_MAX_WIDTH_FRAC;
    /** Gap between title bottom and top of PLAY stack (canvas pixels, scaled). */
    private static final float MENU_GAP_BELOW_TITLE = 12f;
    /** Gap between PLAY and OPTIONS (canvas pixels, scaled). */
    private static final float MENU_GAP_BETWEEN = 22f;
    /** Extra shift downward for PLAY/OPTIONS stack (reference px × {@link CanvasRender#layoutScale()}, y-up). */
    private static final float MENU_BUTTON_EXTRA_DOWN_REF = 22f;
    /** Min bottom edge for OPTIONS when the stack would clip off-screen (fraction of canvas height, y-up). */
    private static final float MENU_BUTTON_STACK_AREA_BOTTOM_FRAC = 0.10f;
    /** Fallback: vertical center of stack when title is missing (y-up, 0–1). */
    private static final float MENU_STACK_CENTER_Y_FRAC = 0.30f;
    /** Main menu sprites: slightly smaller when idle; full size when hovered or keyboard-selected. */
    private static final float MENU_MAIN_IDLE_SCALE = 0.92f;
    /** Darken on hover (keyboard or mouse). */
    private static final Color MENU_MAIN_HOVER_TINT = new Color(0.52f, 0.52f, 0.55f, 1f);
    private static final Color OPTIONS_SCRIM = new Color(0f, 0f, 0f, 0.55f);
    /** Options → How to play: panel behind text (RGB + alpha). */
    private static final Color HELP_PANEL_FILL = new Color(0.07f, 0.09f, 0.11f, 0.88f);
    private static final Color HELP_PANEL_BORDER = new Color(0.42f, 0.45f, 0.50f, 0.72f);
    private static final int OPT_MUSIC = 0;
    private static final int OPT_SOUND = 1;
    private static final int OPT_HELP = 2;
    /** Default budget for asset loader (do nothing but load 60 fps) */
    private static int DEFAULT_BUDGET = 15;

    private static final String TITLE_GIF_INTERNAL = "loading/GameLogo_animated.gif";
    /** Fallback per-frame duration if a GIF frame omits delay metadata. */
    private static final float TITLE_DEFAULT_FRAME_SEC = 1f / 12f;
    /**
     * Max title width/height vs canvas. After opaque crop the logo is wide; a full-width cap blows up scale
     * and steals vertical room from the menu stack.
     */
    private static final float TITLE_MAX_WIDTH_FRAC = 0.72f;
    private static final float TITLE_MAX_HEIGHT_FRAC = 0.38f;
    /** Title position: center X as fraction of canvas width (0–1). */
    private static final float TITLE_CENTER_X_FRAC = 0.5f;
    /** Title position: center Y as fraction of canvas height (0–1); higher = title + menu stack move up. */
    private static final float TITLE_CENTER_Y_FRAC = 0.72f;

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

    private SoundEffect buttonPress;

    private BitmapFont menuFont;
    private Texture pixel;
    private final TextLayout optionsStubLayout = new TextLayout();
    /** {@link TextLayout#getWidth()} does not match rendered line width for help text; measure with {@link GlyphLayout}. */
    private final GlyphLayout helpPanelMeasure = new GlyphLayout();
    private final Vector2 pointer = new Vector2();
    private int menuSelectedIndex;
    private int menuChosenOption = -1;
    private boolean optionsOpen;
    private boolean optionsHelpOpen;
    private boolean mainAssetsFinalized;
    /** Set when Play is chosen; {@link ScreenListener#exitScreen} runs after {@link #draw()} this frame. */
    private boolean pendingExitToGame;
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
        playButtonPress();
        assets.finishLoading();
        pendingExitToGame = true;
    }

    /**
     * Reset menu UI when returning from {@link GameMode} without disposing this screen (avoids reloading boot assets / title GIF).
     */
    public void prepareReturnFromGame() {
        optionsOpen = false;
        optionsHelpOpen = false;
        menuChosenOption = -1;
        menuSelectedIndex = MENU_PLAY;
        pendingExitToGame = false;
        upPrev = false;
        downPrev = false;
        confirmPrev = false;
        clickPrev = false;
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
        initBoot(batch, viewport, millis);
        progress = 0;
        assets = new AssetDirectory(file);
        assets.loadAssets();
        active = true;
    }

    /**
     * Title screen when main game assets are already loaded (e.g. return from level select).
     * Skips the progress bar and async load.
     */
    public LoadingScene(AssetDirectory preloadedMainAssets, SpriteBatch batch, CanvasRender viewport, int millis) {
        initBoot(batch, viewport, millis);
        assets = preloadedMainAssets;
        progress = 1.0f;
        mainAssetsFinalized = true;
        active = true;
    }

    private void initBoot(SpriteBatch batch, CanvasRender viewport, int millis) {
        this.batch = batch;
        this.viewport = viewport;
        budget = millis;

        internal = new AssetDirectory("loading/boot.json");
        internal.loadAssets();
        internal.finishLoading();

        constants = internal.getEntry("constants", JsonValue.class);
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

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
        optionsStubLayout.setFont(menuFont);
        optionsStubLayout.setAlignment(TextAlign.middleCenter);
        optionsStubLayout.setColor(Color.WHITE);
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
        if (progress >= 1.0f && !mainAssetsFinalized) {
            assets.finishLoading();
            mainAssetsFinalized = true;
        }
        titleAnimTime += delta;
        updateMainMenu();
    }

    private void updateMainMenu() {
        if (optionsOpen) {
            updateOptionsOverlay();
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
            menuSelectedIndex = (menuSelectedIndex + MENU_BUTTON_COUNT - 1) % MENU_BUTTON_COUNT;
        }
        if (downPressed && !downPrev) {
            menuSelectedIndex = (menuSelectedIndex + 1) % MENU_BUTTON_COUNT;
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
                case MENU_OPTIONS -> {
                    playButtonPress();
                    optionsOpen = true;
                }
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
        } else if (!optionsOpen) {
            drawMainMenu();
        }

        if (optionsOpen) {
            drawOptionsOverlay();
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
        Rectangle r = computeTitleCanvasRect();
        if (r.width <= 0f) {
            return;
        }
        TextureRegion tr = titleGif.getKeyFrame(titleAnimTime);
        batch.setColor(Color.WHITE);
        batch.draw(tr, r.x, r.y, r.width, r.height);
    }

    /**
     * Title quad in canvas space (bottom-left origin, y-up), matching {@link #drawTitle()}.
     * Width/height are zero if there is no title.
     */
    private Rectangle computeTitleCanvasRect() {
        if (titleGif == null || titleGif.getFrameCount() == 0) {
            return new Rectangle(0, 0, 0, 0);
        }
        TextureRegion tr = titleGif.getKeyFrame(titleAnimTime);
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
        return new Rectangle(x, y, drawW, drawH);
    }

    private Rectangle getMenuButtonBounds(int index) {
        Texture ref = internal.getEntry("menuPlay", Texture.class);
        int tw = ref.getWidth();
        int th = ref.getHeight();
        float layout = CanvasRender.layoutScale();
        Rectangle titleR = computeTitleCanvasRect();
        float panelCap = width * MENU_BUTTON_PANEL_MAX_WIDTH_FRAC;
        float maxW = panelCap;
        if (titleR.width > 1f) {
            maxW = Math.min(panelCap, titleR.width * MENU_BUTTON_WIDTH_VS_TITLE);
        } else {
            maxW = width * MENU_BUTTON_FALLBACK_MAX_WIDTH_FRAC;
        }
        maxW = Math.min(maxW, width * 0.98f);
        maxW *= MENU_BUTTON_SCALE;
        float s = maxW / tw;
        float bw = tw * s;
        float bh = th * s;
        float gapBelowTitle = MENU_GAP_BELOW_TITLE * layout;
        float gapBetween = MENU_GAP_BETWEEN * layout;
        float nudgeDown = MENU_BUTTON_EXTRA_DOWN_REF * layout;
        float yPlay;
        float yOptions;
        float totalH = MENU_BUTTON_COUNT * bh + (MENU_BUTTON_COUNT - 1) * gapBetween;
        if (titleR.height > 0f) {
            float playTopMax = titleR.y - gapBelowTitle - nudgeDown;
            float minBottom = height * MENU_BUTTON_STACK_AREA_BOTTOM_FRAC;
            float room = playTopMax - minBottom;
            if (room > 1f && totalH > room) {
                float factor = room / totalH;
                bh *= factor;
                bw *= factor;
                totalH = MENU_BUTTON_COUNT * bh + (MENU_BUTTON_COUNT - 1) * gapBetween;
            }
            yPlay = playTopMax - bh;
            yOptions = yPlay - gapBetween - bh;
        } else {
            float centerY = height * MENU_STACK_CENTER_Y_FRAC;
            float startY = centerY + totalH / 2f;
            yPlay = startY - bh - nudgeDown;
            yOptions = yPlay - gapBetween - bh;
        }
        if (yOptions < 4f) {
            float lift = 4f - yOptions;
            yPlay += lift;
            yOptions += lift;
        }
        float anchorX = titleR.width > 0f
                ? titleR.x + titleR.width * 0.5f
                : width * TITLE_CENTER_X_FRAC;
        float x = anchorX - bw * 0.5f;
        x = MathUtils.clamp(x, 0f, Math.max(0f, width - bw));
        float y = (index == MENU_PLAY) ? yPlay : yOptions;
        return new Rectangle(x, y, bw, bh);
    }

    private int getHoveredMenuIndex() {
        viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), pointer);
        for (int i = 0; i < MENU_BUTTON_COUNT; i++) {
            if (getMenuButtonBounds(i).contains(pointer.x, pointer.y)) {
                return i;
            }
        }
        return -1;
    }

    private void drawMainMenu() {
        Texture playTex = internal.getEntry("menuPlay", Texture.class);
        Texture optionsTex = internal.getEntry("menuOptions", Texture.class);
        int mouseHover = getHoveredMenuIndex();
        for (int i = 0; i < MENU_BUTTON_COUNT; i++) {
            Rectangle b = getMenuButtonBounds(i);
            // Only the actual mouse position drives hover styling; keyboard focus uses default look.
            boolean hover = mouseHover == i;
            float tScale = hover ? 1f : MENU_MAIN_IDLE_SCALE;
            float dw = b.width * tScale;
            float dh = b.height * tScale;
            float dx = b.x + (b.width - dw) / 2f;
            float dy = b.y + (b.height - dh) / 2f;
            batch.setColor(hover ? MENU_MAIN_HOVER_TINT : Color.WHITE);
            Texture tex = (i == MENU_PLAY) ? playTex : optionsTex;
            batch.draw(tex, dx, dy, dw, dh);
        }
        batch.setColor(Color.WHITE);
    }

    private void updateOptionsOverlay() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (optionsHelpOpen) {
                optionsHelpOpen = false;
            } else {
                optionsOpen = false;
                optionsHelpOpen = false;
            }
            return;
        }
        if (!Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            return;
        }
        if (optionsHelpOpen) {
            return;
        }
        viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), pointer);
        Rectangle[] r = computeOptionIconBounds();
        if (r[OPT_MUSIC].contains(pointer.x, pointer.y)) {
            GameAudio.toggleMusic();
        } else if (r[OPT_SOUND].contains(pointer.x, pointer.y)) {
            GameAudio.toggleSfx();
        } else if (r[OPT_HELP].contains(pointer.x, pointer.y)) {
            optionsHelpOpen = true;
        }
    }

    private Rectangle[] computeOptionIconBounds() {
        Texture ref = internal.getEntry("optionsMusic", Texture.class);
        int tw = ref.getWidth();
        int th = ref.getHeight();
        float sz = Math.min(width, height) * 0.16f;
        float s = sz / Math.max(1, Math.max(tw, th));
        float wIcon = tw * s;
        float hIcon = th * s;
        float gap = Math.min(width, height) * 0.065f;
        float totalW = 3f * wIcon + 2f * gap;
        float startX = (width - totalW) / 2f;
        float cy = height * 0.48f;
        float y = cy - hIcon / 2f;
        return new Rectangle[] {
                new Rectangle(startX, y, wIcon, hIcon),
                new Rectangle(startX + wIcon + gap, y, wIcon, hIcon),
                new Rectangle(startX + 2f * (wIcon + gap), y, wIcon, hIcon),
        };
    }

    private void drawOptionPuck(Texture tex, Rectangle bounds, boolean hovered, Color baseTint) {
        float tScale = hovered ? 1f : MENU_MAIN_IDLE_SCALE;
        float dw = bounds.width * tScale;
        float dh = bounds.height * tScale;
        float dx = bounds.x + (bounds.width - dw) / 2f;
        float dy = bounds.y + (bounds.height - dh) / 2f;
        Color c = baseTint.cpy();
        if (hovered) {
            c.r *= MENU_MAIN_HOVER_TINT.r;
            c.g *= MENU_MAIN_HOVER_TINT.g;
            c.b *= MENU_MAIN_HOVER_TINT.b;
        }
        batch.setColor(c);
        batch.draw(tex, dx, dy, dw, dh);
        batch.setColor(Color.WHITE);
    }

    private void drawOptionsOverlay() {
        batch.setColor(OPTIONS_SCRIM);
        batch.draw(pixel, 0, 0, width, height);
        batch.setColor(Color.WHITE);
        if (optionsHelpOpen) {
            drawHelpPanel();
            return;
        }
        Rectangle[] r = computeOptionIconBounds();
        Texture musicTex = internal.getEntry("optionsMusic", Texture.class);
        Texture soundOn = internal.getEntry("optionsSoundOn", Texture.class);
        Texture soundOff = internal.getEntry("optionsSoundOff", Texture.class);
        Texture helpTex = internal.getEntry("optionsHelp", Texture.class);
        Texture soundTex = GameAudio.isSfxOn() ? soundOn : soundOff;
        viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), pointer);
        Color musicTint = GameAudio.isMusicOn() ? Color.WHITE : new Color(0.5f, 0.5f, 0.52f, 1f);
        drawOptionPuck(musicTex, r[OPT_MUSIC], r[OPT_MUSIC].contains(pointer.x, pointer.y), musicTint);
        drawOptionPuck(soundTex, r[OPT_SOUND], r[OPT_SOUND].contains(pointer.x, pointer.y), Color.WHITE);
        drawOptionPuck(helpTex, r[OPT_HELP], r[OPT_HELP].contains(pointer.x, pointer.y), Color.WHITE);
        optionsStubLayout.setAlignment(TextAlign.middleCenter);
        optionsStubLayout.setColor(Color.WHITE);
        optionsStubLayout.setText("Press Esc to close");
        optionsStubLayout.layout();
        batch.drawText(optionsStubLayout, width / 2f, height * 0.26f);
    }

    /**
     * Matches {@link PauseMenuScene} control list: same font scale, spacing, and positions (proportional to canvas).
     */
    private void drawHelpPanel() {
        BitmapFont helpFont = null;
        if (assets != null && assets.hasEntry("shared-retro", BitmapFont.class)) {
            helpFont = assets.getEntry("shared-retro", BitmapFont.class);
        }
        if (helpFont == null) {
            helpFont = menuFont;
        }
        float UI = CanvasRender.layoutScale();
        float scaleSave = helpFont.getData().scaleX;
        helpFont.getData().setScale(0.5f * UI);
        optionsStubLayout.setFont(helpFont);
        optionsStubLayout.setAlignment(TextAlign.middleCenter);

        float lineStep = 40f * UI;
        float padX = 28f * UI;
        float padY = 22f * UI;
        float border = Math.max(1.5f, 2f * UI);
        String[] lines = PauseMenuScene.HOW_TO_PLAY_LINES;
        int rowCount = lines.length + 1;
        float totalH = rowCount * lineStep;

        helpPanelMeasure.setText(helpFont, PauseMenuScene.HOW_TO_PLAY_TITLE);
        float maxW = helpPanelMeasure.width;
        for (String line : lines) {
            helpPanelMeasure.setText(helpFont, line);
            maxW = Math.max(maxW, helpPanelMeasure.width);
        }

        float panelW = maxW + 2f * padX;
        float maxPanelW = width - 24f * UI;
        if (panelW > maxPanelW) {
            panelW = maxPanelW;
        }
        float panelH = totalH + 2f * padY;
        float panelX = (width - panelW) / 2f;
        float centerY = height * 0.5f;
        float panelY = centerY - panelH / 2f;

        batch.setColor(HELP_PANEL_BORDER);
        batch.draw(pixel, panelX - border, panelY - border, panelW + 2f * border, panelH + 2f * border);
        batch.setColor(HELP_PANEL_FILL);
        batch.draw(pixel, panelX, panelY, panelW, panelH);
        batch.setColor(Color.WHITE);

        float yTitle = centerY + (rowCount - 1) * lineStep * 0.5f;
        optionsStubLayout.setColor(Color.WHITE);
        optionsStubLayout.setText(PauseMenuScene.HOW_TO_PLAY_TITLE);
        optionsStubLayout.layout();
        batch.drawText(optionsStubLayout, width / 2f, yTitle);
        float cy = yTitle - lineStep;
        for (String line : lines) {
            optionsStubLayout.setColor(new Color(0.84f, 0.84f, 0.80f, 1f));
            optionsStubLayout.setText(line);
            optionsStubLayout.layout();
            batch.drawText(optionsStubLayout, width / 2f, cy);
            cy -= lineStep;
        }

        helpFont.getData().setScale(scaleSave);
        optionsStubLayout.setFont(menuFont);
        optionsStubLayout.setColor(Color.WHITE);
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
        if (pendingExitToGame && listener != null) {
            pendingExitToGame = false;
            listener.exitScreen(this, 0);
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
     * Plays button press
     */
    private void playButtonPress() {
        buttonPress = assets.getEntry("platform-button", SoundEffect.class);
        if (buttonPress != null) {
            buttonPress.play();
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
