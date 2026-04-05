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

public class LoseScene implements Screen {
    public static final int RETRY = 0;
    public static final int QUIT  = 1;

    private static final String[] LABELS = { "TRY AGAIN", "QUIT TO MENU" };

    private static final Color OVERLAY   = new Color(0.28f, 0.05f, 0.05f, 0.78f);
    private static final Color SELECTED  = new Color(0.80f, 0.31f, 0.18f, 1f);  // matches pause menu accent
    private static final Color UNSELECTED = new Color(0.84f, 0.84f, 0.80f, 1f);

    private final CanvasRender viewport;
    private final SpriteBatch  batch;
    private final OrthographicCamera camera;
    private final BitmapFont font;
    private Texture pixel;

    private int width;
    private int height;

    private boolean active;
    private int selectedIndex = 0;
    private int chosenOption = -1;
    private String currentPun = "";

    private boolean upPrev, downPrev, confirmPrev, escapePrev, clickPrev;
    private final Vector2 pointer = new Vector2();

    private final TextLayout titleLayout;
    private final TextLayout punLayout;
    private final TextLayout optionLayout;

    public LoseScene(AssetDirectory assets, SpriteBatch batch, CanvasRender viewport) {
        this.viewport = viewport;
        this.batch = batch;
        this.font = assets.getEntry("shared-retro", BitmapFont.class);
        this.camera = new OrthographicCamera();

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();

        font.getData().setScale(0.5f);

        titleLayout = new TextLayout();
        titleLayout.setFont(font);
        titleLayout.setAlignment(TextAlign.middleCenter);
        titleLayout.setColor(new Color(0.95f, 0.30f, 0.20f, 1f));

        punLayout = new TextLayout();
        punLayout.setFont(font);
        punLayout.setAlignment(TextAlign.middleCenter);
        punLayout.setColor(new Color(0.80f, 0.70f, 0.70f, 1f));

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
    public void show() {
        if (active) return;
        active        = true;
        selectedIndex = 0;
        chosenOption  = -1;
        currentPun    = FrogPuns.randomLose();
    }

    @Override public void hide()  { active = false; }

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

        if (upPressed   && !upPrev)   selectedIndex = (selectedIndex + LABELS.length - 1) % LABELS.length;
        if (downPressed && !downPrev) selectedIndex = (selectedIndex + 1) % LABELS.length;

        if (escapePressed && !escapePrev) chosenOption = RETRY;

        if (confirmPressed && !confirmPrev) chosenOption = selectedIndex;

        if (clickPressed && !clickPrev) {
            int clicked = getHoveredIndex();
            if (clicked >= 0) chosenOption = clicked;
        } else {
            int hovered = getHoveredIndex();
            if (hovered >= 0) selectedIndex = hovered;
        }

        upPrev = upPressed; downPrev = downPressed;
        confirmPrev = confirmPressed; escapePrev = escapePressed;
        clickPrev = clickPressed;
    }

    private void draw() {
        font.getData().setScale(0.5f);
        camera.update();
        viewport.apply();
        batch.begin(camera);

        batch.setColor(OVERLAY);
        batch.draw(pixel, 0, 0, width, height);

        titleLayout.setText("YOU CROAKED.");
        titleLayout.layout();
        batch.drawText(titleLayout, width / 2f, height * 0.82f);

        punLayout.setText(currentPun);
        punLayout.layout();
        batch.drawText(punLayout, width / 2f, height * 0.70f);

        for (int i = 0; i < LABELS.length; i++) {
            Rectangle b = getButtonBounds(i);
            boolean sel = i == selectedIndex;
            batch.setColor(sel ? SELECTED : new Color(0f, 0f, 0f, 0f));
            batch.draw(pixel, b.x, b.y, b.width, b.height);

            optionLayout.setColor(sel ? Color.WHITE : UNSELECTED);
            optionLayout.setText(LABELS[i]);
            optionLayout.layout();
            batch.drawText(optionLayout, width / 2f, b.y + b.height / 2f + 18f);
        }

        batch.end();
        viewport.reset();
    }

    private Rectangle getButtonBounds(int index) {
        float bw = Math.min(width * 0.35f, 350f);
        float bh = 60f;
        float gap = 15f;
        float totalH = LABELS.length * bh + (LABELS.length - 1) * gap;
        float startY = height / 2f + totalH / 2f;
        float x = (width - bw) / 2f;
        float y = startY - index * (bh + gap) - bh;
        return new Rectangle(x, y, bw, bh);
    }

    private int getHoveredIndex() {
        viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), pointer);
        for (int i = 0; i < LABELS.length; i++) {
            if (getButtonBounds(i).contains(pointer.x, pointer.y)) return i;
        }
        return -1;
    }

    @Override
    public void resize(int w, int h) {
        this.width  = (int) viewport.getWidth();
        this.height = (int) viewport.getHeight();
        camera.setToOrtho(false, this.width, this.height);
    }

    @Override public void pause()   {}
    @Override public void resume()  {}
    @Override
    public void dispose() {
        pixel.dispose();
    }
}
