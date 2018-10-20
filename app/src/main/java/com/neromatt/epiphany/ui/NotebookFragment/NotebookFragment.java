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

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import eu.davidea.flexibleadapter.FlexibleAdapter;
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
    private Stack<MainModel> history_list;
    private LinkedList<MainModel> bucket_queue;
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

        View v = getView();
        if (v == null) return;
        pullToRefresh = v.findViewById(R.id.pullToRefresh);
        pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                MainActivity ma = (MainActivity) getActivity();
                if (ma != null) {
                    ma.reloadAndOpenFolder(current_model);
                }
                pullToRefresh.setRefreshing(false);
            }
        });

        refreshNotebooks();
    }

    private ArrayList<MainModel> getCurrentList() {
        if (current_model != null) {
            return current_model.getContent();
        } else {
            return library_list;
        }
    }

    /*public MainModel getCurrentModel() {
        return current_model;
    }*/

    public void refreshNotebooks(MainModel current) {
        if (current != null) {
            this.current_model = current;
            current.loadNotes(getContext(), new MainModel.OnModelLoadedListener() {
                @Override
                public void ModelLoaded() {
                    refreshNotebooks(current_model.getContent());
                }
            });
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
        if (getActivity() == null) return null;

        if (staggered) {
            if (adapter != null) adapter.setSpanCount(2);
            return new StaggeredGridLayoutManager(2, 1);
        }
        if (adapter != null) adapter.setSpanCount(1);
        return new StaggeredGridLayoutManager(1, 1);
    }

    public void reloadLibrary(ArrayList<MainModel> items) {
        library_list = items;
        current_model = null;
        history_list = new Stack<>();
        refreshNotebooks(items);
    }

    private void refreshNotebooks(ArrayList<MainModel> items) {
        View v = getView();
        if (v == null) return;
        if (getActivity() == null || getContext() == null) return;

        if (current_model != null) {
            getActivity().setTitle(current_model.getTitle());
        } else {
            getActivity().setTitle(R.string.title_library);
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

        LinearLayout emptyView = v.findViewById(R.id.notebook_emptyview);
        if (items.size() == 0) {
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
        }

        LinearLayout inputNote = v.findViewById(R.id.quickNoteInput);
        TextView inputNoteText = v.findViewById(R.id.quickNoteEdit);
        LinearLayout inputNoteMove = v.findViewById(R.id.noteMoveContainer);

        if ((current_model == null || current_model.isQuickNotes()) && !adapter.isMovingNote()) {

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

        } else if (adapter.isMovingNote()) {

            //addNotebookButton.setVisibility(View.GONE);
            inputNote.setVisibility(View.VISIBLE);
            inputNoteMove.setVisibility(View.VISIBLE);
            inputNoteText.setVisibility(View.GONE);

            ImageView undo_move = v.findViewById(R.id.undo_move);
            TextView current_note_move = v.findViewById(R.id.current_note_move);
            TextView move_note_button = v.findViewById(R.id.move_note_button);

            undo_move.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    adapter.clearMovingNote();
                    refreshNotebooks();
                }
            });

            if (current_model != null && current_model.isFolder() && !adapter.sameFolderAsMovingNote(current_model)) {
                move_note_button.setAlpha(1.0f);
                move_note_button.setText(R.string.move_note_here);
                move_note_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SingleNote note = adapter.getMovingNote();
                        if (note.moveFile(current_model.getPath())) {
                            adapter.getMovingNoteFolder().removeContent(note);
                            adapter.clearMovingNote();
                            addNewNoteToCurrent(note);
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

            current_note_move.setText(adapter.getMovingNote().getName());

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

    private void renameModel() {
        CreateNotebookDialog dialog = new CreateNotebookDialog();
        Bundle args = new Bundle();
        args.putInt("positive", R.string.rename_notebook);
        args.putString("placeholder", current_model.getName());
        dialog.setArguments(args);
        dialog.setDialogListener(new CreateNotebookDialog.CreateNotebookDialogListener() {
            @Override
            public void onDialogPositiveClick(CreateNotebookDialog dialog, String text) {
                MainActivity ma = (MainActivity) getActivity();
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
                    refreshNotebooks();
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

    private void moveInto(MainModel model) {
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

    public void openBucket(MainModel bucket) {
        current_model = null;
        if (history_list == null) history_list = new Stack<>();
        history_list.clear();

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

        if (bucket.isQuickNotes()) {
            MainModel quick_notes_folder = bucket.getFirstFolder();
            if (quick_notes_folder != null) {
                history_list.push(current_model);
                current_model = quick_notes_folder;
                ((SingleNotebook) quick_notes_folder).setQuickNotesFolder();
                quick_notes_folder.loadNotes(getContext(), new MainModel.OnModelLoadedListener() {
                    @Override
                    public void ModelLoaded() {
                        refreshNotebooks(getCurrentList());
                    }
                });
                return;
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

            if (notebook instanceof SingleRack) {
                openBucket(notebook);
            } else if (notebook instanceof SingleNotebook) {
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
                                case R.id.note_move:
                                    adapter.setMovingNote((SingleNote) notebook, current_model);
                                    refreshNotebooks();
                                    break;
                            }
                            return true;
                        }
                    }).show();
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
                        refreshNotebooks();
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