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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.helper.DBInterface;
import com.neromatt.epiphany.helper.Database;
import com.neromatt.epiphany.model.Adapters.SimpleHeader;
import com.neromatt.epiphany.model.NotebooksComparator;
import com.neromatt.epiphany.model.Path;
import com.neromatt.epiphany.ui.MainActivity;
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
import eu.davidea.flexibleadapter.Payload;
import eu.davidea.flexibleadapter.helpers.AnimatorHelper;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.flexibleadapter.items.IFlexible;
import eu.davidea.viewholders.AnimatedViewHolder;
import eu.davidea.viewholders.FlexibleViewHolder;

public class MainModel extends AbstractFlexibleItem<MainModel.MyViewHolder> implements Parcelable {

    private static final int TYPE_NULL = 0;
    private static final int TYPE_PROGRESS_LOADING = -1;
    public static final int TYPE_RACK = 1;
    public static final int TYPE_FOLDER = 2;
    public static final int TYPE_MARKDOWN_NOTE = 3;

    private LoadingStatusEnum _loading_status = LoadingStatusEnum.MORE_TO_LOAD;

    private UUID _uuid;
    private boolean _loaded_content;
    int _model_type;

    private ArrayList<MainModel> _model_contents;
    private ArrayList<MainModel> _model_notes;
    private WeakReference<Context> _context_ref;
    private int _load_count;

    private MainModel _bucket;
    private MainModel _parent;

    public MainModel() {
        super();
        this._model_type = TYPE_NULL;
        this._model_notes = new ArrayList<>();
        this._model_contents = new ArrayList<>();
        this._loaded_content = false;
        this._uuid = UUID.randomUUID();
    }

    public MainModel(Bundle args) {
        super();
        _model_type = TYPE_NULL;
        _model_notes = new ArrayList<>();
        _model_contents = args.getParcelableArrayList("_contents");
        String uuid = args.getString("_uuid", "");
        if (uuid.isEmpty()) {
            this._uuid = UUID.randomUUID();
        } else {
            this._uuid = UUID.fromString(uuid);
        }
        if (_model_contents == null) {
            _model_contents = new ArrayList<>();
            _loaded_content = false;
        } else {
            _loaded_content = true;
            for (MainModel model : _model_contents) {
                if (model.isNote()) {
                    _model_notes.add(model);
                }
            }
        }
    }

    public MainModel(JsonObject args) {
        super();
        _model_type = TYPE_NULL;
        _model_notes = new ArrayList<>();
        _model_contents = new ArrayList<>();
        _loaded_content = false;

        String uuid = args.get("_uuid").getAsString();
        if (uuid.isEmpty()) {
            _uuid = UUID.randomUUID();
        } else {
            _uuid = UUID.fromString(uuid);
        }

        if (args.has("_contents")) {
            JsonArray json_contents = args.getAsJsonArray("_contents");
            _loaded_content = true;
            for (JsonElement element : json_contents) {
                int type = element.getAsJsonObject().get("modelType").getAsInt();
                switch (type) {
                    case TYPE_RACK:
                        _model_contents.add(new SingleRack(element.getAsJsonObject()));
                        break;
                    case TYPE_FOLDER:
                        _model_contents.add(new SingleNotebook(element.getAsJsonObject()));
                        break;
                    case TYPE_MARKDOWN_NOTE:
                        SingleNote n = new SingleNote(element.getAsJsonObject());
                        _model_contents.add(n);
                        _model_notes.add(n);
                        break;
                }
            }
        }
    }

