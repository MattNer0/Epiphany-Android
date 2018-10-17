package com.neromatt.epiphany.model;

import android.content.ComponentName;
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

    public static void launchService(MainActivity context, Path path) {
        if (!service_launched) {
            service_launched = true;
            Intent intent = new Intent(context, LibraryService.class);
            String root_path;
            if (path == null) {
                root_path = PreferenceManager.getDefaultSharedPreferences(context).getString("pref_root_directory", "");
            } else {
                root_path = path.getRootPath();
            }
            intent.putExtra("root", root_path);
            context.startService(intent);
        }
    }

    public static void saveToFile(MainActivity context, ArrayList<MainModel> library_list) {
        FileOutputStream outputStream;
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(MainModel.class, new ModelTypeAdapter());
        Gson gson = builder.create();

        Type modelListType = new TypeToken<ArrayList<MainModel>>() {}.getType();
        String s = gson.toJson(library_list, modelListType);

        try {
            outputStream = context.openFileOutput("library.json", Context.MODE_PRIVATE);
            outputStream.write(s.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<MainModel> readFromFile(MainActivity context) {
        try {
            FileInputStream fis;
            fis = context.openFileInput("library.json");
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
}
