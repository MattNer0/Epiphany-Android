package com.neromatt.epiphany.tasks;

import android.os.AsyncTask;
import android.os.Bundle;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.helper.DBInterface;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.DataObjects.SingleNote;

import java.util.ArrayList;

import androidx.annotation.NonNull;

public class ReadRecentNotesTask extends AsyncTask<Void, Void, ArrayList<MainModel>> {

    private final ReadNotesDBTask.NotesListener listener;
    private final DBInterface db;

    public ReadRecentNotesTask(DBInterface db, @NonNull ReadNotesDBTask.NotesListener listener) {
        this.db = db;
        this.listener = listener;
    }

    @Override
    protected ArrayList<MainModel> doInBackground(Void... voids) {
        ArrayList<MainModel> res = new ArrayList<>();
        ArrayList<Bundle> bundles = db.getDatabase().getRecentNotes();
        for (Bundle b: bundles) {
            SingleNote new_note = new SingleNote(b.getString(Constants.KEY_NOTE_PATH), b.getString(Constants.KEY_NOTE_FILENAME));
            SingleNote.updateObjAfterReadingContent(new_note, b, false);
            res.add(new_note);
        }

        return res;
    }

    @Override
    protected void onPostExecute(ArrayList<MainModel> list) {
        listener.NotesLoaded(list, Constants.NOTES_LOADED_INTO_DATABASE);
    }
}
