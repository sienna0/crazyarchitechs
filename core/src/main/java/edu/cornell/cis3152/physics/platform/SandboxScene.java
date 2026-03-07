/*
 * PlatformScene.java
 *
 * This is the game scene (player mode) specific to the platforming mini-game.
 * You SHOULD NOT need to modify this file. However, you may learn valuable
 * lessons for the rest of the lab by looking at it.
 *
 * Based on the original PhysicsDemo Lab by Don Holden, 2007
 *
 * Author:  Walker M. White
 * Version: 2/8/2025
 */
package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.WeldJointDef;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.cis3152.physics.InputController;
import edu.cornell.cis3152.physics.PhysicsScene;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.audio.SoundEffect;
import edu.cornell.gdiac.audio.SoundEffectManager;
import edu.cornell.gdiac.math.Path2;
import edu.cornell.gdiac.math.PathFactory;
import edu.cornell.gdiac.physics2.Obstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;

/**
 * The game scene for the platformer game.
 *
 * Look at the method {@link #populateLevel} for how we initialize the scene.
 * Beyond that, a lot of work is done in the method for the ContactListener
 * interface. That is the method that is called upon collisions, giving us a
 * chance to define a response.
 */
public class SandboxScene extends PhysicsScene implements ContactListener {
    /** Texture asset for character avatar */
    private TextureRegion avatarTexture;
    /** Texture asset for the spinning barrier */
    private TextureRegion barrierTexture;
    /** Texture asset for the bullet */
    private TextureRegion bulletTexture;
    /** Texture asset for the bridge plank */
    private TextureRegion bridgeTexture;

    /** The jump sound. We only want to play once. */
    private SoundEffect jumpSound;
    /** The weapon fire sound. We only want to play once. */
    private SoundEffect fireSound;
    /** The weapon pop sound. We only want to play once. */
    private SoundEffect plopSound;
    /** The default sound volume */
    private float volume;

    /** Reference to the character avatar */
    private Zuko avatar;
    /** Reference to the goalDoor (for collision detection) */
    private Door goalDoor;

    //** Picture list */
    private Array<Picture> pictures = new Array<>();
    private Picture activePicture;

    private GameObject rock;
    private GameObject cloud;
    private GameObject ice;

    // Rock lift behavior (cloud photo on rock)
    private float rockLiftCeilingY;
    private boolean rockLiftActive;

    // Cloud behavior (rock photo on cloud)
    private boolean cloudDropActive;      // true = cloud behaves like rock (falls)
    private boolean cloudReturnActive;    // true = cloud is lifting back up
    private float cloudLiftCeilingY;      // target Y to lift back to

    // Ice-on-cloud: cloud lowers to half height and freezes
    private boolean iceOnCloudActive;

    // Ice-on-rock: rock becomes slippery (low friction)
    private boolean iceOnRockActive;

    // Rock-on-ice: removes slipperiness from ice
    private boolean rockOnIceActive;

    // Cloud-on-ice: ice becomes bouncy (higher restitution for jump boost)
    private boolean cloudOnIceActive;

    // Tuning constants (keep these simple & consistent)
    private static final float ROCK_DENSITY = 5.0f;
    private static final float ROCK_FRICTION = 0.8f;

    private static final float CLOUD_BASE_DENSITY = 2.0f; // NOT zero for DynamicBody
    private static final float CLOUD_BASE_FRICTION = 0.0f;

    private static final float CLOUD_LIFT_GRAVITY = -0.5f; // negative gravityScale to lift up smoothly

    private static final float ICE_DENSITY = 3.0f;
    private static final float ICE_FRICTION = 0.0f;
    private static final float ICE_RESTITUTION = 0.3f;
    private static final float ICE_SLIDE_DENSITY = 0.3f;
    private static final float ICE_BOUNCE_RESTITUTION = 2.5f;

    private static final float ICE_DAMPING = 1.0f;
    private static final float NORMAL_DAMPING = 10.0f;

    private boolean playerOnIce;

