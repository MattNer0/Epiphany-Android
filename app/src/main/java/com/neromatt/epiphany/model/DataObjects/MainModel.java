package com.neromatt.epiphany.model.DataObjects;

import android.animation.Animator;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.neromatt.epiphany.model.Adapters.SimpleHeader;
import com.neromatt.epiphany.model.NotebooksComparator;
import com.neromatt.epiphany.model.Path;
import com.neromatt.epiphany.ui.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewPropertyAnimatorListener;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.helpers.AnimatorHelper;
import eu.davidea.flexibleadapter.items.AbstractSectionableItem;
import eu.davidea.flexibleadapter.items.IFlexible;
import eu.davidea.viewholders.AnimatedViewHolder;
import eu.davidea.viewholders.FlexibleViewHolder;

public class MainModel extends AbstractSectionableItem<MainModel.MyViewHolder, SimpleHeader> implements Parcelable {

    public static SimpleHeader headerBuckets = new SimpleHeader("Buckets");
    public static SimpleHeader headerFolders = new SimpleHeader("Folders");
    public static SimpleHeader headerNotes = new SimpleHeader("Notes");

    private static final int TYPE_NULL = 0;
    public static final int TYPE_RACK = 1;
    public static final int TYPE_FOLDER = 2;
    public static final int TYPE_MARKDOWN_NOTE = 3;

    private UUID uuid;
    int modelType;

    private ArrayList<MainModel> model_contents;
    private ArrayList<MainModel> model_notes;
    private WeakReference<Context> _context_ref;
    private int _load_count;

    public MainModel(SimpleHeader header) {
        super(header);
        this.modelType = TYPE_NULL;
        this.model_notes = new ArrayList<>();
        this.model_contents = new ArrayList<>();
        this.uuid = UUID.randomUUID();
    }

    public MainModel(Bundle args, SimpleHeader header) {
        super(header);
        this.modelType = TYPE_NULL;
        this.model_notes = new ArrayList<>();
        this.model_contents = args.getParcelableArrayList("_contents");
        String uuid = args.getString("_uuid", "");
        if (uuid.isEmpty()) {
            this.uuid = UUID.randomUUID();
        } else {
            this.uuid = UUID.fromString(uuid);
        }
        if (model_contents == null) {
            this.model_contents = new ArrayList<>();
        } else {
            for (MainModel model : model_contents) {
                if (model.isNote()) {
                    this.model_notes.add(model);
                }
            }
        }
    }

    public MainModel(JsonObject args, SimpleHeader header) {
        super(header);
        this.modelType = TYPE_NULL;
        this.model_notes = new ArrayList<>();
        this.model_contents = new ArrayList<>();
        //this.model_contents = args.getParcelableArrayList("_contents");

        String uuid = args.get("_uuid").getAsString();
        if (uuid.isEmpty()) {
            this.uuid = UUID.randomUUID();
        } else {
            this.uuid = UUID.fromString(uuid);
        }

        JsonArray json_contents = args.getAsJsonArray("_contents");
        for (JsonElement element : json_contents) {
            int type = element.getAsJsonObject().get("modelType").getAsInt();
            switch (type) {
                case TYPE_RACK:
                    model_contents.add(new SingleRack(element.getAsJsonObject()));
                    break;
                case TYPE_FOLDER:
                    model_contents.add(new SingleNotebook(element.getAsJsonObject()));
                    break;
                case TYPE_MARKDOWN_NOTE:
                    SingleNote n = new SingleNote(element.getAsJsonObject());
                    model_contents.add(n);
                    model_notes.add(n);
                    break;
            }
        }
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
        return modelType;
    }

    public boolean isQuickNotes() { return false; }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isBucket() { return modelType == TYPE_RACK; }
    public boolean isFolder() {
        return modelType == TYPE_FOLDER;
    }
    public boolean isNote() { return modelType == TYPE_MARKDOWN_NOTE; }

    public int getOrder() { return 0; }

