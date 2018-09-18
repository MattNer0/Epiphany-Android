package com.neromatt.epiphany.ui.NotesFragment;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.neromatt.epiphany.model.Adapters.NotebookAdapter;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.DataObjects.SingleNote;
import com.neromatt.epiphany.model.DataObjects.SingleNotebook;
import com.neromatt.epiphany.ui.NotebookFragment.NotebookItemClickListener;
import com.neromatt.epiphany.ui.ViewNote;

public class NotesItemClickListener extends NotebookItemClickListener {

    private NotesFragment mCallback;

    public NotesItemClickListener(NotesFragment fragment){
        this.mCallback = fragment;
    }

    @Override
    public void onClick(MainModel object) {
        String path = ((SingleNote) object).getFullPath();
        Intent intent = new Intent(mCallback.getActivity(), ViewNote.class);
        intent.putExtra("notePath", path);
        mCallback.getActivity().startActivity(intent);
    }
}
