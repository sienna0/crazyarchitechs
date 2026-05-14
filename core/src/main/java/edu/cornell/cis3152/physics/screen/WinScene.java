package edu.cornell.cis3152.physics.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import edu.cornell.cis3152.physics.CanvasRender;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;

/**
 * Win screen shown after completing a level.
 * Displays wooden border panel, "RIBBIT!" title, lotus star score,
 * and three icon buttons: restart, levels, next.
 */
public class WinScene implements Screen {

    public static final int NEXT_LEVEL = 1;
    public static final int QUIT = 2;
    public static final int RESTART = 3;

    private static final float PANEL_W_FRAC = 5 / 6f;
    private static final float PANEL_H_FRAC = 5 / 6f;
    private static final float TITLE_MAX_W_FRAC = 0.3f;
    private static final float TITLE_TOP_INSET_FRAC = 0.19f;

    private static final float LOTUS_SIZE_FRAC = 0.1f;
    private static final float LOTUS_GAP_FRAC = 0.03f;
    private static final float LOTUS_Y_FRAC = 0.62f;

    private static final float BTN_SIZE_FRAC = 0.18f;
    private static final float BTN_GAP_FRAC = 0.06f;
    private static final float BTN_Y_FRAC = 0.28f;

    private static final Color HOVER_TINT = new Color(0.75f, 0.75f, 0.75f, 1f);

    private final CanvasRender viewport;
    private final SpriteBatch batch;
    private final OrthographicCamera camera;
    private final Texture pixel;

    private final Texture borderWooden;
    private final Texture ribbitText;
    private final Texture lotusOn;
    private final Texture lotusOff;
    private final Texture btnRestart;
    private final Texture btnLevels;
    private final Texture btnNext;
    private final Texture fly;
    private final Texture polaroid;

    private boolean active;
    private int score = 0;
    private int chosenOption = -1;

    private boolean clickPrev;
    private final Vector2 pointer = new Vector2();

    private int width;
    private int height;

    private int flyCount = 0;
    private int photosUsed = 0;
    private final Texture[] numbersBlack;


    public WinScene(AssetDirectory assets, SpriteBatch batch, CanvasRender viewport) {
        this.viewport = viewport;
        this.batch = batch;
        this.camera = new OrthographicCamera();

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();

        this.borderWooden = assets.getEntry("shared-full-border-wooden", Texture.class);
        this.ribbitText = assets.getEntry("shared-ribbit-text", Texture.class);
        this.lotusOn = assets.getEntry("shared-lotus", Texture.class);
        this.lotusOff = assets.getEntry("shared-lily-gray", Texture.class);
        this.btnRestart = assets.getEntry("shared-win-restart", Texture.class);
        this.btnLevels = assets.getEntry("shared-win-levels", Texture.class);
        this.btnNext = assets.getEntry("shared-win-next", Texture.class);
        this.fly = assets.getEntry("shared-win-fly", Texture.class);
        this.polaroid = assets.getEntry("shared-win-polaroid", Texture.class);
        this.numbersBlack = new Texture[10];
        for (int i = 0; i < 10; i++) {
            this.numbersBlack[i] = assets.getEntry("numbers-B" + i, Texture.class);
        }
        setNearestFilter(borderWooden, ribbitText, lotusOn, lotusOff, btnRestart, btnLevels, btnNext);

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void setScore(int score) {
        this.score = Math.max(0, Math.min(3, score));
    }

    public int consumeChoice() {
        int result = chosenOption;
        chosenOption = -1;
        return result;
    }

    @Override
    public void show() {
        active = true;
        chosenOption = -1;
        clickPrev = false;
    }

    @Override public void hide() { active = false; }
    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void render(float delta) {
        if (!active) return;
        update();
        draw();
    }

    @Override
    public void resize(int w, int h) {
        this.width = (int) viewport.getWidth();
        this.height = (int) viewport.getHeight();
        camera.setToOrtho(false, this.width, this.height);
    }

    @Override
    public void dispose() {
        pixel.dispose();
    }


    private void update() {
        boolean clickPressed = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), pointer);

        if (clickPressed && !clickPrev) {
            if (getRestartBounds().contains(pointer.x, pointer.y)) {
                chosenOption = RESTART;
            } else if (getLevelsBounds().contains(pointer.x, pointer.y)) {
                chosenOption = QUIT;
            } else if (getNextBounds().contains(pointer.x, pointer.y)) {
                chosenOption = NEXT_LEVEL;
            }
        }

