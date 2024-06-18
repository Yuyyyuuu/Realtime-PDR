package com.example.myapplication;

import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.github.mikephil.charting.charts.LineChart;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.example.myapplication.Project_View;

public class DataCollectionActivity extends AppCompatActivity implements SensorEventListener {
    private final StringBuilder sensorDataBuilder = new StringBuilder();
    private SensorManager sensorManager;
    private Sensor accelerometer, gyroscope, magnetometer;
    private DataCollectView dataCollectView;
    private boolean isCollectingData = false;
    private long initialTimestamp = 0;
    private long initialTimestampfile = 0;
    private String currentSessionFileName;
    private MapView trajectoryView;
    private ViewPager viewPager;
    private ImageButton expandButton;
    private ImageButton saveButton;
    private LinearLayout Btn_container;
    private boolean isExpanded = false;
    private MapData mapData;
    String dataPath;
    String coverPath;
    private boolean isStarted;
    private boolean isStopped;
    private boolean isRealTime;
    private Configure CfgInfo;
    private RealtimeProcessor realtimeProcessor;

    private void processPDR(String filename) {
        List<double[]> trajectory = new ArrayList<>();
        if(!isStarted)
        {
            process_realtime_pdr();
            Toast.makeText(this, "开始实时解算！", Toast.LENGTH_SHORT).show();
        }
        else if(isStarted&&isStopped)
        {
            trajectory=process_file_pdr(filename);
            if (trajectory.size() != 0) {
                Toast.makeText(this, "解算成功！", Toast.LENGTH_SHORT).show();
                //drawMap(trajectory);
                mapData.add_data(trajectory);
                mapData.invalid_map(trajectoryView);
                file_invalid();
            }
            else {
                Toast.makeText(this, "解算失败！", Toast.LENGTH_SHORT).show();
            }
        }
        else return;

    }

