//package com.neromatt.epiphany.ui;
//
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.content.SharedPreferences;
//import android.os.Bundle;
//import android.os.Environment;
//import android.preference.PreferenceManager;
//import android.util.Log;
//import android.view.Menu;
//import android.view.MenuItem;
//import android.widget.Toast;
//
//import com.neromatt.epiphany.Constants;
//import com.neromatt.epiphany.helper.DBInterface;
//import com.neromatt.epiphany.helper.Database;
//import com.neromatt.epiphany.tasks.ReadDatabaseTask;
//import com.neromatt.epiphany.model.DataObjects.MainModel;
//import com.neromatt.epiphany.model.DataObjects.SingleNote;
//import com.neromatt.epiphany.model.DataObjects.SingleNotebook;
//import com.neromatt.epiphany.model.DataObjects.SingleRack;
//import com.neromatt.epiphany.model.Library;
//import com.neromatt.epiphany.model.NotebooksComparator;
//import com.neromatt.epiphany.model.Path;
//import com.neromatt.epiphany.model.Adapters.RackAdapter;
//import com.neromatt.epiphany.ui.Fragments.BucketsFragment;
//import com.neromatt.epiphany.helper.CreateNoteHelper;
//import com.neromatt.epiphany.ui.NotebookFragment.NotebookFragment;
//import com.sensorberg.permissionbitte.BitteBitte;
//import com.sensorberg.permissionbitte.PermissionBitte;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.LinkedList;
//
//import androidx.appcompat.app.AlertDialog;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.fragment.app.Fragment;
//import androidx.fragment.app.FragmentManager;
//import androidx.fragment.app.FragmentTransaction;
//import androidx.localbroadcastmanager.content.LocalBroadcastManager;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//public class MainActivityB extends AppCompatActivity implements BitteBitte, CreateNoteHelper.OnQuickPathListener, DBInterface {
//
//    private Database db;
//
//    private RecyclerView mDrawerList;
//    private RackAdapter mRackAdapter;
//    private Path path;
//    private ArrayList<MainModel> library_list;
//
//    private MenuItem action_list_layout;
//    private MenuItem action_staggered_layout;
//    private MenuItem action_searchView;
//
//    private String search_query;
//    private boolean search_opened;
//
//    private MainModel moving_note_folder;
//    private SingleNote moving_note;
//
//    private LinkedList<MainModel> bucket_queue;
//
//    private boolean initial_fragment_created = false;
//
//    /*private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            Library.serviceFinished();
//            if (intent.hasExtra("buckets")) {
//                library_list = intent.getParcelableArrayListExtra("buckets");
//                refreshRackDrawer();
//                if (!createInitialFragment(library_list)) {
//                    updateBuckets(library_list);
//                }
//            } else if (intent.hasExtra("folders")) {
//                ArrayList<MainModel> list = intent.getParcelableArrayListExtra("folders");
//                for (MainModel content: list) {
//                    content.dbLoadNotes(db, MainActivity.this, new MainModel.OnModelLoadedListener() {
//                        @Override
//                        public void ModelLoaded() {
//
//                        }
//                    });
//                }
//
//                updateBucketByUUID(intent.getStringExtra("uuid"), list);
//            } else if (intent.hasExtra("request")) {
//                Bundle extras = intent.getExtras();
//                Library.serviceRequestEnum request = (Library.serviceRequestEnum) extras.getSerializable("request");
//                if (request == Library.serviceRequestEnum.NOTES) {
//                    //Toast.makeText(MainActivity.this, "DB cache saved", Toast.LENGTH_LONG).show();
//                    reloadCurrentFromDB(intent.getStringExtra("uuid"));
//                }
//            }
//        }
//    };*/
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        db = new Database(getApplicationContext());
//
//        library_list = new ArrayList<>();
//        bucket_queue = new LinkedList<>();
//
//        mDrawerList = findViewById(R.id.left_drawer_list);
//        initial_fragment_created = false;
//
//        if (PermissionBitte.shouldAsk(this, this)) {
//            PermissionBitte.ask(MainActivity.this, MainActivity.this);
//        } else {
//            this.yesYouCan();
//        }
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        //LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(broadcastReceiver, new IntentFilter(Constants.BROADCAST_FILTER));
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        //LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(broadcastReceiver);
//    }
//
//    @Override
//    public Database getDatabase() {
//        return db;
//    }
//
//    @Override
//    public Context getContext() {
//        return MainActivity.this;
//    }
//
//    public void refreshRackDrawer() {
//        try {
//            if (mRackAdapter == null) {
//                mRackAdapter = new RackAdapter(this, library_list);
//                mRackAdapter.setOnClickListener(new RackAdapter.OnRackClickListener() {
//                    @Override
//                    public void RackClicked(int position) {
//                        selectItem(position);
//                    }
//                });
//                mDrawerList.setLayoutManager(new LinearLayoutManager(this));
//                mDrawerList.setAdapter(mRackAdapter);
//            } else {
//                mRackAdapter.updateData(library_list);
//            }
//        } catch(NullPointerException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private String getLastFragmentTag() {
//        FragmentManager fm = getSupportFragmentManager();
//        if (fm.getBackStackEntryCount() == 0) {
//            return null;
//        }
//        return fm.getBackStackEntryAt(getSupportFragmentManager().getBackStackEntryCount() - 1).getName();
//    }
//
//    private NotebookFragment getNotebookFragment() {
//        FragmentManager fm = getSupportFragmentManager();
//        if (fm.getBackStackEntryCount() == 0) {
//            return null;
//        }
//        String fragmentTag = fm.getBackStackEntryAt(getSupportFragmentManager().getBackStackEntryCount() - 1).getName();
//        return (NotebookFragment) fm.findFragmentByTag(fragmentTag);
//    }
//
//    private void selectItem(int position) {
//        NotebookFragment nf = getNotebookFragment();
//        try {
//            if (nf != null) nf.getNavigationLayout().closeDrawerIfOpen();
//            MainModel selected_model = mRackAdapter.getItem(position);
//            if (nf != null && nf.isCurrentModel(selected_model)) {
//                return;
//            }
//            pushFragment(selected_model);
//        } catch (NullPointerException e) {
//            Log.e("err", "null item");
//        }
//    }
//
//    public void loadNewBucket(String name) {
//        MainModel new_rack = path.createRack(name);
//        if (new_rack == null) {
//            Toast.makeText(this, "Couldn't create new bucket", Toast.LENGTH_LONG).show();
//            return;
//        }
//        library_list.add(new_rack);
//        refreshRackDrawer();
//    }
//
//    public void loadNewFolder(MainModel current_folder, String name) {
//        MainModel new_folder = path.createNotebook(current_folder.getPath(), name);
//        if (new_folder == null) {
//            Toast.makeText(this, "Couldn't create new folder", Toast.LENGTH_LONG).show();
//            return;
//        }
//
//        current_folder.addContent(new_folder);
//        current_folder.sortContents(this);
//    }
//
//    private void createBucketsFragment() {
//        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//        ft.replace(R.id.content_frame, BucketsFragment.newInstance(path.getRootPath()), Constants.BUCKETS_FRAGMENT_TAG);
//        ft.addToBackStack(Constants.BUCKETS_FRAGMENT_TAG);
//        ft.commit();
//    }
//
//    private boolean createInitialFragment(ArrayList<MainModel> library) {
//        if (initial_fragment_created) return false;
//
//        library_list = library;
//        FragmentManager fm = getSupportFragmentManager();
//        if (fm.getBackStackEntryCount() > 0) {
//            fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
//        }
//
//        if (pushFragment(NotebookFragment.newInstance(library_list), Constants.NOTEBOOK_FRAGMENT_TAG)) {
//            initial_fragment_created = true;
//            return true;
//        }
//
//        return false;
//    }
//
//    private void showSettings() {
//        Intent settingsIntent = new Intent(this, SettingsActivity.class);
//        startActivity(settingsIntent);
//    }
//
//    boolean checkIfFirstRun() {
//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//        String path = PreferenceManager.getDefaultSharedPreferences(this).getString("pref_root_directory", "");
//        if (prefs.getBoolean("firstrun", true) || path.isEmpty() || !(new File(path)).exists()) {
//            File rootDir = Environment.getExternalStorageDirectory();
//            String full = rootDir + "/epiphany";
//            File fullpath = new File(full);
//            if (!(fullpath.exists() && fullpath.isDirectory())) {
//                if (!fullpath.mkdirs()) {
//                    Toast.makeText(this, "Couldn't create initial directories!", Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            SharedPreferences.Editor prefs_edit = prefs.edit();
//
//            prefs_edit.putString("pref_root_directory", full);
//            prefs_edit.putBoolean("firstrun", false);
//            prefs_edit.putString("pref_note_order", "0");
//            prefs_edit.apply();
//
//            this.path = new Path(full);
//            return true;
//        }
//
//        this.path = new Path(path);
//        return false;
//    }
//
//    @Override
//    public void noYouCant() {
//        Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
//    }
//
//    @Override
//    public void askNicer() {
//        new AlertDialog.Builder(this)
//            .setTitle("Storage")
//            .setMessage("Epiphany needs to access your storage to read and save notes")
//            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    PermissionBitte.ask(MainActivity.this, MainActivity.this);
//                }
//            })
//            .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    MainActivity.this.finish();
//                }
//            })
//            .setCancelable(false)
//            .show();
//    }
//
//    @Override
//    public void yesYouCan() {
//        checkIfFirstRun();
//        createBucketsFragment();
//    }
//
//    /*private void launchServiceForBuckets(boolean first_run) {
//        createInitialFragment()
//
//        //Library.launchServiceForBuckets(this, path);
//    }*/
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        if (search_opened) return false;
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//
//        action_list_layout = menu.findItem(R.id.action_list_layout);
//        action_staggered_layout = menu.findItem(R.id.action_staggered_layout);
//
//        toggleLayoutList();
//
//        action_searchView = menu.findItem(R.id.action_search);
//
//        return true;
//    }
//
//    private void toggleLayoutList() {
//        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_staggered_layout", false)) {
//            action_list_layout.setVisible(true);
//            action_staggered_layout.setVisible(false);
//        } else {
//            action_staggered_layout.setVisible(true);
//            action_list_layout.setVisible(false);
//        }
//    }
//
//    public void searchBarOpened(boolean focus) {
//        action_staggered_layout.setVisible(false);
//        action_list_layout.setVisible(false);
//        action_searchView.setVisible(false);
//        if (!search_opened) {
//            search_opened = true;
//        }
//        invalidateOptionsMenu();
//
//        NotebookFragment nf = getNotebookFragment();
//        if (nf != null) nf.startSearch(search_query, focus);
//    }
//
//    public void searchBarClosed() {
//        toggleLayoutList();
//        action_searchView.setVisible(true);
//        search_opened = false;
//        invalidateOptionsMenu();
//
//        NotebookFragment nf = getNotebookFragment();
//        if (nf != null) nf.clearSearch();
//    }
//
//    public void setSearch(String query) {
//        this.search_query = query;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        int id = item.getItemId();
//        if (id == R.id.action_settings) {
//            showSettings();
//            return true;
//        } else if (id == R.id.action_quick_notes) {
//            openQuickNotesBucket();
//        } else if (id == R.id.action_list_layout) {
//            if (action_list_layout != null && action_staggered_layout != null) {
//                action_staggered_layout.setVisible(true);
//                action_list_layout.setVisible(false);
//            }
//            updateLayoutNotebookFragment(false);
//            return true;
//        } else if (id == R.id.action_staggered_layout) {
//            if (action_list_layout != null && action_staggered_layout != null) {
//                action_list_layout.setVisible(true);
//                action_staggered_layout.setVisible(false);
//            }
//            updateLayoutNotebookFragment(true);
//            return true;
//        } else if (id == R.id.action_search) {
//            if (search_opened) {
//                searchBarClosed();
//            } else {
//                searchBarOpened(true);
//            }
//        }
//
//        return super.onOptionsItemSelected(item);
//    }
//
//    private void updateLayoutNotebookFragment(boolean staggered) {
//        NotebookFragment notebookFragment = getNotebookFragment();
//        if (notebookFragment != null && notebookFragment.isVisible()) {
//            notebookFragment.updateLayoutList(staggered);
//        }
//
//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//        SharedPreferences.Editor prefs_edit = prefs.edit();
//
//        prefs_edit.putBoolean("pref_staggered_layout", staggered);
//        prefs_edit.apply();
//    }
//
//    public boolean pushFragment(Fragment fragment, String tag) {
//        if (fragment != null) {
//            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//            ft.setCustomAnimations(R.animator.enter_from_right, R.animator.exit_to_left, R.animator.enter_from_left, R.animator.exit_to_right);
//            ft.replace(R.id.content_frame, fragment, tag);
//            ft.addToBackStack(tag);
//            ft.commitAllowingStateLoss();
//
//            return true;
//        }
//        return false;
//    }
//
//    public boolean pushFragment(MainModel model) {
//        if (model instanceof SingleRack) {
//            newBucketInQueue(model);
//            if (!model.isLoadedContent()) {
//                Library.launchServiceForFolder(this, model);
//            } else if (model.isQuickNotes()) {
//                model = model.getFirstFolder();
//            }
//
//            if (search_opened) {
//                return pushFragment(NotebookFragment.newInstance(model, search_query), Constants.NOTEBOOK_BUCKET_FRAGMENT_TAG);
//            }
//
//            return pushFragment(NotebookFragment.newInstance(model), Constants.NOTEBOOK_BUCKET_FRAGMENT_TAG);
//        } else if (model instanceof SingleNotebook) {
//
//            if (db.shouldUpdateFolder(model.getPath())) {
//                Library.launchServiceForNotes(this, path, model);
//            }
//
//            if (search_opened) {
//                return pushFragment(NotebookFragment.newInstance(model, search_query), Constants.NOTEBOOK_FOLDER_FRAGMENT_TAG);
//            }
//
//            return pushFragment(NotebookFragment.newInstance(model), Constants.NOTEBOOK_FOLDER_FRAGMENT_TAG);
//        }
//        return false;
//    }
//
//    public void popFragment() {
//        FragmentManager fm = getSupportFragmentManager();
//        if (fm.getBackStackEntryCount() > 1) {
//            fm.popBackStack();
//        }
//    }
//
//    public boolean isSearchOpen() {
//        return this.search_opened;
//    }
//
//    /*private boolean isServiceRunning(Class<?> serviceClass) {
//        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
//            if (serviceClass.getName().equals(service.service.getClassName())) {
//                return true;
//            }
//        }
//        return false;
//    }*/
//
//    private void newBucketInQueue(MainModel model) {
//        if (bucket_queue == null) bucket_queue = new LinkedList<>();
//        bucket_queue.add(model);
//
//        if (bucket_queue.size() > 2) {
//            MainModel old_bucket = bucket_queue.removeFirst();
//            if (!old_bucket.getPath().equals(model.getPath())) {
//                old_bucket.unloadNotes(new MainModel.OnModelLoadedListener() {
//                    @Override
//                    public void ModelLoaded() {
//                        Log.i(Constants.LOG, "unload Notes");
//                    }
//                });
//            }
//        }
//    }
//
//    @Override
//    public void onBackPressed() {
//        FragmentManager fm = getSupportFragmentManager();
//
//        String last_tag = getLastFragmentTag();
//        if (last_tag != null) {
//            if (last_tag.equals(Constants.BUCKETS_FRAGMENT_TAG)) {
//                askAndClose();
//            } else {
//                fm.popBackStack();
//            }
//        } else {
//            super.onBackPressed();
//        }
//
//        /*if (last_tag != null && nf.getNavigationLayout() != null && nf.getNavigationLayout().closeDrawerIfOpen()) {
//            return;
//
//        } else if (search_opened) {
//            searchBarClosed();
//        } else if (fm.getBackStackEntryCount() > 1) {
//            fm.popBackStack();
//        } else if (fm.getBackStackEntryCount() > 0) {
//            if (!last_tag.equals(Constants.NOTEBOOK_FRAGMENT_TAG)) {
//                fm.popBackStack();
//                pushFragment(NotebookFragment.newInstance(library_list), Constants.NOTEBOOK_FRAGMENT_TAG);
//                return;
//            }
//            askAndClose();
//        } else {
//            super.onBackPressed();
//        }*/
//    }
//
//    void askAndClose() {
//        new AlertDialog.Builder(this)
//            .setIcon(android.R.drawable.ic_dialog_alert)
//            .setTitle(R.string.dialog_close_title)
//            .setMessage(R.string.dialog_close_message)
//            .setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    finish();
//                }
//            })
//            .setNegativeButton(R.string.dialog_no, null)
//            .show();
//    }
//
//    public void reloadAndOpenFolder(final MainModel bucket, final boolean pop_fragment) {
//        if (bucket == null) return;
//        bucket.clearContent();
//        bucket.loadContent(this, new MainModel.OnModelLoadedListener() {
//            @Override
//            public void ModelLoaded() {
//                if (pop_fragment) getSupportFragmentManager().popBackStack();
//                pushFragment(bucket);
//            }
//        });
//    }
//
//    public void updateBucketByUUID(String uuid, ArrayList<MainModel> contents) {
//        for (MainModel m: library_list) {
//            if (m.equalsUUID(uuid)) {
//                m.addContents(contents);
//                m.initParents();
//
//                if (m.isQuickNotes()) {
//                    popFragment();
//                    pushFragment(m.getFirstFolder());
//                } else {
//                    NotebookFragment nf = getNotebookFragment();
//                    if (nf == null) return;
//                    nf.reloadAdapterIfModel(m, true);
//                }
//                return;
//            }
//        }
//    }
//
//    public void updateBuckets(ArrayList<MainModel> contents) {
//        NotebookFragment nf = getNotebookFragment();
//        if (nf == null) return;
//        nf.reloadAdapterWithLibrary(contents, true);
//    }
//
//    private void reloadCurrentFromDB(String uuid) {
//        final NotebookFragment nf = getNotebookFragment();
//        if (nf == null) return;
//
//        MainModel current = getNotebookFragment().getCurrentModel();
//        if (current == null) return;
//
//        if (current.equalsUUID(uuid)) {
//            current.dbLoadNotes(db, this, new MainModel.OnModelLoadedListener() {
//                @Override
//                public void ModelLoaded() {
//                    nf.reloadAdapter(true);
//                }
//            });
//        }
//    }
//
//    public void openQuickNotesBucket() {
//        MainModel quickBucket = Library.getQuickNotesBucket(library_list);
//        reloadAndOpenFolder(quickBucket, false);
//    }
//
//    public void setMovingNote(SingleNote n, MainModel f) {
//        this.moving_note = n;
//        this.moving_note_folder = f;
//    }
//
//    public boolean sameFolderAsMovingNote(MainModel f) {
//        return moving_note_folder.getPath().equals(f.getPath());
//    }
//
//    public SingleNote getMovingNote() {
//        return moving_note;
//    }
//
//    public MainModel getMovingNoteFolder() {
//        return moving_note_folder;
//    }
//
//    public void clearMovingNote() {
//        moving_note = null;
//        moving_note_folder = null;
//    }
//
//    public boolean isMovingNote() {
//        return moving_note != null;
//    }
//
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
//        super.onActivityResult(requestCode, resultCode, resultIntent);
//        if (requestCode == Constants.NEW_QUICK_NOTE_REQUEST_CODE) {
//            if (resultCode == RESULT_OK) {
//                if (resultIntent.getBooleanExtra("modified", false)) {
//                    openQuickNotesBucket();
//                }
//            }
//        } else if (requestCode == Constants.NEW_NOTE_REQUEST_CODE) {
//            if (resultCode == RESULT_OK) {
//                if (resultIntent.getBooleanExtra("modified", false)) {
//                    Bundle bundle_note = resultIntent.getBundleExtra("note");
//                    if (bundle_note != null) {
//                        NotebookFragment notebookFragment = getNotebookFragment();
//                        if (notebookFragment != null && notebookFragment.isVisible()) {
//                            SingleNote new_note = new SingleNote(bundle_note);
//                            notebookFragment.addNewNoteToCurrent(new_note);
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    public Path getPath() {
//        return this.path;
//    }
//
//    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
//        outState.putString("rootPath", this.path.getRootPath());
//        outState.putString("currentPath", this.path.getCurrentPath());
//    }
//
//    @Override
//    protected void onRestoreInstanceState(Bundle savedInstanceState) {
//        super.onRestoreInstanceState(savedInstanceState);
//        if (this.path == null) {
//            this.path = new Path(savedInstanceState.getString("rootPath"));
//        } else {
//            this.path.setRootPath(savedInstanceState.getString("rootPath"));
//        }
//        this.path.setCurrentPath(savedInstanceState.getString("currentPath"));
//
//        /*if (this.library_list.size() == 0) {
//            launchServiceForBuckets(false);
//        }*/
//    }
//
//    @Override
//    public void QuickPathCreated(Bundle quick_bundle) {
//        String folder_path = quick_bundle.getString("path", "");
//        Boolean created_folder = quick_bundle.getBoolean("created_folder", false);
//        Boolean created_bucket = quick_bundle.getBoolean("created_bucket", false);
//
//        if (created_folder) {
//            File f = new File(folder_path);
//            SingleNotebook quick_note_folder = new SingleNotebook(f.getName(), f.toString(), null);
//            quick_note_folder.setAsQuickNotes();
//
//            if (created_bucket) {
//                SingleRack quick_note_bucket = new SingleRack(Constants.QUICK_NOTES_BUCKET, path.getRootPath()+"/"+Constants.QUICK_NOTES_BUCKET, null);
//                quick_note_bucket.addContent(quick_note_folder);
//
//                library_list.add(quick_note_bucket);
//                Collections.sort(library_list, new NotebooksComparator(this));
//
//            } else {
//
//                for (MainModel m : library_list) {
//                    if (m.isBucket() && m.getName().equals(Constants.QUICK_NOTES_BUCKET)) {
//                        m.addContent(quick_note_folder);
//                        break;
//                    }
//                }
//
//            }
//        }
//    }
//}
//
//
