package edu.cornell.cis3152.physics.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;

import static edu.cornell.cis3152.physics.world.Quality.*;

/**
 * A photographable physics object in the level: honey, ice, or cloud.
 *
 * <p>Extends {@link ObstacleSprite} with photo-related state ({@link #hasPicture},
 * {@link #pictureQuality}) and an {@link ObjectEffect} strategy for how being
 * photographed or receiving a picture changes behavior.</p>
 *
 * <p>Mutable gameplay fields (mass, friction, gravity, etc.) are staged on this object
 * and flushed to Box2D in {@link #syncPhysics()}, which must run from the scene's
 * {@code postUpdate()} after {@code world.step()}—never during the step.</p>
 *
 * <p>The initializing {@link JsonValue} is retained so {@link #restoreOriginalProperties()}
 * can reset all properties to level defaults after a picture is removed.</p>
 */
public class GameObject extends ObstacleSprite {
    /** The initializing values for this object */
    private final JsonValue data;
    /** The original body type so transient effects can restore it */
    private BodyDef.BodyType baseBodyType;
    /** This object's enum type */
    public Obj object;
    /** This object's weight (heavy/light) */
    float weight;

    private BoxObstacle body;
    /** Rest position for spring-based float motion */
    private final Vector2 floatHome = new Vector2();

    private boolean hasPicture = false;

    float gravityScale;
    /** This object's elasticity (rigid/bouncy) */
    float elasticity;
    /** This object's friction (rough/slippery)*/
    float friction;
    /** This object's temperature (hot/cold) */
    float temp;

    Quality pictureQuality = null;

    Quality quality;

    /** Strategy for this object type's photo effect */
    private final ObjectEffect effect;

    /**
     * When true, staged field changes need to be flushed to the Box2D body.
     * Set by ObjectEffect implementations; consumed by {@link #syncPhysics()}.
     */
    boolean pendingPhysicsSync = false;
    boolean pendingHorizontalStop = false;

    /** This object's texture */
    private Texture texture;

    /**
     * Initializes object type, intrinsic {@link Quality}, the matching {@link ObjectEffect}
     * strategy, and reads physics constants (weight, elasticity, friction, temperature,
     * gravity scale) from JSON. Does not create a Box2D body; use the full constructor
     * for that.
     *
     * @param object level object kind (honey, ice, cloud, …)
     * @param data   JSON defaults and constants for this object
     */
    public GameObject(Obj object, JsonValue data) {
        this.data = data;
        this.object = object;
        switch(object) {
            case CLOUD:
                this.quality = FLOAT;
                this.effect = new CloudEffect();
                break;
            case ICE:
                this.quality = SLIPPERY;
                this.effect = new IceEffect();
                break;
            case HONEY:
                this.quality = STICKY;
                this.effect = new HoneyEffect();
                break;
            default:
                this.quality = null;
                this.effect = null;
                break;
        }
        weight = data.getFloat("weight");
        elasticity = data.getFloat("elasticity");
        friction = data.getFloat("friction");
        temp = data.getFloat("temp");
        gravityScale = data.getFloat("gravityScale");
    }

    /**
     * Full construction: delegates to {@link #GameObject(Obj, JsonValue)}, then creates the
     * {@link BoxObstacle}, attaches it as this sprite's obstacle, configures body/sensor
     * flags from parameters, and sizes the draw mesh to match physics units.
     *
     * @param object   level object kind
     * @param data     JSON defaults and constants
     * @param units    world pixels per physics unit
     * @param x,y      body center position in physics space
     * @param w,h      half-extents (or equivalent) for the box obstacle
     * @param bodyType static/kinematic/dynamic Box2D type
     * @param sensor   whether the fixture is a sensor
     */
    public GameObject(Obj object, JsonValue data, float units, float x, float y, float w,
                      float h, BodyDef.BodyType bodyType, boolean sensor) {
        this(object, data);
        this.baseBodyType = bodyType;
        BoxObstacle body = new BoxObstacle(x, y, w, h);
        this.body = body;
        body.setBodyType(bodyType);
        body.setPhysicsUnits(units);
        body.setUserData(this);
        body.setFriction(friction);
        body.setGravityScale(gravityScale);
        body.setFixedRotation(shouldLockRotation());
        body.setName(object.name().toLowerCase());
        body.setSensor(sensor);
        obstacle = body;
        floatHome.set(x, y);

        float drawW = w * units;
        float drawH = h * units;
        mesh.set(-drawW / 2.0f, -drawH / 2.0f, drawW, drawH);
    }

    public float getGravityScale() {
        return gravityScale;
    }

    public Quality getQuality() { return quality; }

    public Obj getObjectType() { return object; }

    public float getOriginalWeight() { return data.getFloat("weight"); }

