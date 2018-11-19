package com.neromatt.epiphany.model;

import android.content.Context;
import android.content.Intent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.Gson.ModelTypeAdapter;
import com.neromatt.epiphany.ui.MainActivity;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class Library {

    /*public static void launchServiceForCleaningDB(Context context) {
        Intent intent = new Intent(context, LibraryService.class);
        intent.putExtra("request", serviceRequestEnum.CLEAN);
        context.startService(intent);
    }*/

    public static void saveToFile(MainActivity context, ArrayList<MainModel> library_list, String bucket) {
        FileOutputStream outputStream;
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(MainModel.class, new ModelTypeAdapter());
        Gson gson = builder.create();

        Type modelListType = new TypeToken<ArrayList<MainModel>>() {}.getType();
        String s = gson.toJson(library_list, modelListType);

        try {
            outputStream = context.openFileOutput(bucket+".json", Context.MODE_PRIVATE);
            outputStream.write(s.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<MainModel> readFromFile(MainActivity context, String bucket) {
        try {
            FileInputStream fis;
            fis = context.openFileInput(bucket+".json");
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }

            String json = sb.toString();

            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(MainModel.class, new ModelTypeAdapter());
            Gson gson = builder.create();

            Type modelListType = new TypeToken<ArrayList<MainModel>>() {}.getType();
            return gson.fromJson(json, modelListType);

        } catch (IOException e) {
            return null;
        }
    }

    public static MainModel getQuickNotesBucket(ArrayList<MainModel> library_list) {
        for (MainModel m : library_list) {
            if (m.isBucket() && m.getName().equals(Constants.QUICK_NOTES_BUCKET)) {
                return m;
            }
        }
        return null;
    }

    public enum serviceRequestEnum {
        BUCKETS,
        FOLDER,
        NOTES,
        CLEAN
    }
}
