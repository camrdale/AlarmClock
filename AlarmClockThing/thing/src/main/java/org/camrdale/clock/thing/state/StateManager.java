package org.camrdale.clock.thing.state;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.util.Log;

import com.cronutils.parser.CronParser;
import com.google.android.things.device.TimeManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.camrdale.clock.thing.alarm.Alarm;
import org.camrdale.clock.thing.alarm.AlarmStorage;
import org.camrdale.clock.thing.alarm.Alarms;
import org.camrdale.clock.thing.sounds.MediaManager;
import org.camrdale.clock.thing.web.CheckInResponse;
import org.camrdale.clock.thing.web.RegisterForUserResponse;
import org.camrdale.clock.thing.web.WebManager;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StateManager {
    private static final String TAG = StateManager.class.getSimpleName();

    private static final String ALARM_ACTION = "org.camrdale.clock.ALARM";
    private static final String EXPIRE_ALARM_ACTION = "org.camrdale.clock.EXPIRE_ALARM";
    private static final String SNOOZE_ACTION = "org.camrdale.clock.SNOOZE";
    private static final String SLEEP_ACTION = "org.camrdale.clock.SLEEP";

    private static final Duration ALARM_EXPIRY_TIME = Duration.ofMinutes(60);
    private static final Duration SNOOZE_EXPIRY_TIME = Duration.ofMinutes(9);
    private static final Duration SLEEP_EXPIRY_TIME = Duration.ofMinutes(60);

    public enum AlarmState {
        IDLE,
        SLEEP,
        FIRING,
        SNOOZED
    }

    private final Alarms alarms;
    private final AlarmStorage alarmStorage;
    private final WebManager webManager;
    private final MediaManager mediaManager;
    private final CronParser cronParser;
    private final Context context;

    private AlarmManager alarmManager;
    private BroadcastReceiver broadcastReceiver;
    private CountDownTimer countDownTimer;

    private PendingIntent nextAlarmIntent;
    private PendingIntent snoozeIntent;
    private PendingIntent sleepIntent;
    private PendingIntent expireAlarmIntent;

    private AlarmState mCurrentState = AlarmState.IDLE;
    private String lastRevision = "";

    @Inject StateManager(
            MediaManager mediaManager,
            WebManager webManager,
            AlarmStorage alarmStorage,
            Alarms alarms,
            CronParser cronParser,
            Context context) {
        this.mediaManager = mediaManager;
        this.webManager = webManager;
        this.alarmStorage = alarmStorage;
        this.alarms = alarms;
        this.cronParser = cronParser;
        this.context = context;

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
            scheduleNextAlarm();
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        countDownTimer = new CountDownTimer(Long.MAX_VALUE, 60 * 1000) {
            // This is called after every 60 sec interval.
            public void onTick(long millisUntilFinished) {
                checkIn();
            }

            public void onFinish() {
                start();
            }
        }.start();
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
                ZonedDateTime expireSleep = now.plus(SLEEP_EXPIRY_TIME);
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
                ZonedDateTime expireSnooze = now.plus(SNOOZE_EXPIRY_TIME);
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
            case IDLE:
                break;
        }
    }

    public void register(String userIdToken, Consumer<String> successCallback, Consumer<Throwable> errorCallback) {
        Log.i(TAG, "Starting registration process with user ID token: " + userIdToken);
        webManager.register(userIdToken, response -> {
            try {
                registrationCallComplete(response);
                successCallback.accept(userIdToken);
            } catch (Exception e) {
                errorCallback.accept(e);
            }
        }, errorCallback);
    }

    private void registrationCallComplete(RegisterForUserResponse response) {
        Log.i(TAG, "Received new clock key: " + response.getClockKey());
        alarmStorage.saveNewWebKey(response.getClockKey());
        checkIn();
    }

    private void checkIn() {
        Optional<String> webKey = alarmStorage.getWebKey();
        if (!webKey.isPresent()) {
            Log.i(TAG, "Skipping checkIn due to lack of clock key");
            return;
        }
        Log.i(TAG, "Starting checkIn process with clock key: " + webKey.get());
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Log.i(TAG, "Using user for checkIn process: " + user.getEmail());
            user.getIdToken(false)
                    .addOnSuccessListener(result -> webManager.checkInForUser(
                            result.getToken(), webKey.get(), this::checkInCallComplete))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed getting user token: " + e, e));
        } else {
            webManager.checkIn(webKey.get(), this::checkInCallComplete);
        }
    }

    private void checkInCallComplete(CheckInResponse response) {
        Log.i(TAG, "Received checkIn response: " + response);
        if (response.getRevision() != null) {
            if (response.getRevision().equals(lastRevision)) {
                Log.i(TAG, "Skipping already seen checkIn response: " + lastRevision);
                return;
            }
        }

        if (response.getClaimed()) {
            if (response.getAlarms() != null) {
                Log.i(TAG, "Rescheduling new alarms: " + response.getAlarms());
                alarms.rescheduleAll(response.getAlarms().stream()
                        .map(alarm -> Alarm.fromJson(cronParser, alarm))
                        .collect(Collectors.toList()));
                if (nextAlarmIntent != null) {
                    nextAlarmIntent.cancel();
                    nextAlarmIntent = null;
                }
                scheduleNextAlarm();
            }
            if (response.getTimeZone() != null && response.getTimeZone().length() > 0) {
                String currentTimeZone = TimeZone.getDefault().getID();
                if (!response.getTimeZone().equals(currentTimeZone)) {
                    Log.i(TAG, "Changing time zone from " + currentTimeZone + " to " + response.getTimeZone());
                    TimeManager timeManager = TimeManager.getInstance();
                    timeManager.setTimeZone(response.getTimeZone());
                }
            }
        }

        if (response.getRevision() != null) {
            lastRevision = response.getRevision();
        }
    }

    private void scheduleNextAlarm() {
        Optional<ZonedDateTime> alarmTime = alarms.nextAlarm();
        if (alarmTime.isPresent()) {
            Log.i(TAG, "Scheduling alarm for: " + alarmTime.get());
            nextAlarmIntent =
                    PendingIntent.getBroadcast(context, 0, new Intent(ALARM_ACTION), 0);
            alarmManager.set(AlarmManager.RTC_WAKEUP,
                    alarmTime.get().toInstant().toEpochMilli(), nextAlarmIntent);
        }
    }

    private void broadcastReceived(Intent intent) {
        Log.i(TAG, "Received broadcast: " + intent.toString());
        if (ALARM_ACTION.equals(intent.getAction())) {
            if (mCurrentState != AlarmState.FIRING) {
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
                ZonedDateTime expireAlarm = now.plus(ALARM_EXPIRY_TIME);
                Log.i(TAG, "Scheduling expiry of alarm: " + expireAlarm);
                expireAlarmIntent =
                        PendingIntent.getBroadcast(context, 0, new Intent(EXPIRE_ALARM_ACTION), 0);
                alarmManager.set(AlarmManager.RTC_WAKEUP,
                        expireAlarm.toInstant().toEpochMilli(), expireAlarmIntent);
            }

            scheduleNextAlarm();
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
        if (nextAlarmIntent != null) {
            nextAlarmIntent.cancel();
            nextAlarmIntent = null;
        }
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
        context.unregisterReceiver(broadcastReceiver);
    }
}
