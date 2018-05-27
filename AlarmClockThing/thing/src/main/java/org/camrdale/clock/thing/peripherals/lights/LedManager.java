package org.camrdale.clock.thing.peripherals.lights;

import android.os.CountDownTimer;
import android.util.Log;

import com.google.android.things.contrib.driver.rainbowhat.RainbowHat;
import com.google.android.things.pio.Gpio;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LedManager {
    private static final String TAG = LedManager.class.getSimpleName();

    private Gpio mRedLed;
    private Gpio mGreenLed;
    private Gpio mBlueLed;
    private Apa102 ledStrip;
    private CountDownTimer countDownTimer;
    private int count = 0;

    @Inject LedManager() {
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
        try {
            ledStrip = new Apa102("SPI0.0", Apa102.Mode.BGR, Apa102.Direction.REVERSED);
            ledStrip.setBrightness(0);
            ledStrip.write(new int[7]);
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize the LED strip.", e);
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

    public boolean getLedStripStatus() {
        return ledStrip.getBrightness() > 0;
    }

    public void setLedStrip(boolean value) {
        if (value) {
            if (ledStrip.getBrightness() == 0) {
                ledStrip.setBrightness(31);
                if (count == 0) {
                    countDownTimer = new StarWarsLeds(ledStrip);
                } else {
                    countDownTimer = new KnightRiderLeds(ledStrip);
                }
                count = (count + 1) % 2;
                countDownTimer.start();
            } else {
                Log.w(TAG, "Led strip is already on");
            }
        } else {
            try {
                countDownTimer.cancel();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                ledStrip.setBrightness(0);
                ledStrip.write(new int[7]);
            } catch (IOException e) {
                Log.e(TAG, "Error turning off led strip", e);
            }
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
        try {
            countDownTimer.cancel();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (ledStrip != null) {
            try {
                ledStrip.setBrightness(0);
                ledStrip.write(new int[7]);
                ledStrip.close();
            } catch (IOException e) {
                Log.e(TAG, "Error disabling led strip", e);
            } finally {
                ledStrip = null;
            }
        }
    }
}
