package com.neromatt.epiphany.model.DataObjects;

import android.os.Bundle;
import android.util.Log;

import com.google.gson.JsonObject;
import com.neromatt.epiphany.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.List;

import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;

public class SingleNotebook extends MainModel {

    private String path;
    private String name;
    private int order;

    private boolean isQuickNotes = false;

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
        this.isQuickNotes = args.getBoolean("quickNotes", false);
    }

    public SingleNotebook(JsonObject args) {
        super(args, headerFolders);

        this.path = args.get("path").getAsString();
        this.name = args.get("name").getAsString();
        this.order = args.get("order").getAsInt();
        this.modelType = MainModel.TYPE_FOLDER;
        this.isQuickNotes = args.has("quickNotes");
    }

    @Override
    public Bundle toBundle() {
        Bundle args = super.toBundle();
        args.putString("path", path);
        args.putString("name", name);
        args.putInt("order", order);
        args.putBoolean("quickNotes", isQuickNotes);
        return args;
    }

    @Override
    public JsonObject toJson() {
        JsonObject obj = super.toJson();
        obj.addProperty("path", path);
        obj.addProperty("name", name);
        obj.addProperty("order", order);
        if (isQuickNotes) obj.addProperty("quickNotes", true);
        return obj;
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

    @Override
    public boolean isQuickNotes() {
        return isQuickNotes;
    }

    public void setQuickNotesFolder() {
        this.isQuickNotes = true;
    }

    public int compareOrderTo(SingleNotebook n1) {
        return Integer.valueOf(order).compareTo(n1.getOrder());
    }

    @Override
    public String getTitle() {
        if (isQuickNotes) return "Quick Notes";
        return name;
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
