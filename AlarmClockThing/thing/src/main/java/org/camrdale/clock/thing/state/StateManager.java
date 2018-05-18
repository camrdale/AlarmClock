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

import org.camrdale.clock.thing.alarm.Alarm;
import org.camrdale.clock.thing.alarm.AlarmStorage;
import org.camrdale.clock.thing.alarm.Alarms;
import org.camrdale.clock.thing.peripherals.DisplayManager;
import org.camrdale.clock.thing.sounds.MediaManager;
import org.camrdale.clock.thing.web.CheckInResponse;
import org.camrdale.clock.thing.web.RegisterResponse;
import org.camrdale.clock.thing.web.WebManager;

import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StateManager {
    private static final String TAG = StateManager.class.getSimpleName();

    private static final String ALARM_ACTION = "org.camrdale.clock.ALARM";
    private static final String EXPIRE_ALARM_ACTION = "org.camrdale.clock.EXPIRE_ALARM";
    private static final String EXPIRE_REGISTRATION_ACTION = "org.camrdale.clock.EXPIRE_REGISTRATION";
    private static final String SNOOZE_ACTION = "org.camrdale.clock.SNOOZE";
    private static final String SLEEP_ACTION = "org.camrdale.clock.SLEEP";

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
    private final DisplayManager displayManager;
    private final CronParser cronParser;
    private final Context context;

    private AlarmManager alarmManager;
    private BroadcastReceiver broadcastReceiver;
    private CountDownTimer countDownTimer;

    private PendingIntent nextAlarmIntent;
    private PendingIntent snoozeIntent;
    private PendingIntent sleepIntent;
    private PendingIntent expireAlarmIntent;
    private PendingIntent expireRegistrationIntent;

    private AlarmState mCurrentState = AlarmState.IDLE;
    private String lastRevision = "";
    private boolean registering = false;

    @Inject StateManager(
            MediaManager mediaManager,
            WebManager webManager,
            DisplayManager displayManager,
            AlarmStorage alarmStorage,
            Alarms alarms,
            CronParser cronParser,
            Context context) {
        this.mediaManager = mediaManager;
        this.webManager = webManager;
        this.displayManager = displayManager;
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
        context.registerReceiver(broadcastReceiver, new IntentFilter(EXPIRE_REGISTRATION_ACTION));
        context.registerReceiver(broadcastReceiver, new IntentFilter(SNOOZE_ACTION));
        context.registerReceiver(broadcastReceiver, new IntentFilter(SLEEP_ACTION));

        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            scheduleNextAlarm();
        }
        scheduleCheckIns(60);
    }

    private void scheduleCheckIns(int seconds) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        countDownTimer = new CountDownTimer(Long.MAX_VALUE, seconds * 1000) {
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
            case IDLE:
                displayManager.setDisplayMode(DisplayManager.DisplayMode.HOUR_MINUTE);
                break;
        }
    }

    public void registerKeyPress() {
        String verificationCode = String.format(Locale.US, "%04d",
                ThreadLocalRandom.current().nextInt(0, 10000));
        Log.i(TAG, "Starting registration process with code: " + verificationCode);
        displayManager.setVerificationCodeDisplayMode(verificationCode);
        webManager.register(verificationCode, this::registrationCallComplete);

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime expireRegistration = now.plusMinutes(1);
        Log.i(TAG, "Scheduling expiry of registration: " + expireRegistration);
        expireRegistrationIntent =
                PendingIntent.getBroadcast(context, 0, new Intent(EXPIRE_REGISTRATION_ACTION), 0);
        alarmManager.set(AlarmManager.RTC_WAKEUP,
                expireRegistration.toInstant().toEpochMilli(), expireRegistrationIntent);
    }

    private void registrationCallComplete(RegisterResponse response) {
        Log.i(TAG, "Received new clock key: " + response.getClockKey());
        alarmStorage.saveNewWebKey(response.getClockKey());
        registering = true;
        scheduleCheckIns(5);
    }

    private void checkIn() {
        Optional<String> webKey = alarmStorage.getWebKey();
        if (!webKey.isPresent()) {
            Log.i(TAG, "Skipping checkIn due to lack of clock key");
            return;
        }
        Log.i(TAG, "Starting checkIn process with clock key: " + webKey.get());
        webManager.checkIn(webKey.get(), this::checkInCallComplete);
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
            if (registering) {
                Log.i(TAG, "Clock is now claimed");
                registrationComplete();
            }
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

    private void registrationComplete() {
        Log.i(TAG, "Registration process is complete");
        registering = false;
        if (expireRegistrationIntent != null) {
            expireRegistrationIntent.cancel();
            expireRegistrationIntent = null;
        }
        displayManager.setDisplayMode(DisplayManager.DisplayMode.HOUR_MINUTE);
        scheduleCheckIns(60);
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
                ZonedDateTime expireAlarm = now.plusMinutes(5);
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
        } else if (EXPIRE_REGISTRATION_ACTION.equals(intent.getAction())) {
            Log.i(TAG, "Registration expired");
            registrationComplete();
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
        if (expireRegistrationIntent != null) {
            expireRegistrationIntent.cancel();
            expireRegistrationIntent = null;
        }
        context.unregisterReceiver(broadcastReceiver);
    }
}
