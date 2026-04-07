package edu.cornell.cis3152.physics;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.*;

import com.badlogic.gdx.utils.Array;
import edu.cornell.gdiac.util.*;

/**
 * Singleton that buffers input from the keyboard, mouse, and Xbox controller and exposes
 * semantic game actions.
 *
 * <p>Use {@link #getInstance()} for the shared instance. Call {@link #sync(Rectangle, Vector2, CanvasRender)}
 * once per frame to sample all devices and update one-frame edge detection (comparing
 * current and previous button state).
 *
 * <p>When a gamepad is connected, {@link #readGamepad(Rectangle, Vector2)} reads Xbox buttons
 * and sticks (crosshair nudges use the left stick with momentum); {@link #readKeyboard(Rectangle, Vector2, CanvasRender, boolean)}
 * still runs as a merge/backup. Otherwise only keyboard and mouse are used. The keyboard path
 * maps the mouse through the viewport into world coordinates.
 *
 * <p>Edge-triggered helpers (true only on the frame the control first goes down) include
 * {@link #didLeftClick()} and {@link #didRightClick()} for mouse buttons, {@link #didToggleRange()}
 * for Tab, {@link #didFlicStickToggle()} for I, and {@link #didDropPhoto()} for Q.
 */
public class InputController {
    // Sensitivity for moving crosshair with gameplay
    private static final float GP_ACCELERATE = 1.0f;
    private static final float GP_MAX_SPEED  = 10.0f;
    private static final float GP_THRESHOLD  = 0.01f;

    /** The singleton instance of the input controller */
    private static InputController theController = null;

    /**
     * Returns the singleton instance of the input controller
     *
     * @return the singleton instance of the input controller
     */
    public static InputController getInstance() {
        if (theController == null) {
            theController = new InputController();
        }
        return theController;
    }

    // Fields to manage buttons
    /** Whether the reset button was pressed. */
    private boolean resetPressed;
    private boolean resetPrevious;
    /** Whether the button to advanced worlds was pressed. */
    private boolean nextPressed;
    private boolean nextPrevious;
    /** Whether the button to step back worlds was pressed. */
    private boolean prevPressed;
    private boolean prevPrevious;
    /** Whether the primary action button was pressed. */
    private boolean primePressed;
    private boolean primePrevious;
    /** Whether the secondary action button was pressed. */
    private boolean secondPressed;
    private boolean secondPrevious;
    /** Whether the teritiary action button was pressed. */
    private boolean tertiaryPressed;
    /** Whether the debug toggle was pressed. */
    private boolean debugPressed;
    private boolean debugPrevious;
    /** Whether the exit button was pressed. */
    private boolean exitPressed;
    private boolean exitPrevious;

    private boolean dropPressed;
    private boolean dropPrevious;

    /** Whether the flic stick toggle button was pressed. */
    private boolean flicStickPressed;
    private boolean flicStickPrevious;

    /** How much did we move horizontally? */
    private float horizontal;
    /** How much did we move vertically? */
    private float vertical;
    /** The crosshair position (for raddoll) */
    private Vector2 crosshair;
    /** The crosshair cache (for using as a return value) */
    private Vector2 crosscache;
    /** For the gamepad crosshair control */
    private float momentum;

    /** Whether the camera button was pressed */
    private boolean leftClickPressed;
    private boolean leftClickPrevious;

    private boolean rightClickPressed;
    private boolean rightClickPrevious;

    /** Whether the range key button was pressed */
    private boolean rangePressed;
    private boolean rangePrevious;

    /** Slot selected via number keys 1-5 this frame, or -1 if none. */
    private int slotSelectPressed = -1;

    /** An X-Box controller (if it is connected) */
    XBoxController xbox;

    /**
     * Returns the amount of sideways movement.
     *
     * -1 = left, 1 = right, 0 = still
     *
     * @return the amount of sideways movement.
     */
    public float getHorizontal() {
        return horizontal;
    }

