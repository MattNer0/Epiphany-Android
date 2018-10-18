package com.neromatt.epiphany.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.neromatt.epiphany.model.DataObjects.SingleNote;
import com.neromatt.epiphany.model.Path;
import com.yydcdut.markdown.callback.OnLinkClickCallback;
import com.yydcdut.markdown.callback.OnTodoClickCallback;
import com.yydcdut.markdown.loader.DefaultLoader;
import com.yydcdut.markdown.syntax.edit.EditFactory;
import com.yydcdut.markdown.theme.ThemeDefault;
import com.yydcdut.rxmarkdown.RxMDConfiguration;
import com.yydcdut.rxmarkdown.RxMDEditText;
import com.yydcdut.rxmarkdown.RxMarkdown;

import java.io.File;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class EditorActivity extends AppCompatActivity {

    private RxMDEditText editTextField;
    private SingleNote note = null;
    private TextUndoRedo mTextUndoRedo;
    private boolean onBackPressed;
    private boolean noteModified = false;
    private Menu optionsMenu = null;

    private String root_path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_note);
        setupToolBar();

        editTextField = findViewById(R.id.edit_text);
        editTextField.setText("");

        RxMarkdown.live(editTextField)
                .config(getMarkdownEditorConfiguration())
                .factory(EditFactory.create())
                .intoObservable()
                .subscribe();

        Intent intent = getIntent();
        Bundle notebundle = intent.getBundleExtra("note");
        String folder = intent.getStringExtra("folder");

        root_path = intent.getStringExtra("root");

        if (notebundle != null) {
            note = new SingleNote(notebundle);
        } else if (folder != null && !folder.isEmpty()) {
            File path = new File(folder);
            String folder_path;
            String note_name_edit;

            if (path.isDirectory()) {
                note_name_edit = Path.newNoteName(folder, "md");
                folder_path = folder;
            } else {
                note_name_edit = path.getName();
                folder_path = path.getParentFile().getPath();
            }
            note = new SingleNote(folder_path, note_name_edit);
            note.markAsNewFile();

            String note_body = intent.getStringExtra("body");
            if (note_body != null && !note_body.isEmpty()) {
                note.updateBody(note_body);
                editTextField.setText(note_body);
            }
        }

        onBackPressed = false;
        if (notebundle != null) {
            this.setTitle(R.string.title_edit_note);
            if (note.doesExist()) {
                note.refreshContent(new SingleNote.OnNoteLoadedListener() {
                    @Override
                    public void NoteLoaded(SingleNote note) {
                        String noteText = note.getMarkdown();
                        editTextField.setText(noteText);
                        addTextChangeListener();
                    }
                });
            } else {
                String noteText = note.getMarkdown();
                if (noteText != null) editTextField.setText(noteText);
                addTextChangeListener();
            }
        } else {
            this.setTitle(R.string.title_add_note);
            addTextChangeListener();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUndoRedo();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("root", this.root_path);
        outState.putBundle("note", this.note.toBundle());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        this.note = new SingleNote(savedInstanceState.getBundle("note"));
        this.root_path = savedInstanceState.getString("root", "");
    }

    private void checkUndoRedo() {
        if (optionsMenu != null && mTextUndoRedo != null) {
            MenuItem itemUndo = optionsMenu.findItem(R.id.undo_note);
            MenuItem itemRedo = optionsMenu.findItem(R.id.redo_note);
            if (mTextUndoRedo.canUndo()) {
                itemUndo.setEnabled(true);
                itemUndo.getIcon().setAlpha(255);
            } else {
                itemUndo.setEnabled(false);
                itemUndo.getIcon().setAlpha(130);
            }

            if (mTextUndoRedo.canRedo()) {
                itemRedo.setEnabled(true);
                itemRedo.getIcon().setAlpha(255);
            } else {
                itemRedo.setEnabled(false);
                itemRedo.getIcon().setAlpha(130);
            }
        }
    }

    private void addTextChangeListener() {
        mTextUndoRedo = new TextUndoRedo(editTextField, new TextUndoRedo.TextChangeInfo() {
            @Override
            public void textAction() {
                note.updateBody(editTextField.getText().toString());
                checkUndoRedo();
            }
        });
    }

    private void setupToolBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    private void saveNote(final boolean quit) {
        if (note.wasModified()) {
            String note_text = editTextField.getText().toString();
            if (note_text.isEmpty() && (note.getMarkdown() == null || note.getMarkdown().isEmpty()))
                return;

            note.updateBody(note_text);
            noteModified = true;
            note.saveNote(new SingleNote.OnNoteSavedListener() {
                @Override
                public void NoteSaved(boolean saved) {
                    if (saved) {
                        Toast.makeText(EditorActivity.this, R.string.toast_note_saved, Toast.LENGTH_LONG).show();
                        if (quit) {
                            finishEditor(true);
                        }
                    } else {
                        Toast.makeText(EditorActivity.this, R.string.toast_note_saved_fail, Toast.LENGTH_LONG).show();
                    }
                }
            });
            //TODO, only overwrite when date is newer then the already synced version, else make a copy with _1
            //also some exception handling would be nice here
        }
    }

    private RxMDConfiguration getMarkdownEditorConfiguration() {
        return new RxMDConfiguration.Builder(this)
            .setHeader1RelativeSize(1.6f)
            .setHeader2RelativeSize(1.5f)
            .setHeader3RelativeSize(1.4f)
            .setHeader4RelativeSize(1.3f)
            .setHeader5RelativeSize(1.2f)
            .setHeader6RelativeSize(1.1f)
            .setBlockQuotesLineColor(Color.LTGRAY)
            .setBlockQuotesBgColor(Color.LTGRAY, Color.RED, Color.BLUE)
            .setBlockQuotesRelativeSize(1.0f)
            .setHorizontalRulesColor(Color.LTGRAY)
            .setHorizontalRulesHeight(Color.LTGRAY)
            .setCodeFontColor(Color.LTGRAY)
            .setCodeBgColor(Color.LTGRAY)
            .setTheme(new ThemeDefault())
            .setTodoColor(Color.DKGRAY)
            .setTodoDoneColor(Color.DKGRAY)
            .setOnTodoClickCallback(new OnTodoClickCallback() {
                @Override
                public CharSequence onTodoClicked(View view, String line, int lineNumber) {
                    return editTextField.getText();
                }
            })
            .setUnOrderListColor(Color.BLACK)
            .setLinkFontColor(Color.RED)
            .showLinkUnderline(true)
            .setOnLinkClickCallback(new OnLinkClickCallback() {
                @Override
                public void onLinkClicked(View view, String link) {
                }
            })
            .setRxMDImageLoader(new DefaultLoader(this))
            .setDefaultImageSize(100, 100)
            .build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_note_edit, menu);
        optionsMenu = menu;
        checkUndoRedo();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.undo_note:
                if (mTextUndoRedo.canUndo()) {
                    mTextUndoRedo.exeUndo();
                }
                break;
            case R.id.redo_note:
                if (mTextUndoRedo.canRedo()) {
                    mTextUndoRedo.exeRedo();
                }
                break;
            case R.id.save_note:
                saveNote(false);
                break;

            case R.id.preview_note:
                Intent intent = new Intent(EditorActivity.this, ViewNote.class);
                intent.putExtra("note", note.toBundle());
                intent.putExtra("from_editor", true);
                //finish();
                startActivity(intent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void finishEditor(boolean modified) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("modified", modified);
        returnIntent.putExtra("note", note.toBundle());
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(!onBackPressed) saveNote(true);
    }

    @Override
    public void onBackPressed() {
        if (note != null && note.wasModified()) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.dialog_save_note_title)
                    .setMessage(R.string.dialog_save_note_message)
                    .setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            saveNote(true);
                        }
                    })
                    .setNegativeButton(R.string.dialog_no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            onBackPressed = true;
                            finishEditor(noteModified);
                        }
                    })
                    .show();
        } else {
            onBackPressed = true;
            //super.onBackPressed();
            finishEditor(noteModified);
        }
    }
}
