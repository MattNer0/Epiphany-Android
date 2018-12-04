package com.neromatt.epiphany.model.DataObjects;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.GlideApp;
import com.neromatt.epiphany.helper.Database;
import com.neromatt.epiphany.model.Adapters.MainAdapter;
import com.neromatt.epiphany.model.Path;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.Nullable;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;

public class SingleNote extends MainModel {

    private boolean noteLoaded;
    private boolean noteModified;

    private String path;
    private String filename;
    private String summary;
    private String title;
    private String body;
    private Date updatedAt;
    private Date createdAt;
    private Bundle metadata;

    private String photo;
    private String oldFilename;
    private boolean newFilename;

    private boolean matchQuery = true;

    private int image_width = 0;
    private int image_height = 0;

    public SingleNote(String path, String filename) {
        super();

        this._model_type = MainModel.TYPE_MARKDOWN_NOTE;

        this.path = path;
        this.filename = filename;
        this.metadata = new Bundle();
        this.noteLoaded = false;
        this.noteModified = false;
        this.newFilename = false;
        this.oldFilename = null;
    }

    public SingleNote(Bundle args) {
        super(args);
        updateObj(args);
    }

    @Override
    public boolean shouldNotifyChange(IFlexible newItem) {
        SingleNote singleNote = (SingleNote) newItem;
        return noteLoaded != singleNote.wasLoaded() || !equals(singleNote);
    }

    @Override
    public boolean equals(Object inObject) {
        if (inObject instanceof SingleNote) {
            SingleNote inItem = (SingleNote) inObject;

            return this.hashCode() == inItem.hashCode() && this.getLastModifiedDate().equals(inItem.getLastModifiedDate());
        }
        return false;
    }

