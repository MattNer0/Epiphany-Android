package com.neromatt.epiphany.ui.NotebookFragment;

import android.content.Intent;
import android.os.Bundle;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.DataObjects.SingleNote;
import com.neromatt.epiphany.model.Path;
import com.neromatt.epiphany.ui.EditorActivity;

import java.lang.ref.WeakReference;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

public class CreateNoteHelper {

    private WeakReference<FragmentActivity> currentActivity;
    private Path path;

    public CreateNoteHelper(Fragment fragment, Path path) {
        this.currentActivity = new WeakReference<>(fragment.getActivity());
        this.path = path;
    }

    public CreateNoteHelper(FragmentActivity ma, Path path) {
        this.currentActivity = new WeakReference<>(ma);
        this.path = path;
    }

    private FragmentActivity getActivity() {
        return this.currentActivity.get();
    }

    public boolean addQuickNote(String initial_text, OnQuickPathListener mOnQuickPathListener) {
        if (getActivity() == null) return false;
        Bundle quick_bundle = path.getQuickNotesPath();

        String folder_path = quick_bundle.getString("path", "");
        Boolean created_folder = quick_bundle.getBoolean("created_folder", false);
        Boolean created_bucket = quick_bundle.getBoolean("created_bucket", false);

        if (folder_path != null && !folder_path.isEmpty()) {

            if ((created_bucket || created_folder) && mOnQuickPathListener != null) {
                mOnQuickPathListener.QuickPathCreated(quick_bundle);
            }

            Intent intent = new Intent(getActivity(), EditorActivity.class);
            intent.putExtra("folder", folder_path);
            initial_text = "# New Quick Note\n\n" + initial_text;
            intent.putExtra("body", initial_text);
            intent.putExtra("root", path.getRootPath());
            getActivity().startActivityForResult(intent, Constants.NEW_QUICK_NOTE_REQUEST_CODE);
            return true;
        }

        return false;
    }

    public boolean addQuickNoteAndSave(String initial_text, OnQuickPathListener mOnQuickPathListener, final OnQUickNoteSaved mOnNoteSavedListener) {
        if (getActivity() == null) return false;
        Bundle quick_bundle = path.getQuickNotesPath();

        String folder_path = quick_bundle.getString("path", "");
        Boolean created_folder = quick_bundle.getBoolean("created_folder", false);
        Boolean created_bucket = quick_bundle.getBoolean("created_bucket", false);

        if (folder_path != null && !folder_path.isEmpty()) {

            if ((created_bucket || created_folder) && mOnQuickPathListener != null) {
                mOnQuickPathListener.QuickPathCreated(quick_bundle);
            }

            final SingleNote note = new SingleNote(folder_path, Path.newNoteName(folder_path, "md"));
            initial_text = "# New Quick Note\n\n" + initial_text;
            note.markAsNewFile();
            note.updateBody(initial_text);
            note.saveNote(new SingleNote.OnNoteSavedListener() {
                @Override
                public void NoteSaved(boolean saved) {
                    mOnNoteSavedListener.QuickNoteSaved(note);
                }
            });
            return true;
        }

        return false;
    }

    public void addNote(MainModel current_folder) {
        if (getActivity() == null) return;
        String folder_path = current_folder.getPath();
        Intent intent = new Intent(getActivity(), EditorActivity.class);
        intent.putExtra("folder", folder_path);
        intent.putExtra("root", path.getRootPath());
        getActivity().startActivityForResult(intent, Constants.NEW_NOTE_REQUEST_CODE);
    }

    public interface OnQuickPathListener {
        void QuickPathCreated(Bundle quick_bundle);
    }

    public interface OnQUickNoteSaved {
        void QuickNoteSaved(SingleNote note);
    }
}
