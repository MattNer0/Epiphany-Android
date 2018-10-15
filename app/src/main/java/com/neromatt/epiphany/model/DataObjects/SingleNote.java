package com.neromatt.epiphany.model.DataObjects;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.neromatt.epiphany.model.Adapters.MainAdapter;
import com.neromatt.epiphany.model.Path;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private String oldFilename;
    private boolean newFilename;

    private OnNoteLoadedListener mOnNoteLoadedListener;

    private WeakReference<MyViewHolder> viewHolder;

    public SingleNote(String path, String filename) {
        super(headerNotes);

        this.path = path;
        this.filename = filename;
        this.modelType = MainModel.TYPE_MARKDOWN_NOTE;
        this.metadata = new Bundle();
        this.noteLoaded = false;
        this.noteModified = false;
        this.newFilename = false;
        this.oldFilename = null;
        //refreshContent();
    }

    public SingleNote(Bundle args) {
        super(args, headerNotes);
        this.noteLoaded = args.getBoolean("noteLoaded");
        this.noteModified = false;
        this.newFilename = false;
        this.oldFilename = null;
        this.modelType = args.getInt("modelType", MainModel.TYPE_MARKDOWN_NOTE);

        this.path = args.getString("path", "");
        this.filename = args.getString("filename", "");

        if (this.noteLoaded) {
            this.summary = args.getString("summary", "");
            this.title = args.getString("title", "");
            this.body = args.getString("body", "");
            this.metadata = args.getBundle("metadata");
            if (this.metadata == null) this.metadata = new Bundle();

            this.updatedAt = new Date(args.getLong("modifiedDate", 0));
            this.createdAt = new Date(args.getLong("createdDate", 0));
        } else {
            this.metadata = new Bundle();
        }
    }

    @Override
    public void bindViewHolder(FlexibleAdapter<IFlexible> adapter, MyViewHolder holder, int position, List<Object> payloads) {
        viewHolder = new WeakReference<>(holder);
        holder.mNotebookTitle.setText(getName());
        if (summary != null) {
            if (summary.isEmpty()) {
                holder.mNoteSummary.setVisibility(View.GONE);
            } else {
                holder.mNoteSummary.setVisibility(View.VISIBLE);
                holder.mNoteSummary.setText(summary);
            }
        }

        holder.itemView.setAlpha(1.0f);
        if (wasLoaded()) {
            holder.itemView.setVisibility(View.VISIBLE);
        } else {
            holder.itemView.setVisibility(View.GONE);
        }

        if (adapter instanceof MainAdapter) {
            MainAdapter ma = (MainAdapter) adapter;
            if (ma.getSpanCount() == 1) {
                holder.mNoteIcon.setVisibility(View.VISIBLE);
            } else {
                holder.mNoteIcon.setVisibility(View.GONE);
            }

            if (ma.isMovingNote() && ma.getMovingNote().getUuid().equals(getUuid())) {
                holder.itemView.setAlpha(0.8f);
            }
        }
    }

    public void refreshContent() {
        refreshContent(null);
    }

    public void refreshContent(OnNoteLoadedListener mOnNoteLoadedListener) {
        this.title="";
        this.summary="";
        this.updatedAt = new Date();
        this.createdAt = new Date();
        this.metadata = new Bundle();

        this.mOnNoteLoadedListener = mOnNoteLoadedListener;

        LoadNoteTask task = new LoadNoteTask(this);
        task.execute(getFullPath());
    }

    public void unloadNote() {
        if (wasLoaded()) {
            this.noteModified = false;
            this.noteLoaded = false;
            this.summary = null;
            this.body = null;
            this.metadata = new Bundle();
        }
    }

    public String getImageFolderPath() {
        return getPath()+"/."+getFileNameNoExtension();
    }

    private void logNote() {
        Log.i("note", "\n----------------------------");
        Log.i("note", "title:     "+this.title);
        Log.i("note", "summary:   "+this.summary);
        Log.i("note", "updatedAt: "+this.updatedAt);
        Log.i("note", "createdAt: "+this.createdAt);
        Log.i("note", "body:\n"+this.body);
    }

    @Override
    public ArrayList<MainModel> getContent() { return null; }

    private static Bundle parseMetadata(String file_body) {
        Bundle result = new Bundle();
        Bundle metadata = new Bundle();

        String regex = "^(([+-]{3,}\\n)|([a-z]+)\\s?[:=]\\s+['\"]?([\\w\\W\\s]+?)['\"]?\\s*\\n(?=(\\w+\\s?[:=])|\\n|([+-]{3,}\\n)?))\\n*";
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(file_body);

        while (matcher.find()) {
            //&& !matcher.group(1).matches("^\\+\\+\\++")
            if (matcher.group(4) != null && matcher.group(3) != null) {
                String metaKey = matcher.group(3);
                String metaValue = matcher.group(4).trim();
                metadata.putString(metaKey, metaValue);
                if (metaKey.equals("updatedAt")) {
                    result.putString("updatedAt", metaValue);
                } else if (metaKey.equals("createdAt")) {
                    result.putString("createdAt", metaValue);
                }
            }
        }

        result.putString("body", matcher.replaceAll(""));
        result.putBundle("metadata", metadata);

        return result;
    }

    private static Bundle parseTitle(String note_body) {
        String[] lines = note_body.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.isEmpty() && line.startsWith("#")) {
                Bundle ret = new Bundle();
                ret.putString("title", line.replaceAll("^#+\\s*", ""));
                List<String> one = Arrays.asList(lines).subList(0, i);
                List<String> two = Arrays.asList(lines).subList(i+1, lines.length);
                ret.putString("body", TextUtils.join("\n", one)+"\n"+TextUtils.join("\n", two));
                return ret;
            }
        }

        return new Bundle();
    }

    private static String readNoteFile(String full_path) {
        File file = new File(full_path);
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append("\n");
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text.toString();
    }

    @Override
    public String getName() {
        if (this.title == null || this.title.isEmpty()) {
            return this.filename;
        }
        return this.title;
    }

    public boolean doesExist() {
        return new File(getFullPath()).exists();
    }

    public void updateBody(String new_body) {
        body = new_body;
        noteModified = true;

        if (new_body.isEmpty()) {
            summary = "";
            title = "New Note";
            updateFilename(title);
            return;
        }

        Bundle title_parse = parseTitle(new_body);
        String str_title = title_parse.getString("title", "");

        summary = parseSummary(title_parse.getString("body", ""));

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
    }

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

        } catch (FileNotFoundException fnfe1) {
            Log.e("tag", fnfe1.getMessage());
            return false;

        } catch (Exception e) {
            Log.e("tag", e.getMessage());
            return false;
        }
    }

    public void saveNote(OnNoteSavedListener mOnNoteSavedListener) {
        if (newFilename) {
            if (doesExist()) {
                filename = Path.newNoteNameFromCurrent(getPath(), getFileNameNoExtension(), getExtension());
                Log.i("log", "new filename: "+filename);
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

        text.append("+++\n");
        for (String key: this.metadata.keySet()) {
            if (key.equals("updatedAt")) {
                text.append("updatedAt = \"");
                text.append(formatDate(null));
                text.append("\"\n");
            } else if (key.equals("createdAt")) {
                text.append("createdAt = \"");
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
        return new SimpleDateFormat("dd/MM/yyyy", loc).format(updatedAt);
    }
    public String getCreatedString(Locale loc) {
        return new SimpleDateFormat("dd/MM/yyyy", loc).format(createdAt);
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

    private static Date parseDate(String date_string) throws ParseException {
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

    public static String parseSummary(String body) {
        String clean_body = body.replaceAll("[*_#]+", "").replaceAll("\n\\s+", "\n").trim();
        String[] lines = clean_body.split("\\n");
        StringBuilder summary = new StringBuilder();
        for(String line: lines) {
            summary.append(line);
            if (summary.length() > 250) {
                break;
            }
            if (!line.isEmpty()) summary.append("\n");
        }

        return summary.toString().trim();
    }

    @Override
    public Bundle toBundle() {
        Bundle args = super.toBundle();
        args.putString("path", path);
        args.putString("filename", filename);
        args.putString("summary", summary);
        args.putString("title", title);
        args.putString("body", body);

        if (updatedAt == null) updatedAt = new Date();
        if (createdAt == null) createdAt = new Date();

        args.putLong("modifiedDate", updatedAt.getTime());
        args.putLong("createdDate", createdAt.getTime());
        args.putBundle("metadata", metadata);
        args.putBoolean("noteLoaded", noteLoaded);
        return args;
    }

    public Bundle toLightBundle() {
        Bundle args = super.toBundle();
        args.putString("path", path);
        args.putString("filename", filename);
        args.putBoolean("noteLoaded", false);
        return args;
    }

    private static class LoadNoteTask extends AsyncTask<String, Void, Bundle> {

        SingleNote note;

        LoadNoteTask(SingleNote thisNote) {
            this.note = thisNote;
        }

        @Override
        protected Bundle doInBackground(String... paths) {
            String file_body = readNoteFile(paths[0]);

            Bundle args = parseMetadata(file_body);
            Bundle title_parse = parseTitle(args.getString("body", ""));

            String title = title_parse.getString("title", "");
            String summary = parseSummary(title_parse.getString("body", ""));

            args.putString("title", title);
            args.putString("summary", summary);

            return args;
        }

        @Override
        protected void onPostExecute(Bundle result) {
            if (note == null) return;

            note.body = result.getString("body", "");
            note.title = result.getString("title", "");
            note.summary = result.getString("summary", "");
            note.metadata = result.getBundle("metadata");

            try {
                note.updatedAt = parseDate(result.getString("updatedAt", ""));
                note.createdAt = parseDate(result.getString("createdAt", ""));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            note.noteLoaded = true;

            if (note.viewHolder != null && note.viewHolder.get() != null) {
                MyViewHolder viewHolder = note.viewHolder.get();
                viewHolder.mNotebookTitle.setText(note.title);
                viewHolder.mNoteSummary.setText(note.summary);
            }

            if (note.mOnNoteLoadedListener != null) {
                note.mOnNoteLoadedListener.NoteLoaded(note);
            }

            //note.logNote();
        }
    }

    public ArrayList<String> getPathArray(String root_path) {
        ArrayList<String> path = new ArrayList<>();
        File f = new File(root_path);
        File n = new File(getPath());

        while(!n.getPath().equals(f.getPath())) {
            if (n.isDirectory()) path.add(n.getName());
            n = n.getParentFile();
        }

        return path;
    }

    public interface OnNoteLoadedListener {
        void NoteLoaded(SingleNote note);
    }

    public interface OnNoteSavedListener {
        void NoteSaved(boolean saved);
    }
}
