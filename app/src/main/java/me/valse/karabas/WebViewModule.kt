package me.valse.karabas

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.webkit.ConsoleMessage
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class WebViewModule(val context: Context, val webView: WebView) {

    private var webViewProxy = WebViewProxy(context);
    //private var webViewBridge = WebViewBridge(context);


    /*var mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            webView.evaluateJavascript("document.querySelector('audio,video').play()", null)
        }

        override fun onPause() {
            webView.evaluateJavascript("document.querySelector('audio,video').pause()", null)
        }

        // Добавьте другие методы по необходимости
    }*/

    init {
        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                if (url.contains("youtube.com") || url.contains("google.com")) {
                    // Открыть в WebView, а не в браузере
                    view?.loadUrl(url)
                    return true
                }
                return false
            }

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                return webViewProxy.shouldInterceptRequest(view, request)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {

            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                Log.d("WebView", consoleMessage.message())
                return true
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                val resources = request?.resources
                resources?.forEach { resource ->
                    if (PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID == resource) {
                        request.grant(resources)
                        return
                    }
                }
                super.onPermissionRequest(request)
            }
        }

        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }


        /* // Разрешаем доступ к файловой системе для кеширования
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.setWebContentsDebuggingEnabled(true);
        }*/

        //webView.addJavascriptInterface(webViewBridge, "WebViewBridge");
        webView.loadUrl("https://web.valse.me/")

//        // Создаем MediaController для управления
//        mediaController = MediaControllerCompat(this, mediaSession).apply {
//            MediaControllerCompat.setMediaController(this@WebViewModule, this)
//        }
    }

    fun onBack(): Boolean {
        webView.evaluateJavascript("window.vaBack()", null)
        return false;
    }

    fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return false;
    }

    fun cleanupTempFiles() {
        webViewProxy.cleanupTempFiles();
    }
}