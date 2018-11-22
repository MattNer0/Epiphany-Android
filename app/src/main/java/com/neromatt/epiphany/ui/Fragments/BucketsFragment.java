package com.neromatt.epiphany.ui.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.helper.CreateBucketHelper;
import com.neromatt.epiphany.helper.CreateNoteHelper;
import com.neromatt.epiphany.model.Adapters.FadeInItemAnimator;
import com.neromatt.epiphany.model.Adapters.MainAdapter;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.tasks.ReadBucketsTask;
import com.neromatt.epiphany.ui.MainActivity;
import com.neromatt.epiphany.ui.Navigation.NavigationLayoutFactory;
import com.neromatt.epiphany.ui.Navigation.OnMovingNoteListener;
import com.neromatt.epiphany.ui.Navigation.OnQuickNoteEdit;
import com.neromatt.epiphany.ui.R;

import java.io.File;
import java.util.ArrayList;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import io.github.kobakei.materialfabspeeddial.FabSpeedDial;
import io.github.kobakei.materialfabspeeddial.FabSpeedDialMenu;

public class BucketsFragment extends MyFragment implements FlexibleAdapter.OnItemClickListener, FlexibleAdapter.OnItemLongClickListener, ReadBucketsTask.BucketsListener {

    private String root_path;
    private ReadBucketsTask buckets_task;

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
                        CreateBucketHelper.addBucket(getMainActivity(), root_path, new CreateBucketHelper.BucketCreatedListener() {
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
            runBucketsTask();
        }

        if (getMainActivity().getSearchOpened()) {
            mNavigationLayout.showSearch("", false);
        }

        mNavigationLayout.setQuickNoteListener(new OnQuickNoteEdit() {
            @Override
            public void openQuickNote(String text) {
                CreateNoteHelper.addQuickNote(getMainActivity(), root_path, "");
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
        }
    }

    @Override
    int getLayout() {
        return R.layout.fragment_buckets;
    }

    private void initList(ArrayList<MainModel> list) {
        adapter = new MainAdapter<>(getMainActivity(), list);
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
    public void onItemLongClick(int position) {

    }

    @Override
    public void BucketsLoaded(ArrayList<MainModel> list) {
        initList(list);
        MainActivity ma = getMainActivity();
        if (ma != null) ma.refreshRackDrawer(list);
    }
}
