package com.example.myapplication;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.hardware.SensorManager;
import android.util.Log;

import android.util.Log;
import java.util.List;

public class PDRProcessor {
    private static final String TAG = "PDRData"; // 定义日志标记

    private Configure CfgInfo;

    public PDRProcessor(Configure config) {
        this.CfgInfo = config;
    }

    public static class HeadingData {
        public final double timestamp;
        public final float headingAngle;
        public final float pitchAngle;
        public final float rollAngle;
        public HeadingData(double timestamp, float headingAngle,float pitchAngle,float rollAngle) {
            this.timestamp = timestamp;
            this.headingAngle = headingAngle;
            this.pitchAngle = pitchAngle;
            this.rollAngle = rollAngle;
        }
    }


    public List<double[]> processSensorData(List<String> sensorDataLines) {
        // 检查列表是否为空
        if (sensorDataLines == null || sensorDataLines.isEmpty()) {
            Log.w(TAG, "没有读取到传感器数据。"); // 使用警告级别的日志
            return null;
        }

        // 创建列表以存储不同传感器的数据
        List<SensorData> accelerometerData = new ArrayList<>();
        List<SensorData> gyroscopeData = new ArrayList<>();
        List<SensorData> magnetometerData = new ArrayList<>();

        // 解析每一行数据并将其存储在相应的列表中
        for (String dataLine : sensorDataLines) {
            String[] dataParts = dataLine.trim().split("\\s+");
            if (dataParts.length < 5) {
                Log.w(TAG, "数据格式错误: " + dataLine);
                continue;
            }
            try {
                int sensorType = Integer.parseInt(dataParts[0]);
                double timestamp = Double.parseDouble(dataParts[1]);
                // 此处调整手机轴系，xyz分别对应前右下
                double x = Double.parseDouble(dataParts[3]);
                double y = Double.parseDouble(dataParts[2]);
                double z = -Double.parseDouble(dataParts[4]);

                SensorData sensorData = new SensorData(sensorType, timestamp, x, y, z);
                switch (sensorType) {
                    case 1: // 加速度计数据
                        accelerometerData.add(sensorData);
                        break;
                    case 2: // 陀螺仪数据
                        gyroscopeData.add(sensorData);
                        break;
                    case 3: // 磁力计数据
                        magnetometerData.add(sensorData);
                        break;
                    default:
                        Log.w(TAG, "未知的传感器类型: " + sensorType);
                }
            } catch (NumberFormatException e) {
                Log.w(TAG, "解析错误: " + dataLine);
            }
        }

        // 创建同步后数据的列表
        List<SensorData> syncedAccelerometerData = new ArrayList<>();
        List<SensorData> syncedGyroscopeData = new ArrayList<>();
        List<SensorData> syncedMagnetometerData = new ArrayList<>();

        // 调用时间同步方法
        synchronizeSensorData(accelerometerData, gyroscopeData, magnetometerData,
                syncedAccelerometerData, syncedGyroscopeData, syncedMagnetometerData);

        List<SensorData> syncedAccelerometerDatafiltered = filterSensorData(syncedAccelerometerData, 3);
        List<SensorData> syncedGyroscopeDatafiltered = filterSensorData(syncedGyroscopeData, 3);
        List<SensorData> syncedMagnetometerfiltered = filterSensorData(syncedMagnetometerData, 3);

        // 使用陀螺仪数据和加速度计数据计算航向角和时间戳
        List<HeadingData> headingDataList = calculateOrientationUsingGyroscope(syncedGyroscopeDatafiltered,syncedAccelerometerDatafiltered);

        // 调用步伐探测方法
        List<Double> stepTimestamps = detectSteps(syncedAccelerometerDatafiltered,headingDataList ,9.8);

        // 调用航迹推算方法
        List<double[]> trajectory = calculateTrajectory(stepTimestamps, headingDataList, syncedAccelerometerDatafiltered);


        // 打印轨迹点到日志
        for (double[] position : trajectory) {
            double posX = position[0];
            double posY = position[1];
            Log.d(TAG, "X: " + posX + ", Y: " + posY);
        }
        // 返回计算出的二维轨迹坐标
        return trajectory;

    }

