package edu.cornell.cis3152.physics.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import edu.cornell.cis3152.physics.CanvasRender;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.TextAlign;
import edu.cornell.gdiac.graphics.TextLayout;

/**
 * Pause overlay: sprite buttons matching the main menu (idle slightly smaller / bright; hover full size / darker).
 */
public class PauseMenuScene implements Screen {

    public static final int RESUME = 0;
    public static final int RESTART = 1;
    public static final int QUIT = 2;
    public static final int HOW_TO_PLAY = 3;

    private static final int BTN_RESTART = 0;
    private static final int BTN_MENU = 1;
    private static final int BTN_HELP = 2;
    private static final int MENU_BUTTON_COUNT = 3;

    /** Same visual language as {@link LoadingScene} main menu. */
    private static final float PAUSE_BTN_MAX_WIDTH_FRAC = 0.38f;
    private static final float PAUSE_BTN_SCALE = 0.55f;
    private static final float PAUSE_BTN_GAP_REF = 15f;
    private static final float MENU_IDLE_SCALE = 0.92f;
    private static final Color MENU_HOVER_TINT = new Color(0.52f, 0.52f, 0.55f, 1f);

    private CanvasRender viewport;
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private BitmapFont font;
    private Texture pixel;
    private final Texture pauseResume;
    private final Texture pauseRestart;
    private final Texture pauseHelp;
    private final Texture pauseMenu;
    private final Texture exitButton;

    private final Texture pauseText;
    private final Texture borderWooden;
    private boolean active;

    private int width;
    private int height;

    private int selectedIndex;
    private int chosenOption = -1;

    private boolean upPrev;
    private boolean downPrev;
    private boolean confirmPrev;
    private boolean escapePrev;
    private boolean clickPrev;
    private final Vector2 pointer = new Vector2();

    private final TextLayout titleLayout;
    private final TextLayout optionLayout;
    private static final float PANEL_W_FRAC = 5 / 6f;
    private static final float PANEL_H_FRAC = 5 / 6f;
    private static final float EXIT_W_FRAC = 0.09f;

    /** Shared with main-menu options help; keep in sync with pause “How to play”. */
    public static final String HOW_TO_PLAY_TITLE = "HOW TO PLAY";
    public static final String[] HOW_TO_PLAY_LINES = {
            "A / D  —  Move",
            "W  —  Jump",
            "LEFT CLICK  —  Take picture",
            "RIGHT CLICK  —  Remove picture",
            "CLICK INVENTORY  —  Select & stick photo",
            "TAB  —  Toggle range",
            "Q  —  Drop photo",
            "P  —  Pause"
    };

