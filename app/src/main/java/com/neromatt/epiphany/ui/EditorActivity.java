package com.neromatt.epiphany.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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

public class EditorActivity extends AppCompatActivity {

    private String folderpath;
    private String noteToEdit;
    private Toolbar toolbar;
    private Path path;
    private RxMDEditText editTextField;
    private SingleNote note = null;
    private TextUndoRedo mTextUndoRedo;
    private boolean onBackPressed;
    private boolean noteModified = false;
    private Menu optionsMenu = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_note);
        setupToolBar();
        Intent intent = getIntent();
        Bundle notebundle = intent.getBundleExtra("note");
        String folder = intent.getStringExtra("folder");
        if (notebundle != null) {
            note = new SingleNote(notebundle);
        } else if (folder != null && !folder.isEmpty()) {
            path = new Path(folder);

            if (path.isDirectory()) {
                noteToEdit = path.newNoteName();
                folderpath = folder;
            } else {
                noteToEdit = path.getName();
                folderpath = path.getNotePath();
                path.setCurrentPath(folderpath);
            }
            note = new SingleNote(folderpath, noteToEdit);
            note.markAsNewFile();

            String note_body = intent.getStringExtra("body");
            if (note_body != null && !note_body.isEmpty()) {
                note.updateBody(note_body);
            }
        }

        editTextField = findViewById(R.id.edit_text);
        editTextField.setText("");

        RxMarkdown.live(editTextField)
                .config(getMarkdownEditorConfiguration())
                .factory(EditFactory.create())
                .intoObservable()
                .subscribe();

        onBackPressed = false;
        if (note.doesExist() || notebundle != null) {
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

        /*FabSpeedDial noteFab = findViewById(R.id.noteeditFab);
        noteFab.addOnMenuItemClickListener(new FabSpeedDial.OnMenuItemClickListener() {
            @Override
            public void onMenuItemClick(FloatingActionButton miniFab, @Nullable TextView label, int itemId) {
                if (itemId == R.id.preview_note) {
                    saveNote();

                    Intent intent = new Intent(EditorActivity.this, ViewNote.class);
                    intent.putExtra("note", note.toBundle());
                    startActivity(intent);

                    finish();
                }
            }
        });*/
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        checkUndoRedo();
    }

    private void setupToolBar() {
        toolbar = findViewById(R.id.toolbar); // Attaching the layout to the toolbar object
        if (toolbar!=null) {
            setSupportActionBar(toolbar);    // Setting toolbar as the ActionBar with setSupportActionBar() call
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    private void saveNote(final boolean quit) {
        if (note.wasModified()) {
            String note_text = editTextField.getText().toString();
            if (note_text.isEmpty() && (note.getMarkdown() == null || note.getMarkdown().isEmpty()))
                return;

            note.updateBody(note_text);
            if (!quit) {
                noteModified = true;
                note.saveNote(new SingleNote.OnNoteSavedListener() {
                    @Override
                    public void NoteSaved(boolean saved) {
                        if (saved) {
                            Toast.makeText(EditorActivity.this, "Note saved", Toast.LENGTH_LONG).show();
                            if (quit) {
                                /*Intent result = getIntent();
                                result.putExtra("RESULT_STRING", note.getFullPath());
                                setResult(Activity.RESULT_OK, result);
                                finish();*/
                                finishEditor(true);
                            }
                        } else {
                            Toast.makeText(EditorActivity.this, "Note couldn't be saved", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }

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
        if (note.wasModified()) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Epiphany")
                    .setMessage("Do you want to save before exiting?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            saveNote(true);
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
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
