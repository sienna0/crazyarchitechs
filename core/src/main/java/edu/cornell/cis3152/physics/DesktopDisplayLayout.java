package edu.cornell.cis3152.physics;

import java.util.Locale;

/**
 * Windowed preset sizes for desktop builds, split by host OS. macOS uses scaled Retina “points” for
 * GLFW window sizes; a 1920×1080 frame often exceeds the built-in display’s logical height, so the
 * “1080p-class” preset uses 1280×720 there instead. Windows and Linux use a literal 1920×1080 window.
 * <p>
 * A future options screen can expose these as labeled choices or let players pick “Windows-style” sizes on Mac.
 */
public final class DesktopDisplayLayout {

    private static final String OS_NAME =
            System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

    private DesktopDisplayLayout() {}

    public static boolean isMacOs() {
        return OS_NAME.contains("mac");
    }

    public static boolean isWindows() {
        return OS_NAME.contains("win");
    }

    /** Anything not macOS/Windows (e.g. Linux): same presets as Windows. */
    public static boolean usesWindowsStyleWindowPresets() {
        return !isMacOs();
    }

    /** 640×360 design window (keyboard {@code Y}) — 16:9 on every desktop OS. */
    public static int smallWindowWidth() {
        return 640;
    }

    public static int smallWindowHeight() {
        return 360;
    }

    /**
     * Mid 16:9 windowed preset (keyboard {@code U}): 1280×720 on Windows and Linux; 960×540 on macOS
     * so the frame usually fits the built-in display’s logical desktop without overflowing.
     */
    public static int mediumWindowWidth() {
        return isMacOs() ? 960 : 1280;
    }

    public static int mediumWindowHeight() {
        return isMacOs() ? 540 : 720;
    }

    /**
     * Largest 16:9 windowed preset (keyboard {@code I}): 1920×1080 on Windows and Linux; 1280×720 on macOS.
     */
    public static int largeWindowWidth() {
        return isMacOs() ? 1280 : 1920;
    }

    public static int largeWindowHeight() {
        return isMacOs() ? 720 : 1080;
    }
}
