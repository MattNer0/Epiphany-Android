package com.neromatt.epiphany.ui;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;

import com.neromatt.epiphany.model.Path;

import io.github.kobakei.materialfabspeeddial.FabSpeedDial;
import us.feras.mdv.MarkdownView;

public class ViewNote extends AppCompatActivity {

    //private Toolbar toolbar;
    private FabSpeedDial noteFab;
    private Path path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_note);

        String path= PreferenceManager.getDefaultSharedPreferences(this).getString("pref_root_directory", "");
        this.path = new Path(path);
        //setupToolBar();

        Intent intent = getIntent();
        String notepath = intent.getStringExtra("notePath");
        MarkdownView markdownView = findViewById(R.id.markdownView);

        //TODO error handling if null
        markdownView.loadMarkdown(this.path.getNoteMarkdown(notepath),"file:///android_asset/markdownview.css");
        //getSupportActionBar().hide();

        noteFab = findViewById(R.id.noteviewFab);
        noteFab.addOnMenuItemClickListener(new FabSpeedDial.OnMenuItemClickListener() {
            @Override
            public void onMenuItemClick(FloatingActionButton miniFab, @Nullable TextView label, int itemId) {
                Log.i("fab", "id: "+itemId);
            }
        });

    }
    /*private void setupToolBar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar); // Attaching the layout to the toolbar object
        if(toolbar!=null) {
            setSupportActionBar(toolbar);    // Setting toolbar as the ActionBar with setSupportActionBar() call
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }*/
}
