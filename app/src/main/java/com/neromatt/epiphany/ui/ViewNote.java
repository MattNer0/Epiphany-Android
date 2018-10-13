package com.neromatt.epiphany.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.DataObjects.SingleNote;
import com.neromatt.epiphany.model.Path;

import io.github.kobakei.materialfabspeeddial.FabSpeedDial;

public class ViewNote extends AppCompatActivity {

    //private Toolbar toolbar;
    private Path path;
    private SingleNote note;
    private boolean from_editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_note);

        String path= PreferenceManager.getDefaultSharedPreferences(this).getString("pref_root_directory", "");
        this.path = new Path(path);
        //setupToolBar();

        Intent intent = getIntent();
        Bundle note_bundle = intent.getBundleExtra("note");
        if (note_bundle == null) {
            String notepath = intent.getStringExtra("notePath");
            String notename = intent.getStringExtra("noteName");
            note = new SingleNote(notepath, notename);
        } else {
            note = new SingleNote(note_bundle);
        }

        from_editor = intent.getBooleanExtra("from_editor", false);

        MarkedView markdownView = findViewById(R.id.markdownView);

        markdownView.setCheckboxCallback(new MarkedView.OnCheckboxChangedListener() {
            @Override
            public void CheckboxChange(String note_body, int num, boolean checked) {
                note.updateBody(note_body);
            }
        });
        markdownView.setNoteImagePath(note.getImageFolderPath());

        //TODO error handling if null
        markdownView.setMDText(note.getMarkdown());
        // "file:///android_asset/markdownview.css"
        //getSupportActionBar().hide();

        FabSpeedDial noteFab = findViewById(R.id.noteviewFab);
        noteFab.addOnMenuItemClickListener(new FabSpeedDial.OnMenuItemClickListener() {
            @Override
            public void onMenuItemClick(FloatingActionButton miniFab, @Nullable TextView label, int itemId) {
                if (label == null) return;
                if (label.getText().toString().equals("Edit")) {
                    editNote();
                } else {
                    showInfo();
                }
            }
        });

        FloatingActionButton close_info = findViewById(R.id.close_info);
        close_info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideInfo();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (note != null && note.wasModified()) {
            note.saveNote(new SingleNote.OnNoteSavedListener() {
                @Override
                public void NoteSaved(boolean saved) {
                    if (saved) {
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("modified", true);
                        setResult(Activity.RESULT_OK, returnIntent);
                        finish();
                    } else {
                        Toast.makeText(ViewNote.this, "Note couldn't be saved", Toast.LENGTH_LONG).show();
                    }
                }
            });
        } else {
            Intent returnIntent = new Intent();
            returnIntent.putExtra("modified", false);
            setResult(Activity.RESULT_OK, returnIntent);
            finish();
        }
    }

    public void updateInfo() {
        TextView note_created_at = findViewById(R.id.note_created_at);
        TextView note_updated_at = findViewById(R.id.note_updated_at);
        TextView note_lines = findViewById(R.id.note_lines);

        note_created_at.setText(note.getCreatedString(MainModel.getCurrentLocale(this)));
        note_updated_at.setText(note.getLastModifiedString(MainModel.getCurrentLocale(this)));

        StringBuilder sb = new StringBuilder();
        sb.append(note.getLinesNumber());
        note_lines.setText(sb.toString());
    }

    public void showInfo() {
        RelativeLayout bInfo = findViewById(R.id.bottom_info);
        FabSpeedDial noteFab = findViewById(R.id.noteviewFab);

        updateInfo();

        noteFab.hide();
        slideUp(bInfo);
    }

    public void hideInfo() {
        RelativeLayout bInfo = findViewById(R.id.bottom_info);
        FabSpeedDial noteFab = findViewById(R.id.noteviewFab);

        noteFab.show();
        slideDown(bInfo);
    }

    public void editNote() {
        if (note != null && note.wasModified()) {
            note.saveNote(new SingleNote.OnNoteSavedListener() {
                @Override
                public void NoteSaved(boolean saved) {
                if (saved) {
                    editNote();
                } else {
                    Toast.makeText(ViewNote.this, "Note couldn't be saved", Toast.LENGTH_LONG).show();
                }
                }
            });
        } else {
            if (from_editor) {
                onBackPressed();
            } else {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("edit", true);
                returnIntent.putExtra("note", note.toBundle());
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        }
    }

    public void slideUp(View view){
        view.setVisibility(View.VISIBLE);
        TranslateAnimation animate = new TranslateAnimation(
                0,                 // fromXDelta
                0,                 // toXDelta
                view.getHeight(),  // fromYDelta
                0);                // toYDelta
        animate.setDuration(500);
        animate.setFillAfter(true);
        view.startAnimation(animate);
    }

    // slide the view from its current position to below itself
    public void slideDown(View view){
        TranslateAnimation animate = new TranslateAnimation(
                0,                 // fromXDelta
                0,                 // toXDelta
                0,                 // fromYDelta
                view.getHeight()); // toYDelta
        animate.setDuration(500);
        animate.setFillAfter(true);
        view.startAnimation(animate);
    }
}