    public PauseMenuScene(AssetDirectory assets, SpriteBatch batch, CanvasRender viewport) {
        this.viewport = viewport;
        this.batch = batch;
        this.font = assets.getEntry("shared-retro", BitmapFont.class);

        this.pauseResume = assets.getEntry("shared-pause-resume", Texture.class);
        this.pauseRestart = assets.getEntry("shared-pause-restart", Texture.class);
        this.pauseHelp = assets.getEntry("shared-pause-help", Texture.class);
        this.pauseMenu = assets.getEntry("shared-pause-menu", Texture.class);
        this.pauseText = assets.getEntry("shared-pause-text", Texture.class);
        this.exitButton = assets.getEntry("shared-exit-button-options", Texture.class);

        this.borderWooden = assets.getEntry("shared-border-wooden", Texture.class);
        for (Texture t : new Texture[] {pauseResume, pauseRestart, pauseHelp, pauseMenu}) {
            if (t != null) {
                t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            }
        }
        this.camera = new OrthographicCamera();

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();
        font.getData().setScale(0.5f * CanvasRender.layoutScale());
        titleLayout = new TextLayout();
        titleLayout.setFont(font);
        titleLayout.setAlignment(TextAlign.middleCenter);
        titleLayout.setColor(Color.WHITE);

        optionLayout = new TextLayout();
        optionLayout.setFont(font);
        optionLayout.setAlignment(TextAlign.middleCenter);

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private Texture textureForMenuIndex(int index) {
        return switch (index) {
            case BTN_RESTART -> pauseRestart;
            case BTN_MENU -> pauseMenu;
            case BTN_HELP -> pauseHelp;
            default -> pauseRestart;
        };
    }

    /** Maps menu row index to {@link #consumeChoice()} value, or -1 for Help (opens overlay). */
    private int choiceForMenuIndex(int menuIndex) {
        if (menuIndex == BTN_RESTART) {
            return RESTART;
        }
        if (menuIndex == BTN_MENU) {
            return QUIT;
        }
        if (menuIndex == BTN_HELP) {
            return HOW_TO_PLAY;
        }
        return -1;
    }

    public int consumeChoice() {
        int result = chosenOption;
        chosenOption = -1;
        return result;
    }

    @Override
    public void show() {
        active = true;
        selectedIndex = 0;
    }

    @Override
    public void hide() {
        active = false;
    }

    @Override
    public void render(float delta) {
        if (!active) {
            return;
        }
        update();
        draw();
    }

    private void update() {
        float UI = CanvasRender.layoutScale();
        boolean upPressed = Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean downPressed = Gdx.input.isKeyPressed(Input.Keys.DOWN);
        boolean confirmPressed = Gdx.input.isKeyPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyPressed(Input.Keys.SPACE);
        boolean escapePressed = Gdx.input.isKeyPressed(Input.Keys.ESCAPE);
        boolean clickPressed = Gdx.input.isButtonPressed(Input.Buttons.LEFT);

        if (upPressed && !upPrev) {
            selectedIndex = (selectedIndex + MENU_BUTTON_COUNT - 1) % MENU_BUTTON_COUNT;
        }
        if (downPressed && !downPrev) {
            selectedIndex = (selectedIndex + 1) % MENU_BUTTON_COUNT;
        }

        if (escapePressed && !escapePrev) {
            chosenOption = RESUME;
        }

        if (confirmPressed && !confirmPrev) {
            chosenOption = choiceForMenuIndex(selectedIndex);
        }

        if (clickPressed && !clickPrev) {
            viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), pointer);
            if (getExitBounds().contains(pointer.x, pointer.y)) {
                chosenOption = RESUME;
            } else {
                int clicked = getHoveredMenuIndex(UI);
                if (clicked >= 0) {
                    chosenOption = choiceForMenuIndex(clicked);
                }
            }
        } else {
            int hovered = getHoveredMenuIndex(UI);
            if (hovered >= 0) {
                selectedIndex = hovered;
            }
        }


