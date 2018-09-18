package com.neromatt.epiphany.ui.NotebookFragment;

import android.content.Context;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.neromatt.epiphany.model.Adapters.NotebookAdapter;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.Path;
import com.neromatt.epiphany.model.SortBy;
import com.neromatt.epiphany.ui.PathSupplier;
import com.neromatt.epiphany.ui.R;

import java.util.ArrayList;
import java.util.Locale;

import io.github.kobakei.materialfabspeeddial.FabSpeedDial;
import io.github.kobakei.materialfabspeeddial.FabSpeedDialMenu;

public class NotebookFragment extends Fragment implements NotebookFragmentCallback  {
    private FabSpeedDial addNotebookButton;
    private NotebookAdapter adapter = null;
    private CreateNotebookHelper mCreateNotebookHelper;
    private CreateNoteHelper mCreateNoteHelper;

    private Path path;

    private static final int FAB_MENU_NEW_FOLDER = 1;
    private static final int FAB_MENU_NEW_NOTE = 2;

    public static NotebookFragment newInstance() {
        NotebookFragment f = new NotebookFragment();
        Bundle args = new Bundle();
        f.setArguments(args);
        return f;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_notebook, container, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        setHasOptionsMenu(true);

        if (getActivity() == null) return;
        path = ((PathSupplier) getActivity()).getPath();
        refreshNotebooks();
    }

    public void refreshNotebooks() {
        View v = getView();
        if (v == null) return;
        if (getActivity() == null || getContext() == null) return;

        getActivity().setTitle(path.getTitle());

        ArrayList<MainModel> notebooks = path.getFoldersAndNotes();

        if (adapter == null) {
            adapter = new NotebookAdapter(notebooks);

            String note_order = PreferenceManager.getDefaultSharedPreferences(getContext()).getString("pref_note_order", "0");
            adapter.sort(SortBy.NAME, Integer.parseInt(note_order));

            adapter.setOnClickListener(new NotebookItemClickListener(this, path));

            RecyclerView notebookList = v.findViewById(R.id.notebookview);
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
            notebookList.setLayoutManager(mLayoutManager);
            notebookList.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

            notebookList.setAdapter(adapter);

            mCreateNotebookHelper = new CreateNotebookHelper(this, path);
            mCreateNoteHelper = new CreateNoteHelper(this, path);

            addNotebookButton = v.findViewById(R.id.notebookFab);
            addNotebookButton.addOnMenuItemClickListener(new FabSpeedDial.OnMenuItemClickListener() {
                @Override
                public void onMenuItemClick(FloatingActionButton fab, TextView textView, int itemId) {
                    switch (itemId) {
                        case FAB_MENU_NEW_FOLDER:
                            Log.i("menu", "new folder");
                            mCreateNotebookHelper.addNotebook();
                            break;
                        case FAB_MENU_NEW_NOTE:
                            Log.i("menu", "new note");
                            mCreateNoteHelper.addNote();
                            break;
                    }
                }
            });

        } else {
            adapter.updateList(notebooks);
            adapter.notifyDataSetChanged();
        }

        if (path.isRack()) {
            FabSpeedDialMenu menu = new FabSpeedDialMenu(getContext());
            menu.add(1,FAB_MENU_NEW_FOLDER,0, "New Folder").setIcon(R.drawable.ic_action_add);
            addNotebookButton.setMenu(menu);
        } else {
            FabSpeedDialMenu menu = new FabSpeedDialMenu(getContext());
            menu.add(1,FAB_MENU_NEW_FOLDER,0, "New Folder").setIcon(R.drawable.ic_action_add);
            menu.add(1,FAB_MENU_NEW_NOTE,0, "New Note").setIcon(R.drawable.ic_action_add);
            addNotebookButton.setMenu(menu);
        }
    }
}

interface NotebookFragmentCallback {
    void refreshNotebooks();
}