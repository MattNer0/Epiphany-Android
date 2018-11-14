package com.neromatt.epiphany.helper;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

public class DirMetaFile {
    public static JSONObject read(String path, String file_name) {
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
}
