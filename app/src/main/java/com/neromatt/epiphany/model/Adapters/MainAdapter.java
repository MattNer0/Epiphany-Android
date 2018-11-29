package com.neromatt.epiphany.model.Adapters;

import java.util.List;

import androidx.annotation.Nullable;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;

public class MainAdapter<T extends IFlexible> extends FlexibleAdapter<T> {

    private int spanCount = 1;

    public MainAdapter(@Nullable List<T> items) {
        super(items);
    }

    public void setSpanCount(int value) {
        this.spanCount = value;
    }

    public int getSpanCount() {
        return spanCount;
    }

    @Override
    public void clear() {
        super.clear();
    }
}
