package org.camrdale.clock.thing.peripherals;

import android.os.CountDownTimer;
import android.util.Log;

import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay;
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DisplayManager {
    private static final String TAG = DisplayManager.class.getSimpleName();

    public enum DisplayMode {
        MONTH_DAY,
        HOUR_MINUTE,
        MINUTE_SECOND,
        VERIFICATION_CODE
    }

    private AlphanumericDisplay mDisplay;

    private CountDownTimer countDownTimer;

    private DisplayMode mDisplayMode = DisplayMode.HOUR_MINUTE;
    private String verificationCode;

    @Inject DisplayManager() {
        try {
            mDisplay = RainbowHat.openDisplay();
            mDisplay.setEnabled(true);
            mDisplay.clear();
            Log.d(TAG, "Initialized I2C Display");
        } catch (IOException e) {
            Log.e(TAG, "Error initializing display", e);
            Log.d(TAG, "Display disabled");
            mDisplay = null;
        }

        countDownTimer = new CountDownTimer(Long.MAX_VALUE, 1000) {
            // This is called after every 1 sec interval.
            public void onTick(long millisUntilFinished) {
                showCurrentTime();
            }

            public void onFinish() {
                start();
            }
        }.start();
    }

    public void setDisplayMode(DisplayMode mode) {
        this.mDisplayMode = mode;
        showCurrentTime();
    }

    public void setVerificationCodeDisplayMode(String verificationCode) {
        this.mDisplayMode = DisplayMode.VERIFICATION_CODE;
        this.verificationCode = verificationCode;
        showCurrentTime();
    }

    public void cleanup() {
        try {
            countDownTimer.cancel();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mDisplay != null) {
            try {
                mDisplay.clear();
                mDisplay.setEnabled(false);
                mDisplay.close();
            } catch (IOException e) {
                Log.e(TAG, "Error disabling display", e);
            } finally {
                mDisplay = null;
            }
        }
    }

    private void showCurrentTime() {
        LocalDateTime now = LocalDateTime.now(Clock.systemDefaultZone());
        switch(mDisplayMode) {
            case MONTH_DAY:
                updateDisplay(
                        String.format(Locale.US, "%2d.%02d",
                                now.get(ChronoField.MONTH_OF_YEAR),
                                now.get(ChronoField.DAY_OF_MONTH)));
                break;
            case HOUR_MINUTE:
                updateDisplay(
                        String.format(Locale.US, "%2d.%02d",
                                now.get(ChronoField.CLOCK_HOUR_OF_AMPM),
                                now.get(ChronoField.MINUTE_OF_HOUR)));
                break;
            case MINUTE_SECOND:
                updateDisplay(
                        String.format(Locale.US, "%2d.%02d",
                                now.get(ChronoField.MINUTE_OF_HOUR),
                                now.get(ChronoField.SECOND_OF_MINUTE)));
                break;
            case VERIFICATION_CODE:
                updateDisplay(verificationCode);
                break;
        }
    }

    private void updateDisplay(String value) {
        if (mDisplay != null) {
            try {
                mDisplay.display(value);
            } catch (IOException e) {
                Log.e(TAG, "Error setting display", e);
            }
        }
    }
}
