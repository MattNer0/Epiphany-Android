package com.neromatt.epiphany.ui.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.model.Adapters.FadeInItemAnimator;
import com.neromatt.epiphany.model.Adapters.MainAdapter;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.tasks.ReadBucketsTask;
import com.neromatt.epiphany.ui.MainActivity;
import com.neromatt.epiphany.ui.Navigation.LayoutFactory;
import com.neromatt.epiphany.ui.Navigation.NavigationLayoutFactory;
import com.neromatt.epiphany.ui.Navigation.OnOptionMenuListener;
import com.neromatt.epiphany.ui.R;

import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.common.SmoothScrollStaggeredLayoutManager;

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
        mNavigationLayout = new NavigationLayoutFactory(true, false, false, true, this);
        return mNavigationLayout.produceLayout(inflater, container);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mNavigationLayout.viewCreated(getMainActivity(), view);
        recycler_view = view.findViewById(R.id.listview);

        Bundle args = getArguments();
        if (args.containsKey(Constants.KEY_DIR_PATH)) {
            root_path = args.getString(Constants.KEY_DIR_PATH, "");

            if (buckets_task != null && !buckets_task.isCancelled()) {
                buckets_task.cancel(true);
            }

            buckets_task = new ReadBucketsTask(getContext(), this);
            buckets_task.execute(root_path);
        }
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

        ArrayList<String> parent = new ArrayList<>();
        parent.add(root_path);

        MainActivity ma = getMainActivity();
        if (ma == null) return false;
        ma.pushFragment(FoldersFragment.newInstance(parent, model.getPath()), Constants.FOLDER_FRAGMENT_TAG);
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
