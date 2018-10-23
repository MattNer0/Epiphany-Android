package com.neromatt.epiphany.ui.NotebookFragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.model.Adapters.MainAdapter;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.DataObjects.SingleNote;
import com.neromatt.epiphany.model.DataObjects.SingleNotebook;
import com.neromatt.epiphany.model.DataObjects.SingleRack;
import com.neromatt.epiphany.model.Path;
import com.neromatt.epiphany.ui.EditorActivity;
import com.neromatt.epiphany.ui.MainActivity;
import com.neromatt.epiphany.ui.R;
import com.neromatt.epiphany.ui.ViewNote;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Stack;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.common.FlexibleItemAnimator;
import io.github.kobakei.materialfabspeeddial.FabSpeedDial;
import io.github.kobakei.materialfabspeeddial.FabSpeedDialMenu;
import ru.whalemare.sheetmenu.SheetMenu;

import static android.app.Activity.RESULT_OK;

public class NotebookFragment extends Fragment implements FlexibleAdapter.OnItemClickListener, FlexibleAdapter.OnItemLongClickListener {

    private SwipeRefreshLayout pullToRefresh;
    private FabSpeedDial addNotebookButton;
    private RecyclerView notebookList;

    private MainAdapter<MainModel> adapter;
    private CreateNotebookHelper mCreateNotebookHelper;
    private CreateNoteHelper mCreateNoteHelper;
    private CreateBucketHelper mCreateBucketHelper;

    private ArrayList<MainModel> library_list;
    private MainModel current_model;

    private Handler handler;

    private Path path;

    public static NotebookFragment newInstance(ArrayList<MainModel> contents) {
        NotebookFragment f = new NotebookFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList("contents", contents);
        f.setArguments(args);
        return f;
    }

    public static NotebookFragment newInstance(MainModel current) {
        NotebookFragment f = new NotebookFragment();
        Bundle args = new Bundle();
        args.putParcelable("current", current);
        f.setArguments(args);
        return f;
    }

    public NotebookFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

        if (args.containsKey("contents")) {
            library_list = args.getParcelableArrayList("contents");
        } else if (args.containsKey("current")) {
            current_model = args.getParcelable("current");
        }

        //FlexibleAdapter.enableLogs(eu.davidea.flexibleadapter.utils.Log.Level.DEBUG);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        path = getMainActivity().getPath();
        if (!path.isRoot()) {
            path.resetPath();
        }

        initializeRecyclerView(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        setHasOptionsMenu(true);

        if (getActivity() == null) return;

        path = getMainActivity().getPath();
        if (!path.isRoot()) {
            path.resetPath();
        }

        //if (adapter != null) adapter.clear();
        //if (current_model != null) current_model.sortContents(getContext());

        initializeTitle();
        initializeUI();
    }

    private ArrayList<MainModel> getCurrentList() {
        if (current_model != null) {
            return current_model.getContent();
        } else {
            return library_list;
        }
    }

    /*public void refreshNotebooks(MainModel current) {
        if (current != null) {
            this.current_model = current;
            current.loadNotes(getContext(), new MainModel.OnModelLoadedListener() {
                @Override
                public void ModelLoaded() {
                    refreshNotebooks(current_model.getContent());
                }
            });
        }
    }*/

