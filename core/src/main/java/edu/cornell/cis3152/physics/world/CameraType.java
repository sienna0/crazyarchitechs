package edu.cornell.cis3152.physics.world;

public enum CameraType {
    THERMAL("Thermal"),
    REGULAR("Weight"),
    TEXTURE("Texture");

    private final String label;

    CameraType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
