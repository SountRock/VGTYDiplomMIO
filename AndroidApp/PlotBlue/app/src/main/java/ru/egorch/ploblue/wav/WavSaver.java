package ru.egorch.ploblue.wav;

import android.widget.Toast;

import java.util.List;

import java.io.*;

import ru.egorch.ploblue.MainActivity;
import ru.egorch.ploblue.Saver;
import ru.egorch.ploblue.wav.WavFile;
import ru.egorch.ploblue.wav.WavFileException;
import ru.egorch.ploblue.wave.WavePoint;

public class WavSaver extends Saver{
    public static boolean save(List<WavePoint> wave, double duration, int sampleRate, String pathParent, String pathChild, MainActivity main) {
        try {
            long numFrames = (long) (duration * sampleRate);

            //////////////////////////////////////////////
            double heightKoeff = Math.pow(10, Math.round(findMaxDepth(wave) / 2));
            //////////////////////////////////////////////
            File newSample = createFileWithDirs(pathParent, pathChild + "(" + heightKoeff + ")", "wav", main);
            //////////////////////////////////////////////

            WavFile wavFile = WavFile.newWavFile(newSample, 2, numFrames, 32, sampleRate);

            double[][] buffer = new double[2][wave.size()];

            // Initialise a local frame counter
            long frameCounter = 0;

            while (frameCounter < numFrames) {
                // Determine how many frames to write, up to a maximum of the buffer size
                long remaining = wavFile.getFramesRemaining();
                int toWrite = (remaining > wave.size()) ? wave.size() : (int) remaining;

                // Fill the buffer, one tone per channel
                for (int s = 0; s < toWrite ; s++, frameCounter++) {
                    buffer[0][s] = wave.get(s).getValue() / heightKoeff;
                    buffer[1][s] = wave.get(s).getValue() / heightKoeff;
                }

                // Write the buffer
                wavFile.writeFrames(buffer, toWrite);
            }

            wavFile.close();
            return true;
        } catch (Exception e){
            Toast.makeText(main, "FAIL FILE CREATED: " + e.getClass().getName() + " | " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}
