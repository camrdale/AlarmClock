package org.camrdale.clock.state;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;

import com.cronutils.model.Cron;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import org.camrdale.clock.sounds.MediaManager;

import java.time.ZonedDateTime;

import javax.inject.Inject;

public class StateManager {
    private static final String TAG = StateManager.class.getSimpleName();

    private static final String PREFERENCES_NAME = "org.camrdale.clock.ALARM_PREFERENCES";
    private static final String PREF_ALARM_KEY = "currentAlarm";
    private static final String DEFAULT_ALARM = "*/15 * * * *";

    private static final String ALARM_ACTION = "org.camrdale.clock.ALARM";
    private static final String EXPIRE_ALARM_ACTION = "org.camrdale.clock.EXPIRE_ALARM";
    private static final String SNOOZE_ACTION = "org.camrdale.clock.SNOOZE";
    private static final String SLEEP_ACTION = "org.camrdale.clock.SLEEP";

    public enum AlarmState {
        IDLE,
        SLEEP,
        FIRING,
        SNOOZED
    }

    private final MediaManager mediaManager;
    private final CronParser cronParser;

    private SharedPreferences preferences;
    private AlarmManager alarmManager;
    private BroadcastReceiver broadcastReceiver;
    private PendingIntent nextAlarmIntent;
    private PendingIntent snoozeIntent;
    private PendingIntent sleepIntent;
    private PendingIntent expireAlarmIntent;

    private Context context;
    private AlarmState mCurrentState = AlarmState.IDLE;
    private Cron alarm;

    @Inject StateManager(MediaManager mediaManager, CronParser cronParser) {
        this.mediaManager = mediaManager;
        this.cronParser = cronParser;
    }

    public void initialize(Context context) {
        this.context = context;
        preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        String currentAlarm = preferences.getString(PREF_ALARM_KEY, DEFAULT_ALARM);
        alarm = cronParser.parse(currentAlarm);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "Alarm fired: " + intent.toString());
                broadcastReceived(intent);
            }
        };
        context.registerReceiver(broadcastReceiver, new IntentFilter(ALARM_ACTION));
        context.registerReceiver(broadcastReceiver, new IntentFilter(EXPIRE_ALARM_ACTION));
        context.registerReceiver(broadcastReceiver, new IntentFilter(SNOOZE_ACTION));
        context.registerReceiver(broadcastReceiver, new IntentFilter(SLEEP_ACTION));

        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            // Get date and time for next alarm.
            ExecutionTime executionTime = ExecutionTime.forCron(alarm);
            ZonedDateTime nextExecution = executionTime.nextExecution(ZonedDateTime.now()).get();
            Log.i(TAG, "Scheduling alarm for: " + nextExecution);
            nextAlarmIntent =
                    PendingIntent.getBroadcast(context, 0, new Intent(ALARM_ACTION), 0);
            alarmManager.set(AlarmManager.RTC_WAKEUP,
                    nextExecution.toInstant().toEpochMilli(), nextAlarmIntent);
        }

        mediaManager.initialize(context);
    }

    public AlarmState getCurrentState() {
        return mCurrentState;
    }

    public void sleepKeyPress() {
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
                        PendingIntent.getBroadcast(context, 0, new Intent(SLEEP_ACTION), 0);
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
    }

    public void snoozeKeyPress() {
        switch (mCurrentState) {
            case FIRING:
                Log.i(TAG, "Snoozing alarm");
                mCurrentState = AlarmState.SNOOZED;
                mediaManager.stopPlaying();

                ZonedDateTime now = ZonedDateTime.now();
                ZonedDateTime expireSnooze = now.plusMinutes(2);
                Log.i(TAG, "Scheduling expiry of snooze: " + expireSnooze);
                snoozeIntent =
                        PendingIntent.getBroadcast(context, 0, new Intent(SNOOZE_ACTION), 0);
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
        }
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
                    PendingIntent.getBroadcast(context, 0, new Intent(EXPIRE_ALARM_ACTION), 0);
            alarmManager.set(AlarmManager.RTC_WAKEUP,
                    expireAlarm.toInstant().toEpochMilli(), expireAlarmIntent);

            // Get date and time for next alarm.
            ExecutionTime executionTime = ExecutionTime.forCron(alarm);
            ZonedDateTime nextExecution = executionTime.nextExecution(now).get();
            Log.i(TAG, "Scheduling next alarm at: " + nextExecution.toString());
            nextAlarmIntent =
                    PendingIntent.getBroadcast(context, 0, new Intent(ALARM_ACTION), 0);
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

    public void cleanup() {
        mediaManager.cleanup();
    }
}
