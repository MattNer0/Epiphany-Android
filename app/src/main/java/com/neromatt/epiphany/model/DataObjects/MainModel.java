package com.neromatt.epiphany.model.DataObjects;

import android.animation.Animator;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.ui.R;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import androidx.annotation.NonNull;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.Payload;
import eu.davidea.flexibleadapter.helpers.AnimatorHelper;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.flexibleadapter.items.IFlexible;
import eu.davidea.viewholders.FlexibleViewHolder;

public class MainModel extends AbstractFlexibleItem<MainModel.MyViewHolder> implements Parcelable {

    private static final int TYPE_NULL = 0;
    private static final int TYPE_PROGRESS_LOADING = -1;
    public static final int TYPE_RACK = 1;
    public static final int TYPE_FOLDER = 2;
    public static final int TYPE_MARKDOWN_NOTE = 3;

    private LoadingStatusEnum _loading_status = LoadingStatusEnum.MORE_TO_LOAD;

    private UUID _uuid;
    int _model_type;

    public MainModel() {
        super();
        this._model_type = TYPE_NULL;
        this._uuid = UUID.randomUUID();
    }

    public MainModel(Bundle args) {
        super();
        _model_type = TYPE_NULL;
        String uuid = args.getString(Constants.KEY_UUID, "");
        if (uuid.isEmpty()) {
            this._uuid = UUID.randomUUID();
        } else {
            this._uuid = UUID.fromString(uuid);
        }
    }

    public MainModel(int modelType) {
        super();
        _model_type = modelType;
    }

    public String getName() {
        return "";
    }
    public String getTitle() {
        return getName();
    }
    public String getPath() {
        return "";
    }

    public int getType() {
        return _model_type;
    }

    public boolean isQuickNotes() { return false; }

    UUID getUuid() {
        return _uuid;
    }

    public boolean isBucket() { return _model_type == TYPE_RACK; }
    public boolean isFolder() {
        return _model_type == TYPE_FOLDER;
    }
    public boolean isNote() { return _model_type == TYPE_MARKDOWN_NOTE; }

    public int getOrder() { return 0; }

