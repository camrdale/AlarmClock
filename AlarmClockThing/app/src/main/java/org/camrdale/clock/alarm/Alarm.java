package org.camrdale.clock.alarm;

import com.cronutils.model.Cron;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.google.common.base.Splitter;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public class Alarm {

    private final String crontab;
    private final boolean buzzer;
    private final Cron cron;

    public static Alarm fromSaveString(CronParser cronParser, String saveString) {
        List<String> pieces = Splitter.on(',').trimResults().splitToList(saveString);
        String crontab = pieces.get(0);
        boolean buzzer = false;
        if (pieces.size() > 1) {
            buzzer = !pieces.get(1).equals("0");
        }
        return new Alarm(cronParser, crontab, buzzer);
    }

    public static Alarm fromJson(CronParser cronParser, org.camrdale.clock.web.Alarm alarm) {
        return new Alarm(cronParser, alarm.getCrontab(), alarm.getBuzzer());
    }

    private Alarm(CronParser cronParser, String crontab, boolean buzzer) {
        this.crontab = crontab;
        this.buzzer = buzzer;
        this.cron = cronParser.parse(crontab);
    }

    public String toSaveString() {
        return crontab + "," + (buzzer ? "1" : "0");
    }

    /** Get date and time for next alarm. */
    public Optional<ZonedDateTime> nextAlarm() {
        return nextAlarm(ZonedDateTime.now());
    }

    /** Get date and time for next alarm after "now". */
    public Optional<ZonedDateTime> nextAlarm(ZonedDateTime now) {
        ExecutionTime executionTime = ExecutionTime.forCron(cron);
        return executionTime.nextExecution(now);
    }

    public String getCrontab() {
        return crontab;
    }

    public boolean getBuzzer() {
        return buzzer;
    }
}
