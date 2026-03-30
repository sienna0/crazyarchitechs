package edu.cornell.cis3152.physics.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.graphics.SpriteMesh;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;

import static edu.cornell.cis3152.physics.world.Obj.HONEY;

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


    /** Cache offset for pictures on objects */
    private final Vector2 offset = new Vector2();

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
    // FIXME remove the units call, unneeded
    //  and add the camera type as a parameter
    public void setTarget(GameObject target, float units) {
        this.target = target;
        BoxObstacle original = (BoxObstacle) target.getObstacle();
        BoxObstacle copy = new BoxObstacle(
                original.getX(), original.getY(),
                original.getWidth(), original.getHeight()
        );
        copy.setBodyType(original.getBodyType());
        copy.setPhysicsUnits(original.getPhysicsUnits());
        copy.setSensor(true);
        copy.setUserData(this);
        copy.setGravityScale(0.0f);
        obstacle = copy;


        target.putPicture(subject);

    }

    /** Clears the object this picture is currently attached to. */
    public void clearTarget() {
        target = null;
    }

    /**
     * Overrides the update method to add extra functionality. In particular, this allows the
     * picture to update its location, angle, and velocity in relation to its target without
     * affecting the target's physics at all.
     *
     * @param dt is the delta time for game loop
     */
    @Override
    public void update(float dt) {
        super.update(dt);

        float targetAngle = target.getObstacle().getBody().getAngle();

        obstacle.getBody().setTransform(
                target.getObstacle().getBody().getPosition().cpy().add(offset),
                targetAngle
        );
        obstacle.getBody().setLinearVelocity(target.getObstacle().getBody().getLinearVelocity().cpy());
        obstacle.getBody().setAngularVelocity(0);  // zero this out, setTransform handles rotation
    }

    public void clearSubject() {
        this.subject = null;
        this.subjectType = null;
        hasSubject = false;
    }

//    public Color getSubjectColor() {
//        return switch (subjectType) {
//            case ROCK -> Color.GRAY;
//            case ICE -> Color.TEAL;
//            case CLOUD -> Color.WHITE;
//        };
//    }

    public Color getColor() {
        return Color.BLACK;
    }


}