    public MainModel(int modelType) {
        super();

        _model_type = modelType;
        _model_notes = new ArrayList<>();
        _model_contents = new ArrayList<>();
        _loaded_content = false;
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

    public void setBucket(MainModel model) {
        this._bucket = model;
    }

    public void setParent(MainModel model) {
        this._parent = model;
    }

    public MainModel getParent() {
        return this._parent;
    }

    public int getType() {
        return _model_type;
    }

    public boolean isQuickNotes() { return false; }

    public void setAsQuickNotes() { }

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
                Log.w("adapter", "invalid view type: "+_model_type);
                return R.layout.row_folder;
        }
    }

    @Override
    public MyViewHolder createViewHolder(View view, FlexibleAdapter<IFlexible> adapter) {
        return new MyViewHolder(view, adapter);
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
        args.putString("_uuid", this._uuid.toString());

        if (this._loaded_content) {
            args.putParcelableArrayList("_contents", _model_contents);
        }

        return args;
    }

    public LoadingStatusEnum getStatus() {
        return _loading_status;
    }

    public void setStatus(LoadingStatusEnum status) {
        this._loading_status = status;
    }

    public boolean delete() {
        return false;
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("modelType", this._model_type);
        obj.addProperty("_uuid", this._uuid.toString());

        if (this._loaded_content) {
            JsonArray content_array = new JsonArray();
            for (MainModel model : _model_contents) {
                content_array.add(model.toJson());
            }

            obj.add("_contents", content_array);
        }
        return obj;
    }

    public void addContents(ArrayList<MainModel> files) {
        _loaded_content = true;
        _model_contents = files;
        for (MainModel model : files) {
            if (model.isNote()) {
                _model_notes.add(model);
            }
        }
    }

    public void addContent(MainModel file) {

        file.setParent(this);
        file.setBucket(_bucket);

        if (file.isNote()) {
            _model_notes.add(file);
        }
        _model_contents.add(file);
    }

    public void removeContent(MainModel obj) {
        if (obj.isNote()) {
            _model_notes.remove(obj);
        }

        _model_contents.remove(obj);
    }

    public ArrayList<MainModel> getContent() { return _model_contents; }
    public ArrayList<MainModel> getNotes() { return _model_notes; }

    /*public ArrayList<MainModel> getFolders() {
        ArrayList<MainModel> res = new ArrayList<>();
        for (MainModel m: _model_contents) {
            if (m.isFolder()) {
                res.add(m);
            }
        }
        return res;
    }*/

    public ArrayList<MainModel> getAllNotes() {
        ArrayList<MainModel> res = new ArrayList<>();
        for (MainModel m: _model_contents) {
            if (m.isFolder()) {
                res.addAll(m.getAllNotes());
            } else if (m.isNote()) {
                res.add(m);
            }
        }
        return res;
    }

    public void initParents() {
        for (MainModel m: _model_contents) {
            m.setParent(this);
            if (isBucket()) {
                m.setBucket(this);
                if (isQuickNotes()) {
                    m.setAsQuickNotes();
                }
                m.initParents(this);
            } else {
                m.setBucket(_bucket);
                m.initParents(_bucket);
            }
        }
    }

    public void initParents(MainModel bucket) {
        for (MainModel m: _model_contents) {
            m.setParent(this);
            m.setBucket(bucket);
            m.initParents(bucket);
        }
    }

    public UUID getUUID() {
        return this._uuid;
    }

    public boolean equalsUUID(String uuid) {
        return _uuid.equals(UUID.fromString(uuid));
    }

    public MainModel getFirstFolder() {
        return _model_contents.size() > 0 && _model_contents.get(0) instanceof SingleNotebook ? _model_contents.get(0) : null;
    }

    public int getContentCount() {
        return this.getContent().size();
    }

    public int getNotesCount() {
        return this.getNotes().size();
    }

    public boolean isLoadedContent() {
        return _loaded_content;
    }

    public void sortContents(Context context) {
        try {
            Collections.sort(_model_contents, new NotebooksComparator(context));
        } catch (NullPointerException e) { }
    }

    public void sortContents() {
        try {
            if (_context_ref != null && _context_ref.get() != null && getContentCount() > 1) {
                Collections.sort(_model_contents, new NotebooksComparator(_context_ref.get()));
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

    public void dbLoadNotes(Database db, Context context, final OnModelLoadedListener modelLoadedCallback) {
        this._load_count = 0;
        this._context_ref = new WeakReference<>(context);

        if (this.isNote() || getNotesCount() == 0) {
            if (modelLoadedCallback != null) modelLoadedCallback.ModelLoaded();
            return;
        }

        for (MainModel n: getNotes()) {
            SingleNote note = (SingleNote) n;
            if (note.wasLoaded()) {
                _load_count++;
                if (_load_count >= getNotesCount()) {
                    sortContents();
                    if (modelLoadedCallback != null) modelLoadedCallback.ModelLoaded();
                }
            } else {
                note.refreshFromDB(db, new SingleNote.OnNoteLoadedListener() {
                    @Override
                    public void NoteLoaded(SingleNote note) {
                        _load_count++;
                        if (_load_count >= getNotesCount()) {
                            sortContents();
                            if (modelLoadedCallback != null) modelLoadedCallback.ModelLoaded();
                        }
                    }
                });
            }
        }
    }

    public void loadNotes(MainActivity ma, OnModelLoadedListener modelLoadedCallback) {
        reloadNotes(false, ma, modelLoadedCallback);
    }

    public void reloadNotes(boolean reload, DBInterface ma, final OnModelLoadedListener modelLoadedCallback) {
        this._load_count = 0;
        this._context_ref = new WeakReference<>(ma.getContext());

        if (this.isNote() || getNotesCount() == 0) {
            if (modelLoadedCallback != null) modelLoadedCallback.ModelLoaded();
            return;
        }

        for (MainModel n: getNotes()) {
            SingleNote note = (SingleNote) n;
            if (!reload && note.wasLoaded()) {
                _load_count++;
                if (_load_count >= getNotesCount()) {
                    sortContents();
                    if (modelLoadedCallback != null) modelLoadedCallback.ModelLoaded();
                }
            } else {
                note.refreshContent(ma.getDatabase(), new SingleNote.OnNoteLoadedListener() {
                    @Override
                    public void NoteLoaded(SingleNote note) {
                    _load_count++;
                    if (_load_count >= getNotesCount()) {
                        sortContents();
                        if (modelLoadedCallback != null) modelLoadedCallback.ModelLoaded();
                    }
                    }
                });
            }
        }
    }

    public void searchNotes(String query, final OnSearchFolderListener folderCallback) {
        final ArrayList<MainModel> mResults = new ArrayList<>();

        if (getContentCount() == 0) {
            folderCallback.SearchMatch(mResults);
            return;
        }

        this._load_count = 0;
        for (MainModel n: getContent()) {
            if (n.isFolder()) {
                mResults.add(n);
                _load_count++;
                if (_load_count >= getContentCount()) {
                    folderCallback.SearchMatch(mResults);
                }
                /*n.searchNotes(query, new OnSearchFolderListener() {
                    @Override
                    public void SearchMatch(ArrayList<MainModel> results) {
                        if (results.size() > 0) {

                        }
                        _load_count++;
                        if (_load_count >= getContentCount()) {
                            folderCallback.SearchMatch(mResults);
                        }
                    }
                });*/
            } else if (n.isNote()) {
                SingleNote note = (SingleNote) n;
                note.searchNote(query, new SingleNote.OnNoteSearchedListener() {
                    @Override
                    public void NoteSearched(SingleNote note, boolean match) {
                        if (match) {
                            mResults.add(note);
                        }

                        _load_count++;
                        if (_load_count >= getContentCount()) {
                            folderCallback.SearchMatch(mResults);
                        }
                    }
                });
            } else {
                Log.e(Constants.LOG, "something else?");
            }
        }
    }

    public void clearContent() {
        _model_contents.clear();
        _model_notes.clear();
        _loaded_content = false;
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

    /*public interface OnSearchNoteListener {
        void SearchMatch(MainModel model);
    }*/

    public interface OnSearchFolderListener {
        void SearchMatch(ArrayList<MainModel> results);
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
        ImageView mNoteIcon;

        TextView mNoteCount;
        LinearLayout mNoteCountContainer;

        TextView mFolderCount;
        LinearLayout mFolderCountContainer;

        ProgressBar progressBar;
        TextView progressMessage;

        MyViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter);
            this.mNotebookTitle = view.findViewById(R.id.notebook_title);

            this.mNoteCount = view.findViewById(R.id.note_count);
            this.mNoteCountContainer = view.findViewById(R.id.note_count_container);

            this.mFolderCount = view.findViewById(R.id.folder_count);
            this.mFolderCountContainer = view.findViewById(R.id.folder_count_container);

            this.mNoteSummary = view.findViewById(R.id.notebook_summary);
            this.mNoteIcon = view.findViewById(R.id.notebook_icon);

            this.progressBar = view.findViewById(R.id.progress_bar);
            this.progressMessage = view.findViewById(R.id.progress_message);
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
    }

    private enum LoadingStatusEnum {
        MORE_TO_LOAD,
        DISABLE_ENDLESS,
        NO_MORE_LOAD,
        ON_CANCEL,
        ON_ERROR
    }
}
