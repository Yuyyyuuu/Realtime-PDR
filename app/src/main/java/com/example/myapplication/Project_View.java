package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.text.style.LeadingMarginSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.view.Gravity;
import android.view.MotionEvent;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatTextView;

import com.github.mikephil.charting.data.Entry;

import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Project_View extends AppCompatTextView {
    private String project_Name;
    private String data_Path;
    private String file_Path;
    private String cover_Path;
    private OnDeleteListener onDeleteListener;
    private OnClickListener onClickListener;
    public static final String REFRESH_RESOURCE_ACTION = "com.example.myapplication.REFRESH_RESOURCE_ACTION";

    private BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(REFRESH_RESOURCE_ACTION)) {
                // 刷新资源文件的操作
                refreshResourceFiles();
            }
        }
    };

    private void refreshResourceFiles() {
        // 获取内部目录下的 cover_path 对应的文件路径
        String coverPath = getContext().getFilesDir() + "/" + cover_Path;
        File coverFile = new File(coverPath);
        Drawable drawable = Drawable.createFromPath(coverFile.getAbsolutePath());
        if (drawable != null) {
            // 调整图片大小
            int newWidth = (int) (drawable.getIntrinsicWidth() * (float) getResources().getDimensionPixelSize(R.dimen.project_view_height) / drawable.getIntrinsicHeight());
            drawable.setBounds(0, 0, newWidth, getResources().getDimensionPixelSize(R.dimen.project_view_height));

            // 创建 SpannableString
            SpannableString spannableString = new SpannableString("   " + project_Name);
            // 创建 ImageSpan
            ImageSpan imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE);

            // 将图片插入到 SpannableString 的开头
            spannableString.setSpan(imageSpan, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new LeadingMarginSpan.Standard((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics())), 0, spannableString.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

            // 设置 TextView 的文本
            setText(spannableString);
        } else {
            // 如果drawable为null，则清除之前的图片显示
            setCompoundDrawables(null, null, null, null);
            // 设置 TextView 的文本
            setText(project_Name);
        }
    }


    public Project_View(Context context) {
        super(context);
        init("default", true);
        IntentFilter filter = new IntentFilter(REFRESH_RESOURCE_ACTION);
        context.registerReceiver(refreshReceiver, filter);
    }

    public Project_View(Context context, AttributeSet attrs) {
        super(context, attrs);
        init("default", true);
        IntentFilter filter = new IntentFilter(REFRESH_RESOURCE_ACTION);
        context.registerReceiver(refreshReceiver, filter);
    }

    public Project_View(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init("default", true);
        IntentFilter filter = new IntentFilter(REFRESH_RESOURCE_ACTION);
        context.registerReceiver(refreshReceiver, filter);
    }

    public Project_View(Context context, String projectName, boolean isCreated) {
        super(context);
        init(projectName, isCreated);
        IntentFilter filter = new IntentFilter(REFRESH_RESOURCE_ACTION);
        context.registerReceiver(refreshReceiver, filter);
    }

    private void create_init(String projectName) {
        project_Name = projectName;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String currentDateAndTime = sdf.format(new Date());
        data_Path = currentDateAndTime + "_" + project_Name + ".txt";
        String filename="Project_"+project_Name + "_" + currentDateAndTime+".txt";
        file_Path = getContext().getFilesDir() + "/" + filename;
        cover_Path = currentDateAndTime + "_" + project_Name + ".png";
        writeToFile(getContext().getFilesDir(), filename);

        File directory = getContext().getFilesDir();
        File file = new File(directory, data_Path);
        try {
            FileWriter writer = new FileWriter(file);
            writer.write("0,0\n");
            writer.flush();
            writer.close();
            Log.d("writePathPointsToFile", "Path points saved to " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("writePathPointsToFile", "Failed to save path points");
        }
        Drawable drawable = getResources().getDrawable(R.drawable.initial_pic);
        // 将 Drawable 转换为 Bitmap
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

        // 保存 Bitmap 到文件
        directory = getContext().getFilesDir();
        file = new File(directory, cover_Path);
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
            Log.d("SaveDrawableToFile", "Drawable saved to " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("SaveDrawableToFile", "Failed to save drawable");
        }
    }


    private void open_init(String path) {
        file_Path = getContext().getFilesDir() + "/" + path;
        File file = new File(getContext().getFilesDir(), path);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                // 拆分每一行数据成键值对
                String[] parts = line.split(": ");
                if (parts.length == 2) {
                    String key = parts[0];
                    String value = parts[1];

                    // 根据键值对进行逻辑处理
                    if (key.equals("Project Name")) {
                        project_Name=value;
                        Log.d("readProjectFromFile", "Project Name: " + value);
                    } else if (key.equals("Data Path")) {
                        data_Path=value;
                        Log.d("readProjectFromFile", "Data Path: " + value);
                    } else if (key.equals("Cover Path")) {
                        cover_Path=value;
                        Log.d("readProjectFromFile", "Cover Path: " + value);
                    }
                    // 在这里添加其他键值对的处理逻辑
                } else {
                    // 如果行的格式不正确，打印警告信息
                    Log.w("readProjectFromFile", "Invalid line format: " + line);
                }
            }
            reader.close();
            Log.d("readProjectFromFile", "File read successfully: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("readProjectFromFile", "Failed to read file: " + file.getAbsolutePath());
        }
    }

    private void init(String projectName, boolean isCreated) {
        this.setClickable(true);
        if (isCreated)create_init(projectName);
        else open_init(projectName);
        // 创建 SpannableString
        SpannableString spannableString = new SpannableString("   " + project_Name);

        // 获取图片资源
        // 获取内部目录下的 cover_path 对应的文件路径
        String coverPath = getContext().getFilesDir()+"/"+cover_Path; // 替换为实际的 cover_path
        File coverFile = new File(coverPath);
        Drawable drawable = Drawable.createFromPath(coverFile.getAbsolutePath());
        int newWidth = (int) (drawable.getIntrinsicWidth() * (float) getResources().getDimensionPixelSize(R.dimen.project_view_height) / drawable.getIntrinsicHeight());
        drawable.setBounds(0, 0, newWidth, getResources().getDimensionPixelSize(R.dimen.project_view_height));
        // 创建 ImageSpan
        ImageSpan imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE);

        // 将图片插入到 SpannableString 的开头
        spannableString.setSpan(imageSpan, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new LeadingMarginSpan.Standard((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics())), 0, spannableString.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        // 设置 TextView 的文本
        setText(spannableString);
        // 设置字体大小为 25sp
        setTextSize(25);
        // 设置字体颜色为黑色
        setTextColor(Color.BLACK);
        // 设置文本框下横线颜色为灰色
        setBackgroundResource(R.drawable.project_view_background);
        // 设置文字和图片居中显示
        setGravity(Gravity.CENTER_VERTICAL | Gravity.START); // 文字和图片居中对齐

        // 设置宽度为 match_parent
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                getResources().getDimensionPixelSize(R.dimen.project_view_height) // 高度为 36dp
        );
        setLayoutParams(params);
        // 设置长按监听器
        setLongClickListener();

        // 设置点击监听器
        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 执行点击后的操作，比如打开项目详情页或者执行其他逻辑
                Toast.makeText(getContext(), "Project " + project_Name + " clicked", Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.d("Project_View", "onTouchEvent: ACTION_DOWN");
                break;
            case MotionEvent.ACTION_UP:
                Log.d("Project_View", "onTouchEvent: ACTION_UP");
                break;
            case MotionEvent.ACTION_CANCEL:
                Log.d("Project_View", "onTouchEvent: ACTION_CANCEL");
                break;
        }
        return result;
    }

    private void writeToFile(File directory, String filename) {
        File file = new File(directory, filename);
        try {
            FileWriter writer = new FileWriter(file);
            writer.append("Project Name: " + project_Name + "\n");
            writer.append("Data Path: " + data_Path + "\n");
            writer.append("Cover Path: " + cover_Path);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

     private void setLongClickListener() {
        setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // 弹出删除对话框
                showDeleteDialog();
                return true;
            }
        });
    }

    private void showDeleteDialog() {
        // 在这里实现弹出删除对话框的逻辑
        // 提示用户是否确认删除
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage("确认删除该工程？")
                .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 调用删除方法
                        delete();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    private void delete() {
        // 释放图片资源
        recycleDrawable();
        // 如果有设置删除监听器，则回调删除方法
        if (onDeleteListener != null) {
            onDeleteListener.onDelete(this);
        }
    }

    private void recycleDrawable() {
        Drawable[] drawables = getCompoundDrawables();
        for (Drawable drawable : drawables) {
            if (drawable != null) {
                drawable.setCallback(null);
            }
        }
    }

    public void setOnDeleteListener(OnDeleteListener listener) {
        onDeleteListener = listener;
    }

    public interface OnDeleteListener {
        void onDelete(Project_View projectView);
    }

    public String getProjectName() {
        return project_Name;
    }

    public String getDataPath() {
        return data_Path;
    }

    public String getFilePath() {
        return file_Path;
    }

    public String getCoverPath() { return cover_Path; }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // 在 View 销毁时取消注册广播接收器
        getContext().unregisterReceiver(refreshReceiver);
    }
}
