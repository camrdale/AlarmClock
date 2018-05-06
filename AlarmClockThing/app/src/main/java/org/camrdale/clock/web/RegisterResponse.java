package org.camrdale.clock.web;

import com.google.gson.annotations.SerializedName;

public class RegisterResponse {
    @SerializedName("clock_key")
    private String clockKey;

    public String getClockKey() {
        return clockKey;
    }
}
