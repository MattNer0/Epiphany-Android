package com.neromatt.epiphany.model;

import android.os.Bundle;
import android.util.Log;

import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.DataObjects.SingleNote;
import com.neromatt.epiphany.model.DataObjects.SingleNotebook;
import com.neromatt.epiphany.model.DataObjects.SingleRack;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class Path {

    private File currentPath;
    private String rootPath;
    private int depth;

    public Path(String rootPath) {
        this.rootPath = rootPath;
        this.currentPath = new File(rootPath);
        depth = 0;
    }

    public void resetPath() {
        currentPath = new File(rootPath);
        depth = 0;
    }

    public boolean isRoot() {
        return depth == 0;
    }

    public boolean isBucket() {
        return depth == 1;
    }

    public String getRootPath() {
        return rootPath;
    }

    public String getCurrentPath(){
       return currentPath.toString();
    }

    public void setCurrentPath(String currentPath){
        this.currentPath = new File(currentPath);
        if (this.currentPath.toString().equals(this.rootPath)) {
            depth = 0;
        } else if (this.currentPath.getParentFile().toString().equals(this.rootPath)) {
            depth = 1;
        }
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    private static JSONObject getMetaFile(String path, String file_name) {
        File metaFile = new File(path+"/"+file_name);

        try {
            FileInputStream stream = new FileInputStream(metaFile);
            try {
                FileChannel fc = stream.getChannel();
                MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
                String json_string = Charset.defaultCharset().decode(bb).toString();
                return new JSONObject(json_string);

            } catch (JSONException e) {
                Log.e("err", "JSONException");
                e.printStackTrace();
            } finally {
                stream.close();
            }
        } catch (FileNotFoundException e) {
            Log.i("info", "FileNotFoundException "+metaFile.toString());
        } catch (IOException e) {
            Log.e("err", "IOException");
            e.printStackTrace();
        }

        return new JSONObject();
    }

    public ArrayList<MainModel> getRacks() {
        File dir = new File(this.rootPath);
        if (dir.exists()) {
            ArrayList<MainModel> racks = new ArrayList<>();
            File[] fileList = dir.listFiles();

            for (File f : fileList) {
                if (!f.getName().startsWith(".")) {
                    if (f.isDirectory()) {
                        racks.add(new SingleRack(f.getName(), f.toString(), getMetaFile(f.toString(), ".bucket.json")));
                    }
                }
            }
            return racks;
        }
        return null;
    }

    public Bundle getQuickNotesPath() {
        Bundle ret = new Bundle();
        boolean created_folder = false;
        boolean created_bucket = false;
        String bucket_quick_path = rootPath+"/"+Constants.QUICK_NOTES_BUCKET;
        String quick_path = bucket_quick_path+"/New Notes";
        File b = new File(bucket_quick_path);
        File f = new File(quick_path);
        if (!f.exists()) {
            if (!b.exists()) {
                created_bucket = true;
            }
            created_folder = true;

            if (!f.mkdirs()) {
                quick_path = "";
                created_folder = false;
                created_bucket = false;
            }
        }

        ret.putString("path", quick_path);
        ret.putBoolean("created_folder", created_folder);
        ret.putBoolean("created_bucket", created_bucket);
        return ret;
    }

    public String getName() {
        File f = new File(this.getCurrentPath());
        return f.getName();
    }

    public ArrayList<MainModel> getBuckets() {
        String current_path = getRootPath();
        File dir = new File(current_path);
        if (dir.exists()) {
            ArrayList<MainModel> filesArray = new ArrayList<>();

            for (File f : dir.listFiles()) {
                if (!f.getName().startsWith(".")) {
                    if (f.isDirectory()) {
                        filesArray.add(new SingleRack(f.getName(), f.toString(), getMetaFile(f.toString(), ".bucket.json")));
                    }
                }
            }
            return filesArray;
        }
        return null;
    }

    public static ArrayList<MainModel> getFoldersAndNotes(String current_path) {
        File dir = new File(current_path);
        if (dir.exists()) {
            ArrayList<MainModel> filesArray = new ArrayList<>();

            for (File f : dir.listFiles()) {
                if (!f.getName().startsWith(".")) {
                    if (f.isDirectory()) {
                        filesArray.add(new SingleNotebook(f.getName(), f.toString(), getMetaFile(f.toString(), ".folder.json")));
                    } else {
                        String extension = getFileExtension(f);
                        if ((extension.equalsIgnoreCase("txt"))||(extension.equalsIgnoreCase("md"))) {
                            //String notepath = dir + "/" + f.getName();
                            filesArray.add(new SingleNote(dir.getPath(),f.getName()));
                            //filesArray.add(new SingleNote(dir.getPath(),getNoteTrunc(notepath,1,5),f.getName(), new Date(f.lastModified())));
                        }
                    }
                }
            }
            return filesArray;
        }
        return null;
    }

    private static String getFileExtension(File filename){
        return filename.toString().substring(filename.toString().lastIndexOf('.') + 1);
    }

    public MainModel createNotebook(String path, String notebook_name) {
        if (createFolder(path+"/"+notebook_name)) {
            return new SingleNotebook(notebook_name, path+"/"+notebook_name, null);
        }
        return null;
    }

    public MainModel createRack(String rackname) {
        if (createFolder(rootPath+"/"+rackname)) {
            return new SingleRack(rackname, rootPath+"/"+rackname, null);
        }
        return null;
    }

    public static String newNoteName(String path, String extension) {
        int name_num = 0;
        String new_filename = "";
        boolean found = false;

        extension = extension.replace(".", "");

        while(!found) {
            found = true;
            new_filename = "note_"+name_num+"."+extension;
            File f1 = new File(path+"/"+new_filename);
            if (f1.exists()) {
                name_num++;
                found = false;
            }
        }

        return new_filename;
    }

    public static String newNoteNameFromCurrent(String path, String current_filename, String extension) {
        int name_num = 0;
        String new_filename = "";
        boolean found = false;

        extension = extension.replace(".", "");

        while(!found) {
            found = true;
            new_filename = current_filename+"_"+name_num+"."+extension;
            File f1 = new File(path+"/"+new_filename);
            if (f1.exists()) {
                name_num++;
                found = false;
            }
        }

        return new_filename;
    }

    public boolean createFolder(String path) {
        File dir = new File(path);
        return dir.mkdir();
    }

    public String getTitle() {
        if (isRoot()) {
            return "Epiphany Library";
        }
        if (isBucket()) {
            if (currentPath.getName().equals("_quick_notes")) return "Quick Notes";
            return currentPath.getName();
        }

        return currentPath.getName();
    }

}
