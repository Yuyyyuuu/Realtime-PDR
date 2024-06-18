package com.example.myapplication;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class RealtimeProcessor {
    // 航向角
    private double Yaw;
    // 是否探测到脚步
    private boolean isStep;
    // 步长
    private double Step_length;
    // 上一次更新坐标
    private double[] Pos;
    private int step=0;
    private static final long MIN_STEP_INTERVAL = 350;
    private static long lastStepTime = 0;
    private static long currenStepTime=0;
    private static long lastStepTimeDiff=0;
    private static long currentStepTimeDiff=0;

    public void reset(){
        Yaw=0.0;
        isStep = false;
        Step_length=0.75;
        Pos=new double[]{0,0};
    }

    public RealtimeProcessor(){
        Yaw=0.0;
        isStep = false;
        Step_length=0.75;
        Pos=new double[]{0,0};
    }

    private void processGyro(SensorEvent event){
        // 添加代码
        float[] Gyro = new float[3];
        Gyro[0] = (float) event.values[0];
        Gyro[1] = (float) event.values[1];
        Gyro[2] = (float) event.values[2];
        float NS2S = 1.0f / 1000000000.0f;
        float[] gyroOrientation = new float[4]; // 四元数表示的姿态角
        float dt=0.02f;
        float k1 = dt * Gyro[2];
        float k2 = dt * (Gyro[2] + 0.5f * k1);
        float k3 = dt * (Gyro[2] + 0.5f * k2);
        float k4 = dt * (Gyro[2] + k3);
        Yaw +=(k1+2*k2+2*k3+k4) /6.0f;
    }
    private void normalizeQuaternion(float[] q) {
        float norm = (float)Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]);
        for (int i = 0; i < 4; i++) {
            q[i] /= norm;
        }
    }
    private static float convertRadiansToDegrees(float radians) {
        float degrees = (float) Math.toDegrees(radians);
        while (degrees > 180) {
            degrees -= 360;
        }
        while (degrees < -180) {
            degrees += 360;
        }
        return degrees;
    }
    private void processAccel(SensorEvent event){
        // 添加代码
        double accelerationMagnitude = Math.sqrt(event.values[0]*event.values[0]+event.values[1]*event.values[1]+event.values[2]*event.values[2]);

        if (accelerationMagnitude > 10.8)
        {
            onNewStepDetected(event);

        }
    }
    private double detect_step_len(){
        double Sf=1.0f/(currentStepTimeDiff*0.001);
        return 0.7f+0.371f*(1.8f-1.6f)+0.227f*(Sf-1.79f)*1.8f/1.6f;
    }
    public void onNewStepDetected(SensorEvent event) {
        float distanceStep = 0.8f;
        currenStepTime = event.timestamp / 1000000; // 获取当前时间
        if(lastStepTime<1e-6)
            lastStepTime=currenStepTime;
        long timeDifference = currenStepTime - lastStepTime; // 计算时间差，本次脚步间隔
        if (timeDifference >= MIN_STEP_INTERVAL) {
            Log.d("时间差： " + timeDifference," 当前时间："+currenStepTime);
            isStep=true;
            step++;
            lastStepTime = currenStepTime; // 更新上一步的时间
            currentStepTimeDiff=timeDifference;
        }
    }



    private void invalid_pos(List<double[]> pos){
        // 添加代码
        double step_length=detect_step_len();
        double y=Pos[0]+step_length*Math.sin(Yaw);
        double x=Pos[1]+step_length*Math.cos(Yaw);
        Log.d("Pos invalid","x: "+x+"    y: "+y);
        pos.add(new double[]{x,-y});
        Pos[0]=y;
        Pos[1]=x;
        isStep=false;
    }

    public List<double[]> processRealTime(SensorEvent event){
        List<double[]> pos=new ArrayList<>();
        List<double[]> output=new ArrayList<>();
        switch (event.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                processAccel(event);break;
            case Sensor.TYPE_GYROSCOPE:
                processGyro(event);break;
            default:break;
        }

        if(isStep){
            invalid_pos(pos);

            Log.d("Step detected", " Yaw: " + Yaw*180/Math.PI +" 当前时间："+currenStepTime+
                    " Step: " + step +
                    " Position: [" + Pos[0] + ", " + Pos[1] + "]");
        }

        return pos;
    }

}
