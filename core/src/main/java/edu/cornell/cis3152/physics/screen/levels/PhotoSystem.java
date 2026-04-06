package edu.cornell.cis3152.physics.screen.levels;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.World;
import edu.cornell.cis3152.physics.InputController;
import edu.cornell.cis3152.physics.screen.WorldState;
import edu.cornell.cis3152.physics.world.GameObject;
import edu.cornell.cis3152.physics.world.Picture;
import edu.cornell.cis3152.physics.world.Quality;
import edu.cornell.cis3152.physics.world.Zuko;
import edu.cornell.gdiac.audio.SoundEffect;
import edu.cornell.gdiac.audio.SoundEffectManager;
import edu.cornell.gdiac.physics2.Obstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import edu.cornell.gdiac.util.PooledList;

/**
 * Handles photo interactions and highlight targeting.
 *
 * <p>Encapsulates taking pictures, sticking them onto targets, removing them, range/highlight
 * feedback, and lift spring forces so {@link LevelBaseScene} stays a thin orchestration layer.
 */
class PhotoSystem {
    private final WorldState worldState;
    private final float liftSpringStiffness;
    private final float liftSpringDamping;
    private final float volume;
    private final SoundEffect fireSound;
    private final SoundEffect plopSound;

    private float STICK_PICTURE_DISTANCE = 9.0f;
    private float TAKE_PICTURE_DISTANCE = 9.0f;

    public PhotoSystem (WorldState worldState,
                float stickDistance,
                float takeDistance,
                float liftSpringStiffness,
                float liftSpringDamping,
                float volume,
                SoundEffect fireSound,
                SoundEffect plopSound) {
        this.worldState = worldState;
        this.liftSpringStiffness = liftSpringStiffness;
        this.liftSpringDamping = liftSpringDamping;
        this.volume = volume;
        this.fireSound = fireSound;
        this.plopSound = plopSound;
    }

    /**
     * Keyboard shortcuts: drop/delete the active photo from inventory (Q) and toggle whether
     * photo range is drawn (Tab).
     */
    public void handlePictureShortcuts(InputController input, Zuko avatar) {
        if (input.didDropPhoto() && worldState.getActivePicture() != null) {
            Picture picture = avatar.getPictureInventory().getPicture(worldState.getSelectedSlotIndex());
            if (picture != null) {
                picture.clearSubject();
                worldState.getPictures().removeValue(picture, true);
            }
            worldState.setSelectedSlotIndex(-1);
            worldState.setActivePicture(null);
        }
        if (input.didToggleRange()) {
            worldState.setShowRange(!worldState.isShowRange());
        }

        // Number keys 1-5 select/deselect inventory slots
        int slot = input.getSlotSelect();
        if (slot >= 0 && slot < avatar.getPictureInventory().getSize()) {
            Picture slotPicture = avatar.getPictureInventory().getPicture(slot);
            if (slotPicture != null && slotPicture.hasSubject()) {
                if (slot == worldState.getSelectedSlotIndex()) {
                    worldState.setSelectedSlotIndex(-1);
                    worldState.setActivePicture(null);
                } else {
                    worldState.setSelectedSlotIndex(slot);
                    worldState.setActivePicture(slotPicture);
                }
            }
        }
    }

    /**
     * Maps horizontal input to movement force and primary action to jump for the avatar.
     */
    public void updateAvatarMovement(InputController input, Zuko avatar) {
        avatar.setMovement(input.getHorizontal() * avatar.getForce());
        avatar.setJumping(input.didPrimary());
    }

    /**
     * Hit-tests the crosshair against non-avatar sprites and stores the result on the avatar
     * as the current interaction target.
     *
     * @return the {@link GameObject} under the cursor, or {@code null}
     */
    public GameObject resolveCurrentTarget(InputController input, Zuko avatar, PooledList<ObstacleSprite> sprites) {
        Vector2 mouse = input.getCrossHair();
        GameObject target = findObjectUnderMouse(mouse.x, mouse.y, avatar, sprites);
        avatar.setCurrentTarget(target);
        return target;
    }

