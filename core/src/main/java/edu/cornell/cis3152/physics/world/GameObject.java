package edu.cornell.cis3152.physics.world;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;

public class GameObject extends ObstacleSprite {
    /** The initializing values for this object */
    private final JsonValue data;
    /** This object's enum type */
    public Obj object;
    /** This object's weight (heavy/light) */
    float weight;

    private BoxObstacle body;

    private boolean hasPicture = false;

    float gravityScale;
    /** This object's elasticity (rigid/bouncy) */
    float elasticity;
    /** This object's friction (rough/slippery)*/
    float friction;
    /** This object's temperature (hot/cold) */
    float temp;

    public GameObject(Obj object, JsonValue data) {
        this.data = data;
        this.object = object;
        weight = data.getFloat("weight");
        elasticity = data.getFloat("elasticity");
        friction = data.getFloat("friction");
        temp = data.getFloat("temp");
        gravityScale = data.getFloat("gravityScale");
    }

    public GameObject(Obj object, JsonValue data, float units, float x, float y, float w,
                      float h, BodyDef.BodyType bodyType, boolean sensor) {
        this(object, data);
        BoxObstacle body = new BoxObstacle(x, y, w, h);
        this.body = body;
        body.setBodyType(bodyType);
        body.setPhysicsUnits(units);
        body.setUserData(this);
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

    public boolean hasLiftPicture() {
        return hasPicture && gravityScale <= 0.0f;
    }

    public void putPicture(GameObject other, CameraType cameraType) {
        if (hasPicture) {
            return;
        }
        hasPicture = true;
        switch (cameraType) {
            case THERMAL:
                this.temp = other.getTemp();
                break;
            case REGULAR:
                this.weight = other.getWeight();
                this.gravityScale = other.getGravityScale();
                this.body.setMass(weight);
                this.body.setGravityScale(gravityScale);
                this.body.setFixedRotation(true);
                this.body.setAngularVelocity(0.0f);
                break;
            case TEXTURE:
                this.elasticity = other.getElasticity();
                this.friction = other.getFriction();
                this.body.setRestitution(elasticity);
                this.body.setFriction(friction);
                this.body.setFixedRotation(true);
                this.body.setAngularVelocity(0.0f);
                break;
        }

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
        this.weight = data.getFloat("weight");
        this.elasticity = data.getFloat("elasticity");
        this.friction = data.getFloat("friction");
        this.gravityScale = data.getFloat("gravityScale");
        this.temp = data.getFloat("temp");

        this.body.setMass(weight);
        this.body.setRestitution(elasticity);
        this.body.setFriction(friction);
        this.body.setGravityScale(gravityScale);
        this.body.setFixedRotation(true);
        this.body.setAngularVelocity(0.0f);
    }
    // TODO: add anything else that you're thinking of and write an explanation in discord/text
}
