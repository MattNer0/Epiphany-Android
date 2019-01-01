package com.neromatt.epiphany.model.Adapters;

import java.util.List;

import androidx.annotation.Nullable;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;

public class MainAdapter<T extends IFlexible> extends FlexibleAdapter<T> {

    private int span_count = 1;
    private boolean show_drag_handle = false;

    public MainAdapter(@Nullable List<T> items) {
        super(items);
    }

    public void setSpanCount(int value) {
        this.span_count = value;
    }

    public int getSpanCount() {
        return span_count;
    }

    public void toggleDragHandle() {
        this.show_drag_handle = !this.show_drag_handle;
    }

    public boolean isShowingDragHandle() {
        return this.show_drag_handle;
    }

    @Override
    public void clear() {
        super.clear();
    }
}
