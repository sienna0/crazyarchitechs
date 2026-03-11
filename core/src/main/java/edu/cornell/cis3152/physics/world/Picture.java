package edu.cornell.cis3152.physics.world;

import com.badlogic.gdx.physics.box2d.Joint;
import edu.cornell.gdiac.graphics.SpriteMesh;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;

public class Picture extends ObstacleSprite {
    /** The GameObject subject of this picture */
    GameObject subject;
    /** The GameObject the picture is stuck to */
    GameObject target;

    /** The Obj enum type of this picture's subject */
    Obj subjectType;

    /** The CameraType enum type of Camera that took this picture */
    CameraType cameraType;

    /** Whether this picture has been given a subject */
    boolean hasSubject;

    /** The id of this picture. Also serves as its index position in the inventory. */
    private int id;

    private Joint pictureJoint;

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
        hasSubject = true;
        // steal the draw data at reduced size
        // Picture itself would need to be a GameObject for this
        mesh = new SpriteMesh(subject.getMesh());
        mesh.scl(0.5f);
        setTexture(subject.getSpriteSheet().getTexture());
        id = -1;
    }

    /** Returns the ID of this picture */
    public int getId(){return id;}

    /** Sets the pointer for the joint attaching this picture */
    public void setJoint(Joint joint) {
        this.pictureJoint = joint;
    }

    /** Returns the pointer for the joint attaching this picture */
    public Joint getJoint() {
        return pictureJoint;
    }

    /** Returns the enum type of this picture's subject */
    public Obj getSubjectType(){return subjectType;}

    /** Returns the enum type of the Camera that took this picture */
    public CameraType getCameraType(){return cameraType;}

    /**
     * Sets a new GameObject subject for this picture instance.
     *
     * This should not be used unless the pictures are pre-allocated.
     *
     * @param newGameObject overrides the pictures current object
     */
    public void setSubject(GameObject newGameObject) {
        subject = newGameObject;
        subjectType = newGameObject.object;
    }

    /** Returns the subject of this picture */
    public GameObject getSubject(){return subject;}

    public void setTarget(GameObject target, float units) {
        this.target = target;
        BoxObstacle original = (BoxObstacle) target.getObstacle();
        BoxObstacle copy = new BoxObstacle(
                original.getX(), original.getY(),
                original.getWidth(), original.getHeight()
        );
        copy.setBodyType(original.getBodyType());
        copy.setPhysicsUnits(units);
        copy.setSensor(true);
        copy.setUserData(this);
        copy.setGravityScale(0.0f);
        obstacle = copy;
    }


}
