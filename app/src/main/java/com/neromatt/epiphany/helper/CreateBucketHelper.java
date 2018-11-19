package com.neromatt.epiphany.helper;

import android.widget.Toast;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.ui.MainActivity;

import java.io.File;

import androidx.annotation.NonNull;

public class CreateBucketHelper {

    public static void addBucket(@NonNull final MainActivity ma, final String root_path, final BucketCreatedListener listener) {
        MyDialog dialog = new MyDialog();
        dialog.setDialogListener(new MyDialog.MyDialogListener() {
            @Override
            public void onDialogPositiveClick(MyDialog dialog, String text) {
                String bucket_name = text.replaceAll("[^\\w. _-]", "");
                File dir = new File(root_path+"/"+bucket_name);
                if (!dir.mkdir()) {
                    Toast.makeText(ma.getContext(), "Couldn't create new bucket", Toast.LENGTH_LONG).show();
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

    public interface BucketCreatedListener {
        void onCreated(boolean success);
    }
}
