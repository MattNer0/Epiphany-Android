package com.neromatt.epiphany.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.github.rahatarmanahmed.cpv.CircularProgressView;
import com.github.rahatarmanahmed.cpv.CircularProgressViewListener;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.DataObjects.SingleRack;
import com.neromatt.epiphany.model.NotebooksComparator;
import com.neromatt.epiphany.model.Path;

import java.util.ArrayList;
import java.util.Collections;

public class LoadLibrary extends AppCompatActivity {

    private Path path;
    private ArrayList<MainModel> buckets;
    private int racks_loaded;

    private CircularProgressView progressView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.load_library);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras == null) {
            finish();
            return;
        }

        String rootPath = extras.getString("root", "");
        path = new Path(rootPath);

        buckets = path.getBuckets();
        racks_loaded = 0;

        progressView = findViewById(R.id.progress_view);
        progressView.setMaxProgress(buckets.size());
        progressView.setProgress(racks_loaded);
        progressView.startAnimation();

        progressView.addListener(new CircularProgressViewListener() {
            @Override
            public void onProgressUpdate(float currentProgress) {

            }

            @Override
            public void onProgressUpdateEnd(float currentProgress) {
                if (currentProgress == buckets.size()) {
                    Log.i("log", "finish!");
                    loadingFinished();
                }
            }

            @Override
            public void onAnimationReset() {

            }

            @Override
            public void onModeChanged(boolean isIndeterminate) {

            }
        });

        loadNextModel(buckets.get(racks_loaded));
    }

    private void loadNextModel(MainModel model) {
        Log.i("log", "loading model...");
        if (model instanceof SingleRack) {
            SingleRack modelRack = (SingleRack) model;
            modelRack.loadContent(this, new MainModel.OnModelLoadedListener() {
                @Override
                public void ModelLoaded() {
                    racks_loaded++;
                    progressView.setProgress(racks_loaded);

                    if (racks_loaded >= buckets.size()) {
                        Log.i("log", "all folders loaded");
                    } else {
                        loadNextModel(buckets.get(racks_loaded));
                    }
                }
            });
        }
    }

    private void loadingFinished() {
        Collections.sort(buckets, new NotebooksComparator(this));

        Intent returnIntent = new Intent();
        returnIntent.putParcelableArrayListExtra("buckets", buckets);
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    @Override
    public void onBackPressed() {

    }
}
