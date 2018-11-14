package com.neromatt.epiphany.ui.NotebookFragment;

import com.neromatt.epiphany.ui.MainActivity;

import java.lang.ref.WeakReference;

public class CreateBucketHelper implements CreateNotebookDialog.CreateNotebookDialogListener{

    private WeakReference<MainActivity> mCallbackActivity;
    private WeakReference<NotebookFragment> mCallbackFragment;

    CreateBucketHelper(NotebookFragment fm) {
        this.mCallbackActivity = new WeakReference<>(fm.getMainActivity());
        this.mCallbackFragment = new WeakReference<>(fm);
    }

    void addBucket() {
        if (mCallbackActivity == null || mCallbackActivity.get() == null) return;
        CreateNotebookDialog dialog = new CreateNotebookDialog();
        dialog.setDialogListener(this);
        dialog.show(mCallbackActivity.get().getSupportFragmentManager(), "CreateNotebookDialogFragment");
    }

    @Override
    public void onDialogPositiveClick(CreateNotebookDialog dialog, String notebookname) {
        notebookname = notebookname.replaceAll("[^\\w. _-]", "");
        //mCallbackActivity.get().loadNewBucket(notebookname);
        //mCallbackFragment.get().reloadAdapter(true);
        dialog.dismiss();
    }

    @Override
    public void onDialogNegativeClick(CreateNotebookDialog dialog) {

    }
}
