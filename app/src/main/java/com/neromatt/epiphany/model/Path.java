package com.neromatt.epiphany.model;

import java.io.File;

public class Path {

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
}
