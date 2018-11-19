package com.neromatt.epiphany.ui.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.helper.CreateFolderHelper;
import com.neromatt.epiphany.model.Adapters.BreadcrumbAdapter;
import com.neromatt.epiphany.model.Adapters.FadeInItemAnimator;
import com.neromatt.epiphany.model.Adapters.MainAdapter;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.DataObjects.SingleNote;
import com.neromatt.epiphany.model.NotebooksComparator;
import com.neromatt.epiphany.tasks.ReadFoldersTask;
import com.neromatt.epiphany.tasks.ReadNotesDBTask;
import com.neromatt.epiphany.tasks.ReadNotesTask;
import com.neromatt.epiphany.ui.EditorActivity;
import com.neromatt.epiphany.ui.MainActivity;
import com.neromatt.epiphany.ui.Navigation.Breadcrumb;
import com.neromatt.epiphany.ui.Navigation.NavigationLayoutFactory;
import com.neromatt.epiphany.ui.R;
import com.neromatt.epiphany.ui.ViewNote;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import io.github.kobakei.materialfabspeeddial.FabSpeedDial;
import io.github.kobakei.materialfabspeeddial.FabSpeedDialMenu;

import static android.app.Activity.RESULT_OK;

public class FoldersFragment extends MyFragment implements FlexibleAdapter.OnItemClickListener, FlexibleAdapter.OnItemLongClickListener, ReadFoldersTask.FoldersListener, ReadNotesDBTask.NotesListener {

    private ArrayList<String> parent_paths;
    private String folder_path;
    private String root_path;

    private String title;

    private ReadFoldersTask folders_task;
    private ReadNotesDBTask notes_db_task;

    private ArrayList<MainModel> contents;

    public FoldersFragment() { }

    public static FoldersFragment newInstance(String root_path, ArrayList<String> parents, String path, String title) {
        FoldersFragment f = new FoldersFragment();
        Bundle args = new Bundle();
        args.putString(Constants.KEY_DIR_PATH, path);
        args.putString(Constants.KEY_DIR_ROOT_PATH, root_path);
        args.putStringArrayList(Constants.KEY_DIR_PARENTS, parents);
        args.putString(Constants.KEY_DIR_TITLE, title);
        f.setArguments(args);
        return f;
    }

    public static FoldersFragment newInstance(String root_path, ArrayList<String> parents, String path) {
        return newInstance(root_path, parents, path, "");
    }

    public static FoldersFragment newInstance(String root_path, String path) {
        ArrayList<String> parent = new ArrayList<>();
        parent.add(root_path);

        return newInstance(root_path, parent, path);
    }

