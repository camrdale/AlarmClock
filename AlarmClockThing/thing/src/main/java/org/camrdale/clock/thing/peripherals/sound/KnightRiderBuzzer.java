package org.camrdale.clock.thing.peripherals.sound;

import java.io.IOException;

public class KnightRiderBuzzer extends TaskBuzzer {
    private static final String TAG = KnightRiderBuzzer.class.getSimpleName();

    private static final int NOTE_B0  = 31;
    private static final int NOTE_C1  = 33;
    private static final int NOTE_CS1 = 35;
    private static final int NOTE_D1  = 37;
    private static final int NOTE_DS1 = 39;
    private static final int NOTE_E1  = 41;
    private static final int NOTE_F1  = 44;
    private static final int NOTE_FS1 = 46;
    private static final int NOTE_G1  = 49;
    private static final int NOTE_GS1 = 52;
    private static final int NOTE_A1  = 55;
    private static final int NOTE_AS1 = 58;
    private static final int NOTE_B1  = 62;
    private static final int NOTE_C2  = 65;
    private static final int NOTE_CS2 = 69;
    private static final int NOTE_D2  = 73;
    private static final int NOTE_DS2 = 78;
    private static final int NOTE_E2  = 82;
    private static final int NOTE_F2  = 87;
    private static final int NOTE_FS2 = 93;
    private static final int NOTE_G2  = 98;
    private static final int NOTE_GS2 = 104;
    private static final int NOTE_A2  = 110;
    private static final int NOTE_AS2 = 117;
    private static final int NOTE_B2  = 123;
    private static final int NOTE_C3  = 131;
    private static final int NOTE_CS3 = 139;
    private static final int NOTE_D3  = 147;
    private static final int NOTE_DS3 = 156;
    private static final int NOTE_E3  = 165;
    private static final int NOTE_F3  = 175;
    private static final int NOTE_FS3 = 185;
    private static final int NOTE_G3  = 196;
    private static final int NOTE_GS3 = 208;
    private static final int NOTE_A3  = 220;
    private static final int NOTE_AS3 = 233;
    private static final int NOTE_B3  = 247;
    private static final int NOTE_C4  = 262;
    private static final int NOTE_CS4 = 277;
    private static final int NOTE_D4  = 294;
    private static final int NOTE_DS4 = 311;
    private static final int NOTE_E4  = 330;
    private static final int NOTE_F4  = 349;
    private static final int NOTE_FS4 = 370;
    private static final int NOTE_G4  = 392;
    private static final int NOTE_GS4 = 415;
    private static final int NOTE_A4  = 440;
    private static final int NOTE_AS4 = 466;
    private static final int NOTE_B4  = 494;
    private static final int NOTE_C5  = 523;
    private static final int NOTE_CS5 = 554;
    private static final int NOTE_D5  = 587;
    private static final int NOTE_DS5 = 622;
    private static final int NOTE_E5  = 659;
    private static final int NOTE_F5  = 698;
    private static final int NOTE_FS5 = 740;
    private static final int NOTE_G5  = 784;
    private static final int NOTE_GS5 = 831;
    private static final int NOTE_A5  = 880;
    private static final int NOTE_AS5 = 932;
    private static final int NOTE_B5  = 988;
    private static final int NOTE_C6  = 1047;
    private static final int NOTE_CS6 = 1109;
    private static final int NOTE_D6  = 1175;
    private static final int NOTE_DS6 = 1245;
    private static final int NOTE_E6  = 1319;
    private static final int NOTE_F6  = 1397;
    private static final int NOTE_FS6 = 1480;
    private static final int NOTE_G6  = 1568;
    private static final int NOTE_GS6 = 1661;
    private static final int NOTE_A6  = 1760;
    private static final int NOTE_AS6 = 1865;
    private static final int NOTE_B6  = 1976;
    private static final int NOTE_C7  = 2093;
    private static final int NOTE_CS7 = 2217;
    private static final int NOTE_D7  = 2349;
    private static final int NOTE_DS7 = 2489;
    private static final int NOTE_E7  = 2637;
    private static final int NOTE_F7  = 2794;
    private static final int NOTE_FS7 = 2960;
    private static final int NOTE_G7  = 3136;
    private static final int NOTE_GS7 = 3322;
    private static final int NOTE_A7  = 3520;
    private static final int NOTE_AS7 = 3729;
    private static final int NOTE_B7  = 3951;
    private static final int NOTE_C8  = 4186;
    private static final int NOTE_CS8 = 4435;
    private static final int NOTE_D8  = 4699;
    private static final int NOTE_DS8 = 4978;

