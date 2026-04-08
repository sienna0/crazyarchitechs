package edu.cornell.cis3152.physics.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.physics.box2d.BodyDef;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import edu.cornell.gdiac.physics2.WheelObstacle;

/**
 * Generic textured circle-backed physics sprite for round gameplay objects.
 */
public class WheelSprite extends ObstacleSprite {

    public WheelSprite(float units, float x, float y, float radius,
                       BodyDef.BodyType bodyType, boolean sensor, boolean fixedRotation,
                       float density, float friction, float restitution, float gravityScale,
                       String name, Texture texture) {
        super();

        WheelObstacle body = new WheelObstacle(x, y, radius);
        body.setBodyType(bodyType);
        body.setPhysicsUnits(units);
        body.setUserData(this);
        body.setSensor(sensor);
        body.setFixedRotation(fixedRotation);
        body.setDensity(density);
        body.setFriction(friction);
        body.setRestitution(restitution);
        body.setGravityScale(gravityScale);
        body.setName(name);
        obstacle = body;

        float diameter = radius * 2.0f * units;
        mesh.set(-diameter / 2.0f, -diameter / 2.0f, diameter, diameter);

        debug = Color.WHITE;
        setTexture(texture);
    }
}
