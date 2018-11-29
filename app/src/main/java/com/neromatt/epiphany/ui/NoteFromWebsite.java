package com.neromatt.epiphany.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.helper.AdBlocker;
import com.neromatt.epiphany.helper.CreateNoteHelper;
import com.neromatt.epiphany.model.DataObjects.SingleNote;
import com.neromatt.epiphany.tasks.ImageDownloaderTask;

import java.util.ArrayList;
import java.util.regex.Pattern;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class NoteFromWebsite extends AppCompatActivity implements CreateNoteHelper.OnQuickPathListener, ImageDownloaderTask.ImageDownloadListener {

    private String root_path;
    private String title;
    private String url;

    private ImageDownloaderTask image_downloader_task;
    private SingleNote current_note;

    private TurndownView turndownView;

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

        AdBlocker.init(this);

        initializeUI();
    }

    private void initializeUI() {
        turndownView = findViewById(R.id.turndownView);
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
                        createNoteFromUrl(url);
                    }
                });

        new AlertDialog.Builder(this)
            .setCancelable(false)
            .setTitle(R.string.dialog_html_to_markdown_title)
            .setMessage(R.string.dialog_html_to_markdown_message)
            .setPositiveButton(R.string.dialog_html_to_markdown_yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    turndownView.loadUrl(url);
                }
            })
            .setNegativeButton(R.string.dialog_html_to_markdown_no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    createNoteFromUrl(url);
                }
            })
            .show();
    }

    private void createNoteFromUrl(String url) {
        CreateNoteHelper.addQuickNoteAndSave(root_path, title, url, NoteFromWebsite.this, new CreateNoteHelper.OnQUickNoteSaved() {
            @Override
            public void QuickNoteSaved(SingleNote note) {
                Intent intent = new Intent(NoteFromWebsite.this, EditorActivity.class);
                intent.putExtra("note", note.toBundle());
                intent.putExtra("root", root_path);
                startActivityForResult(intent, Constants.NOTE_PREVIEW_REQUEST_CODE);
            }
        });
    }

    private void createNote(String body) {
        turndownView.clearHistory();
        turndownView.clearCache(true);

        CreateNoteHelper.addQuickNoteAndSave(root_path, title, body, NoteFromWebsite.this, new CreateNoteHelper.OnQUickNoteSaved() {
            @Override
            public void QuickNoteSaved(SingleNote note) {
                current_note = note;

                if (image_downloader_task != null && !image_downloader_task.isCancelled()) {
                    image_downloader_task.cancel(true);
                }

                image_downloader_task = new ImageDownloaderTask(note.getImageFolderPath(), NoteFromWebsite.this);
                image_downloader_task.execute(note.getRemoteImages().toArray(new String[0]));
            }
        });
    }

    @Override
    public void ImagesDone(ArrayList<ImageDownloaderTask.ImageReplace> list) {
        String body = current_note.getBody();
        for (ImageDownloaderTask.ImageReplace img: list) {
            String escaped_url = Pattern.quote(img.old_path);
            body = body.replaceAll("!\\[([^]]*?)]\\(("+escaped_url+")\\)", "![$1]("+img.new_path+")");
        }

        current_note.updateBody(body);
        current_note.saveNote(new SingleNote.OnNoteSavedListener() {
            @Override
            public void NoteSaved(boolean saved) {
                Intent intent = new Intent(NoteFromWebsite.this, ViewNote.class);
                intent.putExtra("note", current_note.toBundle());
                intent.putExtra("root", root_path);
                startActivityForResult(intent, Constants.NOTE_PREVIEW_REQUEST_CODE);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (image_downloader_task != null && !image_downloader_task.isCancelled()) {
            image_downloader_task.cancel(true);
        }
    }

    @Override
    public void QuickPathCreated(Bundle quick_bundle) {
        Log.i("bucket", "created");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        super.onActivityResult(requestCode, resultCode, resultIntent);
        if (requestCode == Constants.NOTE_PREVIEW_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (resultIntent != null && resultIntent.getBooleanExtra("edit", false)) {
                    Intent intent = new Intent(NoteFromWebsite.this, EditorActivity.class);
                    intent.putExtra("note", resultIntent.getBundleExtra("note"));
                    intent.putExtra("root", root_path);
                    startActivityForResult(intent, Constants.NOTE_EDITOR_REQUEST_CODE);

                } else {
                    Intent returnIntent = new Intent();
                    setResult(Activity.RESULT_OK, returnIntent);
                    finish();
                }
            }
        } else if (requestCode == Constants.NEW_NOTE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Intent returnIntent = new Intent();
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        }
    }
}
