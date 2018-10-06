package com.neromatt.epiphany.ui.NotebookFragment;

import android.support.v4.app.Fragment;

import com.neromatt.epiphany.model.Path;

public class CreateNotebookHelper implements CreateNotebookDialog.CreateNotebookDialogListener{

    private Fragment mCallbackFragment;
    private Path path;

    CreateNotebookHelper(Fragment fragment, Path path) {
        this.path = path;
        this.mCallbackFragment = fragment;
    }

    public void addNotebook() {
        if (mCallbackFragment.getActivity() == null) return;
        CreateNotebookDialog dialog = new CreateNotebookDialog();
        dialog.setDialogListener(this);
        dialog.show(mCallbackFragment.getActivity().getSupportFragmentManager(), "CreateNotebookDialogFragment");
    }

    @Override
    public void onDialogPositiveClick(CreateNotebookDialog dialog, String notebookname) {
        path.createNotebook(notebookname);
        ((NotebookFragment) mCallbackFragment).refreshNotebooks();
    }

    @Override
    public void onDialogNegativeClick(CreateNotebookDialog dialog) {

    }
}