    /**
     * Clears and rebuilds {@link WorldState}'s highlight list: objects within line-of-sight
     * and photo range of Zuko (take vs stick range depends on whether a picture is active).
     */
    void updateHighlights(Zuko avatar, PooledList<ObstacleSprite> sprites, World world) {
        worldState.getHighlighted().clear();
        findObjectNearZuko(avatar, sprites, world);
    }

    /**
     * Per-frame picture UI and world actions: inventory slot clicks select/clear the active
     * picture; right-click on a target {@linkplain #removePictureFromTarget unsticks};
     * left-click with no active picture {@linkplain #takePictureOfTarget takes} a new one,
     * otherwise {@linkplain #applyPictureToTarget sticks} the active picture onto the target.
     */
    void handlePictureAction(InputController input,
                             GameObject target,
                             Zuko avatar,
                             int clickedSlot,
                             World world) {
        if (input.didLeftClick()) {
            if (clickedSlot >= 0) {
                Picture slotPicture = avatar.getPictureInventory().getPicture(clickedSlot);
                if (slotPicture != null && slotPicture.hasSubject()) {
                    if (worldState.getActivePicture() == null || clickedSlot != worldState.getSelectedSlotIndex()) {
                        worldState.setSelectedSlotIndex(clickedSlot);
                        worldState.setActivePicture(slotPicture);
                    } else {
                        worldState.setSelectedSlotIndex(-1);
                        worldState.setActivePicture(null);
                    }
                }
                return;
            }
        }

        if (target == null) {
            return;
        }
        if (input.didRightClick()) {
            removePictureFromTarget(target, avatar);
            return;
        }
        if (!input.didLeftClick()) {
            return;
        }
        if (worldState.getActivePicture() == null || worldState.getSelectedSlotIndex() == -1) {
            takePictureOfTarget(input, target, avatar, world);
        } else {
            applyPictureToTarget(target, avatar, world);
        }
    }

    /**
     * Applies spring-damper forces toward each eligible object's float home: clouds with
     * non-positive gravity scale, or any object currently affected by a lift-type picture.
     */
    void applyLiftSprings(PooledList<ObstacleSprite> sprites) {
        for (ObstacleSprite sprite : sprites) {
            if (!(sprite instanceof GameObject gameObject)) {
                continue;
            }
            boolean isCloud = gameObject.getObjectType() == edu.cornell.cis3152.physics.world.Obj.CLOUD;
            boolean springActive = isCloud
                    ? gameObject.getGravityScale() <= 0.0f
                    : gameObject.hasLiftPicture();
            if (!springActive) {
                continue;
            }

            Body body = gameObject.getObstacle().getBody();
            if (body == null) {
                continue;
            }

            Vector2 floatHome = gameObject.getFloatHome();
            float displacementX = floatHome.x - body.getPosition().x;
            float dampingX = -liftSpringDamping * body.getLinearVelocity().x;
            float springForceX = (liftSpringStiffness * displacementX) + dampingX;

            float displacementY = floatHome.y - body.getPosition().y;
            float dampingY = -liftSpringDamping * body.getLinearVelocity().y;
            float springForceY = (liftSpringStiffness * displacementY) + dampingY;

            body.applyForceToCenter(
                    body.getMass() * springForceX,
                    body.getMass() * springForceY,
                    true
            );
            body.setAngularVelocity(0.0f);
        }
    }

    /**
     * Sprite AABB test in physics-unit space: returns the topmost scanned {@link GameObject}
     * whose mesh bounds contain the crosshair (avatar excluded).
     */
    private GameObject findObjectUnderMouse(float mouseX, float mouseY, Zuko avatar, PooledList<ObstacleSprite> sprites) {
        for (ObstacleSprite sprite : sprites) {
            if (sprite == avatar || !(sprite instanceof GameObject go)) {
                continue;
            }

            Obstacle obj = go.getObstacle();
            float units = obj.getPhysicsUnits();
            Rectangle spriteBounds = sprite.getMesh().computeBounds();
            float centerX = obj.getX() * units;
            float centerY = obj.getY() * units;
            float minX = centerX + spriteBounds.x;
            float minY = centerY + spriteBounds.y;
            float maxX = minX + spriteBounds.width;
            float maxY = minY + spriteBounds.height;
            float posX = mouseX * units;
            float posY = mouseY * units;

            if (posX >= minX && posX <= maxX && posY >= minY && posY <= maxY) {
                return go;
            }
        }
        return null;
    }

