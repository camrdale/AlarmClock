package org.camrdale.clock.peripherals;

import android.util.Log;

import com.google.android.things.contrib.driver.rainbowhat.RainbowHat;
import com.google.android.things.pio.Gpio;

import java.io.IOException;

import javax.inject.Inject;

public class LedManager {
    private static final String TAG = LedManager.class.getSimpleName();

    private Gpio mRedLed;
    private Gpio mGreenLed;
    private Gpio mBlueLed;

    @Inject LedManager() {}

    public void initialize() {
        try {
            mRedLed = RainbowHat.openLedRed();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize the LED.", e);
        }
        try {
            mGreenLed = RainbowHat.openLedGreen();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize the LED.", e);
        }
        try {
            mBlueLed = RainbowHat.openLedBlue();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize the LED.", e);
        }
    }

    public void setRedLed(boolean value) {
        try {
            mRedLed.setValue(value);
        } catch (Exception e) {
            Log.e(TAG, "Failed to light the LED.", e);
        }
    }

    public void setBlueLed(boolean value) {
        try {
            mBlueLed.setValue(value);
        } catch (Exception e) {
            Log.e(TAG, "Failed to light the LED.", e);
        }
    }

    public void setGreenLed(boolean value) {
        try {
            mGreenLed.setValue(value);
        } catch (Exception e) {
            Log.e(TAG, "Failed to light the LED.", e);
        }
    }

    public void cleanup() {
        if (mRedLed != null) {
            try {
                mRedLed.setValue(false);
                mRedLed.close();
            } catch (IOException e) {
                Log.e(TAG, "Error disabling led", e);
            } finally {
                mRedLed = null;
            }
        }
        if (mGreenLed != null) {
            try {
                mGreenLed.setValue(false);
                mGreenLed.close();
            } catch (IOException e) {
                Log.e(TAG, "Error disabling led", e);
            } finally {
                mGreenLed = null;
            }
        }
        if (mBlueLed != null) {
            try {
                mBlueLed.setValue(false);
                mBlueLed.close();
            } catch (IOException e) {
                Log.e(TAG, "Error disabling led", e);
            } finally {
                mBlueLed = null;
            }
        }
    }
}
