package com.neromatt.epiphany.model.Adapters;

import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.neromatt.epiphany.ui.R;

import java.util.List;

import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractHeaderItem;
import eu.davidea.viewholders.FlexibleViewHolder;

public class SimpleHeader extends AbstractHeaderItem<SimpleHeader.HeaderViewHolder> {
    private String title;

    public SimpleHeader(String title) {
        this.title = title;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SimpleHeader && title.equals(((SimpleHeader) o).title);
    }

    public String getTitle() {
        return title;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.header_row;
    }

    @Override
    public HeaderViewHolder createViewHolder(View view, FlexibleAdapter adapter) {
        return new HeaderViewHolder(view, adapter);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter adapter, HeaderViewHolder holder, int position, List payloads) {
        if (payloads.size() > 0) {
            Log.d(this.getClass().getSimpleName(), "HeaderItem Payload " + payloads);
        }
        holder.setFullSpan(true);
        holder.mTitle.setText(getTitle());

        if (holder.itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
            StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) holder.itemView.getLayoutParams();
            layoutParams.setFullSpan(true);
        }
    }

    @Override
    public String toString() {
        return "/" + title;
    }

    static class HeaderViewHolder extends FlexibleViewHolder {

        TextView mTitle;

        HeaderViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter, true); //True for sticky
            mTitle = view.findViewById(R.id.header_title);
        }
    }
}