    /**
     * If in range, film is available, and the camera allows a shot: records a new {@link Picture}
     * of the target, adds it to {@link WorldState} and the inventory, plays audio, and starts
     * the take-photo facing animation from mouse vs avatar position.
     */
    private void takePictureOfTarget(InputController input, GameObject target, Zuko avatar, World world) {
        if (!avatar.getCamera().canTakePicture(
                target.getObstacle().getX(),
                target.getObstacle().getY(),
                avatar.getObstacle().getX(),
                avatar.getObstacle().getY())) {
            return;
        }
        if (!hasFullLineOfSight(target, avatar, world, TAKE_PICTURE_DISTANCE)) {
            return;
        }
        if (avatar.getPictureInventory().getUnusedPicture() == null) {
            return;
        }

        avatar.getCamera().takePicture();
        Vector2 mousePosition = input.getCrossHair();
        Vector2 avatarPosition = avatar.getPosition();
        avatar.startTakingPhoto(mousePosition.x > avatarPosition.x);

        Picture picture = new Picture(target);
        worldState.getPictures().add(picture);
        avatar.getPictureInventory().addPicture(picture);
        SoundEffectManager.getInstance().play("plop", plopSound, Math.min(1.0f, volume * 1.75f));
    }

    /**
     * Populates highlights for every non-avatar object within line-of-sight and the current
     * photo range (stick distance when a picture is selected, take distance otherwise),
     * skipping when the player cannot take a new photo and no slot is selected.
     */
    private void findObjectNearZuko(Zuko avatar, PooledList<ObstacleSprite> sprites, World world) {
        Picture activePicture = worldState.getActivePicture();
        if (avatar.getPictureInventory().getUnusedPicture() == null && worldState.getSelectedSlotIndex() == -1) {
            return;
        }

        float range = (activePicture != null) ? STICK_PICTURE_DISTANCE: TAKE_PICTURE_DISTANCE;
        for (ObstacleSprite sprite : sprites) {
            if (sprite == avatar) continue;
            if (!(sprite instanceof GameObject go)) continue;
            if (hasFullLineOfSight(go, avatar, world, range)) {
                worldState.addHighlight(go);
            }

        }
    }

    /**
     * Sticks the active inventory picture onto {@code target}: clears any previous target's
     * attributes, transfers float-home behavior for float-quality subjects, removes the
     * picture from the hotbar, and plays the stick sound.
     */
    private void applyPictureToTarget(GameObject target, Zuko avatar, World world) {
        Picture activePicture = worldState.getActivePicture();
        if (activePicture == null) {
            return;
        }
        if (activePicture.getSubject() == null) {
            worldState.setActivePicture(null);
            return;
        }
        if (activePicture.getSubject() == target) {
            return;
        }
        if (!hasFullLineOfSight(target, avatar, world, STICK_PICTURE_DISTANCE)) {
            return;
        }

        if (activePicture.getTarget() != null) {
            activePicture.getTarget().resetAttributes();
        }
        activePicture.setTarget(target);
        if (activePicture.getSubject().getQuality() == Quality.FLOAT) {
            GameObject subject = activePicture.getSubject();
            target.setFloatHome(target.getObstacle().getX(), subject.getFloatHome().y);
        }

        int slotIndex = worldState.getSelectedSlotIndex();
        avatar.getPictureInventory().removePicture(slotIndex);
        worldState.setActivePicture(null);
        worldState.setSelectedSlotIndex(-1);

        if (avatar.getCurrentPlatform() == target) {
            avatar.setCurrentPlatform(target);
        }

        avatar.startTongueAnimation(target.getObstacle().getX(), target.getObstacle().getY());
        SoundEffectManager.getInstance().play("fire", fireSound, volume);
    }

