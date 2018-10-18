package com.neromatt.epiphany.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.model.DataObjects.SingleNote;
import com.neromatt.epiphany.model.Path;
import com.neromatt.epiphany.ui.NotebookFragment.CreateNoteHelper;

import androidx.appcompat.app.AppCompatActivity;

public class ShareActivity extends AppCompatActivity implements CreateNoteHelper.OnQuickPathListener {

    String root_path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        root_path = PreferenceManager.getDefaultSharedPreferences(this).getString("pref_root_directory", "");

        if (prefs.getBoolean("firstrun", true)) {
            finish();
        }

        Intent intent = getIntent();
        if (intent == null) return;
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(intent.getAction()) && type != null) {
            if ("text/plain".equals(type)) {
                newSharedNote(intent);
            }
        }
    }

    private void newSharedNote(Intent intent) {
        newNoteFromShared(
                intent.getStringExtra(Intent.EXTRA_SUBJECT),
                intent.getStringExtra(Intent.EXTRA_TEXT)
        );
    }

    private void newNoteFromShared(String title, String text) {
        if (text == null) return;

        if (title == null) {
            text = "# New Quick Note\n\n" + text;
        } else {
            text = "# "+title.trim()+"\n\n" + text;
        }

        CreateNoteHelper mCreateNoteHelper = new CreateNoteHelper(this, new Path(root_path));
        mCreateNoteHelper.addQuickNoteAndSave(text, this, new CreateNoteHelper.OnQUickNoteSaved() {
            @Override
            public void QuickNoteSaved(SingleNote note) {
                Intent intent = new Intent(ShareActivity.this, EditorActivity.class);
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
                finish();
            }
        }
    }
}
