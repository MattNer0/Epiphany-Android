package com.neromatt.epiphany.tasks;

import android.os.AsyncTask;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;

public class ImageDownloaderTask extends AsyncTask<String, Void, ArrayList<ImageDownloaderTask.ImageReplace>> {

    private ImageDownloadListener listener;
    private String image_folder;

    public ImageDownloaderTask(String image_folder, ImageDownloadListener listener) {
        this.image_folder = image_folder;
        this.listener = listener;
    }

    static boolean containsUrl(ArrayList<ImageReplace> list, String url) {
        for(ImageReplace img: list) {
            if (img.old_path.equals(url)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected ArrayList<ImageReplace> doInBackground(String... strings) {
        ArrayList<ImageReplace> res = new ArrayList<>();

        File folder = new File(image_folder);
        if (!folder.exists()) folder.mkdirs();

        for (String url: strings) {
            if (!containsUrl(res, url)) {
                try {
                    URL urlObj = new URL(url);
                    InputStream input = urlObj.openStream();

                    OutputStream output = new FileOutputStream(image_folder + "/" + FilenameUtils.getName(url));
                    try {
                        byte[] buffer = new byte[2048];
                        int bytesRead = 0;
                        while ((bytesRead = input.read(buffer, 0, buffer.length)) >= 0) {
                            output.write(buffer, 0, bytesRead);
                        }
                    } finally {
                        output.close();
                    }

                    res.add(new ImageReplace(url, "epiphany://"+FilenameUtils.getName(url)));

                    input.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return res;
    }

    @Override
    protected void onPostExecute(ArrayList<ImageReplace> data) {
        if (listener != null) listener.ImagesDone(data);
    }

    public class ImageReplace {
        public String old_path;
        public String new_path;

        public ImageReplace(String old_path, String new_path) {
            this.old_path = old_path;
            this.new_path = new_path;
        }

        @Override
        public String toString() {
            return old_path+" -> "+new_path;
        }
    }

    public interface ImageDownloadListener {
        void ImagesDone(ArrayList<ImageReplace> list);
    }
}
