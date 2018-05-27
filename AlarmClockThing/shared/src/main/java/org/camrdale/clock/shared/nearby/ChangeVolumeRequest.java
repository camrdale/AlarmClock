package org.camrdale.clock.shared.nearby;

import com.google.common.base.Objects;

public class ChangeVolumeRequest {
    private float volume;

    public ChangeVolumeRequest(float volume) {
        this.volume = volume;
    }

    public float getVolume() {
        return volume;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChangeVolumeRequest that = (ChangeVolumeRequest) o;
        return Float.compare(that.volume, volume) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(volume);
    }

    @Override
    public String toString() {
        return "ChangeVolumeRequest{" +
                "volume=" + volume +
                '}';
    }
}
