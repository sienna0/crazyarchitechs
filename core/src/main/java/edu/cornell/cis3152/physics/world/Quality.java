package edu.cornell.cis3152.physics.world;

/**
 * Surface quality effects that a photograph can transfer onto targets.
 *
 * <ul>
 *   <li>{@link #FLOAT} &mdash; target hovers (cloud-like behavior)</li>
 *   <li>{@link #SLIPPERY} &mdash; removes friction from the target (ice-like)</li>
 *   <li>{@link #STICKY} &mdash; increases friction and reduces jump height (honey-like)</li>
 * </ul>
 */
public enum Quality {
    FLOAT,
    SLIPPERY,
    STICKY
}
