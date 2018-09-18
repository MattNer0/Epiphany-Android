package com.neromatt.epiphany.model.DataObjects;

import android.content.Context;
import android.os.Build;

import java.util.Locale;

public class MainModel {

    private static final int TYPE_NULL = 0;
    public static final int TYPE_RACK = 1;
    public static final int TYPE_FOLDER = 2;
    public static final int TYPE_MARKDOWN_NOTE = 3;

    int modelType;

    public MainModel() {
        this.modelType = TYPE_NULL;
    }

    public String getName() {
        return "";
    }
    public String getTitle() {
        return getName();
    }
    public String getPath() {
        return "";
    }

    public int getType() {
        return modelType;
    }

    public boolean isFolder() {
        return modelType == TYPE_FOLDER;
    }

    public int getOrder() { return 0; }

    public static Locale getCurrentLocale(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            return context.getResources().getConfiguration().getLocales().get(0);
        } else {
            //noinspection deprecation
            return context.getResources().getConfiguration().locale;
        }
    }
}
