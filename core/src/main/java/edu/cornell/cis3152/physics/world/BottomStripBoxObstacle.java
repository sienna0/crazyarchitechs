package edu.cornell.cis3152.physics.world;

import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.physics2.BoxObstacle;

/**
 * Box obstacle whose collision fixture only occupies a strip along the bottom edge.
 */
// for pulley string
public class BottomStripBoxObstacle extends BoxObstacle {
    private final float fullWidth;
    private final float fullHeight;
    private final float stripWidth;
    private final float stripHeight;

    public BottomStripBoxObstacle(float x, float y, float width, float height,
                                  float collisionWidth, float collisionHeight) {
        super(x, y, width, height);
        this.fullWidth = width;
        this.fullHeight = height;
        this.stripWidth = Math.max(0.05f, Math.min(width, collisionWidth));
        this.stripHeight = Math.max(0.03f, Math.min(height, collisionHeight));
    }

    @Override
    protected void createFixtures() {
        if (body == null || fullWidth <= 0.0f || fullHeight <= 0.0f) {
            return;
        }

        float halfStripHeight = stripHeight * 0.5f;
        float centerY = (-fullHeight * 0.5f) + halfStripHeight;
        shape.setAsBox(stripWidth * 0.5f, halfStripHeight, new Vector2(0.0f, centerY), 0.0f);
        super.createFixtures();
    }
}