    /**
     * Returns the amount of vertical movement.
     *
     * -1 = down, 1 = up, 0 = still
     *
     * @return the amount of vertical movement.
     */
    public float getVertical() {
        return vertical;
    }

    /**
     * Returns the current position of the crosshairs on the screen.
     *
     * This value does not return the actual reference to the crosshairs
     * position. That way this method can be called multiple times without any
     * fear that the position has been corrupted. However, it does return the
     * same object each time. So if you modify the object, the object will be
     * reset in a subsequent call to this getter.
     *
     * @return the current position of the crosshairs on the screen.
     */
    public Vector2 getCrossHair() {
        return crosscache.set(crosshair);
    }

    /**
     * Returns true if the primary action button was pressed.
     *
     * This is a one-press button. It only returns true at the moment it was
     * pressed, and returns false at any frame afterwards.
     *
     * @return true if the primary action button was pressed.
     */
    public boolean didPrimary() {
        return primePressed && !primePrevious;
    }

    /**
     * Returns true if the secondary action button was pressed.
     *
     * This is a one-press button. It only returns true at the moment it was
     * pressed, and returns false at any frame afterwards.
     *
     * @return true if the secondary action button was pressed.
     */
    public boolean didSecondary() {
        return secondPressed && !secondPrevious;
    }

    /**
     * Returns true if the tertiary action button was pressed.
     *
     * This is a sustained button. It will returns true as long as the player
     * holds it down.
     *
     * @return true if the secondary action button was pressed.
     */
    public boolean didTertiary() {
        return tertiaryPressed;
    }

    /**
     * Returns true if the reset button was pressed.
     *
     * @return true if the reset button was pressed.
     */
    public boolean didReset() {
        return resetPressed && !resetPrevious;
    }

    /**
     * Whether the drop-photo control was pressed this frame (Q), using one-frame edge detection.
     *
     * @return true only on the first frame the key is down after having been up
     */
    public boolean didDropPhoto() {
        return dropPressed && !dropPrevious;
    }

    /**
     * Returns true if the player wants to go to the next level.
     *
     * @return true if the player wants to go to the next level.
     */
    public boolean didAdvance() {
        return nextPressed && !nextPrevious;
    }

    /**
     * Returns true if the player wants to go to the previous level.
     *
     * @return true if the player wants to go to the previous level.
     */
    public boolean didRetreat() {
        return prevPressed && !prevPrevious;
    }

    /**
     * Returns true if the player wants to go toggle the debug mode.
     *
     * @return true if the player wants to go toggle the debug mode.
     */
    public boolean didDebug() {
        return debugPressed && !debugPrevious;
    }

    /**
     * Returns true if the exit button was pressed.
     *
     * @return true if the exit button was pressed.
     */
    public boolean didExit() {
        return exitPressed && !exitPrevious;
    }

    /**
     * Creates a new input controller
     *
     * The input controller attempts to connect to the X-Box controller at
     * device 0, if it exists. Otherwise, it falls back to the keyboard
     * control.
     */
    public InputController() {
        // If we have a game-pad for id, then use it.
        Array<XBoxController> controllers = Controllers.get().getXBoxControllers();
        if (controllers.size > 0) {
            xbox = controllers.get( 0 );
        } else {
            xbox = null;
        }
        crosshair = new Vector2();
        crosscache = new Vector2();
    }

