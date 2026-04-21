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
            BufferedImage[] frames = new BufferedImage[n];
            for (int i = 0; i < n; i++) {
                frames[i] = reader.read(i);
            }
            int[] crop = unionOpaqueCropPx(frames);
            TextureRegion[] regions = new TextureRegion[n];
            float[] delays = new float[n];
            Texture[] textures = new Texture[n];
            for (int i = 0; i < n; i++) {
                delays[i] = readGraphicControlDelaySeconds(reader, i, defaultFrameSeconds);
                BufferedImage img = applyCrop(frames[i], crop);
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

    /**
     * Smallest axis-aligned rect (in pixel coords, top-left origin) that contains every non-transparent
     * pixel in every frame. Removes empty GIF margins so layout uses visible art, not the full canvas.
     */
    private static int[] unionOpaqueCropPx(BufferedImage[] frames) {
        if (frames.length == 0) {
            return null;
        }
        int w0 = frames[0].getWidth();
        int h0 = frames[0].getHeight();
        for (int i = 1; i < frames.length; i++) {
            if (frames[i].getWidth() != w0 || frames[i].getHeight() != h0) {
                return null;
            }
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (BufferedImage frame : frames) {
            int[] b = opaqueBoundsPx(frame);
            if (b == null) {
                continue;
            }
            minX = Math.min(minX, b[0]);
            minY = Math.min(minY, b[1]);
            maxX = Math.max(maxX, b[2]);
            maxY = Math.max(maxY, b[3]);
        }
        if (minX > maxX) {
            return null;
        }
        if (minX <= 0 && minY <= 0 && maxX >= w0 - 1 && maxY >= h0 - 1) {
            return null;
        }
        return new int[] { minX, minY, maxX - minX + 1, maxY - minY + 1 };
    }

    /** Inclusive max x,y in top-left coordinates, or null if image has no opaque pixels. */
    private static int[] opaqueBoundsPx(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int minX = w;
        int minY = h;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = (img.getRGB(x, y) >>> 24) & 0xff;
                if (a > 8) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < minX) {
            return null;
        }
        return new int[] { minX, minY, maxX, maxY };
    }

    private static BufferedImage applyCrop(BufferedImage img, int[] crop) {
        if (crop == null) {
            return img;
        }
        int x = crop[0];
        int y = crop[1];
        int cw = crop[2];
        int ch = crop[3];
        if (x == 0 && y == 0 && cw == img.getWidth() && ch == img.getHeight()) {
            return img;
        }
        if (x < 0 || y < 0 || x + cw > img.getWidth() || y + ch > img.getHeight()) {
            return img;
        }
        return img.getSubimage(x, y, cw, ch);
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