    public static Locale getCurrentLocale(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.getResources().getConfiguration().getLocales().get(0);
        } else {
            //noinspection deprecation
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

    @Override
    public int hashCode() {
        return getPath().hashCode();
    }

    @Override
    public int getLayoutRes() {
        switch (modelType) {
            case MainModel.TYPE_MARKDOWN_NOTE:
                return R.layout.markdown_note_row;
            case MainModel.TYPE_RACK:
                if (this.isQuickNotes()) {
                    return R.layout.row_quick_notes;
                } else {
                    return R.layout.row_bucket;
                }
            case MainModel.TYPE_FOLDER:
                return R.layout.row_folder;
            default:
                Log.w("adapter", "invalid view type: "+modelType);
                return R.layout.row_folder;
        }
    }

    @Override
    public MyViewHolder createViewHolder(View view, FlexibleAdapter<IFlexible> adapter) {
        return new MyViewHolder(view, adapter);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter<IFlexible> adapter, MyViewHolder holder, int position, List<Object> payloads) {
    }

    public Bundle toBundle() {
        Bundle args = new Bundle();
        args.putInt("modelType", this.modelType);
        args.putParcelableArrayList("_contents", this.model_contents);
        args.putString("_uuid", this.uuid.toString());
        return args;
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("modelType", this.modelType);
        obj.addProperty("_uuid", this.uuid.toString());

        JsonArray content_array = new JsonArray();
        for (MainModel model: model_contents) {
            content_array.add(model.toJson());
        }

        obj.add("_contents", content_array);
        return obj;
    }

    void addContents(ArrayList<MainModel> files) {
        this.model_contents = files;
        for (MainModel model : files) {
            if (model.isNote()) {
                this.model_notes.add(model);
            }
        }
    }

    public void addContent(MainModel file) {
        if (file.isNote()) {
            this.model_notes.add(file);
        }
        this.model_contents.add(file);
    }

    public void removeContent(MainModel obj) {
        if (obj.isNote()) {
            this.model_notes.remove(obj);
        }

        this.model_contents.remove(obj);
    }

    public ArrayList<MainModel> getContent() { return this.model_contents; }
    public ArrayList<MainModel> getNotes() { return this.model_notes; }

    public MainModel getFirstFolder() {
        return this.model_contents.size() > 0 && this.model_contents.get(0) instanceof SingleNotebook ? this.model_contents.get(0) : null;
    }

    public int getContentCount() {
        return this.getContent().size();
    }

    public int getNotesCount() {
        return this.getNotes().size();
    }

    public void sortContents(Context context) {
        try {
            Collections.sort(model_contents, new NotebooksComparator(context));
        } catch (NullPointerException e) { }
    }

    public void sortContents() {
        try {
            if (_context_ref != null && _context_ref.get() != null && getContentCount() > 1) {
                Collections.sort(model_contents, new NotebooksComparator(_context_ref.get()));
            }
        } catch (NullPointerException e) { }
    }

    private void logContents() {
        if (getContent() != null) {
            for (MainModel m : getContent()) {
                Log.i("log", "-> " + m.getName());
            }
            Log.i("log", "-----------");
        }
    }

    public void unloadNotes(final OnModelLoadedListener mOnModelLoadedListener) {
        this._load_count = 0;

        if (this.isNote()) {
            SingleNote n = (SingleNote) this;
            n.unloadNote();
            if (mOnModelLoadedListener != null) mOnModelLoadedListener.ModelLoaded();
            return;
        }

        if (getContentCount() == 0) {
            if (mOnModelLoadedListener != null) mOnModelLoadedListener.ModelLoaded();
            return;
        }

        for (MainModel f: getContent()) {
            f.unloadNotes(new OnModelLoadedListener() {
                @Override
                public void ModelLoaded() {
                    _load_count++;

                    if (_load_count >= getContentCount()) {
                        if (mOnModelLoadedListener != null) mOnModelLoadedListener.ModelLoaded();
                    }
                }
            });
        }
    }

    public void loadNotes(Context context, OnModelLoadedListener model_loaded_listener) {
        reloadNotes(false, context, model_loaded_listener);
    }

    public void reloadNotes(boolean reload, Context context, final OnModelLoadedListener mOnModelLoadedListener) {
        this._load_count = 0;
        this._context_ref = new WeakReference<>(context);

        if (this.isNote() || getNotesCount() == 0) {
            if (mOnModelLoadedListener != null) mOnModelLoadedListener.ModelLoaded();
            return;
        }

        for (MainModel n: getNotes()) {
            SingleNote note = (SingleNote) n;
            if (!reload && note.wasLoaded()) {
                _load_count++;
                if (_load_count >= getNotesCount()) {
                    sortContents();
                    if (mOnModelLoadedListener != null) mOnModelLoadedListener.ModelLoaded();
                }
            } else {
                note.refreshContent(new SingleNote.OnNoteLoadedListener() {
                    @Override
                    public void NoteLoaded(SingleNote note) {
                    _load_count++;
                    if (_load_count >= getNotesCount()) {
                        sortContents();
                        if (mOnModelLoadedListener != null) mOnModelLoadedListener.ModelLoaded();
                    }
                    }
                });
            }
        }
    }

    public void clearContent() {
        this.model_contents.clear();
        this.model_notes.clear();
    }

    public void loadContent(Context context, final OnModelLoadedListener mOnModelLoadedListener) {
        this._load_count = 0;
        this._context_ref = new WeakReference<>(context);

        if (this.isNote()) {
            if (mOnModelLoadedListener != null) mOnModelLoadedListener.ModelLoaded();
            return;
        }

        LoadFoldersTask task = new LoadFoldersTask(new OnContentLoadedListener() {
            @Override
            public void ContentLoaded(ArrayList<MainModel> files) {
                addContents(files);
                if (files.size() == 0) {
                    if (mOnModelLoadedListener != null) mOnModelLoadedListener.ModelLoaded();
                } else {
                    for (MainModel model : files) {
                        model.loadContent(_context_ref.get(), new OnModelLoadedListener() {
                            @Override
                            public void ModelLoaded() {
                                _load_count++;
                                //Log.i("log", "load: "+_load_count+" / "+getContentCount());
                                if (_load_count >= getContentCount()) {
                                    sortContents();
                                    if (mOnModelLoadedListener != null) mOnModelLoadedListener.ModelLoaded();
                                }
                            }
                        });
                    }
                }
            }
        });
        task.execute(getPath());
    }

    private static class LoadFoldersTask extends AsyncTask<String, Void, ArrayList<MainModel>> {

        private OnContentLoadedListener mOnContentLoadedListener;

        LoadFoldersTask(OnContentLoadedListener content_loaded_listener) {
            this.mOnContentLoadedListener = content_loaded_listener;
        }

        @Override
        protected ArrayList<MainModel> doInBackground(String... paths) {
            ArrayList<MainModel> folders = Path.getFoldersAndNotes(paths[0]);
            if (folders == null) return new ArrayList<>();
            return folders;
        }

        @Override
        protected void onPostExecute(ArrayList<MainModel> result) {
            mOnContentLoadedListener.ContentLoaded(result);
        }
    }

    public interface OnContentLoadedListener {
        void ContentLoaded(ArrayList<MainModel> files);
    }

    public interface OnModelLoadedListener {
        void ModelLoaded();
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
                        return new MainModel(headerBuckets);
                }
            }

