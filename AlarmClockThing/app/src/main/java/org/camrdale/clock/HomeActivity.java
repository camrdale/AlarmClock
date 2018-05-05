package org.camrdale.clock;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import com.cronutils.model.Cron;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import org.camrdale.clock.peripherals.ButtonManager;
import org.camrdale.clock.peripherals.DisplayManager;
import org.camrdale.clock.peripherals.LedManager;
import org.camrdale.clock.sounds.MediaManager;

import java.time.ZonedDateTime;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

/**
 * Skeleton of an Android Things activity.
 */
public class HomeActivity extends Activity {
    private static final String TAG = HomeActivity.class.getSimpleName();

    private static final String PREFERENCES_NAME = "org.camrdale.clock.ALARM_PREFERENCES";
    private static final String PREF_ALARM_KEY = "currentAlarm";
    private static final String DEFAULT_ALARM = "*/15 * * * *";

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

    @Inject CronParser cronParser;

    private SharedPreferences preferences;
    private AlarmManager alarmManager;
    private BroadcastReceiver alarmReceiver;
    private PendingIntent nextAlarmIntent;
    private PendingIntent snoozeIntent;
    private PendingIntent sleepIntent;
    private PendingIntent expireAlarmIntent;

    private AlarmState mCurrentState = AlarmState.IDLE;
    private Cron alarm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        String currentAlarm = preferences.getString(PREF_ALARM_KEY, DEFAULT_ALARM);
        alarm = cronParser.parse(currentAlarm);

        // Get date and time for next alarm.
        ExecutionTime executionTime = ExecutionTime.forCron(alarm);
        ZonedDateTime nextExecution = executionTime.nextExecution(ZonedDateTime.now()).get();

        alarmReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "Alarm fired: " + intent.toString());
                broadcastReceived(intent);
            }
        };
        registerReceiver(alarmReceiver, new IntentFilter(ALARM_ACTION));
        registerReceiver(alarmReceiver, new IntentFilter(EXPIRE_ALARM_ACTION));
        registerReceiver(alarmReceiver, new IntentFilter(SNOOZE_ACTION));
        registerReceiver(alarmReceiver, new IntentFilter(SLEEP_ACTION));

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            Log.i(TAG, "Scheduling alarm for: " + nextExecution);
            nextAlarmIntent =
                    PendingIntent.getBroadcast(this, 0, new Intent(ALARM_ACTION), 0);
            alarmManager.set(AlarmManager.RTC_WAKEUP,
                    nextExecution.toInstant().toEpochMilli(), nextAlarmIntent);
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
                    Log.i(TAG, "Turning on sleep");
                    if (sleepIntent != null) {
                        sleepIntent.cancel();
                        sleepIntent = null;
                    }

                    if (mCurrentState != AlarmState.SLEEP) {
                        mCurrentState = AlarmState.SLEEP;
                        mediaManager.startPlaying();
                    }

                    ZonedDateTime now = ZonedDateTime.now();
                    ZonedDateTime expireSleep = now.plusMinutes(10);
                    Log.i(TAG, "Scheduling expiry of sleep: " + expireSleep);
                    sleepIntent =
                            PendingIntent.getBroadcast(this, 0, new Intent(SLEEP_ACTION), 0);
                    alarmManager.set(AlarmManager.RTC_WAKEUP,
                            expireSleep.toInstant().toEpochMilli(), sleepIntent);
                    break;
                case FIRING:
                case SNOOZED:
                    Log.i(TAG, "Turning off alarm");
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
                    Log.i(TAG, "Snoozing alarm");
                    mCurrentState = AlarmState.SNOOZED;
                    mediaManager.stopPlaying();

                    ZonedDateTime now = ZonedDateTime.now();
                    ZonedDateTime expireSnooze = now.plusMinutes(2);
                    Log.i(TAG, "Scheduling expiry of snooze: " + expireSnooze);
                    snoozeIntent =
                            PendingIntent.getBroadcast(this, 0, new Intent(SNOOZE_ACTION), 0);
                    alarmManager.set(AlarmManager.RTC_WAKEUP,
                            expireSnooze.toInstant().toEpochMilli(), snoozeIntent);
                    break;
                case SLEEP:
                    Log.i(TAG, "Turning off sleep");
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

    private void broadcastReceived(Intent intent) {
        Log.i(TAG, "Received broadcast: " + intent.toString());
        if (ALARM_ACTION.equals(intent.getAction()) && mCurrentState != AlarmState.FIRING) {
            Log.i(TAG, "Turning on alarm");
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

            ZonedDateTime now = ZonedDateTime.now();
            ZonedDateTime expireAlarm = now.plusMinutes(5);
            Log.i(TAG, "Scheduling expiry of alarm: " + expireAlarm);
            expireAlarmIntent =
                    PendingIntent.getBroadcast(this, 0, new Intent(EXPIRE_ALARM_ACTION), 0);
            alarmManager.set(AlarmManager.RTC_WAKEUP,
                    expireAlarm.toInstant().toEpochMilli(), expireAlarmIntent);

            // Get date and time for next alarm.
            ExecutionTime executionTime = ExecutionTime.forCron(alarm);
            ZonedDateTime nextExecution = executionTime.nextExecution(now).get();

            Log.i(TAG, "Scheduling next alarm at: " + nextExecution.toString());
            nextAlarmIntent =
                    PendingIntent.getBroadcast(this, 0, new Intent(ALARM_ACTION), 0);
            alarmManager.set(AlarmManager.RTC_WAKEUP,
                    nextExecution.toInstant().toEpochMilli(), nextAlarmIntent);
        } else if (EXPIRE_ALARM_ACTION.equals(intent.getAction()) && mCurrentState == AlarmState.FIRING) {
            Log.i(TAG, "Alarm expired");
            if (snoozeIntent != null) {
                snoozeIntent.cancel();
                snoozeIntent = null;
            }
            mCurrentState = AlarmState.IDLE;
            mediaManager.stopPlaying();
        } else if (SNOOZE_ACTION.equals(intent.getAction()) && mCurrentState == AlarmState.SNOOZED) {
            Log.i(TAG, "Snooze expired, re-enabling alarm");
            mCurrentState = AlarmState.FIRING;
            mediaManager.startPlaying();
        } else if (SLEEP_ACTION.equals(intent.getAction()) && mCurrentState == AlarmState.SLEEP) {
            Log.i(TAG, "Sleep expired");
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
