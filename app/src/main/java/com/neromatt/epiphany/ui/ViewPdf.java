package com.neromatt.epiphany.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.github.rahatarmanahmed.cpv.CircularProgressView;
import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.helper.CreateNoteHelper;
import com.neromatt.epiphany.helper.Database;

import com.neromatt.epiphany.model.DataObjects.SingleNote;
import com.neromatt.epiphany.tasks.ParsePdfTask;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class ViewPdf extends AppCompatActivity implements CreateNoteHelper.OnQuickPathListener, ParsePdfTask.PdfListener {

    private String root_path;
    private Database db;

    private Toolbar toolbar;
    private MarkedView markdownView;
    private CircularProgressView progressBar;
    private LinearLayout loading_note;

    private String pdf_title;
    private String pdf_content;

    private ParsePdfTask pdf_task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_note);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        root_path = PreferenceManager.getDefaultSharedPreferences(this).getString("pref_root_directory", "");

        if (prefs.getBoolean("firstrun", true)) {
            finish();
        }

        db = new Database(getApplicationContext());

        Intent intent = getIntent();

        pdf_title = intent.getData().getPath();
        pdf_content = "";

        PDFBoxResourceLoader.init(getApplicationContext());

        progressBar = findViewById(R.id.progress_view);
        loading_note = findViewById(R.id.loading_note);
        loading_note.setVisibility(View.VISIBLE);

        if (pdf_task != null && !pdf_task.isCancelled()) {
            pdf_task.cancel(true);
        }

        pdf_task = new ParsePdfTask(this, this);
        pdf_task.execute(intent.getData());

        /*Bundle note_bundle = intent.getBundleExtra("note");
        if (note_bundle == null) {
            String notepath = intent.getStringExtra("notePath");
            String notename = intent.getStringExtra("noteName");
            note = new SingleNote(notepath, notename);
        } else {
            note = new SingleNote(note_bundle);
        }*/

    }

    @Override
    public void onPause() {
        if (pdf_task != null && !pdf_task.isCancelled()) {
            pdf_task.cancel(true);
        }
        super.onPause();
    }

    private void initializeUI() {
        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (loading_note != null) {
            loading_note.setVisibility(View.GONE);
        }

        setTitle(pdf_title);

        markdownView = findViewById(R.id.markdownView);
        markdownView
            .setImageClickCallback(new MarkedView.OnImageClickListener() {
                @Override
                public void ImageClick(String image_url) {
                    if (image_url == null) return;
                    Intent intent = new Intent(ViewPdf.this, ViewPhoto.class);
                    intent.putExtra("image", image_url);
                    startActivity(intent);
                }
            })
            .setMDText(pdf_content);

        registerForContextMenu(markdownView);
    }

    private void saveNote() {
        CreateNoteHelper.addQuickNoteAndSave(root_path, pdf_title, pdf_content, ViewPdf.this, new CreateNoteHelper.OnQUickNoteSaved() {
            @Override
            public void QuickNoteSaved(SingleNote note) {
                Intent intent = new Intent(ViewPdf.this, EditorActivity.class);
                intent.putExtra("note", note.toBundle());
                intent.putExtra("root", root_path);
                startActivityForResult(intent, Constants.NOTE_PREVIEW_REQUEST_CODE);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_note_pdf, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save_pdf:
                saveNote();
                break;
            case R.id.action_font_10:
                item.setChecked(true);
                if (markdownView != null) markdownView.setFontSize(10);
                break;
            case R.id.action_font_12:
                item.setChecked(true);
                if (markdownView != null) markdownView.setFontSize(12);
                break;
            case R.id.action_font_14:
                item.setChecked(true);
                if (markdownView != null) markdownView.setFontSize(14);
                break;
            case R.id.action_font_15:
                item.setChecked(true);
                if (markdownView != null) markdownView.setFontSize(15);
                break;
            case R.id.action_font_16:
                item.setChecked(true);
                if (markdownView != null) markdownView.setFontSize(16);
                break;
            case R.id.action_font_18:
                item.setChecked(true);
                if (markdownView != null) markdownView.setFontSize(18);
                break;
            case R.id.action_font_20:
                item.setChecked(true);
                if (markdownView != null) markdownView.setFontSize(20);
                break;
            case R.id.action_font_24:
                item.setChecked(true);
                if (markdownView != null) markdownView.setFontSize(24);
                break;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
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
                    Intent intent = new Intent(ViewPdf.this, EditorActivity.class);
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

    @Override
    public void Progress(int page, int pages) {
        if (page == 0) {
            progressBar.setMaxProgress(pages);
            progressBar.setIndeterminate(false);
        }
        progressBar.setProgress(page);
        if (page >= pages-1) {
            progressBar.setIndeterminate(true);
        }
    }

    @Override
    public void TextParsed(String body) {
        pdf_content = body;
        initializeUI();
    }
}
