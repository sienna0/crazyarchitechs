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

    public LevelSelectScene(AssetDirectory assets, SpriteBatch batch, CanvasRender viewport, int totalLevels) {
        this.batch = batch;
        this.viewport = viewport;
        this.totalLevels = totalLevels;
        this.font = assets.getEntry("shared-retro", BitmapFont.class);
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
        titleLayout.setText("SELECT LEVEL");
        titleLayout.layout();

        font.getData().setScale(0.4f);
        instructionLayout = new TextLayout();
        instructionLayout.setFont(font);
        instructionLayout.setAlignment(TextAlign.middleCenter);
        instructionLayout.setColor(new Color(0.89f, 0.87f, 0.76f, 1.0f));
        instructionLayout.setText("ARROWS OR MOUSE, ENTER TO START, ESC TO QUIT");
        instructionLayout.layout();
        font.getData().setScale(1f);

        optionLayout = new TextLayout();
        optionLayout.setFont(font);
        optionLayout.setAlignment(TextAlign.middleCenter);

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
            int clickedIndex = getHoveredIndex();
            if (clickedIndex >= 0) {
                selectedIndex = clickedIndex;
                chosenLevel = clickedIndex + 1;
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
    }

    private void draw() {
        ScreenUtils.clear(0.0f, 0.0f, 0.0f, 1.0f);
        camera.update();

        viewport.apply();
        batch.begin(camera);
        batch.drawText(titleLayout, width / 2.0f, height - 100.0f);
        batch.drawText(instructionLayout, width / 2.0f, height - 170.0f);

        for (int ii = 0; ii < totalLevels; ii++) {
            Rectangle bounds = getButtonBounds(ii);
            boolean selected = ii == selectedIndex;

            batch.setColor(selected ? new Color(0.80f, 0.31f, 0.18f, 1.0f) : new Color(0.23f, 0.29f, 0.33f, 1.0f));
            batch.draw(pixel, bounds.x, bounds.y, bounds.width, bounds.height);

            font.getData().setScale(0.85f);
            optionLayout.setColor(selected ? Color.WHITE : new Color(0.84f, 0.84f, 0.80f, 1.0f));
            optionLayout.setText("LEVEL " + (ii + 1));
            optionLayout.layout();
            batch.drawText(optionLayout, bounds.x + bounds.width / 2.0f, bounds.y + bounds.height / 2.0f + 2.0f);
        }
        font.getData().setScale(1.0f);

        batch.end();
        viewport.reset();
    }

    private Rectangle getButtonBounds(int index) {
        float buttonWidth = Math.min(width * 0.55f, 520.0f);
        float buttonHeight = 64.0f;
        float gap = 16.0f;
        float totalHeight = totalLevels * buttonHeight + (totalLevels - 1) * gap;
        float startY = (height - totalHeight) / 2.0f -40f;
        float x = (width - buttonWidth) / 2.0f;
        float y = startY + (totalLevels - 1 - index) * (buttonHeight + gap);
        return new Rectangle(x, y, buttonWidth, buttonHeight);
    }

    private int getHoveredIndex() {
        viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), pointer);
        float x = pointer.x;
        float y = pointer.y;
        for (int ii = 0; ii < totalLevels; ii++) {
            if (getButtonBounds(ii).contains(x, y)) {
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
