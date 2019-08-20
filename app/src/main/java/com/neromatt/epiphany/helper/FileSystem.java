package com.neromatt.epiphany.helper;

import android.util.Log;

import com.neromatt.epiphany.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class FileSystem {
    private static boolean moveFile(String inputPath, String inputFile, String outputPath) {

        InputStream in;
        OutputStream out;
        try {

            //create output directory if it doesn't exist
            File dir = new File (outputPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }


            in = new FileInputStream(inputPath + "/" + inputFile);
            out = new FileOutputStream(outputPath + "/" + inputFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            //in = null;

            // write the output file
            out.flush();
            out.close();
            //out = null;

            // delete the original file
            return new File(inputPath + inputFile).delete();
        }

        catch (FileNotFoundException fnfe1) {
            Log.e(Constants.LOG, fnfe1.getMessage());
        }
        catch (Exception e) {
            Log.e(Constants.LOG, e.getMessage());
        }

        return false;
    }

    public static boolean deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }

        return fileOrDirectory.delete();
    }

    public static boolean moveRecursive(File fileOrDirectory, String destination) {
        if (fileOrDirectory.exists()) {
            if (fileOrDirectory.isDirectory()) {
                moveDirectoryRecursive(fileOrDirectory, destination + "/" + fileOrDirectory.getName());
            } else {
                return moveFile(fileOrDirectory.getParent() + "/", fileOrDirectory.getName(), destination);
            }
        }
        return false;
    }

    public static boolean moveDirectoryRecursive(File fileOrDirectory, String new_destination) {
        for (File child : fileOrDirectory.listFiles()) {
            if (!moveRecursive(child, new_destination)) {
                return false;
            }
        }
        return true;
    }
}
