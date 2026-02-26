package edu.cornell.cis3152.physics.platform;

import edu.cornell.gdiac.physics2.ObstacleSprite;

public class GameObject extends ObstacleSprite {
    Obj object;
    float weight;
    float texture;
    float temp;
    public GameObject(Obj object) {
        this.object = object;
        // TODO: get the json values from assets/platform/constants.json (weight, texture, etc.)
    }

    public void putPicture(Obj object) {
        // TODO: change the weight, texture, etc of object
    }

    public float getWeight() {
        // TODO: return this.weight;
        return 0;
    }

    public float getTemp() {
        // TODO: return this.temp;
        return 0;
    }

    public float getTexture() {
        // TODO: return this.texture;
        return 0;
    }

    // TODO: add anything else that you're thinking of and write an explanation in discord/text
}
