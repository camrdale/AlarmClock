package org.camrdale.clock.thing.web;

import java.util.List;

public class CheckInResponse {
    private Boolean claimed;
    private String revision;
    private List<Alarm> alarms;

    public Boolean getClaimed() {
        return claimed;
    }

    public String getRevision() {
        return revision;
    }

    public List<Alarm> getAlarms() {
        return alarms;
    }
}
