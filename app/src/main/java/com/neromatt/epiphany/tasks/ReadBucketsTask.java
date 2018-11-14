package com.neromatt.epiphany.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.neromatt.epiphany.helper.DirMetaFile;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.DataObjects.SingleRack;
import com.neromatt.epiphany.model.NotebooksComparator;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;

import androidx.annotation.NonNull;

public class ReadBucketsTask extends AsyncTask<String, Void, ArrayList<MainModel>> {

    private final BucketsListener listener;
    private final WeakReference<Context> context;

    public ReadBucketsTask(Context context, @NonNull BucketsListener listener) {
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
                        filesArray.add(new SingleRack(f.getName(), f.toString(), DirMetaFile.read(f.toString(), ".bucket.json")));
                    }
                }
            }
        }
        Collections.sort(filesArray, new NotebooksComparator(context.get()));
        return filesArray;
    }

    @Override
    protected void onPostExecute(ArrayList<MainModel> data) {
        if (isCancelled()) return;
        listener.BucketsLoaded(data);
    }

    public interface BucketsListener {
        void BucketsLoaded(ArrayList<MainModel> list);
    }
}
