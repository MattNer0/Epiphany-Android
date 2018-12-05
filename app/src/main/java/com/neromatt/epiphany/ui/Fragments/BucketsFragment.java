package com.neromatt.epiphany.ui.Fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.helper.BucketHelper;
import com.neromatt.epiphany.helper.CreateNoteHelper;
import com.neromatt.epiphany.helper.FolderHelper;
import com.neromatt.epiphany.model.Adapters.FadeInItemAnimator;
import com.neromatt.epiphany.model.Adapters.MainAdapter;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.DataObjects.SingleNotebook;
import com.neromatt.epiphany.model.DataObjects.SingleRack;
import com.neromatt.epiphany.tasks.ReadBucketsTask;
import com.neromatt.epiphany.ui.MainActivity;
import com.neromatt.epiphany.ui.Navigation.NavigationLayoutFactory;
import com.neromatt.epiphany.ui.Navigation.OnMovingNoteListener;
import com.neromatt.epiphany.ui.Navigation.OnQuickNoteEdit;
import com.neromatt.epiphany.ui.Navigation.OnSearchViewListener;
import com.neromatt.epiphany.ui.R;

import java.io.File;
import java.util.ArrayList;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import io.github.kobakei.materialfabspeeddial.FabSpeedDial;
import io.github.kobakei.materialfabspeeddial.FabSpeedDialMenu;
import ru.whalemare.sheetmenu.SheetMenu;

public class BucketsFragment extends MyFragment implements FlexibleAdapter.OnItemClickListener, FlexibleAdapter.OnItemLongClickListener, ReadBucketsTask.BucketsListener {

    private String root_path;
    private ReadBucketsTask buckets_task;

    private LinearLayout empty_list_message;
    //private SwipeRefreshLayout refresh_layout;

    private boolean should_refresh_list = false;

    public BucketsFragment() { }

