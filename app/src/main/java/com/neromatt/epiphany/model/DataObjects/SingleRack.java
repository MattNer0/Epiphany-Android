package com.neromatt.epiphany.model.DataObjects;

import org.json.JSONException;
import org.json.JSONObject;

public class SingleRack extends MainModel {
    private String path;
    private String name;
    private int order;

    public SingleRack(String name, String path, int order) {
        this.path = path;
        this.name = name;
        this.order = order;
        this.modelType = MainModel.TYPE_RACK;
    }

    public SingleRack(String name, String path, JSONObject data) {
        this.path = path;
        this.name = name;
        try {
            this.order = data.getInt("ordering");
        } catch (JSONException e) {
            this.order = 0;
        }
        this.modelType = MainModel.TYPE_RACK;
    }

    @Override
    public int getOrder() { return order; }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getTitle() {
        if (name.equals("_quick_notes")) return "Quick Notes";
        return name;
    }

    @Override
    public String getPath() {
        return path;
    }
}
