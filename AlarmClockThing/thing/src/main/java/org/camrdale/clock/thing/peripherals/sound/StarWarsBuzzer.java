package org.camrdale.clock.thing.peripherals.sound;

import java.io.IOException;

public class StarWarsBuzzer extends TaskBuzzer {
    private static final String TAG = StarWarsBuzzer.class.getSimpleName();

    private static final int c = 261;
    private static final int d = 294;
    private static final int e = 329;
    private static final int f = 349;
    private static final int g = 391;
    private static final int gS = 415;
    private static final int a = 440;
    private static final int aS = 455;
    private static final int b = 466;
    private static final int cH = 523;
    private static final int cSH = 554;
    private static final int dH = 587;
    private static final int dSH = 622;
    private static final int eH = 659;
    private static final int fH = 698;
    private static final int fSH = 740;
    private static final int gH = 784;
    private static final int gSH = 830;
    private static final int aH = 880;

    @Override
    protected void loop() throws IOException, InterruptedException {
        //Play first section
        firstSection();

        //Play second section
        secondSection();

        //Variant 1
        beep(f, 250);
        beep(gS, 500);
        beep(f, 350);
        beep(a, 125);
        beep(cH, 500);
        beep(a, 375);
        beep(cH, 125);
        beep(eH, 650);

        delay(500);

        //Repeat second section
        secondSection();

        //Variant 2
        beep(f, 250);
        beep(gS, 500);
        beep(f, 375);
        beep(cH, 125);
        beep(a, 500);
        beep(f, 375);
        beep(cH, 125);
        beep(a, 650);

        delay(650);
    }

    private void firstSection() throws IOException, InterruptedException {
        beep(a, 500);
        beep(a, 500);
        beep(a, 500);
        beep(f, 350);
        beep(cH, 150);
        beep(a, 500);
        beep(f, 350);
        beep(cH, 150);
        beep(a, 650);

        delay(500);

        beep(eH, 500);
        beep(eH, 500);
        beep(eH, 500);
        beep(fH, 350);
        beep(cH, 150);
        beep(gS, 500);
        beep(f, 350);
        beep(cH, 150);
        beep(a, 650);

        delay(500);
    }

    private void secondSection() throws IOException, InterruptedException {
        beep(aH, 500);
        beep(a, 300);
        beep(a, 150);
        beep(aH, 500);
        beep(gSH, 325);
        beep(gH, 175);
        beep(fSH, 125);
        beep(fH, 125);
        beep(fSH, 250);

        delay(325);

        beep(aS, 250);
        beep(dSH, 500);
        beep(dH, 325);
        beep(cSH, 175);
        beep(cH, 125);
        beep(b, 125);
        beep(cH, 250);

        delay(350);
    }
}