    public static Locale getCurrentLocale(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.getResources().getConfiguration().getLocales().get(0);
        } else {
            return context.getResources().getConfiguration().locale;
        }
    }

    @Override
    public boolean equals(Object inObject) {
        if (inObject instanceof MainModel) {
            MainModel inItem = (MainModel) inObject;
            return this.getType() == inItem.getType() && this.getUuid().equals(inItem.getUuid());
        }
        return false;
    }

    public boolean equalsUUID(Object inObject) {
        if (inObject instanceof MainModel) {
            MainModel inItem = (MainModel) inObject;
            return this.getUuid().equals(inItem.getUuid());
        }
        return false;
    }

    @Override
    public int hashCode() {
        String h = getPath()+getName();
        return h.hashCode();
    }

    @Override
    public int getLayoutRes() {
        switch (_model_type) {
            case MainModel.TYPE_PROGRESS_LOADING:
                return R.layout.progress_item;
            case MainModel.TYPE_MARKDOWN_NOTE:
                return R.layout.row_markdown_note;
            case MainModel.TYPE_RACK:
                if (this.isQuickNotes()) {
                    return R.layout.row_quick_notes;
                } else {
                    return R.layout.row_bucket;
                }
            case MainModel.TYPE_FOLDER:
                return R.layout.row_folder;
            default:
                Log.w("adapter", "invalid view type: "+_model_type);
                return R.layout.row_folder;
        }
    }

    @Override
    public MyViewHolder createViewHolder(View view, FlexibleAdapter<IFlexible> adapter) {
        return new MyViewHolder(view, adapter, _model_type);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter<IFlexible> adapter, MyViewHolder holder, int position, List<Object> payloads) {
        if (_model_type == MainModel.TYPE_PROGRESS_LOADING) {
            if (!adapter.isEndlessScrollEnabled()) {
                setStatus(LoadingStatusEnum.DISABLE_ENDLESS);
            } else if (payloads.contains(Payload.NO_MORE_LOAD)) {
                setStatus(LoadingStatusEnum.NO_MORE_LOAD);
            }

            Context context = holder.itemView.getContext();
            holder.progressBar.setVisibility(View.GONE);
            holder.progressMessage.setVisibility(View.VISIBLE);

            switch (this._loading_status) {
                case NO_MORE_LOAD:
                    holder.progressMessage.setText(context.getString(R.string.no_more_load_retry));
                    // Reset to default status for next binding
                    setStatus(LoadingStatusEnum.MORE_TO_LOAD);
                    break;
                case DISABLE_ENDLESS:
                    holder.progressMessage.setText(context.getString(R.string.endless_disabled));
                    break;
                case ON_CANCEL:
                    holder.progressMessage.setText(context.getString(R.string.endless_cancel));
                    // Reset to default status for next binding
                    setStatus(LoadingStatusEnum.MORE_TO_LOAD);
                    break;
                case ON_ERROR:
                    holder.progressMessage.setText(context.getString(R.string.endless_error));
                    // Reset to default status for next binding
                    setStatus(LoadingStatusEnum.MORE_TO_LOAD);
                    break;
                default:
                    holder.progressBar.setVisibility(View.VISIBLE);
                    holder.progressMessage.setVisibility(View.GONE);
                    break;
            }
        }
    }

    public Bundle toBundle() {
        Bundle args = new Bundle();
        args.putInt("modelType", this._model_type);
        args.putString(Constants.KEY_UUID, this._uuid.toString());
        return args;
    }

    private void setStatus(LoadingStatusEnum status) {
        this._loading_status = status;
    }

    public boolean delete() {
        return false;
    }

    public boolean equalsUUID(String uuid) {
        return _uuid.equals(UUID.fromString(uuid));
    }

    @Override
    public String toString() {
        return "Model[ " + getPath() + " ][ "+getName()+" ]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (this instanceof  SingleNote) dest.writeBundle(((SingleNote) this).toLightBundle());
        else dest.writeBundle(toBundle());
    }

    public static final Parcelable.Creator<MainModel> CREATOR = new Parcelable.Creator<MainModel>() {
            public MainModel createFromParcel(Parcel in) {
                Bundle args = in.readBundle(getClass().getClassLoader());
                switch(args.getInt("modelType", 0)) {
                    case TYPE_RACK:
                        return new SingleRack(args);
                    case TYPE_FOLDER:
                        return new SingleNotebook(args);
                    case TYPE_MARKDOWN_NOTE:
                        return new SingleNote(args);
                    default:
                        return new MainModel();
                }
            }

            public MainModel[] newArray(int size) {
                return new MainModel[size];
            }
        };

    public class MyViewHolder extends FlexibleViewHolder {

        TextView mNotebookTitle;
        TextView mNoteSummary;
        TextView mNoteTime;
        ImageView mNoteIcon;

        ImageView mNotePhoto;

        ProgressBar progressBar;
        TextView progressMessage;

        RelativeLayout iconContainer;

        TextView mNotebookOrder;
        LinearLayout dragHandle;

        MyViewHolder(View view, FlexibleAdapter adapter, int type) {
            super(view, adapter);
            this.mNotebookTitle = view.findViewById(R.id.notebook_title);
            this.mNoteIcon = view.findViewById(R.id.notebook_icon);

            if (type == TYPE_MARKDOWN_NOTE) {
                this.mNoteSummary = view.findViewById(R.id.notebook_summary);
                this.mNoteTime = view.findViewById(R.id.notebook_time);
                this.mNotePhoto = view.findViewById(R.id.note_photo);
            }

            this.progressBar = view.findViewById(R.id.progress_bar);
            this.progressMessage = view.findViewById(R.id.progress_message);

            this.iconContainer = view.findViewById(R.id.icon_container);

            this.dragHandle = view.findViewById(R.id.notebook_handle);
            this.mNotebookOrder = view.findViewById(R.id.notebook_order);

            if (type == TYPE_RACK) {
                setDragHandleView(dragHandle);
            } else if (type == TYPE_FOLDER) {
                setDragHandleView(dragHandle);
            }
        }

        @Override
        public String toString() {
            return "MainModel[" + super.toString() + "]";
        }

        @Override
        public void scrollAnimators(@NonNull List<Animator> animators, int position, boolean isForward) {
            AnimatorHelper.alphaAnimator(animators, itemView, 0f);
        }
    }

    private enum LoadingStatusEnum {
        MORE_TO_LOAD,
        DISABLE_ENDLESS,
        NO_MORE_LOAD,
        ON_CANCEL,
        ON_ERROR
    }
}
