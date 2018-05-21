package org.camrdale.clock.shared.nearby;

import com.google.common.base.Objects;
import com.google.gson.annotations.SerializedName;

public class FactoryResetResponse {
    private Boolean success;

    @SerializedName("failure_reason")
    private String failureReason;

    public FactoryResetResponse(boolean success, String failureReason) {
        this.success = success;
        this.failureReason = failureReason;
    }

    public Boolean getSuccess() {
        return success;
    }

    public String getFailureReason() {
        return failureReason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FactoryResetResponse that = (FactoryResetResponse) o;
        return Objects.equal(success, that.success) &&
                Objects.equal(failureReason, that.failureReason);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(success, failureReason);
    }

    @Override
    public String toString() {
        return "FactoryResetResponse{" +
                "success=" + success +
                ", failureReason='" + failureReason + '\'' +
                '}';
    }
}
