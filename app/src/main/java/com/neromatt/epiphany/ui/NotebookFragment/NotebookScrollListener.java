package com.neromatt.epiphany.ui.NotebookFragment;

import android.util.Log;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.model.Adapters.MainAdapter;
import com.neromatt.epiphany.model.DataObjects.MainModel;

import java.util.ArrayList;

import eu.davidea.flexibleadapter.FlexibleAdapter;

public class NotebookScrollListener implements FlexibleAdapter.EndlessScrollListener {

    private MainAdapter<MainModel> mAdapter;

    public NotebookScrollListener(MainAdapter<MainModel> adapter) {
        this.mAdapter = adapter;
    }

    @Override
    public void noMoreLoad(int newItemsSize) {
        //mAdapter.onLoadMoreComplete(null);
        onLoadMoreComplete(null);
    }

    @Override
    public void onLoadMore(int lastPosition, int currentPage) {
        Log.i(Constants.LOG, "> "+currentPage+" "+lastPosition);
    }

    public void onLoadMoreComplete(final ArrayList<MainModel> newItems) {
        mAdapter.onLoadMoreComplete(newItems, 1000L);

        Log.d(Constants.LOG, "EndlessCurrentPage=" + mAdapter.getEndlessCurrentPage());
        Log.d(Constants.LOG, "EndlessPageSize=" + mAdapter.getEndlessPageSize());
        Log.d(Constants.LOG, "EndlessTargetCount=" + mAdapter.getEndlessTargetCount());
    }
}
