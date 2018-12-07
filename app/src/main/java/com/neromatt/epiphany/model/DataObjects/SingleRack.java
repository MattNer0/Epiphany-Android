package com.neromatt.epiphany.model.DataObjects;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.helper.IconHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;

public class SingleRack extends MainModel {
    private String path;
    private String name;
    private int order;

    private boolean isQuickNotes = false;
    private String color = null;

    private int grid_columns = 1;

    public SingleRack(String name, String path, JSONObject data) {
        super();

        this.path = path;
        this.name = name;
        if (data != null) {
            try {
                if (data.has("ordering")) {
                    this.order = data.getInt("ordering");
                } else {
                    this.order = 0;
                }

                if (data.has("color")) {
                    this.color = data.getString("color");
                } else {
                    this.color = null;
                }
            } catch (JSONException e) {
                Log.e("err", "json " + data.toString());
                this.order = 0;
                this.color = null;
            }
        }
        this._model_type = MainModel.TYPE_RACK;

        if (this.name.equals(Constants.QUICK_NOTES_BUCKET)) this.isQuickNotes = true;
    }

    public SingleRack(Bundle args) {
        super(args);

        this.path = args.getString("path", "");
        this.name = args.getString("name", "");
        this.order = args.getInt("order", 0);
        this._model_type = MainModel.TYPE_RACK;
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

    public void setViewOptions(int grid_columns) {
        this.grid_columns = grid_columns;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void saveMeta() {
        JSONObject js = new JSONObject();
        try {
            js.put("ordering", order);
            js.put("color", color);

            File file = new File(getPath()+"/.bucket.json");
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
        if (holder.mNotebookTitle != null) {
            if (grid_columns == 1) {
                holder.mNotebookTitle.setText(getTitle());
                holder.mNotebookTitle.setVisibility(View.VISIBLE);
            } else {
                holder.mNotebookTitle.setVisibility(View.GONE);
            }
        }

        String new_color = IconHelper.setIcon(holder.iconContainer, getTitle(), grid_columns, color);
        if (!new_color.equals(color)) {
            color = new_color;
            saveMeta();
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
