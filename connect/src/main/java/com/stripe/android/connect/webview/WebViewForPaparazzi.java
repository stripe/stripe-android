package com.stripe.android.connect.webview;

import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;

/**
 * Partial redeclaration of WebView's interface to workaround tests failing to compile with
 * Paparazzi enabled. See [issue](https://github.com/cashapp/paparazzi/issues/1058).
 */
interface WebViewForPaparazzi {
    WebViewClient getWebViewClient();

    @Nullable
    WebChromeClient getWebChromeClient();

    WebSettings getSettings();
}