            public MainModel[] newArray(int size) {
                return new MainModel[size];
            }
        };

    public class MyViewHolder extends FlexibleViewHolder implements AnimatedViewHolder {

        TextView mNotebookTitle;
        TextView mNoteCount;
        TextView mNoteSummary;
        ImageView mNoteIcon;

        public MyViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter);
            this.mNotebookTitle = view.findViewById(R.id.notebook_title);
            this.mNoteCount = view.findViewById(R.id.note_count);
            this.mNoteSummary = view.findViewById(R.id.notebook_summary);
            this.mNoteIcon = view.findViewById(R.id.notebook_icon);
        }

        @Override
        public void onClick(View view) {
            Log.i("click","clicked view");
            super.onClick(view);
        }

        @Override
        public String toString() {
            return "MainModel[" + super.toString() + "]";
        }

        @Override
        public void scrollAnimators(@NonNull List<Animator> animators, int position, boolean isForward) {
            AnimatorHelper.alphaAnimator(animators, itemView, 0f);
        }

        @Override
        public boolean preAnimateAddImpl() {
            return false;
        }

        @Override
        public boolean preAnimateRemoveImpl() {
            return false;
        }

        @Override
        public boolean animateAddImpl(ViewPropertyAnimatorListener listener, long addDuration, int index) {
            return false;
        }

        @Override
        public boolean animateRemoveImpl(ViewPropertyAnimatorListener listener, long removeDuration, int index) {
            return false;
        }
    }
}
