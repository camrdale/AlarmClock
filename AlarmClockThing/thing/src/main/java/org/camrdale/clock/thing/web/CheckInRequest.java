package org.camrdale.clock.thing.web;

import com.google.common.base.Objects;
import com.google.gson.annotations.SerializedName;

public class CheckInRequest {
    @SerializedName("clock_key")
    private String clockKey;

    CheckInRequest(String clockKey) {
        this.clockKey = clockKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CheckInRequest that = (CheckInRequest) o;
        return Objects.equal(clockKey, that.clockKey);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(clockKey);
    }

    @Override
    public String toString() {
        return "CheckInRequest{" +
                "clockKey='" + clockKey + '\'' +
                '}';
    }
}
