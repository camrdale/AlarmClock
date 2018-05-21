package org.camrdale.clock.shared.nearby;

import com.google.common.base.Objects;
import com.google.gson.annotations.SerializedName;

public class WifiConnectionRequest {
    @SerializedName("network_ssid")
    private String networkSsid;

    @SerializedName("network_password")
    private String networkPassword;

    public WifiConnectionRequest(String networkSsid, String networkPassword) {
        this.networkSsid = networkSsid;
        this.networkPassword = networkPassword;
    }

    public String getNetworkSsid() {
        return networkSsid;
    }

    public String getNetworkPassword() {
        return networkPassword;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WifiConnectionRequest that = (WifiConnectionRequest) o;
        return Objects.equal(networkSsid, that.networkSsid) &&
                Objects.equal(networkPassword, that.networkPassword);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(networkSsid, networkPassword);
    }

    @Override
    public String toString() {
        return "WifiConnectionRequest{" +
                "networkSsid='" + networkSsid + '\'' +
                ", networkPassword='" + networkPassword + '\'' +
                '}';
    }
}