    // Range Variables
    private float STICK_PICTURE_DISTANCE = 5.0f; //I know you wanted it to be 3 times less than take picture but, if you mistakenly take a picture of the rock, you would not be able to reach the cloud unless it is 2 times less
    private float TAKE_PICTURE_DISTANCE = 10.0f;
    private Array<GameObject> highlighted = new Array<>();
    private final Affine2 highlightTransform = new Affine2();
    private boolean showRange = false;

    /** Mark set to handle more sophisticated collision callbacks */
    protected ObjectSet<Fixture> sensorFixtures;

    /**
     * Creates and initialize a new instance of the platformer game
     *
     * The game has default gravity and other settings
     */
    public SandboxScene(AssetDirectory directory) {
        super(directory,"platform");
        world.setContactListener(this);
        sensorFixtures = new ObjectSet<Fixture>();

        // Pull out sounds
        jumpSound = directory.getEntry( "platform-jump", SoundEffect.class );
        fireSound = directory.getEntry( "platform-pew", SoundEffect.class );
        plopSound = directory.getEntry( "platform-plop", SoundEffect.class );
        volume = constants.getFloat("volume", 1.0f);
    }

    /**
     * Resets the status of the game so that we can play again.
     *
     * This method disposes of the world and creates a new one.
     */
    public void reset() {
        JsonValue values = constants.get("world");
        Vector2 gravity = new Vector2(0, values.getFloat( "gravity" ));

        if (world != null) {
            for (ObstacleSprite sprite : sprites) {
                sprite.getObstacle().deactivatePhysics( world );
            }
        }
        sprites.clear();
        addQueue.clear();

        if (world != null) {
            Array<Body> bodies = new Array<>();
            world.getBodies(bodies);
            for(Body b : bodies) {
                world.destroyBody( b );
            }
        }

        if (world == null) {
            world = new World( gravity, false );
            world.setContactListener( this );
        }

        setComplete(false);
        setFailure(false);

        activePicture = null;
        if (pictures != null) {
            pictures.clear();
        }

        rockLiftActive = false;
        cloudDropActive = false;
        cloudReturnActive = false;
        iceOnCloudActive = false;
        iceOnRockActive = false;
        rockOnIceActive = false;
        cloudOnIceActive = false;
        playerOnIce = false;

        populateLevel();
    }

