package com.neromatt.epiphany.ui;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.github.rahatarmanahmed.cpv.CircularProgressView;
import com.neromatt.epiphany.Constants;
import com.neromatt.epiphany.helper.AdBlocker;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.Nullable;

public final class TurndownView extends WebView {

    private Map<String, Boolean> loadedUrls = new HashMap<>();

    private JavaScriptInterface JSInterface;
    private OnMarkdownBodyListener mOnMarkdownBodyListener;
    private CircularProgressView progressBar;

    public TurndownView(Context context) {
        this(context, null);
    }

    public TurndownView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TurndownView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(11)
    @SuppressLint("SetJavaScriptEnabled")
    private void init() {
        // default browser is not called.
        setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                //super.onPageStarted(view, url, favicon);
                if (progressBar != null) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setIndeterminate(false);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                sendScriptAction();
            }

            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    String url_string = request.getUrl().toString();
                    boolean ad;
                    if (!loadedUrls.containsKey(url_string)) {
                        ad = AdBlocker.isAd(request.getUrl());
                        loadedUrls.put(url_string, ad);
                    } else {
                        ad = loadedUrls.get(url_string);
                    }
                    return ad ? AdBlocker.createEmptyResource() : super.shouldInterceptRequest(view, request);
                }

                return super.shouldInterceptRequest(view, request);
            }

        });

        setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d(Constants.LOG, consoleMessage.message() + " -- From line "
                        + consoleMessage.lineNumber() + " of "
                        + consoleMessage.sourceId());
                return super.onConsoleMessage(consoleMessage);
            }

            @Override
            public void onProgressChanged(WebView view, int progress) {
                if (progressBar != null) {
                    progressBar.setProgress(progress);
                    if (progress == 100) {
                        progressBar.setIndeterminate(true);
                    }
                }
            }
        });

        JSInterface = new JavaScriptInterface();
        addJavascriptInterface(JSInterface, "JSInterface");

        getSettings().setJavaScriptEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
    }

    private void sendScriptAction() {
        try {
            InputStream input = getContext().getAssets().open("html/js/turndown.js");
            byte[] buffer = new byte[input.available()];
            input.read(buffer);
            input.close();

            String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);
            evaluateJavascript("(function() {" +
                    "var parent = document.getElementsByTagName('head').item(0);" +
                    "var script = document.createElement('script');" +
                    "script.type = 'text/javascript';" +
                    // Tell the browser to BASE64-decode the string into your script !!!
                    "script.innerHTML = window.atob('" + encoded + "');" +
                    "parent.appendChild(script)" +
                    "})()", new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String s) {
                    runScript();
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            if (mOnMarkdownBodyListener != null) mOnMarkdownBodyListener.LoadFailed();
        }
    }

    private void runScript() {
        evaluateJavascript("(function() {" +
                "var elements = [ " +
                "\"document.querySelector('article') ? document.querySelector('article').innerHTML\",\n" +
                "\"document.querySelector('#article_item') ? document.querySelector('#article_item').innerHTML\",\n" +
                "\"document.querySelector('*[role=\\\"article\\\"]') ? document.querySelector('*[role=\\\"article\\\"]').innerHTML\",\n" +
                "\"document.querySelector('*[itemprop=\\\"articleBody\\\"]') ? document.querySelector('*[itemprop=\\\"articleBody\\\"]').parentNode.innerHTML\",\n" +
                "\"document.querySelector('*[role=\\\"main\\\"]') ? document.querySelector('*[role=\\\"main\\\"]').innerHTML\",\n" +
                "\"document.querySelector('.post-container') ? document.querySelector('.post-container').innerHTML\",\n" +
                "\"document.querySelector('.content-body') ? document.querySelector('.content-body').innerHTML\",\n" +
                "\"document.querySelector('div[itemtype=\\\"http://schema.org/Question\\\"]') ? document.querySelector('div[itemtype=\\\"http://schema.org/Question\\\"]').innerHTML\",\n" +
                "\"document.querySelector('p + p + p + p') ? document.querySelector('p + p + p + p').parentNode.innerHTML\",\n" +
                "\"document.querySelector('.repository-content .file > .data') ? document.querySelector('.repository-content .file > .data').innerHTML\",\n" +
                "\"document.querySelector('body').innerHTML\"\n" +
                "];" +
                "var body_extract = elements.join(' : ');"+
                "var turndownService = new TurndownService({" +
                "hr: \"---\"," +
                "headingStyle: \"atx\", " +
                "bulletListMarker: \"*\", " +
                "codeBlockStyle: \"fenced\" " +
                "});\n" +
                "turndownService.addRule('div', { " +
                    "filter: ['div'], " +
                    "replacement: function(content) { " +
                    " return '\\n' + content + '\\n'; }" +
                "});" +
                "turndownService.addRule('shareUrl', { " +
                    "filter: function (node, options) { " +
                        "return (node.nodeName === 'A' && " +
                            "node.getAttribute('href') && " +
                                "(node.getAttribute('href').search(/(facebook|twitter)\\.com\\/share/) >= 0 || node.getAttribute('href').search(/whatsapp:\\/\\//) >= 0)" +
                        ");" +
                    "}, " +
                    "replacement: function(content) { " +
                    "return ''; }" +
                "});" +
                "turndownService.addRule('script', { " +
                    "filter: ['script', 'style', 'noscript', 'form', 'nav', 'iframe', 'input', 'header', 'footer'], " +
                    "replacement: function(content) { " +
                    "return ''; }" +
                "});"+
                "var body_content = eval(body_extract);"+
                "var markdown = turndownService.turndown(body_content.replace('(\\n)+', ' '));" +
                "var new_md = markdown.replace(/\\n \\n/gi, '\\n\\n');" +
                "new_md = new_md.replace(/\\n{3,}/gi, '\\n\\n');" +
                "new_md = new_md.replace(/(!\\[\\]\\(.+?\\))(\\s*\\1+)/gi, '$1');\n" +
                "new_md = new_md.replace(/(\\[!\\[.*?\\].+?\\]\\(.+?\\))/gi, '\\n$1\\n');\n" +
                "new_md = new_md.replace(/\\]\\(\\/\\//gi, '](http://');\n" +
                "return new_md;"+
                "})()", new ValueCallback<String>() {

            @Override
            public void onReceiveValue(String s) {
                s = s.replaceAll("\\\\r\\\\n|\\\\r|\\\\n", "\n");
                s = s.replaceAll("(^[\\s\"])|([\\s\"]$)", "");
                s = s.trim();
                if (mOnMarkdownBodyListener != null) mOnMarkdownBodyListener.MarkdownBody(s);
            }
        });
    }

    public TurndownView setHtmlBodyCallback(OnMarkdownBodyListener mOnHTMLBodyListener) {
        this.mOnMarkdownBodyListener = mOnHTMLBodyListener;
        return this;
    }

    public TurndownView setProgressBar(CircularProgressView progressBar) {
        this.progressBar = progressBar;
        return this;
    }

    public interface OnMarkdownBodyListener {
        void MarkdownBody(String note_body);
        void LoadFailed();
    }

    public class JavaScriptInterface {

        JavaScriptInterface() { }

        @android.webkit.JavascriptInterface
        public void body(String note_body) {
            if (mOnMarkdownBodyListener != null) mOnMarkdownBodyListener.MarkdownBody(note_body);
        }
    }
}
