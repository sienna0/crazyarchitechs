package edu.cornell.cis3152.physics.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.BodyDef;
import edu.cornell.gdiac.graphics.SpriteBatch;

/**
 * A collectible fly object. Static sensor body; Zuko collects it with his tongue on click.
 * The 32x16 spritesheet is split into two 16x16 frames that animate at a fixed rate.
 */
public class FlyCollectible extends BoxSprite {

    public static final float FLY_SIZE = 0.5f;
    private static final float FRAME_DURATION = 0.15f;
    private static final int FRAME_COUNT = 2;

    private final TextureRegion[] frames;
    private float animTime = 0f;
    private boolean collected = false;

    public FlyCollectible(float units, float x, float y, Texture sheet, int index) {
        super(units, x, y, FLY_SIZE, FLY_SIZE,
                BodyDef.BodyType.StaticBody, true, true,
                0f, 0f, 0f, 0f,
                "fly_" + index,
                sheet);

        int frameW = sheet.getWidth() / FRAME_COUNT;
        int frameH = sheet.getHeight();
        frames = new TextureRegion[FRAME_COUNT];
        for (int i = 0; i < FRAME_COUNT; i++) {
            frames[i] = new TextureRegion(sheet, i * frameW, 0, frameW, frameH);
        }
        setTextureRegion(frames[0]);
    }

    public boolean isCollected() {
        return collected;
    }

    public void markCollected() {
        collected = true;
    }

    @Override
    public void update(float dt) {
        if (collected) return;
        animTime += dt;
        int frame = ((int) (animTime / FRAME_DURATION)) % FRAME_COUNT;
        setTextureRegion(frames[frame]);
        super.update(dt);
    }

    @Override
    public void draw(SpriteBatch batch) {
        if (collected) return;
        super.draw(batch);
    }
}