    /**
     * Lays out the game geography.
     */
    private void populateLevel() {
        float units = height/bounds.height;

        // Add level goal
        Texture texture = directory.getEntry( "shared-goal", Texture.class );
        JsonValue goal = constants.get("sandboxGoal");
        goalDoor = new Door(units, goal);
        goalDoor.setTexture( texture );
        goalDoor.getObstacle().setName("goal");
        addSprite(goalDoor);

        // Grey-blue ice texture for icy ground and ice block
        Pixmap icePixmap = new Pixmap(128, 128, Pixmap.Format.RGBA8888);
        icePixmap.setColor(0.7f, 0.78f, 0.85f, 1.0f);
        icePixmap.fill();
        Texture iceGroundTexture = new Texture(icePixmap);
        icePixmap.dispose();

        texture = directory.getEntry( "shared-earth", Texture.class );
        Texture earthTexture = texture;

        Surface wall;
        String wname = "wall";
        JsonValue walls = constants.get("walls");
        JsonValue walljv = walls.get("positions");
        for (int ii = 0; ii < walljv.size; ii++) {
            wall = new Surface(walljv.get(ii).asFloatArray(), units, walls);
            wall.getObstacle().setName(wname+ii);
            wall.setTexture( texture );
            addSprite(wall);
        }

        Surface platform;
        String pname = "sandboxPlatforms";
        JsonValue plats = constants.get("sandboxPlatforms");
        JsonValue platjv = plats.get("positions");
        for (int ii = 0; ii < platjv.size; ii++) {
            platform = new Surface(platjv.get(ii).asFloatArray(), units, plats);
            platform.getObstacle().setName(pname+ii);
            platform.getObstacle().setFriction(0.0f);
            platform.setTexture( iceGroundTexture );
            addSprite(platform);
        }

        // Create Traci
        texture = directory.getEntry( "platform-traci", Texture.class );
        avatar = new Zuko(units, constants.get("traci"));
        avatar.setTexture(texture);
        addSprite(avatar);
        avatar.createSensor();

        float rockSize = 1.5f;
        float cloudSize = 1.5f;

        rock = new GameObject(
                Obj.ROCK, constants.get("rock"), units,
                5.0f, 4.0f + rockSize / 2.0f,
                rockSize, rockSize,
                BodyDef.BodyType.DynamicBody,
                false
        );
        rock.getObstacle().setDensity(ROCK_DENSITY);
        rock.getObstacle().setFriction(ROCK_FRICTION);
        rock.getObstacle().setRestitution(0.0f);
        rock.setTexture(earthTexture);
        addSprite(rock);

        Texture cloudTexture = directory.getEntry( "cloud", Texture.class );
        cloud = new GameObject(
                Obj.CLOUD, constants.get("cloud"), units,
                8.0f, 7.5f,
                cloudSize, cloudSize,
                BodyDef.BodyType.DynamicBody, false
        );
        cloud.getObstacle().setDensity(CLOUD_BASE_DENSITY);
        cloud.getObstacle().setFriction(CLOUD_BASE_FRICTION);
        cloud.getObstacle().setRestitution(0.0f);
        cloud.getObstacle().setGravityScale(0.0f);
        cloud.setTexture(cloudTexture);
        addSprite(cloud);

        float iceSize = 1.5f;
        ice = new GameObject(
                Obj.ICE, constants.get("ice"), units,
                12.5f, 4.0f + iceSize / 2.0f,
                iceSize, iceSize,
                BodyDef.BodyType.StaticBody,
                false
        );
        ice.getObstacle().setDensity(ICE_DENSITY);
        ice.getObstacle().setFriction(ICE_FRICTION);
        ice.getObstacle().setRestitution(ICE_RESTITUTION);
        ice.setTexture(iceGroundTexture);
        addSprite(ice);

        rockLiftCeilingY = bounds.height - rockSize / 2.0f;

        // Cloud returns to its initial Y (lift target)
        cloudLiftCeilingY = cloud.getObstacle().getY();
        cloudDropActive = false;
        cloudReturnActive = false;
    }

    public boolean preUpdate(float dt) {
        if (!super.preUpdate(dt)) {
            return false;
        }

        if (!isFailure() && avatar.getObstacle().getY() < -1) {
            setFailure(true);
            return false;
        }
        return true;
    }

