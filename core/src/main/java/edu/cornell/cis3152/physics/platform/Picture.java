package edu.cornell.cis3152.physics.platform;

public class Picture {
    /** The GameObject subject of this picture */
    GameObject subject;

    /** The Obj enum type of this picture's subject */
    Obj subjectType;

    /**
     * Constructor for a Picture instance.
     *
     * @param subject is the GameObject which is the subject of the picture
     */
    public Picture(GameObject subject) {
        this.subject = subject;
        this.subjectType = subject.object;
    }

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

    /** Returns the enum type of this picture's subject */
    public Obj getSubjectType(){return subjectType;}

    /** Returns the subject of this picture */
    public GameObject getSubject(){return subject;}
}

