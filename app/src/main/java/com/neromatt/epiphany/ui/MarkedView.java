package com.neromatt.epiphany.ui;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.Nullable;

/**
 * The MarkedView is the Markdown viewer.
 *
 * Created by mittsu on 2016/04/25.
 */
public final class MarkedView extends WebView {

    private static final String TAG = MarkedView.class.getSimpleName();
    //private static final String IMAGE_PATTERN = "!\\[(.*)\\]\\((.*)\\)";

    private String previewText;
    private String noteImagePath;
    private boolean codeScrollDisable;

    private JavaScriptInterface JSInterface;

    private OnCheckboxChangedListener mOnCheckboxChangedListener;
    private OnHTMLBodyListener mOnHTMLBodyListener;
    private OnImageClickListener mOnImageClickListener;

    public MarkedView(Context context) {
        this(context, null);
    }

    public MarkedView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MarkedView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(11)
    @SuppressLint("SetJavaScriptEnabled")
    private void init() {
        // default browser is not called.
        setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                sendScriptAction();
            }

            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    String url = request.getUrl().toString();
                    String scheme = request.getUrl().getScheme();
                    if (scheme != null && noteImagePath != null && scheme.equalsIgnoreCase("epiphany") && request.getRequestHeaders().get("Accept").contains("image")) {
                        try {
                            url = url.replace("epiphany://", "file:///"+noteImagePath+"/");
                            URL urlImage = new URL(url);
                            URLConnection connection = urlImage.openConnection();
                            return new WebResourceResponse(connection.getContentType(), connection.getHeaderField("encoding"), connection.getInputStream());
                        } catch (MalformedURLException e) {
                            Log.e("err", "MalformedURLException");
                            e.printStackTrace();
                        } catch (IOException e) {
                            Log.e("err", "IOException");
                            e.printStackTrace();
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }
        });

        setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("epiphany", consoleMessage.message() + " -- From line "
                        + consoleMessage.lineNumber() + " of "
                        + consoleMessage.sourceId());
                return super.onConsoleMessage(consoleMessage);
            }
        });

        JSInterface = new JavaScriptInterface();
        addJavascriptInterface(JSInterface, "JSInterface");

        getSettings().setJavaScriptEnabled(true);
        loadUrl("file:///android_asset/html/md_preview.html");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
    }

    private void sendScriptAction() {
        evaluateJavascript(previewText, null);
    }

    public MarkedView setCheckboxCallback(OnCheckboxChangedListener mOnCheckboxChangedListener) {
        this.mOnCheckboxChangedListener = mOnCheckboxChangedListener;
        return this;
    }

    public MarkedView setHtmlBodyCallback(OnHTMLBodyListener mOnHTMLBodyListener) {
        this.mOnHTMLBodyListener = mOnHTMLBodyListener;
        return this;
    }

    public MarkedView setImageClickCallback(OnImageClickListener mOnImageClickListener) {
        this.mOnImageClickListener = mOnImageClickListener;
        return this;
    }

    public MarkedView setNoteImagePath(String path) {
        this.noteImagePath = path;
        return this;
    }

    public void loadMDFilePath(String filePath){
        loadMDFile(new File(filePath));
    }

    public void loadMDFile(File file) {
        String mdText = "";
        try {
            FileInputStream fileInputStream = new FileInputStream(file);

            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String readText = "";
            StringBuilder stringBuilder = new StringBuilder();
            while ((readText = bufferedReader.readLine()) != null) {
                stringBuilder.append(readText);
                stringBuilder.append("\n");
            }
            fileInputStream.close();
            mdText = stringBuilder.toString();

        } catch(FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException:" + e);
        } catch(IOException e) {
            Log.e(TAG, "IOException:" + e);
        }
        setMDText(mdText);
    }

    public void setMDText(String text){
        text2Mark(text);
    }

    private void text2Mark(String mdText){
        //String bs64MdText = imgToBase64(mdText);
        String escMdText = escapeForText(mdText);
        previewText = String.format("preview('%s', %b)", escMdText, isCodeScrollDisable());
        sendScriptAction();
    }

    private String escapeForText(String mdText){
        String escText = mdText.replace("\n", "\\\\n");
        escText = escText.replace("'", "\\\'");
        //in some cases the string may have "\r" and our view will show nothing,so replace it
        escText = escText.replace("\r","");
        return escText;
    }

    private String epiphanyUrl(String request_url) {
        Uri uri = Uri.parse(request_url);
        String scheme = uri.getScheme();

        if (scheme != null && noteImagePath != null && scheme.equalsIgnoreCase("epiphany")) {
            return request_url.replace("epiphany://", "file:///" + noteImagePath + "/");
        }

        return null;
    }

    /*private String imgToBase64(String mdText){
        Pattern ptn = Pattern.compile(IMAGE_PATTERN);
        Matcher matcher = ptn.matcher(mdText);
        if(!matcher.find()){
            return mdText;
        }

        String imgPath = matcher.group(2);
        if(isUrlPrefix(imgPath) || !isPathExChack(imgPath)) {
            return mdText;
        }
        String baseType = imgEx2BaseType(imgPath);
        if(baseType.equals("")){
            // image load error.
            return mdText;
        }

        File file = new File(imgPath);
        byte[] bytes = new byte[(int) file.length()];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException:" + e);
        } catch (IOException e) {
            Log.e(TAG, "IOException:" + e);
        }
        String base64Img = baseType + Base64.encodeToString(bytes, Base64.NO_WRAP);

        return mdText.replace(imgPath, base64Img);
    }

    private boolean isUrlPrefix(String text){
        return text.startsWith("http://") || text.startsWith("https://");
    }

    private boolean isPathExChack(String text) {
        return text.endsWith(".png")
                || text.endsWith(".jpg")
                || text.endsWith(".jpeg")
                || text.endsWith(".gif");
    }

    private String imgEx2BaseType(String text) {
        if (text.endsWith(".png")) {
            return "data:image/png;base64,";
        } else if(text.endsWith(".jpg") || text.endsWith(".jpeg")) {
            return "data:image/jpg;base64,";
        } else if(text.endsWith(".gif")) {
            return "data:image/gif;base64,";
        } else {
            return "";
        }
    }*/

    /* options */

    public void setCodeScrollDisable(){
        codeScrollDisable = true;
    }

    private boolean isCodeScrollDisable(){
        return codeScrollDisable;
    }

    public interface OnCheckboxChangedListener {
        void CheckboxChange(String note_body, int num, boolean checked);
    }

    public interface OnHTMLBodyListener {
        void HTMLBody(String note_body);
    }

    public interface OnImageClickListener {
        void ImageClick(String image_url);
    }

    public class JavaScriptInterface {

        JavaScriptInterface() { }

        @android.webkit.JavascriptInterface
        public void checkbox(String note_body, int num, boolean checked) {
            if (mOnCheckboxChangedListener != null) mOnCheckboxChangedListener.CheckboxChange(note_body, num, checked);

        }

        @android.webkit.JavascriptInterface
        public void body(String note_body) {
            if (mOnHTMLBodyListener != null) mOnHTMLBodyListener.HTMLBody(note_body);
        }

        @android.webkit.JavascriptInterface
        public void image(String image_url) {
            if (mOnImageClickListener != null) mOnImageClickListener.ImageClick(epiphanyUrl(image_url));
        }
    }
}
