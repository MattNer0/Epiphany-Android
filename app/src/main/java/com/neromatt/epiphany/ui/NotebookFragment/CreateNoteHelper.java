package com.neromatt.epiphany.ui.NotebookFragment;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.view.View;

import com.neromatt.epiphany.model.Path;
import com.neromatt.epiphany.ui.EditorActivity;

public class CreateNoteHelper {

    private Fragment mCallbackFragment;
    private Path path;
    static final int ADD_NOTE_REQUEST = 1;

    public CreateNoteHelper(Fragment fragment, Path path){
        this.mCallbackFragment = fragment;
        this.path = path;
    }

    public void addNote() {
        if (mCallbackFragment.getActivity() == null) return;
        String folderpath = path.getCurrentPath();
        Intent intent = new Intent(mCallbackFragment.getActivity(), EditorActivity.class);
        intent.putExtra("folderPath",folderpath);
        mCallbackFragment.getActivity().startActivityForResult(intent, ADD_NOTE_REQUEST);
    }
}
