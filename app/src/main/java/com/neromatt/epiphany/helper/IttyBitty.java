package com.neromatt.epiphany.helper;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Base64;

import org.apache.commons.io.IOUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import lzma.streams.LzmaOutputStream;

public class IttyBitty {

    private static final String DATA_PREFIX_BXZE = "data:text/html;charset=utf-8;bxze64,";

    private static byte[] compressLzma(String source) throws IOException {

        final ByteArrayOutputStream finalOut = new ByteArrayOutputStream();

        final LzmaOutputStream compressedOut = new LzmaOutputStream.Builder(finalOut).build();

        final ByteArrayInputStream sourceIn = new ByteArrayInputStream(
                source.getBytes(StandardCharsets.UTF_8)
        );

        IOUtils.copy(sourceIn, compressedOut);

        sourceIn.close();
        compressedOut.flush();
        compressedOut.close();

        return finalOut.toByteArray();
    }

    public static void createLink(String data, String title, OnLinkCreated mOnLinkCreated) {
        CompressTask task = new CompressTask(title, mOnLinkCreated);
        task.execute(data);
    }

    private static class CompressTask extends AsyncTask<String, Void, String> {

        private OnLinkCreated mOnLinkCreated;
        private String title;

        CompressTask(String title, OnLinkCreated mOnLinkCreated) {
            if (title == null) { this.title = ""; }
            else { this.title = Uri.encode(title); }
            this.mOnLinkCreated = mOnLinkCreated;
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                String body = strings[0].replaceAll("[\\n|\\t]+", " ").replaceAll("> +<", "> <");
                byte[] compr = compressLzma(body);
                byte[] encoded = Base64.encode(compr, Base64.DEFAULT);
                return new String(encoded, StandardCharsets.UTF_8);
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                mOnLinkCreated.LinkCreated(null);
                return;
            }

            String url = "https://itty.bitty.site/#" + title + "/" + DATA_PREFIX_BXZE + result;
            mOnLinkCreated.LinkCreated(url);
        }
    }

    public interface OnLinkCreated {
        void LinkCreated(String url);
    }
}
