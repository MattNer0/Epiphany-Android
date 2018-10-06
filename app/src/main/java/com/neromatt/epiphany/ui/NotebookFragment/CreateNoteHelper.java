package com.neromatt.epiphany.ui.NotebookFragment;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.view.View;

import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.Path;
import com.neromatt.epiphany.ui.EditorActivity;

public class CreateNoteHelper {

    private Fragment mCallbackFragment;
    private Path path;
    static final int ADD_NOTE_REQUEST = 1;

    CreateNoteHelper(Fragment fragment, Path path) {
        this.mCallbackFragment = fragment;
        this.path = path;
    }

    boolean addQuickNote(String initial_text) {
        if (mCallbackFragment.getActivity() == null) return false;
        String folder_path = path.getQuickNotesPath();
        if (folder_path != null) {
            Intent intent = new Intent(mCallbackFragment.getActivity(), EditorActivity.class);
            intent.putExtra("folder", folder_path);
            intent.putExtra("body", initial_text);
            mCallbackFragment.getActivity().startActivityForResult(intent, ADD_NOTE_REQUEST);
            return true;
        }

        return false;
    }

    void addNote(MainModel current_folder) {
        if (mCallbackFragment.getActivity() == null) return;
        String folder_path = current_folder.getPath();
        Intent intent = new Intent(mCallbackFragment.getActivity(), EditorActivity.class);
        intent.putExtra("folder", folder_path);
        mCallbackFragment.getActivity().startActivityForResult(intent, ADD_NOTE_REQUEST);
    }
}
