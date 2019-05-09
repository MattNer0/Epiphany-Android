package com.neromatt.epiphany.ui.Fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.model.Adapters.MainAdapter;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.ui.MainActivity;
import com.neromatt.epiphany.ui.Navigation.LayoutFactory;
import com.neromatt.epiphany.ui.Navigation.NavigationLayoutFactory;
import com.neromatt.epiphany.ui.Navigation.OnOptionMenuListener;
import com.neromatt.epiphany.ui.R;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import eu.davidea.flexibleadapter.common.SmoothScrollStaggeredLayoutManager;

public class MyFragment extends Fragment implements LayoutFactory, OnOptionMenuListener {

    NavigationLayoutFactory mNavigationLayout;

    RecyclerView recycler_view;
    MainAdapter<MainModel> adapter;
    Parcelable recycler_view_state;

    MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }

    int getLayout() {
        return 0;
    }

    RecyclerView.LayoutManager getLayoutManager() {
        return getLayoutManager(
            PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("pref_staggered_layout", false)
        );
    }

    private RecyclerView.LayoutManager getLayoutManager(boolean staggered) {
        if (staggered) {
            if (adapter != null) adapter.setSpanCount(2);
            return new SmoothScrollStaggeredLayoutManager(getContext(), 2);
        }
        if (adapter != null) adapter.setSpanCount(1);
        return new SmoothScrollStaggeredLayoutManager(getContext(), 1);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
        if (mNavigationLayout != null) {
            mNavigationLayout.onCreateMenu(getContext(), menu);
            mNavigationLayout.showSearchIfOpened();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mNavigationLayout.onOptionsItemSelected(item, getMainActivity(), this)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View produceLayout(LayoutInflater inflater, @Nullable ViewGroup container) {
        return inflater.inflate(getLayout(), container, false);
    }

    @Override
    public void updateLayoutList(boolean staggered) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor prefs_edit = prefs.edit();
        prefs_edit.putBoolean("pref_staggered_layout", staggered);
        prefs_edit.apply();

        if (recycler_view != null) {
            recycler_view.setLayoutManager(getLayoutManager(staggered));
        }
    }

    @Override
    public void toggleDragHandle() {
        if (adapter != null) {
            adapter.toggleDragHandle();
            adapter.notifyDataSetChanged();

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor prefs_edit = prefs.edit();
            prefs_edit.putBoolean("pref_drag_handle", adapter.isShowingDragHandle());
            prefs_edit.apply();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (recycler_view != null) {
            recycler_view_state = recycler_view.getLayoutManager().onSaveInstanceState();
            state.putParcelable(Constants.RECYCLE_STATE, recycler_view_state);
        }
    }
}
