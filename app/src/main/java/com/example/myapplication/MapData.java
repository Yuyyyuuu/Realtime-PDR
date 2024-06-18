package com.example.myapplication;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.content.ContextWrapper;
import androidx.annotation.NonNull;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapData {
    private List<double[]> start_point;
    private List<double[]> path_point;
    private List<double[]> end_point;
    private int newpts;
    private boolean is_stop;
    private Context context;

    public MapData(Context context)
    {
        this.context=context;
        initial();
    }

    public MapData(Context context, String path, MapView map)
    {
        this.context=context;
        initial();
        load_file(path);
        InitialMap(map);
    }

    public MapData(Context context, MapView map)
    {
        this.context=context;
        initial();
        InitialMap(map);
    }

    private void initial(){
        start_point = new ArrayList<double[]>();
        path_point = new ArrayList<double[]>();
        end_point = new ArrayList<double[]>();
        start_point.add(new double[]{0,0});
        path_point.add(new double[]{0,0});
        end_point.add(new double[]{0,0});
        newpts=0;
        is_stop=false;
    }

    public void InitialMap(MapView map){
        map.addData(path_point,is_stop);
    }


    public void load_file(String path)
    {
        start_point.clear();
        path_point.clear();
        end_point.clear();
        File file = new File(context.getFilesDir(), path);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                // 将每一行分割成 x 和 y 值，并添加到路径点列表中
                String[] parts = line.split(",");
                float x = Float.parseFloat(parts[0]);
                float y = Float.parseFloat(parts[1]);
                path_point.add(new double[]{x,y});
            }
            reader.close();
            Log.d("readPathPointsFromFile", "Path points read from " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("readPathPointsFromFile", "Failed to read path points");
        }
        start_point.add(path_point.get(0));
        end_point.add(path_point.get(path_point.size()-1));
        newpts=path_point.size();
        if(end_point.get(0)[0]!=0||end_point.get(0)[1]!=0)
        {
            is_stop=true;
        }
    }

    public void save_file(String path) {
        if (path_point == null || path_point.isEmpty()) {
            Log.e("writePathPointsToFile", "Path points list is null or empty");
            return;
        }

        File directory = context.getFilesDir();
        File file = new File(directory, path);
        try {
            FileWriter writer = new FileWriter(file, false);
            for (double[] pos : path_point) {
                // 将每个 Entry 对象的 x 和 y 值写入文件，以逗号分隔
                writer.write(pos[0] + "," + pos[1] + "\n");
            }
            writer.flush();
            writer.close();
            Log.d("writePathPointsToFile", "Path points saved to " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("writePathPointsToFile", "Failed to save path points");
        }
    }

    public void load_map(@NonNull MapView map)
    {
        map.restart();
        InitialMap(map);
    }
    public void reset(MapView map)
    {
        start_point.clear();
        path_point.clear();
        end_point.clear();
        start_point.add(new double[]{0,0});
        path_point.add(new double[]{0,0});
        end_point.add(new double[]{0,0});
        is_stop=false;
        newpts=0;
        map.restart();
    }

    public void add_data(@NonNull List<double[]> pos)
    {
        newpts=pos.size();
        for(int i = 0;i<newpts;i++)
        {
            double[] p = pos.get(i);
            float x=(float) p[0];
            float y=(float) p[1];
            path_point.add(p);
        }
    }

    public void change_stop_flag()
    {
        is_stop=!is_stop;
    }

    public void invalid_map(@NonNull MapView map){
        List<double[]> new_pos=new ArrayList<>();
        for(int i=0;i<newpts;i++){
            new_pos.add(path_point.get(path_point.size()-newpts+i));
        }
        map.addData(new_pos,is_stop);
        map.invalidate();
    }
}
