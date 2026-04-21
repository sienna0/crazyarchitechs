package edu.cornell.cis3152.physics.screen.levels;

import edu.cornell.cis3152.physics.CanvasRender;

/**
 * Shared layout for top-right settings / pause HUD: one place for sizes used by {@link LevelRenderer} and hit tests in {@link LevelBaseScene}.
 */
final class LevelHud {

    private LevelHud() {
    }

    static float baseIconSize() {
        return 56f * CanvasRender.layoutScale();
    }

    static float hoverIconSize() {
        return 64f * CanvasRender.layoutScale();
    }

    static float margin() {
        return 15f * CanvasRender.layoutScale();
    }

    static float iconGap() {
        return 12f * CanvasRender.layoutScale();
    }

    /** Uniform scale on the drawn quad so both sprites read at similar visual weight. */
    static float iconDrawScale() {
        return 1.22f;
    }
}
