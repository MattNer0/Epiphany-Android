package com.neromatt.epiphany.model.DataObjects;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.neromatt.epiphany.model.Adapters.MainAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
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

    @Override
    public Bundle toBundle() {
        Bundle args = super.toBundle();
        args.putString("path", path);
        args.putString("name", name);
        args.putInt("order", order);
        args.putBoolean("quickNotes", isQuickNotes);
        return args;
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
        if (adapter instanceof MainAdapter) {
            MainAdapter m_adapter = (MainAdapter) adapter;
            if (m_adapter.isShowingDragHandle()) {
                holder.dragHandle.setVisibility(View.VISIBLE);
                holder.mNotebookOrder.setText(""+getOrder());
            } else {
                holder.dragHandle.setVisibility(View.GONE);
            }
        }

        holder.mNotebookTitle.setText(getName());

        if (holder.itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
            StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) holder.itemView.getLayoutParams();
            layoutParams.setFullSpan(true);
        }
    }

    @Override
    public boolean isQuickNotes() {
        return isQuickNotes;
    }

    public void saveMeta() {
        JSONObject js = new JSONObject();
        try {
            js.put("ordering", order);

            File file = new File(getPath()+"/.folder.json");
            FileOutputStream stream = new FileOutputStream(file);
            try {
                stream.write(js.toString().getBytes());
            } finally {
                stream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPath() {
        return path;
    }
}
