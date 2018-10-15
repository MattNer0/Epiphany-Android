package com.neromatt.epiphany.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import java.io.File;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class SettingsFragment extends PreferenceFragmentCompat {

    private File currentRootDirectory;
    private Preference rootDirPref;
    private FileDialog rootFolderDialog;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);
        this.setupRootDirPref();
        restorePref();

        File mPath = currentRootDirectory;
        rootFolderDialog = new FileDialog(this.getActivity(), mPath);
        rootFolderDialog.setSelectDirectoryOption(true);
        rootFolderDialog.addDirectoryListener(new FileDialog.DirectorySelectedListener() {
            public void directorySelected(File directory) {
                updateRootDir(directory);
                updateRootDirSummary();
            }
        });
    }
    void restorePref() {
        updateRootDirSummary();
    }

    void setupRootDirPref() {
        this.rootDirPref = findPreference("pref_root_directory");
        rootDirPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                rootFolderDialog.showDialog();
                return true;
            }
        });
    }

    public void updateRootDir(File directory) {
        currentRootDirectory = directory;
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        editor.putString("pref_root_directory", currentRootDirectory.toString());
        editor.apply();
    }

    public void updateRootDirSummary() {
        rootDirPref.setSummary(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("pref_root_directory", ""));
        currentRootDirectory = new File(rootDirPref.getSummary().toString());
    }
}