    private void initializeRecyclerView(Bundle savedInstanceState) {
        FlexibleAdapter.useTag("adpt");

        adapter = new MainAdapter<>(getMainActivity(), getCurrentList());
        adapter.addListener(this);

        adapter
            .setStickyHeaderElevation(5)
            .setDisplayHeadersAtStartUp(true)
            .setStickyHeaders(true)
            .setOnlyEntryAnimation(false)
            .setAnimationOnForwardScrolling(true)
            .setAnimationOnReverseScrolling(true)
            .setAnimationEntryStep(true)
            .setAnimationDelay(50)
            .setAnimationDuration(200);

        notebookList = getView().findViewById(R.id.notebookview);
        notebookList.setItemViewCacheSize(0);
        notebookList.setLayoutManager(getLayoutManager(
                PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("pref_staggered_layout", false))
        );

        notebookList.setHasFixedSize(true);
        notebookList.setItemAnimator(new FlexibleItemAnimator());
        notebookList.setAdapter(adapter);

        /*FlexibleItemDecoration mItemDecoration = new FlexibleItemDecoration(getActivity())
                .withOffset(3)
                .withTopEdge(true)
                .withBottomEdge(true);
        notebookList.addItemDecoration(mItemDecoration);*/

        mCreateBucketHelper = new CreateBucketHelper(this);
        mCreateNotebookHelper = new CreateNotebookHelper(this);
        mCreateNoteHelper = new CreateNoteHelper(this, path);

        pullToRefresh = getView().findViewById(R.id.pullToRefresh);
        pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                MainActivity ma = getMainActivity();
                if (ma != null) {
                    ma.reloadAndOpenFolder(current_model, true);
                }
                pullToRefresh.setRefreshing(false);
            }
        });

        //loadFullList();
    }

    private void initializeTitle() {
        if (current_model != null) {
            getActivity().setTitle(current_model.getTitle());
        } else {
            getActivity().setTitle(R.string.title_library);
        }
    }

    private void initializeUI() {
        initializeFab();
        initializeQuickNote();
        initializeBottomBar();

        LinearLayout emptyView = getView().findViewById(R.id.notebook_emptyview);
        if (getCurrentList().size() == 0) {
            Log.i(Constants.LOG, "visible");
            emptyView.setVisibility(View.VISIBLE);
        } else {
            Log.i(Constants.LOG, "gone");
            emptyView.setVisibility(View.GONE);
        }
    }

    private void initializeFab() {
        if (addNotebookButton == null) addNotebookButton = getView().findViewById(R.id.notebookFab);;

        addNotebookButton.addOnMenuItemClickListener(new FabSpeedDial.OnMenuItemClickListener() {
            @Override
            public void onMenuItemClick(FloatingActionButton fab, TextView textView, int itemId) {
                switch (itemId) {
                    case Constants.FAB_MENU_NEW_BUCKET:
                        mCreateBucketHelper.addBucket();
                    case Constants.FAB_MENU_NEW_FOLDER:
                        mCreateNotebookHelper.addNotebook(current_model);
                        break;
                    case Constants.FAB_MENU_NEW_NOTE:
                        if (current_model != null) mCreateNoteHelper.addNote(current_model);
                        break;
                    case Constants.FAB_MENU_RENAME_BUCKET:
                        if (current_model instanceof SingleRack) {
                            renameModel();
                        }
                        break;
                    case Constants.FAB_MENU_RENAME_FOLDER:
                        if (current_model instanceof SingleNotebook) {
                            renameModel();
                        }
                        break;
                }
            }
        });
    }

    private void initializeQuickNote() {
        EditText quickNoteEdit = getView().findViewById(R.id.quickNoteEdit);
        if (quickNoteEdit != null) {
            quickNoteEdit.clearFocus();
            quickNoteEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText quickNoteEdit = v.findViewById(R.id.quickNoteEdit);
                    MainActivity ma = getMainActivity();
                    mCreateNoteHelper.addQuickNote(quickNoteEdit.getText().toString(), ma);
                }
            });
            quickNoteEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    EditText quickNoteEdit = v.findViewById(R.id.quickNoteEdit);

                    if (hasFocus) {
                        MainActivity ma = getMainActivity();
                        mCreateNoteHelper.addQuickNote(quickNoteEdit.getText().toString(), ma);
                    }
                }
            });
        }
    }

    private void initializeBottomBar() {
        View v = getView();

        LinearLayout inputNote = v.findViewById(R.id.quickNoteInput);
        TextView inputNoteText = v.findViewById(R.id.quickNoteEdit);
        LinearLayout inputNoteMove = v.findViewById(R.id.noteMoveContainer);

        final MainActivity ma = getMainActivity();

        if ((current_model == null || current_model.isQuickNotes()) && !ma.isMovingNote()) {

            inputNote.setVisibility(View.VISIBLE);
            inputNoteText.setVisibility(View.VISIBLE);
            inputNoteMove.setVisibility(View.GONE);

            addNotebookButton.setVisibility(View.VISIBLE);
            if (current_model != null && current_model.isQuickNotes()) {
                FabSpeedDialMenu menu = new FabSpeedDialMenu(getContext());
                menu.add(1, Constants.FAB_MENU_NEW_NOTE, 0, R.string.fab_new_quick_note).setIcon(R.drawable.ic_action_add);
                addNotebookButton.setMenu(menu);
            } else {
                FabSpeedDialMenu menu = new FabSpeedDialMenu(getContext());
                menu.add(1, Constants.FAB_MENU_NEW_BUCKET, 0, R.string.fab_new_bucket).setIcon(R.drawable.ic_action_add);
                addNotebookButton.setMenu(menu);
            }

        } else if (ma.isMovingNote()) {
            initializeMovingNoteUI();

        } else if (current_model != null) {

            inputNote.setVisibility(View.GONE);

            if (getContext() == null) {
                addNotebookButton.setVisibility(View.GONE);
                return;
            }

            addNotebookButton.setVisibility(View.VISIBLE);
            if (current_model instanceof SingleRack) {
                FabSpeedDialMenu menu = new FabSpeedDialMenu(getContext());
                menu.add(1, Constants.FAB_MENU_NEW_FOLDER, 2, R.string.fab_new_folder).setIcon(R.drawable.ic_action_add);
                menu.add(1, Constants.FAB_MENU_RENAME_BUCKET, 1, R.string.fab_rename_bucket).setIcon(R.drawable.ic_mode_edit_white_24dp);
                addNotebookButton.setMenu(menu);

            } else if (current_model.isQuickNotes()) {
                FabSpeedDialMenu menu = new FabSpeedDialMenu(getContext());
                menu.add(1, Constants.FAB_MENU_NEW_NOTE, 1, R.string.fab_new_note).setIcon(R.drawable.ic_action_add);
                addNotebookButton.setMenu(menu);
            } else {
                FabSpeedDialMenu menu = new FabSpeedDialMenu(getContext());
                menu.add(1, Constants.FAB_MENU_NEW_NOTE, 3, R.string.fab_new_note).setIcon(R.drawable.ic_action_add);
                menu.add(1, Constants.FAB_MENU_NEW_FOLDER, 2, R.string.fab_new_folder).setIcon(R.drawable.ic_action_add);
                menu.add(1, Constants.FAB_MENU_RENAME_FOLDER, 1, R.string.fab_rename_folder).setIcon(R.drawable.ic_mode_edit_white_24dp);
                addNotebookButton.setMenu(menu);
            }
        }
    }

    private void initializeMovingNoteUI() {
        View v = getView();

        LinearLayout inputNote = v.findViewById(R.id.quickNoteInput);
        TextView inputNoteText = v.findViewById(R.id.quickNoteEdit);
        LinearLayout inputNoteMove = v.findViewById(R.id.noteMoveContainer);

        final MainActivity ma = getMainActivity();

        inputNote.setVisibility(View.VISIBLE);
        inputNoteMove.setVisibility(View.VISIBLE);
        inputNoteText.setVisibility(View.GONE);

        ImageView undo_move = v.findViewById(R.id.undo_move);
        TextView current_note_move = v.findViewById(R.id.current_note_move);
        TextView move_note_button = v.findViewById(R.id.move_note_button);

        undo_move.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ma.clearMovingNote();
                initializeBottomBar();
                reloadAdapter(false);
            }
        });

        if (current_model != null && current_model.isFolder() && !ma.sameFolderAsMovingNote(current_model)) {
            move_note_button.setAlpha(1.0f);
            move_note_button.setText(R.string.move_note_here);
            move_note_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SingleNote note = ma.getMovingNote();
                    if (note.moveFile(current_model.getPath())) {
                        ma.getMovingNoteFolder().removeContent(note);
                        ma.clearMovingNote();
                        addNewNoteToCurrent(note);
                        initializeBottomBar();
                    } else {
                        Toast.makeText(getContext(), "Couldn't move note!", Toast.LENGTH_LONG).show();
                    }
                }
            });
        } else {
            move_note_button.setAlpha(0.8f);
            move_note_button.setOnClickListener(null);
            move_note_button.setText(R.string.cant_move_here);
        }

        current_note_move.setText(ma.getMovingNote().getName());
    }

    /*public void refreshNotebooks() {
        if (getActivity() == null || getContext() == null) return;

        refreshNotebooks(getCurrentList());
    }*/

    private void refreshFolder() {
        if (current_model != null) {
            current_model.clearContent();
            current_model.loadContent(getContext(), new MainModel.OnModelLoadedListener() {
                @Override
                public void ModelLoaded() {
                    current_model.loadNotes(getContext(), new MainModel.OnModelLoadedListener() {
                        @Override
                        public void ModelLoaded() {
                            refreshNotebooks(getCurrentList());
                        }
                    });
                }
            });
        }
    }

    private void refreshNotes() {
        if (current_model != null) {
            current_model.reloadNotes(true, getContext(), new MainModel.OnModelLoadedListener() {
                @Override
                public void ModelLoaded() {
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
        if (staggered) {
            if (adapter != null) adapter.setSpanCount(2);
            return new StaggeredGridLayoutManager(2, 1);
        }
        if (adapter != null) adapter.setSpanCount(1);
        return new StaggeredGridLayoutManager(1, 1);
    }

    private void refreshNotebooks(ArrayList<MainModel> items) {
        View v = getView();
        if (v == null) return;
        if (getActivity() == null || getContext() == null) return;

        for (MainModel m: items) {
            m.loadNotes(getContext(), new MainModel.OnModelLoadedListener() {
                @Override
                public void ModelLoaded() {

                }
            });
        }
    }

    private void renameModel() {
        CreateNotebookDialog dialog = new CreateNotebookDialog();
        Bundle args = new Bundle();
        args.putInt("positive", R.string.rename_notebook);
        args.putString("placeholder", current_model.getName());
        dialog.setArguments(args);
        dialog.setDialogListener(new CreateNotebookDialog.CreateNotebookDialogListener() {
            @Override
            public void onDialogPositiveClick(CreateNotebookDialog dialog, String text) {
                MainActivity ma = getMainActivity();
                if (ma == null) return;

                boolean renamed = false;
                if (current_model instanceof SingleRack) {
                    SingleRack bucket = (SingleRack) current_model;
                    renamed = bucket.renameDirectory(text);
                    if (renamed) ma.refreshRackDrawer();

                } else if (current_model instanceof SingleNotebook) {
                    SingleNotebook folder = (SingleNotebook) current_model;
                    renamed = folder.renameDirectory(text);
                }

                if (renamed) {
                    initializeTitle();
                    reloadAdapter(false);
                } else {
                    Toast.makeText(getContext(), "Couldn't rename folder!", Toast.LENGTH_LONG).show();
                }
                dialog.dismiss();
            }

            @Override
            public void onDialogNegativeClick(CreateNotebookDialog dialog) {
                dialog.dismiss();
            }
        });
        dialog.show(getActivity().getSupportFragmentManager(), "CreateNotebookDialogFragment");
    }

    public boolean addNewNoteToCurrent(SingleNote new_note) {
        if (current_model == null) return false;

        new_note.refreshContent(new SingleNote.OnNoteLoadedListener() {
            @Override
            public void NoteLoaded(SingleNote note) {
                current_model.addContent(note);
                current_model.sortContents(getContext());
                reloadAdapter(true);
            }
        });
        return true;
    }

    @Override
    public boolean onItemClick(View view, int position) {
        MainActivity ma = getMainActivity();
        if (adapter.getItem(position) != null) {
            MainModel notebook = adapter.getItem(position);

            if (ma == null || notebook == null) return false;

            if (notebook instanceof SingleRack || notebook instanceof SingleNotebook) {
                ma.pushFragment(notebook);

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
        if (adapter.getItem(position) != null) {
            final MainModel notebook = adapter.getItem(position);
            Context context = getContext();

            if (notebook == null || context == null) return;

            if (notebook.isNote()) {
                SheetMenu.with(context)
                    .setMenu(R.menu.popup_note_menu)
                    .setAutoCancel(true)
                    .setClick(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.note_delete:
                                    deleteNote((SingleNote) notebook);
                                    break;
                                case R.id.note_edit:
                                    SingleNote note = (SingleNote) notebook;
                                    Intent intent = new Intent(getActivity(), EditorActivity.class);
                                    intent.putExtra("note", note.toBundle());
                                    intent.putExtra("root", path.getRootPath());
                                    startActivityForResult(intent, Constants.NOTE_EDITOR_REQUEST_CODE);
                                    break;
                                case R.id.note_move:
                                    getMainActivity().setMovingNote((SingleNote) notebook, current_model);
                                    initializeMovingNoteUI();
                                    reloadAdapter(false);
                                    break;
                            }
                            return true;
                        }
                    }).show();
            }
        }
    }

    public MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }

    public void reloadAdapter(boolean refresh_list) {
        if (adapter != null) {
            if (refresh_list) {
                adapter.updateDataSet(getCurrentList());
                //loadFullList();
            } else {
                adapter.notifyDataSetChanged();
            }
        }
    }

    private void deleteNote(final SingleNote note) {
        if (getContext() == null || note == null || current_model == null) return;
        new AlertDialog.Builder(getContext())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.dialog_delete_note_title)
                .setMessage(R.string.dialog_delete_note_message)
                .setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (note.delete()) {
                            Toast.makeText(getContext(), "Deleted Note: " + note.getTitle(), Toast.LENGTH_SHORT).show();
                        }
                        current_model.removeContent(note);
                        current_model.sortContents(getContext());
                        reloadAdapter(true);
                    }
                })
                .setNegativeButton(R.string.dialog_no, null)
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        super.onActivityResult(requestCode, resultCode, resultIntent);

        if (requestCode == Constants.NOTE_EDITOR_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (resultIntent.getBooleanExtra("modified", false)) {
                    refreshFolder();
                }
            }
        } else if (requestCode == Constants.NOTE_PREVIEW_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (resultIntent.getBooleanExtra("edit", false)) {
                    MainActivity ma = getMainActivity();
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