package com.stripe.android.shoppay

import android.app.Application
import android.content.Context
import android.util.Log
import android.webkit.WebView
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.webkit.WebViewAssetLoader
import com.stripe.android.shoppay.ShopPayActivity.Companion.getArgs
import com.stripe.android.shoppay.bridge.BridgeHandler
import com.stripe.android.shoppay.di.DaggerShopPayComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import java.io.BufferedReader
import javax.inject.Inject

internal class ShopPayViewModel @Inject constructor(
    val bridgeHandler: BridgeHandler
) : ViewModel() {
    private val _popupWebView = MutableStateFlow<WebView?>(null)
    val popupWebView: StateFlow<WebView?> = _popupWebView

    val showPopup: StateFlow<Boolean> = _popupWebView.mapLatest {
        it != null
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = false
    )

    fun setWebView(webView: WebView) {
        webView.loadUrl("https://pay.stripe.com/assets/www/index.html")
    }

    fun setPopupWebView(webView: WebView?) {
        _popupWebView.value = webView
    }

    fun closePopup() {
        _popupWebView.value = null
    }

    fun assetLoader(context: Context): WebViewAssetLoader {
        return WebViewAssetLoader.Builder()
            .setDomain("pay.stripe.com")
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()
    }

    fun onPageLoaded(view: WebView, url: String) {
        val jsBridge = view.context.assets.open("www/native.js")
            .bufferedReader()
            .use(BufferedReader::readText)

        view.evaluateJavascript(jsBridge, null)

        if (url.contains("pay.stripe.com")) {
            view.evaluateJavascript("initializeApp()") {
                Log.d("WebViewBridge", "initializeApp() => $it")
            }
        }
    }

    companion object {
        fun factory(savedStateHandle: SavedStateHandle? = null): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val handle: SavedStateHandle = savedStateHandle ?: createSavedStateHandle()
                val app = this[APPLICATION_KEY] as Application
                val args: ShopPayArgs = getArgs(handle) ?: throw IllegalArgumentException("No args found")
                DaggerShopPayComponent
                    .builder()
                    .context(app)
                    .configuration(args.shopPayConfiguration)
                    .publishableKey(args.publishableKey)
                    .build()
                    .viewModel
            }
        }
    }
}
