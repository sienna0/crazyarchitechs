package edu.cornell.cis3152.physics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.utils.Array;
import edu.cornell.gdiac.audio.SoundEffectManager;
import edu.cornell.gdiac.assets.AssetDirectory;

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
    private static final Array<Music> pausedByMute = new Array<>();

    private static Music titleIntro;
    private static Music titleLoop;
    private static boolean titleLoopStarted = false;
    private static float titleLoopPlayTime = 0f;
    private static final float TITLE_LOOP_POINT = 16.8f; // when intro transitions to loop
    private static final float TITLE_LOOP_DURATION = 16.8f; // actual loop length in seconds

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
        if (musicVolume == 0f) {
            musicOn = false;
            pausedByMute.clear();
            for (Music m : registeredMusic) {
                if (m != null && m.isPlaying()) {
                    pausedByMute.add(m);  // remember only what was actually playing
                    m.setVolume(0f);
                    m.pause();
                }
            }

        } else {
                if (!musicOn) {
                    musicOn = true;
                    for (Music m : pausedByMute) {  // only resume what was playing before
                        if (m != null) {
                            m.setVolume(musicVolume);
                            m.play();
                        }
                    }
                    pausedByMute.clear();
                } else {
                    for (Music m : registeredMusic) {
                        if (m != null) m.setVolume(musicVolume);
                    }
                }

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

    public static void startTitleMusic(AssetDirectory assets) {
        if (!isMusicOn()) return;

        if (titleIntro == null) {
            titleIntro = assets.getEntry("platform-introost", Music.class);
            titleLoop = assets.getEntry("platform-introost2", Music.class);
            registerMusic(titleIntro);
            registerMusic(titleLoop);
        }

        if (titleIntro == null || titleLoop == null) return;

        titleIntro.stop();
        titleLoop.stop();

        titleIntro.setVolume(musicVolume);
        titleLoop.setVolume(musicVolume);

        titleLoop.setLooping(false);
        titleIntro.play();

        titleLoopStarted = false;  // always reset
        titleLoopPlayTime = 0f;    // always reset
    }

    public static void updateTitleMusic() {
        if (!musicOn || titleIntro == null || titleLoop == null) return;

        if (musicOn && titleLoopStarted && !titleLoop.isPlaying()) {
            titleLoop.setVolume(musicVolume);
            titleLoop.play();
        }

        if (musicOn && !titleLoopStarted && !titleIntro.isPlaying()) {
            titleIntro.setVolume(musicVolume);
            titleIntro.play();
        }

        // Transition from intro to loop
        if (!titleLoopStarted && titleIntro.isPlaying()
                && titleIntro.getPosition() >= TITLE_LOOP_POINT) {
            titleIntro.stop();
            titleLoop.setVolume(musicVolume);
            titleLoop.setLooping(false);
            titleLoop.play();
            titleLoopStarted = true;
            titleLoopPlayTime = 0f;
        }

        // Restart loop by elapsed time instead of position
        if (titleLoopStarted && titleLoop.isPlaying()) {
            titleLoopPlayTime += Gdx.graphics.getDeltaTime();
            if (titleLoopPlayTime >= TITLE_LOOP_DURATION) {
                titleLoop.stop();
                titleLoop.setVolume(musicVolume);
                titleLoop.play();
                titleLoopPlayTime = 0f;
            }
        }
    }

    public static void stopTitleMusic() {
        if (titleIntro != null) titleIntro.stop();
        if (titleLoop != null) titleLoop.stop();

        titleLoopStarted = false;
    }

    public static void fadeOutTitleMusic(float duration) {
        // simplest version for now: just lower volume over time elsewhere
    }

    public static void setTitleMusicVolume(float volume) {
        if (titleIntro != null) titleIntro.setVolume(volume);
        if (titleLoop != null) titleLoop.setVolume(volume);
    }
}
