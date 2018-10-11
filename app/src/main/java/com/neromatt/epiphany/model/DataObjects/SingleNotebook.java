package com.neromatt.epiphany.model.DataObjects;

import android.os.Bundle;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;

import com.neromatt.epiphany.model.Adapters.SimpleHeader;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;

public class SingleNotebook extends MainModel {

    private String path;
    private String name;
    private int order;

    public SingleNotebook(String name, String path, JSONObject data) {
        super(headerFolders);

        this.path = path;
        this.name = name;
        try {
            if (data != null && data.has("ordering")) {
                this.order = data.getInt("ordering");
            } else {
                this.order = 0;
            }
        } catch (JSONException e) {
            Log.e("err", "json "+data.toString());
            this.order = 0;
        }
        this.modelType = MainModel.TYPE_FOLDER;
    }

    public SingleNotebook(Bundle args) {
        super(args, headerFolders);

        this.path = args.getString("path", "");
        this.name = args.getString("name", "");
        this.order = args.getInt("order", 0);
        this.modelType = MainModel.TYPE_FOLDER;
    }

    @Override
    public Bundle toBundle() {
        Bundle args = super.toBundle();
        args.putString("path", path);
        args.putString("name", name);
        args.putInt("order", order);
        return args;
    }

    @Override
    public void bindViewHolder(FlexibleAdapter<IFlexible> adapter, MyViewHolder holder, int position, List<Object> payloads) {
        holder.mNotebookTitle.setText(getName());
        int noteCount = getNotesCount();
        if (noteCount > 0) {
            holder.mNoteCount.setText(String.valueOf(noteCount));
        } else {
            holder.mNoteCount.setText("");
        }

        if (holder.itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
            StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) holder.itemView.getLayoutParams();
            layoutParams.setFullSpan(true);
        }
    }

    public int compareOrderTo(SingleNotebook n1) {
        return Integer.valueOf(order).compareTo(n1.getOrder());
    }

    @Override
    public int getOrder() { return order; }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPath() {
        return path;
    }
}
