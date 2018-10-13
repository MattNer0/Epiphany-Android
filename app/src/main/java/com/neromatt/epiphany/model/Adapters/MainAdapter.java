package com.neromatt.epiphany.model.Adapters;

import android.support.annotation.Nullable;

import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.DataObjects.SingleNote;

import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;

public class MainAdapter<T extends IFlexible> extends FlexibleAdapter<T> {

    private int spanCount = 1;

    private MainModel moving_note_folder;
    private SingleNote moving_note;

    public MainAdapter(@Nullable List<T> items) {
        super(items);
    }

    public void setSpanCount(int value) {
        this.spanCount = value;
    }

    public int getSpanCount() {
        return spanCount;
    }

    public void setMovingNote(SingleNote n, MainModel f) {
        this.moving_note = n;
        this.moving_note_folder = f;
    }

    public boolean sameFolderAsMovingNote(MainModel f) {
        return moving_note_folder.getPath().equals(f.getPath());
    }

    public SingleNote getMovingNote() {
        return moving_note;
    }

    public MainModel getMovingNoteFolder() {
        return moving_note_folder;
    }

    public void clearMovingNote() {
        moving_note = null;
        moving_note_folder = null;
    }

    public boolean isMovingNote() {
        return moving_note != null;
    }

    @Override
    public void clear() {
        super.clear();
        clearMovingNote();
    }
}
