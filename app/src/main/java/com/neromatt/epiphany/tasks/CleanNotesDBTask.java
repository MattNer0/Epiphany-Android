package com.neromatt.epiphany.tasks;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.helper.DBInterface;

import java.io.File;
import java.util.ArrayList;

public class CleanNotesDBTask extends AsyncTask<String, Void, Integer> {
    private final DBInterface db;

    public CleanNotesDBTask(DBInterface db) {
        this.db = db;
    }

    @Override
    protected Integer doInBackground(String... paths) {
        Integer res = 0;

        ArrayList<Bundle> db_data = db.getDatabase().getNotesByFolderPath(paths[0]);
        for (Bundle note: db_data) {
            File f = new File(note.getString(Constants.KEY_NOTE_PATH));
            if (!f.exists()) {
                if (db.getDatabase().deleteNoteByID(note.getInt("id"))) {
                    res++;
                }
            }
        }

        return res;
    }

    @Override
    protected void onPostExecute(Integer count) {
        if (count > 0) {
            Log.i(Constants.LOG, "database cleaned " + count);
        }
    }
}