    /***** 轨迹推算的函数 *****/
    public List<double[]> calculateTrajectory(List<Double> stepsTimestamps, List<HeadingData> eulerAnglesData, List<SensorData> syncedAccelerometerData) {
        double posX = 0;
        double posY = 0;
        List<double[]> positions = new ArrayList<>();
        positions.add(new double[]{posX, posY}); // 存储每一步的位置

        double lastStepTime = stepsTimestamps.get(0); // 初始化上一次脚步的时间为第一次脚步的时间

        for (double stepTime : stepsTimestamps) {
            // 计算时间差
            double dT = stepTime - lastStepTime;

            // 更新上一次步骤的时间
            lastStepTime = stepTime;

            // 找到对应的航向角
            double headingAngle = findHeadingAngle(eulerAnglesData, stepTime);

            // 步长估计
            double stepLength = 0;
            double sf=(dT==0)?0:1/dT; // 步频
            switch(CfgInfo.step_length_mode){
                case 1:
                    stepLength=0.3;
                    break;
                case 2:
                    double h=CfgInfo.height;
                    double a=0.371;
                    double b=0.227;
                    double c=1;
                    stepLength=c*(0.7+a*(h-1.75)+b*(sf-1.79)*h/1.75);
                    break;
                case 3:
                    double acc_norm=0;
                    for(SensorData data : syncedAccelerometerData){
                        if (data.timestamp<=stepTime){
                            acc_norm=Math.sqrt(data.x*data.x+data.y*data.y+data.z*data.z);
                        }else{
                            break;
                        }
                    }
                    stepLength = 0.132*(acc_norm-9.8)+0.123*sf+0.225;
                    break;
                default:
                    throw new IllegalArgumentException("未知的步长估计模式");
            }




            // 更新位置
            double[] newPos = updatePosition(posX, posY, stepLength, headingAngle);
            posX = newPos[0];
            posY = newPos[1];

            // 将新位置添加到数组
            positions.add(new double[]{posX, posY});

        }

        return positions;
    }

    // 辅助方法：根据步伐时间戳找到对应的航向角
    private double findHeadingAngle(List<HeadingData> eulerAnglesData, double stepTime) {
        double headingAngle = 0;
        for (HeadingData data : eulerAnglesData) {
            if (data.timestamp <= stepTime) {
                headingAngle = data.headingAngle;
            } else {
                break;
            }
        }
        return headingAngle;
    }

    // 辅助方法：根据当前位置、步长和航向角更新位置
    private double[] updatePosition(double currentPosX, double currentPosY, double stepLength, double headingAngleRad) {
        double newX = currentPosX + stepLength * Math.cos(headingAngleRad);
        double newY = currentPosY + stepLength * Math.sin(headingAngleRad);
        return new double[]{newX, newY};
    }


