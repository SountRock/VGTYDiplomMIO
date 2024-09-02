package ru.egorch.ploblue.wave;

import java.util.ArrayList;
import java.util.List;

public class WaveMap{
    private List<WavePoint> waveList;
    private double lastTimeValue;
    private int size = 0;
    private double maxValue;
    private double minValue;

    public WaveMap() {
        waveList = new ArrayList<>();
        maxValue = 0.0;
        minValue = 0.0;
    }

    public boolean addRecord(double value, double time){
        if(time > lastTimeValue){
            WavePoint newPoint = new WavePoint(value, time);
            boolean isAdd = waveList.add(newPoint);
            if(isAdd){
                lastTimeValue = time;
                maxValue = value > maxValue ? value : maxValue;
                minValue = value < minValue ? value : minValue;
            }
            return isAdd;
        }

        return false;
    }

    public List<WavePoint> getPoints() {
        return waveList;
    }

    public void clear(){
        waveList.clear();
    }

    public int size(){
        return waveList.size();
    }

    public double getLastTimeValue(){
        return lastTimeValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public double getMinValue() {
        return minValue;
    }

    public double getStartTime(){
        try {
            return waveList.get(0).getTime();
        } catch (IndexOutOfBoundsException e){
            return 0.0;
        }
    }

    public double getStartValue(){
        try {
            return waveList.get(0).getValue();
        } catch (IndexOutOfBoundsException e){
            return 0.0;
        }
    }
}
