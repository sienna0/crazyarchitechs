package edu.cornell.cis3152.physics.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import edu.cornell.gdiac.physics2.PolygonObstacle;

/**
 * Physics-only polygon used to supply collision without rendering duplicate art.
 */
public class InvisibleSurface extends ObstacleSprite {

    public InvisibleSurface(float[] points, float units, JsonValue settings) {
        super();
        obstacle = new PolygonObstacle(points);
        obstacle.setBodyType(BodyDef.BodyType.StaticBody);
        obstacle.setDensity(settings.getFloat("density", 0));
        obstacle.setFriction(settings.getFloat("friction", 0));
        obstacle.setRestitution(settings.getFloat("restitution", 0));
        obstacle.setPhysicsUnits(units);
        obstacle.setUserData(this);
        debug = ParserUtils.parseColor(settings.get("debug"), Color.WHITE);
    }

    @Override
    public void draw(SpriteBatch batch) {
        // Intentionally invisible.
    }
}