    /**
     * Called once per frame to sample keyboard, mouse, and (if connected) Xbox input.
     *
     * <p>Copies prior-frame button state for edge detection, then reads the gamepad and/or
     * keyboard paths so {@link #didLeftClick()}, {@link #didReset()}, and similar methods
     * reflect the current frame.
     *
     * @param bounds   rectangle that clamps the crosshair (mouse/gamepad aim)
     * @param scale    drawing scale; combined with the viewport for screen-to-world conversion
     * @param viewport used to convert mouse screen coordinates to canvas/world space
     */
    public void sync(Rectangle bounds, Vector2 scale, CanvasRender viewport, OrthographicCamera camera) {
        // Copy state from last animation frame
        // Helps us ignore buttons that are held down
        dropPrevious = dropPressed;
        primePrevious  = primePressed;
        secondPrevious = secondPressed;
        resetPrevious  = resetPressed;
        debugPrevious  = debugPressed;
        exitPrevious = exitPressed;
        nextPrevious = nextPressed;
        prevPrevious = prevPressed;
        leftClickPrevious = leftClickPressed;
        rightClickPrevious = rightClickPressed;
        rangePrevious = rangePressed;
        flicStickPrevious = flicStickPressed;


        // Check to see if a GamePad is connected
        if (xbox != null && xbox.isConnected()) {
            readGamepad(bounds, scale);
            readKeyboard(bounds, scale, viewport, camera, true); // Read as a back-up
        } else {
            readKeyboard(bounds, scale, viewport, camera, false);
        }
    }

    /**
     * Reads Xbox controller buttons, triggers, and sticks for this frame.
     *
     * <p>Updates movement axes, action buttons, and crosshair motion from the sticks
     * (with momentum), then clamps the crosshair to {@code bounds}. {@code scale} scales
     * stick deltas into world space.
     *
     * @param bounds crosshair clamp rectangle
     * @param scale  inverse scale applied to stick deltas for world-space movement
     */
    private void readGamepad(Rectangle bounds, Vector2 scale) {
        resetPressed = xbox.getStart();
        exitPressed  = xbox.getBack();
        nextPressed  = xbox.getRBumper();
        prevPressed  = xbox.getLBumper();
        primePressed = xbox.getA();
        debugPressed  = xbox.getY();

        // Increase animation frame, but only if trying to move
        horizontal = xbox.getLeftX();
        vertical   = xbox.getLeftY();
        secondPressed = xbox.getRightTrigger() > 0.6f;

        // Move the crosshairs with the right stick.
        tertiaryPressed = xbox.getA();
        crosscache.set(xbox.getLeftX(), xbox.getLeftY());
        if (crosscache.len2() > GP_THRESHOLD) {
            momentum += GP_ACCELERATE;
            momentum = Math.min(momentum, GP_MAX_SPEED);
            crosscache.scl(momentum);
            crosscache.scl(1/scale.x,1/scale.y);
            crosshair.add(crosscache);
        } else {
            momentum = 0;
        }
        clampPosition(bounds);
    }

    /**
     * Reads keyboard keys and mouse buttons for this frame, and positions the crosshair from the mouse.
     *
     * <p>When {@code secondary} is true (gamepad also active), each key/button state is OR'd with
     * values already set by the gamepad so held gamepad actions are preserved. Mouse position is
     * converted from screen space to canvas coordinates via {@code viewport}, then scaled by
     * {@code scale} into world space; the result is clamped to {@code bounds}.
     *
     * @param bounds    crosshair clamp rectangle
     * @param scale     drawing scale for screen-to-world conversion after {@code viewport}
     * @param viewport  maps mouse screen coordinates to canvas space
     * @param secondary if true, merge with existing gamepad-derived state instead of replacing it
     */
    private void readKeyboard(Rectangle bounds, Vector2 scale, CanvasRender viewport, OrthographicCamera camera, boolean secondary) {
        // Give priority to gamepad results
        dropPressed = (secondary && dropPressed) || (Gdx.input.isKeyPressed(Input.Keys.Q));
        resetPressed = (secondary && resetPressed) || (Gdx.input.isKeyPressed(Input.Keys.R));
        debugPressed = (secondary && debugPressed) || (Gdx.input.isKeyPressed(Input.Keys.SLASH));
        primePressed = (secondary && primePressed) || (Gdx.input.isKeyPressed(Input.Keys.W));
        secondPressed = (secondary && secondPressed) || (Gdx.input.isKeyPressed(Input.Keys.SPACE));
        prevPressed = (secondary && prevPressed) || (Gdx.input.isKeyPressed(Input.Keys.P));
        nextPressed = (secondary && nextPressed) || (Gdx.input.isKeyPressed(Input.Keys.N));
        exitPressed  = (secondary && exitPressed) || (Gdx.input.isKeyPressed(Input.Keys.ESCAPE));
        flicStickPressed  = (secondary && flicStickPressed) || (Gdx.input.isKeyPressed(Input.Keys.I));

        // Directional controls
        horizontal = (secondary ? horizontal : 0.0f);
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            horizontal += 1.0f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            horizontal -= 1.0f;
        }

