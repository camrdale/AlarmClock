package org.camrdale.clock.thing.web;

import com.google.common.base.Objects;
import com.google.gson.annotations.SerializedName;

public class RegisterForUserRequest {
    @SerializedName("verification_number")
    private String verificationNumber;

    RegisterForUserRequest(String verificationNumber) {
        this.verificationNumber = verificationNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegisterForUserRequest that = (RegisterForUserRequest) o;
        return Objects.equal(verificationNumber, that.verificationNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(verificationNumber);
    }

    @Override
    public String toString() {
        return "RegisterForUserRequest{" +
                "verificationNumber='" + verificationNumber + '\'' +
                '}';
    }
}
