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
    private static final int OPT_MUSIC = 0;
    private static final int OPT_SOUND = 1;

    private static final float PANEL_W_FRAC = 5 / 6f;
    private static final float PANEL_H_FRAC = 5 / 6f;
    private static final float EXIT_W_FRAC = 0.09f;

    private final SpriteBatch batch;
    private final CanvasRender viewport;
    private final OrthographicCamera camera;
    private final Texture pixel;

    private final Texture sliderBarTex;
    private final Texture sliderToggleTex;

    private final Texture musicIcon;
    private final Texture soundIcon;
    private final Texture musicText;
    private final Texture soundText;
    private final Texture exitButton;

    private final Texture optionsTitle;
    private final Texture borderWooden;

    private final BitmapFont font;
    private final TextLayout textLayout = new TextLayout();
    private final Vector2 pointer = new Vector2();

    private int width;
    private int height;
    private boolean open;
    private boolean draggingMusicSlider;
    private boolean draggingSfxSlider;

    public GameplayOptionsOverlay(AssetDirectory assets, SpriteBatch batch, CanvasRender viewport) {
        this.batch = batch;
        this.viewport = viewport;
        this.camera = new OrthographicCamera();
        this.font = assets.getEntry("shared-retro", BitmapFont.class);
        this.sliderBarTex = new Texture(Gdx.files.internal("sliderbar.png"));
        this.sliderBarTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        this.sliderToggleTex = new Texture(Gdx.files.internal("slidertoggle.png"));
        this.sliderToggleTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        this.musicIcon = assets.getEntry("shared-options-music-icon", Texture.class);
        this.soundIcon = assets.getEntry("shared-options-sound-icon", Texture.class);
        this.musicText = assets.getEntry("shared-options-music-text", Texture.class);
        this.soundText = assets.getEntry("shared-options-sound-text", Texture.class);
        this.exitButton = assets.getEntry("shared-exit-button-options", Texture.class);
        this.optionsTitle = assets.getEntry("shared-options-text", Texture.class);
        this.borderWooden = assets.getEntry("shared-border-wooden", Texture.class);
        this.exitButton.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        this.borderWooden.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

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
    }

    public void hide() {
        open = false;
        draggingMusicSlider = false;
        draggingSfxSlider = false;
    }

    public boolean isOpen() {
        return open;
    }

    public void update() {
        if (!open) return;

        viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), pointer);

        boolean btnDown = Gdx.input.isTouched() && Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        boolean btnJust = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);

        if (!btnDown) {
            draggingMusicSlider = false;
            draggingSfxSlider = false;
            return;
        }

        Rectangle musicBar = getSliderBounds(OPT_MUSIC);
        Rectangle sfxBar = getSliderBounds(OPT_SOUND);

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
        if (btnJust && getExitBounds().contains(pointer.x, pointer.y)) {
            hide();
        }
        // Icon toggle clicks — only on the first frame of the press

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
        drawOptionsPanel(UI);
        batch.end();
        viewport.reset();
    }

    public void dispose() {
        pixel.dispose();
        sliderBarTex.dispose();
        sliderToggleTex.dispose();
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

    private float[] getIconBounds(float UI) {
        Rectangle panel = getPanelBounds();
        float     iconSize = panel.height * 0.14f;
        float     rowHeight = panel.height * 0.22f;
        float     contentH = 2 * rowHeight;
        float     startY = panel.y + (panel.height - contentH) / 2f + rowHeight * 0.8f;
        float     iconX = panel.x + panel.width * 0.15f;
        float     soundY = startY + (rowHeight - iconSize) / 2f;
        float     musicY = startY - rowHeight + (rowHeight - iconSize) / 2f;
        return new float[]{ iconX, soundY, iconSize, iconSize, iconX, musicY, iconSize, iconSize };
    }

    private Rectangle getSliderBounds(int row) {
        float UI = CanvasRender.layoutScale();
        Rectangle panel = getPanelBounds();
        float[] icons = getIconBounds(UI);
        float iconSize = icons[2];
        float iconX = icons[0];
        float iconY = (row == OPT_SOUND) ? icons[1] : icons[5];
        float sliderX = iconX + iconSize + panel.width * 0.4f;
        float sliderW = iconSize * 2.5f;
        float barH = 28f * UI;
        float sliderY = iconY + (iconSize - barH) / 2f;
        return new Rectangle(sliderX, sliderY, sliderW, barH);
    }

    private void drawOptionsPanel(float UI) {
        Rectangle panel = getPanelBounds();
        batch.setColor(Color.WHITE);
        if (borderWooden != null) {
            batch.draw(borderWooden, panel.x, panel.y, panel.width, panel.height);
        }
        if (optionsTitle != null) {
            float maxW = panel.width * 0.45f;
            float scale = maxW / optionsTitle.getWidth() * 0.8f;
            float tw = optionsTitle.getWidth() * scale;
            float th = optionsTitle.getHeight() * scale;
            batch.draw(optionsTitle, panel.x + (panel.width - tw) / 2f, panel.y + panel.height * 0.72f, tw, th);
        }
        viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), pointer);
        float[] icons = getIconBounds(UI);
        float iconSize = icons[2];

        Rectangle soundIconRect = new Rectangle(icons[0], icons[1], iconSize, iconSize);
        drawOptionPuck(soundIcon, soundIconRect, soundIconRect.contains(pointer.x, pointer.y), Color.WHITE);
        if (soundText != null) {
            float th = iconSize * 0.45f;
            float tw = soundText.getWidth() * (th / soundText.getHeight());
            batch.setColor(Color.WHITE);
            batch.draw(soundText, soundIconRect.x + iconSize + 8f * UI, soundIconRect.y + (iconSize - th) / 2f, tw, th);
        }
        drawSlider(getSliderBounds(OPT_SOUND), GameAudio.getSfxVolume());

        Rectangle musicIconRect = new Rectangle(icons[4], icons[5], iconSize, iconSize);
        drawOptionPuck(musicIcon, musicIconRect, musicIconRect.contains(pointer.x, pointer.y), Color.WHITE);
        if (musicText != null) {
            float th = iconSize * 0.45f;
            float tw = musicText.getWidth() * (th / musicText.getHeight());
            batch.setColor(Color.WHITE);
            batch.draw(musicText, musicIconRect.x + iconSize + 8f * UI, musicIconRect.y + (iconSize - th) / 2f, tw, th);
        }
        drawSlider(getSliderBounds(OPT_MUSIC), GameAudio.getMusicVolume());

        drawExit();

    }

    private void drawExit() {
        if (exitButton == null) return;
        Rectangle eb = getExitBounds();
        boolean hov = eb.contains(pointer.x, pointer.y);
        batch.setColor(hov ? new Color(0.75f, 0.75f, 0.75f, 1f) : Color.WHITE);
        batch.draw(exitButton, eb.x, eb.y, eb.width, eb.height);
        batch.setColor(Color.WHITE);
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
}