        clickPrev = clickPressed;
    }

    private void draw() {
        camera.update();
        viewport.apply();
        batch.begin(camera);

        batch.setColor(new Color(0f, 0f, 0f, 0.1f));
        batch.draw(pixel, 0, 0, width, height);
        batch.setColor(Color.WHITE);

        Rectangle panel = getPanelBounds();
        batch.draw(borderWooden, panel.x, panel.y, panel.width, panel.height);

        drawTitle(panel);

        drawLotus(panel);
        drawStats(panel);
        drawButtons(panel);

        batch.setColor(Color.WHITE);
        batch.end();
        viewport.reset();
    }

    private void drawTitle(Rectangle panel) {
        if (ribbitText == null) return;
        float maxW = panel.width * TITLE_MAX_W_FRAC;
        float scale = maxW / ribbitText.getWidth();
        float tw = ribbitText.getWidth() * scale;
        float th = ribbitText.getHeight() * scale;
        float tx = panel.x + (panel.width - tw) / 2f;
        float ty = panel.y + panel.height - panel.height * TITLE_TOP_INSET_FRAC - th;
        batch.setColor(Color.WHITE);
        batch.draw(ribbitText, tx, ty, tw, th);
    }

    private void drawLotus(Rectangle panel) {
        float lotusSize = panel.width * LOTUS_SIZE_FRAC;
        float lotusGap = panel.width * LOTUS_GAP_FRAC;
        float totalW = 3 * lotusSize + 2 * lotusGap;
        float startX = panel.x + (panel.width - totalW) / 2f;
        float cy = panel.y + panel.height * LOTUS_Y_FRAC;

        for (int i = 0; i < 3; i++) {
            float sx = startX + i * (lotusSize + lotusGap);
            float sy = cy - lotusSize / 2f;
            Texture t = (i < score) ? lotusOn : lotusOff;
            if (t != null) {
                batch.setColor(Color.WHITE);
                batch.draw(t, sx, sy, lotusSize, lotusSize);
            }
        }
    }

    private void drawButtons(Rectangle panel) {
        viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), pointer);

        Rectangle rb = getRestartBounds();
        Rectangle lb = getLevelsBounds();
        Rectangle nb = getNextBounds();

        drawBtn(btnRestart, rb);
        drawBtn(btnLevels, lb);
        drawBtn(btnNext, nb);
    }

    private void drawBtn(Texture tex, Rectangle b) {
        if (tex == null) return;
        boolean hov = b.contains(pointer.x, pointer.y);
        batch.setColor(hov ? HOVER_TINT : Color.WHITE);
        batch.draw(tex, b.x, b.y, b.width, b.height);
        batch.setColor(Color.WHITE);
    }


    private Rectangle getPanelBounds() {
        float pw = width * PANEL_W_FRAC;
        float ph = height * PANEL_H_FRAC;
        return new Rectangle((width - pw) / 2f, (height - ph) / 2f, pw, ph);
    }

    private Rectangle getRestartBounds() { return getButtonBounds(0); }
    private Rectangle getLevelsBounds() { return getButtonBounds(1); }
    private Rectangle getNextBounds() { return getButtonBounds(2); }

    private Rectangle getButtonBounds(int index) {
        Rectangle panel = getPanelBounds();
        float btnSize = panel.width * BTN_SIZE_FRAC * 0.5f;
        float btnGap = panel.width * BTN_GAP_FRAC;
        float totalW = 3 * btnSize + 2 * btnGap;
        float startX = panel.x + (panel.width - totalW) / 2f;
        float cy = panel.y + panel.height * BTN_Y_FRAC;
        float bx = startX + index * (btnSize + btnGap);
        float by = cy - btnSize / 2f;
        return new Rectangle(bx, by, btnSize, btnSize);
    }

    private static void setNearestFilter(Texture... textures) {
        for (Texture t : textures) {
            if (t != null) {
                t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            }
        }
    }

    public void setStats(int flyCount, int photosUsed) {
        this.flyCount = flyCount;
        this.photosUsed = photosUsed;
    }

    private void drawStats(Rectangle panel) {
        float iconSize = panel.width * 0.03f;
        float numH = iconSize * 0.8f;
        float gap = panel.width * 0.015f;
        float groupGap = panel.width * 0.08f;

        float lotusY = panel.y + panel.height * LOTUS_Y_FRAC;
        float btnY = panel.y + panel.height * BTN_Y_FRAC;
        float cy = (lotusY + btnY) / 2f;

        float flyNumW = measureNumWidth(flyCount, numbersBlack, numH);
        float picNumW = measureNumWidth(photosUsed, numbersBlack, numH);

        float flyGroupW = flyNumW + gap + iconSize;
        float picGroupW = picNumW + gap + iconSize;
        float totalW = flyGroupW + groupGap + picGroupW;

        float startX = panel.x + (panel.width - totalW) / 2f;
        float iconY = cy - iconSize / 2f;
        float numY = cy - numH / 2f;

        float cx = startX;
        cx = drawNumber(flyCount, numbersBlack, cx, numY, numH);
        cx += gap;
        batch.setColor(Color.WHITE);
        batch.draw(fly, cx, iconY, iconSize, iconSize);
        cx += iconSize + groupGap;

        cx = drawNumber(photosUsed, numbersBlack, cx, numY, numH);
        cx += gap;
        batch.setColor(Color.WHITE);
        batch.draw(polaroid, cx, iconY, iconSize, iconSize);
    }

    private float measureNumWidth(int count, Texture[] nums, float digitH) {
        if (count >= 10) {
            Texture t = nums[count / 10];
            Texture o = nums[count % 10];
            float tW = digitH * t.getWidth() / t.getHeight();
            float oW = digitH * o.getWidth() / o.getHeight();
            return tW + digitH * 0.05f + oW;
        } else {
            Texture d = nums[count];
            return digitH * d.getWidth() / d.getHeight();
        }
    }

    private float drawNumber(int count, Texture[] nums, float x, float y, float digitH) {
        float digitGap = digitH * 0.05f;
        if (count >= 10) {
            Texture t = nums[count / 10];
            Texture o = nums[count % 10];
            float tW = digitH * t.getWidth() / t.getHeight();
            float oW = digitH * o.getWidth() / o.getHeight();
            batch.setColor(Color.WHITE);
            batch.draw(t, x, y, tW, digitH);
            batch.draw(o, x + tW + digitGap, y, oW, digitH);
            return x + tW + digitGap + oW;
        } else {
            Texture d = nums[count];
            float dW = digitH * d.getWidth() / d.getHeight();
            batch.setColor(Color.WHITE);
            batch.draw(d, x, y, dW, digitH);
            return x + dW;
        }
    }

}