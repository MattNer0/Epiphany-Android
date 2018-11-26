package com.neromatt.epiphany.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.helper.CreateNoteHelper;
import com.neromatt.epiphany.model.DataObjects.SingleNote;

import androidx.appcompat.app.AppCompatActivity;

public class NoteFromWebsite extends AppCompatActivity implements CreateNoteHelper.OnQuickPathListener {

    private String root_path;
    private String title;
    private String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_website);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        root_path = PreferenceManager.getDefaultSharedPreferences(this).getString("pref_root_directory", "");

        if (prefs.getBoolean("firstrun", true)) {
            finish();
        }

        Intent intent = getIntent();
        title = intent.getStringExtra("title");
        url = intent.getStringExtra("url");

        initializeUI();
    }

    private void initializeUI() {
        TurndownView turndownView = findViewById(R.id.turndownView);

        turndownView
                .setHtmlBodyCallback(new TurndownView.OnMarkdownBodyListener() {
                    @Override
                    public void MarkdownBody(String note_body) {
                        if (note_body == null) return;

                        note_body = "[Source]("+ url +")\n\n" + note_body;

                        createNote(note_body);
                    }

                    @Override
                    public void LoadFailed() {
                        Log.i(Constants.LOG, "load failed!");
                        createNote(url);
                    }
                });

        turndownView.loadUrl(url);
    }

    private void createNote(String body) {
        CreateNoteHelper.addQuickNoteAndSave(root_path, title, body, NoteFromWebsite.this, new CreateNoteHelper.OnQUickNoteSaved() {
            @Override
            public void QuickNoteSaved(SingleNote note) {
                Intent intent = new Intent(NoteFromWebsite.this, EditorActivity.class);
                intent.putExtra("note", note.toBundle());
                intent.putExtra("root", root_path);
                startActivityForResult(intent, Constants.NOTE_EDITOR_REQUEST_CODE);
            }
        });
    }

    @Override
    public void QuickPathCreated(Bundle quick_bundle) {
        Log.i("bucket", "created");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        super.onActivityResult(requestCode, resultCode, resultIntent);
        if (requestCode == Constants.NOTE_EDITOR_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Intent returnIntent = new Intent();
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        }
    }
}
