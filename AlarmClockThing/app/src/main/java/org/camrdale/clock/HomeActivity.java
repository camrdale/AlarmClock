package org.camrdale.clock;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import org.camrdale.clock.peripherals.ButtonManager;
import org.camrdale.clock.peripherals.DisplayManager;
import org.camrdale.clock.peripherals.LedManager;
import org.camrdale.clock.sounds.MediaManager;

import java.util.Calendar;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

/**
 * Skeleton of an Android Things activity.
 */
public class HomeActivity extends Activity {
    private static final String TAG = HomeActivity.class.getSimpleName();

    private static final String ALARM_ACTION = "org.camrdale.clock.ALARM";
    private static final String EXPIRE_ALARM_ACTION = "org.camrdale.clock.EXPIRE_ALARM";
    private static final String SNOOZE_ACTION = "org.camrdale.clock.SNOOZE";
    private static final String SLEEP_ACTION = "org.camrdale.clock.SLEEP";

    private enum AlarmState {
        IDLE,
        SLEEP,
        FIRING,
        SNOOZED
    }

    @Inject MediaManager mediaManager;
    @Inject DisplayManager displayManager;
    @Inject LedManager ledManager;
    @Inject ButtonManager buttonManager;

    private AlarmManager alarmManager;
    private BroadcastReceiver alarmReceiver;
    private PendingIntent nextAlarmIntent;
    private PendingIntent snoozeIntent;
    private PendingIntent sleepIntent;
    private PendingIntent expireAlarmIntent;

    private AlarmState mCurrentState = AlarmState.IDLE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        alarmReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "Alarm fired");
                alarmReceived(intent);
            }
        };
        registerReceiver(alarmReceiver, new IntentFilter(ALARM_ACTION));
        registerReceiver(alarmReceiver, new IntentFilter(EXPIRE_ALARM_ACTION));
        registerReceiver(alarmReceiver, new IntentFilter(SNOOZE_ACTION));
        registerReceiver(alarmReceiver, new IntentFilter(SLEEP_ACTION));

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

        mediaManager.initialize(this);
        displayManager.initialize();
        ledManager.initialize();
        buttonManager.initialize();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_A) {
            ledManager.setRedLed(true);
            displayManager.setDisplayMode(DisplayManager.DisplayMode.MINUTE_SECOND);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_B) {
            ledManager.setGreenLed(true);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_C) {
            ledManager.setBlueLed(true);
            if (mCurrentState == AlarmState.IDLE) {
                displayManager.setDisplayMode(DisplayManager.DisplayMode.MONTH_DAY);
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_A) {
            ledManager.setRedLed(false);
            displayManager.setDisplayMode(DisplayManager.DisplayMode.HOUR_MINUTE);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_B) {
            ledManager.setGreenLed(false);
            switch (mCurrentState) {
                case IDLE:
                case SLEEP:
                    if (sleepIntent != null) {
                        sleepIntent.cancel();
                        sleepIntent = null;
                    }

                    if (mCurrentState != AlarmState.SLEEP) {
                        mCurrentState = AlarmState.SLEEP;
                        mediaManager.startPlaying();
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
                    mediaManager.stopPlaying();
                    break;
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_C) {
            ledManager.setBlueLed(false);
            switch (mCurrentState) {
                case FIRING:
                    mCurrentState = AlarmState.SNOOZED;
                    mediaManager.stopPlaying();

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
                    mediaManager.stopPlaying();
                    break;
                case IDLE:
                    displayManager.setDisplayMode(DisplayManager.DisplayMode.HOUR_MINUTE);
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
            mediaManager.startPlaying();

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
            mediaManager.stopPlaying();
        } else if (SNOOZE_ACTION.equals(intent.getAction()) && mCurrentState == AlarmState.SNOOZED) {
            mCurrentState = AlarmState.FIRING;
            mediaManager.startPlaying();
        } else if (SLEEP_ACTION.equals(intent.getAction()) && mCurrentState == AlarmState.SLEEP) {
            mCurrentState = AlarmState.IDLE;
            mediaManager.stopPlaying();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        displayManager.cleanup();
        ledManager.cleanup();
        buttonManager.cleanup();
        mediaManager.cleanup();
    }
}
