package com.neromatt.epiphany.tasks;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.neromatt.epiphany.Constants;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDDocumentInformation;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.text.TextPosition;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.List;

import androidx.annotation.NonNull;

public class ParsePdfTask extends AsyncTask<Uri, Integer, String> {

    private final PdfListener listener;
    private final WeakReference<Context> context;

    private int page_number;
    private float page_width = 0.0f;
    private StringBuilder sb;

    public ParsePdfTask(Context context, @NonNull PdfListener listener) {
        this.context = new WeakReference<>(context);
        this.listener = listener;
    }

    @Override
    protected String doInBackground(Uri... uris) {
        String pdf_content = "";
        try {

            PDDocument document = null;

            if (uris[0].getScheme().equals("content")) {

                InputStream is = context.get().getContentResolver().openInputStream(uris[0]);
                document = PDDocument.load(is);

            } else if (uris[0].getScheme().equalsIgnoreCase("http") || uris[0].getScheme().equalsIgnoreCase("https")) {

                // download pdf

            } else {
                File f = new File(uris[0].toString());
                document = PDDocument.load(f);
            }

            if (document != null) {

                //PDDocumentInformation pdd = document.getDocumentInformation();
                /*if (pdd.getTitle() != null && !pdd.getTitle().isEmpty()) {
                    pdf_title = pdd.getTitle();
                }*/

                sb = new StringBuilder();

                PDFTextStripper pdfStripper = new PDFTextStripper() {
                    @Override
                    protected void writeString(String str, List<TextPosition> textPositions) throws IOException {

                        if (str.isEmpty() || str.matches("^\\s+$")) {
                            sb.append("\n\n");
                        } else {

                            float character_x = textPositions.get(textPositions.size()-1).getX();
                            float character_width = textPositions.get(textPositions.size()-1).getWidth();

                            float total_width = character_x + character_width;

                            //Log.i(Constants.LOG, str+" > "+total_width+ " " +page_width);

                            if (str.matches("^[a-zA-Z0-9]\\.") && sb.substring(sb.length()-1, sb.length()).equals(" ")) {
                                sb.append("\n");
                            }

                            sb.append(str);

                            if (page_width-total_width > 110 || str.matches("[.:;!?]\\s?$")) {
                                if (page_width-total_width <= 110) Log.i(Constants.LOG, str+" > "+(page_width-total_width));
                                sb.append("\n");
                            } else if (!str.endsWith(" ")) {
                                sb.append(" ");
                            }
                        }

                        super.writeString(str, textPositions);
                        // you may process the line here itself, as and when it is obtained
                    }
                };

                page_number = document.getNumberOfPages();

                for (int i=0; i<page_number; i++) {

                    page_width = document.getPage(i).getMediaBox().getWidth();

                    pdfStripper.setSortByPosition(true);
                    pdfStripper.setStartPage(i);
                    pdfStripper.setEndPage(i);

                    pdfStripper.getText(document);

                    publishProgress(i);
                }

                pdf_content = sb.toString();
            }

        } catch (IOException e) {
            e.printStackTrace();

            if (sb != null && sb.length() > 0) {
                pdf_content = sb.toString();
            }
        }

        return pdf_content;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        listener.Progress(progress[0], page_number);
    }

    @Override
    protected void onPostExecute(String data) {
        if (isCancelled()) return;
        listener.TextParsed(data);
    }

    public interface PdfListener {
        void Progress(int page, int pages);
        void TextParsed(String body);
    }
}
