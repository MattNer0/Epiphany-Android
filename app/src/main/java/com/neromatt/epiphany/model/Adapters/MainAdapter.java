package com.neromatt.epiphany.model.Adapters;

import com.neromatt.epiphany.model.DataObjects.SingleNote;
import com.neromatt.epiphany.ui.MainActivity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;

public class MainAdapter<T extends IFlexible> extends FlexibleAdapter<T> {

    private int spanCount = 1;
    private WeakReference<MainActivity> ma;

    public MainAdapter(@Nullable List<T> items) {
        super(items);
    }

    public MainAdapter(MainActivity ma, @Nullable List<T> items) {
        super(items);
        this.ma = new WeakReference<>(ma);
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

    public SingleNote getMovingNote() {
        if (ma != null && ma.get() != null) {
            //return ma.get().getMovingNote();
        }
        return null;
    }

    public boolean isMovingNote() {
        //return ma.get() != null && ma.get().isMovingNote();
        return false;
    }
}
