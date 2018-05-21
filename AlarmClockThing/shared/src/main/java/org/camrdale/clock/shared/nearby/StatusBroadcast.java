package org.camrdale.clock.shared.nearby;

import com.google.common.base.Objects;
import com.google.gson.annotations.SerializedName;

public class StatusBroadcast {
    @SerializedName("wifi_connected")
    private Boolean wifiConnected;

    @SerializedName("wifi_network_ssid")
    private String wifiNetworkSsid;

    @SerializedName("signed_in")
    private Boolean signedIn;

    @SerializedName("signed_in_user_email")
    private String signedInUserEmail;

    @SerializedName("signed_in_user_name")
    private String signedInUserName;

    @SerializedName("up_time_millis")
    private Long upTimeMillis;

    private Boolean registered;

    public StatusBroadcast(
            boolean wifiConnected,
            String wifiNetworkSsid,
            boolean signedIn,
            String signedInUserEmail,
            String signedInUserName,
            long upTimeMillis,
            boolean registered) {
        this.wifiConnected = wifiConnected;
        this.wifiNetworkSsid = wifiNetworkSsid;
        this.signedIn = signedIn;
        this.signedInUserEmail = signedInUserEmail;
        this.signedInUserName = signedInUserName;
        this.upTimeMillis = upTimeMillis;
        this.registered = registered;
    }

    public Boolean getWifiConnected() {
        return wifiConnected;
    }

    public String getWifiNetworkSsid() {
        return wifiNetworkSsid;
    }

    public Boolean getSignedIn() {
        return signedIn;
    }

    public String getSignedInUserEmail() {
        return signedInUserEmail;
    }

    public String getSignedInUserName() {
        return signedInUserName;
    }

    public Long getUpTimeMillis() {
        return upTimeMillis;
    }

    public Boolean getRegistered() {
        return registered;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StatusBroadcast that = (StatusBroadcast) o;
        return Objects.equal(wifiConnected, that.wifiConnected) &&
                Objects.equal(wifiNetworkSsid, that.wifiNetworkSsid) &&
                Objects.equal(signedIn, that.signedIn) &&
                Objects.equal(signedInUserEmail, that.signedInUserEmail) &&
                Objects.equal(signedInUserName, that.signedInUserName) &&
                Objects.equal(upTimeMillis, that.upTimeMillis) &&
                Objects.equal(registered, that.registered);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(wifiConnected, wifiNetworkSsid, signedIn, signedInUserEmail, signedInUserName, upTimeMillis, registered);
    }

    @Override
    public String toString() {
        return "StatusBroadcast{" +
                "wifiConnected=" + wifiConnected +
                ", wifiNetworkSsid='" + wifiNetworkSsid + '\'' +
                ", signedIn=" + signedIn +
                ", signedInUserEmail='" + signedInUserEmail + '\'' +
                ", signedInUserName='" + signedInUserName + '\'' +
                ", upTimeMillis=" + upTimeMillis +
                ", registered=" + registered +
                '}';
    }
}
