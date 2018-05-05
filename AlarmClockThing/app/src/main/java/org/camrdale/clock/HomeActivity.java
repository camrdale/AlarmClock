package org.camrdale.clock;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.KeyEvent;

import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay;
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat;
import com.google.android.things.pio.Gpio;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.Locale;

/**
 * Skeleton of an Android Things activity.
 */
public class HomeActivity extends Activity {
    private static final String TAG = HomeActivity.class.getSimpleName();
    private static final String ALARM_ACTION = "org.camrdale.clock.ALARM";
    private static final String EXPIRE_ALARM_ACTION = "org.camrdale.clock.EXPIRE_ALARM";
    private static final String SNOOZE_ACTION = "org.camrdale.clock.SNOOZE";
    private static final String SLEEP_ACTION = "org.camrdale.clock.SLEEP";

    private enum DisplayMode {
        HOUR_MINUTE,
        MINUTE_SECOND
    }
    private enum AlarmState {
        IDLE,
        SLEEP,
        FIRING,
        SNOOZED
    }

    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;

    private AlphanumericDisplay mDisplay;
    private Gpio mRedLed;
    private Gpio mGreenLed;
    private Gpio mBlueLed;
    private ButtonInputDriver mButtonA;
    private ButtonInputDriver mButtonB;
    private ButtonInputDriver mButtonC;

    private CountDownTimer countDownTimer;
    private Clock clock;
    private AlarmManager alarmManager;
    private BroadcastReceiver alarmReceiver;
    private PendingIntent nextAlarmIntent;
    private PendingIntent snoozeIntent;
    private PendingIntent sleepIntent;
    private PendingIntent expireAlarmIntent;

