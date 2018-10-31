package com.neromatt.epiphany.model;

import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.Gson.ModelTypeAdapter;
import com.neromatt.epiphany.service.LibraryService;
import com.neromatt.epiphany.ui.MainActivity;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class Library {

    private static boolean service_launched = false;

    public static void serviceFinished() {
        service_launched = false;
    }

    public static void launchServiceForBuckets(MainActivity context, Path path) {
        launchService(context, path, serviceRequestEnum.BUCKETS, null);
    }

    public static void launchServiceForFolder(MainActivity context, MainModel model) {
        launchService(context, null, serviceRequestEnum.FOLDER, model);
    }

    public static void launchServiceForNotes(MainActivity context, Path path, MainModel model) {
        launchService(context, path, serviceRequestEnum.NOTES, model);
        service_launched = false;
    }

    private static void launchService(MainActivity context, Path path, serviceRequestEnum request, MainModel model) {
        if (!service_launched) {
            service_launched = true;
            Intent intent = new Intent(context, LibraryService.class);
            if (path != null) {
                intent.putExtra("root", path.getRootPath());
            }

            intent.putExtra("request", request);

            if (request == serviceRequestEnum.FOLDER || request == serviceRequestEnum.NOTES) {
                intent.putExtra("model", model);
            }

            context.startService(intent);
        }
    }

    public static void launchServiceForCleaningDB(Context context) {
        Intent intent = new Intent(context, LibraryService.class);
        intent.putExtra("request", serviceRequestEnum.CLEAN);
        context.startService(intent);
    }

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
