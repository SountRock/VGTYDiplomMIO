package ru.egorch.ploblue;

import android.widget.Toast;

import java.util.List;

import java.io.*;

import ru.egorch.ploblue.wav.WavFile;
import ru.egorch.ploblue.wav.WavFileException;

public class WavSaver {

    public static WavFile save(List<Double> wave, double duration, String pathParent, String pathChild, MainActivity main) throws IOException, WavFileException {
        int sampleRate = 192000;    // Samples per second 44100

        long numFrames = (long) (duration * sampleRate);

        //////////////////////////////////////////////
        File newSample = new File(pathParent);
        boolean isCreate = newSample.mkdirs();
        if(!isCreate && !newSample.exists()) {
            Toast.makeText(main, "FAIL DIRS CREATED!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(main, "DIRS CREATED", Toast.LENGTH_SHORT).show();
        }

        newSample = new File(pathParent, pathChild);
        isCreate = newSample.createNewFile();
        if(!isCreate){
            Toast.makeText(main, "FAIL FILE CREATED!", Toast.LENGTH_SHORT).show();
            return null;
        }
        Toast.makeText(main, "FILE CREATED", Toast.LENGTH_SHORT).show();
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
                buffer[0][s] = wave.get(s) / 1000.0;
                buffer[1][s] = wave.get(s) / 1000.0;
            }

            // Write the buffer
            wavFile.writeFrames(buffer, toWrite);
        }

        wavFile.close();
        return wavFile;
    }
}
