package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.text.DecimalFormat;

public class MapView extends View {

    private Paint start_point_paint;//开始圆点
    private Paint end_point_paint;//结束圆点
    private Paint line_paint;//线
    private Paint xaxis_paint;//坐标轴
    private Paint yaxis_paint;//坐标轴
    private Paint text_paint;//文字
    private Paint dashedLinePaint;//网格
    private Path path;//轨迹
    private Path xAxis;
    private Path yAxis;
    private final List<XY> xyList = new ArrayList<>();//绘制点集合

    private int view_width;//view的宽
    private int view_height;//view的高

    private float x_point_left = 0;//轨迹最左（西）边的X点
    private float x_point_right = 0;//轨迹最右（东）边的X点
    private float y_point_top = 0;//轨迹最顶（北）边的Y点
    private float y_point_bottom = 0;//轨迹最底（南）边的Y点

    private boolean isEnd;

    private float scale=1f;
    private float move_x=0;
    private float move_y=0;
    private ScaleGestureDetector scaleGestureDetector;
    private float lastX, lastY;
    private boolean isDragging = false;


    // 内部类，用于监听放缩手势
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            scale *= scaleFactor;
            invalidate(); // 重新绘制
            return true;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 将触摸事件传递给ScaleGestureDetector
        scaleGestureDetector.onTouchEvent(event);
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = x;
                lastY = y;
                isDragging = true;
                break;
            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    float deltaX = x - lastX;
                    float deltaY = y - lastY;
                    move_x += deltaX;
                    move_y += deltaY;
                    lastX = x;
                    lastY = y;
                    invalidate(); // 重新绘制
                }
                break;
            case MotionEvent.ACTION_UP:
                isDragging = false;
                break;
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                break;
        }

        return true;
    }


    public MapView(Context context) {
        this(context, null);
    }

    public MapView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MapView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MapView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        start_point_paint = new Paint();
        end_point_paint = new Paint();
        line_paint = new Paint();
        xaxis_paint=new Paint();
        yaxis_paint=new Paint();
        dashedLinePaint = new Paint();

        text_paint = new Paint();

        path = new Path();
        xAxis=new Path();
        yAxis=new Path();
        isEnd=false;

        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
    }

    //绘制前重置一下避免一致绘制缓存过大出现卡顿现象
    public void reset() {
        start_point_paint.reset();
        start_point_paint.setAntiAlias(true);
        start_point_paint.setDither(true);
        start_point_paint.setStyle(Paint.Style.FILL_AND_STROKE);
        start_point_paint.setColor(Color.parseColor("#03A9F4"));
        start_point_paint.setStrokeWidth(35.0f);

        end_point_paint.reset();
        end_point_paint.setAntiAlias(true);
        end_point_paint.setDither(true);
        end_point_paint.setStyle(Paint.Style.FILL_AND_STROKE);
        end_point_paint.setColor(Color.parseColor("#EC4F44"));
        end_point_paint.setStrokeWidth(35.0f);

        line_paint.reset();
        line_paint.setAntiAlias(true);
        line_paint.setDither(true);
        line_paint.setStyle(Paint.Style.STROKE);
        line_paint.setColor(Color.parseColor("#FDF447"));
        line_paint.setStrokeWidth(25.0f);
        line_paint.setStrokeJoin(Paint.Join.ROUND);//设置线段连接处为圆角

        xaxis_paint.reset();
        xaxis_paint.setAntiAlias(true);
        xaxis_paint.setDither(true);
        xaxis_paint.setStyle(Paint.Style.STROKE);
        xaxis_paint.setColor(Color.parseColor("#B9B6B6"));
        xaxis_paint.setStrokeWidth(10.0f);

        yaxis_paint.reset();
        yaxis_paint.setAntiAlias(true);
        yaxis_paint.setDither(true);
        yaxis_paint.setStyle(Paint.Style.STROKE);
        yaxis_paint.setColor(Color.parseColor("#B9B6B6"));
        yaxis_paint.setStrokeWidth(10.0f);

        text_paint.reset();
        text_paint.setAntiAlias(true);
        text_paint.setDither(true);
        text_paint.setColor(Color.BLACK);
        text_paint.setTextSize(40.0f);

        dashedLinePaint.setColor(Color.GRAY);
        dashedLinePaint.setStrokeWidth(1);
        dashedLinePaint.setStyle(Paint.Style.STROKE);
        dashedLinePaint.setPathEffect(new DashPathEffect(new float[]{5, 10}, 0));

        path.reset();
        xAxis.reset();
        yAxis.reset();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.i("view", "onMeasure");
        int width_size = MeasureSpec.getSize(widthMeasureSpec);
        int width_mode = MeasureSpec.getMode(widthMeasureSpec);
        int height_size = MeasureSpec.getSize(heightMeasureSpec);
        int height_mode = MeasureSpec.getMode(heightMeasureSpec);

        if (width_mode == MeasureSpec.EXACTLY) {
            view_width = width_size;
        } else if (width_mode == MeasureSpec.AT_MOST) {
            view_width = getPaddingStart() + getPaddingEnd();
        }
        if (height_mode == MeasureSpec.EXACTLY) {
            view_height = height_size;
        } else if (height_mode == MeasureSpec.AT_MOST) {
            view_height = getPaddingTop() + getPaddingBottom();
        }
        Log.d("view", "view_width:" + view_width + " *** view_height:" + view_height);
        setMeasuredDimension(view_width, view_height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        Log.i("view", "onLayout");
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.i("view", "onDraw");
        Log.i("view", "xyList.size: " + xyList.size());
        reset();
        if (xyList.size() == 0) {
            return;
        }

        Log.d("view", "x_point_left: " + x_point_left);
        Log.d("view", "x_point_right: " + x_point_right);
        Log.d("view", "y_point_top: " + y_point_top);
        Log.d("view", "y_point_bottom: " + y_point_bottom);

        float scaleNumX = 300f;//X轴默认值缩放倍数
        float scaleNumY = 300f;//Y轴默认值缩放倍数

        float path_width = (x_point_right - x_point_left) * scaleNumX;//pat图形的宽
        float path_height = (y_point_bottom - y_point_top) * scaleNumY;//pat图形的高

        if (path_width == 0 && path_height == 0 || view_width == 0 && view_height == 0) {//正常数据计算的宽高是不为0的
            Log.e("view", "path_width: " + path_width + " **** path_height: " + path_height);
            return;//数据异常不在执行，否则下面while死循环
        }

        scaleNumX = view_width / (x_point_right - x_point_left);//X轴默认值缩放倍数
        scaleNumY = view_height / (y_point_bottom - y_point_top);//Y轴默认值缩放倍数
        while (path_width > (float) view_width) {
            scaleNumX -= 0.01f;
            path_width = (x_point_right - x_point_left) * scaleNumX;
        }
        while (path_width < (float) view_width) {
            scaleNumX += 0.01f;
            path_width = (x_point_right - x_point_left) * scaleNumX;
        }
        while (path_height > (float) view_height) {
            scaleNumY -= 0.01f;
            path_height = (y_point_bottom - y_point_top) * scaleNumY;
        }
        while (path_height < (float) view_height) {
            scaleNumY += 0.01f;
            path_height = (y_point_bottom - y_point_top) * scaleNumY;
        }

        if (scaleNumX > scaleNumY) scaleNumX = scaleNumY;
        else scaleNumY = scaleNumX;
        path_width = (x_point_right - x_point_left) * scaleNumX;
        path_height = (y_point_bottom - y_point_top) * scaleNumY;
        Log.i("view", "scaleNumX: " + scaleNumX + " **** scaleNumY: " + scaleNumY);
        Log.i("view", "view_width: " + view_width + " **** view_height: " + view_height);
        Log.i("view", "path_width: " + path_width + " **** path_height: " + path_height);
//		Log.d("view", "view_width / 2 = " + view_width / 2 + " **** view_height / 2 = " + view_height / 2);
//		Log.d("view", "path_width / 2 = " + path_width / 2 + " **** path_height / 2 = " + path_height / 2);


        float left = x_point_left * scaleNumX;//缩放倍数后的最左边点
        float right = x_point_right * scaleNumX;//...最右边点
        float top = y_point_top * scaleNumY;//...最顶边点
        float bottom = y_point_bottom * scaleNumY;//...最底边点

        Log.d("view", "left: " + left);
        Log.d("view", "right: " + right);
        Log.d("view", "top: " + top);
        Log.d("view", "bottom: " + bottom);

        /****************** 计算整个path图形居中显示，开始点X，Y轴需要缩放、平移的位置 start *****************/
        //缩小比例使开始和结束点不挨着边界
        float sx = 0.7f * scale;//X等比缩小30%
        float sy = 0.7f * scale;//Y等比缩小30%
        float px = view_width / 2f;
        float py = view_height / 2f;
        Log.d("view", "px: " + px + ", py: " + py);
        //缩小canvas
        canvas.scale(sx, sy, px, py);

//		///////////缩放后可绘制的区域///////////
//      RectF rectF = new RectF(0,0, path_width, path_height);
//		//绘制上述矩形区域
//		canvas.drawRect(rectF, line_paint);
//		/////////////////////////////////////

        //计算开始点需要平移的位置
        float onePointX = 0;//平移的第一个X点
        float onePointY = 0;//平移的第一个Y点
        if (left <= 0) {
            float leftTo0X = 0 - left;//最左边点到0X的距离
            onePointX = xyList.get(0).x * scaleNumX + leftTo0X;
        }

        if (right >= path_width) {
            float rightToPw = right - path_width;//最右边点到path宽度的距离
            onePointX = xyList.get(0).x * scaleNumX - rightToPw;
        }

        if (top <= 0) {
            float topTo0Y = 0 - top;//最顶边点到0Y的距离
            onePointY = xyList.get(0).y * scaleNumY + topTo0Y;
        }

        if (bottom >= path_height) {
            float bottomTo0Y = bottom - path_height;//最底边点到path高度的距离
            onePointY = xyList.get(0).y * scaleNumY - bottomTo0Y;
        }

        if (xyList.get(0).x * scaleNumX > 0) {
            onePointX -= xyList.get(0).x * scaleNumX;
        } else {
            onePointX += 0 - xyList.get(0).x * scaleNumX;
        }

        if (xyList.get(0).y * scaleNumY > 0) {
            onePointY -= xyList.get(0).y * scaleNumY;
        } else {
            onePointY += 0 - xyList.get(0).y * scaleNumY;
        }
        onePointX = -scaleNumX * (x_point_left+x_point_right)/(2f) + move_x + px;
        onePointY = -scaleNumY * (y_point_bottom+y_point_top)/(2f) + move_y + py;
        Log.d("move_x", String.valueOf(move_x));
        Log.d("move_y", String.valueOf(move_y));
        Log.i("view", "onePointX: " + onePointX + ", onePointY: " + onePointY);
        Log.i("view", "xyList.get(0).x * scaleNumX: " + (xyList.get(0).x * scaleNumX) + ", xyList.get(0).y * scaleNumX: " + (xyList.get(0).y * scaleNumY));
        Log.i("view", "xyList.get(0).x: " + (xyList.get(0).x) + ", xyList.get(0).y: " + (xyList.get(0).y));
        //平移到计算的显示位置
        canvas.translate(onePointX, onePointY);
        /****************** 计算整个path图形居中显示，开始点X，Y轴需要缩放、平移的位置 end *****************/

        //设置起点位置
        path.moveTo(xyList.get(0).x * scaleNumX, xyList.get(0).y * scaleNumY);
        //设置终点位置
        //path.setLastPoint(xyList.get(xyList.size() - 1).x * scaleNumX, xyList.get(xyList.size() - 1).y * scaleNumY);
        for (int i = 0; i < xyList.size(); i++) {
            //连接其他点
            path.lineTo(xyList.get(i).x * scaleNumX, xyList.get(i).y * scaleNumY);
        }
        //绘制轨迹
        canvas.drawPath(path, line_paint);

        if(isEnd){
            // 绘制坐标轴
            int left_num = 0;
            int right_num = 0;
            int top_num = 0;
            int bottom_num = 0;
            float x_axis_left = -((1 - sx) * px + onePointX * sx) / sx;
            float x_axis_right = (view_width - ((1 - sx) * px + onePointX * sx)) / sx;
            float y_axis_top = -((1 - sy) * py + onePointY * sy) / sy;
            float y_axis_bottom = (view_height - ((1 - sy) * py + onePointY * sy)) / sy;
            double x_interval = findClosestNumber((x_axis_right - x_axis_left) / (12.0f * scaleNumX * sx));
            double y_interval = findClosestNumber((y_axis_bottom - y_axis_top) / (16.0f * scaleNumY * sy));
            while ((x_axis_right - x_axis_left)/(x_interval*scaleNumX*sx)<10)x_interval/=2;
            while ((x_axis_right - x_axis_left)/(x_interval*scaleNumX*sx)>25)x_interval*=2;
            while ((y_axis_bottom - y_axis_top)/(y_interval*scaleNumY*sy)<10)y_interval/=2;
            while ((y_axis_bottom - y_axis_top)/(y_interval*scaleNumY*sy)>25)y_interval*=2;
            Log.d("x_interval", String.valueOf(x_interval));
            Log.d("x_num", String.valueOf((x_axis_right - x_axis_left)/(x_interval*scaleNumX*sx)));
            Log.d("y_interval", String.valueOf(y_interval));
            Log.d("y_num", String.valueOf((y_axis_bottom - y_axis_top)/(y_interval*scaleNumY*sy)));
            // 绘制X轴
            canvas.drawLine(x_axis_left, 0, x_axis_right, 0, xaxis_paint);
            xAxis.moveTo(x_axis_right-5/sx,0);
            xAxis.lineTo(x_axis_right-30/sx, 20/sx);
            xAxis.moveTo(x_axis_right-5/sx,0);
            xAxis.lineTo(x_axis_right-30/sx, -20/sx);
            canvas.drawPath(xAxis, xaxis_paint);
            // 绘制Y轴
            canvas.drawLine(0, y_axis_top, 0, y_axis_bottom, yaxis_paint);
            yAxis.moveTo(0, y_axis_top+5/sx);
            yAxis.lineTo(20/sx, y_axis_top+30/sx);
            yAxis.moveTo(0, y_axis_top+5/sx);
            yAxis.lineTo(-20/sx, y_axis_top+30/sx);
            canvas.drawPath(yAxis, yaxis_paint);
            float x = 0;
            for (left_num = 1; -left_num * x_interval * sx * scaleNumX > x_axis_left; left_num++) {
                x = -(float) (left_num * x_interval * scaleNumX);
                canvas.drawLine(x, 0, x, -20 / sx, xaxis_paint);
                canvas.drawLine(x, y_axis_top, x, y_axis_bottom, dashedLinePaint);
                // 绘制文字标签
                DecimalFormat decimalFormat = new DecimalFormat("#.##");
                // 格式化数字，并转换为字符串
                String label = decimalFormat.format(-left_num * x_interval);
                float labelWidth = text_paint.measureText(label);
                canvas.drawText(label, x - labelWidth / 2, 40/ sx, text_paint);
            }
            for (right_num = 1; right_num * x_interval * sx * scaleNumX < x_axis_right; right_num++) {
                x = (float) (right_num * x_interval * scaleNumX);
                canvas.drawLine(x, 0, x, -20 / sx, xaxis_paint);
                canvas.drawLine(x, y_axis_top, x, y_axis_bottom, dashedLinePaint);
                // 绘制文字标签
                DecimalFormat decimalFormat = new DecimalFormat("#.##");
                // 格式化数字，并转换为字符串
                String label = decimalFormat.format(right_num * x_interval);
                float labelWidth = text_paint.measureText(label);
                canvas.drawText(label, x - labelWidth / 2, 40/ sx, text_paint);
            }
            for (top_num = 1; -top_num * y_interval * sx * scaleNumY > y_axis_top; top_num++) {
                x = -(float) (top_num * y_interval * scaleNumY);
                canvas.drawLine(0, x, 20 / sx, x, xaxis_paint);
                canvas.drawLine(x_axis_left, x, x_axis_right, x, dashedLinePaint);
                // 绘制文字标签
                DecimalFormat decimalFormat = new DecimalFormat("#.##");
                // 格式化数字，并转换为字符串
                String label = decimalFormat.format(top_num * y_interval);
                float labelWidth = text_paint.measureText(label);
                canvas.drawText(label, -labelWidth - 20, x + 15/ sx, text_paint);
            }
            for (bottom_num = 1; bottom_num * y_interval * sx * scaleNumY < y_axis_bottom; bottom_num++) {
                x = (float) (bottom_num * y_interval * scaleNumY);
                canvas.drawLine(0, x, 20 / sx, x, xaxis_paint);
                canvas.drawLine(x_axis_left, x, x_axis_right, x, dashedLinePaint);
                // 绘制文字标签
                DecimalFormat decimalFormat = new DecimalFormat("#.##");
                // 格式化数字，并转换为字符串
                String label = decimalFormat.format(-bottom_num * y_interval);
                float labelWidth = text_paint.measureText(label);
                canvas.drawText(label, -labelWidth - 20, x + 15/ sx, text_paint);
            }
            canvas.drawText("0", -10/ sx, 40/ sx, text_paint);
        }

        //绘制开始圆点
        canvas.drawCircle(xyList.get(0).x * scaleNumX, xyList.get(0).y * scaleNumY, 1.0f, start_point_paint);

        //绘制起点文字
        canvas.drawText("起点", xyList.get(0).x * scaleNumX, xyList.get(0).y * scaleNumY, text_paint);

        if(!isEnd){
            end_point_paint.setColor(Color.parseColor("#4CAF50"));
            canvas.drawCircle(xyList.get(xyList.size() - 1).x * scaleNumX, xyList.get(xyList.size() - 1).y * scaleNumY, 1.0f, end_point_paint);
        }
        if(isEnd){
            end_point_paint.setColor(Color.parseColor("#EC4F44"));
            //绘制结束圆点
            canvas.drawCircle(xyList.get(xyList.size() - 1).x * scaleNumX, xyList.get(xyList.size() - 1).y * scaleNumY, 1.0f, end_point_paint);
            //绘制终点文字
            canvas.drawText("终点", xyList.get(xyList.size() - 1).x * scaleNumX, xyList.get(xyList.size() - 1).y * scaleNumY, text_paint);
        }
    }

    /**
     * 设置数据
     *
     * @param trajectory
     */
    public void addData(List<double[]> trajectory, boolean isEnd) {
        for (int i = 0; i < trajectory.size(); i++) {
            double[] p=trajectory.get(i);
            XY xy=new XY((float) p[1], (float) -p[0]);
            xyList.add(xy);
            if (x_point_left > xy.x) x_point_left = xy.x;//最左的边x点
            if (x_point_right < xy.x) x_point_right = xy.x;//最右边的x点
            if (y_point_top > xy.y) y_point_top = xy.y;//最上边的y点
            if (y_point_bottom < xy.y) y_point_bottom = xy.y;//最底边的y点
        }
        this.isEnd=isEnd;
        invalidate();
    }

    public void addend(){
        isEnd=true;
        invalidate();
    }


    public static class XY {
        float x;
        float y;

        public XY() {
        }

        public XY(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    public void saveToImage(File file) {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas);
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void restart(){
        xyList.clear();
        x_point_left = 0;//轨迹最左（西）边的X点
        x_point_right = 0;//轨迹最右（东）边的X点
        y_point_top = 0;//轨迹最顶（北）边的Y点
        y_point_bottom = 0;//轨迹最底（南）边的Y点
        reset();
        invalidate();
    }

    private static double findClosestNumber(double num) {
        if (num == 0.0) return 1.0; // 边界情况：如果给定数为 0，则返回 1

        double absNum = Math.abs(num); // 转换为正数进行处理

        double magnitude = 1.0;
        while (magnitude <= absNum) {
            magnitude *= 10.0; // 找到给定数的数量级
        }

        // 根据数量级选择最接近的数
        double closestNumber;
        if (absNum % (magnitude / 10.0) >= magnitude / 20.0) {
            closestNumber = magnitude / 2.0;
        } else {
            closestNumber = magnitude / 10.0;
        }

        return num < 0 ? -closestNumber : closestNumber; // 如果原数为负数，则返回负数形式的最接近数
    }


}