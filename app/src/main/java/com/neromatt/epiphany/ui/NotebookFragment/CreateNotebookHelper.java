package com.neromatt.epiphany.ui.NotebookFragment;

import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.ui.MainActivity;

public class CreateNotebookHelper {

    private MainActivity mCallbackActivity;

    CreateNotebookHelper(MainActivity ma) {
        this.mCallbackActivity = ma;
    }

    public void addNotebook(final MainModel current_folder) {
        if (mCallbackActivity == null) return;
        CreateNotebookDialog dialog = new CreateNotebookDialog();
        dialog.setDialogListener(new CreateNotebookDialog.CreateNotebookDialogListener() {
            @Override
            public void onDialogPositiveClick(CreateNotebookDialog dialog, String notebookname) {
                notebookname = notebookname.replaceAll("[^\\w. _-]", "");
                mCallbackActivity.loadNewFolder(current_folder, notebookname);
                dialog.dismiss();
            }

            @Override
            public void onDialogNegativeClick(CreateNotebookDialog dialog) {

            }
        });
        dialog.show(mCallbackActivity.getSupportFragmentManager(), "CreateNotebookDialogFragment");
    }
}
