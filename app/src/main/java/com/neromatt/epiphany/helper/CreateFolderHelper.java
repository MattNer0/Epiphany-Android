package com.neromatt.epiphany.helper;

import android.widget.Toast;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.ui.MainActivity;

import java.io.File;

import androidx.annotation.NonNull;

public class CreateFolderHelper {

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

    public interface FolderCreatedListener {
        void onCreated(boolean success);
    }
}
