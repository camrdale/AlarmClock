package org.camrdale.clock.thing.web;

import com.google.common.base.Objects;

public class Alarm {
    private String crontab;
    private Boolean buzzer;

    public String getCrontab() {
        return crontab;
    }

    public Boolean getBuzzer() {
        return buzzer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Alarm alarm = (Alarm) o;
        return Objects.equal(crontab, alarm.crontab) &&
                Objects.equal(buzzer, alarm.buzzer);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(crontab, buzzer);
    }

    @Override
    public String toString() {
        return "Alarm{" +
                "crontab='" + crontab + '\'' +
                ", buzzer=" + buzzer +
                '}';
    }
}
