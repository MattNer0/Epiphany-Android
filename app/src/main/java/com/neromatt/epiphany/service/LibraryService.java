package com.neromatt.epiphany.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.helper.DBInterface;
import com.neromatt.epiphany.helper.Database;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.Library;
import com.neromatt.epiphany.model.NotebooksComparator;
import com.neromatt.epiphany.model.Path;
import com.neromatt.epiphany.ui.MainActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class LibraryService extends IntentService implements DBInterface {

    private Database db;

    //private Library.serviceRequestEnum request;
    //private Path path;
    //private ArrayList<MainModel> buckets;
    //private int racks_loaded;

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
        db = new Database(getApplicationContext());

        Bundle extras = intent.getExtras();
        if (extras == null) {
            stopSelf();
            return;
        }

        final Library.serviceRequestEnum request = (Library.serviceRequestEnum) extras.getSerializable("request");

        if (request == Library.serviceRequestEnum.BUCKETS) {
            String rootPath = extras.getString("root", "");
            if (rootPath.isEmpty()) {
                stopSelf();
                return;
            }

            Path path = new Path(rootPath);
            answerList("buckets", path.getBuckets(), null);
            stopSelf();

        } else if (request == Library.serviceRequestEnum.FOLDER) {
            final MainModel model = extras.getParcelable("model");
            model.loadContent(this, new MainModel.OnModelLoadedListener() {
                @Override
                public void ModelLoaded() {
                    answerList("folders", model.getContent(), model.getUUID());
                    stopSelf();
                }
            });

        } else if (request == Library.serviceRequestEnum.NOTES) {
            final MainModel model = extras.getParcelable("model");
            model.reloadNotes(false, this, new MainModel.OnModelLoadedListener() {
                @Override
                public void ModelLoaded() {
                    answerTaskFinished(request, model.getUUID());
                    stopSelf();
                }
            });

        } else if (request == Library.serviceRequestEnum.CLEAN) {
            if (db.deleteFolders() && db.deleteNotes()) {
                Log.i(Constants.LOG, "finished!");
                answerTaskFinished(request, null);
            }

            stopSelf();
        }
    }

    private void answerList(String key, ArrayList<MainModel> models_list, UUID current_model) {
        Collections.sort(models_list, new NotebooksComparator(this));

        Intent returnIntent = new Intent();
        returnIntent.putParcelableArrayListExtra(key, models_list);

        if (current_model != null) {
            returnIntent.putExtra("uuid", current_model.toString());
        }

        returnIntent.setAction(Constants.BROADCAST_FILTER);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(returnIntent);
    }

    private void answerTaskFinished(Library.serviceRequestEnum request, UUID current_model) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("request", request);

        if (current_model != null) {
            returnIntent.putExtra("uuid", current_model.toString());
        }

        returnIntent.setAction(Constants.BROADCAST_FILTER);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(returnIntent);
    }

    @Override
    public Database getDatabase() {
        return db;
    }

    @Override
    public Context getContext() {
        return getApplicationContext();
    }

    @Override
    public void onDestroy() {
        if (db != null) db.closeDB();
    }

    /*private void loadNextModel(MainModel model) {
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
    }*/
}