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
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.TextAlign;
import edu.cornell.gdiac.graphics.TextLayout;

public class PauseMenuScene implements Screen {

    public static final int RESUME      = 0;
    public static final int RESTART     = 1;
    public static final int QUIT        = 2;

    private CanvasRender viewport;
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private BitmapFont font;
    private Texture pixel;
    private boolean active;

    private int width;
    private int height;

    private static final String[] LABELS = { "RESUME", "RESTART LEVEL", "HOW TO PLAY", "QUIT TO MENU" };
    private int selectedIndex = 0;
    private int chosenOption  = -1;

    private boolean showingControls = false;

    private boolean upPrev, downPrev, confirmPrev, escapePrev, clickPrev;
    private final Vector2 pointer = new Vector2();

    private final TextLayout titleLayout;
    private final TextLayout optionLayout;

    private static final String[] CONTROLS = {
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
        this.font   = assets.getEntry("shared-nunito-extra-bold", BitmapFont.class);
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

    public int consumeChoice() {
        int result = chosenOption;
        chosenOption = -1;
        return result;
    }

    @Override
    public void show() { active = true; selectedIndex = 0; showingControls = false; }

    @Override
    public void hide() { active = false; }

    @Override
    public void render(float delta) {
        if (!active) return;
        update();
        draw();
    }

    private void update() {
        boolean upPressed      = Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean downPressed    = Gdx.input.isKeyPressed(Input.Keys.DOWN);
        boolean confirmPressed = Gdx.input.isKeyPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyPressed(Input.Keys.SPACE);
        boolean escapePressed  = Gdx.input.isKeyPressed(Input.Keys.ESCAPE);
        boolean clickPressed   = Gdx.input.isButtonPressed(Input.Buttons.LEFT);

        if (showingControls) {
            if (escapePressed && !escapePrev) {
                showingControls = false;
            }
            if (clickPressed && !clickPrev && isBackButtonHovered(CanvasRender.layoutScale())) {
                showingControls = false;
            }
        } else {
            if (upPressed && !upPrev)
                selectedIndex = (selectedIndex + LABELS.length - 1) % LABELS.length;
            if (downPressed && !downPrev)
                selectedIndex = (selectedIndex + 1) % LABELS.length;

            if (escapePressed && !escapePrev)
                chosenOption = RESUME;

            if (confirmPressed && !confirmPrev) {
                if (selectedIndex == 2) {
                    showingControls = true;
                } else {
                    chosenOption = selectedIndex < 2 ? selectedIndex : selectedIndex - 1;
                }
            }

            if (clickPressed && !clickPrev) {
                int clicked = getHoveredIndex(CanvasRender.layoutScale());
                if (clicked == 2) {
                    showingControls = true;
                } else if (clicked >= 0) {
                    chosenOption = clicked < 2 ? clicked : clicked - 1;
                }
            } else {
                int hovered = getHoveredIndex(CanvasRender.layoutScale());
                if (hovered >= 0) selectedIndex = hovered;
            }
        }

        upPrev = upPressed; downPrev = downPressed;
        confirmPrev = confirmPressed; escapePrev = escapePressed;
        clickPrev = clickPressed;
    }

    private void draw() {
        float UI = CanvasRender.layoutScale();
        font.getData().setScale(0.5f * UI);
        camera.update();
        viewport.apply();
        batch.begin(camera);

        float panelWidth  = width;
        float panelHeight = height;
        float panelX = 0;
        float panelY = 0;

        batch.setColor(new Color(0.2f, 0.45f, 0.2f, 0.6f));
        batch.draw(pixel, panelX, panelY, panelWidth, panelHeight);

        if (showingControls) {
            drawControls(UI);
        } else {
            drawButtons(panelHeight, UI);
        }

        batch.end();
        viewport.reset();
    }

    private void drawButtons(float panelHeight, float UI) {
        titleLayout.setText("PAUSED");
        titleLayout.layout();
        batch.drawText(titleLayout, width / 2f, (float) (panelHeight * 0.85));

        for (int i = 0; i < LABELS.length; i++) {
            Rectangle b = getButtonBounds(i, UI);
            boolean selected = i == selectedIndex;
            batch.setColor(selected
                    ? new Color(0.80f, 0.31f, 0.18f, 1f)
                    : new Color(0.0f, 0.0f, 0.0f, 0f));
            batch.draw(pixel, b.x, b.y, b.width, b.height);

            optionLayout.setColor(selected ? Color.WHITE : new Color(0.84f, 0.84f, 0.80f, 1f));
            optionLayout.setText(LABELS[i]);
            optionLayout.layout();
            batch.drawText(optionLayout, width / 2f, b.y + b.height / 2f + 18f * UI);
        }
    }

    private void drawControls(float UI) {
        titleLayout.setText("HOW TO PLAY");
        titleLayout.layout();
        batch.drawText(titleLayout, width / 2f, height * 0.85f);

        float cy = height * 0.75f;
        float lineStep = 40f * UI;
        for (String line : CONTROLS) {
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
    }

    private Rectangle getButtonBounds(int index, float UI) {
        float bw = Math.min(width * 0.35f, 350f * UI);
        float bh = 60f * UI;
        float gap = 15f * UI;
        float totalH = LABELS.length * bh + (LABELS.length - 1) * gap;
        float startY = height / 2f + totalH / 2f;
        float x = (width - bw) / 2f;
        float y = startY - index * (bh + gap) - bh;
        return new Rectangle(x, y, bw, bh);
    }

    private Rectangle getBackButtonBounds(float UI) {
        float m = 40f * UI;
        return new Rectangle(m, m, 160f * UI, 50f * UI);
    }

    private boolean isBackButtonHovered(float UI) {
        viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), pointer);
        return getBackButtonBounds(UI).contains(pointer.x, pointer.y);
    }

    private int getHoveredIndex(float UI) {
        viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), pointer);
        for (int i = 0; i < LABELS.length; i++) {
            if (getButtonBounds(i, UI).contains(pointer.x, pointer.y)) return i;
        }
        return -1;
    }

    @Override
    public void resize(int width, int height) {
        this.width  = (int) viewport.getWidth();
        this.height = (int) viewport.getHeight();
        camera.setToOrtho(false, this.width, this.height);
    }

    @Override public void pause()   {}
    @Override public void resume()  {}
    @Override public void dispose() {
        pixel.dispose();
    }
}
