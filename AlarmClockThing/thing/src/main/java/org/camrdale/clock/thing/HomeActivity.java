package org.camrdale.clock.thing;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;

import org.camrdale.clock.thing.nearby.NearbyManager;
import org.camrdale.clock.thing.peripherals.ButtonManager;
import org.camrdale.clock.thing.peripherals.DisplayManager;
import org.camrdale.clock.thing.peripherals.LedManager;
import org.camrdale.clock.thing.sounds.MediaManager;
import org.camrdale.clock.thing.state.StateManager;
import org.camrdale.clock.thing.web.WebManager;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

/**
 * Skeleton of an Android Things activity.
 */
public class HomeActivity extends Activity {
    private static final String TAG = HomeActivity.class.getSimpleName();

    @Inject DisplayManager displayManager;
    @Inject LedManager ledManager;
    @Inject ButtonManager buttonManager;
    @Inject StateManager stateManager;
    @Inject WebManager webManager;
    @Inject MediaManager mediaManager;
    @Inject NearbyManager nearbyManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_A) {
            ledManager.setRedLed(true);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_B) {
            ledManager.setGreenLed(true);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_C) {
            ledManager.setBlueLed(true);
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_A) {
            ledManager.setRedLed(false);
            nearbyManager.pairing();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_B) {
            ledManager.setGreenLed(false);
            stateManager.sleepKeyPress();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_C) {
            ledManager.setBlueLed(false);
            stateManager.snoozeKeyPress();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        nearbyManager.cleanup();
        webManager.cleanup();
        mediaManager.cleanup();
        stateManager.cleanup();
        displayManager.cleanup();
        ledManager.cleanup();
        buttonManager.cleanup();
    }
}
