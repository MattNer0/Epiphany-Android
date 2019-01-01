package com.neromatt.epiphany.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.webkit.WebView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.neromatt.epiphany.helper.Database;
import com.neromatt.epiphany.helper.IttyBitty;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.DataObjects.SingleNote;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import ru.whalemare.sheetmenu.SheetMenu;

public class ViewNote extends AppCompatActivity {

    private Database db;

    private Toolbar toolbar;
    private MarkedView markdownView;

    private SingleNote note;
    private boolean from_editor;

    private String html_body;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_note);

        db = new Database(getApplicationContext());

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

        if (!note.wasLoaded()) {
            note.refreshContent(db, new SingleNote.OnNoteLoadedListener() {
                @Override
                public void NoteLoaded(SingleNote note) {
                    initializeUI();
                }
            });
            return;
        }

        initializeUI();
    }

    private void initializeUI() {
        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setTitle(note.getTitle());

        markdownView = findViewById(R.id.markdownView);
        markdownView
                .setCheckboxCallback(new MarkedView.OnCheckboxChangedListener() {
                    @Override
                    public void CheckboxChange(String note_body, int num, boolean checked) {
                        note.updateBody(note_body);
                    }
                })
                .setHtmlBodyCallback(new MarkedView.OnHTMLBodyListener() {
                    @Override
                    public void HTMLBody(String note_body) {
                        html_body = note_body;
                    }
                })
                .setImageClickCallback(new MarkedView.OnImageClickListener() {
                    @Override
                    public void ImageClick(String image_url) {
                        if (image_url == null) return;
                        Intent intent = new Intent(ViewNote.this, ViewPhoto.class);
                        intent.putExtra("image", image_url);
                        startActivity(intent);
                    }
                })
                .setNoteImagePath(note.getImageFolderPath())
                .setMDText(note.getMarkdown());

        registerForContextMenu(markdownView);

        /*FabSpeedDial noteFab = findViewById(R.id.noteviewFab);

        noteFab.removeAllOnMenuItemClickListeners();
        noteFab.addOnMenuItemClickListener(new FabSpeedDial.OnMenuItemClickListener() {
            @Override
            public void onMenuItemClick(FloatingActionButton miniFab, @Nullable TextView label, int itemId) {
                if (label == null) return;
                switch (itemId) {
                    case Constants.FAB_MENU_EDIT_NOTE:
                        editNote();
                        break;
                    case Constants.FAB_MENU_INFO_NOTE:
                        showInfo();
                        break;
                    case Constants.FAB_MENU_SHARE_NOTE:
                        shareNote();
                        break;
                    case Constants.FAB_MENU_OPEN_LINK_NOTE:
                        String web = note.getMetaString(Constants.METATAG_WEB);
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(web));
                        startActivity(browserIntent);
                        break;
                }
            }
        });

        FabSpeedDialMenu menu = new FabSpeedDialMenu(this);
        menu.add(1, Constants.FAB_MENU_EDIT_NOTE, 1, R.string.fab_edit_note).setIcon(R.drawable.ic_mode_edit_white_24dp);
        menu.add(1, Constants.FAB_MENU_INFO_NOTE, 2, R.string.fab_info_note).setIcon(R.drawable.ic_info_outline_white_24dp);
        menu.add(1, Constants.FAB_MENU_SHARE_NOTE, 3, R.string.fab_share_note).setIcon(R.drawable.ic_share_white_24dp);

        String web = note.getMetaString(Constants.METATAG_WEB);
        if (web != null && Patterns.WEB_URL.matcher(web.toLowerCase()).matches()) {
            menu.add(1, Constants.FAB_MENU_OPEN_LINK_NOTE, 4, R.string.fab_open_link_note).setIcon(R.drawable.ic_open_in_browser_white_24dp);
        }

        noteFab.setMenu(menu);*/

        FloatingActionButton close_info = findViewById(R.id.close_info);
        close_info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideInfo();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_note_view, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.info_note:
                showInfo();
                break;
            case R.id.edit_note:
                editNote();
                break;
            case R.id.share_note:
                shareNote();
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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (v instanceof MarkedView) {
            MarkedView webView = (MarkedView) v;
            WebView.HitTestResult result = webView.getHitTestResult();

            if (result != null) {
                if (result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                    final String link = result.getExtra();
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            linkMenu(link);
                        }
                    });
                }
            }
        }
    }

    private void linkMenu(final String link) {
        SheetMenu.with(ViewNote.this)
                .setTitle(link)
                .setMenu(R.menu.popup_link_menu)
                .setAutoCancel(true)
                .setClick(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.link_copy:
                                setClipboard(ViewNote.this, note.getTitle(), link);
                                Toast.makeText(ViewNote.this, "Link copied to clipboard", Toast.LENGTH_LONG).show();
                                break;
                            case R.id.link_open:
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                                startActivity(browserIntent);
                                break;
                        }
                        return true;
                    }
                }).show();
    }

    private void setClipboard(Context context, String title, String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText(title, text);
        clipboard.setPrimaryClip(clip);
    }

    public void updateInfo() {
        TextView note_filename = findViewById(R.id.note_filename);
        TextView note_path = findViewById(R.id.note_path);
        TextView note_created_at = findViewById(R.id.note_created_at);
        TextView note_updated_at = findViewById(R.id.note_updated_at);
        TextView note_lines = findViewById(R.id.note_lines);

        note_filename.setText(note.getFilename());
        note_path.setText(note.getFullPath());

        note_created_at.setText(note.getCreatedString(MainModel.getCurrentLocale(this)));
        note_updated_at.setText(note.getLastModifiedString(MainModel.getCurrentLocale(this)));

        StringBuilder sb = new StringBuilder();
        sb.append(note.getLinesNumber());
        note_lines.setText(sb.toString());
    }

    public void showInfo() {
        RelativeLayout bInfo = findViewById(R.id.bottom_info);
        //FabSpeedDial noteFab = findViewById(R.id.noteviewFab);

        updateInfo();

        //noteFab.hide();
        slideUp(bInfo);
    }

    public void hideInfo() {
        RelativeLayout bInfo = findViewById(R.id.bottom_info);
        //FabSpeedDial noteFab = findViewById(R.id.noteviewFab);

        //noteFab.show();
        slideDown(bInfo);
    }

    public void shareNote() {
        if (html_body == null || html_body.isEmpty()) {
            Toast.makeText(this, "Can't share note!", Toast.LENGTH_LONG).show();
            return;
        }

        IttyBitty.createLink(html_body, note.getFileNameNoExtension(), new IttyBitty.OnLinkCreated() {
            @Override
            public void LinkCreated(String url) {
                if (url == null) return;
                Intent share = new Intent(android.content.Intent.ACTION_SEND);
                share.setType("text/plain");
                share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                share.putExtra(Intent.EXTRA_SUBJECT, note.getName());
                share.putExtra(Intent.EXTRA_TEXT, url);
                startActivity(Intent.createChooser(share, "Share link!"));
            }
        });
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
            0,
            0,
            view.getHeight(),
            0);
        animate.setDuration(500);
        animate.setFillAfter(true);
        view.startAnimation(animate);
    }

    public void slideDown(View view){
        TranslateAnimation animate = new TranslateAnimation(
            0,
            0,
            0,
            view.getHeight());
        animate.setDuration(500);
        animate.setFillAfter(true);
        view.startAnimation(animate);
    }
}
