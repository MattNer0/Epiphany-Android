package com.neromatt.epiphany.model.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.SortBy;
import com.neromatt.epiphany.ui.Navigation.Breadcrumb;
import com.neromatt.epiphany.ui.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class BreadcrumbAdapter extends RecyclerView.Adapter<BreadcrumbAdapter.ViewHolder> {

    private Context context;
    private ArrayList<Breadcrumb> data;
    private OnBreadcrumbClickListener listener;

    public BreadcrumbAdapter(Context context, ArrayList<Breadcrumb> content, OnBreadcrumbClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.data = content;
    }

    Breadcrumb getItem(int i) {
        return data.get(i);
    }

    @NonNull
    @Override
    public BreadcrumbAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.row_breadcrumb, parent, false);
        return new BreadcrumbAdapter.ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BreadcrumbAdapter.ViewHolder holder, int position) {
        holder.bindView(position, getItem(position), listener);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView rackName;
        LinearLayout underline;

        ViewHolder(View itemView) {
            super(itemView);
            rackName = itemView.findViewById(R.id.rowText);
            underline = itemView.findViewById(R.id.rowUnderline);
        }

        private void bindView(final int position, final Breadcrumb obj, final OnBreadcrumbClickListener listener) {
            if (position == 0) {
                this.rackName.setText(R.string.title_library);
            } else if (obj.name.equalsIgnoreCase(Constants.QUICK_NOTES_BUCKET)) {
                this.rackName.setText("Quick Notes");
            } else {
                this.rackName.setText(obj.name);
            }

            if (obj.current) {
                underline.setVisibility(View.VISIBLE);
            } else {
                underline.setVisibility(View.GONE);
            }

            if (listener != null && !obj.current) {
                this.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        listener.CrumbClicked(obj, position);
                    }
                });
            }
        }
    }

    public interface OnBreadcrumbClickListener {
        void CrumbClicked(Breadcrumb crumb, int position);
    }
}
