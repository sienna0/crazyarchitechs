package edu.cornell.cis3152.physics.world;

/**
 * Effect applied when an ice photo is stuck onto another object.
 * Transfers the ice's elasticity and friction (slippery behavior).
 */
public class IceEffect implements ObjectEffect {

    @Override
    public void apply(GameObject source, GameObject target) {
        target.elasticity = source.getOriginalElasticity();
        target.friction = source.getOriginalFriction();
        target.pictureQuality = Quality.SLIPPERY;
        target.pendingPhysicsSync = true;
    }

    @Override
    public void remove(GameObject target) {
        target.restoreOriginalProperties();
    }
}