        vertical = (secondary ? vertical : 0.0f);
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            vertical += 1.0f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            vertical -= 1.0f;
        }

        //Range Control
        rangePressed = (secondary && rangePressed) || (Gdx.input.isKeyPressed(Input.Keys.TAB));

        // Inventory slot selection via number keys (1-5 map to slots 0-4)
        slotSelectPressed = -1;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) slotSelectPressed = 0;
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) slotSelectPressed = 1;
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) slotSelectPressed = 2;
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) slotSelectPressed = 3;
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)) slotSelectPressed = 4;

        // Mouse results
        tertiaryPressed = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        leftClickPressed = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        rightClickPressed = Gdx.input.isButtonPressed(Input.Buttons.RIGHT);
        viewport.screenToCanvas(Gdx.input.getX(), Gdx.input.getY(), crosshair);
        if (camera != null) {
            float visibleWidth = camera.viewportWidth * camera.zoom;
            float visibleHeight = camera.viewportHeight * camera.zoom;
            float worldPixelX = (camera.position.x - visibleWidth * 0.5f) + (crosshair.x / viewport.getWidth()) * visibleWidth;
            float worldPixelY = (camera.position.y - visibleHeight * 0.5f) + (crosshair.y / viewport.getHeight()) * visibleHeight;
            crosshair.set(worldPixelX / scale.x, worldPixelY / scale.y);
        } else {
            crosshair.scl(1/scale.x,1/scale.y);
        }
        clampPosition(bounds);
    }

    /**
     * Clamps the cursor position so that it does not go outside the window
     *
     * While this is not usually a problem with mouse control, this is critical
     * for the gamepad controls.
     */
    private void clampPosition(Rectangle bounds) {
        crosshair.x = Math.max(bounds.x, Math.min(bounds.x+bounds.width, crosshair.x));
        crosshair.y = Math.max(bounds.y, Math.min(bounds.y+bounds.height, crosshair.y));
    }

    /**
     * One-frame edge detection for the primary (left) mouse button: true only when the button
     * transitions from up to down this frame.
     *
     * @return true on the first frame the left button is pressed
     */
    public boolean didLeftClick() {
        return leftClickPressed && !leftClickPrevious;
    }

    /**
     * One-frame edge detection for the secondary (right) mouse button.
     *
     * @return true on the first frame the right button is pressed
     */
    public boolean didRightClick() {
        return rightClickPressed && !rightClickPrevious;
    }

    /**
     * One-frame edge detection for the range toggle (Tab key).
     *
     * @return true on the first frame Tab is pressed after being released
     */
    public boolean didToggleRange() {
        return rangePressed && ! rangePrevious;
    }

    /**
     * One-frame edge detection for the flic-stick toggle (I key).
     *
     * @return true on the first frame I is pressed after being released
     */
    public boolean didFlicStickToggle() {
        return flicStickPressed && !flicStickPrevious;
    }

    /**
     * Returns the inventory slot selected via number keys this frame (0-4),
     * or -1 if no number key was pressed.
     */
    public int getSlotSelect() {
        return slotSelectPressed;
    }
}
