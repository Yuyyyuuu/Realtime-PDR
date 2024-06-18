package com.example.myapplication;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class Configure extends DialogFragment {

    private Activity activity;

    public int filter_mode = 3;
    public int yaw_update_mode = 3;
    public int step_detect_mode = 1;
    public int step_length_mode = 3;
    public double height = 1.65;

    public Configure(Activity activity) {
        this.activity = activity;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // 创建对话框并设置标题
        Dialog dialog = new Dialog(activity);
        dialog.setTitle("Configure");

        // 设置对话框的内容视图
        View contentView = LayoutInflater.from(activity).inflate(R.layout.configure_layout, null);
        dialog.setContentView(contentView);

        // 获取对话框中的控件
        TextView filterText = contentView.findViewById(R.id.filter_text);
        RadioGroup filterRadios = contentView.findViewById(R.id.filter_radios);
        TextView qUpdateText = contentView.findViewById(R.id.q_update_text);
        RadioGroup qUpdateRadios = contentView.findViewById(R.id.q_update_radios);
        TextView stepText = contentView.findViewById(R.id.step_text);
        RadioGroup stepRadios = contentView.findViewById(R.id.step_radios);
        TextView lengthText = contentView.findViewById(R.id.length_text);
        RadioGroup lengthRadios = contentView.findViewById(R.id.length_radios);
        EditText heightText=contentView.findViewById(R.id.height_edit_text);

        filterRadios.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                handleFilterSelection(checkedId);
            }
        });

        qUpdateRadios.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                handleQUpdateSelection(checkedId);
            }
        });

        stepRadios.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                handleStepSelection(checkedId);
            }
        });

        lengthRadios.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                handleLengthSelection(checkedId);
            }
        });

        heightText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                height=Double.parseDouble(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        invalid_configure(contentView);

        return dialog;
    }


    private void handleFilterSelection(int checkedId) {
        if(checkedId==R.id.filter_1) {
            filter_mode=1;
        }
        else if(checkedId==R.id.filter_2){
            filter_mode=2;
        }
        else if(checkedId==R.id.filter_3){
            filter_mode=3;
        }
    }

    private void handleStepSelection(int checkedId) {
        if(checkedId==R.id.step_1)step_detect_mode=1;
        else if(checkedId==R.id.step_2)step_detect_mode=2;
    }

    private void handleQUpdateSelection(int checkedId) {
        if(checkedId==R.id.q_update_1)yaw_update_mode=1;
        else if(checkedId==R.id.q_update_2)yaw_update_mode=2;
        else if(checkedId==R.id.q_update_3)yaw_update_mode=3;
    }

    private void handleLengthSelection(int checkedId) {
        if(checkedId==R.id.length_1)step_length_mode=1;
        else if(checkedId==R.id.length_2)step_length_mode=2;
        else if(checkedId==R.id.length_3)step_length_mode=3;
    }

    private void invalid_configure(View contentView){
        RadioButton btn;
        switch (filter_mode){
            case 1:
                btn=contentView.findViewById(R.id.filter_1);
                btn.setChecked(true);
                break;
            case 2:
                btn=contentView.findViewById(R.id.filter_2);
                btn.setChecked(true);
                break;
            case 3:
                btn=contentView.findViewById(R.id.filter_3);
                btn.setChecked(true);
                break;
        }
        switch (yaw_update_mode){
            case 1:
                btn=contentView.findViewById(R.id.q_update_1);
                btn.setChecked(true);
                break;
            case 2:
                btn=contentView.findViewById(R.id.q_update_2);
                btn.setChecked(true);
                break;
            case 3:
                btn=contentView.findViewById(R.id.q_update_3);
                btn.setChecked(true);
                break;
        }
        switch (step_detect_mode){
            case 1:
                btn=contentView.findViewById(R.id.step_1);
                btn.setChecked(true);
                break;
            case 2:
                btn=contentView.findViewById(R.id.step_2);
                btn.setChecked(true);
                break;
        }
        switch (step_length_mode){
            case 1:
                btn=contentView.findViewById(R.id.length_1);
                btn.setChecked(true);
                break;
            case 2:
                btn=contentView.findViewById(R.id.length_2);
                btn.setChecked(true);
                break;
            case 3:
                btn=contentView.findViewById(R.id.length_3);
                btn.setChecked(true);
                break;
        }
        EditText editText=contentView.findViewById(R.id.height_edit_text);
        editText.setHint(Double.toString(height));
    }
}
