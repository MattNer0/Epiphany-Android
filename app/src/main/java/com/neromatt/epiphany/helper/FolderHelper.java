package com.neromatt.epiphany.helper;

import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.model.DataObjects.SingleNotebook;
import com.neromatt.epiphany.ui.MainActivity;
import com.neromatt.epiphany.ui.R;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

public class FolderHelper {

    public static void addFolder(@NonNull final MainActivity ma, final String current_path, final FolderCreatedListener listener) {
        MyDialog dialog = new MyDialog();
        dialog.setDialogListener(new MyDialog.MyDialogListener() {
            @Override
            public void onDialogPositiveClick(MyDialog dialog, String text) {
                String folder_name = text.replaceAll("[^\\w. _-]", "");
                File dir = new File(current_path+"/"+folder_name);
                if (!dir.mkdir()) {
                    Toast.makeText(ma.getContext(), "Couldn't create new folder", Toast.LENGTH_LONG).show();
                    listener.onCreated(false);
                } else {
                    listener.onCreated(true);
                }
                dialog.dismiss();
            }

            @Override
            public void onDialogNegativeClick(MyDialog dialog) {
                listener.onCreated(false);
                dialog.dismiss();
            }
        });
        dialog.show(ma.getSupportFragmentManager(), Constants.DIALOG_CREATE_BUCKET);
    }

    public static void renameFolder(@NonNull final MainActivity ma, final SingleNotebook folder, final FolderRenamedListener listener) {
        MyDialog dialog = new MyDialog();
        Bundle args = new Bundle();
        args.putString("placeholder", folder.getName());
        args.putInt("positive", R.string.rename_notebook);
        dialog.setArguments(args);
        dialog.setDialogListener(new MyDialog.MyDialogListener() {
            @Override
            public void onDialogPositiveClick(MyDialog dialog, String text) {
                if (folder.renameDirectory(text)) {
                    listener.onRenamed(true);
                } else {
                    listener.onRenamed(false);
                }
                dialog.dismiss();
            }

            @Override
            public void onDialogNegativeClick(MyDialog dialog) {
                listener.onRenamed(false);
                dialog.dismiss();
            }
        });
        dialog.show(ma.getSupportFragmentManager(), Constants.DIALOG_CREATE_BUCKET);
    }

    public static void deleteFolder(@NonNull final MainActivity ma, final SingleNotebook folder, final FolderDeletedListener listener) {
        new AlertDialog.Builder(ma)
                .setCancelable(false)
                .setTitle(R.string.dialog_delete_folder_title)
                .setMessage(R.string.dialog_delete_folder_message)
                .setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (folder.delete()) {
                            listener.onDeleted(true);
                        } else {
                            listener.onDeleted(false);
                        }
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.dialog_no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onDeleted(false);
                        dialog.dismiss();
                    }
                })
                .show();
    }

    public interface FolderCreatedListener {
        void onCreated(boolean success);
    }

    public interface FolderRenamedListener {
        void onRenamed(boolean success);
    }

    public interface FolderDeletedListener {
        void onDeleted(boolean success);
    }
}