    public float getOriginalElasticity() { return data.getFloat("elasticity"); }

    public float getOriginalFriction() { return data.getFloat("friction"); }

    public float getOriginalGravityScale() { return data.getFloat("gravityScale"); }

    public boolean hasLiftPicture() {
        return hasPicture && gravityScale <= 0.0f;
    }

    public Vector2 getFloatHome() {
        return floatHome;
    }

    public void setFloatHome(float x, float y) {
        floatHome.set(x, y);
    }

    public boolean hasPicture() { return hasPicture; }

    public Quality getPictureQuality() { return pictureQuality; }

    /**
     * Surface quality used for Zuko's movement (e.g. jump reduction on sticky surfaces).
     * If a non-float picture quality is applied, that overrides the base type quality;
     * otherwise the intrinsic {@link #quality} applies.
     */
    public Quality getEffectiveSurfaceQuality() {
        // this is for the jump reduction for texture qualities only
        if (hasPicture && pictureQuality != null && pictureQuality != FLOAT) {
            return pictureQuality;
        }
        return quality;
    }

    /**
     * Marks this object as bearing a picture and applies the {@code source}'s
     * {@link ObjectEffect} onto this instance (mutating staged fields; Box2D is updated
     * later via {@link #syncPhysics()}). No-op if a picture is already applied.
     *
     * @param source the photographed object whose effect should transfer here
     */
    public void putPicture(GameObject source) {
        if (hasPicture) {
            return;
        }
        hasPicture = true;
        ObjectEffect sourceEffect = source.getEffect();
        if (sourceEffect != null) {
            System.out.println("in here: putting " + source.getName() + " on " + this.getName());
            sourceEffect.apply(source, this);
            System.out.println("friction: " + this.getFriction());
        }
    }

    public ObjectEffect getEffect() {
        return effect;
    }

    /**
     * Pushes staged mass, friction, restitution, gravity, body type, and rotation
     * constraints from this object onto the live {@link BoxObstacle} / {@link Body}.
     * Must only be called from the scene's {@code postUpdate()}, never during
     * {@code world.step()}.
     */
    public void syncPhysics() {
        if (!pendingPhysicsSync) {
            return;
        }
        pendingPhysicsSync = false;

        this.body.setBodyType(baseBodyType);
        this.body.setMass(weight);
        this.body.setGravityScale(gravityScale);
        this.body.setRestitution(elasticity);
        this.body.setFriction(friction);
        applyRotationConstraint();
        this.body.setAngularVelocity(0.0f);

        Body physicsBody = this.body.getBody();
        if (physicsBody != null) {
            physicsBody.setType(baseBodyType);
            physicsBody.setGravityScale(gravityScale);
            physicsBody.setFixedRotation(shouldLockRotation());
            for (Fixture fixture : physicsBody.getFixtureList()) {
                fixture.setFriction(friction);
                fixture.setRestitution(elasticity);
            }
            if (pendingHorizontalStop) {
                physicsBody.setLinearVelocity(0.0f, physicsBody.getLinearVelocity().y);
                pendingHorizontalStop = false;
            }
            physicsBody.setAwake(true);
        }
    }

    /**
     * Resets picture flag, picture quality, and all physics fields to values read from
     * the stored JSON, then sets {@link #pendingPhysicsSync}. Invoked when a picture is
     * removed (e.g. from {@link ObjectEffect} teardown) so the body returns to level defaults.
     */
    void restoreOriginalProperties() {
        hasPicture = false;
        pictureQuality = null;
        this.weight = data.getFloat("weight");
        this.elasticity = data.getFloat("elasticity");
        this.friction = data.getFloat("friction");
        this.gravityScale = data.getFloat("gravityScale");
        this.temp = data.getFloat("temp");
        pendingPhysicsSync = true;
        pendingHorizontalStop = false;
    }

    public void setTexture(Texture texture) {
        this.texture = texture;
        super.setTexture(texture);
    }

    public Texture getTexture() {
        return texture;
    }

    public float getWeight() {
        return weight;
    }

    public float getTemp() {
        return temp;
    }

    public float getElasticity() {
        return elasticity;
    }

    public float getFriction() {
        return friction;
    }

    /**
     * Public entry point for {@link #restoreOriginalProperties()}; use when gameplay
     * needs to clear applied-picture state. Box2D catches up in {@link #syncPhysics()}.
     */
    public void resetAttributes() {
        restoreOriginalProperties();
    }

    /**
     * Floating objects ({@code gravityScale <= 0}) keep fixed rotation so they do not
     * spin from contacts or impulses.
     */
    private boolean shouldLockRotation() {
        return gravityScale <= 0.0f;
    }

    private void applyRotationConstraint() {
        this.body.setFixedRotation(shouldLockRotation());
    }
}
