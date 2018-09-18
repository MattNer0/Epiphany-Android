package com.neromatt.epiphany.ui.NotebookFragment;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.ViewGroup;

import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.DataObjects.SingleNote;
import com.neromatt.epiphany.model.Path;
import com.neromatt.epiphany.ui.ViewNote;

public class NotebookItemClickListener {

    private NotebookFragment mCallbackFragment;
    private Path path;

    public NotebookItemClickListener() { }

    public NotebookItemClickListener(NotebookFragment notebookFragment, Path path) {
        mCallbackFragment = notebookFragment;
        this.path = path;
    }

    public void onClick(MainModel notebook) {
        FragmentActivity ma = mCallbackFragment.getActivity();
        View view = mCallbackFragment.getView();
        if (ma == null || view == null) return;

        String selectedModelName = notebook.getName();

        if (notebook.getType() == MainModel.TYPE_FOLDER) {
            FragmentManager fm = ma.getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fm.beginTransaction();
            NotebookFragment fragment = NotebookFragment.newInstance();
            this.path.goForward(selectedModelName);
            fragmentTransaction.replace(((ViewGroup) view.getParent()).getId(), fragment, "Notebook_Fragment");
            fragmentTransaction.commit();
        } else if (notebook.getType() == MainModel.TYPE_MARKDOWN_NOTE) {
            String path = ((SingleNote) notebook).getFullPath();
            Intent intent = new Intent(ma, ViewNote.class);
            intent.putExtra("notePath", path);
            ma.startActivity(intent);
        }
    }
}
