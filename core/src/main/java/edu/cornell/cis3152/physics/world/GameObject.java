package edu.cornell.cis3152.physics.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;

import static edu.cornell.cis3152.physics.world.Quality.*;

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

    /** This object's texture */
    private Texture texture;

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
     * Returns the surface quality that should affect contact behavior.
     *
     */
    public Quality getEffectiveSurfaceQuality() {
        // this is for the jump reduction for texture qualities only
        if (hasPicture && pictureQuality != null && pictureQuality != FLOAT) {
            return pictureQuality;
        }
        return quality;
    }

    public void putPicture(GameObject source) {
        if (hasPicture) {
            return;
        }
        hasPicture = true;
        ObjectEffect sourceEffect = source.getEffect();
        if (sourceEffect != null) {
            sourceEffect.apply(source, this);
        }
    }

    public ObjectEffect getEffect() {
        return effect;
    }

    /**
     * Flushes staged property changes to the Box2D body.
     * Must be called from the physics scene's postUpdate(), never during world.step().
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
            physicsBody.setAwake(true);
        }
    }

    /**
     * Restores all properties to their JSON-defined originals and flags a physics sync.
     * Called by ObjectEffect.remove() implementations.
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
     * Resets all attributes to JSON defaults and flags a full physics sync.
     * The actual Box2D body update happens in {@link #syncPhysics()}.
     */
    public void resetAttributes() {
        restoreOriginalProperties();
    }

    private boolean shouldLockRotation() {
        return gravityScale <= 0.0f;
    }

    private void applyRotationConstraint() {
        this.body.setFixedRotation(shouldLockRotation());
    }
}
