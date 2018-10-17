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
import android.widget.LinearLayout;
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

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
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

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            library_list = intent.getParcelableArrayListExtra("buckets");
            if (mDrawerToggle != null) mDrawerToggle.setDrawerIndicatorEnabled(true);
            if (library_list != null && library_list.size() > 0) Library.saveToFile(MainActivity.this, library_list);

            Library.serviceFinished();
            NotebookFragment notebookFragment = getNotebookFragment();
            if (notebookFragment != null && notebookFragment.isVisible()) {
                notebookFragment.reloadLibrary(library_list);
            } else {
                createNotebookFragment();
            }

            refreshRackDrawer();
            hideLoading();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        library_list = new ArrayList<>();

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
        mDrawerToggle.setDrawerIndicatorEnabled(false);
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
        return (NotebookFragment) getSupportFragmentManager().findFragmentByTag(Constants.NOTEBOOK_FRAGMENT_TAG);
    }

    private void selectItem(int position) {
        try {
            Log.i("log", "select "+position);
            NotebookFragment notebookFragment = getNotebookFragment();
            if (notebookFragment != null && notebookFragment.isVisible()) {
                notebookFragment.openBucket(mRackAdapter.getItem(position));
            } else {
                path.resetPath();
                createNotebookFragment();
            }

            mDrawerLayout.closeDrawer(mDrawerNavigation);
        } catch (NullPointerException e) {
            Log.e("err", "null item");
        }
    }

    public void loadNewBucket(String name) {
        MainModel new_rack = path.createRack(name);
        if (new_rack == null) {
            Toast.makeText(this, "Couldn't create new bucket", Toast.LENGTH_LONG).show();
            return;
        }
        library_list.add(new_rack);
        refreshRackDrawer();
        NotebookFragment notebookFragment = getNotebookFragment();
        if (notebookFragment != null && notebookFragment.isVisible()) {
            notebookFragment.refreshNotebooks();
        }
    }

    public void loadNewFolder(MainModel current_folder, String name) {
        MainModel new_folder = path.createNotebook(current_folder.getPath(), name);
        if (new_folder == null) {
            Toast.makeText(this, "Couldn't create new folder", Toast.LENGTH_LONG).show();
            return;
        }

        current_folder.addContent(new_folder);
        NotebookFragment notebookFragment = (NotebookFragment) getSupportFragmentManager().findFragmentByTag("Notebook_Fragment");
        if (notebookFragment != null && notebookFragment.isVisible()) {
            notebookFragment.refreshNotebooks();
        }
    }

    private boolean createNotebookFragment() {
        Fragment fragment = NotebookFragment.newInstance(library_list);
        if (fragment != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.content_frame, fragment, Constants.NOTEBOOK_FRAGMENT_TAG);
            transaction.addToBackStack(Constants.NOTEBOOK_FRAGMENT_TAG);
            transaction.commit();
            return true;
        } else {
            return false;
        }
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
            createNotebookFragment();
            showLoading();
            Library.launchService(this, path);
        } else {
            //library_list = Library.readFromFile(this);
            if (library_list != null) {
                createNotebookFragment();
            }
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
        } else if (id == R.id.action_reload) {
            if (library_list != null) library_list.clear();
            NotebookFragment notebookFragment = getNotebookFragment();
            if (notebookFragment != null && notebookFragment.isVisible()) {
                getSupportFragmentManager().popBackStack();
            }
            showLoading();
            Library.launchService(this, path);
            return true;
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

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            this.mDrawerLayout.closeDrawer(GravityCompat.START, true);
        } else {
            NotebookFragment notebookFragment = getNotebookFragment();
            if (notebookFragment != null && notebookFragment.isVisible()) {
                if (!notebookFragment.moveBack()) {
                    askAndClose();
                }
            } else {
                askAndClose();
            }
        }
    }

    void askAndClose() {
        new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle("Closing Epiphany")
            .setMessage("Are you sure you want to quit?")
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            })
            .setNegativeButton("No", null)
            .show();
    }

    public void reloadAndOpenFolder(final MainModel bucket) {
        if (bucket == null) return;
        bucket.clearContent();
        bucket.loadContent(this, new MainModel.OnModelLoadedListener() {
            @Override
            public void ModelLoaded() {
                Log.i("log", "model loaded");
                NotebookFragment notebookFragment = getNotebookFragment();
                if (notebookFragment != null && notebookFragment.isVisible()) {
                    if (bucket instanceof SingleRack) {
                        notebookFragment.openBucket(bucket);
                    } else {
                        notebookFragment.refreshNotebooks(bucket);
                    }
                }
            }
        });
    }

    public void openQuickNotesBucket() {
        MainModel quickBucket = Library.getQuickNotesBucket(library_list);
        reloadAndOpenFolder(quickBucket);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        super.onActivityResult(requestCode, resultCode, resultIntent);
        if (requestCode == Constants.NEW_QUICK_NOTE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                openQuickNotesBucket();
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


