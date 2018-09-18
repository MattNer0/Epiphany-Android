package com.neromatt.epiphany.ui.RackFragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;

import com.neromatt.epiphany.model.Path;

public class CreateRackListener implements AdapterView.OnClickListener, CreateRackDialog.CreateRackDialogListener {

    private AppCompatActivity activity;
    private Path path;

    public CreateRackListener(AppCompatActivity activity, Path path){
        this.activity = activity;
        this.path = path;
    }

    @Override
    public void onClick(View view) {
        addRack();
    }

    public void addRack() {
        CreateRackDialog dialog = new CreateRackDialog();
        dialog.setDialogListener(this);
        dialog.show(activity.getSupportFragmentManager(), "CreateNotebookDialogFragment");
    }

    @Override
    public void onDialogPositiveClick(CreateRackDialog dialog, String text) {
        if (text.length() < 128 && text.matches("^[\\w\\s]+$")) {
            path.createRack(text);
            ((RackFragmentCallback) activity).refreshRackDrawer();
        } else {
            AlertDialog.Builder builder;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder = new AlertDialog.Builder(activity, android.R.style.Theme_Material_Dialog_Alert);
            } else {
                builder = new AlertDialog.Builder(activity);
            }
            builder.setTitle("Error")
                .setMessage("Invalid Bucket Name")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
        }
    }

    @Override
    public void onDialogNegativeClick(CreateRackDialog dialog) {

    }
}
