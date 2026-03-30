package edu.cornell.cis3152.physics.world;

import com.badlogic.gdx.graphics.Texture;
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

    private boolean hasPicture = false;
    private boolean frozenByIcePicture = false;

    float gravityScale;
    /** This object's elasticity (rigid/bouncy) */
    float elasticity;
    /** This object's friction (rough/slippery)*/
    float friction;
    /** This object's temperature (hot/cold) */
    float temp;

    Quality quality;

    /** This object's texture */
    private Texture texture;

    public GameObject(Obj object, JsonValue data) {
        this.data = data;
        this.object = object;
        switch(object) {
            case CLOUD:
                this.quality = FLOAT;
                break;
            case ICE:
                this.quality = SLIPPERY;
                break;
            case HONEY:
                this.quality = STICKY;
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
        body.setFixedRotation(true);
        body.setName(object.name().toLowerCase());
        body.setSensor(sensor);
        obstacle = body;

        float drawW = w * units;
        float drawH = h * units;
        mesh.set(-drawW / 2.0f, -drawH / 2.0f, drawW, drawH);
    }

    public float getGravityScale() {
        return gravityScale;
    }

    public Quality getQuality() { return quality; }

    public float getOriginalTemp() { return data.getFloat("temp"); }

    public float getOriginalWeight() { return data.getFloat("weight"); }

    public float getOriginalElasticity() { return data.getFloat("elasticity"); }

    public float getOriginalFriction() { return data.getFloat("friction"); }

    public float getOriginalGravityScale() { return data.getFloat("gravityScale"); }

    public boolean hasLiftPicture() {
        return hasPicture && gravityScale <= 0.0f;
    }

    public void putPicture(GameObject other) {
        if (hasPicture) {
            return;
        }
        hasPicture = true;
        Quality otherQuality = other.getQuality();
        // determines quality to put on object and changes the parameters as expected
        switch (otherQuality) {
            case FLOAT:
                this.weight = other.getOriginalWeight();
                this.gravityScale = other.getOriginalGravityScale();
                this.body.setMass(weight);
                this.body.setGravityScale(gravityScale);
                this.body.setFixedRotation(true);
                this.body.setAngularVelocity(0.0f);
                break;
            case SLIPPERY:
                this.elasticity = other.getOriginalElasticity();
                this.friction = other.getOriginalFriction();
                this.body.setRestitution(elasticity);
                this.body.setFriction(friction);
                this.body.setFixedRotation(true);
                this.body.setAngularVelocity(0.0f);
                break;
            case STICKY:
                this.elasticity = other.getOriginalElasticity();
                this.friction = other.getOriginalFriction();
                this.body.setRestitution(elasticity);
                this.body.setFriction(friction);
                this.body.setFixedRotation(true);
                this.body.setAngularVelocity(0.0f);
                break;
        }


//        switch (cameraType) {
//            case THERMAL:
//                this.temp = other.getOriginalTemp();
//                if (other.object == Obj.ICE) {
//                    freezeInPlace();
//                } else if (this.object == Obj.ICE) {
//                    thawFromThermalPicture();
//                }
//                break;
//            case REGULAR:
//                this.weight = other.getOriginalWeight();
//                this.gravityScale = other.getOriginalGravityScale();
//                this.body.setMass(weight);
//                this.body.setGravityScale(gravityScale);
//                this.body.setFixedRotation(true);
//                this.body.setAngularVelocity(0.0f);
//                break;
//            case TEXTURE:
//                this.elasticity = other.getOriginalElasticity();
//                this.friction = other.getOriginalFriction();
//                this.body.setRestitution(elasticity);
//                this.body.setFriction(friction);
//                this.body.setFixedRotation(true);
//                this.body.setAngularVelocity(0.0f);
//                break;
//        }
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

    public void resetAttributes() {
        hasPicture = false;
        frozenByIcePicture = false;
        this.weight = data.getFloat("weight");
        this.elasticity = data.getFloat("elasticity");
        this.friction = data.getFloat("friction");
        this.gravityScale = data.getFloat("gravityScale");
        this.temp = data.getFloat("temp");

        this.body.setBodyType(baseBodyType);
        this.body.setMass(weight);
        this.body.setRestitution(elasticity);
        this.body.setFriction(friction);
        this.body.setGravityScale(gravityScale);
        this.body.setFixedRotation(true);
        this.body.setAngularVelocity(0.0f);

        Body physicsBody = this.body.getBody();
        if (physicsBody != null) {
            physicsBody.setType(baseBodyType);
            physicsBody.setLinearVelocity(0.0f, 0.0f);
            physicsBody.setAngularVelocity(0.0f);
            physicsBody.setGravityScale(gravityScale);
            physicsBody.setAwake(true);
        }
    }

    public boolean isFrozenByIcePicture() {
        return frozenByIcePicture;
    }

    private void freezeInPlace() {
        frozenByIcePicture = true;
        this.body.setBodyType(BodyDef.BodyType.StaticBody);
        this.body.setGravityScale(0.0f);
        this.body.setFixedRotation(true);
        this.body.setAngularVelocity(0.0f);

        Body physicsBody = this.body.getBody();
        if (physicsBody != null) {
            physicsBody.setLinearVelocity(0.0f, 0.0f);
            physicsBody.setAngularVelocity(0.0f);
            physicsBody.setGravityScale(0.0f);
            physicsBody.setType(BodyDef.BodyType.StaticBody);
            physicsBody.setAwake(true);
        }
    }

    private void thawFromThermalPicture() {
        frozenByIcePicture = false;
        this.body.setBodyType(BodyDef.BodyType.DynamicBody);
        this.body.setGravityScale(gravityScale);
        this.body.setFixedRotation(true);
        this.body.setAngularVelocity(0.0f);

        Body physicsBody = this.body.getBody();
        if (physicsBody != null) {
            physicsBody.setType(BodyDef.BodyType.DynamicBody);
            physicsBody.setGravityScale(gravityScale);
            physicsBody.setAwake(true);
        }
    }
    // TODO: add anything else that you're thinking of and write an explanation in discord/text
}
