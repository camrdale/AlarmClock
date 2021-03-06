package org.camrdale.clock.thing.web;

import com.google.common.base.Objects;
import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    @SerializedName("verification_number")
    private String verificationNumber;

    RegisterRequest(String verificationNumber) {
        this.verificationNumber = verificationNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegisterRequest that = (RegisterRequest) o;
        return Objects.equal(verificationNumber, that.verificationNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(verificationNumber);
    }

    @Override
    public String toString() {
        return "RegisterRequest{" +
                "verificationNumber='" + verificationNumber + '\'' +
                '}';
    }
}
