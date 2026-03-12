package edu.cornell.cis3152.physics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.graphics.TextLayout;
import edu.cornell.gdiac.math.Path2;
import edu.cornell.gdiac.physics2.ObstacleSprite;

/**
 * Primary view class for the game, handling all drawing.
 *
 * This class is a wrapper around SpriteBatch that allows us to
 * decouple the drawing code from the LibGDX sprite batch.
 */
public class GameCanvas {
    /** The sprite batch for drawing */
    private SpriteBatch batch;

    /**
     * Creates a new GameCanvas
     */
    public GameCanvas() {
        batch = new SpriteBatch();
    }

    /**
     * Returns the sprite batch for this canvas.
     *
     * @return the sprite batch for this canvas
     */
    public SpriteBatch getSpriteBatch() {
        return batch;
    }

    /**
     * Compute the transformation matrix for the given parameters.
     *
     * @param transform The matrix to store the result
     * @param ox The x-origin
     * @param oy The y-origin
     * @param tx The x-translation
     * @param ty The y-translation
     * @param angle The rotation angle (in degrees)
     * @param sx The x-scale
     * @param sy The y-scale
     */
    public static void computeTransform(Affine2 transform, float ox, float oy,
                                        float tx, float ty, float angle, float sx, float sy) {
        SpriteBatch.computeTransform(transform, ox, oy, tx, ty, angle, sx, sy);
    }

    /**
     * Eliminates this canvas and its resources.
     */
    public void dispose() {
        batch.dispose();
        batch = null;
    }

    /**
     * Starts the drawing loop.
     *
     * @param camera The camera for this scene
     */
    public void begin(OrthographicCamera camera) {
        batch.begin(camera);
    }

    /**
     * Ends the drawing loop.
     */
    public void end() {
        batch.end();
    }

    /**
     * Draws the given texture to the canvas.
     *
     * @param texture The texture to draw
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @param width The width
     * @param height The height
     */
    public void draw(Texture texture, float x, float y, float width, float height) {
        batch.draw(texture, x, y, width, height);
    }

    /**
     * Draws the given texture region to the canvas.
     *
     * @param region The texture region to draw
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @param width The width
     * @param height The height
     */
    public void draw(TextureRegion region, float x, float y, float width, float height) {
        batch.draw(region, x, y, width, height);
    }

    /**
     * Draws the given texture with an affine transform.
     *
     * @param texture The texture to draw
     * @param transform The affine transform
     */
    public void draw(Texture texture, Affine2 transform) {
        batch.draw(texture, transform);
    }

    /**
     * Draws the given obstacle sprite to the canvas.
     *
     * @param sprite The obstacle sprite to draw
     */
    public void draw(ObstacleSprite sprite) {
        sprite.draw(batch);
    }

    /**
     * Draws the debug information for the given obstacle sprite.
     *
     * @param sprite The obstacle sprite to draw
     */
    public void drawDebug(ObstacleSprite sprite) {
        sprite.drawDebug(batch);
    }

    /**
     * Outlines the given path.
     *
     * @param path The path to outline
     * @param transform The transform to use
     */
    public void outline(Path2 path, Affine2 transform) {
        batch.outline(path, transform);
    }

    /**
     * Draws text to the canvas.
     *
     * @param layout The text layout
     * @param x The x-coordinate
     * @param y The y-coordinate
     */
    public void drawText(TextLayout layout, float x, float y) {
        batch.drawText(layout, x, y);
    }

    /**
     * Sets the color for the next drawing operations.
     *
     * @param color The color to set
     */
    public void setColor(Color color) {
        batch.setColor(color);
    }

    /**
     * Sets the texture for the next drawing operations.
     *
     * @param texture The texture to set
     */
    public void setTexture(Texture texture) {
        batch.setTexture(texture);
    }

    /**
     * Returns the width of this canvas.
     *
     * @return the width of this canvas
     */
    public float getWidth() {
        return Gdx.graphics.getWidth();
    }

    /**
     * Returns the height of this canvas.
     *
     * @return the height of this canvas
     */
    public float getHeight() {
        return Gdx.graphics.getHeight();
    }
}
