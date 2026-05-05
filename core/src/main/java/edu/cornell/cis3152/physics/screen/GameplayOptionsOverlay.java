package edu.cornell.cis3152.physics.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import edu.cornell.cis3152.physics.CanvasRender;
import edu.cornell.cis3152.physics.GameAudio;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.TextAlign;
import edu.cornell.gdiac.graphics.TextLayout;

/**
 * In-game options overlay matching the title screen (music / sound / help + how-to panel).
 */
public class GameplayOptionsOverlay {

    private static final float MENU_MAIN_IDLE_SCALE = 0.92f;
    private static final Color MENU_MAIN_HOVER_TINT = new Color(0.52f, 0.52f, 0.55f, 1f);
    private static final Color OPTIONS_SCRIM = new Color(0f, 0f, 0f, 0.55f);
    private static final Color HELP_PANEL_FILL = new Color(0.07f, 0.09f, 0.11f, 0.88f);
    private static final Color HELP_PANEL_BORDER = new Color(0.42f, 0.45f, 0.50f, 0.72f);
    private static final int OPT_MUSIC = 0;
    private static final int OPT_SOUND = 1;
    private static final int OPT_HELP = 2;

    private final SpriteBatch batch;
    private final CanvasRender viewport;
    private final OrthographicCamera camera;
    private final Texture pixel;
    private final Texture musicTex;
    private final Texture soundOn;
    private final Texture soundOff;
    private final Texture helpTex;
    private final Texture sliderBarTex;
    private final Texture sliderToggleTex;
    private final BitmapFont font;
    private final TextLayout textLayout = new TextLayout();
    private final GlyphLayout helpMeasure = new GlyphLayout();
    private final Vector2 pointer = new Vector2();

    private int width;
    private int height;
    private boolean open;
    private boolean helpOpen;
    private boolean draggingMusicSlider;
    private boolean draggingSfxSlider;

