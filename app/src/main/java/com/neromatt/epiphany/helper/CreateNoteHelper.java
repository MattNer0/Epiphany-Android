package com.neromatt.epiphany.helper;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.model.DataObjects.SingleNote;
import com.neromatt.epiphany.model.Path;
import com.neromatt.epiphany.ui.EditorActivity;
import com.neromatt.epiphany.ui.MainActivity;

import java.io.File;

import androidx.fragment.app.FragmentActivity;

public class CreateNoteHelper {

    private static Bundle getQuickNotesPath(String root_path) {
        Bundle ret = new Bundle();
        boolean created_folder = false;
        boolean created_bucket = false;
        String bucket_quick_path = root_path+"/"+Constants.QUICK_NOTES_BUCKET;
        String quick_path = bucket_quick_path+"/New Notes";
        File b = new File(bucket_quick_path);
        File f = new File(quick_path);
        if (!f.exists()) {
            if (!b.exists()) {
                created_bucket = true;
            }
            created_folder = true;

            if (!f.mkdirs()) {
                quick_path = "";
                created_folder = false;
                created_bucket = false;
            }
        }

        ret.putString("path", quick_path);
        ret.putBoolean("created_folder", created_folder);
        ret.putBoolean("created_bucket", created_bucket);
        return ret;
    }

    public static boolean addQuickNote(FragmentActivity ma, String root_path, String initial_text) {
        return addQuickNote(ma, root_path, initial_text, null);
    }

    public static boolean addQuickNote(FragmentActivity ma, String root_path, String initial_text, OnQuickPathListener mOnQuickPathListener) {
        Bundle quick_bundle = getQuickNotesPath(root_path);

        String folder_path = quick_bundle.getString("path", "");
        Boolean created_folder = quick_bundle.getBoolean("created_folder", false);
        Boolean created_bucket = quick_bundle.getBoolean("created_bucket", false);

        if (folder_path != null && !folder_path.isEmpty()) {

            if ((created_bucket || created_folder) && mOnQuickPathListener != null) {
                mOnQuickPathListener.QuickPathCreated(quick_bundle);
            }

            Intent intent = new Intent(ma, EditorActivity.class);
            intent.putExtra("folder", folder_path);
            initial_text = "# New Quick Note\n\n" + initial_text;
            intent.putExtra("body", initial_text);
            intent.putExtra("root", root_path);
            ma.startActivityForResult(intent, Constants.NEW_QUICK_NOTE_REQUEST_CODE);
            return true;
        }

        return false;
    }

    public static boolean addQuickNoteAndSave(String root_path, String title, String text, OnQuickPathListener mOnQuickPathListener, final OnQUickNoteSaved mOnNoteSavedListener) {
        Bundle quick_bundle = getQuickNotesPath(root_path);

        String folder_path = quick_bundle.getString("path", "");
        Boolean created_folder = quick_bundle.getBoolean("created_folder", false);
        Boolean created_bucket = quick_bundle.getBoolean("created_bucket", false);

        String note_body;
        if (title == null) {
            note_body = "# New Quick Note\n\n" + text;
        } else {
            note_body = "# "+title.trim()+"\n\n" + text;
        }

        if (folder_path != null && !folder_path.isEmpty()) {

            if ((created_bucket || created_folder) && mOnQuickPathListener != null) {
                mOnQuickPathListener.QuickPathCreated(quick_bundle);
            }

            final SingleNote note = new SingleNote(folder_path, Path.newNoteName(folder_path, "md"));
            note.markAsNewFile();
            note.updateBody(note_body);

            if (Patterns.WEB_URL.matcher(text.toLowerCase()).matches()) {
                note.addMetadata(Constants.METATAG_WEB, text.toLowerCase());
            }
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

    /*void addNote(String folder_path) {
        if (getActivity() == null) return;
        Intent intent = new Intent(getActivity(), EditorActivity.class);
        intent.putExtra("folder", folder_path);
        intent.putExtra("root", path.getRootPath());
        getActivity().startActivityForResult(intent, Constants.NEW_NOTE_REQUEST_CODE);
    }*/

    public interface OnQuickPathListener {
        void QuickPathCreated(Bundle quick_bundle);
    }

    public interface OnQUickNoteSaved {
        void QuickNoteSaved(SingleNote note);
    }
}
