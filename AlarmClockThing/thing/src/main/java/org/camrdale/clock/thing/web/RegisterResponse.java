package org.camrdale.clock.thing.web;

import com.google.common.base.Objects;
import com.google.gson.annotations.SerializedName;

public class RegisterResponse {
    @SerializedName("clock_key")
    private String clockKey;

    public String getClockKey() {
        return clockKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegisterResponse that = (RegisterResponse) o;
        return Objects.equal(clockKey, that.clockKey);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(clockKey);
    }

    @Override
    public String toString() {
        return "RegisterResponse{" +
                "clockKey='" + clockKey + '\'' +
                '}';
    }
}
