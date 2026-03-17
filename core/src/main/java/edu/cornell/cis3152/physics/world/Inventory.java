package edu.cornell.cis3152.physics.world;


import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.cis3152.physics.InputController;
import edu.cornell.gdiac.audio.SoundEffectManager;

public class Inventory{

    /** The primary array backing of the inventory. Requires that pictures are not moved around. */
    //FIXME Because this will always be small, it may be worth making this private and then only
    // passing copies
    private Array<Picture> pictureInventory;

    /** The size of this level's inventory */
    int size;

    /** The current Picture index*/
    private int currPicIndex;

    /**
     * Initializes a new Inventory instance.
     *
     * @param data is the JSON data for the current level
     */
    public Inventory (JsonValue data){

        // Pretend the json data gives this here
        //this.size = data.get("inventory size")
        size = 5;
        currPicIndex = 0;
        this.pictureInventory = new Array<>(size);
        pictureInventory.ordered = true;
        populateInventory();
    }
    // PUBLIC ACCESSOR METHODS
    public int getCurrPicIndex(){return currPicIndex;}

    public void nextCurrPic(){currPicIndex = (currPicIndex + 1) % size;}

    /**
     * Returns the currently selected picture in inventory
     *
     * Use this for cycling through the pictures
     *
     * @return Picture instance if found, null otherwise
     */
    public Picture getCurrPicture(){
        for (Picture pic : pictureInventory){
            if (pic.getId() == currPicIndex) {return pic;}
        }
        return null;
    }

    /**
     * Returns the picture in inventory with given id
     *
     * Use this for directly clicking on a picture in UI
     *
     * @param id is the id of the desired picture
     * @return Picture instance if found, null otherwise
     */
    public Picture getPicture(int id){
        if (id < 0 || id >= pictureInventory.size){return null;}
        return pictureInventory.get(id);
    }

    /**
     * Returns if there's an available picture to use
     *
     * @return true if unused picture exists else false
     */
    public boolean isAvailablePicture(){
        for (Picture pic : pictureInventory){
            if (!pic.hasSubject) {return true;}
        }
        return false;
    }

    /**
     * Returns an available Picture, false otherwise
     *
     * @return int of an available picture, or -2 if none
     */
    public Picture getUnusedPicture(){
        for (Picture pic : pictureInventory){
            if (!pic.hasSubject) {return pic;}
        }
        return null;
    }

    /**
     * Returns if a GameObject has a picture placed on it
     *
     * @param go is a GameObject
     * @return true if thi
     */
    public boolean hasPicture(GameObject go){
        for (Picture pic : pictureInventory){
            if (pic.getTarget() != null && pic.getTarget() == go) {return true;}
        }
        return false;
    }




    // PRIVATE HELPER METHODS
    /**
     * Initializes the inventory with new Picture objects
     */
    private void populateInventory(){
        for (int i = 0; i < size; i++){
            pictureInventory.add(new Picture(i));
        }
    }

    public int getSize(){
        return size;
    }

    public void addPicture(Picture picture){
        for (int i = 0; i < pictureInventory.size; i++) {
            if (!pictureInventory.get(i).hasSubject) {
                picture.setId(i);
                pictureInventory.set(i,  picture);
                return;
            }
        }
    }

    public void removePicture(int index){
        if (index >= 0 && index < pictureInventory.size){
            pictureInventory.set(index, new Picture(index));
        }
    }

    public void reset() {
        pictureInventory.clear();
        populateInventory();
    }
}
