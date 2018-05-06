package org.camrdale.clock.peripherals;

import android.util.Log;
import android.view.KeyEvent;

import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat;

import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ButtonManager {
    private static final String TAG = ButtonManager.class.getSimpleName();

    private ButtonInputDriver mButtonA;
    private ButtonInputDriver mButtonB;
    private ButtonInputDriver mButtonC;

    @Inject ButtonManager() {
        try {
            mButtonA = RainbowHat.createButtonAInputDriver(KeyEvent.KEYCODE_A);
            mButtonA.register();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize the button.", e);
        }
        try {
            mButtonB = RainbowHat.createButtonBInputDriver(KeyEvent.KEYCODE_B);
            mButtonB.register();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize the button.", e);
        }
        try {
            mButtonC = RainbowHat.createButtonCInputDriver(KeyEvent.KEYCODE_C);
            mButtonC.register();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize the button.", e);
        }
    }

    public void cleanup() {
        if (mButtonA != null) {
            try {
                mButtonA.close();
            } catch (IOException e) {
                Log.e(TAG, "Error disabling button", e);
            } finally {
                mButtonA = null;
            }
        }
        if (mButtonB != null) {
            try {
                mButtonB.close();
            } catch (IOException e) {
                Log.e(TAG, "Error disabling button", e);
            } finally {
                mButtonB = null;
            }
        }
        if (mButtonC != null) {
            try {
                mButtonC.close();
            } catch (IOException e) {
                Log.e(TAG, "Error disabling button", e);
            } finally {
                mButtonC = null;
            }
        }
    }
}
