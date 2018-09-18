package com.neromatt.epiphany.model;

import android.util.Log;

import com.neromatt.epiphany.model.DataObjects.MainModel;
import com.neromatt.epiphany.model.DataObjects.SingleNote;
import com.neromatt.epiphany.model.DataObjects.SingleNotebook;
import com.neromatt.epiphany.model.DataObjects.SingleRack;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;

public class Path{

    private File currentPath;
    private String rootPath;
    private int depth;

    public Path(String rootPath) {
        this.rootPath = rootPath;
        this.currentPath = new File(rootPath);
        depth = 0;
    }

    public void getBack() {
        if(!currentPath.toString().equals(rootPath)){
            currentPath = this.currentPath.getParentFile();
            depth -= 1;
        }
    }

    public void goForward(String path) {
        File newPath = new File(currentPath.toString()+"/"+path);
        if (newPath.exists() && newPath.isDirectory()) {
            depth+=1;
            this.currentPath = newPath;
        }
    }

    public void resetPath() {
        currentPath = new File(rootPath);
        depth = 0;
    }

    public boolean isRack() {
        return depth == 1;
    }

    public String getCurrentPath(){
       return currentPath.toString();
    }
    public void setCurrentPath(String currentPath){
        this.currentPath = new File(currentPath);
        if (this.currentPath.getParentFile().toString().equals(this.rootPath)) {
            depth = 1;
        }
    }

    public boolean isLeaf() {
        for (File f : currentPath.listFiles()) {
            if (f.isDirectory()){
                return false;
            }
        }
        return true;
    }

    public ArrayList<MainModel> getRacks() {
        File dir = new File(this.rootPath);
        if (dir.exists()) {
            ArrayList<MainModel> racks = new ArrayList<>();
            File[] fileList = dir.listFiles();

            for (File f : fileList) {
                if (!f.getName().startsWith(".")) {
                    if (f.isDirectory()) {
                        File metaBucket = new File(f.toString()+"/.bucket.json");

                        try {
                            FileInputStream stream = new FileInputStream(metaBucket);
                            try {
                                FileChannel fc = stream.getChannel();
                                MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
                                String json_string = Charset.defaultCharset().decode(bb).toString();
                                racks.add(new SingleRack(f.getName(), f.toString(), new JSONObject(json_string)));

                            } catch (JSONException e) {
                                Log.e("err", "JSONException");
                                e.printStackTrace();
                            } finally {
                                stream.close();
                            }
                        } catch (FileNotFoundException e) {
                            Log.i("info", "FileNotFoundException "+metaBucket.toString());
                            racks.add(new SingleRack(f.getName(), f.toString(), fileList.length));
                        } catch (IOException e) {
                            Log.e("err", "IOException");
                            e.printStackTrace();
                        }
                    }
                }
            }
            return racks;
        }
        return null;
    }

    public ArrayList<MainModel> getFoldersAndNotes() {
        File dir = new File(this.getCurrentPath());
        if (dir.exists()) {
            ArrayList<MainModel> filesArray = new ArrayList<>();

            for (File f : dir.listFiles()) {
                if (!f.getName().startsWith(".")) {
                    if (f.isDirectory()) {
                        File[] files = f.listFiles(new FilenameFilter() {
                            public boolean accept(File dir, String name) {
                                name = name.toLowerCase();
                                return name.endsWith(".txt") || name.endsWith(".md");
                            }
                        });
                        int numberOfNotes = files.length;
                        filesArray.add(new SingleNotebook(f.getName(),f.toString(),numberOfNotes));
                    } else {
                        String extension = getFileExtension(f);
                        if ((extension.equalsIgnoreCase("txt"))||(extension.equalsIgnoreCase("md"))) {
                            String notepath = dir + "/" + f.getName();
                            filesArray.add(new SingleNote(dir.getPath(),getNoteTrunc(notepath,1,5),f.getName(), new Date(f.lastModified())));
                        }
                    }
                }
            }
            return filesArray;
        }
        return null;
    }

    public ArrayList<MainModel> getNotes() {
        File dir = new File(getCurrentPath());
        if (dir.exists()) {
            ArrayList<MainModel> notes = new ArrayList<>();

            for (File f : dir.listFiles()) {
                if (!f.getName().startsWith(".")) {
                    if (!f.isDirectory()) {
                        String extension = getFileExtension(f);
                        if ((extension.equalsIgnoreCase("txt"))||(extension.equalsIgnoreCase("md"))) {
                            String note_path = dir+"/"+f.getName();
                            notes.add(new SingleNote(dir.getPath(),getNoteTrunc(note_path,1,5),f.getName(), new Date(f.lastModified())));
                        }
                    }
                }
            }
            return notes;
        }
        return null;
    }

