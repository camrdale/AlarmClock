package org.camrdale.clock.thing.peripherals.sound;

import android.os.AsyncTask;

import com.google.android.things.contrib.driver.pwmspeaker.Speaker;

import java.io.IOException;

abstract class TaskBuzzer extends AsyncTask<Speaker, Object, Object> {
    private static final String TAG = TaskBuzzer.class.getSimpleName();

    private Speaker buzzer;

    @Override
    protected Object doInBackground(Speaker[] speakers) {
        buzzer = speakers[0];
        while(true) {
            try {
                loop();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    void beep(int note, int duration) throws IOException, InterruptedException {
        buzzer.play(note);
        delay(duration);
        buzzer.stop();
        //delay(50);
    }

    void delay(int duration) throws InterruptedException {
        Thread.sleep(duration);
    }

    protected abstract void loop() throws IOException, InterruptedException;
}
