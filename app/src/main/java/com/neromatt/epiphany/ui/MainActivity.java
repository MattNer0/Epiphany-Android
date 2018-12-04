package com.neromatt.epiphany.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.helper.DBInterface;
import com.neromatt.epiphany.helper.Database;
import com.neromatt.epiphany.model.Adapters.RackAdapter;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.ui.Fragments.BucketsFragment;
import com.neromatt.epiphany.ui.Fragments.FoldersFragment;
import com.neromatt.epiphany.ui.Fragments.MyFragment;
import com.neromatt.epiphany.ui.Fragments.RecentNotesFragment;
import com.neromatt.epiphany.ui.Navigation.SearchState;
import com.sensorberg.permissionbitte.BitteBitte;
import com.sensorberg.permissionbitte.PermissionBitte;

import java.io.File;
import java.util.ArrayList;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity implements DBInterface, BitteBitte {

    private Database db;
    private String root_path;
    private boolean next_back_will_close_app = false;

    private RecyclerView drawer_list;
    private RackAdapter rack_adapter;

    private SearchState search_state;
    private ArrayList<MainModel> moving_notes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new Database(getApplicationContext());
        if (isFirstRun()) {
            if (PermissionBitte.shouldAsk(this, this)) {
                PermissionBitte.ask(MainActivity.this, MainActivity.this);
            } else {
                yesYouCan();
            }
        } else {
            createBucketsFragment(root_path);
        }
    }

    @Override
    public void onBackPressed() {
        String last_tag = getLastFragmentTag();
        if (closeDrawerIfOpen()) return;

        if (last_tag.equals(Constants.BUCKETS_FRAGMENT_TAG)) {
            if (next_back_will_close_app) {
                finish();
            } else {
                next_back_will_close_app = true;
                Toast.makeText(getContext(), getString(R.string.toast_back_to_quit), Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public Database getDatabase() {
        return db;
    }

    @Override
    public Context getContext() {
        return getApplicationContext();
    }

    public void setMovingNotes(ArrayList<MainModel> list) {
        this.moving_notes = list;
    }

    public ArrayList<MainModel> getMovingNotes() {
        return this.moving_notes;
    }

    public void setSearchState(SearchState search_state) {
        this.search_state = search_state;
    }

    public SearchState getSearchState() {
        return this.search_state;
    }

    public void showSettings() {
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingsIntent);
    }

    public void showRecentNotes() {
        pushFragment(RecentNotesFragment.newInstance(root_path), Constants.RECENT_NOTES_FRAGMENT_TAG, Constants.RECENT_NOTES_FRAGMENT_TAG);
    }

    public void openQuickNotes() {
        String model_path = root_path+"/"+Constants.QUICK_NOTES_BUCKET;
        String quick_path = model_path+"/New Notes";
        pushFragment(FoldersFragment.newInstance(root_path, quick_path), Constants.FOLDER_FRAGMENT_TAG, Constants.FOLDER_FRAGMENT_TAG + model_path);
    }

    public void refreshRackDrawer(ArrayList<MainModel> list) {
        try {
            if (drawer_list == null) drawer_list = findViewById(R.id.left_drawer_list);
            if (rack_adapter == null) {
                rack_adapter = new RackAdapter(getContext(), list);
                rack_adapter.setOnClickListener(new RackAdapter.OnRackClickListener() {
                    @Override
                    public void RackClicked(int position) {
                        closeDrawerIfOpen();
                        MainModel model = rack_adapter.getItem(position);
                        if (model.isQuickNotes()) {
                            String quick_path = model.getPath()+"/New Notes";
                            File f = new File(quick_path);
                            if (f.exists() || f.mkdirs()) {
                                pushFragment(FoldersFragment.newInstance(root_path, quick_path, model.getTitle()), Constants.FOLDER_FRAGMENT_TAG, Constants.FOLDER_FRAGMENT_TAG + model.getPath());
                                return;
                            }
                        }
                        pushFragment(FoldersFragment.newInstance(root_path, model.getPath()), Constants.FOLDER_FRAGMENT_TAG, Constants.FOLDER_FRAGMENT_TAG + model.getPath());
                    }
                });
                drawer_list.setLayoutManager(new LinearLayoutManager(this));
                drawer_list.setAdapter(rack_adapter);
            } else {
                rack_adapter.updateData(list);
            }
        } catch(NullPointerException e) {
            e.printStackTrace();
        }
    }

    public boolean closeDrawerIfOpen() {
        DrawerLayout drawer_layout = findViewById(R.id.drawer_layout);
        if (drawer_layout == null) return false;

        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START, true);
            return true;
        }

        return false;
    }

    public void pushFragment(MyFragment fragment, String fragment_tag, String back_stack_tag) {
        next_back_will_close_app = false;
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.animator.enter_from_right, R.animator.exit_to_left, R.animator.enter_from_left, R.animator.exit_to_right);
        ft.replace(R.id.content_frame, fragment, fragment_tag);
        ft.addToBackStack(back_stack_tag);
        ft.commitAllowingStateLoss();
    }

    public void pushFragment(MyFragment fragment, String fragment_tag, String back_stack_tag, boolean go_back) {
        if (go_back) {
            FragmentManager fm = getSupportFragmentManager();
            if (fm.popBackStackImmediate(back_stack_tag, 0)) {
                return;
            }
        }

        pushFragment(fragment, fragment_tag, back_stack_tag);
    }

    private void createBucketsFragment(String root_path) {
        next_back_will_close_app = false;
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.content_frame, BucketsFragment.newInstance(root_path), Constants.BUCKETS_FRAGMENT_TAG);
        ft.addToBackStack(Constants.BUCKETS_FRAGMENT_TAG);
        ft.commit();
    }

    private boolean isFirstRun() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String path = PreferenceManager.getDefaultSharedPreferences(this).getString("pref_root_directory", "");
        if (prefs.getBoolean("firstrun", true) || path.isEmpty()) {
            return true;
        }

        this.root_path = path;
        return false;
    }

    private String getLastFragmentTag() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() == 0) {
            return "";
        }
        return fm.getBackStackEntryAt(fm.getBackStackEntryCount() - 1).getName();
    }

    @Override
    public void yesYouCan() {
        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();

        File rootDir = Environment.getExternalStorageDirectory();
        String full = rootDir + "/epiphany";
        File fullpath = new File(full);
        if (!(fullpath.exists() && fullpath.isDirectory())) {
            if (!fullpath.mkdirs()) {
                Toast.makeText(this, "Couldn't create initial directories!", Toast.LENGTH_LONG).show();
            }
        }

        root_path = full;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor prefs_edit = prefs.edit();
        prefs_edit.putString("pref_root_directory", full);
        prefs_edit.putBoolean("firstrun", false);
        prefs_edit.putString("pref_note_order", "0");
        if (prefs_edit.commit()) {
            createBucketsFragment(root_path);
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
}
