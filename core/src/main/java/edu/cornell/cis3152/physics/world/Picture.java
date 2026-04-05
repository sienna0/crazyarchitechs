package edu.cornell.cis3152.physics.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import edu.cornell.gdiac.graphics.SpriteMesh;
import edu.cornell.gdiac.physics2.ObstacleSprite;

/**
 * One slot in Zuko's photo inventory. A picture has a {@link #subject} (what was
 * photographed) and optionally a {@link #target} (the surface it is stuck to).
 *
 * <p>When {@link #setTarget(GameObject)} is used, the subject's {@link ObjectEffect}
 * is applied to the target via {@link GameObject#putPicture(GameObject)}.</p>
 *
 * <p>Extends {@link ObstacleSprite} only for mesh/texture storage for HUD rendering;
 * instances are not added to the physics world.</p>
 */
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
     * Empty inventory slot: no subject, identified by {@code id} (inventory index).
     *
     * @param id slot index / picture id
     */
    public Picture(int id){
        hasSubject = false;
        this.id = id;
    }

    /**
     * Snapshot of {@code subject}: copies its {@link SpriteMesh} at half scale and keeps
     * its texture for Polaroid-style display. {@link #id} is set to {@code -1} until placed
     * in the inventory.
     *
     * @param subject the object being photographed
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
     * Records {@code target} and applies this picture's {@link #subject} onto it through
     * {@link GameObject#putPicture(GameObject)} (transferring the subject's effect).
     *
     * @param target surface receiving the stuck picture
     */
    public void setTarget(GameObject target) {
        this.target = target;
        target.putPicture(subject);
    }

    /**
     * Clears {@link #target} only; does not call {@link GameObject#resetAttributes()} or
     * otherwise restore the former target—callers must handle physics/state cleanup.
     */
    public void clearTarget() {
        target = null;
    }


    /**
     * Drops the subject reference and type flags so this picture becomes a blank slot;
     * does not clear {@link #target} or undo effects already applied to a surface.
     */
    public void clearSubject() {
        this.subject = null;
        this.subjectType = null;
        hasSubject = false;
    }

    /**
     * Tint used when drawing this picture in the HUD or overlays (Polaroid accent).
     */
    public Color getColor() {
        Color lightGreen = new Color(0.47f,0.75f,0.33f,1);
        return lightGreen;
    }


}
