package com.example.myapplication;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;


public class FileHelper {

    public static void writeToFile(Context context, String fileName, String data) {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), fileName);

        try {
            FileWriter writer = new FileWriter(file, true);
            writer.append(data).append("\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


