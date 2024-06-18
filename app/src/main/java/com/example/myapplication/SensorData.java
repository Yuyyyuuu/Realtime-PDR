package com.example.myapplication;

public class SensorData {
    public int sensorType;
    public double timestamp;
    public double x;
    public double y;
    public double z;

    public SensorData(int sensorType, double timestamp, double x, double y, double z) {
        this.sensorType = sensorType;
        this.timestamp = timestamp;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}