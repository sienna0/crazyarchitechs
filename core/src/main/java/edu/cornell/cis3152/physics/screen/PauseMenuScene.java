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

    private static final int BTN_RESUME = 0;
    private static final int BTN_RESTART = 1;
    private static final int BTN_HELP = 2;
    private static final int BTN_MENU = 3;
    private static final int MENU_BUTTON_COUNT = 4;

    /** Same visual language as {@link LoadingScene} main menu. */
    private static final float PAUSE_BTN_MAX_WIDTH_FRAC = 0.38f;
    private static final float PAUSE_BTN_SCALE = 0.55f;
    private static final float PAUSE_BTN_GAP_REF = 20f;
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
    private boolean active;

    private int width;
    private int height;

    private int selectedIndex;
    private int chosenOption = -1;

    private boolean showingControls;

    private boolean upPrev;
    private boolean downPrev;
    private boolean confirmPrev;
    private boolean escapePrev;
    private boolean clickPrev;
    private final Vector2 pointer = new Vector2();

    private final TextLayout titleLayout;
    private final TextLayout optionLayout;

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
            case BTN_RESUME -> pauseResume;
            case BTN_RESTART -> pauseRestart;
            case BTN_HELP -> pauseHelp;
            case BTN_MENU -> pauseMenu;
            default -> pauseResume;
        };
    }

    /** Maps menu row index to {@link #consumeChoice()} value, or -1 for Help (opens overlay). */
    private int choiceForMenuIndex(int menuIndex) {
        if (menuIndex == BTN_RESUME) {
            return RESUME;
        }
        if (menuIndex == BTN_RESTART) {
            return RESTART;
        }
        if (menuIndex == BTN_MENU) {
            return QUIT;
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
        showingControls = false;
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

        if (showingControls) {
            if (escapePressed && !escapePrev) {
                showingControls = false;
            }
            if (clickPressed && !clickPrev && isBackButtonHovered(UI)) {
                showingControls = false;
            }
        } else {
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
                if (selectedIndex == BTN_HELP) {
                    showingControls = true;
                } else {
                    chosenOption = choiceForMenuIndex(selectedIndex);
                }
            }

            if (clickPressed && !clickPrev) {
                int clicked = getHoveredMenuIndex(UI);
                if (clicked == BTN_HELP) {
                    showingControls = true;
                } else if (clicked >= 0) {
                    chosenOption = choiceForMenuIndex(clicked);
                }
            } else {
                int hovered = getHoveredMenuIndex(UI);
                if (hovered >= 0) {
                    selectedIndex = hovered;
                }
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

        if (showingControls) {
            drawControls(UI);
        } else {
            drawMenuButtons(UI);
        }

        batch.end();
        viewport.reset();
    }

    private void drawMenuButtons(float UI) {
        titleLayout.setText("PAUSED");
        titleLayout.layout();
        batch.drawText(titleLayout, width / 2f, height * 0.88f);

        int mouseHover = getHoveredMenuIndex(UI);
        for (int i = 0; i < MENU_BUTTON_COUNT; i++) {
            Rectangle b = getMenuButtonBounds(i, UI);
            Texture tex = textureForMenuIndex(i);
            if (tex == null) {
                continue;
            }
            boolean hover = mouseHover == i;
            float tScale = hover ? 1f : MENU_IDLE_SCALE;
            float dw = b.width * tScale;
            float dh = b.height * tScale;
            float dx = b.x + (b.width - dw) / 2f;
            float dy = b.y + (b.height - dh) / 2f;
            batch.setColor(hover ? MENU_HOVER_TINT : Color.WHITE);
            batch.draw(tex, dx, dy, dw, dh);
        }
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
        float yTop = height * 0.52f + totalH * 0.5f;
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

    private int getHoveredMenuIndex(float UI) {
        viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), pointer);
        for (int i = 0; i < MENU_BUTTON_COUNT; i++) {
            if (getMenuButtonBounds(i, UI).contains(pointer.x, pointer.y)) {
                return i;
            }
        }
        return -1;
    }

    private void drawControls(float UI) {
        titleLayout.setText(HOW_TO_PLAY_TITLE);
        titleLayout.layout();
        batch.drawText(titleLayout, width / 2f, height * 0.85f);

        float cy = height * 0.75f;
        float lineStep = 40f * UI;
        for (String line : HOW_TO_PLAY_LINES) {
            optionLayout.setColor(new Color(0.84f, 0.84f, 0.80f, 1f));
            optionLayout.setText(line);
            optionLayout.layout();
            batch.drawText(optionLayout, width / 2f, cy);
            cy -= lineStep;
        }

        Rectangle back = getBackButtonBounds(UI);
        boolean hovered = isBackButtonHovered(UI);
        batch.setColor(hovered
                ? new Color(0.80f, 0.31f, 0.18f, 1f)
                : new Color(0.23f, 0.29f, 0.33f, 0.9f));
        batch.draw(pixel, back.x, back.y, back.width, back.height);

        optionLayout.setColor(Color.WHITE);
        optionLayout.setText("< BACK");
        optionLayout.layout();
        batch.drawText(optionLayout, back.x + back.width / 2f, back.y + back.height / 2f + 18f * UI);
        batch.setColor(Color.WHITE);
    }

    private Rectangle getBackButtonBounds(float UI) {
        float m = 40f * UI;
        return new Rectangle(m, m, 160f * UI, 50f * UI);
    }

    private boolean isBackButtonHovered(float UI) {
        viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), pointer);
        return getBackButtonBounds(UI).contains(pointer.x, pointer.y);
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