    public void update(float dt) {
        InputController input = InputController.getInstance();
        findObjectNearZuko();
        if (input.didDropPhoto()) {
            activePicture = null;
            pictures.clear();
        }

        if (input.didToggleRange()) {
            showRange = !showRange;
        }

        // Process actions in object model
        avatar.setMovement(input.getHorizontal() * avatar.getForce());
        avatar.setJumping(input.didPrimary());
        avatar.setShooting(input.didSecondary());

        Vector2 mouse = input.getCrossHair();
        GameObject target = findObjectUnderMouse(mouse.x, mouse.y);
        avatar.setCurrentTarget(target);

        if (input.didLeftClick()) {
            SoundEffectManager sounds = SoundEffectManager.getInstance();
            float picVolume = Math.min(1.0f, volume * 1.75f);

            if (target != null) {
                if (activePicture == null) {
                    if (avatar.canTakePicture()) {
                        avatar.takePicture();
                        Picture picture = new Picture(target);
                        pictures.clear();
                        pictures.add(picture);
                        activePicture = picture;
                        sounds.play("plop", plopSound, picVolume);
                    }
                } else {
                    if (activePicture.getSubject() != null && activePicture.getSubject() != target  && avatar.hasLineOfSight(target.getObstacle().getX(), target.getObstacle().getY(), STICK_PICTURE_DISTANCE)) {
                        Obj src = activePicture.getSubjectType();
                        Obj dst = target.object;
                        if (src == Obj.CLOUD && dst == Obj.ROCK && rock != null && rock.getObstacle() != null && rock.getObstacle().getBody() != null) {
                            rockLiftActive = !rockLiftActive;
                            sounds.play("fire", fireSound, volume);

                            if (rockLiftActive) {
                                rock.putPicture(activePicture.getSubject());
                                float units = height/bounds.height;
                                activePicture.setTarget(rock, units);
                                addSprite(activePicture);
                                WeldJointDef weldDef = new WeldJointDef();
                                weldDef.initialize(rock.getObstacle().getBody(), activePicture.getObstacle().getBody(), rock.getObstacle().getPosition());
                                activePicture.setJoint(world.createJoint(weldDef));
                            } else {
                                if (activePicture != null && activePicture.getJoint() != null) {
                                    world.destroyJoint(activePicture.getJoint());
                                }
                                sprites.remove(activePicture);
                                activePicture = null;
                                rock.resetAttributes();
                                rock.getObstacle().getBody().setGravityScale(1.0f);
                            }
                        }

                        if (src == Obj.ROCK && dst == Obj.CLOUD && cloud != null && cloud.getObstacle() != null && cloud.getObstacle().getBody() != null) {
                            cloudDropActive = !cloudDropActive;
                            sounds.play("fire", fireSound, volume);

                            if (cloudDropActive) {
                                cloud.putPicture(activePicture.getSubject());
                                cloud.getObstacle().setDensity(ROCK_DENSITY);
                                cloud.getObstacle().setFriction(ROCK_FRICTION);
                                cloud.getObstacle().getBody().resetMassData();
                                float units = height/bounds.height;
                                activePicture.setTarget(cloud, units);
                                addSprite(activePicture);
                                WeldJointDef weldDef = new WeldJointDef();
                                weldDef.initialize(cloud.getObstacle().getBody(), activePicture.getObstacle().getBody(), cloud.getObstacle().getPosition());
                                activePicture.setJoint(world.createJoint(weldDef));
                                cloudReturnActive = false;
                            } else {
                                if (activePicture != null && activePicture.getJoint() != null) {
                                    world.destroyJoint(activePicture.getJoint());
                                }
                                sprites.remove(activePicture);
                                activePicture = null;
                                cloud.resetAttributes();
                                cloud.getObstacle().setDensity(CLOUD_BASE_DENSITY);
                                cloud.getObstacle().setFriction(CLOUD_BASE_FRICTION);
                                cloud.getObstacle().getBody().resetMassData();
                                cloudReturnActive = true;
                            }
                        }

                        if (src == Obj.ICE && dst == Obj.CLOUD && cloud != null && cloud.getObstacle() != null && cloud.getObstacle().getBody() != null) {
                            iceOnCloudActive = !iceOnCloudActive;
                            sounds.play("fire", fireSound, volume);

                            if (iceOnCloudActive) {
                                cloud.putPicture(activePicture.getSubject());
                                Body cb = cloud.getObstacle().getBody();
                                cb.setLinearVelocity(0, 0);
                                cb.setAngularVelocity(0);
                                cb.setGravityScale(0.0f);
                                cb.setType(BodyDef.BodyType.StaticBody);
                                float units = height/bounds.height;
                                activePicture.setTarget(cloud, units);
                                addSprite(activePicture);
                                WeldJointDef weldDef = new WeldJointDef();
                                weldDef.initialize(cloud.getObstacle().getBody(), activePicture.getObstacle().getBody(), cloud.getObstacle().getPosition());
                                activePicture.setJoint(world.createJoint(weldDef));
                            } else {
                                if (activePicture != null && activePicture.getJoint() != null) {
                                    world.destroyJoint(activePicture.getJoint());
                                }
                                sprites.remove(activePicture);
                                activePicture = null;
                                cloud.resetAttributes();
                                Body cb2 = cloud.getObstacle().getBody();
                                cb2.setType(BodyDef.BodyType.DynamicBody);
                                cb2.setGravityScale(0.0f);
                                cb2.setLinearVelocity(0, 0);
                                cloud.getObstacle().setDensity(CLOUD_BASE_DENSITY);
                                cloud.getObstacle().setFriction(CLOUD_BASE_FRICTION);
                                cb2.resetMassData();
                            }
                        }

                        if (src == Obj.ICE && dst == Obj.ROCK && rock != null && rock.getObstacle() != null && rock.getObstacle().getBody() != null) {
                            iceOnRockActive = !iceOnRockActive;
                            sounds.play("fire", fireSound, volume);

                            if (iceOnRockActive) {
                                rock.putPicture(activePicture.getSubject());
                                rock.getObstacle().setFriction(0.0f);
                                rock.getObstacle().setDensity(ICE_SLIDE_DENSITY);
                                rock.getObstacle().getBody().resetMassData();
                                float units = height/bounds.height;
                                activePicture.setTarget(rock, units);
                                addSprite(activePicture);
                                WeldJointDef weldDef = new WeldJointDef();
                                weldDef.initialize(rock.getObstacle().getBody(), activePicture.getObstacle().getBody(), rock.getObstacle().getPosition());
                                activePicture.setJoint(world.createJoint(weldDef));
                            } else {
                                if (activePicture != null && activePicture.getJoint() != null) {
                                    world.destroyJoint(activePicture.getJoint());
                                }
                                sprites.remove(activePicture);
                                activePicture = null;
                                rock.resetAttributes();
                                rock.getObstacle().setFriction(ROCK_FRICTION);
                                rock.getObstacle().setDensity(ROCK_DENSITY);
                                rock.getObstacle().getBody().resetMassData();
                            }
                        }

                        if (src == Obj.ROCK && dst == Obj.ICE && ice != null && ice.getObstacle() != null && ice.getObstacle().getBody() != null) {
                            rockOnIceActive = !rockOnIceActive;
                            sounds.play("fire", fireSound, volume);

                            if (rockOnIceActive) {
                                ice.putPicture(activePicture.getSubject());
                                ice.getObstacle().setFriction(ROCK_FRICTION);
                                float units = height/bounds.height;
                                activePicture.setTarget(ice, units);
                                addSprite(activePicture);
                                WeldJointDef weldDef = new WeldJointDef();
                                weldDef.initialize(ice.getObstacle().getBody(), activePicture.getObstacle().getBody(), ice.getObstacle().getPosition());
                                activePicture.setJoint(world.createJoint(weldDef));
                            } else {
                                if (activePicture != null && activePicture.getJoint() != null) {
                                    world.destroyJoint(activePicture.getJoint());
                                }
                                sprites.remove(activePicture);
                                activePicture = null;
                                ice.resetAttributes();
                                ice.getObstacle().setFriction(ICE_FRICTION);
                            }
                        }

                        if (src == Obj.CLOUD && dst == Obj.ICE && ice != null && ice.getObstacle() != null && ice.getObstacle().getBody() != null) {
                            cloudOnIceActive = !cloudOnIceActive;
                            sounds.play("fire", fireSound, volume);

                            if (cloudOnIceActive) {
                                ice.putPicture(activePicture.getSubject());
                                ice.getObstacle().setRestitution(ICE_BOUNCE_RESTITUTION);
                                float units = height/bounds.height;
                                activePicture.setTarget(ice, units);
                                addSprite(activePicture);
                                WeldJointDef weldDef = new WeldJointDef();
                                weldDef.initialize(ice.getObstacle().getBody(), activePicture.getObstacle().getBody(), ice.getObstacle().getPosition());
                                activePicture.setJoint(world.createJoint(weldDef));
                            } else {
                                if (activePicture != null && activePicture.getJoint() != null) {
                                    world.destroyJoint(activePicture.getJoint());
                                }
                                sprites.remove(activePicture);
                                activePicture = null;
                                ice.resetAttributes();
                                ice.getObstacle().setRestitution(ICE_RESTITUTION);
                            }
                        }
                    }
                }
            }
        }

        if (avatar.isPictureTaken()) {
            avatar.clearPictureTaken();
        }

        if (rockLiftActive && rock != null && rock.getObstacle() != null && rock.getObstacle().getBody() != null) {
            Body body = rock.getObstacle().getBody();
            if (body.getPosition().y < rockLiftCeilingY) {
                body.setGravityScale(-0.35f);
            } else {
                body.setGravityScale(0.0f);
                Vector2 v = body.getLinearVelocity();
                body.setLinearVelocity(v.x, 0.0f);
            }
        }

        if (cloud != null && cloud.getObstacle() != null && cloud.getObstacle().getBody() != null) {
            Body body = cloud.getObstacle().getBody();

            if (iceOnCloudActive) {
                // frozen in place -- nothing to do
            } else if (cloudDropActive) {
                body.setGravityScale(1.0f);
            } else if (cloudReturnActive) {
                if (body.getPosition().y < cloudLiftCeilingY) {
                    body.setGravityScale(CLOUD_LIFT_GRAVITY);
                } else {
                    body.setGravityScale(0.0f);
                    Vector2 v = body.getLinearVelocity();
                    body.setLinearVelocity(v.x, 0.0f);
                    cloudReturnActive = false;
                }
            } else {
                body.setGravityScale(0.0f);
            }
        }

        if (avatar.isGrounded()) {
            if (playerOnIce && rockOnIceActive) {
                avatar.setDamping(NORMAL_DAMPING);
            } else {
                avatar.setDamping(ICE_DAMPING);
            }
        }

        avatar.applyForce();
        if (avatar.isJumping()) {
            SoundEffectManager sounds = SoundEffectManager.getInstance();
            sounds.play("jump", jumpSound, volume);
        }
    }

