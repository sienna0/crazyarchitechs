package edu.cornell.cis3152.physics.screen;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import edu.cornell.cis3152.physics.world.GameObject;
import edu.cornell.cis3152.physics.world.Picture;

/**
 * Mutable gameplay state shared by {@code LevelBaseScene} helpers.
 */
public class WorldState {
    private final Array<Picture> pictures = new Array<>();
    private final Array<GameObject> highlighted = new Array<>();
    private final Vector2 pauseMouseCache = new Vector2();

    private Picture activePicture;
    private int selectedSlotIndex = -1;
    private boolean showRange;
    private boolean pauseIconHovered;
    private boolean pauseIconWasHovered;

    public void reset() {
        pictures.clear();
        highlighted.clear();
        activePicture = null;
        selectedSlotIndex = -1;
        showRange = false;
        pauseIconHovered = false;
        pauseIconWasHovered = false;
    }

    public Array<Picture> getPictures() {
        return pictures;
    }

    public void addHighlight(GameObject go) {
        highlighted.add(go);
    }

    public Array<GameObject> getHighlighted() {
        return highlighted;
    }

    public Vector2 getPauseMouseCache() {
        return pauseMouseCache;
    }

    public Picture getActivePicture() {
        return activePicture;
    }

    public void setActivePicture(Picture activePicture) {
        this.activePicture = activePicture;
    }

    public int getSelectedSlotIndex() {
        return selectedSlotIndex;
    }

    public void setSelectedSlotIndex(int selectedSlotIndex) {
        this.selectedSlotIndex = selectedSlotIndex;
    }

    public boolean isShowRange() {
        return showRange;
    }

    public void setShowRange(boolean showRange) {
        this.showRange = showRange;
    }

    public boolean isPauseIconHovered() {
        return pauseIconHovered;
    }

    public void setPauseIconHovered(boolean pauseIconHovered) {
        this.pauseIconHovered = pauseIconHovered;
    }

    public boolean wasPauseIconHovered() {
        return pauseIconWasHovered;
    }

    public void setPauseIconWasHovered(boolean pauseIconWasHovered) {
        this.pauseIconWasHovered = pauseIconWasHovered;
    }
}
