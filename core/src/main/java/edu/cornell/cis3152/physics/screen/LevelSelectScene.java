package edu.cornell.cis3152.physics.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import edu.cornell.cis3152.physics.CanvasRender;
import edu.cornell.cis3152.physics.graphics.SpriteStripAnimation;
import edu.cornell.cis3152.physics.screen.levels.LevelController;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.TextAlign;
import edu.cornell.gdiac.graphics.TextLayout;
import org.w3c.dom.css.Rect;

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
    /** Animated logo shown at top center */
    private SpriteStripAnimation titleAnimation;
    /** Lily pad buttons for levels 1-3 */
    private final Texture lilyTexture;
    private final Texture helpButtonTexture;
    private final Texture homeButtonTexture;
    private final Texture leftArrow;
    private final Texture rightArrow;
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
    /** Whether the how to play is requested */
    private boolean howToPlayRequested;
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

    private int currentPage;

    /** Slide-transition state for arrow page changes. */
    private boolean sliding = false;
    private int slidingFromPage;
    private int slidingToPage;
    private int slideDirection; // +1 = advancing (right arrow), -1 = retreating (left arrow)
    private float slideTimer;
    private static final float SLIDE_DURATION = 0.28f;

    private final TextLayout arrowLayout;

    private Rectangle leftArrowBounds;
    private Rectangle rightArrowBounds;
    private float titleAnimTime;

    private static final float ARROW_SIZE_REF = 70f;
    private static final float ARROW_PADDING_REF = 20f;
    private static final int PAGE_SIZE = 5;
    private static final float CONTENT_SIDE_MARGIN_REF = 95f;
    private static final String TITLE_STRIP_INTERNAL = "loading/GameLogo_animated.png";
    private static final int TITLE_FRAME_COUNT = 7;
    private static final float TITLE_DEFAULT_FRAME_SEC = 1f / 12f;
    private static final float TITLE_MAX_WIDTH_FRAC = 0.6f;
    private static final float TITLE_MAX_HEIGHT_FRAC = 0.26f;
    private static final float TITLE_CENTER_X_FRAC = 0.5f;
    private static final float TITLE_CENTER_Y_FRAC = 0.88f;
    private static final float MENU_BTN_MAX_W_FRAC = 0.08f;
    private static final float MENU_BTN_TOP_MARGIN_REF = 16f;
    private static final float MENU_BTN_RIGHT_MARGIN_REF = 16f;
    private static final float MENU_IDLE_SCALE = 0.92f;
    private static final Color MENU_HOVER_TINT = new Color(0.52f, 0.52f, 0.55f, 1f);
    private static final float LILY_ARCH_OFFSET = 5f;
    private static final float DIGIT_HEIGHT_RATIO = 0.3f;  // digit height as fraction of lily size
    private static final float DIGIT_GAP_RATIO    = 0.025f;  // gap between digits, also a fraction of size
    private static final float DIGIT_Y_RATIO      = 0.0f; // vertical nudge if the lily's visual center is off from its texture center

    private final LevelController controller;
    private final Texture starTexture;
    private final Texture lilyFlowerGrayTexture;
    private final Texture grayLilyPad;
    public boolean MASTER_UNLOCK = false;

    private Texture[] numbersBlack = new Texture[10];
    private Texture[] numbersWhite = new Texture[10];




    public LevelSelectScene(AssetDirectory assets, SpriteBatch batch, CanvasRender viewport, int totalLevels, LevelController controller) {
        this.batch = batch;
        this.viewport = viewport;
        this.totalLevels = totalLevels;
        this.font = assets.getEntry("shared-retro", BitmapFont.class);
        this.backgroundTexture = assets.getEntry("shared-water", Texture.class);
        if (this.backgroundTexture != null) {
            this.backgroundTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }
        try {
            this.titleAnimation = SpriteStripAnimation.loadHorizontalStrip(
                    Gdx.files.internal(TITLE_STRIP_INTERNAL),
                    TITLE_FRAME_COUNT,
                    Texture.TextureFilter.Nearest,
                    TITLE_DEFAULT_FRAME_SEC);
        } catch (RuntimeException e) {
            Gdx.app.error("LevelSelectScene", "Could not load title sprite strip: " + TITLE_STRIP_INTERNAL, e);
        }
        this.lilyTexture = assets.getEntry("shared-blank-lily", Texture.class);
        if (lilyTexture != null) {
                lilyTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }
        this.menuButtonTexture = new Texture(Gdx.files.internal("menubuttonlevel.png"));
        this.menuButtonTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        this.helpButtonTexture = assets.getEntry("shared-help", Texture.class);
        this.helpButtonTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        this.homeButtonTexture = assets.getEntry("shared-home", Texture.class);
        this.homeButtonTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        this.leftArrow = assets.getEntry("shared-left-arrow", Texture.class);
        this.leftArrow.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        this.rightArrow = assets.getEntry("shared-right-arrow", Texture.class);
        this.rightArrow.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

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
        currentPage = 0;
        arrowLayout = new TextLayout();
        arrowLayout.setFont(font);
        arrowLayout.setAlignment(TextAlign.middleCenter);

        leftArrowBounds = new Rectangle();
        rightArrowBounds = new Rectangle();

        this.starTexture = assets.getEntry("shared-lotus", Texture.class);
        this.lilyFlowerGrayTexture = assets.getEntry("shared-lily-gray", Texture.class);
        this.grayLilyPad = assets.getEntry("shared-gray-lilypad", Texture.class);
        this.controller = controller;
        loadNumberTexures(assets);

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

    public boolean consumeHowToPlayRequested() {
        boolean result = howToPlayRequested;
        howToPlayRequested = false;
        return result;
    }

    @Override
    public void show() {
        active = true;
        selectedIndex = Math.max(0, Math.min(selectedIndex, totalLevels - 1));
        currentPage = pageForIndex(selectedIndex);
    }

    @Override
    public void render(float delta) {
        if (!active) {
            return;
        }

        titleAnimTime += delta;
        update(delta);
        draw();
    }

    private void update(float delta) {
        if (sliding) {
            slideTimer += delta;
            if (slideTimer >= SLIDE_DURATION) {
                sliding = false;
                currentPage = slidingToPage;
                int pageStart = currentPage * PAGE_SIZE;
                int pageEnd = Math.min(totalLevels, pageStart + PAGE_SIZE) - 1;
                selectedIndex = Math.max(pageStart, Math.min(selectedIndex, pageEnd));
            }
            // Consume input state so no ghost events fire after animation.
            upPrevious    = Gdx.input.isKeyPressed(Input.Keys.UP);
            downPrevious  = Gdx.input.isKeyPressed(Input.Keys.DOWN);
            confirmPrevious = Gdx.input.isKeyPressed(Input.Keys.ENTER) || Gdx.input.isKeyPressed(Input.Keys.SPACE);
            exitPrevious  = Gdx.input.isKeyPressed(Input.Keys.ESCAPE);
            clickPrevious = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
            return;
        }

        boolean upPressed = Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean downPressed = Gdx.input.isKeyPressed(Input.Keys.DOWN);
        boolean confirmPressed = Gdx.input.isKeyPressed(Input.Keys.ENTER) || Gdx.input.isKeyPressed(Input.Keys.SPACE);
        boolean exitPressed = Gdx.input.isKeyPressed(Input.Keys.ESCAPE);
        boolean clickPressed = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        if (Gdx.input.isKeyJustPressed(Input.Keys.K)) {
            MASTER_UNLOCK = !MASTER_UNLOCK;
        }

        if (upPressed && !upPrevious) {
            selectedIndex = (selectedIndex + totalLevels - 1) % totalLevels;
            currentPage = pageForIndex(selectedIndex);
        }
        if (downPressed && !downPrevious) {
            selectedIndex = (selectedIndex + 1) % totalLevels;
            currentPage = pageForIndex(selectedIndex);
        }
        if (confirmPressed && !confirmPrevious) {
            chosenLevel = selectedIndex + 1;
        }
        if (exitPressed && !exitPrevious) {
            exitRequested = true;
        }

        if (clickPressed && !clickPrevious) {
            viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), pointer);
            if (getHomeButtonBounds().contains(pointer.x, pointer.y)) {
                exitRequested = true;
            }
            else if (getHelpButtonBounds().contains(pointer.x, pointer.y)) {
                howToPlayRequested = true;
            }
            else if (leftArrowBounds.contains(pointer)) {
                changePage(-1);
            } else if (rightArrowBounds.contains(pointer)) {
                changePage(1);
            } else {
                int clickedIndex = getHoveredIndex();
                if (clickedIndex >= 0) {
                    selectedIndex = clickedIndex;
                    currentPage = pageForIndex(selectedIndex);
                    chosenLevel = clickedIndex + 1;
                }
            }
        } else {
            int hoveredIndex = getHoveredIndex();
            if (hoveredIndex >= 0) {
                selectedIndex = hoveredIndex;
                currentPage = pageForIndex(selectedIndex);
            }
        }

        upPrevious = upPressed;
        downPrevious = downPressed;
        confirmPrevious = confirmPressed;
        exitPrevious = exitPressed;
        clickPrevious = clickPressed;
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
        drawTitle();
        float titleY = height - height * (100f / 720f);
        float instructY = height - height * (170f / 720f);
        batch.drawText(titleLayout, width / 2.0f, titleY);
        batch.drawText(instructionLayout, width / 2.0f, instructY);

        if (sliding) {
            float t = Math.min(1f, slideTimer / SLIDE_DURATION);
            float s = t * t * (3f - 2f * t); // smoothstep
            drawPageItems(slidingFromPage, -slideDirection * s * width);
            drawPageItems(slidingToPage,   slideDirection * (1f - s) * width);
        } else {
            drawPageItems(currentPage, 0f);
        }
        int displayPage = sliding ? slidingToPage : currentPage;
        drawArrow(false, displayPage > 0);
        drawArrow(true, displayPage < pageCount() - 1);

        drawButton();

        batch.end();
        viewport.reset();
    }

    private float contentSideMargin() {
        return width * (CONTENT_SIDE_MARGIN_REF / 1280f);
    }

    private float lilySizeSelected() {
        return width * (165f / 1280f);
    }

    private float lilySizeNormal() {
        return width * (132f / 1280f);
    }

    private Vector2 getLevelPosition(int index) {
        int pageStart = (index / PAGE_SIZE) * PAGE_SIZE;
        int visibleCount = Math.min(PAGE_SIZE, totalLevels - pageStart);
        int slot = index - pageStart;
        float contentLeft = contentSideMargin();
        float contentWidth = width - 2f * contentLeft;
        float x = contentLeft + contentWidth * (slot + 1f) / (visibleCount + 1f);
        float amplitude = height * (120f / 720f);
        float base = height * 0.43f;
        float y = base + (slot % 2 == 0 ? amplitude : -amplitude);
        return new Vector2(x,y);
    }


    private int getHoveredIndex() {
        viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), pointer);
        if (getHomeButtonBounds().contains(pointer.x, pointer.y)) {
            return -1;
        }
        if (getHelpButtonBounds().contains(pointer.x, pointer.y)) {
            return -1;
        }

        float hoverRad = width * (60f / 1280f);
        int startIndex = currentPage * PAGE_SIZE;
        int endIndex = Math.min(totalLevels, startIndex + PAGE_SIZE);
        for (int ii = startIndex; ii < endIndex; ii++) {
            Vector2 pos = getLevelPosition(ii);
            if (pointer.dst(pos) < hoverRad) {
                if (!(MASTER_UNLOCK || controller.isLevelOpen(ii + 1))){return -1;}
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

    private int pageCount() {
        return Math.max(1, (totalLevels + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private int pageForIndex(int index) {
        return Math.max(0, Math.min(pageCount() - 1, index / PAGE_SIZE));
    }

    private void changePage(int direction) {
        int targetPage = Math.max(0, Math.min(pageCount() - 1, currentPage + direction));
        if (targetPage == currentPage) return;
        slidingFromPage = currentPage;
        slidingToPage = targetPage;
        slideDirection = direction;
        slideTimer = 0f;
        sliding = true;
    }

    private Rectangle getHomeButtonBounds() {
        if (homeButtonTexture == null) {
            return new Rectangle(0, 0, 0, 0);
        }
        float UI = CanvasRender.layoutScale();
        float marginTop = MENU_BTN_TOP_MARGIN_REF * UI;
        float maxW = width * MENU_BTN_MAX_W_FRAC;
        float s = maxW / Math.max(1f, homeButtonTexture.getWidth());
        float bw = menuButtonTexture.getWidth() * s * 0.7f;
        float bh = menuButtonTexture.getHeight() * s * 0.7f;
        float x = width - marginTop - bw;
        float y = height - marginTop - bh;
        return new Rectangle(x, y, bw, bh);
    }

    private Rectangle getHelpButtonBounds() {
        if (helpButtonTexture == null) {
            return new Rectangle(0, 0, 0, 0);
        }
        float UI = CanvasRender.layoutScale();
        float marginTop = MENU_BTN_TOP_MARGIN_REF * UI;
        float maxW = width * MENU_BTN_MAX_W_FRAC;
        float s = maxW / Math.max(1f, homeButtonTexture.getWidth());
        float bw = menuButtonTexture.getWidth() * s * 0.7f;
        float bh = menuButtonTexture.getHeight() * s * 0.7f;
        Rectangle home = getHomeButtonBounds();
        float x = home.x - marginTop - bw - 3;
        float y = height - marginTop - bh;
        return new Rectangle(x, y, bw, bh);
    }

    private void drawButton() {
        Rectangle hb = getHomeButtonBounds();
        Rectangle helpB = getHelpButtonBounds();
        viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), pointer);

        boolean homeHover = hb.contains(pointer.x, pointer.y);
        batch.setColor(homeHover ? MENU_HOVER_TINT : Color.WHITE);
        batch.draw(homeButtonTexture, hb.x, hb.y, hb.width, hb.height);

        boolean helpHover = helpB.contains(pointer.x, pointer.y);
        batch.setColor(helpHover ? MENU_HOVER_TINT : Color.WHITE);
        batch.draw(helpButtonTexture, helpB.x, helpB.y, helpB.width, helpB.height);

        batch.setColor(Color.WHITE);
    }

    private void drawPageItems(int page, float xOffset) {
        int startIndex = page * PAGE_SIZE;
        int endIndex = Math.min(totalLevels, startIndex + PAGE_SIZE);
        for (int ii = startIndex; ii < endIndex; ii++) {
            Vector2 pos = getLevelPosition(ii);
            pos.x += xOffset;
            boolean selected = ii == selectedIndex;

            int score = controller.getLevelScore(ii+1);
            boolean unlocked = MASTER_UNLOCK || controller.isLevelOpen(ii+1);

            float size = selected && unlocked ? lilySizeSelected() : lilySizeNormal();

            if (lilyTexture != null && unlocked) {
                batch.setColor(selected ? Color.WHITE : new Color(0.82f, 0.82f, 0.82f, 1.0f));
                batch.draw(lilyTexture, pos.x - size / 2, pos.y - size / 2, size, size);
            } else {
                batch.setColor(Color.WHITE);
                batch.draw(grayLilyPad, pos.x - size / 2, pos.y - size / 2, size, size);
            }
            // Drawing the lilies
            if (starTexture != null && score > 0) {
                int maxScore  = 3;
                float starSize = size * 0.3f;
                float gap      = starSize * 0.1f;
                float totalW   = maxScore * starSize + (maxScore - 1) * gap;
                float startX   = pos.x - totalW / 2f;
                float flowerY  = pos.y + size / 2f;

                batch.setColor(Color.WHITE);
                for (int s = 0; s < maxScore; s++) {
                    int i = s == 1 ? 0 : -1;
                    float fx = startX + s * (starSize + gap);
                    if (s < score) {
                        batch.draw(starTexture, fx, flowerY + i * LILY_ARCH_OFFSET, starSize, starSize);
                    } else if (lilyFlowerGrayTexture != null) {
                        batch.draw(lilyFlowerGrayTexture, fx, flowerY + i * LILY_ARCH_OFFSET, starSize, starSize);
                    }
                }
            }

            // Drawing the numbers
            batch.setColor(Color.WHITE);
            Texture[] nums = unlocked ? numbersBlack : numbersWhite;
            int levelNum = ii + 1;

            float digitHeight = size * DIGIT_HEIGHT_RATIO;
            float gap         = size * DIGIT_GAP_RATIO;
            float cy          = pos.y + size * DIGIT_Y_RATIO;   // center line for the digits
            float y           = cy - digitHeight / 2f;

            if (levelNum > 9) {
                Texture tens = nums[levelNum / 10];
                Texture ones = nums[levelNum % 10];

                float tensW = digitHeight * (float) tens.getWidth() / tens.getHeight();
                float onesW = digitHeight * (float) ones.getWidth() / ones.getHeight();
                float totalW = tensW + gap + onesW;
                float startX = pos.x - totalW / 2f;

                drawDigit(batch, tens, startX,                 y, digitHeight);
                drawDigit(batch, ones, startX + tensW + gap,   y, digitHeight);
            } else {
                Texture d = nums[levelNum];
                float w = digitHeight * (float) d.getWidth() / d.getHeight();
                drawDigit(batch, d, pos.x - w / 2f, y, digitHeight);
            }
        }
        font.getData().setScale(1.0f);
    }

    private void drawDigit(SpriteBatch batch, Texture tex, float x, float y, float targetHeight) {
        float aspect = (float) tex.getWidth() / tex.getHeight();
        batch.draw(tex, x, y, targetHeight * aspect, targetHeight);
    }

    private void drawArrow(boolean isRight, boolean active) {
        float UI = CanvasRender.layoutScale();
        float arrowSize = ARROW_SIZE_REF * UI;
        float arrowPad = ARROW_PADDING_REF * UI;
        float cy = height * 0.5f;
        float x = isRight ? width - arrowPad - arrowSize : arrowPad;
        float y = cy - arrowSize / 2f;

        Rectangle bounds = isRight ? rightArrowBounds : leftArrowBounds;
        bounds.set(x, y, arrowSize, arrowSize);

        if (!active) return;

        Texture tex = isRight ? rightArrow : leftArrow;
        viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), pointer);
        boolean hovered = bounds.contains(pointer.x, pointer.y);
        batch.setColor(hovered ? MENU_HOVER_TINT : Color.WHITE);
        batch.draw(tex, x, y, arrowSize, arrowSize);
        batch.setColor(Color.WHITE);
    }

    private void drawTitle() {
        Rectangle r = computeTitleCanvasRect();
        if (r.width <= 0f || titleAnimation == null) {
            return;
        }
        TextureRegion tr = titleAnimation.getKeyFrame(titleAnimTime);
        batch.setColor(Color.WHITE);
        batch.draw(tr, r.x, r.y, r.width, r.height);
    }

    private Rectangle computeTitleCanvasRect() {
        if (titleAnimation == null || titleAnimation.getFrameCount() == 0) {
            return new Rectangle(0, 0, 0, 0);
        }
        TextureRegion tr = titleAnimation.getKeyFrame(titleAnimTime);
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
        return new Rectangle(x, y, drawW, drawH);
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
        if (titleAnimation != null) {
            titleAnimation.dispose();
            titleAnimation = null;
        }
        pixel.dispose();
        menuButtonTexture.dispose();
    }

    private void loadNumberTexures(AssetDirectory assets){
        for (int i = 0; i < 10; i++){
            String s = String.valueOf(i);
            numbersBlack[i] = assets.getEntry("numbers-B"+s, Texture.class);
            numbersWhite[i] = assets.getEntry("numbers-W"+s, Texture.class);
        }
    }
}
