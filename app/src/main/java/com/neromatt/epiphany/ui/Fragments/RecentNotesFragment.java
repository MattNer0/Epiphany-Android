package com.neromatt.epiphany.ui.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.helper.DeleteNoteHelper;
import com.neromatt.epiphany.model.Adapters.FadeInItemAnimator;
import com.neromatt.epiphany.model.Adapters.MainAdapter;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.DataObjects.SingleNote;
import com.neromatt.epiphany.tasks.CleanNotesDBTask;
import com.neromatt.epiphany.tasks.ReadNotesDBTask;
import com.neromatt.epiphany.tasks.ReadRecentNotesTask;
import com.neromatt.epiphany.tasks.SearchNotesTask;
import com.neromatt.epiphany.ui.EditorActivity;
import com.neromatt.epiphany.ui.MainActivity;
import com.neromatt.epiphany.ui.Navigation.NavigationLayoutFactory;
import com.neromatt.epiphany.ui.Navigation.OnMovingNoteListener;
import com.neromatt.epiphany.ui.Navigation.OnSearchViewListener;
import com.neromatt.epiphany.ui.Navigation.SearchState;
import com.neromatt.epiphany.ui.R;
import com.neromatt.epiphany.ui.ViewNote;

import java.util.ArrayList;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import ru.whalemare.sheetmenu.SheetMenu;

import static android.app.Activity.RESULT_OK;

public class RecentNotesFragment extends MyFragment implements FlexibleAdapter.OnItemClickListener, FlexibleAdapter.OnItemLongClickListener, ReadNotesDBTask.NotesListener {

    private String root_path;

    private ArrayList<MainModel> contents;

    private SearchNotesTask search_notes_task;
    private ReadRecentNotesTask recent_notes_task;
    private CleanNotesDBTask clean_notes_db_task;

    private boolean database_cleaned = false;
    private boolean should_refresh_list = false;

    public RecentNotesFragment() { }

    public static RecentNotesFragment newInstance(String path) {
        RecentNotesFragment f = new RecentNotesFragment();
        Bundle args = new Bundle();
        args.putString(Constants.KEY_DIR_PATH, path);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mNavigationLayout = new NavigationLayoutFactory(true, false, false, true, false, this);
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
        if (args.containsKey(Constants.KEY_DIR_PATH)) {
            root_path = args.getString(Constants.KEY_DIR_PATH, "");
        }

        should_refresh_list = true;
        runCleanDBTask();

        mNavigationLayout.setOnQueryTextListener(new OnSearchViewListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public void onSearchClosed() {
                mNavigationLayout.hideSearch(getContext());
            }
        });
    }

    private void runCleanDBTask() {
        if (!database_cleaned) {
            if (clean_notes_db_task != null && !clean_notes_db_task.isCancelled()) {
                clean_notes_db_task.cancel(true);
            }

            clean_notes_db_task = new CleanNotesDBTask(getMainActivity(), new CleanNotesDBTask.CleanDBListener() {
                @Override
                public void DatabaseCleaned() {
                    runRecentNotesTask();
                }
            });
            clean_notes_db_task.execute(root_path);

            database_cleaned = true;
        } else {
            runRecentNotesTask();
        }
    }

    private void runRecentNotesTask() {
        if (recent_notes_task != null && !recent_notes_task.isCancelled()) {
            recent_notes_task.cancel(true);
        }

        recent_notes_task = new ReadRecentNotesTask(getMainActivity(), this);
        recent_notes_task.execute();
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mNavigationLayout.setTitle(R.string.title_recent_notes);
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

        MainActivity ma = getMainActivity();
        if (ma != null) {
            mNavigationLayout.setSearchState(ma.getSearchState());
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        MainActivity ma = getMainActivity();
        if (ma != null && mNavigationLayout != null) {
            ma.setSearchState(mNavigationLayout.getSearchState());
        }

        cancelTasks();
    }

    private void cancelTasks() {
        if (recent_notes_task != null && !recent_notes_task.isCancelled()) {
            recent_notes_task.cancel(true);
        }
        if (clean_notes_db_task != null && !clean_notes_db_task.isCancelled()) {
            clean_notes_db_task.cancel(true);
        }
        if (search_notes_task != null && !search_notes_task.isCancelled()) {
            search_notes_task.cancel(true);
        }
    }

    @Override
    int getLayout() {
        return R.layout.fragment_buckets;
    }

    private void initList(ArrayList<MainModel> list) {
        contents = list;

        if (adapter == null || should_refresh_list) {
            adapter = new MainAdapter<>(contents);
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

            should_refresh_list = false;
        }

        SearchState searchState = mNavigationLayout.getSearchState();
        if (searchState.getSearchOpen() && !searchState.getSearchString().isEmpty()) {
            if (search_notes_task != null && !search_notes_task.isCancelled()) {
                search_notes_task.cancel(true);
            }

            search_notes_task = new SearchNotesTask(getMainActivity(), searchState.getSearchString(), this);
            search_notes_task.execute(list.toArray(new MainModel[0]));
        }
    }

    @Override
    public void NoteLoaded(SingleNote note) {

    }

    @Override
    public void NotesLoaded(ArrayList<MainModel> list, int flag) {
        initList(list);
    }

    @Override
    public boolean onItemClick(View view, int position) {
        MainModel model = adapter.getItem(position);
        if (model.getType() == MainModel.TYPE_MARKDOWN_NOTE) {
            Intent intent = new Intent(getMainActivity(), ViewNote.class);
            intent.putExtra("note", model.toBundle());
            startActivityForResult(intent, Constants.NOTE_PREVIEW_REQUEST_CODE);
        }
        return true;
    }

    @Override
    public void onItemLongClick(final int position) {
        MainModel model = adapter.getItem(position);
        if (model.isNote()) {
            SheetMenu.with(getContext())
                    .setTitle(model.getTitle())
                    .setMenu(R.menu.popup_recent_note_menu)
                    .setAutoCancel(true)
                    .setClick(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            SingleNote note = (SingleNote) adapter.getItem(position);
                            switch (item.getItemId()) {
                                case R.id.note_delete:
                                    DeleteNoteHelper.deleteNote(getContext(), note, new DeleteNoteHelper.OnNoteDeleteListener() {
                                        @Override
                                        public void NoteDeleted(boolean deleted) {
                                            if (deleted) {
                                                Toast.makeText(getContext(), "Deleted Note!", Toast.LENGTH_SHORT).show();
                                                runRecentNotesTask();
                                            }
                                        }
                                    });
                                    break;
                                case R.id.note_edit:
                                    Intent intent = new Intent(getActivity(), EditorActivity.class);
                                    intent.putExtra("note", note.toBundle());
                                    intent.putExtra("root", root_path);
                                    startActivityForResult(intent, Constants.NOTE_EDITOR_REQUEST_CODE);
                                    break;
                            }
                            return true;
                        }
                    }).show();
        }
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
                    runRecentNotesTask();
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
                            n.updatePath(note.getString(Constants.KEY_NOTE_PATH), note.getString(Constants.KEY_NOTE_FILENAME));
                            n.refreshFromDB(getMainActivity().getDatabase());
                            runRecentNotesTask();
                            break;
                        }
                    }
                }
            }
        } else if (requestCode == Constants.NEW_NOTE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                should_refresh_list = true;
                runRecentNotesTask();
            }
        }
    }
}
