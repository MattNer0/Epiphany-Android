package com.neromatt.epiphany.ui.NotebookFragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.Path;
import com.neromatt.epiphany.ui.EditorActivity;

public class CreateNoteHelper {

    private Fragment mCallbackFragment;
    private Path path;

    CreateNoteHelper(Fragment fragment, Path path) {
        this.mCallbackFragment = fragment;
        this.path = path;
    }

    boolean addQuickNote(String initial_text, OnQuickPathListener mOnQuickPathListener) {
        if (mCallbackFragment.getActivity() == null) return false;
        Bundle quick_bundle = path.getQuickNotesPath();

        String folder_path = quick_bundle.getString("path", "");
        Boolean created_folder = quick_bundle.getBoolean("created_folder", false);
        Boolean created_bucket = quick_bundle.getBoolean("created_bucket", false);

        if (folder_path != null && !folder_path.isEmpty()) {

            if ((created_bucket || created_folder) && mOnQuickPathListener != null) {
                mOnQuickPathListener.QuickPathCreated(quick_bundle);
            }

            Intent intent = new Intent(mCallbackFragment.getActivity(), EditorActivity.class);
            intent.putExtra("folder", folder_path);
            initial_text = "# New Quick Note\n\n" + initial_text;
            intent.putExtra("body", initial_text);
            intent.putExtra("root", path.getRootPath());
            mCallbackFragment.getActivity().startActivityForResult(intent, Constants.NEW_QUICK_NOTE_REQUEST_CODE);
            return true;
        }

        return false;
    }

    void addNote(MainModel current_folder) {
        if (mCallbackFragment.getActivity() == null) return;
        String folder_path = current_folder.getPath();
        Intent intent = new Intent(mCallbackFragment.getActivity(), EditorActivity.class);
        intent.putExtra("folder", folder_path);
        intent.putExtra("root", path.getRootPath());
        mCallbackFragment.getActivity().startActivityForResult(intent, Constants.NEW_NOTE_REQUEST_CODE);
    }

    public interface OnQuickPathListener {
        void QuickPathCreated(Bundle quick_bundle);
    }
}
