package com.example.myapplication;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;

public class DataCollectView extends LinearLayout {
    private TextView accelerometerData, gyroscopeData, magnetometerData;
    private LineChart accel_chart, gyro_chart, magn_chart;
    private TextView collectionTimeTextView;
    private ArrayList<Entry> accel_x, accel_y, accel_z, gyro_x, gyro_y, gyro_z, magn_x, magn_y, magn_z;

    public DataCollectView(Context context) {
        super(context);
    }
    public DataCollectView(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.data_collect_view, this);

        accel_chart = findViewById(R.id.accel_chart);
        gyro_chart = findViewById(R.id.gyro_chart);
        magn_chart = findViewById(R.id.magn_chart);
        accelerometerData = findViewById(R.id.accelerometerData);
        gyroscopeData = findViewById(R.id.gyroscopeData);
        magnetometerData = findViewById(R.id.magnetometerData);
        collectionTimeTextView = findViewById(R.id.collectionTime);

        initial();
    }

    private void initial()
    {
        TextView time_text=findViewById(R.id.collectionTime);
        time_text.setText("已采集时间: 0秒");
        TextView accel_text=findViewById(R.id.accelerometerData);
        accel_text.setText("加速度计数据：");
        TextView gyro_text=findViewById(R.id.gyroscopeData);
        gyro_text.setText("陀螺仪数据：");
        TextView magn_text=findViewById(R.id.magnetometerData);
        magn_text.setText("磁力计数据：");
        accel_x =new ArrayList<>();
        accel_x.add(new Entry(0,0));
        accel_y =new ArrayList<>();
        accel_y.add(new Entry(0,0));
        accel_z =new ArrayList<>();
        accel_z.add(new Entry(0,0));
        gyro_x =new ArrayList<>();
        gyro_x.add(new Entry(0,0));
        gyro_y =new ArrayList<>();
        gyro_y.add(new Entry(0,0));
        gyro_z =new ArrayList<>();
        gyro_z.add(new Entry(0,0));
        magn_x =new ArrayList<>();
        magn_x.add(new Entry(0,0));
        magn_y =new ArrayList<>();
        magn_y.add(new Entry(0,0));
        magn_z =new ArrayList<>();
        magn_z.add(new Entry(0,0));

        chartInitial();
    }

    private void chartInitial(){
        // Initialize each chart
        initializeLineChart(accel_chart);
        initializeLineChart(gyro_chart);
        initializeLineChart(magn_chart);
    }

    private static void initializeLineChart(@NonNull LineChart chart) {
        chart.setScaleEnabled(true);
        chart.getLegend().setForm(Legend.LegendForm.LINE);
        chart.getLegend().setXEntrySpace(12);
        ArrayList<Entry> entries1 = new ArrayList<>();
        ArrayList<Entry> entries2 = new ArrayList<>();
        ArrayList<Entry> entries3 = new ArrayList<>();
        entries1.add(new Entry(0,0));
        entries2.add(new Entry(0,0));
        entries3.add(new Entry(0,0));
        LineDataSet dataSet1 = new LineDataSet(entries1, "X");
        LineDataSet dataSet2 = new LineDataSet(entries2, "Y");
        LineDataSet dataSet3 = new LineDataSet(entries3, "Z");
        dataSet1.setColor(Color.parseColor("#03A9F4"));
        dataSet2.setColor(Color.parseColor("#EC4F44"));
        dataSet3.setColor(Color.parseColor("#FF9800"));
        dataSet1.setDrawValues(false);
        dataSet2.setDrawValues(false);
        dataSet3.setDrawValues(false);
        dataSet1.setDrawCircles(false);
        dataSet2.setDrawCircles(false);
        dataSet3.setDrawCircles(false);
        LineData lineData = new LineData(dataSet1, dataSet2, dataSet3);
        chart.setData(lineData);

        // Set background color
        chart.setBackgroundColor(Color.WHITE);

        // Enable drag
        chart.setDragEnabled(true);
        chart.getDescription().setEnabled(false);

        // Customize X axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.LTGRAY);
        //xAxis.setXOffset(0);

        // Customize Y axis
        YAxis leftYAxis = chart.getAxisLeft();
        leftYAxis.setDrawGridLines(true);
        leftYAxis.setGridColor(Color.LTGRAY);
        YAxis rightYAxis = chart.getAxisRight();
        rightYAxis.setEnabled(false); // Disable right Y axis
        chart.invalidate();
    }

    public static void updateChartData(ArrayList<Entry> x, ArrayList<Entry> y, ArrayList<Entry> z, @NonNull LineChart chart) {
        // Get the current LineData from the chart
        LineData lineData = chart.getLineData();

        if (lineData != null) {
            for(int i=0;i<x.size();i++)
            {
                lineData.addEntry(x.get(i),0);
                lineData.addEntry(y.get(i),1);
                lineData.addEntry(z.get(i),2);
            }
            LineDataSet dataSet1 = (LineDataSet) lineData.getDataSetByIndex(0);
            LineDataSet dataSet2 = (LineDataSet) lineData.getDataSetByIndex(1);
            LineDataSet dataSet3 = (LineDataSet) lineData.getDataSetByIndex(2);
            while (dataSet1.getEntryCount()>=1000)
            {
                lineData.removeEntry(0,0);
                lineData.removeEntry(0,1);
                lineData.removeEntry(0,2);
            }
            // Notify chart data has changed
            lineData.notifyDataChanged();
            chart.notifyDataSetChanged();

            // Move to the latest entry
            chart.invalidate();
        }
    }

    public void reset()
    {
        initial();
    }

    public void update(float time, float x, float y, float z, int type, String dataString)
    {
        boolean isupdate = true;
        int Size =0;
        switch (type) {
            case Sensor.TYPE_ACCELEROMETER:
                accel_x.add(new Entry(time, x));accel_y.add(new Entry(time, y));accel_z.add(new Entry(time, z));
                Size=(int)accel_chart.getLineData().getEntryCount()/300+1;
                if(accel_x.size()<Size)
                    //isupdate=false;
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyro_x.add(new Entry(time, x));gyro_y.add(new Entry(time, y));gyro_z.add(new Entry(time, z));
                Size=(int)gyro_chart.getLineData().getEntryCount()/300+20;
                if(gyro_x.size()<Size)
                    //isupdate=false;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magn_x.add(new Entry(time, x));magn_y.add(new Entry(time, y));magn_z.add(new Entry(time, z));
                Size=(int)magn_chart.getLineData().getEntryCount()/300+20;
                if(magn_x.size()<Size)
                    //isupdate=false;
                break;
            default:break;
        }

        // 根据传感器类型更新UI
        switch (type) {
            case Sensor.TYPE_ACCELEROMETER:
                accelerometerData.setText(dataString);
                if (isupdate)
                {
                    updateChartData(accel_x, accel_y, accel_z, accel_chart);
                    accel_x.clear();accel_y.clear();accel_z.clear();
                }
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyroscopeData.setText(dataString);
                if (isupdate)
                {
                    updateChartData(gyro_x, gyro_y, gyro_z, gyro_chart);
                    gyro_x.clear();gyro_y.clear();gyro_z.clear();
                }
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magnetometerData.setText(dataString);
                if (isupdate)
                {
                    updateChartData(magn_x, magn_y, magn_z, magn_chart);
                    magn_x.clear();magn_y.clear();magn_z.clear();
                }
                break;
        }
        // 更新已采集时间的TextView
        collectionTimeTextView.setText(String.format("已采集时间: %.3f秒", time));
    }

}
