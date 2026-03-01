package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.physics2.ObstacleSprite;

public class GameObject extends ObstacleSprite {
    /** The initializing values for this object */
    private final JsonValue data;
    /** This object's enum type */
    Obj object;
    /** This object's weight (heavy/light) */
    float weight;
    /** This object's texture (rough/slippery) */
    float texture;
    /** This object's temperature (hot/cold) */
    float temp;

    public GameObject(Obj object, JsonValue data) {
        this.data = data;
        this.object = object;
        weight = data.getFloat("weight");
        texture = data.getFloat("texture");
        temp = data.getFloat("temp");
    }

    public void putPicture(GameObject other) {
        this.weight = other.getWeight();
        this.texture = other.getTexture();
        this.temp = other.getTemp();
    }

    public float getWeight() {
        return weight;
    }

    public float getTemp() {
        return temp;
    }

    public float getTexture() {
        return texture;
    }

    public void resetAttributes() {
        this.weight = data.getFloat("weight");
        this.texture = data.getFloat("texture");
        this.temp = data.getFloat("temp");
    }
    // TODO: add anything else that you're thinking of and write an explanation in discord/text
}
