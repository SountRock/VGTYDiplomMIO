package ru.egorch.ploblue;

import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.List;

import ru.egorch.ploblue.wave.WavePoint;

public class Saver {
    public static int  findMaxDepth(List<WavePoint> wave){
        try {
            double max =  wave.get(0).getValue();
            for (WavePoint w : wave) {
                max = w.getValue() > max ? w.getValue() : max;
            }
            String numStr = Double.toString(Math.round(max));

            return numStr.length();
        } catch (IndexOutOfBoundsException e){
            return 3;
        }
    }

    public static File createFileWithDirs(String pathParent, String pathChild, String formatFile, MainActivity main) throws IOException {
        File newSample = new File(pathParent);
        boolean isCreate = newSample.mkdirs();
        if(!isCreate && !newSample.exists()) {
            Toast.makeText(main, "FAIL DIRS CREATED!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(main, "DIRS CREATED", Toast.LENGTH_SHORT).show();
        }

        String fileName = pathChild + "." + formatFile;
        newSample = new File(pathParent, fileName);
        isCreate = newSample.createNewFile();
        if(!isCreate){
            Toast.makeText(main, "FAIL " + fileName + " CREATED!" , Toast.LENGTH_SHORT).show();
            throw new IOException();
        }
        Toast.makeText(main, "FILE CREATED " + fileName, Toast.LENGTH_SHORT).show();
        return newSample;
    }
}
