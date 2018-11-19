package com.neromatt.epiphany.helper;

import android.content.Context;
import android.content.DialogInterface;

import com.neromatt.epiphany.model.DataObjects.SingleNote;
import com.neromatt.epiphany.ui.R;

import androidx.appcompat.app.AlertDialog;

public class DeleteNoteHelper {

    public static void deleteNote(Context context, final SingleNote note, final OnNoteDeleteListener listener) {
        if (context == null || note == null) return;
        new AlertDialog.Builder(context)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.dialog_delete_note_title)
                .setMessage(R.string.dialog_delete_note_message)
                .setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.NoteDeleted(note.delete());
                    }
                })
                .setNegativeButton(R.string.dialog_no, null)
                .show();
    }

    public interface OnNoteDeleteListener {
        void NoteDeleted(boolean deleted);
    }
}
