package edu.cornell.cis3152.physics.world;

/**
 * Strategy interface for object-based photo effects.
 *
 * Each photographable object type defines its own effect when its photo
 * is applied to another object. Implementations stage property changes
 * on the target GameObject's fields; the physics scene applies those
 * changes to Box2D bodies during postUpdate(), never during world.step().
 */
public interface ObjectEffect {
    /**
     * Apply this object's photo effect to the target.
     *
     * @param source the GameObject that was photographed
     * @param target the GameObject the photo is being stuck onto
     */
    void apply(GameObject source, GameObject target);

    /**
     * Remove this object's photo effect from the target, restoring original properties.
     *
     * @param target the GameObject to restore
     */
    void remove(GameObject target);
}
