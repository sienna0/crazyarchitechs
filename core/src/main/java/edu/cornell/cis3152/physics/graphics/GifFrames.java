package edu.cornell.cis3152.physics.graphics;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Decodes an animated GIF into GPU textures for LibGDX (core library has no GIF animation support).
 * Use anywhere you need a looping GIF: title screens, UI accents, cutscenes, etc.
 * <p>
 * Call {@link #dispose()} when finished. Thread: load on the render thread before first draw.
 */
public final class GifFrames implements Disposable {

    private final TextureRegion[] regions;
    private final float[] frameDurationSeconds;
    private final Texture[] textures;
    private final float loopDurationSeconds;

    private GifFrames(TextureRegion[] regions, float[] frameDurationSeconds, Texture[] textures) {
        this.regions = regions;
        this.frameDurationSeconds = frameDurationSeconds;
        this.textures = textures;
        float sum = 0f;
        for (float d : frameDurationSeconds) {
            sum += d;
        }
        this.loopDurationSeconds = sum > 0.001f ? sum : 0.1f;
    }

    /**
     * @param file                  internal or absolute {@link FileHandle} to the {@code .gif}
     * @param defaultFrameSeconds   used when a frame omits delay metadata
     * @param filter                usually {@link Texture.TextureFilter#Nearest} for pixel art
     */
    public static GifFrames load(FileHandle file, float defaultFrameSeconds, Texture.TextureFilter filter)
            throws IOException {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
        if (!readers.hasNext()) {
            throw new IOException("No GIF ImageReader available");
        }
        ImageReader reader = readers.next();
        try (InputStream raw = file.read();
             ImageInputStream iis = ImageIO.createImageInputStream(raw)) {
            if (iis == null) {
                throw new IOException("Could not open GIF stream");
            }
            reader.setInput(iis, false, true);
            int n = reader.getNumImages(true);
            if (n <= 0) {
                throw new IOException("GIF has no frames");
            }
            TextureRegion[] regions = new TextureRegion[n];
            float[] delays = new float[n];
            Texture[] textures = new Texture[n];
            for (int i = 0; i < n; i++) {
                BufferedImage img = reader.read(i);
                delays[i] = readGraphicControlDelaySeconds(reader, i, defaultFrameSeconds);
                textures[i] = bufferedImageToTexture(img, filter);
                regions[i] = new TextureRegion(textures[i]);
            }
            return new GifFrames(regions, delays, textures);
        } finally {
            reader.dispose();
        }
    }

    public int getFrameCount() {
        return regions.length;
    }

    public float getLoopDurationSeconds() {
        return loopDurationSeconds;
    }

    /** Texture for the frame active at {@code stateTime} (loops). */
    public TextureRegion getKeyFrame(float stateTimeSeconds) {
        return regions[frameIndexAt(stateTimeSeconds)];
    }

    public int frameIndexAt(float stateTimeSeconds) {
        float t = stateTimeSeconds % loopDurationSeconds;
        float acc = 0f;
        for (int i = 0; i < frameDurationSeconds.length; i++) {
            acc += frameDurationSeconds[i];
            if (t < acc) {
                return i;
            }
        }
        return frameDurationSeconds.length - 1;
    }

    @Override
    public void dispose() {
        for (Texture t : textures) {
            t.dispose();
        }
    }

    private static float readGraphicControlDelaySeconds(ImageReader reader, int frameIndex,
                                                        float defaultSeconds) throws IOException {
        try {
            IIOMetadata meta = reader.getImageMetadata(frameIndex);
            IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree("javax_imageio_gif_image_1.0");
            if (root == null) {
                return defaultSeconds;
            }
            NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (!(child instanceof IIOMetadataNode node)) {
                    continue;
                }
                if (!"GraphicControlExtension".equals(node.getNodeName())) {
                    continue;
                }
                String d = node.getAttribute("delayTime");
                if (d != null && !d.isEmpty()) {
                    int hundredths = Integer.parseInt(d);
                    if (hundredths == 0) {
                        hundredths = Math.max(1, Math.round(defaultSeconds * 100f));
                    }
                    return hundredths / 100f;
                }
            }
        } catch (RuntimeException ignored) {
            // Malformed metadata
        }
        return defaultSeconds;
    }

    private static Texture bufferedImageToTexture(BufferedImage image, Texture.TextureFilter filter) {
        int width = image.getWidth();
        int height = image.getHeight();
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                float a = ((argb >> 24) & 0xff) / 255f;
                float r = ((argb >> 16) & 0xff) / 255f;
                float g = ((argb >> 8) & 0xff) / 255f;
                float b = (argb & 0xff) / 255f;
                pixmap.drawPixel(x, y, Color.rgba8888(r, g, b, a));
            }
        }
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        texture.setFilter(filter, filter);
        return texture;
    }
}
