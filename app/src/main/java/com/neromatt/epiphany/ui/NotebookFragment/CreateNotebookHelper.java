package com.neromatt.epiphany.ui.NotebookFragment;

import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.ui.MainActivity;

import java.lang.ref.WeakReference;

public class CreateNotebookHelper {

    private WeakReference<MainActivity> mCallbackActivity;
    private WeakReference<NotebookFragment> mCallbackFragment;

    CreateNotebookHelper(NotebookFragment fm) {
        this.mCallbackActivity = new WeakReference<>(fm.getMainActivity());
        this.mCallbackFragment = new WeakReference<>(fm);
    }

    void addNotebook(final MainModel current_folder) {
        if (mCallbackActivity == null || mCallbackActivity.get() == null) return;
        CreateNotebookDialog dialog = new CreateNotebookDialog();
        dialog.setDialogListener(new CreateNotebookDialog.CreateNotebookDialogListener() {
            @Override
            public void onDialogPositiveClick(CreateNotebookDialog dialog, String notebookname) {
                notebookname = notebookname.replaceAll("[^\\w. _-]", "");
                /*mCallbackActivity.get().loadNewFolder(current_folder, notebookname);
                mCallbackFragment.get().reloadAdapter(true);*/
                dialog.dismiss();
            }

            @Override
            public void onDialogNegativeClick(CreateNotebookDialog dialog) {

            }
        });
        dialog.show(mCallbackActivity.get().getSupportFragmentManager(), "CreateNotebookDialogFragment");
    }
}
