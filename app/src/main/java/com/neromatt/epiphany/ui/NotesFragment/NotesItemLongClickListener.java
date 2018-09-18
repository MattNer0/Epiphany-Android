package com.neromatt.epiphany.ui.NotesFragment;

import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.neromatt.epiphany.model.Adapters.NotebookAdapter;
import com.neromatt.epiphany.model.Adapters.NotesAdapter;
import com.neromatt.epiphany.model.DataObjects.SingleNote;
import com.neromatt.epiphany.model.DataObjects.SingleNotebook;
import com.neromatt.epiphany.model.Path;
import com.neromatt.epiphany.ui.FileDialog;
import com.neromatt.epiphany.ui.R;

import java.io.File;

public class NotesItemLongClickListener implements View.OnCreateContextMenuListener {

    private ListView notesList;
    private Path path;
    private Fragment mCallback;

    public NotesItemLongClickListener(Fragment fragment,ListView notesList, Path path){
        this.notesList = notesList;
        this.path = path;
        mCallback = fragment;
    }
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo contextMenuInfo) {

        if (v.getId()==R.id.notesview) {
            menu.add(Menu.NONE, 0, 0, R.string.move_note).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onContextItemSelected(item);
                }
            });
            menu.add(Menu.NONE, 1, 1, R.string.delete_note).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onContextItemSelected(item);
                }
            });
        }

    }
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        int menuItemIndex = item.getItemId();

        if(menuItemIndex == 0){
            FileDialog moveNoteDialog = new FileDialog(mCallback.getActivity(), new File(path.getCurrentPath()));
            moveNoteDialog.setSelectDirectoryOption(true);
            moveNoteDialog.addDirectoryListener(new FileDialog.DirectorySelectedListener() {
                public void directorySelected(File directory) {
                    moveNote(info.position, directory);
                }
            });
            moveNoteDialog.showDialog();

        }
        if(menuItemIndex == 1){
            deleteNote(info.position);
        }
        return true;
    }
    public void moveNote(int position, File directory){

        NotesAdapter na = (NotesAdapter)notesList.getAdapter();
        final String oldNoteName=((SingleNote)na.getItem(position)).getName();

        na.remove(na.getItem(position));
        na.notifyDataSetChanged();

        path.moveFile(directory.getPath(),oldNoteName);
    }
    public void deleteNote(int position){
        NotesAdapter na = (NotesAdapter)notesList.getAdapter();
        String path =((SingleNote)na.getItem(position)).getFullPath();
        this.path.deleteNote(path);
        na.remove(na.getItem(position));
        na.notifyDataSetChanged();
    }
}
