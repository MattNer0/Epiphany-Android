package com.neromatt.epiphany.ui.NotebookFragment;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.model.Adapters.MainAdapter;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.DataObjects.SingleNote;
import com.neromatt.epiphany.model.DataObjects.SingleNotebook;
import com.neromatt.epiphany.model.DataObjects.SingleRack;
import com.neromatt.epiphany.model.NotebooksComparator;
import com.neromatt.epiphany.model.Path;
import com.neromatt.epiphany.ui.EditorActivity;
import com.neromatt.epiphany.ui.MainActivity;
import com.neromatt.epiphany.ui.PathSupplier;
import com.neromatt.epiphany.ui.R;
import com.neromatt.epiphany.ui.ViewNote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Stack;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import io.github.kobakei.materialfabspeeddial.FabSpeedDial;
import io.github.kobakei.materialfabspeeddial.FabSpeedDialMenu;

import static android.app.Activity.RESULT_OK;

public class NotebookFragment extends Fragment implements FlexibleAdapter.OnItemClickListener, FlexibleAdapter.OnItemLongClickListener {
    private FabSpeedDial addNotebookButton;
    private RecyclerView notebookList;
    private MainAdapter<MainModel> adapter;
    private CreateNotebookHelper mCreateNotebookHelper;
    private CreateNoteHelper mCreateNoteHelper;
    private CreateBucketHelper mCreateBucketHelper;

    private ArrayList<MainModel> library_list;
    private Stack<MainModel> history_list;
    private LinkedList<MainModel> bucket_queue;
    private MainModel current_model;

    private Path path;

    private static final int FAB_MENU_NEW_BUCKET = 0;
    private static final int FAB_MENU_NEW_FOLDER = 1;
    private static final int FAB_MENU_NEW_NOTE = 2;

    public static NotebookFragment newInstance(ArrayList<MainModel> contents) {
        NotebookFragment f = new NotebookFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList("contents", contents);
        f.setArguments(args);
        return f;
    }

    public static NotebookFragment newInstance(ArrayList<MainModel> contents, MainModel current) {
        NotebookFragment f = new NotebookFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList("contents", contents);
        args.putParcelable("current", current);
        f.setArguments(args);
        return f;
    }

    public NotebookFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_notebook, container, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        current_model = null;
        if (args == null) {
            library_list = new ArrayList<>();
            return;
        }

        if (args.containsKey("current")) {
            current_model = args.getParcelable("current");
        }

        library_list = args.getParcelableArrayList("contents");

