package com.neromatt.epiphany.model.DataObjects;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

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
        super();

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
        this._model_type = MainModel.TYPE_FOLDER;
    }

    public SingleNotebook(Bundle args) {
        super(args);

        this.path = args.getString("path", "");
        this.name = args.getString("name", "");
        this.order = args.getInt("order", 0);
        this._model_type = MainModel.TYPE_FOLDER;
        this.isQuickNotes = args.getBoolean("quickNotes", false);
    }

    public SingleNotebook(JsonObject args) {
        super(args);

        this.path = args.get("path").getAsString();
        this.name = args.get("name").getAsString();
        this.order = args.get("order").getAsInt();
        this._model_type = MainModel.TYPE_FOLDER;
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
        int contentsCount = getContentCount();
        if (noteCount > 0) {
            holder.mNoteCount.setText(String.valueOf(noteCount));
            holder.mNoteCountContainer.setVisibility(View.VISIBLE);
        } else {
            holder.mNoteCountContainer.setVisibility(View.GONE);
        }

        if (contentsCount-noteCount > 0) {
            holder.mFolderCount.setText(String.valueOf(contentsCount-noteCount));
            holder.mFolderCountContainer.setVisibility(View.VISIBLE);
        } else {
            holder.mFolderCountContainer.setVisibility(View.GONE);
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

    @Override
    public boolean delete() {
        File dir = new File(path);
        if (dir.exists() && dir.isDirectory()) {
            for (File f : dir.listFiles()) {
                if (f.isDirectory()) return false;
                String filename = f.getName();
                String extension = filename.substring(filename.lastIndexOf('.') + 1);
                if (extension.equals(".md") || extension.equals(".txt")) return false;
                if (!f.delete()) return false;
            }
            return dir.delete();
        }

        return false;
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