    @Override
    public void bindViewHolder(FlexibleAdapter<IFlexible> adapter, final MyViewHolder holder, int position, List<Object> payloads) {
        //viewHolder = new WeakReference<>(holder);
        holder.mNotebookTitle.setText(getName());
        if (summary == null || summary.isEmpty()) {
            holder.mNoteSummary.setVisibility(View.GONE);
        } else {
            holder.mNoteSummary.setVisibility(View.VISIBLE);
            holder.mNoteSummary.setText(summary);
        }

        if (photo != null && !photo.isEmpty()) {
            holder.mNotePhoto.setVisibility(View.VISIBLE);
            holder.mNoteSummary.setMaxLines(2);

            if (image_width > 0 && image_height > 0) {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.mNotePhoto.getLayoutParams();
                params.width = image_width;
                params.height = image_height;
                holder.mNotePhoto.setLayoutParams(params);
            } else {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.mNotePhoto.getLayoutParams();
                params.width = LinearLayout.LayoutParams.MATCH_PARENT;
                params.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                holder.mNotePhoto.setLayoutParams(params);
            }

            GlideApp.with(holder.itemView.getContext())
                    .asBitmap()
                    .load(photo)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .listener(new RequestListener<Bitmap>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                            if (holder.mNotePhoto != null && image_width == 0 && image_height == 0) {
                                holder.mNotePhoto.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        image_width = holder.mNotePhoto.getMeasuredWidth();
                                        image_height = holder.mNotePhoto.getMeasuredHeight();
                                    }
                                });
                            }
                            return false;
                        }
                    })
                    .into(holder.mNotePhoto);

        } else {
            holder.mNotePhoto.setVisibility(View.GONE);
            holder.mNoteSummary.setMaxLines(5);
        }

        if (wasLoaded() || updatedAt != null) {
            holder.mNoteTime.setVisibility(View.VISIBLE);
            holder.mNoteTime.setText(getLastModifiedDateString(Locale.US));
        } else {
            holder.mNoteTime.setVisibility(View.GONE);
        }

        if (matchQuery) {
            holder.itemView.setAlpha(1.0f);
        } else {
            holder.itemView.setAlpha(0.7f);
        }

        if (adapter instanceof MainAdapter) {
            MainAdapter ma = (MainAdapter) adapter;
            if (ma.getSpanCount() == 1) {
                holder.mNoteIcon.setVisibility(View.VISIBLE);
            } else {
                holder.mNoteIcon.setVisibility(View.GONE);
            }

            if (ma.getSpanCount() == 0) {
                holder.mNoteTime.setVisibility(View.GONE);
            }

            /*if (ma.isMovingNote() && ma.getMovingNote().getUuid().equals(getUuid())) {

            }*/
        }
    }

    public void refreshContent(Database db, OnNoteLoadedListener noteLoadedCallback) {
        this.title="";
        this.summary="";
        this.updatedAt = new Date();
        this.createdAt = new Date();
        this.metadata = new Bundle();

        LoadNoteTask task = new LoadNoteTask(this, db, true, noteLoadedCallback);
        task.execute(getFullPath());
    }

    public void refreshFromDB(Database db) {
        this.title="";
        this.summary="";
        this.updatedAt = new Date();
        this.createdAt = new Date();
        this.metadata = new Bundle();

        Bundle res = db.getNoteByPath(getFullPath());
        updateObjAfterReadingContent(this, res, false);
    }

    public void refreshFromFile(Database db) {
        this.title="";
        this.summary="";
        this.updatedAt = new Date();
        this.createdAt = new Date();
        this.metadata = new Bundle();

        Bundle res = readNoteFile(getFullPath());
        if (db != null) db.saveNote(getFullPath(), res);
        updateObjAfterReadingContent(this, res, true);
    }

    public boolean searchNoteBody(String query) {
        return body.contains(query);
    }

    public void addMetadata(String key, String value) {
        if (metadata == null) metadata = new Bundle();
        metadata.putString(key, value);
    }

    public String getMetaString(String key) {
        if (metadata == null) return null;
        return metadata.getString(key, null);
    }

    public String getImageFolderPath() {
        return getPath()+"/."+getFileNameNoExtension();
    }

    private static String getImageFolderPath(File file) {
        String filename = file.getName();
        filename = filename.substring(0, filename.lastIndexOf('.'));
        return file.getParentFile().getPath()+"/."+filename;
    }

    private void logNote() {
        Log.i("note", "\n----------------------------");
        Log.i("note", "title:     "+this.title);
        Log.i("note", "summary:   "+this.summary);
        Log.i("note", "updatedAt: "+this.updatedAt);
        Log.i("note", "createdAt: "+this.createdAt);
        Log.i("note", "body:\n"+this.body);
    }

    public String getBody() {
        return this.body;
    }

    public ArrayList<String> getRemoteImages() {
        ArrayList<String> list = new ArrayList<>();
        String regex = "!\\[([^]]*?)]\\((https?://.*?)\\)";
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(body);

        ArrayList<String> formats = new ArrayList<>(Arrays.asList("png", "jpg", "jpeg", "gif", "bmp"));

        while (matcher.find()) {
            String url = matcher.group(2).trim();
            String extension = FilenameUtils.getExtension(url).toLowerCase();
            if (formats.contains(extension)) {
                list.add(url);
            }
        }

        return list;
    }

    private static Bundle readNoteFile(String full_path) {
        File file = new File(full_path);
        StringBuilder text = new StringBuilder();
        StringBuilder summary = new StringBuilder();
        Bundle data = new Bundle();
        Bundle metadata = new Bundle();

        boolean metadata_start = false;
        boolean metadata_end = false;
        boolean title_found = false;

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                if (!metadata_end && line.matches("^[+-]{3,}$")) {
                    if (metadata_start) metadata_end = true;
                    metadata_start = true;
                } else if (metadata_start && !metadata_end) {
                    if (line.isEmpty()) continue;
                    String regex = "^([a-z]+)\\s?[:=]\\s+['\"]?([\\w\\W\\s]+?)['\"]?\\s*$";
                    Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(line);

                    if (matcher.find()) {
                        String metaKey = matcher.group(1);
                        String metaValue = matcher.group(2).trim();
                        metadata.putString(metaKey, metaValue);
                    }

                } else {
                    if (line.startsWith("#") && !title_found) {
                        String title = line.replaceFirst("^#+\\s*", "");
                        if (!title.isEmpty()) {
                            data.putString(Constants.KEY_NOTE_TITLE, title);
                            title_found = true;
                        }
                    } else if (title_found && summary.length() < 220 && !line.isEmpty()) {
                        String summary_line = line.replaceAll("[*_#]+", "").replaceAll("\n\\s+", "\n").trim();
                        if (!summary_line.isEmpty()) {
                            summary.append(summary_line);
                            summary.append("\n");
                        }
                    }
                    text.append(line);
                    text.append("\n");
                }
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        String body = text.toString().trim();
        data.putBundle(Constants.KEY_NOTE_METADATA, metadata);
        data.putString(Constants.KEY_NOTE_BODY, body);
        data.putString(Constants.KEY_NOTE_SUMMARY, summary.toString().trim());

        String regex_photo = "(https?|epiphany):\\/\\/[-a-zA-Z0-9@:%_+.~#?&//=]+?\\.(png|jpeg|jpg|gif)";
        Pattern pattern_photo = Pattern.compile(regex_photo, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher matcher_photo = pattern_photo.matcher(body);

        if (matcher_photo.find()) {
            String photo_url = matcher_photo.group(0);

            Uri uri = Uri.parse(photo_url);
            String scheme = uri.getScheme();

            if (scheme != null && scheme.equalsIgnoreCase("epiphany")) {
                photo_url = photo_url.replace("epiphany://", "file:///" + getImageFolderPath(file) + "/");
            }

            data.putString(Constants.KEY_NOTE_PHOTO, photo_url);
        }

        if (!title_found) {
            if (text.length() > 220) {
                data.putString(Constants.KEY_NOTE_SUMMARY, text.substring(0, 220));
            } else {
                data.putString(Constants.KEY_NOTE_SUMMARY, text.toString());
            }
        }
        return data;
    }

    private static Bundle parseTitle(String note_body) {
        String[] lines = note_body.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.isEmpty() && line.startsWith("#")) {
                Bundle ret = new Bundle();
                ret.putString(Constants.KEY_NOTE_TITLE, line.replaceAll("^#+\\s*", ""));
                List<String> one = Arrays.asList(lines).subList(0, i);
                List<String> two = Arrays.asList(lines).subList(i+1, lines.length);
                ret.putString(Constants.KEY_NOTE_BODY, TextUtils.join("\n", one)+"\n"+TextUtils.join("\n", two));
                return ret;
            }
        }

        return new Bundle();
    }

    private static String parseSummary(String body) {
        String clean_body = body.replaceAll("[*_#]+", "").replaceAll("\n\\s+", "\n").trim();
        String[] lines = clean_body.split("\\n");
        StringBuilder summary = new StringBuilder();
        for(String line: lines) {
            summary.append(line);
            if (summary.length() > 220) {
                break;
            }
            if (!line.isEmpty()) summary.append("\n");
        }

        return summary.toString().trim();
    }

    @Override
    public String getName() {
        if (this.title == null || this.title.isEmpty()) {
            return getFileNameNoExtension();
        }
        return this.title;
    }

    public boolean doesExist() {
        return new File(getFullPath()).exists();
    }

    public void updateBody(String new_body) {
        updateBody(new_body, true);
    }

    public void updateBody(String new_body, boolean mark_as_modified) {
        body = new_body;
        if (mark_as_modified) {
            noteModified = true;
        }

        if (new_body.isEmpty()) {
            summary = "";
            title = "New Note";
            updateFilename(title);
            return;
        }

        Bundle title_parse = parseTitle(new_body);
        String str_title = title_parse.getString(Constants.KEY_NOTE_TITLE, "");

        summary = parseSummary(title_parse.getString(Constants.KEY_NOTE_BODY, ""));

        if (title == null || !title.equals(str_title)) {
            title = str_title;
            if (title.isEmpty()) title = "New Note";
            updateFilename(title);
        }
    }

    private void updateFilename(String title) {
        String old_extension = getExtension();
        if (oldFilename == null) oldFilename = filename;
        filename = title.replaceAll("[^\\w _-]", "").replaceAll("\\s+", " ");
        filename = filename.substring(0, Math.min(filename.length(), 40)).trim() + "." + old_extension;

        if (filename.equals(oldFilename)) {
            oldFilename = null;
        } else {
            newFilename = true;
        }
    }

    public boolean wasModified() {
        return this.noteModified;
    }
    public boolean wasLoaded() { return this.noteLoaded; }

    public void markAsNewFile() {
        newFilename = true;
        oldFilename = null;

        updatedAt = new Date();
        createdAt = new Date();

        if (metadata != null) {
            metadata.putString(Constants.METATAG_UPDATED, getLastModifiedString(Locale.US));
            metadata.putString(Constants.METATAG_CREATED, getCreatedString(Locale.US));
        }
    }

    public boolean isNewFile() {
        return this.newFilename;
    }

    @Override
    public boolean delete() {
        if (oldFilename != null && !oldFilename.isEmpty()) {
            File old_file = new File(getPath()+"/"+oldFilename);
            if (!old_file.exists()) {
                oldFilename = null;
            } if (old_file.delete()) {
                oldFilename = null;
            }
        }

        File file = new File(getFullPath());
        if (file.exists()) return file.delete();

        return false;
    }

    public boolean moveFile(String outputPath) {
        InputStream in;
        OutputStream out;

        if (getFullPath().equals(outputPath + "/" + getFilename())) return false;

        try {
            File dir = new File (outputPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            in = new FileInputStream(getFullPath());
            out = new FileOutputStream(outputPath + "/" + getFilename());

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;

            out.flush();
            out.close();
            out = null;

            new File(getFullPath()).delete();

            this.path = outputPath;
            return true;

        } catch (Exception e) {
            Log.e(Constants.LOG, e.getMessage());
            return false;
        }
    }

    public void saveNote(OnNoteSavedListener mOnNoteSavedListener) {
        if (newFilename) {
            if (doesExist()) {
                filename = Path.newNoteNameFromCurrent(getPath(), getFileNameNoExtension(), getExtension());
                Log.i(Constants.LOG, "new filename: "+filename);
            }
            newFilename = false;
        }

        this.noteModified = false;
        if (oldFilename != null && !oldFilename.isEmpty()) {
            File old_file = new File(getPath()+"/"+oldFilename);
            if (old_file.exists()) old_file.delete();
            oldFilename = null;
        }

        StringBuilder text = new StringBuilder();

        if (this.createdAt == null) this.createdAt = new Date();

        if (!this.metadata.containsKey(Constants.METATAG_CREATED)) {
            this.metadata.putString(Constants.METATAG_CREATED, getCreatedString(Locale.US));
        }

        this.updatedAt = new Date();
        this.metadata.putString(Constants.METATAG_UPDATED, getLastModifiedString(Locale.US));

        text.append("+++\n");
        for (String key: this.metadata.keySet()) {
            if (key.equals(Constants.METATAG_UPDATED)) {
                text.append(Constants.METATAG_UPDATED);
                text.append(" = \"");
                text.append(formatDate(this.updatedAt));
                text.append("\"\n");
            } else if (key.equals(Constants.METATAG_CREATED)) {
                text.append(Constants.METATAG_CREATED);
                text.append(" = \"");
                text.append(formatDate(this.createdAt));
                text.append("\"\n");
            } else {
                text.append(key);
                text.append(" = \"");
                text.append(this.metadata.getString(key));
                text.append("\"\n");
            }
        }

        text.append("+++\n\n");
        text.append(this.body);

        try {
            File file = new File(getFullPath());
            FileOutputStream stream = new FileOutputStream(file);
            try {
                stream.write(text.toString().getBytes());
            } finally {
                stream.close();
                if (mOnNoteSavedListener != null) mOnNoteSavedListener.NoteSaved(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (mOnNoteSavedListener != null) mOnNoteSavedListener.NoteSaved(false);
        }
    }

    @Override
    public String getPath() {
        return path;
    }

    public String getSummary() {
        return summary;
    }

    public String getLastModifiedString(Locale loc) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", loc).format(updatedAt);
    }
    public String getLastModifiedDateString(Locale loc) {
        return new SimpleDateFormat("dd/MM/yyyy", loc).format(updatedAt);
    }
    public String getCreatedString(Locale loc) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", loc).format(createdAt);
    }

    public Date getLastModifiedDate() {
        if (updatedAt == null) return new Date();
        return updatedAt;
    }
    public Date getCreatedDate() {
        return createdAt;
    }

    public int getLinesNumber() {
        String[] lines = this.body.split("\n");
        return lines.length;
    }

    public static Date parseDate(String date_string) throws ParseException {
        if (date_string == null || date_string.isEmpty()) {
            return new Date();
        }
        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        return parser.parse(date_string);
    }

    private static String formatDate(Date date_object) {
        if (date_object == null) date_object = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        return formatter.format(date_object);
    }

    public int compareModifiedDateTo(SingleNote n1) {
        return -1 * getLastModifiedDate().compareTo(n1.getLastModifiedDate());
    }

    public int compareCreatedDateTo(SingleNote n1) {
        return -1 * getCreatedDate().compareTo(n1.getCreatedDate());
    }

    public String getExtension() {
        return this.filename.substring(this.filename.lastIndexOf('.') + 1);
    }
    public String getFileNameNoExtension() {
        return this.filename.substring(0, this.filename.lastIndexOf('.'));
    }
    public String getFilename() {
        return this.filename;
    }

    public String getFullPath() {
        return getPath()+"/"+filename;
    }

    public String getMarkdown() {
        return this.body;
    }

    @Override
    public Bundle toBundle() {
        Bundle args = super.toBundle();
        args.putString(Constants.KEY_NOTE_PATH, path);
        args.putString(Constants.KEY_NOTE_FILENAME, filename);
        args.putString(Constants.KEY_NOTE_SUMMARY, summary);
        args.putString(Constants.KEY_NOTE_TITLE, title);
        args.putString(Constants.KEY_NOTE_PHOTO, photo);
        args.putString(Constants.KEY_NOTE_BODY, body);
        args.putBoolean(Constants.KEY_NOTE_SEARCH_RESULT, matchQuery);

        if (updatedAt == null) updatedAt = new Date();
        if (createdAt == null) createdAt = new Date();

        args.putLong(Constants.METATAG_UPDATED, updatedAt.getTime());
        args.putLong(Constants.METATAG_CREATED, createdAt.getTime());
        args.putBundle(Constants.KEY_NOTE_METADATA, metadata);
        args.putBoolean("noteLoaded", noteLoaded);
        return args;
    }

    public Bundle toLightBundle() {
        Bundle args = super.toBundle();
        args.putString(Constants.KEY_NOTE_PATH, path);
        args.putString(Constants.KEY_NOTE_FILENAME, filename);
        args.putBoolean("noteLoaded", false);

        if (noteLoaded) {
            args.putLong(Constants.METATAG_UPDATED, updatedAt.getTime());
            args.putLong(Constants.METATAG_CREATED, createdAt.getTime());
        }

        return args;
    }

    public void updateObj(Bundle args) {
        this.noteLoaded = args.getBoolean("noteLoaded");
        this.noteModified = false;
        this.newFilename = false;
        this.oldFilename = null;

        this._model_type = MainModel.TYPE_MARKDOWN_NOTE;
        this.path = args.getString(Constants.KEY_NOTE_PATH, "");
        this.filename = args.getString(Constants.KEY_NOTE_FILENAME, "");

        if (args.containsKey(Constants.KEY_NOTE_PHOTO)) {
            this.photo = args.getString(Constants.KEY_NOTE_PHOTO, "");
        } else {
            this.photo = null;
        }

        this.matchQuery = args.getBoolean(Constants.KEY_NOTE_SEARCH_RESULT, true);

        if (args.containsKey(Constants.METATAG_UPDATED)) {
            this.updatedAt = new Date(args.getLong(Constants.METATAG_UPDATED, 0));
        }

        if (args.containsKey(Constants.METATAG_CREATED)) {
            this.createdAt = new Date(args.getLong(Constants.METATAG_CREATED, 0));
        }

        if (this.noteLoaded) {
            this.summary = args.getString(Constants.KEY_NOTE_SUMMARY, "");
            this.title = args.getString(Constants.KEY_NOTE_TITLE, "");
            this.body = args.getString(Constants.KEY_NOTE_BODY, "");
            this.metadata = args.getBundle(Constants.KEY_NOTE_METADATA);
            if (this.metadata == null) this.metadata = new Bundle();
        } else {
            this.metadata = new Bundle();
        }
    }

    public void setNotMatched() {
        this.matchQuery = false;
    }

    public void setMatched() {
        this.matchQuery = true;
    }

    public boolean isMatched() {
        return this.matchQuery;
    }

    public int compareMatchedTo(SingleNote note) {
        if (this.matchQuery && !note.isMatched()) return -1;
        if (!this.matchQuery && note.isMatched()) return 1;

        return 0;
    }

    public void updatePath(String new_path, String file_name) {
        if (new_path != null && !new_path.isEmpty() && file_name != null && !file_name.isEmpty()) {
            this.path = new_path;
            this.filename = file_name;
        }
    }

    private static class LoadNoteTask extends AsyncTask<String, Void, Bundle> {

        SingleNote note;
        OnNoteLoadedListener noteLoadedCallback;
        Database db;
        boolean load_from_file;

        LoadNoteTask(SingleNote thisNote, Database db, boolean load_from_file, OnNoteLoadedListener noteLoadedCallback) {
            this.note = thisNote;
            this.db = db;
            this.load_from_file = load_from_file;
            this.noteLoadedCallback = noteLoadedCallback;
        }

        @Override
        protected Bundle doInBackground(String... paths) {
            Bundle res;
            if (db == null || load_from_file) {
                res = readNoteFile(paths[0]);

                if (db != null) db.saveNote(note.getFullPath(), res);

            } else {
                res = db.getNoteByPath(note.getFullPath());
            }
            return res;
        }

        @Override
        protected void onPostExecute(Bundle result) {
            updateObjAfterReadingContent(note, result, load_from_file);
            if (noteLoadedCallback != null) {
                noteLoadedCallback.NoteLoaded(note);
            }
        }
    }

    public static void updateObjAfterReadingContent(SingleNote note, Bundle result, boolean loaded_from_file) {
        if (note == null || result == null) return;

        note.body = result.getString(Constants.KEY_NOTE_BODY, "");
        note.title = result.getString(Constants.KEY_NOTE_TITLE, "");
        note.summary = result.getString(Constants.KEY_NOTE_SUMMARY, "");

        if (result.containsKey(Constants.KEY_NOTE_PHOTO)) {
            note.photo = result.getString(Constants.KEY_NOTE_PHOTO, "");
        }

        if (result.containsKey(Constants.KEY_NOTE_METADATA)) {
            note.metadata = result.getBundle(Constants.KEY_NOTE_METADATA);

            try {
                if (note.metadata.containsKey(Constants.METATAG_UPDATED)) {
                    note.updatedAt = parseDate(note.metadata.getString(Constants.METATAG_UPDATED, ""));
                }
                if (note.metadata.containsKey(Constants.METATAG_CREATED)) {
                    note.createdAt = parseDate(note.metadata.getString(Constants.METATAG_CREATED, ""));
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            note.updatedAt = new Date(result.getLong(Constants.METATAG_UPDATED));
            note.createdAt = new Date(result.getLong(Constants.METATAG_CREATED));
        }

        if (loaded_from_file) note.noteLoaded = true;
    }

    @Override
    public String toString() {
        return "Note[" + getPath()+"/"+getFilename() + "][ "+getLastModifiedString(Locale.US)+" ]";
    }

    public interface OnNoteLoadedListener {
        void NoteLoaded(SingleNote note);
    }

    public interface OnNoteSavedListener {
        void NoteSaved(boolean saved);
    }
}
