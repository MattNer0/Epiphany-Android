package com.neromatt.epiphany.model.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.SortBy;
import com.neromatt.epiphany.ui.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class RackAdapter extends ArrayAdapter {

    private Context context;
    private ArrayList<MainModel> data;

    public RackAdapter(Context context, int resource,ArrayList<MainModel> content) {
        super(context,resource,content);
        this.context = context;
        this.data = content;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int i) {
        return data.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View row = inflater.inflate(R.layout.rack_row, viewGroup, false);
        TextView rackName = row.findViewById(R.id.rowText);
        rackName.setText(data.get(i).getName());

        return row;
    }

    public void sort(SortBy sortBy){

        if (sortBy == SortBy.ORDER) {
            Collections.sort(data, new Comparator<MainModel>() {

                @Override
                public int compare(MainModel singleRack, MainModel t1) {
                    return singleRack.getOrder() - t1.getOrder();
                }


            });
        } else if (sortBy == SortBy.NAME) {
            Collections.sort(data, new Comparator<MainModel>() {

                @Override
                public int compare(MainModel singleRack, MainModel t1) {
                    return singleRack.getName().compareTo(t1.getName());
                }


            });
        }
        this.notifyDataSetChanged();
    }
}
