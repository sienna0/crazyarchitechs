package edu.cornell.cis3152.physics.world;

/**
 * Effect applied when a honey (sticky) photo is stuck onto another object.
 * Transfers the honey's elasticity and friction (sticky behavior).
 */
public class HoneyEffect implements ObjectEffect {

    @Override
    public void apply(GameObject source, GameObject target) {
        target.elasticity = source.getOriginalElasticity();
        target.friction = source.getOriginalFriction();
        target.pictureQuality = Quality.STICKY;
        target.pendingPhysicsSync = true;
    }

    @Override
    public void remove(GameObject target) {
        target.restoreOriginalProperties();
    }
}