    private static final int[][] aNotes = {
            // 1
            {NOTE_A4, 250}, {NOTE_AS4, 125}, {NOTE_A4, 125},
            {NOTE_A4, 125}, {NOTE_AS4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125},
            {NOTE_AS4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125},
            {NOTE_GS4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125},
            {NOTE_A4, 250}, {NOTE_AS4, 125}, {NOTE_A4, 125},
            {NOTE_A4, 125}, {NOTE_AS4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125},
            {NOTE_AS4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125},
            {NOTE_GS4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125},
            {NOTE_G4, 250}, {NOTE_GS4, 125}, {NOTE_G4, 125},
            {NOTE_G4, 125}, {NOTE_GS4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125},
            {NOTE_GS4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125},
            {NOTE_FS4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125},
            {NOTE_G4, 250}, {NOTE_GS4, 125}, {NOTE_G4, 125},
            {NOTE_G4, 125}, {NOTE_GS4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125},
            {NOTE_GS4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125},
            {NOTE_FS4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125},
            // 2
            {NOTE_A4, 250}, {NOTE_AS4, 125}, {NOTE_A4, 125},
            {NOTE_A4, 125}, {NOTE_AS4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125},
            {NOTE_AS4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125},
            {NOTE_GS4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125},
            {NOTE_A4, 250}, {NOTE_AS4, 125}, {NOTE_A4, 125},
            {NOTE_A4, 125}, {NOTE_AS4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125},
            {NOTE_AS4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125},
            {NOTE_GS4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125},
            {NOTE_G4, 250}, {NOTE_GS4, 125}, {NOTE_G4, 125},
            {NOTE_G4, 125}, {NOTE_GS4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125},
            {NOTE_GS4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125},
            {NOTE_FS4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125},
            {NOTE_G4, 250}, {NOTE_GS4, 125}, {NOTE_G4, 125},
            {NOTE_G4, 125}, {NOTE_GS4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125},
            {NOTE_GS4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125},
            {NOTE_FS4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125},
            // 3
            {NOTE_A4, 250}, {NOTE_AS4, 125}, {NOTE_A4, 125},
            {NOTE_A4, 125}, {NOTE_AS4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125},
            {NOTE_AS4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125},
            {NOTE_GS4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125},
            {NOTE_A4, 250}, {NOTE_AS4, 125}, {NOTE_A4, 125},
            {NOTE_A4, 125}, {NOTE_AS4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125},
            {NOTE_AS4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125},
            {NOTE_GS4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125}, {NOTE_A4, 125},
            {NOTE_G4, 250}, {NOTE_GS4, 125}, {NOTE_G4, 125},
            {NOTE_G4, 125}, {NOTE_GS4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125},
            {NOTE_GS4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125},
            {NOTE_FS4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125},
            {NOTE_G4, 250}, {NOTE_GS4, 125}, {NOTE_G4, 125},
            {NOTE_G4, 125}, {NOTE_GS4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125},
            {NOTE_GS4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125},
            {NOTE_FS4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125}, {NOTE_G4, 125},
            // solo
            {NOTE_A4, 250}, {NOTE_AS4, 125}, {NOTE_A4, 125}, {NOTE_E5, 1500},
            {NOTE_A5, 250}, {NOTE_AS5, 125}, {NOTE_A5, 125}, {NOTE_E5, 1500},
            {NOTE_A4, 250}, {NOTE_AS4, 125}, {NOTE_A4, 125}, {NOTE_E5, 250}, {NOTE_A5, 250}, {NOTE_G5, 2000},
            {NOTE_A4, 250}, {NOTE_AS4, 125}, {NOTE_A4, 125}, {NOTE_E5, 1500},
            {NOTE_A5, 250}, {NOTE_AS5, 125}, {NOTE_A5, 125}, {NOTE_E5, 1500},
            {NOTE_A4, 250}, {NOTE_AS4, 125}, {NOTE_A4, 125}, {NOTE_E5, 250}, {NOTE_A5, 250}, {NOTE_AS5, 2500}, {NOTE_G5, 250}, {NOTE_A5, 500}
    };

    @Override
    protected void loop() throws IOException, InterruptedException {
        for (int[] note : aNotes) {
            beep(note[0], note[1]);
        }

        delay(650);
    }
}
