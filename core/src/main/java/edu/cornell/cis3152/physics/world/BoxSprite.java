package edu.cornell.cis3152.physics.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.physics.box2d.BodyDef;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;

/**
 * Generic textured box-backed physics sprite for non-photo gameplay objects.
 */
public class BoxSprite extends ObstacleSprite {

    public BoxSprite(float units, float x, float y, float width, float height,
                     BodyDef.BodyType bodyType, boolean sensor, boolean fixedRotation,
                     float density, float friction, float restitution, float gravityScale,
                     String name, Texture texture) {
        super();

        BoxObstacle body = new BoxObstacle(x, y, width, height);
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

        float drawW = width * units;
        float drawH = height * units;
        mesh.set(-drawW / 2.0f, -drawH / 2.0f, drawW, drawH);

        debug = Color.WHITE;
        setTexture(texture);
    }
}
