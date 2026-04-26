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
import com.badlogic.gdx.utils.ScreenUtils;
import edu.cornell.cis3152.physics.CanvasRender;
import edu.cornell.cis3152.physics.screen.levels.LevelController;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.TextAlign;
import edu.cornell.gdiac.graphics.TextLayout;

/**
 * Simple menu screen for selecting a level.
 */
public class LevelSelectScene implements Screen {
    /** Shared sprite batch */
    private SpriteBatch batch;
    /** Shared letterboxed viewport */
    private CanvasRender viewport;
    /** Drawing camera */
    private OrthographicCamera camera;
    /** Shared font */
    private final BitmapFont font;
    /** Background texture for the level select screen */
    private final Texture backgroundTexture;
    /** Lily pad buttons for levels 1-3 */
    private final Texture lilyTexture;
    /** Title screen–style control; returns to main menu */
    private final Texture menuButtonTexture;
    /** Solid pixel used to draw button rectangles */
    private final Texture pixel;

    /** Menu title */
    private final TextLayout titleLayout;
    /** Menu instructions */
    private final TextLayout instructionLayout;
    /** Reusable label layout */
    private final TextLayout optionLayout;

    /** Total number of selectable levels */
    private final int totalLevels;
    /** Current selected button index */
    private int selectedIndex;
    /** Pending selected level */
    private int chosenLevel;
    /** Whether the menu requested exit */
    private boolean exitRequested;
    /** Whether this screen is active */
    private boolean active;

    /** Screen width */
    private int width;
    /** Screen height */
    private int height;

    /** Input edge detection */
    private boolean upPrevious;
    private boolean downPrevious;
    private boolean confirmPrevious;
    private boolean exitPrevious;
    private boolean clickPrevious;
    /** Reusable coordinate buffer */
    private final Vector2 pointer;

    private float scroll;

    private final TextLayout arrowLayout;

    private Rectangle leftArrowBounds;
    private Rectangle rightArrowBounds;

    private static final float ARROW_SIZE_REF = 70f;
    private static final float ARROW_PADDING_REF = 20f;
    private static final int PAGE_SIZE = 3;
    private static final float MENU_BTN_MAX_W_FRAC = 0.2f;
    private static final float MENU_BTN_TOP_MARGIN_REF = 16f;
    private static final float MENU_BTN_RIGHT_MARGIN_REF = 16f;
    private static final float MENU_IDLE_SCALE = 0.92f;
    private static final Color MENU_HOVER_TINT = new Color(0.52f, 0.52f, 0.55f, 1f);

    private final LevelController controller;
    private final Texture starTexture;

    public LevelSelectScene(AssetDirectory assets, SpriteBatch batch, CanvasRender viewport, int totalLevels, LevelController controller) {
        this.batch = batch;
        this.viewport = viewport;
        this.totalLevels = totalLevels;
        this.font = assets.getEntry("shared-retro", BitmapFont.class);
        this.backgroundTexture = assets.getEntry("shared-water", Texture.class);
        if (this.backgroundTexture != null) {
            this.backgroundTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }
        this.lilyTexture = assets.getEntry("shared-blank-lily", Texture.class);
        if (lilyTexture != null) {
                lilyTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }
        this.menuButtonTexture = assets.getEntry("shared-pause-menu", Texture.class);
        if (menuButtonTexture != null) {
            menuButtonTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }

        this.camera = new OrthographicCamera();
        this.selectedIndex = 0;
        this.chosenLevel = -1;
        this.pointer = new Vector2();

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        this.pixel = new Texture(pixmap);
        pixmap.dispose();

        titleLayout = new TextLayout();
        titleLayout.setFont(font);
        titleLayout.setAlignment(TextAlign.middleCenter);
        titleLayout.setColor(Color.WHITE);
        // titleLayout.setText("SELECT LEVEL");
        titleLayout.layout();

        font.getData().setScale(0.4f * CanvasRender.layoutScale());
        instructionLayout = new TextLayout();
        instructionLayout.setFont(font);
        instructionLayout.setAlignment(TextAlign.middleCenter);
        instructionLayout.setColor(new Color(0.89f, 0.87f, 0.76f, 1.0f));
        // instructionLayout.setText("ARROWS OR MOUSE, ENTER TO START, ESC TO QUIT");
        instructionLayout.layout();
        font.getData().setScale(1f);

        optionLayout = new TextLayout();
        optionLayout.setFont(font);
        optionLayout.setAlignment(TextAlign.middleCenter);
        scroll = 0f;
        arrowLayout = new TextLayout();
        arrowLayout.setFont(font);
        arrowLayout.setAlignment(TextAlign.middleCenter);

        leftArrowBounds = new Rectangle();
        rightArrowBounds = new Rectangle();

        this.starTexture = assets.getEntry("shared-star", Texture.class);
        this.controller = controller;

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void setBatch(SpriteBatch batch) {
        this.batch = batch;
    }

    public int consumeChosenLevel() {
        int result = chosenLevel;
        chosenLevel = -1;
        return result;
    }

    public boolean consumeExitRequested() {
        boolean result = exitRequested;
        exitRequested = false;
        return result;
    }

    @Override
    public void show() {
        active = true;
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
        boolean upPressed = Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean downPressed = Gdx.input.isKeyPressed(Input.Keys.DOWN);
        boolean confirmPressed = Gdx.input.isKeyPressed(Input.Keys.ENTER) || Gdx.input.isKeyPressed(Input.Keys.SPACE);
        boolean exitPressed = Gdx.input.isKeyPressed(Input.Keys.ESCAPE);
        boolean clickPressed = Gdx.input.isButtonPressed(Input.Buttons.LEFT);

        if (upPressed && !upPrevious) {
            selectedIndex = (selectedIndex + totalLevels - 1) % totalLevels;
        }
        if (downPressed && !downPrevious) {
            selectedIndex = (selectedIndex + 1) % totalLevels;
        }
        if (confirmPressed && !confirmPrevious) {
            chosenLevel = selectedIndex + 1;
        }
        if (exitPressed && !exitPrevious) {
            exitRequested = true;
        }

        if (clickPressed && !clickPrevious) {
            viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), pointer);
            if (getMenuButtonBounds().contains(pointer.x, pointer.y)) {
                exitRequested = true;
            } else if (leftArrowBounds.contains(pointer)) {
                scrollByPage(-1);
            } else if (rightArrowBounds.contains(pointer)) {
                scrollByPage(1);
            } else {
                int clickedIndex = getHoveredIndex();
                if (clickedIndex >= 0) {
                    selectedIndex = clickedIndex;
                    chosenLevel = clickedIndex + 1;
                }
            }
        } else {
            int hoveredIndex = getHoveredIndex();
            if (hoveredIndex >= 0) {
                selectedIndex = hoveredIndex;
            }
        }