        upPrev = upPressed;
        downPrev = downPressed;
        confirmPrev = confirmPressed;
        escapePrev = escapePressed;
        clickPrev = clickPressed;
    }

    private void draw() {
        float UI = CanvasRender.layoutScale();
        font.getData().setScale(0.5f * UI);
        camera.update();
        viewport.apply();
        batch.begin(camera);

        batch.setColor(new Color(0.2f, 0.45f, 0.2f, 0.6f));
        batch.draw(pixel, 0, 0, width, height);


        drawMenuButtons(UI);
        drawExit();


        batch.end();
        viewport.reset();
    }

    private void drawMenuButtons(float UI) {
        float scaleWood = 5/6f;
        float drawW = width * scaleWood;
        float drawH = height * scaleWood;
        float wx = (width  - drawW) / 2f;
        float wy = (height - drawH) / 2f;

        batch.setColor(Color.WHITE);
        batch.draw(borderWooden,wx, wy,drawW,drawH);

        if (pauseText != null) {
            float maxW = width * 0.3f;
            float scale = maxW / pauseText.getWidth();
            float tw = pauseText.getWidth() * scale;
            float th = pauseText.getHeight() * scale;
            batch.setColor(Color.WHITE);
            batch.draw(pauseText, (width - tw) / 2f, height * 0.7f, tw, th);

        }

        int mouseHover = getHoveredMenuIndex(UI);
        for (int i = 0; i < MENU_BUTTON_COUNT; i++) {
            Rectangle b = getMenuButtonBounds(i, UI);
            Texture tex = textureForMenuIndex(i);
            if (tex == null) {
                continue;
            }
            boolean hover = mouseHover == i;
            float tScale = hover ? 1f : MENU_IDLE_SCALE;
            float dw = b.width * tScale * 0.87f;
            float dh = b.height * tScale * 0.87f;
            float dx = b.x + (b.width - dw) / 2f;
            float dy = b.y + (b.height - dh) / 2f;
            batch.setColor(hover ? MENU_HOVER_TINT : Color.WHITE);
            batch.draw(tex, dx, dy, dw, dh);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawExit() {
        if (exitButton == null) return;
        viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), pointer);
        Rectangle eb = getExitBounds();
        boolean hov = eb.contains(pointer.x, pointer.y);
        batch.setColor(hov ? new Color(0.75f, 0.75f, 0.75f, 1f) : Color.WHITE);
        batch.draw(exitButton, eb.x, eb.y, eb.width, eb.height);
        batch.setColor(Color.WHITE);
    }

    private Rectangle getMenuButtonBounds(int index, float UI) {
        Texture tex = textureForMenuIndex(index);
        if (tex == null) {
            return new Rectangle(0, 0, 0, 0);
        }
        float maxW = width * PAUSE_BTN_MAX_WIDTH_FRAC * PAUSE_BTN_SCALE;
        float gap = PAUSE_BTN_GAP_REF * UI;
        float[] hArr = new float[MENU_BUTTON_COUNT];
        float[] wArr = new float[MENU_BUTTON_COUNT];
        float totalH = 0f;
        for (int i = 0; i < MENU_BUTTON_COUNT; i++) {
            Texture t = textureForMenuIndex(i);
            float tw = t != null ? t.getWidth() : 1;
            float th = t != null ? t.getHeight() : 1;
            float s = maxW / Math.max(1f, tw);
            wArr[i] = tw * s;
            hArr[i] = th * s;
            totalH += hArr[i];
            if (i < MENU_BUTTON_COUNT - 1) {
                totalH += gap;
            }
        }
        float yTop = height * 0.45f + totalH * 0.5f;
        float yCursor = yTop;
        for (int j = 0; j < index; j++) {
            yCursor -= hArr[j] + gap;
        }
        float bw = wArr[index];
        float bh = hArr[index];
        float x = (width - bw) / 2f;
        float y = yCursor - bh;
        return new Rectangle(x, y, bw, bh);
    }

    private Rectangle getPanelBounds() {
        float pw = width * PANEL_W_FRAC;
        float ph = height * PANEL_H_FRAC;
        return new Rectangle((width - pw) / 2f, (height - ph) / 2f, pw, ph);
    }

    private Rectangle getExitBounds() {
        Rectangle panel = getPanelBounds();
        float ew = panel.width * EXIT_W_FRAC;
        float eh = (exitButton != null) ? ew * exitButton.getHeight() / Math.max(1, exitButton.getWidth()) : ew;
        float ex = panel.x + panel.width - ew * 1.3f;
        float ey = panel.y + panel.height - eh * 0.9f;
        return new Rectangle(ex, ey, ew, eh);
    }

    private int getHoveredMenuIndex(float UI) {
        viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), pointer);
        for (int i = 0; i < MENU_BUTTON_COUNT; i++) {
            if (getMenuButtonBounds(i, UI).contains(pointer.x, pointer.y)) {
                return i;
            }
        }
        return -1;
    }



    @Override
    public void resize(int width, int height) {
        this.width = (int) viewport.getWidth();
        this.height = (int) viewport.getHeight();
        camera.setToOrtho(false, this.width, this.height);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        pixel.dispose();
    }
}
