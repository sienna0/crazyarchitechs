package edu.cornell.cis3152.physics.world;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Zuko's photo inventory. Holds a fixed number of {@link Picture} slots
 * that can be filled by photographing objects and emptied by sticking
 * or dropping photos.
 *
 * The slot count is read from the Zuko JSON block ("inventory_size"),
 * defaulting to 5 if the key is absent.
 */
public class Inventory {

    /** Ordered array of picture slots; index == slot position in the HUD. */
    private final Array<Picture> pictureInventory;

    /** Number of slots in this level's inventory. */
    private final int size;

    /** Index of the currently selected slot (for keyboard cycling). */
    private int currPicIndex;

    /**
     * Creates an inventory sized from the Zuko JSON data.
     *
     * @param data the Zuko JSON node (expects "inventory_size")
     */
    public Inventory(JsonValue data, JsonValue levelSettings) {

        this.size = (levelSettings != null && levelSettings.has("inventory_size"))
                ? levelSettings.getInt("inventory_size")
                : data.getInt("inventory_size", 5);
        this.currPicIndex = 0;
        this.pictureInventory = new Array<>(size);
        pictureInventory.ordered = true;
        populateInventory();
    }

    public int getCurrPicIndex() { return currPicIndex; }

    /** Advances the keyboard-cycling index by one slot, wrapping around. */
    public void nextCurrPic() { currPicIndex = (currPicIndex + 1) % size; }

    /**
     * Returns the picture at the current cycling index, or null if empty.
     */
    public Picture getCurrPicture() {
        for (Picture pic : pictureInventory) {
            if (pic.getId() == currPicIndex) { return pic; }
        }
        return null;
    }

    /**
     * Returns the picture in a specific slot, or null if the index is out of range.
     *
     * @param id slot index (0-based)
     */
    public Picture getPicture(int id) {
        if (id < 0 || id >= pictureInventory.size) { return null; }
        return pictureInventory.get(id);
    }

    /** Returns true if at least one slot has no subject (available for a new photo). */
    public boolean isAvailablePicture() {
        for (Picture pic : pictureInventory) {
            if (!pic.hasSubject) { return true; }
        }
        return false;
    }

    /** Returns the first unused picture slot, or null if all slots are full. */
    public Picture getUnusedPicture() {
        for (Picture pic : pictureInventory) {
            if (!pic.hasSubject) { return pic; }
        }
        return null;
    }

    /** Returns true if any picture in the inventory is currently stuck on the given object. */
    public boolean hasPicture(GameObject go) {
        for (Picture pic : pictureInventory) {
            if (pic.getTarget() != null && pic.getTarget() == go) { return true; }
        }
        return false;
    }

    public int getSize() { return size; }

    /**
     * Places a picture into the first available slot.
     * Does nothing if the inventory is full.
     */
    public void addPicture(Picture picture) {
        for (int i = 0; i < pictureInventory.size; i++) {
            if (!pictureInventory.get(i).hasSubject) {
                picture.setId(i);
                pictureInventory.set(i, picture);
                return;
            }
        }
    }

    /** Clears a slot by replacing it with a blank picture. */
    public void removePicture(int index) {
        if (index >= 0 && index < pictureInventory.size) {
            pictureInventory.set(index, new Picture(index));
        }
    }

    /** Resets all slots to blank pictures. */
    public void reset() {
        pictureInventory.clear();
        populateInventory();
    }

    /** Fills every slot with a blank {@link Picture}. */
    private void populateInventory() {
        for (int i = 0; i < size; i++) {
            pictureInventory.add(new Picture(i));
        }
    }
}
