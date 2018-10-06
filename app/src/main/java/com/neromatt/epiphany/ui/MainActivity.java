package com.neromatt.epiphany.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.Path;
import com.neromatt.epiphany.model.Adapters.RackAdapter;
import com.neromatt.epiphany.ui.NotebookFragment.NotebookFragment;
import com.sensorberg.permissionbitte.BitteBitte;
import com.sensorberg.permissionbitte.PermissionBitte;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements BitteBitte, PathSupplier {

    private Toolbar toolbar; // Declaring the Toolbar Object
    android.support.v7.app.ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private ConstraintLayout mDrawerNavigation;
    private RecyclerView mDrawerList;
    private RackAdapter mRackAdapter;
    private Path path;
    private ArrayList<MainModel> library_list;

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
            return;
        } else {
            this.yesYouCan();
        }

        if (savedInstanceState == null) {
            loadLibrary();
        }

        Intent intent = getIntent();
        if (intent == null) return;
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(intent.getAction()) && type != null) {
            if ("text/plain".equals(type)) {
                newSharedNote(intent); // Handle text being sent
            }
        }
    }

    private void newSharedNote(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            Log.i("text", sharedText);
        }
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
                //mRackAdapter.sort(SortBy.ORDER);
                //mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
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
    protected void onResume() {
        super.onResume();
        refreshRackDrawer();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    private void setupToolBar() {
        toolbar = findViewById(R.id.toolbar); // Attaching the layout to the toolbar object
        if (toolbar != null) {
            setSupportActionBar(toolbar);    // Setting toolbar as the ActionBar with setSupportActionBar() call
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    void setupDrawerToggle() {
        mDrawerToggle = new android.support.v7.app.ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.app_name, R.string.app_name);
        mDrawerToggle.syncState();
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void selectItem(int position) {
        try {
            Log.i("log", "select "+position);
            NotebookFragment notebookFragment = (NotebookFragment) getSupportFragmentManager().findFragmentByTag("Notebook_Fragment");
            if (notebookFragment != null && notebookFragment.isVisible()) {
                notebookFragment.openBucket(mRackAdapter.getItem(position));
            } else {
                //String rackName = mRackAdapter.getItem(position).getName();
                path.resetPath();
                createNotebookFragment();
            }

            /*mDrawerList.setItemChecked(position, true);
            mDrawerList.setSelection(position);*/
            mDrawerLayout.closeDrawer(mDrawerNavigation);
        } catch (NullPointerException e) {
            Log.e("err", "null item");
        }
    }

    public void loadNewBucket(String name) {
        MainModel new_rack = path.createRack(name);
        library_list.add(new_rack);
        refreshRackDrawer();
        NotebookFragment notebookFragment = (NotebookFragment) getSupportFragmentManager().findFragmentByTag("Notebook_Fragment");
        if (notebookFragment != null && notebookFragment.isVisible()) {
            notebookFragment.refreshNotebooks();
        }
    }

    private void loadLibrary() {
        Intent intent = new Intent(this, LoadLibrary.class);
        intent.putExtra("root", path.getRootPath());
        startActivityForResult(intent, Constants.LOADING_REQUEST_CODE);
    }

    private boolean createNotebookFragment() {
        Fragment fragment = NotebookFragment.newInstance(library_list);
        if (fragment != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.content_frame, fragment, "Notebook_Fragment");
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
            //String fullnotebook = full+"/FirstNotebook";
            //File defaultnotebookpath = new File(fullnotebook);
            if (!(fullpath.exists() && fullpath.isDirectory())) {
                fullpath.mkdirs();
                //defaultnotebookpath.mkdirs();
            }

            SharedPreferences.Editor prefs_edit = prefs.edit();

            prefs_edit.putString("pref_root_directory", full);
            prefs_edit.putBoolean("firstrun", false);
            prefs_edit.putInt("pref_note_order", 0);
            prefs_edit.apply();

            this.path = new Path(full);
            return true;
        }
        return false;
    }

    @Override
    public void yesYouCan() {
        if (checkIfFirstRun()) {
            loadLibrary();
        } else {
            String path = PreferenceManager.getDefaultSharedPreferences(this).getString("pref_root_directory", "");
            this.path = new Path(path);
        }

        //setupDrawerHeader();
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
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            showSettings();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(Gravity.START)) {
            this.mDrawerLayout.closeDrawer(Gravity.START, true);
        } else {
            NotebookFragment notebookFragment = (NotebookFragment) getSupportFragmentManager().findFragmentByTag("Notebook_Fragment");
            if (notebookFragment != null && notebookFragment.isVisible()) {
                if (!notebookFragment.moveBack()) {
                    askAndClose();
                }
                /*if (path.isRoot()) {
                    askAndClose();
                } else {
                    path.getBack();

                }*/
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        super.onActivityResult(requestCode, resultCode, resultIntent);
        if (requestCode == Constants.LOADING_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                library_list = resultIntent.getParcelableArrayListExtra("buckets");
                //Log.i("log", "loading is over!");
                createNotebookFragment();
            }
        }
    }

    public Path getPath() {
        return this.path;
    }

    public void pushFragment(Fragment myFragment) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        fragmentTransaction.replace(R.id.content_frame, myFragment, "Notebook_Fragment");
        fragmentTransaction.commit();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("currentPath", this.path.getCurrentPath());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        this.path.setCurrentPath(savedInstanceState.getString("currentPath"));
    }
}


