package edu.cornell.cis3152.physics.graphics;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/**
 * Simple horizontal sprite-strip animation backed by a single texture.
 */
public final class SpriteStripAnimation implements Disposable {

    private final Texture texture;
    private final TextureRegion[] frames;
    private final float frameDurationSeconds;
    private final float loopDurationSeconds;

    private SpriteStripAnimation(Texture texture, TextureRegion[] frames, float frameDurationSeconds) {
        this.texture = texture;
        this.frames = frames;
        this.frameDurationSeconds = frameDurationSeconds;
        this.loopDurationSeconds = Math.max(0.001f, frames.length * frameDurationSeconds);
    }

    public static SpriteStripAnimation loadHorizontalStrip(FileHandle file, int frameCount,
                                                           Texture.TextureFilter filter,
                                                           float frameDurationSeconds) {
        if (frameCount <= 0) {
            throw new IllegalArgumentException("frameCount must be positive");
        }
        Texture texture = new Texture(file);
        texture.setFilter(filter, filter);
        if (texture.getWidth() % frameCount != 0) {
            texture.dispose();
            throw new IllegalArgumentException("Texture width must be divisible by frameCount: " + file.path());
        }
        int frameWidth = texture.getWidth() / frameCount;
        int frameHeight = texture.getHeight();
        TextureRegion[] frames = new TextureRegion[frameCount];
        for (int i = 0; i < frameCount; i++) {
            frames[i] = new TextureRegion(texture, i * frameWidth, 0, frameWidth, frameHeight);
        }
        return new SpriteStripAnimation(texture, frames, frameDurationSeconds);
    }

    public static SpriteStripAnimation loadSquareStrip(FileHandle file, float frameDurationSeconds,
                                                       Texture.TextureFilter filter) {
        Texture probe = new Texture(file);
        int frameSize = probe.getHeight();
        int frameCount = frameSize <= 0 ? 0 : probe.getWidth() / frameSize;
        boolean valid = frameSize > 0 && probe.getWidth() >= frameSize && probe.getWidth() % frameSize == 0;
        probe.dispose();
        if (!valid) {
            throw new IllegalArgumentException("Expected horizontal square sprite strip: " + file.path());
        }
        return loadHorizontalStrip(file, frameCount, filter, frameDurationSeconds);
    }

    public int getFrameCount() {
        return frames.length;
    }

    public TextureRegion getKeyFrame(float stateTimeSeconds) {
        int frame = (int)(stateTimeSeconds / frameDurationSeconds);
        frame %= frames.length;
        if (frame < 0) {
            frame += frames.length;
        }
        return frames[frame];
    }

    public float getLoopDurationSeconds() {
        return loopDurationSeconds;
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