    public static FoldersFragment newInstance(String root_path, String path, String title) {
        ArrayList<String> parent = new ArrayList<>();
        parent.add(root_path);

        return newInstance(root_path, parent, path, title);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mNavigationLayout = new NavigationLayoutFactory(true, true, true, true, false, this);
        return mNavigationLayout.produceLayout(inflater, container);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mNavigationLayout.viewCreated(getMainActivity(), view);
        recycler_view = view.findViewById(R.id.listview);

        if (recycler_view_state == null && savedInstanceState != null) {
            recycler_view_state = savedInstanceState.getParcelable(Constants.RECYCLE_STATE);
        }

        Bundle args = getArguments();
        if (args.containsKey(Constants.KEY_DIR_TITLE)) {
            title = args.getString(Constants.KEY_DIR_TITLE, "");
        }
        if (args.containsKey(Constants.KEY_DIR_PATH)) {
            folder_path = args.getString(Constants.KEY_DIR_PATH, "");
            root_path = args.getString(Constants.KEY_DIR_ROOT_PATH, "");
            parent_paths = args.getStringArrayList(Constants.KEY_DIR_PARENTS);
            if (parent_paths == null) parent_paths = new ArrayList<>();
            runFoldersTask();
        }

        if (getMainActivity().getSearchOpened()) {
            mNavigationLayout.showSearch("", false);
        }

        if (parent_paths != null && parent_paths.size() > 0) {
            FabSpeedDialMenu menu = new FabSpeedDialMenu(getContext());
            if (title != null && title.equalsIgnoreCase("quick notes")) {
                menu.add(1, Constants.FAB_MENU_NEW_NOTE, 4, R.string.fab_new_note).setIcon(R.drawable.ic_action_add);
            } else if (parent_paths.size() == 1) {
                menu.add(1, Constants.FAB_MENU_NEW_FOLDER, 2, R.string.fab_new_folder).setIcon(R.drawable.ic_action_add);
                //menu.add(1, Constants.FAB_MENU_RENAME_BUCKET, 1, R.string.fab_rename_bucket).setIcon(R.drawable.ic_mode_edit_white_24dp);
            } else {
                menu.add(1, Constants.FAB_MENU_NEW_NOTE, 4, R.string.fab_new_note).setIcon(R.drawable.ic_action_add);
                menu.add(1, Constants.FAB_MENU_NEW_FOLDER, 3, R.string.fab_new_folder).setIcon(R.drawable.ic_action_add);
                //menu.add(1, Constants.FAB_MENU_RENAME_FOLDER, 2, R.string.fab_rename_folder).setIcon(R.drawable.ic_mode_edit_white_24dp);
                /*if (current_model.getContentCount() == 0) {
                    menu.add(1, Constants.FAB_MENU_DELETE_FOLDER, 1, R.string.fab_remove_folder).setIcon(R.drawable.ic_delete_white_24dp);
                }*/
            }
            mNavigationLayout.setFabMenu(menu);
            mNavigationLayout.addOnFabMenuItemClickListener(new FabSpeedDial.OnMenuItemClickListener() {
                @Override
                public void onMenuItemClick(FloatingActionButton fab, TextView textView, int itemId) {
                    switch (itemId) {
                        case Constants.FAB_MENU_RENAME_BUCKET:
                            break;
                        case Constants.FAB_MENU_NEW_FOLDER:
                            CreateFolderHelper.addFolder(getMainActivity(), folder_path, new CreateFolderHelper.FolderCreatedListener() {
                                @Override
                                public void onCreated(boolean success) {
                                    runFoldersTask();
                                }
                            });
                            break;
                        case Constants.FAB_MENU_RENAME_FOLDER:
                            break;
                        case Constants.FAB_MENU_NEW_NOTE:
                            Intent intent = new Intent(getActivity(), EditorActivity.class);
                            intent.putExtra("folder", folder_path);
                            intent.putExtra("root", root_path);
                            startActivityForResult(intent, Constants.NEW_NOTE_REQUEST_CODE);
                            break;
                    }
                }
            });

            ArrayList<Breadcrumb> breadcrumbs = new ArrayList<>();
            for (String p : parent_paths) {
                File f = new File(p);
                breadcrumbs.add(new Breadcrumb(f.getName(), f.getPath()));
            }
            if (folder_path != null && !folder_path.isEmpty()) {
                if (title != null && !title.isEmpty()) {
                    breadcrumbs.add(new Breadcrumb(title, folder_path, true));
                } else {
                    File f = new File(folder_path);
                    breadcrumbs.add(new Breadcrumb(f.getName(), f.getPath(), true));
                }
            }
            mNavigationLayout.setBreadcrumbs(getContext(), breadcrumbs, new BreadcrumbAdapter.OnBreadcrumbClickListener() {
                @Override
                public void CrumbClicked(Breadcrumb crumb, int position) {
                    MainActivity ma = getMainActivity();
                    if (ma == null) return;

                    if (position == 0) {
                        ma.pushFragment(BucketsFragment.newInstance(crumb.value), Constants.BUCKETS_FRAGMENT_TAG, Constants.BUCKETS_FRAGMENT_TAG, true);
                    } else {
                        ArrayList<String> parent = new ArrayList<>();
                        for (int i=0; i<position; i++) {
                            parent.add(parent_paths.get(i));
                        }
                        ma.pushFragment(FoldersFragment.newInstance(root_path, parent, crumb.value), Constants.FOLDER_FRAGMENT_TAG, Constants.FOLDER_FRAGMENT_TAG+crumb.value, true);
                    }
                }
            });

        } else {
            mNavigationLayout.hideFab();
        }
    }

