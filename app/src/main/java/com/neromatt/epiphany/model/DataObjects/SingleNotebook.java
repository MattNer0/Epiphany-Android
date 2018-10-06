package com.neromatt.epiphany.model.DataObjects;

import android.os.Bundle;
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
    private int noteCount;
    private int order;

    public SingleNotebook(String name, String path, int noteCount, JSONObject data) {
        super(headerFolders);

        this.path = path;
        this.name = name;
        this.noteCount = noteCount;
        try {
            if (data.has("ordering")) {
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
        this.noteCount = args.getInt("noteCount", 0);
        this.order = args.getInt("order", 0);
        this.modelType = MainModel.TYPE_FOLDER;
    }

    @Override
    public Bundle toBundle() {
        Bundle args = super.toBundle();
        args.putString("path", path);
        args.putString("name", name);
        args.putInt("noteCount", noteCount);
        args.putInt("order", order);
        return args;
    }

    @Override
    public void bindViewHolder(FlexibleAdapter<IFlexible> adapter, MyViewHolder holder, int position, List<Object> payloads) {
        holder.mNotebookTitle.setText(getName());
        if (noteCount > 0) {
            holder.mNoteCount.setText(""+noteCount);
        } else {
            holder.mNoteCount.setText("");
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

    public int getNoteCount(){
        return this.noteCount;
    }
}
