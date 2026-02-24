/*
 * Spinner.java
 *
 * This class provides a spinning rectangle on a fixed pin. We did not really
 * need a separate class for this, as it has no update. Like our other model
 * classes, it is solely for organizational purposes. It is a subclass of
 * ObstacleGroup because the primary purpose of this class is to initialize
 * the joints between obstacles.
 *
 * This is one of the files that you are expected to modify. Please limit
 * changes to the regions that say INSERT CODE HERE.
 *
 * Based on the original PhysicsDemo Lab by Don Holden, 2007
 *
 * Author:  Walker M. White
 * Version: 2/8/2025
 */
package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.joints.*;

import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.cis3152.physics.ObstacleGroup;
import edu.cornell.gdiac.physics2.*;

/**
 * A rectangular spinner on a fixed pin
 *
 * This method creates two ObstacleSprite objects: the barrier and pin.
 * Technically the pin will have no mesh so that it does not appear on the
 * screen. But by making it an ObstacleSprite, we can still see it in debug
 * mode (which is useful).
 */
public class Spinner extends ObstacleGroup {
    /** The initializing data (to avoid magic numbers) */
    private JsonValue data;

    /** The primary spinner obstacle */
    private ObstacleSprite barrier;

    private ObstacleSprite pinSprite;

    private WheelObstacle pin;

    /**
     * Creates a new spinner with the given physics data.
     *
     * The physics units are used to size the mesh (only barrier has a mesh)
     * relative to the physics body. All other attributes are defined by the
     * JSON file.
     *
     * @param units     The physics units
     * @param data      The physics constants for the spinner
     */
    public Spinner(float units, JsonValue data) {
        super();

        this.data = data;
        float x = data.get( "pos" ).getFloat( 0 );
        float y = data.get( "pos" ).getFloat( 1 );
        float w = data.get( "size" ).getFloat( 0 );
        float h = data.get( "size" ).getFloat( 1 );

        // Create the barrier
        Obstacle obstacle = new BoxObstacle( x, y, w, h );
        obstacle.setName( "barrier" );
        obstacle.setDensity( data.getFloat( "high density", 0 ) );
        obstacle.setPhysicsUnits( units ); // You must set this before wrapping with sprite

        // This automatically computes the mesh from the obstacle boundaries.
        // It works because we remembered to set physics units first.
        barrier = new ObstacleSprite(obstacle);
        barrier.setObstacle( obstacle );
        sprites.add( barrier );

        //#region INSERT CODE HERE
        // Create a pin to anchor the barrier
        // It should be a wheel obstacle wrapped in a ObstacleSprite
        //
        // Radius:  data.getFloat("radius")
        // Density: data.getFloat("low_density")
        // Name: "pin"

        float radius = data.getFloat("radius");
        float density = data.getFloat("low density");
        String name = "pin";

        pin = new WheelObstacle(x, y, radius);
        pin.setDensity(density);
        pin.setBodyType(BodyDef.BodyType.StaticBody);
        pin.setName(name);
        pin.setPhysicsUnits(units);
        pinSprite = new ObstacleSprite();
        pinSprite.setObstacle(pin);
        sprites.add(pinSprite);

        //#endregion
    }

    /**
     * Creates the joints for this obstacle group.
     *
     * This method is executed as part of activePhysics. This is the primary
     * method to override for custom physics objects.
     *
     * @param world the box2d world referencing the obstacles
     *
     * @return true if object allocation succeeded
     */
    protected boolean createJoints(World world) {
        assert sprites.size > 0;

        //#region INSERT CODE HERE
        // Attach the barrier to the pin here
        Vector2 anchorWorld = new Vector2(pin.getX(), pin.getY());
        Vector2 anchor1 = pinSprite.getObstacle().getBody().getLocalPoint(anchorWorld);
        Vector2 anchor2 = barrier.getObstacle().getBody().getLocalPoint(anchorWorld);

        RevoluteJointDef jointDef = new RevoluteJointDef();

        jointDef.bodyA = pinSprite.getObstacle().getBody();
        jointDef.bodyB = barrier.getObstacle().getBody();
        jointDef.localAnchorA.set(anchor1);
        jointDef.localAnchorB.set(anchor2);
        jointDef.collideConnected = false;
        Joint joint = world.createJoint(jointDef);
        joints.add(joint);

        //#endregion

        return true;
    }

    /**
     * Sets the texture for the barrier
     *
     * This texture is applied to the obstacle sprite of the barrier only.
     *
     * @param texture   The barrier texture
     */
    public void setTexture(Texture texture) {
        barrier.setTexture(texture);
    }
}
