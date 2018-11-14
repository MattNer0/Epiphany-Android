package com.neromatt.epiphany.ui.Fragments;

import android.content.Intent;
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
import com.neromatt.epiphany.model.Adapters.BreadcrumbAdapter;
import com.neromatt.epiphany.model.Adapters.FadeInItemAnimator;
import com.neromatt.epiphany.model.Adapters.MainAdapter;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.tasks.ReadBucketsTask;
import com.neromatt.epiphany.tasks.ReadFoldersTask;
import com.neromatt.epiphany.ui.MainActivity;
import com.neromatt.epiphany.ui.Navigation.Breadcrumb;
import com.neromatt.epiphany.ui.Navigation.LayoutFactory;
import com.neromatt.epiphany.ui.Navigation.NavigationLayoutFactory;
import com.neromatt.epiphany.ui.Navigation.OnOptionMenuListener;
import com.neromatt.epiphany.ui.R;
import com.neromatt.epiphany.ui.ViewNote;

import java.io.File;
import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import eu.davidea.flexibleadapter.FlexibleAdapter;

public class FoldersFragment extends MyFragment implements FlexibleAdapter.OnItemClickListener, FlexibleAdapter.OnItemLongClickListener, ReadFoldersTask.FoldersListener {

    private ArrayList<String> parent_paths;
    private String folder_path;

    private ReadFoldersTask folders_task;

    public FoldersFragment() { }

    public static FoldersFragment newInstance(ArrayList<String> parents, String path) {
        FoldersFragment f = new FoldersFragment();
        Bundle args = new Bundle();
        args.putString(Constants.KEY_DIR_PATH, path);
        args.putStringArrayList(Constants.KEY_DIR_PARENTS, parents);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mNavigationLayout = new NavigationLayoutFactory(true, true, false, true, this);
        return mNavigationLayout.produceLayout(inflater, container);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mNavigationLayout.viewCreated(getMainActivity(), view);
        recycler_view = view.findViewById(R.id.listview);

        Bundle args = getArguments();
        if (args.containsKey(Constants.KEY_DIR_PATH)) {
            folder_path = args.getString(Constants.KEY_DIR_PATH, "");
            parent_paths = args.getStringArrayList(Constants.KEY_DIR_PARENTS);
            if (parent_paths == null) parent_paths = new ArrayList<>();

            if (folders_task != null && !folders_task.isCancelled()) {
                folders_task.cancel(true);
            }

            folders_task = new ReadFoldersTask(getContext(), this);
            folders_task.execute(folder_path);
        }

        if (parent_paths != null && parent_paths.size() > 0) {
            ArrayList<Breadcrumb> breadcrumbs = new ArrayList<>();
            for (String p : parent_paths) {
                File f = new File(p);
                breadcrumbs.add(new Breadcrumb(f.getName(), f.getPath()));
            }
            if (folder_path != null && !folder_path.isEmpty()) {
                File f = new File(folder_path);
                breadcrumbs.add(new Breadcrumb(f.getName(), f.getPath(), true));
            }
            mNavigationLayout.setBreadcrumbs(getContext(), breadcrumbs, new BreadcrumbAdapter.OnBreadcrumbClickListener() {
                @Override
                public void CrumbClicked(Breadcrumb crumb, int position) {
                    MainActivity ma = getMainActivity();
                    if (ma == null) return;

                    if (position == 0) {
                        ma.pushFragment(BucketsFragment.newInstance(crumb.value), Constants.BUCKETS_FRAGMENT_TAG);
                    } else {
                        ArrayList<String> parent = new ArrayList<>();
                        for (int i=0; i<position; i++) {
                            parent.add(parent_paths.get(i));
                        }
                        ma.pushFragment(FoldersFragment.newInstance(parent, crumb.value), Constants.FOLDER_FRAGMENT_TAG);
                    }
                }
            });
        }
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (folder_path != null && !folder_path.isEmpty()) {
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
        }
    }

    @Override
    int getLayout() {
        return R.layout.fragment_folders;
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
        if (model.isFolder()) {
            ArrayList<String> parent = new ArrayList<>();
            parent.addAll(parent_paths);
            parent.add(folder_path);

            MainActivity ma = getMainActivity();
            if (ma == null) return false;

            ma.pushFragment(FoldersFragment.newInstance(parent, model.getPath()), Constants.FOLDER_FRAGMENT_TAG);
        } else if (model.getType() == MainModel.TYPE_MARKDOWN_NOTE) {
            Intent intent = new Intent(getMainActivity(), ViewNote.class);
            intent.putExtra("note", model.toBundle());
            startActivityForResult(intent, Constants.NOTE_PREVIEW_REQUEST_CODE);
        }
        return true;
    }

    @Override
    public void onItemLongClick(int position) {

    }

    @Override
    public void FoldersLoaded(ArrayList<MainModel> list) {
        initList(list);
    }
}