    /**
     * Right-click unstick: finds a picture stuck on {@code target}, resets the object's
     * attributes, clears picture subject/target, removes it from the world list, and plays audio.
     */
    private void removePictureFromTarget(GameObject target, Zuko avatar) {
        Picture attachedPicture = findPictureOnTarget(target);
        if (attachedPicture == null) {
            return;
        }

        target.resetAttributes();
        attachedPicture.clearTarget();
        attachedPicture.clearSubject();
        worldState.getPictures().removeValue(attachedPicture, true);

        if (avatar.getCurrentPlatform() == target) {
            avatar.setCurrentPlatform(target);
        }
        SoundEffectManager.getInstance().play("plop", plopSound, volume);
    }

    /**
     * Linear search in {@link WorldState#getPictures()} for a picture whose stuck target is
     * {@code target}.
     */
    private Picture findPictureOnTarget(GameObject target) {
        for (Picture picture : worldState.getPictures()) {
            if (picture.getTarget() == target) {
                return picture;
            }
        }
        return null;
    }

    /**
     * A target is photographable only if every sampled point on it is both in range and hit
     * first by the raycast before any wall, invisible collider, or other object.
     */
    private boolean hasFullLineOfSight(GameObject target, Zuko avatar, World world, float maxDistance) {
        if (target == null || world == null) {
            return false;
        }

        for (Vector2 sample : getVisibilitySamples(target)) {
            if (!isSampleVisible(target, avatar, world, sample, maxDistance)) {
                return false;
            }
        }
        return true;
    }

    private Vector2[] getVisibilitySamples(GameObject target) {
        Rectangle bounds = target.getMesh().computeBounds();
        float units = target.getObstacle().getPhysicsUnits();
        float centerX = target.getObstacle().getX();
        float centerY = target.getObstacle().getY();
        float halfWidth = (bounds.width / units) * 0.5f;
        float halfHeight = (bounds.height / units) * 0.5f;
        float sampleOffsetX = Math.max(halfWidth * 0.5f, 0.05f);
        float sampleOffsetY = Math.max(halfHeight * 0.5f, 0.05f);

        return new Vector2[] {
                new Vector2(centerX, centerY),
                new Vector2(centerX - sampleOffsetX, centerY - sampleOffsetY),
                new Vector2(centerX - sampleOffsetX, centerY + sampleOffsetY),
                new Vector2(centerX + sampleOffsetX, centerY - sampleOffsetY),
                new Vector2(centerX + sampleOffsetX, centerY + sampleOffsetY)
        };
    }

    private boolean isSampleVisible(GameObject target, Zuko avatar, World world, Vector2 sample, float maxDistance) {
        float originX = avatar.getObstacle().getX();
        float originY = avatar.getObstacle().getY();
        float dx = sample.x - originX;
        float dy = sample.y - originY;
        if ((dx * dx + dy * dy) > (maxDistance * maxDistance)) {
            return false;
        }

        RaycastHit hit = new RaycastHit();
        world.rayCast((fixture, point, normal, fraction) -> {
            if (shouldIgnoreRaycastFixture(fixture, avatar)) {
                return -1.0f;
            }
            if (fraction < hit.fraction) {
                hit.fixture = fixture;
                hit.fraction = fraction;
            }
            return 1.0f;
        }, originX, originY, sample.x, sample.y);

        return hit.fixture != null && hit.fixture.getBody().getUserData() == target;
    }

    private boolean shouldIgnoreRaycastFixture(Fixture fixture, Zuko avatar) {
        if (fixture == null || fixture.isSensor()) {
            return true;
        }
        Object owner = fixture.getBody().getUserData();
        return owner == avatar;
    }

    private static final class RaycastHit {
        private Fixture fixture;
        private float fraction = Float.MAX_VALUE;
    }
}