    private List<double[]> process_file_pdr(String filename) {
        List<String> sensorDataLines = new ArrayList<>();
        // 获取外部存储的公共文档目录
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), filename);

        try {
            // 打开文件输入流
            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));

            String line;
            while ((line = reader.readLine()) != null) {
                sensorDataLines.add(line);
            }

            reader.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "读取数据文件失败！", Toast.LENGTH_SHORT).show();
            return new ArrayList<>();
        }
        // 创建PDR处理器实例并处理数据
        PDRProcessor pdrProcessor = new PDRProcessor(CfgInfo);
        return pdrProcessor.processSensorData(sensorDataLines);
    }

    private void process_realtime_pdr(){
        isRealTime=true;
        startDataCollection();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_collection);

        isStarted=false;
        isStopped=false;
        isRealTime=false;
        dataPath = getIntent().getStringExtra("data_path");
        coverPath = getIntent().getStringExtra("cover_path");
        CfgInfo=new Configure(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 初始化UI组件
        ImageButton startButton = findViewById(R.id.StartButton);
        ImageButton resetButton = findViewById(R.id.ResetButton);
        ImageButton stopButton = findViewById(R.id.StopButton);
        ImageButton settingButton = findViewById(R.id.SettingButton);

        dataCollectView = findViewById(R.id.data_collect_view);
        trajectoryView = findViewById(R.id.trajectoryView);
        //mapInitial();
        mapData=new MapData(this, dataPath, trajectoryView);
        viewPager = findViewById(R.id.viewpager);
        ArrayList<View> view_array = new ArrayList<>();
        view_array.add(dataCollectView);
        view_array.add(trajectoryView);
        MyPagerAdapter adapter = new MyPagerAdapter(view_array);
        viewPager = findViewById(R.id.viewpager);
        viewPager.setAdapter(adapter);

        // 获取传感器服务
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // 设置按钮监听器
        startButton.setOnClickListener(v -> startDataCollection());
        stopButton.setOnClickListener(v -> stopDataCollection());
        resetButton.setOnClickListener(v-> reset());
        settingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CfgInfo.show(getSupportFragmentManager(), "配置信息");
            }
        });
        // 事后PDR按钮
        ImageButton processPDRButton = findViewById(R.id.processPDRButton);
        processPDRButton.setOnClickListener(v -> processPDR(currentSessionFileName));

        expandButton = findViewById(R.id.expandButton);
        saveButton = findViewById(R.id.save_pic_btn);
        Btn_container = findViewById(R.id.buttonsContainer);
        expandButton.setOnClickListener(v -> Btn_move());
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showConfirmationDialog();
            }
        });

        // 注册广播接收器
        IntentFilter filter = new IntentFilter(Project_View.REFRESH_RESOURCE_ACTION);

        realtimeProcessor=new RealtimeProcessor();
    }

    private void showConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("保存");
        builder.setMessage("确认保存？");
        builder.setPositiveButton("保存", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 用户点击确认按钮时保存图片
                saveChartAsImage();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 用户点击取消按钮时关闭对话框
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    private void saveChartAsImage() {
        // 生成带时间戳的文件名
        String fileName = "chart_" + System.currentTimeMillis()+".png";
        // 获取外部存储的公共目录
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        // 创建一个名为 "MyApp" 的子目录
        File appDir = new File(storageDir, "MyApp");
        if (!appDir.exists()) {
            // 如果子目录不存在，则创建它
            appDir.mkdirs();
        }
        // 创建图片文件
        File file = new File(appDir, fileName);
        // 调用 saveToImage() 函数保存图片
        trajectoryView.saveToImage(file);
        // 通知图库有新图片添加
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
        // 提示用户图片保存成功
        Toast.makeText(this, "图片已保存至相册", Toast.LENGTH_SHORT).show();
    }

    private void startDataCollection() {
        isStarted=true;
        if (!isCollectingData) {
            // 注册传感器监听器
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
            // 为新的采集会话创建文件名
            initialTimestampfile = System.currentTimeMillis();
            currentSessionFileName = "SensorData_" + initialTimestampfile + ".txt";
            isCollectingData = true;
        }
    }

    private void stopDataCollection() {
        isStopped=true;
        if (isCollectingData) {
            isCollectingData=false;
            // 注销传感器监听器
            sensorManager.unregisterListener(this);
            // 将累积的数据写入文件
            FileHelper.writeToFile(this, currentSessionFileName, sensorDataBuilder.toString());
            // 清空StringBuilder以释放内存
            sensorDataBuilder.setLength(0);
            isCollectingData = false;
            initialTimestamp = 0;
            // 显示数据文件保存成功的消息提示
            Toast.makeText(this, "数据文件保存成功！", Toast.LENGTH_SHORT).show();
            mapData.change_stop_flag();
            if(isRealTime){
                trajectoryView.addend();
                file_invalid();
                isRealTime=false;
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isCollectingData) {
            // 如果是第一次收集数据，设置初始时间戳
            if (initialTimestamp == 0) {
                initialTimestamp = event.timestamp;
            }

            // 计算相对时间戳（单位：秒）
            float relativeTimestamp = (event.timestamp - initialTimestamp) / 1_000_000_000.0f;

            // 将传感器数据写入文件
            String sensorDataLine = formatSensorDataLine(event.sensor.getType(), relativeTimestamp, event.values[0], event.values[1], event.values[2]);
            // 将传感器数据添加到StringBuilder
            sensorDataBuilder.append(sensorDataLine).append("\n");

            // 格式化传感器数据字符串
            String dataString = String.format("%s:\n %.4f %.4f %.4f",
                    getSensorString(event.sensor.getType()),
                    event.values[0], event.values[1], event.values[2]);

            dataCollectView.update(relativeTimestamp, event.values[0], event.values[1], event.values[2], event.sensor.getType(), dataString);

            // 可以在这里传入数据，来实现实时PDR，
            if(isRealTime){
                List<double[]>trajectory =realtimeProcessor.processRealTime(event);
                if(!trajectory.isEmpty()){
                    mapData.add_data(trajectory);
                    mapData.invalid_map(trajectoryView);
                }
            }
        }
    }

    private String getSensorString(int sensorType) {
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                return "acc(m/s2)";
            case Sensor.TYPE_GYROSCOPE:
                return "gyro(rad/s)";
            case Sensor.TYPE_MAGNETIC_FIELD:
                return "mag(uT)";
            default:
                return "";
        }
    }

    // 格式化传感器数据行的方法
    private String formatSensorDataLine(int sensorType, float timestamp, float x, float y, float z) {
        int sensorIdentifier;
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                sensorIdentifier = 1;
                break;
            case Sensor.TYPE_GYROSCOPE:
                sensorIdentifier = 2;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                sensorIdentifier = 3;
                break;
            default:
                sensorIdentifier = -1; // Unknown sensor type
        }
        return String.format("%d %f %f %f %f", sensorIdentifier, timestamp, x, y, z);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 在这里处理传感器精度变化
    }

    private void Btn_move() {
        if (isExpanded) {
            // 向上移动 100dp
            animateLayout(Btn_container, -100);
            expandButton.setImageResource(R.drawable.ic_expand_down);
        } else {
            // 向下移动 100dp
            animateLayout(Btn_container, 100);
            expandButton.setImageResource(R.drawable.ic_expand_up);
        }

        // 切换状态
        isExpanded = !isExpanded;
    }

    private void animateLayout(@NonNull final View view, float targetY) {
        float density = getResources().getDisplayMetrics().density;
        float targetYDp = targetY * density;

        // 获取视图的初始位置
        final float startY = view.getY();
        final float endY = startY + targetYDp;

        // 创建一个 ValueAnimator 对象，用于执行动画
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);

        animator.setDuration(300); // 设置动画时长为 600 毫秒
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                // 获取动画的当前进度（取值范围 0~1）
                float progress = (float) animation.getAnimatedValue();
                // 计算当前位置
                float currentY = startY + progress * (endY - startY);
                // 更新视图的位置
                view.setY(currentY);
            }
        });
        // 启动动画
        animator.start();
    }

    private void reset(){
        isStarted=false;
        isStopped=false;
        isCollectingData=false;
        mapData.reset(trajectoryView);
        dataCollectView.reset();
        realtimeProcessor.reset();
        file_invalid();
    }

    private void file_invalid()
    {
        mapData.save_file(dataPath);
        saveChartPNG(trajectoryView);
    }

    private void saveChartPNG(MapView mapView) {
        // 将 Bitmap 保存为 PNG 格式的文件到应用的内部文件目录中
        File directory = getFilesDir(); // 获取应用的内部文件目录
        File file = new File(directory, coverPath);
        mapView.saveToImage(file);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 发送广播通知刷新资源文件
        Intent intent = new Intent(Project_View.REFRESH_RESOURCE_ACTION);
        sendBroadcast(intent);
    }


}