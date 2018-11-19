package com.neromatt.epiphany.tasks;

import android.os.AsyncTask;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.helper.DBInterface;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.DataObjects.SingleNote;

import java.util.ArrayList;

import androidx.annotation.NonNull;

public class ReadNotesTask extends AsyncTask<MainModel, Void, ArrayList<MainModel>> {

    private final ReadNotesDBTask.NotesListener listener;
    private final DBInterface db;

    public ReadNotesTask(DBInterface db, @NonNull ReadNotesDBTask.NotesListener listener) {
        this.db = db;
        this.listener = listener;
    }

    @Override
    protected ArrayList<MainModel> doInBackground(MainModel... mainModels) {
        ArrayList<MainModel> res = new ArrayList<>();
        for (MainModel m: mainModels) {
            if (m.isNote()) {
                SingleNote note = (SingleNote) m;
                if (!note.wasLoaded()) {
                    note.refreshFromFile(db.getDatabase());
                    res.add(note);
                }
            }
        }

        return res;
    }

    @Override
    protected void onPostExecute(ArrayList<MainModel> list) {
        listener.NotesLoaded(list, Constants.NOTES_LOADED_INTO_DATABASE);
    }
}
