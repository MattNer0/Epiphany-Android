package com.neromatt.epiphany.tasks;

import android.os.AsyncTask;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.helper.DBInterface;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.DataObjects.SingleNote;

import java.util.ArrayList;

import androidx.annotation.NonNull;

public class SearchNotesTask extends AsyncTask<MainModel, Void, ArrayList<MainModel>> {

    private final ReadNotesDBTask.NotesListener listener;
    private final DBInterface db;
    private final String search_query;

    public SearchNotesTask(DBInterface db, String search_query, @NonNull ReadNotesDBTask.NotesListener listener) {
        this.db = db;
        this.listener = listener;
        this.search_query = search_query;
    }

    @Override
    protected ArrayList<MainModel> doInBackground(MainModel... mainModels) {
        ArrayList<MainModel> res = new ArrayList<>();
        for (MainModel m: mainModels) {
            if (m.isNote()) {
                SingleNote note = (SingleNote) m;
                note.setNotMatched();
                if (!note.wasLoaded()) {
                    note.refreshFromFile(db.getDatabase());
                }

                if (note.searchNoteBody(search_query)) {
                    res.add(note);
                    note.setMatched();
                }

                listener.NoteLoaded(note);
            }
        }

        return res;
    }

    @Override
    protected void onPostExecute(ArrayList<MainModel> list) {
        listener.NotesLoaded(list, Constants.NOTES_LOADED_AND_SEARCHED);
    }
}
