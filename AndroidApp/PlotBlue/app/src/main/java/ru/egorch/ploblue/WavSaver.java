package ru.egorch.ploblue;

import java.util.List;

import java.io.*;

import ru.egorch.ploblue.wav.WavFile;
import ru.egorch.ploblue.wav.WavFileException;

public class WavSaver {

    public static WavFile save(List<Double> wave, double duration, String pathParent, String pathChild) throws IOException, WavFileException {
        int sampleRate = 44100;    // Samples per second

        long numFrames = (long) (duration * sampleRate);

        try {
            //File newHeuroSample = new File(pathParent);
            //newHeuroSample.mkdirs();
            //newHeuroSample = new File(pathParent, pathChild);
            File newHeuroSample = new File(pathParent, pathChild);
            newHeuroSample.createNewFile();

            WavFile wavFile = WavFile.newWavFile(newHeuroSample, 2, numFrames, 16, sampleRate);

            double[][] buffer = new double[2][wave.size()];

            // Initialise a local frame counter
            long frameCounter = 0;

            while (frameCounter < numFrames) {
                // Determine how many frames to write, up to a maximum of the buffer size
                long remaining = wavFile.getFramesRemaining();
                int toWrite = (remaining > wave.size()) ? wave.size() : (int) remaining;

                // Fill the buffer, one tone per channel
                int phase = wave.size() - toWrite;
                for (int s = phase; s < toWrite ; s++) {
                    buffer[0][s] = wave.get(s) * 10;
                    buffer[1][s] = wave.get(s) * 10;
                }

                // Write the buffer
                wavFile.writeFrames(buffer, toWrite);
            }

            wavFile.close();
            return wavFile;
        } catch (Exception e) {
            return null;
        }
    }
}
