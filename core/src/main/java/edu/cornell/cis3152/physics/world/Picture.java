package edu.cornell.cis3152.physics.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import edu.cornell.gdiac.graphics.SpriteMesh;
import edu.cornell.gdiac.physics2.ObstacleSprite;

public class Picture extends ObstacleSprite {
    /** The GameObject subject of this picture */
    GameObject subject;

    /** The GameObject the picture is stuck to */
    GameObject target;

    /** The Obj enum type of this picture's subject */
    Obj subjectType;

    /** Whether this picture has been given a subject */
    boolean hasSubject;

    /** The id of this picture. Also serves as its index position in the inventory. */
    private int id;


    /** Texture of this picture */
    private Texture texture;

    private Quality subjectQuality;

    /**
     * Constructor for a blank Picture instance with no subject yet
     */
    public Picture(int id){
        hasSubject = false;
        this.id = id;
    }

    /**
     * Constructor for a Picture instance when initialized with a subject.
     *
     * @param subject is the GameObject which is the subject of the picture
     */
    public Picture(GameObject subject) {
        this.subject = subject;
        this.subjectType = subject.object;
        this.subjectQuality = subject.getQuality();
        hasSubject = true;
        // steal the draw data at reduced size
        // Picture itself would need to be a GameObject for this
        mesh = new SpriteMesh(subject.getMesh());
        mesh.scl(0.5f);
        this.texture = subject.getTexture();
        id = -1;
    }

    /** Returns the ID of this picture */
    public int getId(){return id;}

    public void setId(int id){
        this.id = id;
    }


    /** Returns whether this picture has a subject */
    public boolean hasSubject(){return hasSubject;}


    /** Returns the enum type of this picture's subject */
    public Obj getSubjectType(){return subjectType;}


    /** Returns the texture of this picture */
    public Texture getTexture(){return texture;}

    /**
     * Sets a GameObject subject for this picture instance.
     *
     * @param go overrides/sets the pictures current object
     */
    public void setSubject(GameObject go) {
        subject = go;
        subjectType = go.object;
        hasSubject = true;
        mesh = new SpriteMesh(subject.getMesh());
        mesh.scl(0.5f);
        setTexture(subject.getSpriteSheet().getTexture());
    }

    /** Returns the subject of this picture */
    public GameObject getSubject(){return subject;}

    /** Returns the target of this picture */
    public GameObject getTarget(){return target;}

    /**
     * Sets the target the picture will be placed on. Must be called before adding the picture
     * to the World.
     *
     * @param target is the GameObject the picture is placed on
     * @param units are the physics units for the World
     */
    public void setTarget(GameObject target) {
        this.target = target;
        target.putPicture(subject);
    }

    /** Clears the object this picture is currently attached to. */
    public void clearTarget() {
        target = null;
    }


    public void clearSubject() {
        this.subject = null;
        this.subjectType = null;
        hasSubject = false;
    }

    public Color getColor() {
        Color lightGreen = new Color(0.47f,0.75f,0.33f,1);
        return lightGreen;
    }


}
