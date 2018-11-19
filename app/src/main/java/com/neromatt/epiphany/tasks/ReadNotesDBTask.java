package com.neromatt.epiphany.tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.helper.DBInterface;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.DataObjects.SingleNote;

import java.util.ArrayList;

import androidx.annotation.NonNull;

public class ReadNotesDBTask extends AsyncTask<MainModel, Void, ArrayList<MainModel>> {

    private final NotesListener listener;
    private final DBInterface db;

    public ReadNotesDBTask(DBInterface db, @NonNull NotesListener listener) {
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
                    if (db.getDatabase().isNoteInDB(note.getFullPath())) {
                        note.refreshFromDB(db.getDatabase());
                    } else {
                        note.refreshFromFile(db.getDatabase());
                    }
                    listener.NoteLoaded(note);
                    res.add(note);
                }
            }
        }

        return res;
    }

    @Override
    protected void onPostExecute(ArrayList<MainModel> list) {
        listener.NotesLoaded(list, Constants.NOTES_LOADED);
    }

    public interface NotesListener {
        void NoteLoaded(SingleNote note);
        void NotesLoaded(ArrayList<MainModel> list, int flag);
    }
}
