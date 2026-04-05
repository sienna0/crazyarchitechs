package edu.cornell.cis3152.physics.world;

/**
 * Effect applied when a cloud photo is stuck onto another object.
 * Transfers the cloud's weight and gravity scale (float behavior).
 */
public class CloudEffect implements ObjectEffect {

    @Override
    public void apply(GameObject source, GameObject target) {
        target.weight = source.getOriginalWeight();
        target.gravityScale = source.getOriginalGravityScale();
        target.pictureQuality = Quality.FLOAT;
        target.pendingPhysicsSync = true;
    }

    @Override
    public void remove(GameObject target) {
        target.restoreOriginalProperties();
    }
}
