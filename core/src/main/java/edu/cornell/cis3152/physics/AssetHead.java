package edu.cornell.cis3152.physics;

import edu.cornell.gdiac.assets.AssetDirectory;

/**
 * Handles the loading of all assets in a specific level.
 */
public class AssetHead {
    /** The asset directory for this head */
    private AssetDirectory directory;

    /**
     * Creates a new AssetHead with the given directory.
     *
     * @param directory The asset directory to use
     */
    public AssetHead(AssetDirectory directory) {
        this.directory = directory;
    }

    /**
     * Loads assets for a specific level.
     *
     * @param level The level number
     */
    public void loadLevelAssets(int level) {
        // Implementation for loading level-specific assets
    }

    /**
     * Unloads assets for a specific level.
     *
     * @param level The level number
     */
    public void unloadLevelAssets(int level) {
        // Implementation for unloading level-specific assets
    }

    /**
     * Returns the asset directory.
     *
     * @return the asset directory
     */
    public AssetDirectory getAssets() {
        return directory;
    }
}
