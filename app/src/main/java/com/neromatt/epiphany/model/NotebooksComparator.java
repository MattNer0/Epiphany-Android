package com.neromatt.epiphany.model;

import android.content.Context;
import android.preference.PreferenceManager;

import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.DataObjects.SingleNote;
import com.neromatt.epiphany.model.DataObjects.SingleNotebook;
import com.neromatt.epiphany.model.DataObjects.SingleRack;

import java.util.Comparator;

public class NotebooksComparator implements Comparator<MainModel> {

    private SortBy sortByNotes;
    private SortBy sortByFolders;

    public NotebooksComparator(Context context) {
        int pref_note_order = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("pref_note_order", "0"));
        switch (pref_note_order) {
            case 0:
                sortByNotes = SortBy.MODIFIED;
                break;
            case 1:
                sortByNotes = SortBy.CREATED;
                break;
            case 2:
                sortByNotes = SortBy.NAME;
                break;
            default:
                sortByNotes = SortBy.MODIFIED;
        }
        sortByFolders = SortBy.ORDER;
    }

    @Override
    public int compare(MainModel v1, MainModel v2) {
        int result = 0;

        if (v1 instanceof SingleRack && v2 instanceof SingleRack) {

            if (v1.isQuickNotes()) {
                result = 1;
            } else if (v2.isQuickNotes()) {
                result = -1;
            } else if (sortByFolders == SortBy.NAME) {
                result = v1.getName().compareTo(v2.getName());
            } else {
                result = ((SingleRack) v1).compareOrderTo((SingleRack) v2);
            }

        } else if (v1 instanceof SingleNotebook && v2 instanceof SingleNotebook) {

            if (sortByFolders == SortBy.NAME) {
                result = v1.getName().compareTo(v2.getName());
            } else {
                result = ((SingleNotebook) v1).compareOrderTo((SingleNotebook) v2);
            }

        } else if (v1 instanceof SingleNote && v2 instanceof SingleNote) {

            if (sortByNotes == SortBy.MODIFIED) {
                result = ((SingleNote) v1).compareModifiedDateTo((SingleNote) v2);
            } else if (sortByNotes == SortBy.CREATED) {
                result = ((SingleNote) v1).compareCreatedDateTo((SingleNote) v2);
            } else {
                result = v1.getTitle().compareTo(v2.getTitle());
            }

        } else if (v1 instanceof SingleNotebook && v2 instanceof SingleNote) {
            result = -1;
        } else if (v1 instanceof SingleNote && v2 instanceof SingleNotebook) {
            result = 1;
        }
        return result;
    }
}
