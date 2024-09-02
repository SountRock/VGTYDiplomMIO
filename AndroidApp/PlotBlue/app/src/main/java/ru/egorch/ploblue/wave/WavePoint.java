package ru.egorch.ploblue.wave;

public class WavePoint{
    private final double value;
    private final double time;

    public WavePoint(double value, double time) {
        this.value = value;
        this.time = time;
    }

    public double getValue() {
        return value;
    }

    public double getTime() {
        return time;
    }
}
