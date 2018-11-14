package com.neromatt.epiphany.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.neromatt.epiphany.helper.DirMetaFile;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.DataObjects.SingleNote;
import com.neromatt.epiphany.model.DataObjects.SingleNotebook;
import com.neromatt.epiphany.model.NotebooksComparator;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;

import androidx.annotation.NonNull;

public class ReadFoldersTask extends AsyncTask<String, Void, ArrayList<MainModel>> {

    private final FoldersListener listener;
    private final WeakReference<Context> context;

    public ReadFoldersTask(Context context, @NonNull FoldersListener listener) {
        this.context = new WeakReference<>(context);
        this.listener = listener;
    }

    @Override
    protected ArrayList<MainModel> doInBackground(String... strings) {
        File dir = new File(strings[0]);
        ArrayList<MainModel> filesArray = new ArrayList<>();
        if (dir.exists()) {
            for (File f : dir.listFiles()) {
                if (!f.getName().startsWith(".")) {
                    if (f.isDirectory()) {
                        filesArray.add(new SingleNotebook(f.getName(), f.toString(), DirMetaFile.read(f.toString(), ".folder.json")));
                    } else {
                        String extension = getFileExtension(f);
                        if ((extension.equalsIgnoreCase("txt"))||(extension.equalsIgnoreCase("md"))) {
                            filesArray.add(new SingleNote(dir.getPath(),f.getName()));
                        }
                    }
                }
            }
        }
        Collections.sort(filesArray, new NotebooksComparator(context.get()));
        return filesArray;
    }

    private static String getFileExtension(File filename){
        return filename.toString().substring(filename.toString().lastIndexOf('.') + 1);
    }

    @Override
    protected void onPostExecute(ArrayList<MainModel> data) {
        if (isCancelled()) return;
        listener.FoldersLoaded(data);
    }

    public interface FoldersListener {
        void FoldersLoaded(ArrayList<MainModel> list);
    }
}