    public String getFileExtension(File filename){
        return filename.toString().substring(filename.toString().lastIndexOf('.') + 1);
    }

    public String getNote(String noteName){
        return getNoteTrunc(getCurrentPath()+"/"+noteName, 0,0);
    }

    public String getNoteMarkdown (String absolutePath) {
        File file = new File(absolutePath);
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            int counter=0;

            while ((line = br.readLine()) != null) {
                if(counter==0) {
                    text.append("##");
                    counter++;
                }
                text.append(line);
                text.append("  \n");
            }
            br.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return text.toString();
    }
    private String getNoteTrunc(String absolutePath, int fromLine, int toLine) {
        int counter = 0;
        File file = new File(absolutePath);
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            finish:
            while ((line = br.readLine()) != null) {
                if(counter >= fromLine) {
                    text.append(line);
                    text.append("\n");
                }
                if(toLine !=0) {
                    counter++;
                    if (counter == toLine) {
                        text.append("....");
                        break finish;
                    }
                }
            }
            br.close();
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
            return null;
        }
        return text.toString();
    }
    public boolean createNote(String path, String name, String text) {

        String filename = path + "/" + name + ".md";
        String tempfile = path + "/" + name + ".md.temp";

        File temppath = new File(tempfile);
        File filepath = new File(filename);

        FileWriter writer = null;
        try {
            writer = new FileWriter(temppath);
            writer.append(text);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (filepath.exists()) {
            int counter = 1;
            while (filepath.exists()) {
                String tempFile = path + "/" + name + "##" + counter + "##" + ".md";
                filepath = new File(tempFile);
                counter++;
            }
            temppath.renameTo(filepath);
        }
        else {
            temppath.renameTo(filepath);
        }
        return true;
    }
    public Date getLastModifiedDate(String filename){
        File filepath = new File(getCurrentPath()+"/"+filename);
        return new Date(filepath.lastModified());
    }

    /**
     * Note : this is not robust, since I don't know what syncthing does
     * Does the oldFile still exist, that means it didn't get deleted by Syncthing
     * - Check if it was modified by syncthing in the meantime by checking date
     *      - Yes
     *          Create Note should save it as a duplicate with higher number
     *      - No
     *          Delete Old File, then Create Note
     */
    public void modifyNote(String absoluteFolderPath, String oldname, String newName, String text, Date lastModified){
        File oldFile = new File(absoluteFolderPath+"/"+oldname+".md");

        if(oldFile.exists()){
            long lastModifiedDate = new Date(oldFile.lastModified()).getTime();
            Log.d("lastModDate",String.valueOf(lastModifiedDate));
            Log.d("lastModDate2",String.valueOf(lastModified.getTime()));
            if(lastModifiedDate == lastModified.getTime()){
                oldFile.delete();
                Log.d("del","delete old file");
                createNote(absoluteFolderPath,newName,text);
            }
            else{
                createNote(absoluteFolderPath,newName,text);
            }
        }
        else{
            createNote(absoluteFolderPath,newName,text);
        }
    }
    public void deleteNote(String absolutePath){
        File toDelete = new File(absolutePath);
        toDelete.delete();
    }
    public void createNotebook(String notebookname) {
        createFolder(getCurrentPath()+"/"+notebookname);
    }
    public void createRack(String rackname) {
        createFolder(rootPath+"/"+rackname);
    }
    public void renameNotebook(String oldName, String newName) {
        File f1=new File(currentPath+"/"+oldName);
        f1.renameTo(new File(currentPath+"/"+newName));
    }

    public void createFolder(String path) {
        File dir = new File(path);
        dir.mkdir();
    }

    public void deleteNotebook(String absoluteFolderPath){
        File dir = new File(absoluteFolderPath);
        deleteRecursive(dir);
    }
    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }

    public void moveFile(String absoluteFolderPathNew, String notename){
        //TODO don't hardcode this one
        String note = getNote(notename+".md");
        createNote(absoluteFolderPathNew,notename,note);
        deleteNote(getCurrentPath()+"/"+notename+".md");
    }
    public String getTitle() {
        if (isRack()) {
            if (currentPath.getName().equals("_quick_notes")) return "Quick Notes";
            return currentPath.getName();
        }

        return currentPath.getParentFile().getName()+"/"+currentPath.getName();
    }

}