    public static BucketsFragment newInstance(String path) {
        BucketsFragment f = new BucketsFragment();
        Bundle args = new Bundle();
        args.putString(Constants.KEY_DIR_PATH, path);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mNavigationLayout = new NavigationLayoutFactory(true, false, true, true, true, this);
        return mNavigationLayout.produceLayout(inflater, container);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mNavigationLayout.viewCreated(getMainActivity(), view);
        recycler_view = view.findViewById(R.id.listview);
        //refresh_layout = view.findViewById(R.id.swipe_refresh);
        empty_list_message = view.findViewById(R.id.no_buckets);

        if (recycler_view_state == null && savedInstanceState != null) {
            recycler_view_state = savedInstanceState.getParcelable(Constants.RECYCLE_STATE);
        }

        FabSpeedDialMenu menu = new FabSpeedDialMenu(getContext());
        menu.add(1, Constants.FAB_MENU_NEW_BUCKET, 0, R.string.fab_new_bucket).setIcon(R.drawable.ic_action_add);
        mNavigationLayout.setFabMenu(menu);
        mNavigationLayout.addOnFabMenuItemClickListener(new FabSpeedDial.OnMenuItemClickListener() {
            @Override
            public void onMenuItemClick(FloatingActionButton fab, TextView textView, int itemId) {
                switch (itemId) {
                    case Constants.FAB_MENU_NEW_BUCKET:
                        BucketHelper.addBucket(getMainActivity(), root_path, new BucketHelper.BucketCreatedListener() {
                            @Override
                            public void onCreated(boolean success) {
                                runBucketsTask();
                            }
                        });
                        break;
                }
            }
        });

        Bundle args = getArguments();
        if (args.containsKey(Constants.KEY_DIR_PATH)) {
            root_path = args.getString(Constants.KEY_DIR_PATH, "");
            should_refresh_list = true;
            runBucketsTask();
        }

        mNavigationLayout.setQuickNoteListener(new OnQuickNoteEdit() {
            @Override
            public void openQuickNote(String text) {
                CreateNoteHelper.addQuickNote(getMainActivity(), root_path, "");
            }
        });

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

    private void runBucketsTask() {
        if (buckets_task != null && !buckets_task.isCancelled()) {
            buckets_task.cancel(true);
        }

        buckets_task = new ReadBucketsTask(getContext(), this);
        buckets_task.execute(root_path);
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mNavigationLayout.setTitle(R.string.title_library);
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
        mNavigationLayout.setMovingNoteListener(new OnMovingNoteListener() {
            @Override
            public boolean onMovingNote(ArrayList<MainModel> list) {
                Toast.makeText(getContext(), "Can't move notes here", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        if (ma != null) {
            mNavigationLayout.setMovingNotes(ma, ma.getMovingNotes());
            mNavigationLayout.setSearchState(ma.getSearchState());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (buckets_task != null && !buckets_task.isCancelled()) {
            buckets_task.cancel(true);
        }

        MainActivity ma = getMainActivity();
        if (ma != null && mNavigationLayout != null) {
            ma.setMovingNotes(mNavigationLayout.getMovingNotes());
            ma.setSearchState(mNavigationLayout.getSearchState());
        }
    }

    @Override
    int getLayout() {
        return R.layout.fragment_buckets;
    }

    private void initList(ArrayList<MainModel> list) {
        Log.i(Constants.LOG, "list: "+list.size()+" "+should_refresh_list);
        if (adapter == null || should_refresh_list) {
            adapter = new MainAdapter<>(list);
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

            /*refresh_layout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    refresh_layout.setRefreshing(true);
                    runBucketsTask();
                }
            });
            refresh_layout.setColorSchemeResources(android.R.color.holo_blue_bright,
                    android.R.color.holo_green_light,
                    android.R.color.holo_orange_light,
                    android.R.color.holo_red_light);*/
        }

        //refresh_layout.setRefreshing(false);

        if (list.size() == 0) {
            recycler_view.setVisibility(View.GONE);
            empty_list_message.setVisibility(View.VISIBLE);
            empty_list_message.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    BucketHelper.addBucket(getMainActivity(), root_path, new BucketHelper.BucketCreatedListener() {
                        @Override
                        public void onCreated(boolean success) {
                            runBucketsTask();
                        }
                    });
                }
            });
        } else {
            recycler_view.setVisibility(View.VISIBLE);
            empty_list_message.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onItemClick(View view, int position) {
        MainModel model = adapter.getItem(position);

        MainActivity ma = getMainActivity();
        if (ma == null) return false;
        ma.pushFragment(FoldersFragment.newInstance(root_path, model.getPath()), Constants.FOLDER_FRAGMENT_TAG, Constants.FOLDER_FRAGMENT_TAG + model.getPath());
        return true;
    }

    @Override
    public void onItemLongClick(final int position) {
        MainModel model = adapter.getItem(position);
        File dir = new File(model.getPath());
        File[] files = dir.listFiles();
        int folder_menu = R.menu.popup_folder_menu;
        if (files.length == 0 || files.length == 1 && files[0].getName().equals(".bucket.json")) {
            folder_menu = R.menu.popup_folder_menu_delete;
        }

        SheetMenu.with(getContext())
            .setTitle(model.getTitle())
            .setMenu(folder_menu)
            .setAutoCancel(true)
            .setClick(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    SingleRack folder = (SingleRack) adapter.getItem(position);
                    switch (item.getItemId()) {
                        case R.id.folder_delete:
                            BucketHelper.deleteBucket(getMainActivity(), folder, new BucketHelper.BucketDeletedListener() {
                                @Override
                                public void onDeleted(boolean success) {
                                    if (success) {
                                        Toast.makeText(getContext(), "Bucket deleted!", Toast.LENGTH_SHORT).show();
                                        runBucketsTask();
                                    }
                                }
                            });
                            break;
                        case R.id.folder_rename:
                            BucketHelper.renameBucket(getMainActivity(), folder, new BucketHelper.BucketRenamedListener() {
                                @Override
                                public void onRenamed(boolean success) {
                                    if (success) {
                                        Toast.makeText(getContext(), "Bucket renamed!", Toast.LENGTH_SHORT).show();
                                        runBucketsTask();
                                    }
                                }
                            });
                            break;
                    }
                    return true;
                }
            }).show();
    }

    @Override
    public void BucketsLoaded(ArrayList<MainModel> list) {
        initList(list);
        MainActivity ma = getMainActivity();
        if (ma != null) ma.refreshRackDrawer(list);
    }
}
