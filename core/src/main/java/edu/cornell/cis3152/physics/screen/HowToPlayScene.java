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


public class HowToPlayScene implements Screen {

    private static final float PANEL_W_FRAC = 5 / 6f;
    private static final float PANEL_H_FRAC = 5 / 6f;

    private static final float TITLE_MAX_W_FRAC = 0.45f;
    private static final float TITLE_TOP_INSET_FRAC = 0.20f;
    private static final float CONTENT_TOP_FRAC = 0.22f;
    private static final float CONTENT_BOT_FRAC = 0.90f;

    private static final float ARROW_W_FRAC = 0.06f;
    private static final float ARROW_Y_FRAC = 0.5f;

    private static final float EXIT_W_FRAC = 0.09f;

    // Per-page layout
    private static final float KEY_MAX_W_FRAC = 0.047f;
    private static final float KEY_TEXT_GAP_FRAC = 0.024f;
    private static final float ROW_GAP_FRAC = 0.08f;


    private final CanvasRender viewport;
    private final SpriteBatch batch;
    private final OrthographicCamera camera;
    private final Texture pixel;

    // Shared UI
    private final Texture borderWooden;
    private final Texture howToPlayText;
    private final Texture leftArrow;
    private final Texture rightArrow;
    private final Texture exitButton;

    // Page 1 textures
    private final Texture p1ControlA;
    private final Texture p1ControlD;
    private final Texture p1ControlSpace;
    private final Texture p1ControlW;
    private final Texture p1JumpText;
    private final Texture p1MoveLeftText;
    private final Texture p1MoveRightText;
    private final Texture p1Slash;

    // Page 2 textures
    private final Texture p2ControlP;
    private final Texture p2ControlQ;
    private final Texture p2ControlTab;
    private final Texture p2DropPhotoText;
    private final Texture p2PauseText;
    private final Texture p2ToggleRangeText;
    // Page 3 textures
    private final Texture p3LeftClick;
    private final Texture p3RightClick;
    private final Texture p3RemovePictureText;
    private final Texture p3TakePictureText;

    // Page 4 textures
    private final Texture p4Control1;
    private final Texture p4Control2;
    private final Texture p4Control3;
    private final Texture p4Control4;
    private final Texture p4SwitchInventoryText;

    // Page 5 textures
    private final Texture p5ClickPhotoText;

    // Page 6 textures
    private final Texture p6StickPhotoText;

    // Page 7 textures
    private final Texture p7FliesText;

    private boolean active;
    private boolean exiting;

    private int currentPage = 0;
    private static final int TOTAL_PAGES = 7;

    private int width;
    private int height;

    private boolean leftPrev;
    private boolean rightPrev;
    private boolean escapePrev;
    private boolean clickPrev;
    private final Vector2 pointer = new Vector2();

    public enum Origin {LEVEL_SELECT, PAUSE_MENU}

    private Origin origin;

