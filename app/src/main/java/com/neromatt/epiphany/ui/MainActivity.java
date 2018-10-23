package com.neromatt.epiphany.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.DataObjects.SingleNote;
import com.neromatt.epiphany.model.DataObjects.SingleNotebook;
import com.neromatt.epiphany.model.DataObjects.SingleRack;
import com.neromatt.epiphany.model.Library;
import com.neromatt.epiphany.model.NotebooksComparator;
import com.neromatt.epiphany.model.Path;
import com.neromatt.epiphany.model.Adapters.RackAdapter;
import com.neromatt.epiphany.ui.NotebookFragment.CreateNoteHelper;
import com.neromatt.epiphany.ui.NotebookFragment.NotebookFragment;
import com.sensorberg.permissionbitte.BitteBitte;
import com.sensorberg.permissionbitte.PermissionBitte;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity implements BitteBitte, PathSupplier, CreateNoteHelper.OnQuickPathListener {
    private Toolbar toolbar;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private ConstraintLayout mDrawerNavigation;
    private RecyclerView mDrawerList;
    private RackAdapter mRackAdapter;
    private Path path;
    private ArrayList<MainModel> library_list;

    private MenuItem action_list_layout;
    private MenuItem action_staggered_layout;

    private MainModel moving_note_folder;
    private SingleNote moving_note;

    private LinkedList<MainModel> bucket_queue;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            library_list = intent.getParcelableArrayListExtra("buckets");
            if (library_list != null && library_list.size() > 0) Library.saveToFile(MainActivity.this, library_list);

            //Log.i(Constants.LOG, "onReceive");

            Library.serviceFinished();
            createInitialFragment();
            refreshRackDrawer();
            hideLoading();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        library_list = new ArrayList<>();
        bucket_queue = new LinkedList<>();

        mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerNavigation = findViewById(R.id.left_drawer);
        mDrawerList = findViewById(R.id.left_drawer_list);

        setupToolBar();
        setupDrawerToggle();

        if (PermissionBitte.shouldAsk(this, this)) {
            PermissionBitte.ask(MainActivity.this, MainActivity.this);
        } else {
            this.yesYouCan();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(broadcastReceiver, new IntentFilter(Constants.BROADCAST_FILTER));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(broadcastReceiver);
    }

    public void refreshRackDrawer() {
        try {
            if (mRackAdapter == null) {
                mRackAdapter = new RackAdapter(this, library_list);
                mRackAdapter.setOnClickListener(new RackAdapter.OnRackClickListener() {
                    @Override
                    public void RackClicked(int position) {
                        selectItem(position);
                    }
                });
                mDrawerList.setLayoutManager(new LinearLayoutManager(this));
                mDrawerList.setAdapter(mRackAdapter);
            } else {
                mRackAdapter.updateData(library_list);
            }
        } catch(NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    private void setupToolBar() {
        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    void setupDrawerToggle() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.app_name, R.string.app_name);
        mDrawerToggle.syncState();
        mDrawerToggle.setDrawerIndicatorEnabled(true);
    }

    void hideLoading() {
        RelativeLayout loading = findViewById(R.id.container_loading);
        FrameLayout content = findViewById(R.id.content_frame);
        content.setVisibility(View.VISIBLE);
        loading.setVisibility(View.GONE);
    }

    void showLoading() {
        RelativeLayout loading = findViewById(R.id.container_loading);
        FrameLayout content = findViewById(R.id.content_frame);
        content.setVisibility(View.GONE);
        loading.setVisibility(View.VISIBLE);
    }

    private NotebookFragment getNotebookFragment() {
        FragmentManager fm = getSupportFragmentManager();
        String fragmentTag = fm.getBackStackEntryAt(getSupportFragmentManager().getBackStackEntryCount() - 1).getName();
        return (NotebookFragment) fm.findFragmentByTag(fragmentTag);
    }

    private void selectItem(int position) {
        try {
            pushFragment(mRackAdapter.getItem(position));
        } catch (NullPointerException e) {
            Log.e("err", "null item");
        }
        mDrawerLayout.closeDrawer(mDrawerNavigation);
    }

    public void loadNewBucket(String name) {
        MainModel new_rack = path.createRack(name);
        if (new_rack == null) {
            Toast.makeText(this, "Couldn't create new bucket", Toast.LENGTH_LONG).show();
            return;
        }
        library_list.add(new_rack);
        refreshRackDrawer();
    }

    public void loadNewFolder(MainModel current_folder, String name) {
        MainModel new_folder = path.createNotebook(current_folder.getPath(), name);
        if (new_folder == null) {
            Toast.makeText(this, "Couldn't create new folder", Toast.LENGTH_LONG).show();
            return;
        }

        current_folder.addContent(new_folder);
    }

    private boolean createInitialFragment() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        return pushFragment(NotebookFragment.newInstance(library_list), Constants.NOTEBOOK_FRAGMENT_TAG);
    }

    private void showSettings() {
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingsIntent);
    }

    boolean checkIfFirstRun() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String path = PreferenceManager.getDefaultSharedPreferences(this).getString("pref_root_directory", "");
        if (prefs.getBoolean("firstrun", true) || path.isEmpty() || !(new File(path)).exists()) {
            File rootDir = Environment.getExternalStorageDirectory();
            String full = rootDir + "/epiphany";
            File fullpath = new File(full);
            if (!(fullpath.exists() && fullpath.isDirectory())) {
                fullpath.mkdirs();
            }

            SharedPreferences.Editor prefs_edit = prefs.edit();

            prefs_edit.putString("pref_root_directory", full);
            prefs_edit.putBoolean("firstrun", false);
            prefs_edit.putString("pref_note_order", "0");
            prefs_edit.apply();

            this.path = new Path(full);
            return true;
        }

        this.path = new Path(path);
        return false;
    }

    @Override
    public void yesYouCan() {
        if (checkIfFirstRun()) {
            showLoading();
            Library.launchService(this, path);
        } else {
            showLoading();
            Library.launchService(this, path);
        }
    }

    @Override
    public void noYouCant() {
        Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void askNicer() {
        new AlertDialog.Builder(this)
            .setTitle("Storage")
            .setMessage("Epiphany needs to access your storage to read and save notes")
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    PermissionBitte.ask(MainActivity.this, MainActivity.this);
                }
            })
            .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    MainActivity.this.finish();
                }
            })
            .setCancelable(false)
            .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        action_list_layout = menu.findItem(R.id.action_list_layout);
        action_staggered_layout = menu.findItem(R.id.action_staggered_layout);

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_staggered_layout", false)) {
            action_list_layout.setVisible(true);
            action_staggered_layout.setVisible(false);
        } else {
            action_staggered_layout.setVisible(true);
            action_list_layout.setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            showSettings();
            return true;
        } else if (id == R.id.action_quick_notes) {
            openQuickNotesBucket();
        /*} else if (id == R.id.action_reload) {
            if (library_list != null) library_list.clear();
            NotebookFragment notebookFragment = getNotebookFragment();
            if (notebookFragment != null && notebookFragment.isVisible()) {
                getSupportFragmentManager().popBackStack();
            }
            showLoading();
            Library.launchService(this, path);
            return true;*/
        } else if (id == R.id.action_list_layout) {
            if (action_list_layout != null && action_staggered_layout != null) {
                action_staggered_layout.setVisible(true);
                action_list_layout.setVisible(false);
            }
            updateLayoutNotebookFragment(false);
            return true;
        } else if (id == R.id.action_staggered_layout) {
            if (action_list_layout != null && action_staggered_layout != null) {
                action_list_layout.setVisible(true);
                action_staggered_layout.setVisible(false);
            }
            updateLayoutNotebookFragment(true);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateLayoutNotebookFragment(boolean staggered) {
        NotebookFragment notebookFragment = getNotebookFragment();
        if (notebookFragment != null && notebookFragment.isVisible()) {
            notebookFragment.updateLayoutList(staggered);
        }
    }

    public boolean pushFragment(Fragment fragment, String tag) {
        if (fragment != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.animator.enter_from_right, R.animator.exit_to_left, R.animator.enter_from_left, R.animator.exit_to_right);
            ft.replace(R.id.content_frame, fragment, tag);
            ft.addToBackStack(tag);
            ft.commitAllowingStateLoss();
            return true;
        }
        return false;
    }

    public boolean pushFragment(MainModel model) {
        if (model instanceof SingleRack) {

            newBucketInQueue(model);
            for (MainModel m: model.getContent()) {
                m.loadNotes(this, new MainModel.OnModelLoadedListener() {
                    @Override
                    public void ModelLoaded() {
                        Log.i(Constants.LOG, "loaded notes");
                    }
                });
            }

            if (model.isQuickNotes()) {
                MainModel quick_note_folder = model.getFirstFolder();
                return pushFragment(NotebookFragment.newInstance(quick_note_folder), Constants.NOTEBOOK_FOLDER_FRAGMENT_TAG);
            }

            return pushFragment(NotebookFragment.newInstance(model), Constants.NOTEBOOK_BUCKET_FRAGMENT_TAG);
        } else if (model instanceof SingleNotebook) {
            return pushFragment(NotebookFragment.newInstance(model), Constants.NOTEBOOK_FOLDER_FRAGMENT_TAG);
        }
        return false;
    }

    private void newBucketInQueue(MainModel model) {
        if (bucket_queue == null) bucket_queue = new LinkedList<>();
        bucket_queue.add(model);

        if (bucket_queue.size() > 2) {
            MainModel old_bucket = bucket_queue.removeFirst();
            if (!old_bucket.getPath().equals(model.getPath())) {
                old_bucket.unloadNotes(new MainModel.OnModelLoadedListener() {
                    @Override
                    public void ModelLoaded() {
                        Log.i(Constants.LOG, "unload Notes");
                    }
                });
            }
        }
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();

        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            this.mDrawerLayout.closeDrawer(GravityCompat.START, true);
        } else if (fm.getBackStackEntryCount() > 1) {
            fm.popBackStack();
        } else {
            String fragmentTag = fm.getBackStackEntryAt(getSupportFragmentManager().getBackStackEntryCount() - 1).getName();
            if (!fragmentTag.equals(Constants.NOTEBOOK_FRAGMENT_TAG)) {
                fm.popBackStack();
                pushFragment(NotebookFragment.newInstance(library_list), Constants.NOTEBOOK_FRAGMENT_TAG);
                return;
            }
            askAndClose();
        }
    }

    void askAndClose() {
        new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.dialog_close_title)
            .setMessage(R.string.dialog_close_message)
            .setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            })
            .setNegativeButton(R.string.dialog_no, null)
            .show();
    }

    public void reloadAndOpenFolder(final MainModel bucket, final boolean pop_fragment) {
        if (bucket == null) {
            showLoading();
            getSupportFragmentManager().popBackStack();
            Library.launchService(this, path);
            return;
        }
        bucket.clearContent();
        bucket.loadContent(this, new MainModel.OnModelLoadedListener() {
            @Override
            public void ModelLoaded() {
                if (pop_fragment) getSupportFragmentManager().popBackStack();
                pushFragment(bucket);
            }
        });
    }

    public void openQuickNotesBucket() {
        MainModel quickBucket = Library.getQuickNotesBucket(library_list);
        reloadAndOpenFolder(quickBucket, false);
    }

    public void setMovingNote(SingleNote n, MainModel f) {
        this.moving_note = n;
        this.moving_note_folder = f;
    }

    public boolean sameFolderAsMovingNote(MainModel f) {
        return moving_note_folder.getPath().equals(f.getPath());
    }

    public SingleNote getMovingNote() {
        return moving_note;
    }

    public MainModel getMovingNoteFolder() {
        return moving_note_folder;
    }

    public void clearMovingNote() {
        moving_note = null;
        moving_note_folder = null;
    }

    public boolean isMovingNote() {
        return moving_note != null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        super.onActivityResult(requestCode, resultCode, resultIntent);
        if (requestCode == Constants.NEW_QUICK_NOTE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (resultIntent.getBooleanExtra("modified", false)) {
                    openQuickNotesBucket();
                }
            }
        } else if (requestCode == Constants.NEW_NOTE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (resultIntent.getBooleanExtra("modified", false)) {
                    Bundle bundle_note = resultIntent.getBundleExtra("note");
                    if (bundle_note != null) {
                        NotebookFragment notebookFragment = (NotebookFragment) getSupportFragmentManager().findFragmentByTag("Notebook_Fragment");
                        if (notebookFragment != null && notebookFragment.isVisible()) {
                            SingleNote new_note = new SingleNote(bundle_note);
                            notebookFragment.addNewNoteToCurrent(new_note);
                        }
                    }
                }
            }
        }
    }

    public Path getPath() {
        return this.path;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("rootPath", this.path.getRootPath());
        outState.putString("currentPath", this.path.getCurrentPath());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (this.path == null) {
            this.path = new Path(savedInstanceState.getString("rootPath"));
        } else {
            this.path.setRootPath(savedInstanceState.getString("rootPath"));
        }
        this.path.setCurrentPath(savedInstanceState.getString("currentPath"));

        if (this.library_list.size() == 0) {
            Library.launchService(this, path);
        }
    }

    @Override
    public void QuickPathCreated(Bundle quick_bundle) {
        String folder_path = quick_bundle.getString("path", "");
        Boolean created_folder = quick_bundle.getBoolean("created_folder", false);
        Boolean created_bucket = quick_bundle.getBoolean("created_bucket", false);

        if (created_folder) {
            File f = new File(folder_path);
            SingleNotebook quick_note_folder = new SingleNotebook(f.getName(), f.toString(), null);
            quick_note_folder.setQuickNotesFolder();

            if (created_bucket) {
                SingleRack quick_note_bucket = new SingleRack(Constants.QUICK_NOTES_BUCKET, path.getRootPath()+"/"+Constants.QUICK_NOTES_BUCKET, null);
                quick_note_bucket.addContent(quick_note_folder);

                library_list.add(quick_note_bucket);
                Collections.sort(library_list, new NotebooksComparator(this));

            } else {

                for (MainModel m : library_list) {
                    if (m.isBucket() && m.getName().equals(Constants.QUICK_NOTES_BUCKET)) {
                        m.addContent(quick_note_folder);
                        break;
                    }
                }

            }
        }
    }
}


