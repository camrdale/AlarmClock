package org.camrdale.clock.web;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    @SerializedName("verification_number")
    private String verificationNumber;

    RegisterRequest(String verificationNumber) {
        this.verificationNumber = verificationNumber;
    }
}
