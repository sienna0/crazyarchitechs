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
    private static float musicVolume = 0.25f;
    private static float sfxVolume = 1.0f;
    private static float preMuteMusicVolume = 1.0f;
    private static float preMuteSfxVolume = 1.0f;
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
        if (musicOn) {
            preMuteMusicVolume = musicVolume > 0f ? musicVolume : 1.0f;
            setMusicVolume(0f);
            setMusicOn(false);
        } else {
            setMusicOn(true);
            setMusicVolume(preMuteMusicVolume);
        }
    }

    public static float getMusicVolume() { return musicVolume; }

    public static void setMusicVolume(float v) {
        musicVolume = Math.max(0f, Math.min(1f, v));
        // If dragging above 0 while muted, unmute automatically.
        if (musicVolume > 0f && !musicOn) {
            musicOn = true;
        }
        for (Music m : registeredMusic) {
            if (m != null) m.setVolume(musicVolume);
        }
    }

    public static float getSfxVolume() { return sfxVolume; }

    public static void setSfxVolume(float v) {
        sfxVolume = Math.max(0f, Math.min(1f, v));
        if (sfxVolume > 0f && !sfxOn) {
            sfxOn = true;
        }
    }

    /** Remember a looping track (or any music) so {@link #stopRegisteredMusic()} can stop it. */
    public static void registerMusic(Music music) {
        if (music != null && !registeredMusic.contains(music, true)) {
            registeredMusic.add(music);
            music.setVolume(musicVolume);
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
        if (sfxOn) {
            preMuteSfxVolume = sfxVolume > 0f ? sfxVolume : 1.0f;
            setSfxVolume(0f);
            setSfxOn(false);
        } else {
            setSfxOn(true);
            setSfxVolume(preMuteSfxVolume);
        }
    }

    /** Multiply a nominal SFX volume by the global sfx volume; zero when SFX are disabled. */
    public static float effectiveSfxVolume(float volume) {
        return sfxOn ? volume * sfxVolume : 0f;
    }

    private static void stopTaggedSoundEffects() {
        SoundEffectManager mgr = SoundEffectManager.getInstance();
        mgr.stop("jump");
        mgr.stop("hover");
        mgr.stop("fire");
        mgr.stop("plop");
    }
}
