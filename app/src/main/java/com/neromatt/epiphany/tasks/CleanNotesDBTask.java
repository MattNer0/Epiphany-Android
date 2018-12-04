package com.neromatt.epiphany.tasks;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.helper.DBInterface;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.DataObjects.SingleNote;

import java.io.File;
import java.util.ArrayList;

public class CleanNotesDBTask extends AsyncTask<String, Void, Integer> {

    private final DBInterface db;
    private final CleanDBListener listener;

    public CleanNotesDBTask(DBInterface db) {
        this.db = db;
        this.listener = null;
    }

    public CleanNotesDBTask(DBInterface db, CleanDBListener listener) {
        this.db = db;
        this.listener = listener;
    }

    @Override
    protected Integer doInBackground(String... paths) {
        Integer res = 0;

        ArrayList<Bundle> db_data = db.getDatabase().getNotesByFolderPath(paths[0]);
        for (Bundle note: db_data) {
            File f = new File(note.getString(Constants.KEY_NOTE_PATH)+"/"+note.getString(Constants.KEY_NOTE_FILENAME));
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
        if (listener != null) listener.DatabaseCleaned();
    }

    public interface CleanDBListener {
        void DatabaseCleaned();
    }
}
