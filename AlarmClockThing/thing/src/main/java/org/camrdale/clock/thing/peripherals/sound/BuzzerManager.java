package org.camrdale.clock.thing.peripherals.sound;

import android.util.Log;

import com.google.android.things.contrib.driver.pwmspeaker.Speaker;
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BuzzerManager {
    private static final String TAG = BuzzerManager.class.getSimpleName();

    private TaskBuzzer task;

    private Speaker buzzer;
    private boolean buzzerOn = false;
    private int count = 0;

    @Inject
    BuzzerManager() {
        try {
            buzzer = RainbowHat.openPiezo();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize the buzzer.", e);
        }
    }

    public boolean getStatus() {
        return buzzerOn;
    }

    public void setBuzzer(boolean value) {
        if (value) {
            if (buzzerOn) {
                Log.w(TAG, "Buzzer already on");
            } else {
                buzzerOn = true;
                if (count == 0) {
                    task = new StarWarsBuzzer();
                } else {
                    task = new KnightRiderBuzzer();
                }
                count = (count + 1) % 2;
                task.execute(buzzer);
            }
        } else {
            if (buzzerOn) {
                buzzerOn = false;
                if (task != null) {
                    task.cancel(true);
                }
                task = null;
                try {
                    buzzer.stop();
                } catch (IOException e) {
                    Log.e(TAG, "Error turning off buzzer", e);
                }
            } else {
                Log.w(TAG, "Buzzer already off");
            }
        }
    }

    public void cleanup() {
        if (buzzer != null) {
            try {
                buzzer.stop();
                buzzer.close();
            } catch (IOException e) {
                Log.e(TAG, "Error disabling buzzer", e);
            } finally {
                buzzer = null;
            }
        }
    }
}
