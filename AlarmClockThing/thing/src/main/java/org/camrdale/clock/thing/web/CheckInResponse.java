package org.camrdale.clock.thing.web;

import com.google.common.base.Objects;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CheckInResponse {
    private Boolean claimed;
    private String revision;
    private List<Alarm> alarms;
    @SerializedName("time_zone")
    private String timeZone;

    public Boolean getClaimed() {
        return claimed;
    }

    public String getRevision() {
        return revision;
    }

    public List<Alarm> getAlarms() {
        return alarms;
    }

    public String getTimeZone() {
        return timeZone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CheckInResponse that = (CheckInResponse) o;
        return Objects.equal(claimed, that.claimed) &&
                Objects.equal(revision, that.revision) &&
                Objects.equal(alarms, that.alarms) &&
                Objects.equal(timeZone, that.timeZone);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(claimed, revision, alarms, timeZone);
    }

    @Override
    public String toString() {
        return "CheckInResponse{" +
                "claimed=" + claimed +
                ", revision='" + revision + '\'' +
                ", alarms=" + alarms +
                ", timeZone='" + timeZone + '\'' +
                '}';
    }
}
