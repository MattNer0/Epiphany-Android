package com.neromatt.epiphany.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.helper.IttyBitty;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.DataObjects.SingleNote;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import io.github.kobakei.materialfabspeeddial.FabSpeedDial;
import io.github.kobakei.materialfabspeeddial.FabSpeedDialMenu;

public class ViewNote extends AppCompatActivity {

    private SingleNote note;
    private boolean from_editor;

    private String html_body;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_note);

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

        FabSpeedDial noteFab = findViewById(R.id.noteviewFab);
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

        noteFab.setMenu(menu);

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
