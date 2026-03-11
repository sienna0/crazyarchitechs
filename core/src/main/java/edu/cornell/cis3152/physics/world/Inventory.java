package edu.cornell.cis3152.physics.world;


import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;

public class Inventory{

    /** The primary array backing of the inventory. Requires that pictures are not moved around. */
    //FIXME Because this will always be small, it may be worth making this private and then only
    // passing copies
    private Array<Picture> pictureInventory;

    /** The size of this level's inventory */
    int size;

    /**
     * Initializes a new Inventory instance.
     *
     * @param data is the JSON data for the current level
     */
    public Inventory (JsonValue data){

        // Pretend the json data gives this here
        //this.size = data.get("inventory size")
        size = 5;
        this.pictureInventory = new Array<>(size);
        pictureInventory.ordered = true;
        populateInventory();
    }
    // PUBLIC ACCESSOR METHODS
    /**
     * Returns the picture in inventory with given id
     *
     * @param id is the id of the desired picture
     * @return Picture instance if found, null otherwise
     */
    public Picture getPicture(int id){
        for (Picture pic : pictureInventory){
            if (pic.getId() == id) {return pic;}
        }
        return null;
    }

    /**
     * Returns an available picture's id that can be used, otherwise -2
     *
     * @return int of an available picture, or -2 if none
     */
    public int availablePicture(){
        for (Picture pic : pictureInventory){
            if (!pic.hasSubject) {return pic.getId();}
        }
        return -2;
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
}
