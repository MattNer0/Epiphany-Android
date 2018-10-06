package com.neromatt.epiphany.model.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.DataObjects.SingleNote;
import com.neromatt.epiphany.model.DataObjects.SingleNotebook;
import com.neromatt.epiphany.model.SortBy;
import com.neromatt.epiphany.ui.NotebookFragment.NotebookItemClickListener;
import com.neromatt.epiphany.ui.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class RackAdapter extends RecyclerView.Adapter<RackAdapter.ViewHolder> {

    private Context context;
    private ArrayList<MainModel> data;
    private OnRackClickListener mOnRackClickListener;

    public RackAdapter(Context context, ArrayList<MainModel> content) {
        this.context = context;
        this.data = content;
    }

    public MainModel getItem(int i) {
        return data.get(i);
    }

    public void updateData(ArrayList<MainModel> content) {
        this.data = content;
        notifyDataSetChanged();
    }

    public void setOnClickListener(OnRackClickListener mClickListener) {
        this.mOnRackClickListener = mClickListener;
    }

    @NonNull
    @Override
    public RackAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.rack_row, parent, false);
        return new RackAdapter.ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RackAdapter.ViewHolder holder, int position) {
        holder.bindView(position, getItem(position), mOnRackClickListener);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public int getItemCount() {
        return data.size();
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

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView rackName;

        ViewHolder(View itemView) {
            super(itemView);
            rackName = itemView.findViewById(R.id.rowText);
        }

        private void bindView(final int position, MainModel obj, final OnRackClickListener mClickListener) {
            this.rackName.setText(obj.getTitle());

            if (mClickListener != null) {
                this.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mClickListener.RackClicked(position);
                    }
                });
            }
        }
    }

    public interface OnRackClickListener {
        void RackClicked(int position);
    }
}
