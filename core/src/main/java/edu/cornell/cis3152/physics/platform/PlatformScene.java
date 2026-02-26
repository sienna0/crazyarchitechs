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

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.audio.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;

import edu.cornell.cis3152.physics.InputController;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.cis3152.physics.PhysicsScene;
import edu.cornell.gdiac.audio.SoundEffect;
import edu.cornell.gdiac.audio.SoundEffectManager;
import edu.cornell.gdiac.physics2.*;

/**
 * The game scene for the platformer game.
 *
 * Look at the method {@link #populateLevel} for how we initialize the scene.
 * Beyond that, a lot of work is done in the method for the ContactListener
 * interface. That is the method that is called upon collisions, giving us a
 * chance to define a response.
 */
public class PlatformScene extends PhysicsScene implements ContactListener {
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
    private Traci avatar;
    /** Reference to the goalDoor (for collision detection) */
    private Door goalDoor;

    /** Mark set to handle more sophisticated collision callbacks */
    protected ObjectSet<Fixture> sensorFixtures;

    /**
     * Creates and initialize a new instance of the platformer game
     *
     * The game has default gravity and other settings
     */
    public PlatformScene(AssetDirectory directory) {
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
                Obstacle obj = sprite.getObstacle();
                sprite.getObstacle().deactivatePhysics( world );
            }
        }
        sprites.clear();
        addQueue.clear();
        if (world != null) {
            Array<Body> bodies = new Array<Body>();
            world.getBodies(bodies);
            for(Body b : bodies) {
                world.destroyBody( b );
            }
            //world.dispose();
            //world = null;
        }

        if (world == null) {
            world = new World( gravity, false );
            world.setContactListener( this );
        }
        setComplete(false);
        setFailure(false);
        populateLevel();
    }

    /**
     * Lays out the game geography.
     */
    private void populateLevel() {
        // TODO: change this to Level 1 geography
        float units = height/bounds.height;

        // Add level goal
        Texture texture = directory.getEntry( "shared-goal", Texture.class );

        JsonValue goal = constants.get("goal");
        JsonValue goalpos = goal.get("pos");
        goalDoor = new Door(units, goal);
        goalDoor.setTexture( texture );
        goalDoor.getObstacle().setName("goal");
        addSprite(goalDoor);

        // Create ground pieces
        texture = directory.getEntry( "shared-earth", Texture.class );

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
        String pname = "platform";
        JsonValue plats = constants.get("platforms");
        JsonValue platjv = plats.get("positions");
        for (int ii = 0; ii < platjv.size; ii++) {
            platform = new Surface(platjv.get(ii).asFloatArray(), units, walls);
            platform.getObstacle().setName(pname+ii);
            platform.setTexture( texture );
            addSprite(platform);
        }

        // Create Traci
        texture = directory.getEntry( "platform-traci", Texture.class );
        avatar = new Traci(units, constants.get("traci"));
        avatar.setTexture(texture);
        addSprite(avatar);
        // Have to do after body is created
        avatar.createSensor();


        // Create rope bridge
        texture = directory.getEntry( "platform-rope", Texture.class );
        RopeBridge bridge = new RopeBridge(units, constants.get("bridge"));
        bridge.setTexture(texture);
        addSpriteGroup(bridge);

        // Create spinning platform
        texture = directory.getEntry( "platform-barrier", Texture.class );
        Spinner spinPlatform = new Spinner(units,constants.get("spinner"));
        spinPlatform.setTexture(texture);
        addSpriteGroup(spinPlatform);
    }

    /**
     * Returns whether to process the update loop
     *
     * At the start of the update loop, we check if it is time
     * to switch to a new game mode. If not, the update proceeds
     * normally.
     *
     * @param dt    Number of seconds since last animation frame
     *
     * @return whether to process the update loop
     */
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

    /**
     * Advances the core gameplay loop of this world.
     *
     * This method contains the specific update code for this mini-game. It
     * does not handle collisions, as those are managed by the parent class
     * PhysicsScene. This method is called after input is synced to the current
     * frame, but before collisions are resolved. The very last thing that it
     * should do is apply forces to the appropriate objects.
     *
     * @param dt    Number of seconds since last animation frame
     */
    public void update(float dt) {
        InputController input = InputController.getInstance();

        // Process actions in object model
        avatar.setMovement(input.getHorizontal() *avatar.getForce());
        avatar.setJumping(input.didPrimary());
        avatar.setShooting(input.didSecondary());

        // Add a bullet if we fire
        if (avatar.isShooting()) {
            createBullet();
        }

        avatar.applyForce();
        if (avatar.isJumping()) {
            SoundEffectManager sounds = SoundEffectManager.getInstance();
            sounds.play("jump", jumpSound, volume);
        }
    }

    /**
     * Adds a new bullet to the world and send it in the right direction.
     */
    private void createBullet() {
        float units = height/bounds.height;

        JsonValue bulletjv = constants.get("bullet");
        Obstacle traci = avatar.getObstacle();

        Texture texture = directory.getEntry("platform-bullet", Texture.class);
        Bullet bullet = new Bullet(units, bulletjv, traci.getPosition(),avatar.isFacingRight());
        bullet.setTexture(texture);
        addQueuedObject(bullet);

        SoundEffectManager sounds = SoundEffectManager.getInstance();
        sounds.play("fire", fireSound, volume);
    }

    /**
     * Removes a new bullet from the world.
     *
     * @param  bullet   the bullet to remove
     */
    public void removeBullet(ObstacleSprite bullet) {
        bullet.getObstacle().markRemoved(true);
        SoundEffectManager sounds = SoundEffectManager.getInstance();
        sounds.play("plop", plopSound, volume);
    }


    /**
     * Callback method for the start of a collision
     *
     * This method is called when we first get a collision between two objects.
     * We use this method to test if it is the "right" kind of collision. In
     * particular, we use it to test if we made it to the win door.
     *
     * @param contact The two bodies that collided
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
                sensorFixtures.add(avatar == bd1 ? fix2 : fix1); // Could have more than one ground
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
     * Callback method for the start of a collision
     *
     * This method is called when two objects cease to touch. The main use of
     * this method is to determine when the characer is NOT on the ground. This
     * is how we prevent double jumping.
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
        }
    }

    /** Unused ContactListener method */
    public void postSolve(Contact contact, ContactImpulse impulse) {}
    /** Unused ContactListener method */
    public void preSolve(Contact contact, Manifold oldManifold) {}

    /**
     * Called when the Screen is paused.
     *
     * We need this method to stop all sounds when we pause.
     * Pausing happens when we switch game modes.
     */
    public void pause() {
        SoundEffectManager sounds = SoundEffectManager.getInstance();
        sounds.stop("plop");
        sounds.stop("fire");
        sounds.stop("jump");
    }
}