    private void runFoldersTask() {
        if (folders_task != null && !folders_task.isCancelled()) {
            folders_task.cancel(true);
        }

        folders_task = new ReadFoldersTask(getContext(), this);
        folders_task.execute(folder_path);
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (title != null && !title.isEmpty()) {
            mNavigationLayout.setTitle(title);
        } else if (folder_path != null && !folder_path.isEmpty()) {
            File f = new File(folder_path);

            if (f.getName().equalsIgnoreCase(Constants.QUICK_NOTES_BUCKET)) {
                mNavigationLayout.setTitle(R.string.title_quicknotes_bucket);
            } else {
                mNavigationLayout.setTitle(f.getName());
            }
        } else {
            mNavigationLayout.setTitle(R.string.title_library);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null && recycler_view != null) {
            recycler_view.setLayoutManager(getLayoutManager());

            if (recycler_view_state != null) {
                recycler_view.getLayoutManager().onRestoreInstanceState(recycler_view_state);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        cancelTasks();
    }

    @Override
    int getLayout() {
        return R.layout.fragment_folders;
    }

    private void cancelTasks() {
        if (folders_task != null && !folders_task.isCancelled()) {
            folders_task.cancel(true);
        }
        if (notes_db_task != null && !notes_db_task.isCancelled()) {
            notes_db_task.cancel(true);
        }
    }

    private void initList(ArrayList<MainModel> list) {
        contents = list;

        adapter = new MainAdapter<>(getMainActivity(), contents);
        adapter.addListener(this);

        adapter
                .setNotifyMoveOfFilteredItems(true)
                .setOnlyEntryAnimation(false)
                .setAnimationOnForwardScrolling(false)
                .setAnimationOnReverseScrolling(false)
                .setAnimationEntryStep(true)
                .setAnimationInterpolator(new DecelerateInterpolator())
                .setAnimationDelay(50)
                .setAnimationDuration(200);

        recycler_view.setItemViewCacheSize(0);
        recycler_view.setLayoutManager(getLayoutManager());

        recycler_view.setHasFixedSize(true);
        recycler_view.setAdapter(adapter);

        recycler_view.setItemAnimator(new FadeInItemAnimator(new OvershootInterpolator(1f)));
        recycler_view.getItemAnimator().setAddDuration(500);
        recycler_view.getItemAnimator().setRemoveDuration(500);

        if (notes_db_task != null && !notes_db_task.isCancelled()) {
            notes_db_task.cancel(true);
        }

        notes_db_task = new ReadNotesDBTask(getMainActivity(), this);
        notes_db_task.execute(list.toArray(new MainModel[list.size()]));
    }

    private void refreshNotes() {
        //adapter.getCurrentItems();
        Collections.sort(contents, new NotebooksComparator(getContext()));
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                adapter.updateDataSet(contents);
            }
        });
    }

    @Override
    public boolean onItemClick(View view, int position) {
        MainModel model = adapter.getItem(position);
        if (model.isFolder()) {
            ArrayList<String> parent = new ArrayList<>();
            parent.addAll(parent_paths);
            parent.add(folder_path);

            MainActivity ma = getMainActivity();
            if (ma == null) return false;

            ma.pushFragment(FoldersFragment.newInstance(root_path, parent, model.getPath()), Constants.FOLDER_FRAGMENT_TAG, Constants.FOLDER_FRAGMENT_TAG+model.getPath());
        } else if (model.getType() == MainModel.TYPE_MARKDOWN_NOTE) {
            Intent intent = new Intent(getMainActivity(), ViewNote.class);
            intent.putExtra("note", model.toBundle());
            startActivityForResult(intent, Constants.NOTE_PREVIEW_REQUEST_CODE);
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        super.onActivityResult(requestCode, resultCode, resultIntent);

        if (requestCode == Constants.NOTE_PREVIEW_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (resultIntent.getBooleanExtra("edit", false)) {
                    MainActivity ma = getMainActivity();
                    if (ma == null) return;

                    Intent intent = new Intent(ma, EditorActivity.class);
                    intent.putExtra("note", resultIntent.getBundleExtra("note"));
                    intent.putExtra("root", root_path);
                    startActivityForResult(intent, Constants.NOTE_EDITOR_REQUEST_CODE);

                } else if (resultIntent.getBooleanExtra("modified", false)) {
                    refreshNotes();
                }
            }
        } else if (requestCode == Constants.NOTE_EDITOR_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (resultIntent.getBooleanExtra("modified", false)) {
                    Bundle note = resultIntent.getBundleExtra("note");
                    if (note == null) return;
                    for (MainModel m: contents) {
                        if (m.isNote() && m.equalsUUID(note.getString(Constants.KEY_UUID))) {
                            SingleNote n = (SingleNote) m;
                            n.refreshFromDB(getMainActivity().getDatabase());
                            refreshNotes();
                            break;
                        }
                    }
                }
            }
        } else if (requestCode == Constants.NEW_NOTE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                runFoldersTask();
            }
        }
    }

    @Override
    public void onItemLongClick(int position) {

    }

    @Override
    public void FoldersLoaded(ArrayList<MainModel> list) {
        initList(list);
    }

    @Override
    public void NoteLoaded(SingleNote note) {
        for (MainModel m: contents) {
            if (m.isNote() && m.equalsUUID(note)) {
                SingleNote old_note = (SingleNote) m;
                old_note.updateObj(note.toBundle());
                break;
            }
        }
    }

    @Override
    public void NotesLoaded(ArrayList<MainModel> notes, int flag) {
        if (notes.size() > 0) {
            refreshNotes();
        }
    }
}
