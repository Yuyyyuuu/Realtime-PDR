package com.example.myapplication;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import java.util.ArrayList;

class MyPagerAdapter extends PagerAdapter {
    private ArrayList<View> viewArrayList;

    public MyPagerAdapter(ArrayList<View> viewArrayList) {
        this.viewArrayList = viewArrayList;
    }

    //获取ViewPager中View的个数
    @Override
    public int getCount() {
        return viewArrayList.size();
    }


    //通过instantiateItem的方法，判断instantiateItem方法返回的View是否为同一个。一般固定写 return view == object;
    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }


    //主要的：获取View集合中处于position位置的View，将View添加到ViewGroup 容器中并返回集合中的当前View
    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        container.addView(viewArrayList.get(position));
        return viewArrayList.get(position);
    }

    //移除前一个界面
    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView(viewArrayList.get(position));
    }
}
