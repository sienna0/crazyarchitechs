package edu.cornell.cis3152.physics.screen.levels;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
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
    }

    public void updateAvatarMovement(InputController input, Zuko avatar) {
        avatar.setMovement(input.getHorizontal() * avatar.getForce());
        avatar.setJumping(input.didPrimary());
    }

    public GameObject resolveCurrentTarget(InputController input, Zuko avatar, PooledList<ObstacleSprite> sprites) {
        Vector2 mouse = input.getCrossHair();
        GameObject target = findObjectUnderMouse(mouse.x, mouse.y, avatar, sprites);
        avatar.setCurrentTarget(target);
        return target;
    }

    void updateHighlights(Zuko avatar, PooledList<ObstacleSprite> sprites) {
        worldState.getHighlighted().clear();
        findObjectNearZuko(avatar, sprites);
    }

    void handlePictureAction(InputController input,
                             GameObject target,
                             Zuko avatar,
                             int clickedSlot) {
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
            takePictureOfTarget(input, target, avatar);
        } else {
            applyPictureToTarget(target, avatar);
        }
    }

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

    private void takePictureOfTarget(InputController input, GameObject target, Zuko avatar) {
        if (!avatar.getCamera().canTakePicture(
                target.getObstacle().getX(),
                target.getObstacle().getY(),
                avatar.getObstacle().getX(),
                avatar.getObstacle().getY())) {
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

    private void findObjectNearZuko(Zuko avatar, PooledList<ObstacleSprite> sprites) {
        Picture activePicture = worldState.getActivePicture();
        if (avatar.getPictureInventory().getUnusedPicture() == null && worldState.getSelectedSlotIndex() == -1) {
            return;
        }

        float range = (activePicture != null) ? STICK_PICTURE_DISTANCE: TAKE_PICTURE_DISTANCE;
        for (ObstacleSprite sprite : sprites) {
            if (sprite == avatar) continue;
            if (!(sprite instanceof GameObject go)) continue;
            float x = go.getObstacle().getX();
            float y = go.getObstacle().getY();
            if (avatar.getCamera().hasLineOfSight(x, y, avatar.getObstacle().getX(), avatar.getObstacle().getY(), range)) {
                worldState.addHighlight(go);
            }

        }
    }

    private void applyPictureToTarget(GameObject target, Zuko avatar) {
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
        if (!avatar.getCamera().hasLineOfSight(
                target.getObstacle().getX(),
                target.getObstacle().getY(),
                avatar.getObstacle().getX(),
                avatar.getObstacle().getY(),
                STICK_PICTURE_DISTANCE)) {
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

        SoundEffectManager.getInstance().play("fire", fireSound, volume);
    }

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

    private Picture findPictureOnTarget(GameObject target) {
        for (Picture picture : worldState.getPictures()) {
            if (picture.getTarget() == target) {
                return picture;
            }
        }
        return null;
    }
}
