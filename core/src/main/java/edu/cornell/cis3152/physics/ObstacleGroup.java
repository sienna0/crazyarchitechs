/*
 * ObstacleGroup.java
 *
 * Box2d handles obstacles individually. But sometimes obstacles are grouped
 * together by joints. That is the purpose of this class -- to put obstacles
 * that are connected together by joints in a single container object.
 *
 * Over the years, we have gone back-and-forth about whether this belongs in
 * the package edu.cornell.gdiac.physics2.  While it is extremely useful for
 * this lab, it is not that important in broader applications. Currently we
 * have made this class unique to this lab.
 *
 * Based on the original PhysicsDemo Lab by Don Holden, 2007
 *
 * Author:  Walker M. White
 * Version: 2/8/2025
 */
package edu.cornell.cis3152.physics;

import com.badlogic.gdx.physics.box2d.Joint;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import edu.cornell.gdiac.physics2.*;

/**
 * Container class for organizing obstacles connected by joints.
 *
 * In this lab, several obstacles will be connected by joints (e.g. rope bridge,
 * ragdoll, and spinner). We have found it useful to create a container class
 * for these connected obstacles. While the individual obstacles are still
 * added to the scene individually, and drawn individually, this class provides
 * a central location for creating and destroying joints.
 */
public abstract class ObstacleGroup {
    /** A complex physics object has multiple bodies */
    protected Array<ObstacleSprite> sprites;
    /** Potential joints for connecting the multiple bodies */
    protected Array<Joint> joints;
    /** Whether physics is active for this group */
    protected boolean active;

    /**
     * Returns the collection of component physics objects.
     *
     * While the iterable does not allow you to modify the list, it is possible
     * to modify the individual objects.
     *
     * @return the collection of component physics objects.
     */
    public Iterable<ObstacleSprite> getSprites() {
        return sprites;
    }

    /**
     * Returns the collection of joints for this object (may be empty).
     *
     * While the iterable does not allow you to modify the list, it is possible
     * to modify the individual joints.
     *
     * @return the collection of joints for this object.
     */
    public Iterable<Joint> getJoints() {
        return joints;
    }

    /**
     * Creates a new, empty obstacle group.
     */
    protected ObstacleGroup() {
        sprites = new Array<ObstacleSprite>();
        joints = new Array<Joint>();
    }

    /**
     * Creates the physics body(s) for the obstacles, adding them to the world.
     *
     * This method invokes {@link Obstacle#activatePhysics} for the individual
     * obstacles in the list. It also calls the internal method
     * {@link #createJoints} to link them all together. You should override
     * that method, not this one, for specific physics objects.
     *
     * @param world the box2d world to generate the bodies
     *
     * @return true if object allocation succeeded
     */
    public boolean activatePhysics(World world) {
        active = true;

        // Create all other bodies.
        for(ObstacleSprite s : sprites) {
            Obstacle obj = s.getObstacle();
            active = active && obj.activatePhysics(world);
        }
        active = active && createJoints(world);

        // Clean up if we failed
        if (!active) {
            deactivatePhysics(world);
        }
        return active;
    }

    /**
     * Destroys the physics body(s) of all of the associated obstacles.
     *
     * The obstacles are immediately removed from the world
     *
     * @param world the box2d world to generate the bodies
     */
    public void deactivatePhysics(World world) {
        if (active) {
            // Should be good for most (simple) applications.
            for (Joint joint : joints) {
                world.destroyJoint(joint);
            }
            joints.clear();
            for(ObstacleSprite s : sprites) {
                Obstacle obj = s.getObstacle();
                obj.deactivatePhysics(world);
            }
            active = false;
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
    protected abstract boolean createJoints(World world);

}
