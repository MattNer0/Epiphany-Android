package com.neromatt.epiphany.ui.NotebookFragment;

import android.support.v4.app.Fragment;

import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.DataObjects.SingleRack;
import com.neromatt.epiphany.model.Path;
import com.neromatt.epiphany.ui.MainActivity;

public class CreateBucketHelper implements CreateNotebookDialog.CreateNotebookDialogListener{

    private MainActivity mCallbackActivity;

    CreateBucketHelper(MainActivity ma) {
        this.mCallbackActivity = ma;
    }

    public void addBucket() {
        if (mCallbackActivity == null) return;
        CreateNotebookDialog dialog = new CreateNotebookDialog();
        dialog.setDialogListener(this);
        dialog.show(mCallbackActivity.getSupportFragmentManager(), "CreateNotebookDialogFragment");
    }

    @Override
    public void onDialogPositiveClick(CreateNotebookDialog dialog, String notebookname) {
        notebookname = notebookname.replaceAll("[^\\w. _-]", "");
        mCallbackActivity.loadNewBucket(notebookname);
        dialog.dismiss();
    }

    @Override
    public void onDialogNegativeClick(CreateNotebookDialog dialog) {

    }
}
