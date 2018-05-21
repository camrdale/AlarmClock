package org.camrdale.clock.shared.nearby;

import com.google.common.base.Objects;
import com.google.gson.annotations.SerializedName;

public class SignInRequest {
    @SerializedName("authentication_token")
    private String authenticationToken;

    public SignInRequest(String authenticationToken) {
        this.authenticationToken = authenticationToken;
    }

    public String getAuthenticationToken() {
        return authenticationToken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SignInRequest that = (SignInRequest) o;
        return Objects.equal(authenticationToken, that.authenticationToken);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(authenticationToken);
    }

    @Override
    public String toString() {
        return "SignInRequest{" +
                "authenticationToken='" + authenticationToken + '\'' +
                '}';
    }
}