        //FlexibleAdapter.enableLogs(eu.davidea.flexibleadapter.utils.Log.Level.DEBUG);
    }

    @Override
    public void onResume() {
        super.onResume();
        setHasOptionsMenu(true);

        if (getActivity() == null) return;
        if (adapter != null) adapter.clear();

        if (current_model != null) current_model.sortContents(getContext());

        refreshNotebooks();
    }

    private void sortNotebooks(ArrayList<MainModel> items) {
        Collections.sort(items, new NotebooksComparator(getContext()));
    }

    public ArrayList<MainModel> getCurrentList() {
        if (current_model != null) {
            return current_model.getContent();
        } else {
            return library_list;
        }
    }

    public MainModel getCurrentModel() {
        return current_model;
    }

    public void refreshNotebooks(MainModel current) {
        if (current != null) {
            this.current_model = current;
            current.loadNotes(getContext(), new MainModel.OnModelLoadedListener() {
                @Override
                public void ModelLoaded() {
                }
            });

            Log.i("log", current.getNotesCount()+" -");

            refreshNotebooks(current.getContent());
        }
    }

    public void refreshNotebooks() {
        if (getActivity() == null || getContext() == null) return;

        path = ((PathSupplier) getActivity()).getPath();
        if (!path.isRoot()) {
            path.resetPath();
        }

        refreshNotebooks(getCurrentList());
    }

    public void moveInto(MainModel model) {
        if (current_model != null) {
            if (history_list == null) history_list = new Stack<>();
            history_list.push(current_model);
        }

        current_model = model;
        refreshNotebooks(getCurrentList());
    }

    public boolean moveBack() {
        if (history_list != null && history_list.size() > 0) {
            current_model = history_list.pop();
            refreshNotebooks(getCurrentList());
            return true;
        } else if (current_model != null) {
            current_model = null;
            refreshNotebooks(getCurrentList());
            return true;
        }

        return false;
    }

    public void refreshFolder() {
        if (current_model != null) {
            current_model.clearContent();
            current_model.loadContent(getContext(), new MainModel.OnModelLoadedListener() {
                @Override
                public void ModelLoaded() {
                    current_model.loadNotes(getContext(), new MainModel.OnModelLoadedListener() {
                        @Override
                        public void ModelLoaded() {
                            Log.i("log", "notes refreshed");
                            refreshNotebooks(getCurrentList());
                        }
                    });
                }
            });
        }
    }

    public void refreshNotes() {
        if (current_model != null) {
            current_model.reloadNotes(true, getContext(), new MainModel.OnModelLoadedListener() {
                @Override
                public void ModelLoaded() {
                    Log.i("log", "notes refreshed");
                    refreshNotebooks(getCurrentList());
                }
            });
        }
    }

    public void updateLayoutList(boolean staggered) {
        if (notebookList != null) notebookList.setLayoutManager(getLayoutManager(staggered));
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private RecyclerView.LayoutManager getLayoutManager(boolean staggered) {
        if (getActivity() == null) return null;

        if (staggered) {
            if (adapter != null) adapter.setSpanCount(2);
            return new StaggeredGridLayoutManager(2, 1);
        }
        if (adapter != null) adapter.setSpanCount(1);
        return new StaggeredGridLayoutManager(1, 1);
        //return new SmoothScrollGridLayoutManager(getActivity(), 1);
    }

    public void refreshNotebooks(ArrayList<MainModel> items) {
        View v = getView();
        if (v == null) return;
        if (getActivity() == null || getContext() == null) return;

        if (current_model != null) {
            getActivity().setTitle(current_model.getTitle());
        } else {
            getActivity().setTitle("Library");
        }

        for (MainModel m: items) {
            m.loadNotes(getContext(), new MainModel.OnModelLoadedListener() {
                @Override
                public void ModelLoaded() {

                }
            });
        }

        notebookList = v.findViewById(R.id.notebookview);

        if (adapter == null) {

            FlexibleAdapter.useTag("adpt");
            adapter = new MainAdapter<>(items);
            adapter.addListener(this);
            adapter.setAnimationEntryStep(true);

            adapter.setStickyHeaderElevation(5);
            adapter.setDisplayHeadersAtStartUp(true);
            adapter.setStickyHeaders(true);
            /*adapter.setAnimationOnForwardScrolling(true);
            adapter.setAnimationOnReverseScrolling(true);
            adapter.setAnimationEntryStep(true);
            adapter.setAnimationDuration(1000);*/

            notebookList.setItemViewCacheSize(0);
            notebookList.setLayoutManager(getLayoutManager(
                    PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("pref_staggered_layout", false))
            );
            notebookList.setAdapter(adapter);
            notebookList.setHasFixedSize(true);

            /*FlexibleItemDecoration mItemDecoration = new FlexibleItemDecoration(getActivity())
                    .withOffset(3)
                    .withTopEdge(true)
                    .withBottomEdge(true);

            notebookList.addItemDecoration(mItemDecoration);*/


            mCreateBucketHelper = new CreateBucketHelper((MainActivity) getActivity());
            mCreateNotebookHelper = new CreateNotebookHelper((MainActivity) getActivity());
            mCreateNoteHelper = new CreateNoteHelper(this, path);

            addNotebookButton = v.findViewById(R.id.notebookFab);
            addNotebookButton.addOnMenuItemClickListener(new FabSpeedDial.OnMenuItemClickListener() {
                @Override
                public void onMenuItemClick(FloatingActionButton fab, TextView textView, int itemId) {
                    switch (itemId) {
                        case FAB_MENU_NEW_BUCKET:
                            Log.i("menu", "new bucket");
                            mCreateBucketHelper.addBucket();
                        case FAB_MENU_NEW_FOLDER:
                            Log.i("menu", "new folder");
                            mCreateNotebookHelper.addNotebook(current_model);
                            break;
                        case FAB_MENU_NEW_NOTE:
                            Log.i("menu", "new note");
                            if (current_model != null) mCreateNoteHelper.addNote(current_model);
                            break;
                    }
                }
            });

        } else {
            adapter.updateDataSet(items);
        }

        EditText quickNoteEdit = v.findViewById(R.id.quickNoteEdit);
        if (quickNoteEdit != null) {
            quickNoteEdit.clearFocus();
            quickNoteEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText quickNoteEdit = v.findViewById(R.id.quickNoteEdit);
                    MainActivity ma = (MainActivity) getActivity();
                    mCreateNoteHelper.addQuickNote(quickNoteEdit.getText().toString(), ma);
                }
            });
            quickNoteEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    EditText quickNoteEdit = v.findViewById(R.id.quickNoteEdit);

                    if (hasFocus) {
                        MainActivity ma = (MainActivity) getActivity();
                        mCreateNoteHelper.addQuickNote(quickNoteEdit.getText().toString(), ma);
                    }
                }
            });
        }

        LinearLayout inputNote = v.findViewById(R.id.quickNoteInput);
        if (current_model == null) {

            if (inputNote != null) inputNote.setVisibility(View.VISIBLE);

            FabSpeedDialMenu menu = new FabSpeedDialMenu(getContext());
            menu.add(1,FAB_MENU_NEW_BUCKET,0, "New Bucket").setIcon(R.drawable.ic_action_add);
            addNotebookButton.setMenu(menu);
        } else if (current_model instanceof SingleRack) {

            if (inputNote != null) inputNote.setVisibility(View.GONE);

            FabSpeedDialMenu menu = new FabSpeedDialMenu(getContext());
            menu.add(1,FAB_MENU_NEW_FOLDER,0, "New Folder").setIcon(R.drawable.ic_action_add);
            addNotebookButton.setMenu(menu);
        } else {

            if (inputNote != null) inputNote.setVisibility(View.GONE);

            FabSpeedDialMenu menu = new FabSpeedDialMenu(getContext());
            menu.add(1,FAB_MENU_NEW_FOLDER,0, "New Folder").setIcon(R.drawable.ic_action_add);
            menu.add(1,FAB_MENU_NEW_NOTE,0, "New Note").setIcon(R.drawable.ic_action_add);
            addNotebookButton.setMenu(menu);
        }
    }

    public void openBucket(MainModel bucket) {
        current_model = null;
        if (history_list != null) history_list.clear();
        this.path.resetPath();
        this.path.goForward(bucket.getName());

        if (bucket_queue == null) bucket_queue = new LinkedList<>();
        bucket_queue.add(bucket);

        if (bucket_queue.size() > 2) {
            MainModel old_bucket = bucket_queue.removeFirst();
            if (!old_bucket.getPath().equals(bucket.getPath())) {
                old_bucket.unloadNotes(new MainModel.OnModelLoadedListener() {
                    @Override
                    public void ModelLoaded() {
                        //Log.i("log", "notes unloaded!");
                    }
                });
            }
        }

        moveInto(bucket);
    }

    public boolean addNewNoteToCurrent(SingleNote new_note) {
        if (current_model == null) return false;

        new_note.refreshContent(new SingleNote.OnNoteLoadedListener() {
            @Override
            public void NoteLoaded(SingleNote note) {
                current_model.addContent(note);
                current_model.sortContents(getContext());
                refreshNotebooks();
            }
        });
        return true;
    }

    @Override
    public boolean onItemClick(View view, int position) {
        MainActivity ma = (MainActivity) getActivity();
        if (adapter.getItem(position) instanceof MainModel) {
            MainModel notebook = adapter.getItem(position);

            if (ma == null || notebook == null) return false;

            String selectedModelName = notebook.getName();

            if (notebook instanceof SingleRack) {
                openBucket(notebook);
            } else if (notebook instanceof SingleNotebook) {
                this.path.goForward(selectedModelName);
                moveInto(notebook);
            } else if (notebook.getType() == MainModel.TYPE_MARKDOWN_NOTE) {
                Intent intent = new Intent(ma, ViewNote.class);
                intent.putExtra("note", notebook.toBundle());
                startActivityForResult(intent, Constants.NOTE_PREVIEW_REQUEST_CODE);
            }
        }
        return false;
    }

    @Override
    public void onItemLongClick(int position) {
        if (adapter.getItem(position) instanceof MainModel) {
            final MainModel notebook = adapter.getItem(position);
            Context context = getContext();

            if (notebook == null || context == null) return;

            if (notebook.isNote()) {
                PopupMenu popup = new PopupMenu(context, adapter.getRecyclerView().getChildAt(position));
                popup.getMenuInflater().inflate(R.menu.popup_note_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.note_delete:
                                deleteNote((SingleNote) notebook);
                                break;
                        }
                        return true;
                    }
                });

                popup.show();
            }
        }
    }

    private void deleteNote(final SingleNote note) {
        if (getContext() == null || note == null || current_model == null) return;
        new AlertDialog.Builder(getContext())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (note.delete()) {
                            Toast.makeText(getContext(), "Deleted Note: " + note.getTitle(), Toast.LENGTH_SHORT).show();
                        }
                        current_model.removeContent(note);
                        current_model.sortContents(getContext());
                        refreshNotebooks();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        super.onActivityResult(requestCode, resultCode, resultIntent);

        if (requestCode == Constants.NOTE_EDITOR_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Log.i("log", "editor exit");
                if (resultIntent.getBooleanExtra("modified", false)) {
                    refreshFolder();
                }
            }
        } else if (requestCode == Constants.NOTE_PREVIEW_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (resultIntent.getBooleanExtra("edit", false)) {
                    MainActivity ma = (MainActivity) getActivity();
                    if (ma == null) return;

                    Intent intent = new Intent(ma, EditorActivity.class);
                    intent.putExtra("note", resultIntent.getBundleExtra("note"));
                    intent.putExtra("root", path.getRootPath());
                    startActivityForResult(intent, Constants.NOTE_EDITOR_REQUEST_CODE);

                } else if (resultIntent.getBooleanExtra("modified", false)) {
                    refreshNotes();
                }
            }
        }
    }
}