    public HowToPlayScene(AssetDirectory assets, SpriteBatch batch, CanvasRender viewport) {
        this.viewport = viewport;
        this.batch = batch;
        this.camera = new OrthographicCamera();

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();

        // Shared UI
        borderWooden = assets.getEntry("shared-border-wooden", Texture.class);
        howToPlayText = assets.getEntry("how-to-play-text", Texture.class);
        leftArrow = assets.getEntry("shared-left-arrow", Texture.class);
        rightArrow = assets.getEntry("shared-right-arrow", Texture.class);
        exitButton = assets.getEntry("shared-exit-button-options", Texture.class);

        // Page 1
        p1ControlA = assets.getEntry("control-a", Texture.class);
        p1ControlD = assets.getEntry("control-d", Texture.class);
        p1ControlSpace = assets.getEntry("control-space", Texture.class);
        p1ControlW = assets.getEntry("control-w", Texture.class);
        p1JumpText = assets.getEntry("jump-text", Texture.class);
        p1MoveLeftText = assets.getEntry("move-left-text", Texture.class);
        p1MoveRightText = assets.getEntry("move-right-text", Texture.class);
        p1Slash = assets.getEntry("slash", Texture.class);

        // Page 2
        p2ControlP = assets.getEntry("control-p", Texture.class);
        p2ControlQ = assets.getEntry("control-q", Texture.class);
        p2ControlTab = assets.getEntry("control-tab", Texture.class);
        p2DropPhotoText = assets.getEntry("drop-photo-text", Texture.class);
        p2PauseText = assets.getEntry("pause-text", Texture.class);
        p2ToggleRangeText = assets.getEntry("toggle-range-text", Texture.class);

        // Page 3
        p3LeftClick = assets.getEntry("left-click", Texture.class);
        p3RightClick = assets.getEntry("right-click", Texture.class);
        p3RemovePictureText = assets.getEntry("remove-picture-text", Texture.class);
        p3TakePictureText = assets.getEntry("take-picture-text", Texture.class);

        // Page 4
        p4Control1 = assets.getEntry("control-1", Texture.class);
        p4Control2 = assets.getEntry("control-2", Texture.class);
        p4Control3 = assets.getEntry("control-3", Texture.class);
        p4Control4 = assets.getEntry("control-4", Texture.class);
        p4SwitchInventoryText = assets.getEntry("switch-inventory-text", Texture.class);

        // Page 5
        p5ClickPhotoText = assets.getEntry("click-photo-text", Texture.class);

        // Page 6
        p6StickPhotoText = assets.getEntry("stick-photo-text", Texture.class);

        // Page 7
        p7FliesText = assets.getEntry("flies-text", Texture.class);

        setNearestFilter(borderWooden, howToPlayText, leftArrow, rightArrow, exitButton, p1ControlA, p1ControlD,
                p1ControlSpace, p1ControlW, p1JumpText, p1MoveLeftText, p1MoveRightText, p1Slash, p2ControlP,
                p2ControlQ, p2ControlTab, p2DropPhotoText, p2PauseText, p2ToggleRangeText, p3LeftClick, p3RightClick,
                p3RemovePictureText, p3TakePictureText, p4Control1, p4Control2, p4Control3, p4Control4, p4SwitchInventoryText,
                p5ClickPhotoText, p6StickPhotoText, p7FliesText
        );

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public boolean isExiting() {
        return exiting;
    }

    public boolean consumeExit() {
        boolean v = exiting;
        exiting = false;
        return v;
    }

    @Override
    public void show() {
        active = true;
        exiting = false;
        currentPage = 0;
    }

    public void show(Origin origin) {
        this.origin = origin;
        show();
    }

    public Origin getOrigin() {
        return origin;
    }

    @Override public void hide()
    {
        active = false;
    }
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
        boolean leftPressed = Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean rightPressed = Gdx.input.isKeyPressed(Input.Keys.RIGHT);
        boolean escapePressed = Gdx.input.isKeyPressed(Input.Keys.ESCAPE);
        boolean clickPressed = Gdx.input.isButtonPressed(Input.Buttons.LEFT);

        viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), pointer);

        if (leftPressed && !leftPrev) previousPage();
        if (rightPressed && !rightPrev) nextPage();
        if (escapePressed && !escapePrev) exiting = true;

        if (clickPressed && !clickPrev) {
            if (getExitBounds().contains(pointer.x, pointer.y)) {
                exiting = true;
            } else if (getLeftArrowBounds().contains(pointer.x, pointer.y)) {
                previousPage();
            } else if (getRightArrowBounds().contains(pointer.x, pointer.y)) {
                nextPage();
            }
        }

        leftPrev = leftPressed;
        rightPrev = rightPressed;
        escapePrev = escapePressed;
        clickPrev = clickPressed;
    }

    private void nextPage() { if (currentPage < TOTAL_PAGES - 1) currentPage++; }
    private void previousPage() { if (currentPage > 0) currentPage--; }

    private void draw() {
        camera.update();
        viewport.apply();
        batch.begin(camera);

        if (origin == Origin.PAUSE_MENU) {
            batch.setColor(new Color(0f, 0f, 0f, 0.55f));
        } else {
            batch.setColor(new Color(0.2f, 0.46f, 0.46f, 0.1f));

        }
        batch.draw(pixel, 0, 0, width, height);

        batch.setColor(Color.WHITE);

        Rectangle panel = getPanelBounds();
        batch.draw(borderWooden, panel.x, panel.y, panel.width, panel.height);

        drawTitleImage(panel);

        Rectangle content = getContentArea(panel);
        switch (currentPage) {
            case 0: drawPage1(panel, content); break;
            case 1: drawPage2(panel, content); break;
            case 2: drawPage3(panel, content); break;
            case 3: drawPage4(panel, content); break;
            case 4: drawSingleCentredImage(panel, content, p5ClickPhotoText);  break;
            case 5: drawSingleCentredImage(panel, content, p6StickPhotoText);  break;
            case 6: drawSingleCentredImage(panel, content, p7FliesText);       break;
            default: break;
        }

        drawArrows();

        drawExit();

        batch.setColor(Color.WHITE);
        batch.end();
        viewport.reset();
    }

    private void drawTitleImage(Rectangle panel) {
        if (howToPlayText == null) return;
        float maxW = panel.width * TITLE_MAX_W_FRAC;
        float scale = maxW / howToPlayText.getWidth();
        float tw = howToPlayText.getWidth() * scale;
        float th = howToPlayText.getHeight() * scale;
        float tx = panel.x + (panel.width - tw) / 2f;
        float ty = panel.y + panel.height - panel.height * TITLE_TOP_INSET_FRAC - th;
        batch.setColor(Color.WHITE);
        batch.draw(howToPlayText, tx, ty, tw, th);
    }

    private void drawPage1(Rectangle panel, Rectangle content) {
        float keyMaxW = panel.width * KEY_MAX_W_FRAC;
        float gap = panel.width * KEY_TEXT_GAP_FRAC;
        float rowGap = panel.height * ROW_GAP_FRAC;

        float row0H = scaleToWidth(p1ControlA, keyMaxW).y;
        float row1H = scaleToWidth(p1ControlD, keyMaxW).y;
        float row2H = scaleToWidth(p1ControlW, keyMaxW).y;

        float totalH = row0H + row1H + row2H + rowGap * 2;
        float startY = content.y + (content.height + totalH) / 2f;

        float row0Y = startY - row0H;
        drawKeyRow_single(p1ControlA, p1MoveLeftText, panel, row0Y, keyMaxW, gap, row0H);

        float row1Y = row0Y - rowGap - row1H;
        drawKeyRow_single(p1ControlD, p1MoveRightText, panel, row1Y, keyMaxW, gap, row1H);

        float row2Y = row1Y - rowGap - row2H;
        drawKeyRow_WSpace(panel, row2Y, keyMaxW, gap, row2H);
    }


    private void drawKeyRow_WSpace(Rectangle panel, float rowY, float keyMaxW, float gap, float rowH) {
        float spaceMaxW = panel.width * 0.237f;
        Vector2 sW = scaleToWidth(p1ControlW, keyMaxW);
        Vector2 sSpace = scaleToWidth(p1ControlSpace, spaceMaxW);
        Vector2 sSlash = scaleToHeight(p1Slash,rowH * 0.65f);
        Vector2 sJump = scaleToHeight(p1JumpText,rowH * 0.50f);

        float keyGroupW = sW.x + sSlash.x + sSpace.x + gap * 0.5f;
        float totalW = keyGroupW + gap + sJump.x;
        float startX = panel.x + (panel.width - totalW) / 2f;

        float cx = startX;

        draw(p1ControlW, cx, rowY + (rowH - sW.y) / 2f, sW.x, sW.y);
        cx += sW.x + gap * 0.25f;

        draw(p1Slash, cx, rowY + (rowH - sSlash.y) / 2f, sSlash.x, sSlash.y);
        cx += sSlash.x + gap * 0.25f;

        draw(p1ControlSpace, cx, rowY + (rowH - sSpace.y) / 2f, sSpace.x, sSpace.y);
        cx += sSpace.x + gap;

        draw(p1JumpText, cx, rowY + (rowH - sJump.y) / 2f, sJump.x, sJump.y);
    }

    private void drawKeyRow_single(Texture key, Texture label, Rectangle panel, float rowY, float keyMaxW, float gap, float rowH) {
        Vector2 sk = scaleToWidth(key, keyMaxW);
        Vector2 sl = scaleToHeight(label, rowH * 0.40f);

        float totalW = sk.x + gap + sl.x;
        float startX = panel.x + (panel.width - totalW) / 2f;

        draw(key, startX,rowY + (rowH - sk.y) / 2f, sk.x, sk.y);
        draw(label, startX + sk.x + gap, rowY + (rowH - sl.y) / 2f, sl.x, sl.y);
    }

    private void drawPage2(Rectangle panel, Rectangle content) {
        float keyMaxW = panel.width * KEY_MAX_W_FRAC;
        float tabMaxW = keyMaxW * 4.0f;
        float gap = panel.width * KEY_TEXT_GAP_FRAC;
        float rowGap = panel.height * ROW_GAP_FRAC;

        float row0H = scaleToWidth(p2ControlP, keyMaxW).y;
        float row1H = scaleToWidth(p2ControlQ, keyMaxW).y;
        float row2H = scaleToWidth(p2ControlTab, tabMaxW).y;

        float totalH = row0H + row1H + row2H + rowGap * 2;
        float startY = content.y + (content.height + totalH) / 2f;

        float row0Y = startY - row0H;
        drawKeyRow_single(p2ControlP, p2PauseText, panel, row0Y, keyMaxW, gap, row0H);

        float row1Y = row0Y - rowGap - row1H;
        drawKeyRow_single(p2ControlQ, p2DropPhotoText, panel, row1Y, keyMaxW, gap, row1H);

        float row2Y = row1Y - rowGap - row2H;
        drawKeyRow_single(p2ControlTab, p2ToggleRangeText, panel, row2Y, tabMaxW, gap, row2H);
    }

    private void drawPage3(Rectangle panel, Rectangle content) {
        float keyMaxW = panel.width * KEY_MAX_W_FRAC;
        float gap = panel.width * KEY_TEXT_GAP_FRAC;
        float rowGap = panel.height * ROW_GAP_FRAC;

        float row0H = scaleToWidth(p3LeftClick, keyMaxW).y;
        float row1H = scaleToWidth(p3RightClick, keyMaxW).y;

        float totalH = row0H + row1H + rowGap;
        float startY = content.y + (content.height + totalH) / 2f;

        float row0Y = startY - row0H;
        drawKeyRow_single(p3LeftClick, p3TakePictureText, panel, row0Y, keyMaxW, gap, row0H);

        float row1Y = row0Y - rowGap - row1H;
        drawKeyRow_single(p3RightClick, p3RemovePictureText, panel, row1Y, keyMaxW, gap, row1H);
    }

    private void drawPage4(Rectangle panel, Rectangle content) {
        float keyMaxW = panel.width * KEY_MAX_W_FRAC;
        float keyGap = panel.width * 0.01f * 4f;
        float rowGap = panel.height * ROW_GAP_FRAC;

        Vector2 s1 = scaleToWidth(p4Control1, keyMaxW);
        Vector2 s2 = scaleToWidth(p4Control2, keyMaxW);
        Vector2 s3 = scaleToWidth(p4Control3, keyMaxW);
        Vector2 s4 = scaleToWidth(p4Control4, keyMaxW);

        float keyRowH = s1.y;
        float labelH = keyRowH * 0.55f;
        Vector2 sLabel = scaleToHeight(p4SwitchInventoryText, labelH);

        float totalH = keyRowH + rowGap + sLabel.y;
        float startY = content.y + (content.height + totalH) / 2f;

        float keysW = s1.x + s2.x + s3.x + s4.x + keyGap * 3;
        float keysX = panel.x + (panel.width - keysW) / 2f;
        float keysY = startY - keyRowH;

        draw(p4Control1, keysX, keysY, s1.x, s1.y);
        draw(p4Control2, keysX + s1.x + keyGap, keysY, s2.x, s2.y);
        draw(p4Control3, keysX + s1.x + s2.x + keyGap * 2, keysY, s3.x, s3.y);
        draw(p4Control4, keysX + s1.x + s2.x + s3.x + keyGap * 3, keysY, s4.x, s4.y);

        float labelX = panel.x + (panel.width - sLabel.x) / 2f;
        float labelY = keysY - rowGap - sLabel.y;
        draw(p4SwitchInventoryText, labelX, labelY, sLabel.x, sLabel.y);
    }

    private void drawSingleCentredImage(Rectangle panel, Rectangle content, Texture image) {
        if (image == null) return;
        float maxW = panel.width * 0.65f;
        float scale = maxW / image.getWidth();
        float iw = image.getWidth() * scale * 0.7f;
        float ih = image.getHeight() * scale * 0.7f;
        float ix = panel.x + (panel.width - iw) / 2f;
        float iy = content.y + (content.height - ih) / 2f + panel.height * 0.05f;;
        draw(image, ix, iy, iw, ih);
    }

    private void drawArrows() {
        viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), pointer);

        if (currentPage > 0 && leftArrow != null) {
            Rectangle lb = getLeftArrowBounds();
            boolean hov = lb.contains(pointer.x, pointer.y);
            batch.setColor(hov ? new Color(0.75f, 0.75f, 0.75f, 1f) : Color.WHITE);
            batch.draw(leftArrow, lb.x, lb.y, lb.width, lb.height);
        }

        if (currentPage < TOTAL_PAGES - 1 && rightArrow != null) {
            Rectangle rb = getRightArrowBounds();
            boolean hov = rb.contains(pointer.x, pointer.y);
            batch.setColor(hov ? new Color(0.75f, 0.75f, 0.75f, 1f) : Color.WHITE);
            batch.draw(rightArrow, rb.x, rb.y, rb.width, rb.height);
        }

        batch.setColor(Color.WHITE);
    }

    private void drawExit() {
        if (exitButton == null) return;
        Rectangle eb = getExitBounds();
        boolean hov = eb.contains(pointer.x, pointer.y);
        batch.setColor(hov ? new Color(0.75f, 0.75f, 0.75f, 1f) : Color.WHITE);
        batch.draw(exitButton, eb.x, eb.y, eb.width, eb.height);
        batch.setColor(Color.WHITE);
    }

    private Rectangle getPanelBounds() {
        float pw = width * PANEL_W_FRAC;
        float ph = height * PANEL_H_FRAC;
        return new Rectangle((width - pw) / 2f, (height - ph) / 2f, pw, ph);
    }

    private Rectangle getContentArea(Rectangle panel) {
        float top = panel.y + panel.height * (1f - CONTENT_TOP_FRAC);
        float bot = panel.y + panel.height * (1f - CONTENT_BOT_FRAC);
        return new Rectangle(panel.x, bot, panel.width, top - bot);
    }

    private Rectangle getLeftArrowBounds() {
        Rectangle panel = getPanelBounds();
        float aw = panel.width * ARROW_W_FRAC * 1.5f;
        float ah = (leftArrow != null) ? aw * leftArrow.getHeight() / Math.max(1, leftArrow.getWidth()) : aw;
        float ax = panel.x + aw * 1.7f;
        float ay = panel.y + panel.height * ARROW_Y_FRAC - ah / 2f;
        return new Rectangle(ax, ay, aw, ah);
    }

    private Rectangle getRightArrowBounds() {
        Rectangle panel = getPanelBounds();
        float aw = panel.width * ARROW_W_FRAC * 1.5f;
        float ah = (rightArrow != null) ? aw * rightArrow.getHeight() / Math.max(1, rightArrow.getWidth()) : aw;
        float ax = panel.x + panel.width - aw * 2.7f;
        float ay = panel.y + panel.height * ARROW_Y_FRAC - ah / 2f;
        return new Rectangle(ax, ay, aw, ah);
    }

    private Rectangle getExitBounds() {
        Rectangle panel = getPanelBounds();
        float ew = panel.width * EXIT_W_FRAC;
        float eh = (exitButton != null) ? ew * exitButton.getHeight() / Math.max(1, exitButton.getWidth()) : ew;
        float ex = panel.x + panel.width - ew * 1.3f;
        float ey = panel.y + panel.height - eh * 0.9f;
        return new Rectangle(ex, ey, ew, eh);
    }

    private Vector2 scaleToWidth(Texture t, float targetW) {
        if (t == null) return new Vector2(targetW, targetW);
        float s = targetW / t.getWidth();
        return new Vector2(targetW, t.getHeight() * s);
    }

    private Vector2 scaleToHeight(Texture t, float targetH) {
        if (t == null) return new Vector2(targetH, targetH);
        float s = targetH / t.getHeight();
        return new Vector2(t.getWidth() * s, targetH);
    }

    private void draw(Texture t, float x, float y, float w, float h) {
        if (t == null) return;
        batch.setColor(Color.WHITE);
        batch.draw(t, x, y, w, h);
    }

    private static void setNearestFilter(Texture... textures) {
        for (Texture t : textures) {
            if (t != null) {
                t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            }
        }
    }
}