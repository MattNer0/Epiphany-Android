package com.neromatt.epiphany.helper;

import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.model.DataObjects.SingleRack;
import com.neromatt.epiphany.ui.MainActivity;
import com.neromatt.epiphany.ui.R;

import java.io.File;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

public class BucketHelper {

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

    public static void renameBucket(@NonNull final MainActivity ma, final SingleRack folder, final BucketRenamedListener listener) {
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

    public static void changeColorBucket(@NonNull final MainActivity ma, final SingleRack folder, final BucketRenamedListener listener) {
        MyDialog dialog = new MyDialog();
        Bundle args = new Bundle();
        String current_color = folder.getColor().toLowerCase();
        if (current_color.length() == 9 && current_color.startsWith("#ff")) {
            current_color = current_color.replace("#ff", "#");
        }
        args.putString("placeholder", current_color);

        HashMap<String, String> material_colors = new HashMap<>();
        material_colors.put("red", "#e51c23");
        material_colors.put("pink", "#e91e63");
        material_colors.put("purple", "#9c27b0");
        material_colors.put("deep_purple", "#673ab7");
        material_colors.put("indigo", "#3f51b5");
        material_colors.put("blue", "#5677fc");
        material_colors.put("light_blue", "#03a9f4");
        material_colors.put("cyan", "#00bcd4");
        material_colors.put("teal", "#009688");
        material_colors.put("green", "#259b24");
        material_colors.put("light_green", "#8bc34a");
        material_colors.put("lime", "#cddc39");
        material_colors.put("yellow", "#ffeb3b");
        material_colors.put("orange", "#ff9800");
        material_colors.put("deep_orange", "#ff5722");
        material_colors.put("brown", "#795548");
        material_colors.put("grey", "#9e9e9e");
        material_colors.put("blue_grey", "#607d8b");

        args.putSerializable("select", material_colors);
        args.putInt("positive", R.string.save);
        dialog.setArguments(args);
        dialog.setDialogListener(new MyDialog.MyDialogListener() {
            @Override
            public void onDialogPositiveClick(MyDialog dialog, String text) {
                folder.setColor(text);
                folder.saveMeta();
                listener.onRenamed(true);
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

    public static void deleteBucket(@NonNull final MainActivity ma, final SingleRack folder, final BucketDeletedListener listener) {
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

    public interface BucketCreatedListener {
        void onCreated(boolean success);
    }

    public interface BucketRenamedListener {
        void onRenamed(boolean success);
    }

    public interface BucketDeletedListener {
        void onDeleted(boolean success);
    }
}
