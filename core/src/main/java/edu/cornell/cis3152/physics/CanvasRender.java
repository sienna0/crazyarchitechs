package edu.cornell.cis3152.physics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Tracks the game's letterboxed viewport and input coordinate conversion.
 */
public class CanvasRender {
    /** The original game design resolution */
    public static final int DESIGN_WIDTH = 1280;
    /** The original game design resolution */
    public static final int DESIGN_HEIGHT = 720;

    /** The current screen width */
    private int screenWidth;
    /** The current screen height */
    private int screenHeight;
    /** Letterboxed viewport x-offset */
    private int viewportX;
    /** Letterboxed viewport y-offset */
    private int viewportY;
    /** Letterboxed viewport width */
    private int viewportWidth;
    /** Letterboxed viewport height */
    private int viewportHeight;

    public CanvasRender() {
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    /**
     * Updates the letterboxed viewport for the current screen size.
     */
    public void resize(int width, int height) {
        screenWidth = Math.max(1, width);
        screenHeight = Math.max(1, height);

        float scale = Math.min(
                (float) screenWidth / DESIGN_WIDTH,
                (float) screenHeight / DESIGN_HEIGHT
        );

        viewportWidth = Math.max(1, Math.round(DESIGN_WIDTH * scale));
        viewportHeight = Math.max(1, Math.round(DESIGN_HEIGHT * scale));
        viewportX = (screenWidth - viewportWidth) / 2;
        viewportY = (screenHeight - viewportHeight) / 2;
    }

    /**
     * Applies the letterboxed viewport before drawing.
     */
    public void apply() {
        Gdx.gl.glViewport(viewportX, viewportY, viewportWidth, viewportHeight);
    }

    /**
     * Restores the full window viewport after drawing.
     */
    public void reset() {
        Gdx.gl.glViewport(0, 0, screenWidth, screenHeight);
    }

    /**
     * Converts a raw screen-space input position to the letterboxed canvas.
     */
    public Vector2 screenToCanvas(float screenX, float screenY, Vector2 out) {
        float viewportTop = screenHeight - viewportY - viewportHeight;
        float localX = (screenX - viewportX) / viewportWidth;
        float localY = (screenY - viewportTop) / viewportHeight;

        out.x = MathUtils.clamp(localX, 0.0f, 1.0f) * DESIGN_WIDTH;
        out.y = (1.0f - MathUtils.clamp(localY, 0.0f, 1.0f)) * DESIGN_HEIGHT;
        return out;
    }

    public float getWidth() {
        return DESIGN_WIDTH;
    }

    public float getHeight() {
        return DESIGN_HEIGHT;
    }
}
