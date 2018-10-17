package com.neromatt.epiphany.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.DataObjects.SingleNotebook;
import com.neromatt.epiphany.model.DataObjects.SingleRack;
import com.neromatt.epiphany.model.NotebooksComparator;
import com.neromatt.epiphany.model.Path;

import java.util.ArrayList;
import java.util.Collections;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class LibraryService extends IntentService {

    private Path path;
    private ArrayList<MainModel> buckets;
    private int racks_loaded;

    public LibraryService() {
        super("LibraryService");
    }

    /**
     * The IntentService calls this method from the default worker thread with
     * the intent that started the service. When this method returns, IntentService
     * stops the service, as appropriate.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            stopSelf();
            return;
        }

        String rootPath = extras.getString("root", "");
        path = new Path(rootPath);

        buckets = path.getBuckets();
        racks_loaded = 0;

        loadNextModel(buckets.get(racks_loaded));
    }

    private void loadNextModel(MainModel model) {
        if (model instanceof SingleRack) {
            final SingleRack modelRack = (SingleRack) model;
            modelRack.loadContent(this, new MainModel.OnModelLoadedListener() {
                @Override
                public void ModelLoaded() {
                    racks_loaded++;

                    if (modelRack.isQuickNotes()) {
                        SingleNotebook folder = (SingleNotebook) modelRack.getFirstFolder();
                        if (folder != null) folder.setQuickNotesFolder();
                    }

                    if (racks_loaded >= buckets.size()) {
                        loadingFinished();
                    } else {
                        loadNextModel(buckets.get(racks_loaded));
                    }
                }
            });
        }
    }

    private void loadingFinished() {
        Log.i("service", "loading finished");
        Collections.sort(buckets, new NotebooksComparator(this));

        Intent returnIntent = new Intent();
        returnIntent.putParcelableArrayListExtra("buckets", buckets);
        returnIntent.setAction(Constants.BROADCAST_FILTER);

        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(returnIntent);
        stopSelf();
    }
}