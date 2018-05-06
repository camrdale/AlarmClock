package org.camrdale.clock.web;

import com.google.gson.annotations.SerializedName;

public class CheckInRequest {
    @SerializedName("clock_key")
    private String clockKey;

    CheckInRequest(String clockKey) {
        this.clockKey = clockKey;
    }
}
