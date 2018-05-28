package org.camrdale.clock.thing.sounds;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.util.Log;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MediaManager {
    private static final String TAG = MediaManager.class.getSimpleName();

    private MediaPlayer mediaPlayer;

    @Inject MediaManager() {}

    public void startPlaying() {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
            }
            String url = "https://streams2.kqed.org/kqedradio"; // your URL here
            mediaPlayer.reset();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnBufferingUpdateListener((mp, percent) -> Log.i(TAG, "Buffering: " + percent));
            mediaPlayer.setOnPreparedListener(mp -> {
                Log.i(TAG, "Starting playback of media.");
                mp.start();
                mp.setVolume(1.0f, 1.0f);
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to start the stream.", e);
        }
    }

    public void stopPlaying() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    Log.i(TAG, "Stopping playback of media.");
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop the stream.", e);
        }
    }

    public void cleanup() {
        stopPlaying();
    }
}