    /***** 获取脚步发生历元的函数 *****/
    // 添加步伐探测方法
    public List<Double> detectSteps(List<SensorData> accelerometerData, List<HeadingData> headingDataList,double threshold) {
        List<Double> stepsDetected = new ArrayList<>();
        double lastStepTime = 0; // 初始化为0
        int mode= CfgInfo.step_detect_mode; // 获取步伐探测模式

        switch (mode) {
            case 1:
                // 使用加速度计模长法探测步伐
                for (int i = 1; i < accelerometerData.size() - 1; i++) {
                    double accelMagnitude = accelMagnitude(accelerometerData.get(i));
                    // 使用连续三个历元进行峰值探测
                    if (accelMagnitude > Math.max(accelMagnitude(accelerometerData.get(i - 1)),
                            accelMagnitude(accelerometerData.get(i + 1))) && accelMagnitude > threshold) {
                        double currentTime = accelerometerData.get(i).timestamp;
                        if (currentTime - lastStepTime > 0.5) { // 检查时间间隔是否大于0.5秒
                            stepsDetected.add(currentTime);
                            lastStepTime = currentTime;
                        }
                    }
                }
                break;
            case 2:
                // 使用垂向加速度法探测步伐
                for (int i = 0; i < accelerometerData.size(); i++) {
                    SensorData accelData = accelerometerData.get(i);
                    HeadingData eulerData = headingDataList.get(i);
                    double verticalAccel = calculateVerticalAccel(accelData, eulerData);

                    // 确认是否为步伐
                    if (Math.abs(verticalAccel) > threshold) {
                        double currentTime = accelData.timestamp;
                        if (currentTime - lastStepTime > 0.5) { // 时间间隔大于0.5秒
                            stepsDetected.add(currentTime);
                            lastStepTime = currentTime; // 更新最近一次步伐时间戳
                        }
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("未知的脚步探测模式");
        }

        return stepsDetected;

    }

    // 辅助方法：计算加速度模长
    private double accelMagnitude(SensorData data) {
        return Math.sqrt(Math.pow(data.x, 2) + Math.pow(data.y, 2) + Math.pow(data.z, 2));
    }

    // 辅助方法：计算垂直加速度
    private double calculateVerticalAccel(SensorData accelData, HeadingData eulerData) {

        double VerticalAccel=-Math.sin(eulerData.pitchAngle) * accelData.x + Math.sin(eulerData.rollAngle)*Math.cos(eulerData.pitchAngle) * accelData.y + Math.cos(eulerData.pitchAngle) *Math.cos(eulerData.rollAngle)* accelData.z;

        // 返回垂直方向的加速度
        return VerticalAccel;
    }

    /***** 获取航向角函数 *****/
    // 使用陀螺仪数据计算航向角
    private List<HeadingData> calculateOrientationUsingGyroscope(List<SensorData> syncedGyroscopeData,List<SensorData> syncedAccelerometerData) {

        int mode = CfgInfo.yaw_update_mode;

        // 计算初始的水平姿态角
        float[] initialOrientation = calculateInitialOrientation(syncedAccelerometerData);
        float roll0 = initialOrientation[0];
        float pitch0 = initialOrientation[1];
        float yaw0 = 0; // 航向角初始化为0

        // 初始化四元数
        float[] quaternion = new float[]{1, 0, 0, 0};
        quaternion = eulerToQuaternion(yaw0, pitch0, roll0);

        List<HeadingData> headingDataList = new ArrayList<>();

        float[] eInt= new float[]{0,0,0};
        // 遍历每个陀螺仪数据点
        for (int i = 0; i < syncedGyroscopeData.size(); i++) {
            SensorData Gyrodata = syncedGyroscopeData.get(i);
            SensorData Accdata=syncedAccelerometerData.get(i);
            double dT = (i == 0) ? 0 : Gyrodata.timestamp - syncedGyroscopeData.get(i - 1).timestamp;

            // AHRS六轴补偿陀螺原始输出
            float[] gyrodata = {(float) Gyrodata.x, (float) Gyrodata.y, (float) Gyrodata.z};
            float[] accdata  = {(float) Accdata.x, (float) Accdata.y, (float) Accdata.z};
            gyrodata = ahrs(quaternion, gyrodata, accdata,eInt, (float) dT);

            // 根据mode选择四元数更新方式
            switch (mode) {
                case 1: // 一阶龙格库塔
                    quaternion = updateQuaternionRK1(quaternion, gyrodata[0], gyrodata[1], gyrodata[2], (float) dT);
                    break;
                case 2: // 二阶龙格库塔
                    quaternion = updateQuaternionRK2(quaternion, gyrodata[0], gyrodata[1], gyrodata[2], (float) dT);
                    break;
                case 3: // 四阶龙格库塔
                    quaternion = updateQuaternionRK4(quaternion, gyrodata[0], gyrodata[1], gyrodata[2], (float) dT);
                    break;
                default:
                    Log.e(TAG, "未知的四元数更新模式");
                    return null;
            }

            // 计算航向角
            float headingAngle = calculateHeadingAngle(quaternion);
            // 计算俯仰角
            float pitchAngle = calculatePitchAngle(quaternion);
            // 计算横滚角
            float rollAngle = calculateRollAngle(quaternion);
            // 创建航向数据对象并添加到列表中
            headingDataList.add(new HeadingData(Gyrodata.timestamp, headingAngle,pitchAngle,rollAngle));
        }
        return headingDataList;
    }
    private float[] ahrs(float[] q, float[] gyroOutputOld, float[] accOutput, float[] eInt, float dT) {

        final float Kp = 1.0f; // 比例增益
        final float Ki = 0.005f; // 积分增益

        // 将四元数转换为旋转矩阵
        float[][] Cbn = quaternionToRotationMatrix(q);
        float[][] Cnb = {
                {Cbn[0][0], Cbn[1][0], Cbn[2][0]},
                {Cbn[0][1], Cbn[1][1], Cbn[2][1]},
                {Cbn[0][2], Cbn[1][2], Cbn[2][2]}
        };

        // 重力向量转换到机体坐标系
        float[] gravityVector = {0, 0, -1};
        float[] v = new float[3];
        for (int i = 0; i < 3; i++) {
            v[i] = 0; // 初始化结果向量的元素为0
            for (int j = 0; j < 3; j++) {
                v[i] += Cnb[i][j] * gravityVector[j];
            }
        }
        // 此时 v 就是重力向量转换到机体坐标系下的结果

        // 加速度计输出归一化
        float a_norm = (float) Math.sqrt(accOutput[0] * accOutput[0] +
                accOutput[1] * accOutput[1] +
                accOutput[2] * accOutput[2]);
        float ax = accOutput[0] / a_norm;
        float ay = accOutput[1] / a_norm;
        float az = accOutput[2] / a_norm;

        // 补偿向量
        float[] e = new float[3];
        e[0] = ay * v[2] - az * v[1];
        e[1] = az * v[0] - ax * v[2];
        e[2] = ax * v[1] - ay * v[0];

        // 记录补偿向量随时间的累积量
        eInt[0] += e[0] * dT;
        eInt[1] += e[1] * dT;
        eInt[2] += e[2] * dT;

        // 补偿陀螺输出
        float[] gyroOutputNew = new float[3];
        gyroOutputNew[0] = gyroOutputOld[0] + Kp * e[0] + Ki * eInt[0];
        gyroOutputNew[1] = gyroOutputOld[1] + Kp * e[1] + Ki * eInt[1];
        gyroOutputNew[2] = gyroOutputOld[2] + Kp * e[2] + Ki * eInt[2];

        return gyroOutputNew;
    }
    private float[][] quaternionToRotationMatrix(float[] q) {
        // 本函数转化所得为Cbn，即可将b系的坐标转化到n系中去
        // 提取四元数的各个分量
        float w = q[0];
        float x = q[1];
        float y = q[2];
        float z = q[3];

        // 计算旋转矩阵的各个元素
        float R11 = 1 - 2 * (y * y + z * z);
        float R12 = 2 * (x * y - z * w);
        float R13 = 2 * (x * z + y * w);

        float R21 = 2 * (x * y + z * w);
        float R22 = 1 - 2 * (x * x + z * z);
        float R23 = 2 * (y * z - x * w);

        float R31 = 2 * (x * z - y * w);
        float R32 = 2 * (y * z + x * w);
        float R33 = 1 - 2 * (x * x + y * y);

        // 构建旋转矩阵
        float[][] R = {
                {R11, R12, R13},
                {R21, R22, R23},
                {R31, R32, R33}
        };

        return R;
    }
    private float[] eulerToQuaternion(float yaw, float pitch, float roll) {
        float cy = (float) Math.cos(yaw / 2);
        float sy = (float) Math.sin(yaw / 2);
        float cp = (float) Math.cos(pitch / 2);
        float sp = (float) Math.sin(pitch / 2);
        float cr = (float) Math.cos(roll / 2);
        float sr = (float) Math.sin(roll / 2);

        float[] q = new float[4];
        q[0] = cy * cp * cr + sy * sp * sr; // q0
        q[1] = cy * cp * sr - sy * sp * cr; // q1
        q[2] = sy * cp * sr + cy * sp * cr; // q2
        q[3] = sy * cp * cr - cy * sp * sr; // q3

        return q;
    }
    // 计算初始水平姿态角即俯仰角和航向角
    private float[] calculateInitialOrientation(List<SensorData> syncedAccelerometerData) {
        // 初始姿态角列表
        List<Float> pitchList = new ArrayList<>();
        List<Float> rollList = new ArrayList<>();

        // 只计算前2秒的数据
        int sampleRate = 50; // 假定采样频率为50Hz
        int samplesToCalculate = 2 * sampleRate;

        // 遍历加速度计数据计算姿态角
        for (int i = 0; i < Math.min(samplesToCalculate, syncedAccelerometerData.size()); i++) {
            SensorData accData = syncedAccelerometerData.get(i);

            // 从加速度计数据中获取X, Y, Z值
            double accX = accData.x;
            double accY = accData.y;
            double accZ = accData.z;

            // 计算roll和pitch值
            float rollValue = (float) Math.atan2(-accY, -accZ);
            float pitchValue = (float) Math.atan2(accX, -accZ);
            // 将计算的值添加到列表中
            rollList.add(rollValue);
            pitchList.add(pitchValue);
        }

        // 计算平均的roll和pitch值
        float roll0 = average(rollList);
        float pitch0 = average(pitchList);

        return new float[]{roll0, pitch0};
    }
    // 辅助方法：计算列表中值的平均值
    private float average(List<Float> values) {
        float sum = 0;
        for (Float v : values) {
            sum += v;
        }
        return sum / values.size();
    }
    // 更新四元数的方法，使用一阶龙格-库塔方法（欧拉方法）
    private float[] updateQuaternionRK1(float[] q, double gx, double gy, double gz, float dT) {
        // 角速度为弧度/秒

        // 计算角速度向量的一半
        double halfGX = 0.5 * gx;
        double halfGY = 0.5 * gy;
        double halfGZ = 0.5 * gz;

        // K1 是四元数的导数
        float[] k1 = qDot(q, halfGX, halfGY, halfGZ);

        // 计算四元数更新值
        float[] qNew = new float[4];
        for (int i = 0; i < 4; i++) {
            qNew[i] = q[i] + k1[i] * dT;
        }

        // 四元数归一化
        float norm = (float)Math.sqrt(qNew[0] * qNew[0] + qNew[1] * qNew[1] + qNew[2] * qNew[2] + qNew[3] * qNew[3]);
        for (int i = 0; i < 4; i++) {
            qNew[i] /= norm;
        }

        return qNew;
    }
    // 更新四元数的方法，使用二阶龙格-库塔方法
    private float[] updateQuaternionRK2(float[] q, double gx, double gy, double gz, float dT) {
        // 角速度为弧度/秒

        // 计算角速度向量的一半
        double halfGX = 0.5 * gx;
        double halfGY = 0.5 * gy;
        double halfGZ = 0.5 * gz;

        // K1 是四元数的导数
        float[] k1 = qDot(q, halfGX, halfGY, halfGZ);

        // 计算中间值
        float[] q2 = new float[4];
        for (int i = 0; i < 4; i++) {
            q2[i] = q[i] + k1[i] * (dT / 2);
        }

        // K2 是中间值的导数
        float[] k2 = qDot(q2, halfGX, halfGY, halfGZ);

        // 综合 K1 和 K2 来得到最终的四元数更新值
        float[] qNew = new float[4];
        for (int i = 0; i < 4; i++) {
            qNew[i] = q[i] + (dT / 2) * (k1[i] + k2[i]);
        }

        // 四元数归一化
        float norm = (float)Math.sqrt(qNew[0] * qNew[0] + qNew[1] * qNew[1] + qNew[2] * qNew[2] + qNew[3] * qNew[3]);
        for (int i = 0; i < 4; i++) {
            qNew[i] /= norm;
        }

        return qNew;
    }
    // 更新四元数的方法，使用四阶龙格-库塔方法
    private float[] updateQuaternionRK4(float[] q, double gx, double gy, double gz, float dT) {
        // 角速度为弧度/秒

        // 计算角速度向量的一半
        double halfGX = 0.5 * gx;
        double halfGY = 0.5 * gy;
        double halfGZ = 0.5 * gz;

        // K1 是四元数的导数
        float[] k1 = qDot(q, halfGX, halfGY, halfGZ);

        // 计算中间值
        float[] q2 = new float[4];
        for (int i = 0; i < 4; i++) {
            q2[i] = q[i] + k1[i] * (dT / 2);
        }

        // K2 是中间值的导数
        float[] k2 = qDot(q2, halfGX, halfGY, halfGZ);

        // 计算另一个中间值
        float[] q3 = new float[4];
        for (int i = 0; i < 4; i++) {
            q3[i] = q[i] + k2[i] * (dT / 2);
        }

        // K3 是第二个中间值的导数
        float[] k3 = qDot(q3, halfGX, halfGY, halfGZ);

        // 计算终值
        float[] q4 = new float[4];
        for (int i = 0; i < 4; i++) {
            q4[i] = q[i] + k3[i] * dT;
        }

        // K4 是终值的导数
        float[] k4 = qDot(q4, halfGX, halfGY, halfGZ);

        // 综合 K1, K2, K3 和 K4 来得到最终的四元数更新值
        float[] qNew = new float[4];
        for (int i = 0; i < 4; i++) {
            qNew[i] = q[i] + (dT / 6) * (k1[i] + 2 * k2[i] + 2 * k3[i] + k4[i]);
        }

        // 四元数归一化
        float norm = (float)Math.sqrt(qNew[0] * qNew[0] + qNew[1] * qNew[1] + qNew[2] * qNew[2] + qNew[3] * qNew[3]);
        for (int i = 0; i < 4; i++) {
            qNew[i] /= norm;
        }

        return qNew;
    }
    //计算四元数的导数的方法
    float[] qDot(float[] quaternion, double hx, double hy, double hz) {
        return new float[]{
                (float)(-hx * quaternion[1] - hy * quaternion[2] - hz * quaternion[3]),
                (float)(hx * quaternion[0] + hz * quaternion[2] - hy * quaternion[3]),
                (float)(hy * quaternion[0] - hz * quaternion[1] + hx * quaternion[3]),
                (float)(hz * quaternion[0] + hy * quaternion[1] - hx * quaternion[2])
        };
    }
    // 计算航向角的方法
    private float calculateHeadingAngle(float[] quaternion) {
        double w=quaternion[0];
        double q1=quaternion[1];
        double q2=quaternion[2];
        double q3=quaternion[3];

        double yaw=Math.atan2(2*(q1*q2+q3*w),(1-2*(q2*q2+q3*q3)));

        return (float) yaw;
    }
    // 计算俯仰角的方法
    private float calculatePitchAngle(float[] quaternion) {
        double w=quaternion[0];
        double q1=quaternion[1];
        double q2=quaternion[2];
        double q3=quaternion[3];

        double t1=-2*(q1*q3-w*q2);
        double t2=Math.sqrt(4*(q2*q3+w*q1)*(q2*q3+w*q1)+(1-2*(q1*q1+q2*q2))*(1-2*(q1*q1+q2*q2)));
        double pitch=Math.atan2(t1,t2);

        return (float) pitch;
    }
    // 计算横滚角的方法
    private float calculateRollAngle(float[] quaternion) {
        double w=quaternion[0];
        double q1=quaternion[1];
        double q2=quaternion[2];
        double q3=quaternion[3];

        double roll=Math.atan2(2*(q2*q3+q1*w),(1-2*(q1*q1+q2*q2)));

        return (float) roll;
    }

    /***** 数据预处理--滤波降噪函数 *****/
    public List<SensorData> filterSensorData(List<SensorData> originalData, int windowSize) {
        if (originalData == null || originalData.isEmpty() || windowSize <= 0) {
            Log.w(TAG, "原始数据为空，或窗口大小无效。");
            return null;
        }

        int mode = CfgInfo.filter_mode;
        List<SensorData> filteredData = new ArrayList<>();

        // 根据mode选择滤波方式
        switch (mode) {
            case 1: // 均值滤波
                filteredData = meanFilter(originalData, windowSize);
                break;
            case 2: // 中值滤波
                filteredData = medianFilter(originalData, windowSize);
                break;
            case 3: // 去掉最大值和最小值后求均值
                filteredData = trimmedMeanFilter(originalData, windowSize);
                break;
            default:
                Log.e(TAG, "未知的滤波模式");
                return null;
        }

        return filteredData;
    }

    // 均值滤波
    private List<SensorData> meanFilter(List<SensorData> data, int windowSize) {
        List<SensorData> filteredData = new ArrayList<>();
        int dataSize = data.size();

        for (int i = 0; i < dataSize; i++) {
            double sumX = 0, sumY = 0, sumZ = 0;
            int count = 0;
            int start = Math.max(i - windowSize, 0);
            int end = Math.min(i + windowSize + 1, dataSize);

            for (int j = start; j < end; j++) {
                sumX += data.get(j).x;
                sumY += data.get(j).y;
                sumZ += data.get(j).z;
                count++;
            }

            double avgX = sumX / count;
            double avgY = sumY / count;
            double avgZ = sumZ / count;

            filteredData.add(new SensorData(data.get(i).sensorType, data.get(i).timestamp, avgX, avgY, avgZ));
        }

        return filteredData;
    }

    // 中值滤波
    private List<SensorData> medianFilter(List<SensorData> data, int windowSize) {
        List<SensorData> filteredData = new ArrayList<>();
        int dataSize = data.size();

        for (int i = 0; i < dataSize; i++) {
            List<Double> windowX = new ArrayList<>();
            List<Double> windowY = new ArrayList<>();
            List<Double> windowZ = new ArrayList<>();
            int start = Math.max(i - windowSize, 0);
            int end = Math.min(i + windowSize + 1, dataSize);

            for (int j = start; j < end; j++) {
                windowX.add(data.get(j).x);
                windowY.add(data.get(j).y);
                windowZ.add(data.get(j).z);
            }

            double medianX = getMedian(windowX);
            double medianY = getMedian(windowY);
            double medianZ = getMedian(windowZ);

            filteredData.add(new SensorData(data.get(i).sensorType, data.get(i).timestamp, medianX, medianY, medianZ));
        }

        return filteredData;
    }

    // 修剪均值滤波
    private List<SensorData> trimmedMeanFilter(List<SensorData> data, int windowSize) {
        List<SensorData> filteredData = new ArrayList<>();
        int dataSize = data.size();

        for (int i = 0; i < dataSize; i++) {
            List<Double> windowX = new ArrayList<>();
            List<Double> windowY = new ArrayList<>();
            List<Double> windowZ = new ArrayList<>();
            int start = Math.max(i - windowSize, 0);
            int end = Math.min(i + windowSize + 1, dataSize);

            for (int j = start; j < end; j++) {
                windowX.add(data.get(j).x);
                windowY.add(data.get(j).y);
                windowZ.add(data.get(j).z);
            }

            double trimmedMeanX = getTrimmedMean(windowX);
            double trimmedMeanY = getTrimmedMean(windowY);
            double trimmedMeanZ = getTrimmedMean(windowZ);

            filteredData.add(new SensorData(data.get(i).sensorType, data.get(i).timestamp, trimmedMeanX, trimmedMeanY, trimmedMeanZ));
        }

        return filteredData;
    }

    private double getMedian(List<Double> windowData) {
        Collections.sort(windowData);
        int middle = windowData.size() / 2;
        if (windowData.size() % 2 == 1) {
            return windowData.get(middle);
        } else {
            return (windowData.get(middle - 1) + windowData.get(middle)) / 2.0;
        }
    }

    private double getTrimmedMean(List<Double> windowData) {
        Collections.sort(windowData);
        if (windowData.size() > 2) {
            windowData.remove(windowData.size() - 1);
            windowData.remove(0);
        }
        double sum = 0;
        for (Double value : windowData) {
            sum += value;
        }
        return sum / windowData.size();
    }

    /*public List<SensorData> filterSensorData(List<SensorData> originalData, int windowSize) {
        if (originalData == null || originalData.isEmpty() || windowSize <= 0) {
            Log.w(TAG, "原始数据为空，或窗口大小无效。");
            return null;
        }

        int mode = CfgInfo.filter_mode;

        List<SensorData> filteredData = new ArrayList<>();
        int dataSize = originalData.size();

        // 遍历每个数据点
        for (int i = 0; i < dataSize; i++) {
            // 初始化累加器
            double sumX = 0, sumY = 0, sumZ = 0;
            int countX = 0, countY = 0, countZ = 0;

            // 确定窗口的范围
            int start = Math.max(i - windowSize, 0);
            int end = Math.min(i + windowSize + 1, dataSize);

            // 创建窗口内数据点的数组
            List<Double> windowX = new ArrayList<>();
            List<Double> windowY = new ArrayList<>();
            List<Double> windowZ = new ArrayList<>();

            // 将窗口内的数据点添加到列表
            for (int j = start; j < end; j++) {
                windowX.add(originalData.get(j).x);
                windowY.add(originalData.get(j).y);
                windowZ.add(originalData.get(j).z);
            }

            // 对每个轴排序并移除最大值和最小值
            Collections.sort(windowX);
            Collections.sort(windowY);
            Collections.sort(windowZ);

            if (windowX.size() > 2) {
                windowX.remove(windowX.size() - 1); // 移除最大值
                windowX.remove(0); // 移除最小值
            }
            if (windowY.size() > 2) {
                windowY.remove(windowY.size() - 1); // 移除最大值
                windowY.remove(0); // 移除最小值
            }
            if (windowZ.size() > 2) {
                windowZ.remove(windowZ.size() - 1); // 移除最大值
                windowZ.remove(0); // 移除最小值
            }

            // 计算平均值
            for (Double x : windowX) {
                sumX += x;
                countX++;
            }
            for (Double y : windowY) {
                sumY += y;
                countY++;
            }
            for (Double z : windowZ) {
                sumZ += z;
                countZ++;
            }

            // 如果计数大于0，则计算平均，否则保持原始值
            double avgX = (countX > 0) ? sumX / countX : originalData.get(i).x;
            double avgY = (countY > 0) ? sumY / countY : originalData.get(i).y;
            double avgZ = (countZ > 0) ? sumZ / countZ : originalData.get(i).z;

            // 添加到滤波后的数据列表
            filteredData.add(new SensorData(originalData.get(i).sensorType, originalData.get(i).timestamp, avgX, avgY, avgZ));
        }

        return filteredData;
    }
*/

    /***** 数据预处理--时间同步函数 *****/
    public void synchronizeSensorData(List<SensorData> accelerometerData,
                                      List<SensorData> gyroscopeData,
                                      List<SensorData> magnetometerData,
                                      List<SensorData> syncedAccelerometerData,
                                      List<SensorData> syncedGyroscopeData,
                                      List<SensorData> syncedMagnetometerData) {
        // 清空传入的同步列表
        syncedAccelerometerData.clear();
        syncedGyroscopeData.clear();
        syncedMagnetometerData.clear();

        // 遍历加速度计数据的时间戳
        for (SensorData accelData : accelerometerData) {
            double accelTimestamp = accelData.timestamp;

            // 对陀螺仪和磁力计数据进行线性插值
            SensorData gyroInterp = interpolateSensorData(gyroscopeData, accelTimestamp);
            SensorData magInterp = interpolateSensorData(magnetometerData, accelTimestamp);

            // 只有当两种插值都成功时，才将加速度计的数据点添加到同步列表中
            if (gyroInterp != null && magInterp != null) {
                syncedAccelerometerData.add(accelData);
                syncedGyroscopeData.add(gyroInterp);
                syncedMagnetometerData.add(magInterp);
            }
        }
    }

    private SensorData interpolateSensorData(List<SensorData> sensorDataList, double targetTimestamp) {
        SensorData before = null;
        SensorData after = null;

        // 寻找目标时间戳前后的数据点
        for (int i = 0; i < sensorDataList.size(); i++) {
            SensorData currentData = sensorDataList.get(i);
            if (currentData.timestamp <= targetTimestamp) {
                before = currentData;
            }
            if (currentData.timestamp > targetTimestamp) {
                after = currentData;
                break;
            }
        }

        // 如果找到了前后数据点，则进行线性插值
        if (before != null && after != null) {
            double ratio = (targetTimestamp - before.timestamp) / (after.timestamp - before.timestamp);
            double xInterp = before.x + ratio * (after.x - before.x);
            double yInterp = before.y + ratio * (after.y - before.y);
            double zInterp = before.z + ratio * (after.z - before.z);
            return new SensorData(before.sensorType, targetTimestamp, xInterp, yInterp, zInterp);
        }

        // 如果没有有效的前后数据点，返回null
        return null;
    }




}