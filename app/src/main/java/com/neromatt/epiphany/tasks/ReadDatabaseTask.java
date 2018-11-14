package com.neromatt.epiphany.tasks;

import android.os.AsyncTask;
import android.os.Bundle;

import com.neromatt.epiphany.helper.Database;
import com.neromatt.epiphany.model.DataObjects.MainModel;

import java.util.ArrayList;

public class ReadDatabaseTask extends AsyncTask<String, Void, ArrayList<MainModel>> {

    private final Database db;
    private final DatabaseListener listener;

    public ReadDatabaseTask(Database db, DatabaseListener listener) {
        this.db = db;
        this.listener = listener;
    }

    @Override
    protected ArrayList<MainModel> doInBackground(String... strings) {
        if (strings[0].equals("buckets")) {
            return db.getBuckets();
        }

        return null;
    }

    @Override
    protected void onPostExecute(ArrayList<MainModel> data) {
        if (listener != null) listener.DataLoaded(data);
    }

    public interface DatabaseListener {
        void DataLoaded(ArrayList<MainModel> list);
    }
}
