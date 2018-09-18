package com.neromatt.epiphany.ui.NotesFragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.neromatt.epiphany.model.Adapters.NotebookAdapter;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.NotesEditListernerInterface;
import com.neromatt.epiphany.model.Path;
import com.neromatt.epiphany.model.SortBy;
import com.neromatt.epiphany.ui.EditorActivity;
import com.neromatt.epiphany.ui.PathSupplier;
import com.neromatt.epiphany.ui.R;

import java.util.ArrayList;

public class NotesFragment extends Fragment implements NotesEditListernerInterface {
    private FloatingActionButton addNoteButton;
    private NotebookAdapter adapter;
    private RecyclerView notesList;
    private Path path;

    static final int ADD_NOTE_REQUEST = 1;

    public static NotesFragment newInstance() {
        NotesFragment f = new NotesFragment();
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_notes, container, false);
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume(){
        super.onResume();
        setHasOptionsMenu(true);
        this.path = ((PathSupplier)getActivity()).getPath();
        getActivity().setTitle(path.getTitle());
        refreshNotes();
    }

    private void refreshNotes(){
        //TODO handle Exception if notelist is null
        View v = getView();
        if (v == null) return;
        ArrayList<MainModel> notes = this.path.getNotes();
        adapter = new NotebookAdapter(notes);
        adapter.sort(SortBy.DATE, 0);
        //adapter.setOnClickListener(new NotesItemClickListener(this));
        //adapter.setCallBack(this); <-------------------------------------
        addNoteButton = v.findViewById(R.id.addNoteButton);
        notesList = v.findViewById(R.id.notesview);
        notesList.setAdapter(adapter);

        //notesList.setOnItemClickListener(new NotesItemClickListener(this,notesList));
        //notesList.setOnCreateContextMenuListener(new NotesItemLongClickListener(this,notesList,path));
        //addNoteButton.setOnClickListener(new CreateNoteHelper(this,path));
    }
    @Override
    public void editNote(String name) {
        String folderpath = path.getCurrentPath();
        Log.d("folderpath",folderpath);
        Log.d("notename",name);
        Intent intent = new Intent(getActivity(), EditorActivity.class);
        intent.putExtra("folderPath",folderpath);
        intent.putExtra("noteName", name);
        startActivity(intent);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == ADD_NOTE_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
             //  refreshNotes();
            }
        }
    }
}
