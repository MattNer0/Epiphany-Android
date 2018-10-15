package com.neromatt.epiphany.model.DataObjects;

import android.os.Bundle;
import android.util.Log;

import com.neromatt.epiphany.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;

public class SingleRack extends MainModel {
    private String path;
    private String name;
    private int order;

    private boolean isQuickNotes = false;
    private ArrayList<MainModel> folders;

    public SingleRack(String name, String path, JSONObject data) {
        super(headerBuckets);

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
        this.modelType = MainModel.TYPE_RACK;

        if (this.name.equals(Constants.QUICK_NOTES_BUCKET)) this.isQuickNotes = true;
    }

    public SingleRack(Bundle args) {
        super(args, headerBuckets);

        this.path = args.getString("path", "");
        this.name = args.getString("name", "");
        this.order = args.getInt("order", 0);
        this.modelType = MainModel.TYPE_RACK;

        this.folders = args.getParcelableArrayList("folders");

        if (this.name.equals(Constants.QUICK_NOTES_BUCKET)) this.isQuickNotes = true;
    }

    public boolean renameDirectory(String new_name) {
        new_name = new_name.replaceAll("[^\\w. _-]", "");
        File fileFrom = new File(path);
        File fileTo = new File(fileFrom.getParentFile().getPath()+"/"+new_name);
        if (fileFrom.renameTo(fileTo)) {
            this.path = fileTo.getPath();
            this.name = new_name;
            return true;
        }

        return false;
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
        holder.mNotebookTitle.setText(getTitle());
        holder.mNoteCount.setText("");

        if (holder.itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
            StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) holder.itemView.getLayoutParams();
            layoutParams.setFullSpan(true);
        }
    }

    @Override
    public boolean isQuickNotes() {
        return isQuickNotes;
    }

    public int compareOrderTo(SingleRack n1) {
        return Integer.valueOf(order).compareTo(n1.getOrder());
    }

    @Override
    public int getOrder() { return order; }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String new_name) {
        this.name = new_name;
    }

    @Override
    public String getTitle() {
        if (isQuickNotes) return "Quick Notes";
        return name;
    }

    @Override
    public String getPath() {
        return path;
    }
}
