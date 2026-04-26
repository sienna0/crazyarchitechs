package edu.cornell.cis3152.physics.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import edu.cornell.gdiac.graphics.SpriteBatch;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Spawns a short confetti burst at a world-space position when a fly is collected.
 * Particles are drawn as small rotating colored squares in world/pixel space.
 */
public class ConfettiSystem {

    private static final Color[] PALETTE = {
        new Color(1f, 0.25f, 0.25f, 1f),
        new Color(1f, 0.82f, 0.08f, 1f),
        new Color(0.22f, 0.92f, 0.38f, 1f),
        new Color(0.25f, 0.58f, 1f, 1f),
        new Color(1f, 0.45f, 0.08f, 1f),
        new Color(0.85f, 0.25f, 1f, 1f),
        new Color(1f, 0.45f, 0.75f, 1f),
    };

    private static final int COUNT = 12;
    private static final float LIFETIME = 0.7f;
    private static final float GRAVITY = -280f;

    private static class Particle {
        float x, y, vx, vy, life, rotation, rotSpeed, size;
        final Color color = new Color();
    }

    private final ArrayList<Particle> active = new ArrayList<>();

    /**
     * Spawns a burst of confetti at the given world-space position.
     *
     * @param worldX world x in physics units
     * @param worldY world y in physics units
     * @param units  physics-to-pixel scale factor
     */
    public void spawn(float worldX, float worldY, float units) {
        float px = worldX * units;
        float py = worldY * units;
        for (int i = 0; i < COUNT; i++) {
            Particle p = new Particle();
            p.x = px;
            p.y = py;
            float angle = MathUtils.random(MathUtils.PI2);
            float speed = MathUtils.random(20f, 70f);
            p.vx = MathUtils.cos(angle) * speed;
            p.vy = MathUtils.sin(angle) * speed + MathUtils.random(15f, 45f);
            p.life = LIFETIME;
            p.rotation = MathUtils.random(360f);
            p.rotSpeed = MathUtils.random(-360f, 360f);
            p.size = MathUtils.random(1.5f, 3.5f);
            p.color.set(PALETTE[MathUtils.random(PALETTE.length - 1)]);
            active.add(p);
        }
    }

    public void update(float dt) {
        Iterator<Particle> it = active.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.x  += p.vx * dt;
            p.y  += p.vy * dt;
            p.vy += GRAVITY * dt;
            p.rotation += p.rotSpeed * dt;
            p.life -= dt;
            if (p.life <= 0f) it.remove();
        }
    }

    public boolean isEmpty() { return active.isEmpty(); }

    /** Renders all live particles. Must be called inside an active SpriteBatch world-space block. */
    public void draw(SpriteBatch batch, Texture pixel) {
        for (Particle p : active) {
            float alpha = p.life / LIFETIME;
            batch.setColor(p.color.r, p.color.g, p.color.b, alpha);
            float half = p.size * 0.5f;
            batch.draw(pixel,
                    p.x - half, p.y - half,
                    half, half,
                    p.size, p.size,
                    1f, 1f,
                    p.rotation,
                    0, 0, 1, 1,
                    false, false);
        }
        batch.setColor(Color.WHITE);
    }
}
