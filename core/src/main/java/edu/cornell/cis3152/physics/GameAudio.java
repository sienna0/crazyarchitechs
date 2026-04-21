package edu.cornell.cis3152.physics;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.utils.Array;
import edu.cornell.gdiac.audio.SoundEffectManager;

/**
 * Global audio preferences for music and sound effects. Register {@link Music} instances so
 * toggling music off can stop active playback.
 */
public final class GameAudio {

    private static boolean musicOn = true;
    private static boolean sfxOn = true;
    private static final Array<Music> registeredMusic = new Array<>();

    private GameAudio() {
    }

    public static boolean isMusicOn() {
        return musicOn;
    }

    public static void setMusicOn(boolean on) {
        musicOn = on;
        if (!on) {
            stopRegisteredMusic();
        }
    }

    public static void toggleMusic() {
        setMusicOn(!musicOn);
    }

    /** Remember a looping track (or any music) so {@link #stopRegisteredMusic()} can stop it. */
    public static void registerMusic(Music music) {
        if (music != null && !registeredMusic.contains(music, true)) {
            registeredMusic.add(music);
        }
    }

    public static void unregisterMusic(Music music) {
        registeredMusic.removeValue(music, true);
    }

    public static void stopRegisteredMusic() {
        for (Music m : registeredMusic) {
            if (m != null) {
                m.stop();
            }
        }
    }

    public static boolean isSfxOn() {
        return sfxOn;
    }

    public static void setSfxOn(boolean on) {
        sfxOn = on;
        if (!on) {
            stopTaggedSoundEffects();
        }
    }

    public static void toggleSfx() {
        setSfxOn(!sfxOn);
    }

    /** Multiply a nominal SFX volume; zero when SFX are disabled. */
    public static float effectiveSfxVolume(float volume) {
        return sfxOn ? volume : 0f;
    }

    private static void stopTaggedSoundEffects() {
        SoundEffectManager mgr = SoundEffectManager.getInstance();
        mgr.stop("jump");
        mgr.stop("hover");
        mgr.stop("fire");
        mgr.stop("plop");
    }
}
