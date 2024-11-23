package ru.egorch.ploblue.graphpng;

import android.widget.Toast;

import com.aspose.drawing.Bitmap;
import com.aspose.drawing.Brush;
import com.aspose.drawing.Color;
import com.aspose.drawing.Font;
import com.aspose.drawing.FontStyle;
import com.aspose.drawing.Graphics;
import com.aspose.drawing.Pen;
import com.aspose.drawing.SolidBrush;
import com.aspose.drawing.imaging.BitmapData;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;

import ru.egorch.ploblue.MainActivity;
import ru.egorch.ploblue.Saver;
import ru.egorch.ploblue.wave.WaveMap;
import ru.egorch.ploblue.wave.WavePoint;

public class GraphSaver extends Saver{

    public static boolean drawNSave(WaveMap wave, String pathParent, String pathChild, MainActivity main){
        try {
            //Prepare//////////////////////////////////////////
            //final int width = (int) ((wave.getLastTimeValue() - wave.getStartTime()) * 200) + 300;
            final int width = 2000;
            final int height = 5000;

            Bitmap graphWithPointsValues = generateBitmapWithPoints2(width, height, wave);
            createFileWithDirs(pathParent, pathChild, "png", main);
            String savePath = pathParent + "/" + pathChild + ".png";
            graphWithPointsValues.save(savePath);

            Toast.makeText(main, "GRAPH SAVED", Toast.LENGTH_SHORT).show();
            return true;
        } catch (Exception e){
            Toast.makeText(main, "FAIL CREATE GRAPH: " + e.getClass().getName() + " | " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }
    private static Bitmap generateBitmapWithPoints2(int width, int height, WaveMap wave){
        Bitmap bitmap = new Bitmap(width, height);
        Graphics graphics = Graphics.fromImage(bitmap);
        Pen penWave = new Pen(Color.getAqua(), 2);
        Pen penSignature = new Pen(Color.getCoral(), 2);
        //////////////////////////////////////////////////

        //Draw Signature Info
        Brush brush = new SolidBrush(Color.getAliceBlue());
        Font font = new Font("Arial", 10, FontStyle.Regular);

        String signatureInfo = "Val/Time";
        graphics.drawString(signatureInfo, font, brush, 10,  bitmap.getHeight() / 2);
        List<WavePoint> waveList = wave.getPoints();
        double maxValue = wave.getMaxValue();
        double min = wave.getMinValue();
        double maxTime = wave.getLastTimeValue();
        double startTime = wave.getStartTime();

        int startX = 200;
        double lastYDouble = wave.getStartValue() - min;
        int lastX = startX;
        int lastY = (int) (bitmap.getHeight() - (lastYDouble / maxValue * bitmap.getHeight() / 2));
        DecimalFormat decimalFormat = new DecimalFormat( "#.##" );
        int X, Y;
        for (WavePoint w : waveList) {
            //Draw Wave Line
            //X////////////////////////////
            double KX = (w.getTime() - startTime) / maxTime;
            X = (int) Math.round( (bitmap.getWidth() - startX) * KX * 10) + startX;
            //////////////////////////////

            //Y///////////////////////////Улетает вершина
            //double KY = (w.getValue() - min) / maxValue;
            double KY1 = (w.getValue() - min) / maxValue;
            double KY2 = (w.getValue() - lastYDouble) / maxValue;
            //Y = bitmap.getHeight() - (int) Math.round( (bitmap.getHeight() / 2) * KY ) - (int) Math.round( (w.getValue() / lastYDouble) * 10) - 40;
            Y = bitmap.getHeight() - (int) Math.round( (bitmap.getHeight() / 4) * KY1 + (bitmap.getHeight() / 4) * KY2 ) - 40;
            Y = Y < 0 ? 0 : Y;
            //Y = bitmap.getHeight() - (int) Math.round( (bitmap.getHeight() / 2) * KY ) - 40;
            //////////////////////////////

            graphics.drawLine(penWave, lastX, lastY, X, Y);
            //Накладываеться друг на друга
            if(X - lastX > 70){
                graphics.drawLine(penSignature, X, bitmap.getHeight() - 40, X, Y);
                String tempSignature = decimalFormat.format(w.getTime()) + "/" + decimalFormat.format(w.getValue());
                graphics.drawString(tempSignature, font, brush,  X,  bitmap.getHeight() - 20);
            }
            lastX = X;
            lastY = Y;
        }

        return bitmap;
    }

    private static Bitmap generateBitmapWithPoints(int width, int height, WaveMap wave){
        Bitmap bitmap = new Bitmap(width, height);
        Graphics graphics = Graphics.fromImage(bitmap);
        Pen penWave = new Pen(Color.getAqua(), 2);
        Pen penSignature = new Pen(Color.getCoral(), 2);
        //////////////////////////////////////////////////

        //Draw Signature Info
        Brush brush = new SolidBrush(Color.getAliceBlue());
        Font font = new Font("Arial", 10, FontStyle.Regular);

        String signatureInfo = "Val/Time";
        graphics.drawString(signatureInfo, font, brush, 10,  bitmap.getHeight() / 2);
        List<WavePoint> waveList = wave.getPoints();
        double max = wave.getMaxValue();
        double min = wave.getMinValue();
        double startTime = wave.getStartTime();

        int startX = 200;
        double lastXDouble = 1.0;
        double lastYDouble = wave.getStartValue() - min;
        int lastX = startX;
        int lastY = (int) (bitmap.getHeight() - (lastYDouble / max * bitmap.getHeight() / 2));
        DecimalFormat decimalFormat = new DecimalFormat( "#.##" );
        int X, Y;
        for (WavePoint w : waveList) {
            //Draw Wave Line
            //X////////////////////////////
            double tempX = w.getTime() - startTime;
            X = (int) Math.round(tempX) * 133 + (int) Math.round( (w.getTime() / lastXDouble) * 100);
            //////////////////////////////

            //Y///////////////////////////
            double K = (w.getValue() - min) / max;
            Y = bitmap.getHeight() / 3 - (int) Math.round( (bitmap.getHeight() / 2) * K ) - (int) Math.round( (w.getValue() / lastYDouble) * 10);
            //Y = bitmap.getHeight() / 3 - (int) Math.round( (bitmap.getHeight() / 2) * K );
            //////////////////////////////

            graphics.drawLine(penWave, lastX, lastY, X, Y);
            if(X - lastX > 10){
                graphics.drawLine(penSignature, X, bitmap.getHeight() - 40, X, Y);
                String tempSignature = decimalFormat.format(w.getTime()) + "/" + decimalFormat.format(w.getValue());
                graphics.drawString(tempSignature, font, brush,  X,  bitmap.getHeight() - 20);
            }
            lastX = X;
            lastY = Y;
        }

        return bitmap;
    }
}
