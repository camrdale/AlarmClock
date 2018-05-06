package org.camrdale.clock.web;

public class Alarm {
    private String crontab;
    private Boolean buzzer;

    public String getCrontab() {
        return crontab;
    }

    public Boolean getBuzzer() {
        return buzzer;
    }
}
