package com.neromatt.epiphany.model.Adapters;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
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

public class NotebookAdapter extends RecyclerView.Adapter<NotebookAdapter.ViewHolder> {
    private ArrayList<MainModel> data;
    private NotebookItemClickListener clickListener;

    static class ViewHolder extends RecyclerView.ViewHolder {

        View itemView;

        TextView mNotebookTitle;
        TextView mNoteCount;
        TextView mNoteSummary;

        private ViewHolder(View v) {
            super(v);
            this.itemView = v;
            this.mNotebookTitle = v.findViewById(R.id.notebook_title);
            this.mNoteCount = v.findViewById(R.id.note_count);
            this.mNoteSummary = v.findViewById(R.id.notebook_summary);
        }

        private void bindView(final MainModel obj, final NotebookItemClickListener mClickListener) {
            this.mNotebookTitle.setText(obj.getName());

            if (obj.isBucket()) {
                this.mNoteCount.setText("");
            } else if (obj.isFolder()) {
                if (obj.getNotesCount() > 0) {
                    this.mNoteCount.setText(String.valueOf(obj.getNotesCount()));
                } else {
                    this.mNoteCount.setText("");
                }

            } else {
                //this.mNoteCount.setText("");
                SingleNote objNote = (SingleNote) obj;
                if (this.mNoteSummary != null) this.mNoteSummary.setText(objNote.getSummary());
            }

            if (mClickListener != null) {
                this.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mClickListener.onClick(obj);
                    }
                });
            }
        }
    }

    public NotebookAdapter(ArrayList<MainModel> content) {
        this.data = content;
    }

    public void setOnClickListener(NotebookItemClickListener mClickListener) {
        this.clickListener = mClickListener;
    }

    public void updateList(ArrayList<MainModel> content) {
        this.data = content;
    }

    public void removeItem(int i) {
        data.remove(i);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public MainModel getItem(int i) {
        return data.get(i);
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getType();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        switch (viewType) {
            case MainModel.TYPE_MARKDOWN_NOTE:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.markdown_note_row, parent, false);
                break;
            case MainModel.TYPE_RACK:
            case MainModel.TYPE_FOLDER:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.notebook_row, parent, false);
                break;
            default:
                Log.w("adapter", "invalid view type: "+viewType);
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.notebook_row, parent, false);
        }
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bindView(getItem(position), clickListener);
    }

    public void sort(final SortBy sortByFolders, int note_order) {

        final SortBy sortByNotes;
        switch (note_order) {
            case 0:
                sortByNotes = SortBy.MODIFIED;
                break;
            case 1:
                sortByNotes = SortBy.CREATED;
                break;
            case 2:
                sortByNotes = SortBy.NAME;
                break;
            default:
                sortByNotes = SortBy.MODIFIED;
        }

        Collections.sort(data, new Comparator<MainModel>() {
            @Override
            public int compare(MainModel singleNotebook, MainModel t1) {
                if (singleNotebook.getType() == t1.getType() && singleNotebook.getType() == MainModel.TYPE_RACK) {
                    if (sortByFolders == SortBy.NAME) {
                        return singleNotebook.getName().compareTo(t1.getName());
                    } else {
                        return singleNotebook.getOrder() - t1.getOrder();
                    }
                }

                if (singleNotebook.getType() == t1.getType() && singleNotebook.getType() == MainModel.TYPE_FOLDER) {
                    if (sortByFolders == SortBy.NAME) {
                        return singleNotebook.getName().compareTo(t1.getName());
                    } else {
                        return singleNotebook.getOrder() - t1.getOrder();
                    }
                }

                if (singleNotebook.getType() == t1.getType() && singleNotebook.getType() == MainModel.TYPE_MARKDOWN_NOTE) {
                    SingleNote singleNote = (SingleNote) singleNotebook;
                    SingleNote n1 = (SingleNote) t1;

                    if (sortByNotes == SortBy.MODIFIED) {
                        return n1.getLastModifiedDate().compareTo(singleNote.getLastModifiedDate());
                    } else if (sortByNotes == SortBy.CREATED) {
                        return n1.getCreatedDate().compareTo(singleNote.getCreatedDate());
                    } else {
                        return singleNote.getName().compareTo(n1.getName());
                    }
                }

                if (singleNotebook.getType() == MainModel.TYPE_FOLDER) {
                    return -1;
                }

                return 0;
            }
        });

        this.notifyDataSetChanged();
    }
}
