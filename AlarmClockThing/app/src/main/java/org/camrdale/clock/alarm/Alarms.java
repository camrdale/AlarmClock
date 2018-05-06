package org.camrdale.clock.alarm;

import com.cronutils.parser.CronParser;
import com.google.common.collect.ImmutableList;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Alarms {
    private static final String TAG = Alarms.class.getSimpleName();

    private final AlarmStorage alarmStorage;

    private ImmutableList<Alarm> alarms;

    @Inject Alarms(AlarmStorage alarmStorage, CronParser cronParser) {
        this.alarmStorage = alarmStorage;
        alarms = alarmStorage.getAlarms().stream()
                .map(saveString -> Alarm.fromSaveString(cronParser, saveString))
                .collect(ImmutableList.toImmutableList());
    }

    public void rescheduleAll(Collection<Alarm> alarms) {
        this.alarms = ImmutableList.copyOf(alarms);

        Set<String> alarmSaveStrings = alarms.stream()
                .map(Alarm::toSaveString)
                .collect(Collectors.toSet());
        alarmStorage.saveNewAlarms(alarmSaveStrings);
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
}