    /**
     * Callback method for the start of a collision
     */
    public void beginContact(Contact contact) {
        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();

        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        Object fd1 = fix1.getUserData();
        Object fd2 = fix2.getUserData();

        try {
            ObstacleSprite bd1 = (ObstacleSprite)body1.getUserData();
            ObstacleSprite bd2 = (ObstacleSprite)body2.getUserData();

            // Test bullet collision with world
            if (bd1.getName().equals("bullet") && bd2 != avatar && !bd2.getName().equals( "goal" )) {
                removeBullet(bd1);
            }
            if (bd2.getName().equals("bullet") && bd1 != avatar && !bd1.getName().equals( "goal" )) {
                removeBullet(bd2);
            }

            // See if we have landed on the ground.
            if ((avatar.getSensorName().equals(fd2) && avatar != bd1) ||
                    (avatar.getSensorName().equals(fd1) && avatar != bd2)) {
                avatar.setGrounded(true);
                sensorFixtures.add(avatar == bd1 ? fix2 : fix1);

                ObstacleSprite other = (ObstacleSprite)(avatar == bd1 ? bd2 : bd1);
                if (other instanceof GameObject && ((GameObject)other).object == Obj.ICE) {
                    playerOnIce = true;
                }
            }

            // Check for win condition
            if ((bd1 == avatar && bd2.getName().equals( "goal" )) ||
                    (bd1.getName().equals("goal")  && bd2 == avatar)) {
                setComplete(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Callback method for when two objects cease to touch.
     */
    public void endContact(Contact contact) {
        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();

        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        Object fd1 = fix1.getUserData();
        Object fd2 = fix2.getUserData();

        Object bd1 = body1.getUserData();
        Object bd2 = body2.getUserData();

        if ((avatar.getSensorName().equals(fd2) && avatar != bd1) ||
                (avatar.getSensorName().equals(fd1) && avatar != bd2)) {
            sensorFixtures.remove(avatar == bd1 ? fix2 : fix1);
            if (sensorFixtures.size == 0) {
                avatar.setGrounded(false);
            }

            Object other = (avatar == bd1) ? bd2 : bd1;
            if (other instanceof GameObject && ((GameObject)other).object == Obj.ICE) {
                playerOnIce = false;
            }
        }
    }

    /** Unused ContactListener method */
    public void postSolve(Contact contact, ContactImpulse impulse) {}
    /** Unused ContactListener method */
    public void preSolve(Contact contact, Manifold oldManifold) {}

    public void pause() {
        SoundEffectManager sounds = SoundEffectManager.getInstance();
        sounds.stop("plop");
        sounds.stop("fire");
        sounds.stop("jump");
    }

    /**
     * Removes a bullet from the world.
     */
    public void removeBullet(ObstacleSprite bullet) {
        bullet.getObstacle().markRemoved(true);
        SoundEffectManager sounds = SoundEffectManager.getInstance();
        sounds.play("plop", plopSound, volume);
    }

    /**
     * Returns the GameObject under the mouse position, or null if there is none
     */
    private GameObject findObjectUnderMouse(float mouseX, float mouseY) {
        for (ObstacleSprite sprite : sprites ) {
            if (sprite == avatar) continue;
            if (!(sprite instanceof GameObject)) continue;

            GameObject go = (GameObject) sprite;
            Obstacle obj = go.getObstacle();
            float u = obj.getPhysicsUnits();

            Rectangle bounds = sprite.getMesh().computeBounds();
            float centerX = obj.getX() * u;
            float centerY = obj.getY() * u;

            float minX = centerX + bounds.x;
            float minY = centerY + bounds.y;
            float maxX = minX + bounds.width;
            float maxY = minY + bounds.height;

            float posX = mouseX * u;
            float posY = mouseY * u;

            if (posX >= minX && posX <= maxX && posY >= minY && posY <= maxY) {
                return go;
            }
        }
        return null;
    }

    private void findObjectNearZuko() {
        highlighted.clear();
        float range = (activePicture != null) ? STICK_PICTURE_DISTANCE : TAKE_PICTURE_DISTANCE;
        for (ObstacleSprite sprite : sprites) {
            if (sprite == avatar) continue;
            if (!(sprite instanceof GameObject go)) continue;

            float x = go.getObstacle().getX();
            float y = go.getObstacle().getY();
            if (avatar.hasLineOfSight(x, y, range)) {
                highlighted.add(go);
            }

        }
    }
    @Override
    public void draw(float dt) {
        super.draw(dt);

        batch.begin(camera);
        Color highlighter = (activePicture != null) ? Color.LIME : Color.CORAL;
        batch.setColor(highlighter);

        for (GameObject go : highlighted) {
            Obstacle obj = go.getObstacle();
            float u = obj.getPhysicsUnits();
            float a = obj.getAngle();
            Vector2 p = obj.getPosition();

            highlightTransform.idt();
            highlightTransform.preRotate((float)(a * 180.0f/ Math.PI));
            highlightTransform.preTranslate(p.x * u, p.y * u);

            batch.outline(obj.getOutline(), highlightTransform);
        }
        if (showRange) {
            Obstacle obj = avatar.getObstacle();
            Vector2 p = obj.getPosition();
            float u = obj.getPhysicsUnits();
            float cx = p.x * u;
            float cy = p.y * u;
            float dashSize = 20f;
            float gapSize = 10f;
            float total = dashSize + gapSize;
            PathFactory factory = new PathFactory();

            highlightTransform.idt();
            batch.setColor(Color.LIME);
            for (float angle = 0; angle < 360; angle += total) {
                Path2 stickArc = factory.makeArc(cx,cy, (STICK_PICTURE_DISTANCE * u * 2) - 1, angle, dashSize, false);
                batch.outline(stickArc, highlightTransform);
                Path2 stickArc2 = factory.makeArc(cx,cy, (STICK_PICTURE_DISTANCE * u * 2) , angle, dashSize, false);
                batch.outline(stickArc2, highlightTransform);
                Path2 stickArc3 = factory.makeArc(cx,cy, (STICK_PICTURE_DISTANCE * u * 2) -2, angle, dashSize, false);
                batch.outline(stickArc3, highlightTransform);
            }
            batch.setColor(Color.CORAL);
            for (float angle = 0; angle < 360; angle += total) {
                Path2 takeArc = factory.makeArc(cx,cy, (TAKE_PICTURE_DISTANCE * u * 2) - 1, angle, dashSize, false);
                batch.outline(takeArc, highlightTransform);
                Path2 takeArc2 = factory.makeArc(cx,cy, (TAKE_PICTURE_DISTANCE * u  * 2), angle, dashSize, false);
                batch.outline(takeArc2, highlightTransform);
                Path2 takeArc3 = factory.makeArc(cx,cy, (TAKE_PICTURE_DISTANCE * u * 2) - 2, angle, dashSize, false);
                batch.outline(takeArc3, highlightTransform);

            }
        }

        batch.end();
    }
}