        upPrevious = upPressed;
        downPrevious = downPressed;
        confirmPrevious = confirmPressed;
        exitPrevious = exitPressed;
        clickPrevious = clickPressed;
        float scrollStep = 5f * CanvasRender.layoutScale();
        scroll -= Gdx.input.isKeyPressed(Input.Keys.A) ? scrollStep : 0f;
        scroll -= Gdx.input.isKeyPressed(Input.Keys.LEFT) ? scrollStep : 0f;

        scroll += Gdx.input.isKeyPressed(Input.Keys.D) ? scrollStep : 0f;
        scroll += Gdx.input.isKeyPressed(Input.Keys.RIGHT) ? scrollStep : 0f;

        float spacing = levelSpacing();
        float start = levelStartMargin();
        float maxScroll = Math.max(0, (totalLevels - 1) * spacing - (width - start * 2));
        scroll = Math.max(0, Math.min(scroll, maxScroll));
    }

    private void draw() {
        ScreenUtils.clear(0.0f, 0.0f, 0.0f, 1.0f);
        camera.update();

        viewport.apply();
        batch.begin(camera);
        batch.setColor(Color.WHITE);
        if (backgroundTexture != null) {
            batch.draw(backgroundTexture, 0, 0, width, height);
        }
        float titleY = height - height * (100f / 720f);
        float instructY = height - height * (170f / 720f);
        batch.drawText(titleLayout, width / 2.0f, titleY);
        batch.drawText(instructionLayout, width / 2.0f, instructY);

        batch.setColor(0, 0, 0, 1f);
        for (int ii = 0; ii < totalLevels; ii++) {
            Vector2 pos = getLevelPosition(ii);
            boolean selected = ii == selectedIndex;
            float size = selected ? lilySizeSelected() : lilySizeNormal();

            if (lilyTexture != null) {
                batch.setColor(selected ? Color.WHITE : new Color(0.82f, 0.82f, 0.82f, 1.0f));
                batch.draw(lilyTexture, pos.x - size/2, pos.y - size /2, size, size);
            }

            if (starTexture != null && controller.isBeaten(ii + 1)) {
                float starSize = size * 0.35f;                 // ~45% of the lily
                float sx = pos.x + size * 0.25f - starSize / 2;  // offset toward top-right
                float sy = pos.y + size * 0.25f - starSize / 2;
                batch.setColor(Color.WHITE);                    // don't inherit the lily tint
                batch.draw(starTexture, sx, sy, starSize, starSize);
            }
            int score = controller.getLevelScore(ii + 1);
            if (score != -1) {
                font.getData().setScale(0.5f * CanvasRender.layoutScale());
                optionLayout.setColor(Color.WHITE);
                optionLayout.setText(String.valueOf(score));
                optionLayout.layout();
                float numLift = -size * 0.25f;   // below the lily's center
                batch.drawText(optionLayout, pos.x, pos.y + numLift);
            }


            font.getData().setScale(0.85f * CanvasRender.layoutScale());
            optionLayout.setColor(Color.WHITE);
            optionLayout.setText(String.valueOf(ii + 1));
            optionLayout.layout();
            float labelLift = 5f * CanvasRender.layoutScale();
            batch.drawText(optionLayout, pos.x, pos.y + labelLift);

        }
        font.getData().setScale(1.0f);
        float spacing = levelSpacing();
        float start = levelStartMargin();
        float maxScroll = Math.max(0, (totalLevels - 1) * spacing - (width - start * 2));
        drawArrow(false, scroll > 1f);
        drawArrow(true, scroll < maxScroll - 1f);

        drawMenuButton();

        batch.end();
        viewport.reset();
    }

    private float levelSpacing() {
        return width * (220f / 1280f);
    }

    private float levelStartMargin() {
        return width * (150f / 1280f);
    }

    private float lilySizeSelected() {
        return width * (140f / 1280f);
    }

    private float lilySizeNormal() {
        return width * (110f / 1280f);
    }

    private Vector2 getLevelPosition(int index) {
        float spacing = levelSpacing();
        float start = levelStartMargin();
        float x = start + index * spacing - scroll;

        float amplitude = height * (120f / 720f);
        float base = height * 0.5f;
        float y = base + (index % 2 == 0 ? amplitude : -amplitude);
        return new Vector2(x,y);
    }


    private int getHoveredIndex() {
        viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), pointer);
        if (getMenuButtonBounds().contains(pointer.x, pointer.y)) {
            return -1;
        }

        float hoverRad = width * (60f / 1280f);
        for (int ii = 0; ii < totalLevels; ii++) {
            Vector2 pos = getLevelPosition(ii);
            if (pointer.dst(pos) < hoverRad) {
                return ii;
            }
        }
        return -1;
    }

    @Override
    public void resize(int width, int height) {
        this.width = (int)viewport.getWidth();
        this.height = (int)viewport.getHeight();
        camera.setToOrtho(false, this.width, this.height);
    }

    private void clampScroll() {
        float spacing = levelSpacing();
        float start = levelStartMargin();
        float maxScroll = Math.max(0, (totalLevels - 1) * spacing - (width - start * 2));
        scroll = Math.max(0, Math.min(scroll, maxScroll));
    }

    private void scrollByPage(int direction) {
        scroll += direction * PAGE_SIZE * levelSpacing();
        clampScroll();
    }

    private Rectangle getMenuButtonBounds() {
        if (menuButtonTexture == null) {
            return new Rectangle(0, 0, 0, 0);
        }
        float UI = CanvasRender.layoutScale();
        float marginTop = MENU_BTN_TOP_MARGIN_REF * UI;
        float marginRight = MENU_BTN_RIGHT_MARGIN_REF * UI;
        float maxW = width * MENU_BTN_MAX_W_FRAC;
        float s = maxW / Math.max(1f, menuButtonTexture.getWidth());
        float bw = menuButtonTexture.getWidth() * s;
        float bh = menuButtonTexture.getHeight() * s;
        float x = width - marginRight - bw;
        float y = height - marginTop - bh;
        return new Rectangle(x, y, bw, bh);
    }

    private void drawMenuButton() {
        if (menuButtonTexture == null) {
            return;
        }
        Rectangle b = getMenuButtonBounds();
        viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), pointer);
        boolean hover = b.contains(pointer.x, pointer.y);
        float tScale = hover ? 1f : MENU_IDLE_SCALE;
        float dw = b.width * tScale;
        float dh = b.height * tScale;
        float dx = b.x + (b.width - dw) / 2f;
        float dy = b.y + (b.height - dh) / 2f;
        batch.setColor(hover ? MENU_HOVER_TINT : Color.WHITE);
        batch.draw(menuButtonTexture, dx, dy, dw, dh);
        batch.setColor(Color.WHITE);
    }

    private void drawArrow(boolean isRight, boolean active) {
        float UI = CanvasRender.layoutScale();
        float arrowSize = ARROW_SIZE_REF * UI;
        float arrowPad = ARROW_PADDING_REF * UI;
        float cy = height * 0.5f;
        float x = isRight
                ? width - arrowPad - arrowSize
                : arrowPad;

        Rectangle bounds = isRight ? rightArrowBounds : leftArrowBounds;
        bounds.set(x, cy - arrowSize / 2, arrowSize, arrowSize);

        Color bgColor = active
                ? new Color(0f, 0f, 0f, 0.5f)
                : new Color(0f, 0f, 0f, 0.25f);
        batch.setColor(bgColor);
        batch.draw(pixel, x, cy - arrowSize / 2, arrowSize, arrowSize);

        font.getData().setScale(0.9f * UI);
        arrowLayout.setColor(active ? Color.WHITE : new Color(1f, 1f, 1f, 0.3f));
        arrowLayout.setText(isRight ? ">" : "<");
        arrowLayout.layout();
        batch.setColor(Color.WHITE);
        batch.drawText(arrowLayout, x + arrowSize / 2, cy + 5f * UI);
        font.getData().setScale(1.0f);
    }
    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        active = false;
    }

    @Override
    public void dispose() {
        pixel.dispose();
    }
}
