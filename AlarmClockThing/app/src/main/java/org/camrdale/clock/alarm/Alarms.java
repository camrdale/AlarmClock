package org.camrdale.clock.alarm;

import android.content.Context;
import android.content.SharedPreferences;

import com.cronutils.parser.CronParser;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class Alarms {
    private static final String TAG = Alarms.class.getSimpleName();

    private static final String PREFERENCES_NAME = "org.camrdale.clock.ALARM_PREFERENCES";
    private static final String PREF_ALARMS_KEY = "currentAlarms";
    private static final String DEFAULT_ALARM = "*/15 * * * *,0";

    private final CronParser cronParser;
    private SharedPreferences preferences;
    private ImmutableList<Alarm> alarms;

    @Inject Alarms(CronParser cronParser) {
        this.cronParser = cronParser;
        alarms = ImmutableList.of();
    }

    public void initialize(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        Set<String> alarmSaveStrings =
                preferences.getStringSet(PREF_ALARMS_KEY, ImmutableSet.of(DEFAULT_ALARM));
        alarms = alarmSaveStrings.stream()
                .map(saveString -> Alarm.fromSaveString(cronParser, saveString))
                .collect(ImmutableList.toImmutableList());
    }

    public void rescheduleAll(Collection<Alarm> alarms) {
        this.alarms = ImmutableList.copyOf(alarms);

        Set<String> alarmSaveStrings = alarms.stream()
                .map(Alarm::toSaveString)
                .collect(Collectors.toSet());

        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet(PREF_ALARMS_KEY, alarmSaveStrings);
        editor.apply();
    }

    public Optional<ZonedDateTime> nextAlarm() {
        return nextAlarm(ZonedDateTime.now());
    }

    public Optional<ZonedDateTime> nextAlarm(ZonedDateTime now) {
        return alarms.stream()
                .map(alarm -> alarm.nextAlarm(now))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted()
                .findFirst();
    }

    public ImmutableList<ZonedDateTime> nextAlarms(int numAlarms) {
        return nextAlarms(numAlarms, ZonedDateTime.now());
    }

    public ImmutableList<ZonedDateTime> nextAlarms(int numAlarms, ZonedDateTime now) {
        ImmutableList.Builder<ZonedDateTime> result = ImmutableList.builder();
        for (int i = 0; i < numAlarms; i++) {
            Optional<ZonedDateTime> nextAlarm = nextAlarm(now);
            if (!nextAlarm.isPresent()) {
                break;
            }
            now = nextAlarm.get();
            result.add(nextAlarm.get());
        }
        return result.build();
    }

    public int getNumAlarms() {
        return alarms.size();
    }

    public List<String> getAlarmJsonStrings() {
        return alarms.stream().map(Alarm::toJsonString).collect(Collectors.toList());
    }
}