    public GameplayOptionsOverlay(AssetDirectory assets, SpriteBatch batch, CanvasRender viewport) {
        this.batch = batch;
        this.viewport = viewport;
        this.camera = new OrthographicCamera();
        this.musicTex = assets.getEntry("shared-options-music", Texture.class);
        this.soundOn = assets.getEntry("shared-options-sound-on", Texture.class);
        this.soundOff = assets.getEntry("shared-options-sound-off", Texture.class);
        this.helpTex = assets.getEntry("shared-options-help", Texture.class);
        this.font = assets.getEntry("shared-retro", BitmapFont.class);
        this.sliderBarTex = new Texture(Gdx.files.internal("sliderbar.png"));
        this.sliderBarTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        this.sliderToggleTex = new Texture(Gdx.files.internal("slidertoggle.png"));
        this.sliderToggleTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();
        textLayout.setFont(font);
        textLayout.setAlignment(TextAlign.middleCenter);
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void resize(int screenW, int screenH) {
        this.width = (int) viewport.getWidth();
        this.height = (int) viewport.getHeight();
        camera.setToOrtho(false, this.width, this.height);
    }

    public void show() {
        open = true;
        helpOpen = false;
    }

    public void hide() {
        open = false;
        helpOpen = false;
        draggingMusicSlider = false;
        draggingSfxSlider = false;
    }

    public boolean isOpen() {
        return open;
    }

    public void update() {
        if (!open) return;

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (helpOpen) helpOpen = false;
            else hide();
            return;
        }

        viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), pointer);

        if (helpOpen) return;

        boolean btnDown = Gdx.input.isTouched() && Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        boolean btnJust = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);

        if (!btnDown) {
            draggingMusicSlider = false;
            draggingSfxSlider = false;
            return;
        }

        Rectangle musicBar = getMusicSliderBounds();
        Rectangle sfxBar   = getSfxSliderBounds();

        // Continue an active drag or start one if the pointer is over a slider bar.
        // No btnJust requirement for starting — the hit area is small so we allow
        // latching onto a drag on any held-button frame, not just the first one.
        if (draggingMusicSlider || musicBar.contains(pointer.x, pointer.y)) {
            draggingMusicSlider = true;
            GameAudio.setMusicVolume(MathUtils.clamp((pointer.x - musicBar.x) / musicBar.width, 0f, 1f));
            return;
        }
        if (draggingSfxSlider || sfxBar.contains(pointer.x, pointer.y)) {
            draggingSfxSlider = true;
            GameAudio.setSfxVolume(MathUtils.clamp((pointer.x - sfxBar.x) / sfxBar.width, 0f, 1f));
            return;
        }

        // Icon toggle clicks — only on the first frame of the press
        if (btnJust) {
            Rectangle[] r = computeOptionIconBounds();
            if (r[OPT_MUSIC].contains(pointer.x, pointer.y)) {
                GameAudio.toggleMusic();
            } else if (r[OPT_SOUND].contains(pointer.x, pointer.y)) {
                GameAudio.toggleSfx();
            } else if (r[OPT_HELP].contains(pointer.x, pointer.y)) {
                helpOpen = true;
            }
        }
    }

    public void draw() {
        if (!open) return;
        float UI = CanvasRender.layoutScale();
        font.getData().setScale(0.5f * UI);
        camera.update();
        viewport.apply();
        batch.begin(camera);
        batch.setColor(OPTIONS_SCRIM);
        batch.draw(pixel, 0, 0, width, height);
        batch.setColor(Color.WHITE);
        if (helpOpen) {
            drawHelpPanel(UI);
        } else {
            Rectangle[] r = computeOptionIconBounds();
            viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), pointer);
            Color musicTint = GameAudio.isMusicOn() ? Color.WHITE : new Color(0.5f, 0.5f, 0.52f, 1f);
            Texture soundTex = GameAudio.isSfxOn() ? soundOn : soundOff;
            drawOptionPuck(musicTex, r[OPT_MUSIC], r[OPT_MUSIC].contains(pointer.x, pointer.y), musicTint);
            drawOptionPuck(soundTex, r[OPT_SOUND], r[OPT_SOUND].contains(pointer.x, pointer.y), Color.WHITE);
            drawOptionPuck(helpTex, r[OPT_HELP], r[OPT_HELP].contains(pointer.x, pointer.y), Color.WHITE);

            Rectangle musicBar = getMusicSliderBounds();
            Rectangle sfxBar   = getSfxSliderBounds();
            drawSlider(musicBar, GameAudio.getMusicVolume());
            drawSlider(sfxBar,   GameAudio.getSfxVolume());

            textLayout.setFont(font);
            textLayout.setAlignment(TextAlign.middleCenter);
            textLayout.setColor(Color.WHITE);
            textLayout.setText("Press Esc to close");
            textLayout.layout();
            float hintY = Math.min(musicBar.y, sfxBar.y) - 22f * UI;
            batch.drawText(textLayout, width / 2f, hintY);
        }
        batch.end();
        viewport.reset();
    }

    public void dispose() {
        pixel.dispose();
        sliderBarTex.dispose();
        sliderToggleTex.dispose();
    }

    private Rectangle[] computeOptionIconBounds() {
        Texture ref = musicTex;
        int tw = ref != null ? ref.getWidth() : 1;
        int th = ref != null ? ref.getHeight() : 1;
        float sz = Math.min(width, height) * 0.16f;
        float s = sz / Math.max(1, Math.max(tw, th));
        float wIcon = tw * s;
        float hIcon = th * s;
        float gap = Math.min(width, height) * 0.065f;
        float totalW = 3f * wIcon + 2f * gap;
        float startX = (width - totalW) / 2f;
        float rowMidY = height * 0.55f;
        float y = rowMidY - hIcon / 2f;
        return new Rectangle[] {
                new Rectangle(startX, y, wIcon, hIcon),
                new Rectangle(startX + wIcon + gap, y, wIcon, hIcon),
                new Rectangle(startX + 2f * (wIcon + gap), y, wIcon, hIcon),
        };
    }

    private Rectangle getMusicSliderBounds() {
        Rectangle icon = computeOptionIconBounds()[OPT_MUSIC];
        return sliderBoundsBelow(icon);
    }

    private Rectangle getSfxSliderBounds() {
        Rectangle icon = computeOptionIconBounds()[OPT_SOUND];
        return sliderBoundsBelow(icon);
    }

    private Rectangle sliderBoundsBelow(Rectangle icon) {
        float UI = CanvasRender.layoutScale();
        float barH = 28f * UI;  // tall enough to click reliably; visual is drawn at half height
        float gapY = 8f * UI;
        return new Rectangle(icon.x, icon.y - gapY - barH, icon.width, barH);
    }

    private void drawSlider(Rectangle bar, float value) {
        float UI = CanvasRender.layoutScale();
        float visualH = 14f * UI;
        float visualY = bar.y + (bar.height - visualH) / 2f;

        // Bar (drawn at visual height, centered in the taller hit rect)
        batch.setColor(Color.WHITE);
        batch.draw(sliderBarTex, bar.x, visualY, bar.width, visualH);

        // Toggle knob
        float knobSize = visualH * 2.2f;
        float knobX = bar.x + value * bar.width - knobSize / 2f;
        float knobY = bar.y + bar.height / 2f - knobSize / 2f;
        batch.draw(sliderToggleTex, knobX, knobY, knobSize, knobSize);
        batch.setColor(Color.WHITE);
    }

    private void drawOptionPuck(Texture tex, Rectangle bounds, boolean hovered, Color baseTint) {
        if (tex == null) return;
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

    private void drawHelpPanel(float UI) {
        float scaleSave = font.getData().scaleX;
        font.getData().setScale(0.5f * UI);
        textLayout.setFont(font);
        textLayout.setAlignment(TextAlign.middleCenter);

        float lineStep = 40f * UI;
        float padX = 28f * UI;
        float padY = 22f * UI;
        float border = Math.max(1.5f, 2f * UI);
        String[] lines = PauseMenuScene.HOW_TO_PLAY_LINES;
        int rowCount = lines.length + 1;
        float totalH = rowCount * lineStep;

        helpMeasure.setText(font, PauseMenuScene.HOW_TO_PLAY_TITLE);
        float maxW = helpMeasure.width;
        for (String line : lines) {
            helpMeasure.setText(font, line);
            maxW = Math.max(maxW, helpMeasure.width);
        }

        float panelW = maxW + 2f * padX;
        float maxPanelW = width - 24f * UI;
        if (panelW > maxPanelW) panelW = maxPanelW;
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
        textLayout.setColor(Color.WHITE);
        textLayout.setText(PauseMenuScene.HOW_TO_PLAY_TITLE);
        textLayout.layout();
        batch.drawText(textLayout, width / 2f, yTitle);
        float cy = yTitle - lineStep;
        for (String line : lines) {
            textLayout.setColor(new Color(0.84f, 0.84f, 0.80f, 1f));
            textLayout.setText(line);
            textLayout.layout();
            batch.drawText(textLayout, width / 2f, cy);
            cy -= lineStep;
        }

        font.getData().setScale(scaleSave);
    }
}
