/*
 * RopeBridge.java
 *
 * This class provides a bridge of connected planks. We did not really need a
 * separate class for this, as it has no update. Like our other model classes,
 * it is solely for organizational purposes. It is a subclass of ObstacleGroup
 * because the primary purpose of this class is to initialize the joints
 * between obstacles.
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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.joints.*;

import com.badlogic.gdx.utils.JsonValue;

import edu.cornell.cis3152.physics.ObstacleGroup;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.physics2.*;

/**
 * A bridge with planks connected by revolute joints.
 *
 * Note that this class has two box2d bodies which are NOT obstacles. That
 * means they have no volume so they are not drawn on the screen and they
 * cannot collide with anything. Because ObstacleGroup is designed to manage
 * obstacles and not stand-alone bodies, that means we have to manually clean
 * up after ourselves in {@link #deactivatePhysics}. In addition, because these
 * bodies are not obstacles, they do not show up in debug mode.
 */
public class RopeBridge extends ObstacleGroup {
    /** The initializing data (to avoid magic numbers) */
    private JsonValue data;

    // Invisible anchor objects
    /** The left side of the bridge */
    private WheelObstacle start = null;
    /** The right side of the bridge */
    private WheelObstacle finish = null;

    // Dimension information
    /** The size of the entire bridge */
    protected Vector2 dimension;
    /** The size of a single plank */
    protected Vector2 planksize;
    /* The length of each link */
    protected float linksize = 1.0f;
    /** The spacing between each link */
    protected float spacing = 0.0f;

    /**
     * Creates a new rope bridge with the given physics data
     *
     * This bridge is straight horizontal. The coordinates taken from the
     * JSOIN define the lefmost anchor. * The physics units are used to size
     * the meshes to each plank.
     *
     * @param units     The physics units
     * @param data      The physics constants for this rope bridge
     */
    public RopeBridge(float units, JsonValue data) {
        super();

        this.data = data;
        float x0 = data.get("pos").getFloat(0);
        float y0 = data.get("pos").getFloat(1);
        float lw = data.get("size").getFloat(0);
        float lh = data.get("size").getFloat(1);

        planksize = new Vector2(lw,lh);
        linksize = planksize.x;

        // Compute the bridge length
        float extent = data.getFloat( "extent", 0 );
        dimension = new Vector2(extent,0);
        float length = dimension.len();
        Vector2 norm = new Vector2(dimension);
        norm.nor();

        // If too small, only make one plank.
        int nLinks = (int)(length / linksize);
        if (nLinks <= 1) {
            nLinks = 1;
            linksize = length;
            spacing = 0;
        } else {
            spacing = length - nLinks * linksize;
            spacing /= (nLinks-1);
        }

        Color color =  ParserUtils.parseColor( data.get("debug"), Color.WHITE);
        float density = data.getFloat("density",0);

        // Create the planks
        planksize.x = linksize;
        Vector2 pos = new Vector2();
        for (int ii = 0; ii < nLinks; ii++) {
            float t = ii*(linksize+spacing) + linksize/2.0f;
            pos.set(norm);
            pos.scl(t);
            pos.add(x0,y0);

            BoxObstacle plank = new BoxObstacle(pos.x, pos.y, planksize.x, planksize.y);
            plank.setName("plank"+ii);
            plank.setDensity(density);
            plank.setPhysicsUnits( units ); // YOU MUST DO THIS BEFORE WRAPPING WITH SPRITE

            // This constructor AUTOMATICALLY makes a mesh for us
            // Because we set the physics units for the obstacle, it uses
            // that value and the fixture shapes to create the mesh.
            ObstacleSprite sprite = new ObstacleSprite(plank);
            sprite.setDebugColor( color );

            sprites.add(sprite);
        }
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
    @Override
    protected boolean createJoints(World world) {
        assert sprites.size > 0;

        Vector2 anchor1 = new Vector2();
        Vector2 anchor2 = new Vector2(-linksize / 2, 0);

        // Create the leftmost anchor
        // Normally, we would do this in constructor, but we have
        // reasons to not add the anchor to the bodies list.
        Obstacle obs = sprites.get(0).getObstacle();

        Vector2 pos = obs.getPosition();
        pos.x -= linksize / 2;
        start = new WheelObstacle(pos.x,pos.y,data.getFloat("pin_radius", 1));
        start.setName("pin0");
        start.setDensity(data.getFloat("density", 0));
        start.setBodyType(BodyDef.BodyType.StaticBody);
        start.activatePhysics(world);

        // Definition for a revolute joint
        RevoluteJointDef jointDef = new RevoluteJointDef();

        // Initial joint
        jointDef.bodyA = start.getBody();
        jointDef.bodyB = obs.getBody();
        jointDef.localAnchorA.set(anchor1);
        jointDef.localAnchorB.set(anchor2);
        jointDef.collideConnected = false;
        Joint joint = world.createJoint(jointDef);
        joints.add(joint);

        // Link the planks together
        anchor1.x = linksize / 2;
        for (int ii = 0; ii < sprites.size-1; ii++) {
            //#region INSERT CODE HERE
            // Look at what we did above
            RevoluteJointDef jointDefNew = new RevoluteJointDef();
            jointDefNew.bodyA = sprites.get(ii).getObstacle().getBody();
            jointDefNew.bodyB = sprites.get(ii + 1).getObstacle().getBody();
            jointDefNew.localAnchorA.set(anchor1);
            jointDefNew.localAnchorB.set(anchor2);
            jointDefNew.collideConnected = false;
            Joint jointNew = world.createJoint(jointDefNew);
            joints.add(jointNew);
            //#endregion
        }

        // Create the rightmost anchor
        Obstacle last = sprites.get(sprites.size-1).getObstacle();

        pos = last.getPosition();
        pos.x += linksize / 2;
        finish = new WheelObstacle(pos.x,pos.y,data.getFloat("pin_radius", 1));
        finish.setName("pin1");
        finish.setDensity(data.getFloat("density", 0));
        finish.setBodyType(BodyDef.BodyType.StaticBody);
        finish.activatePhysics(world);

        // Final joint
        anchor2.x = 0;
        jointDef.bodyA = last.getBody();
        jointDef.bodyB = finish.getBody();
        jointDef.localAnchorA.set(anchor1);
        jointDef.localAnchorB.set(anchor2);
        joint = world.createJoint(jointDef);
        joints.add(joint);

        return true;
    }

    /**
     * Destroys the physics body(s) of all of the associated obstacles.
     *
     * The obstacles are immediately removed from the world
     *
     * @param world the box2d world to generate the bodies
     */
    @Override
    public void deactivatePhysics(World world) {
        super.deactivatePhysics(world);
        if (start != null) {
            start.deactivatePhysics(world);
        }
        if (finish != null) {
            finish.deactivatePhysics(world);
        }
    }

    /**
     * Sets the texture for the individual planks
     *
     * @param texture the texture for the individual planks
     */
    public void setTexture(Texture texture) {
        for(ObstacleSprite sprite : sprites) {
            sprite.setTexture( texture );
        }
    }
}