    private DisplayMode mDisplayMode = DisplayMode.HOUR_MINUTE;
    private AlarmState mCurrentState = AlarmState.IDLE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        clock = Clock.systemDefaultZone();
        alarmReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "Alarm fired");
                alarmReceived(intent);
            }
        };
        registerReceiver(alarmReceiver, new IntentFilter(ALARM_ACTION));
        registerReceiver(alarmReceiver, new IntentFilter(SNOOZE_ACTION));
        registerReceiver(alarmReceiver, new IntentFilter(SLEEP_ACTION));

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                    0);
        }

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            Log.i(TAG, "Scheduling alarm");
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());

            nextAlarmIntent =
                    PendingIntent.getBroadcast(this, 0, new Intent(ALARM_ACTION), 0);
            alarmManager.set(AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis() + 60 * 1000, nextAlarmIntent);
        }

        try {
            mDisplay = RainbowHat.openDisplay();
            mDisplay.setEnabled(true);
            mDisplay.clear();
            Log.d(TAG, "Initialized I2C Display");
        } catch (IOException e) {
            Log.e(TAG, "Error initializing display", e);
            Log.d(TAG, "Display disabled");
            mDisplay = null;
        }

        try {
            mRedLed = RainbowHat.openLedRed();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize the LED.", e);
        }
        try {
            mGreenLed = RainbowHat.openLedGreen();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize the LED.", e);
        }
        try {
            mBlueLed = RainbowHat.openLedBlue();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize the LED.", e);
        }

        try {
            mButtonA = RainbowHat.createButtonAInputDriver(KeyEvent.KEYCODE_A);
            mButtonA.register();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize the button.", e);
        }
        try {
            mButtonB = RainbowHat.createButtonBInputDriver(KeyEvent.KEYCODE_B);
            mButtonB.register();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize the button.", e);
        }
        try {
            mButtonC = RainbowHat.createButtonCInputDriver(KeyEvent.KEYCODE_C);
            mButtonC.register();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize the button.", e);
        }

        countDownTimer = new CountDownTimer(Long.MAX_VALUE, 1000) {
            // This is called after every 1 sec interval.
            public void onTick(long millisUntilFinished) {
                showCurrentTime();
            }

            public void onFinish() {
                start();
            }
        }.start();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_A) {
            try {
                mRedLed.setValue(true);
            } catch (Exception e) {
                Log.e(TAG, "Failed to light the LED.", e);
            }
            mDisplayMode = DisplayMode.MINUTE_SECOND;
            showCurrentTime();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_B) {
            try {
                mGreenLed.setValue(true);
            } catch (Exception e) {
                Log.e(TAG, "Failed to light the LED.", e);
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_C) {
            try {
                mBlueLed.setValue(true);
            } catch (Exception e) {
                Log.e(TAG, "Failed to light the LED.", e);
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_A) {
            try {
                mRedLed.setValue(false);
            } catch (Exception e) {
                Log.e(TAG, "Failed to unlight the LED.", e);
            }
            mDisplayMode = DisplayMode.HOUR_MINUTE;
            showCurrentTime();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_B) {
            try {
                mGreenLed.setValue(false);
            } catch (Exception e) {
                Log.e(TAG, "Failed to light the LED.", e);
            }
            switch (mCurrentState) {
                case IDLE:
                case SLEEP:
                    if (sleepIntent != null) {
                        sleepIntent.cancel();
                        sleepIntent = null;
                    }

                    if (mCurrentState != AlarmState.SLEEP) {
                        mCurrentState = AlarmState.SLEEP;
                        startPlaying();
                    }

                    Log.i(TAG, "Scheduling expiry of sleep");
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(System.currentTimeMillis());
                    sleepIntent =
                            PendingIntent.getBroadcast(this, 0, new Intent(SLEEP_ACTION), 0);
                    alarmManager.set(AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis() + 10 * 60 * 1000, sleepIntent);
                    break;
                case FIRING:
                case SNOOZED:
                    if (snoozeIntent != null) {
                        snoozeIntent.cancel();
                        snoozeIntent = null;
                    }
                    if (expireAlarmIntent != null) {
                        expireAlarmIntent.cancel();
                        expireAlarmIntent = null;
                    }
                    mCurrentState = AlarmState.IDLE;
                    stopPlaying();
                    break;
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_C) {
            try {
                mBlueLed.setValue(false);
            } catch (Exception e) {
                Log.e(TAG, "Failed to unlight the LED.", e);
            }
            switch (mCurrentState) {
                case FIRING:
                    mCurrentState = AlarmState.SNOOZED;
                    stopPlaying();

                    Log.i(TAG, "Scheduling expiry of snooze");
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(System.currentTimeMillis());
                    snoozeIntent =
                            PendingIntent.getBroadcast(this, 0, new Intent(SNOOZE_ACTION), 0);
                    alarmManager.set(AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis() + 2 * 60 * 1000, snoozeIntent);
                    break;
                case SLEEP:
                    if (sleepIntent != null) {
                        sleepIntent.cancel();
                        sleepIntent = null;
                    }
                    mCurrentState = AlarmState.IDLE;
                    stopPlaying();
                    break;
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void alarmReceived(Intent intent) {
        Log.i(TAG, "Received alarm: " + intent.toString());
        if (ALARM_ACTION.equals(intent.getAction()) && mCurrentState != AlarmState.FIRING) {
            if (snoozeIntent != null) {
                snoozeIntent.cancel();
                snoozeIntent = null;
            }
            if (sleepIntent != null) {
                sleepIntent.cancel();
                sleepIntent = null;
            }
            if (expireAlarmIntent != null) {
                expireAlarmIntent.cancel();
                expireAlarmIntent = null;
            }
            mCurrentState = AlarmState.FIRING;
            startPlaying();

            Log.i(TAG, "Scheduling expiry of alarm");
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            expireAlarmIntent =
                    PendingIntent.getBroadcast(this, 0, new Intent(EXPIRE_ALARM_ACTION), 0);
            alarmManager.set(AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis() + 5 * 60 * 1000, expireAlarmIntent);

            Log.i(TAG, "Scheduling next alarm");
            nextAlarmIntent =
                    PendingIntent.getBroadcast(this, 0, new Intent(ALARM_ACTION), 0);
            alarmManager.set(AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis() + 15 * 60 * 1000, nextAlarmIntent);
        } else if (EXPIRE_ALARM_ACTION.equals(intent.getAction()) && mCurrentState == AlarmState.FIRING) {
            if (snoozeIntent != null) {
                snoozeIntent.cancel();
                snoozeIntent = null;
            }
            mCurrentState = AlarmState.IDLE;
            stopPlaying();
        } else if (SNOOZE_ACTION.equals(intent.getAction()) && mCurrentState == AlarmState.SNOOZED) {
            mCurrentState = AlarmState.FIRING;
            startPlaying();
        } else if (SLEEP_ACTION.equals(intent.getAction()) && mCurrentState == AlarmState.SLEEP) {
            mCurrentState = AlarmState.IDLE;
            stopPlaying();
        }
    }

    private void startPlaying() {
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
            mediaPlayer.setOnPreparedListener(mp -> {
                Log.i(TAG, "Starting playback of media.");
                mp.start();
                mp.setVolume(1.0f, 1.0f);
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to start the stream.", e);
        }
    }

    private void stopPlaying() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop the stream.", e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            countDownTimer.cancel();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mDisplay != null) {
            try {
                mDisplay.clear();
                mDisplay.setEnabled(false);
                mDisplay.close();
            } catch (IOException e) {
                Log.e(TAG, "Error disabling display", e);
            } finally {
                mDisplay = null;
            }
        }

        if (mRedLed != null) {
            try {
                mRedLed.setValue(false);
                mRedLed.close();
            } catch (IOException e) {
                Log.e(TAG, "Error disabling led", e);
            } finally {
                mRedLed = null;
            }
        }
        if (mGreenLed != null) {
            try {
                mGreenLed.setValue(false);
                mGreenLed.close();
            } catch (IOException e) {
                Log.e(TAG, "Error disabling led", e);
            } finally {
                mGreenLed = null;
            }
        }
        if (mBlueLed != null) {
            try {
                mBlueLed.setValue(false);
                mBlueLed.close();
            } catch (IOException e) {
                Log.e(TAG, "Error disabling led", e);
            } finally {
                mBlueLed = null;
            }
        }

        if (mButtonA != null) {
            try {
                mButtonA.close();
            } catch (IOException e) {
                Log.e(TAG, "Error disabling button", e);
            } finally {
                mButtonA = null;
            }
        }
        if (mButtonB != null) {
            try {
                mButtonB.close();
            } catch (IOException e) {
                Log.e(TAG, "Error disabling button", e);
            } finally {
                mButtonB = null;
            }
        }
        if (mButtonC != null) {
            try {
                mButtonC.close();
            } catch (IOException e) {
                Log.e(TAG, "Error disabling button", e);
            } finally {
                mButtonC = null;
            }
        }

        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void showCurrentTime() {
        LocalDateTime now = LocalDateTime.now(clock);
        switch(mDisplayMode) {
            case HOUR_MINUTE:
                updateDisplay(
                        String.format(Locale.US, "%2d.%02d",
                                now.get(ChronoField.CLOCK_HOUR_OF_AMPM),
                                now.get(ChronoField.MINUTE_OF_HOUR)));
                break;
            case MINUTE_SECOND:
                updateDisplay(
                        String.format(Locale.US, "%2d.%02d",
                                now.get(ChronoField.MINUTE_OF_HOUR),
                                now.get(ChronoField.SECOND_OF_MINUTE)));
                break;
        }
    }

    private void updateDisplay(String value) {
        if (mDisplay != null) {
            try {
                mDisplay.display(value);
            } catch (IOException e) {
                Log.e(TAG, "Error setting display", e);
            }
        }
    }
}
