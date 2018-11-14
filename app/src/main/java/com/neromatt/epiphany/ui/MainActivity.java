package com.neromatt.epiphany.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.helper.DBInterface;
import com.neromatt.epiphany.helper.Database;
import com.neromatt.epiphany.model.Adapters.RackAdapter;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.ui.Fragments.BucketsFragment;
import com.neromatt.epiphany.ui.Fragments.FoldersFragment;
import com.neromatt.epiphany.ui.Fragments.MyFragment;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity implements DBInterface {

    private Database db;
    private String root_path;
    private boolean next_back_will_close_app = false;

    private RecyclerView drawer_list;
    private RackAdapter rack_adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new Database(getApplicationContext());
        if (!isFirstRun()) {
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

    public void showSettings() {
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingsIntent);
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

                        MainModel m = rack_adapter.getItem(position);

                        ArrayList<String> parent = new ArrayList<>();
                        parent.add(root_path);
                        pushFragment(FoldersFragment.newInstance(parent, m.getPath()), Constants.FOLDER_FRAGMENT_TAG);
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

    public void pushFragment(MyFragment fragment, String tag) {
        next_back_will_close_app = false;
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.animator.enter_from_right, R.animator.exit_to_left, R.animator.enter_from_left, R.animator.exit_to_right);
        ft.replace(R.id.content_frame, fragment, tag);
        ft.addToBackStack(tag);
        ft.commitAllowingStateLoss();
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
}
