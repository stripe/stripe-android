package com.stripe.android.identity.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.stripe.android.camera.CameraPermissionEnsureable
import com.stripe.android.identity.IdentityVerificationSheet.VerificationFlowResult
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.viewmodel.IdentityViewModel

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun VerificationWebViewScreen(
    identityViewModel: IdentityViewModel,
    verificationFlowFinishable: VerificationFlowFinishable
) {
    val verificationPageState by identityViewModel.verificationPage.observeAsState(Resource.loading())

    CheckVerificationPageAndCompose(
        verificationPageResource = verificationPageState,
        onError = {
            identityViewModel.errorCause.postValue(it)
            verificationFlowFinishable.finishWithResult(
                VerificationFlowResult.Failed(it)
            )
        }
    ) { verificationPage ->
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        // Essential JavaScript settings for camera functionality
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false

                        setSupportZoom(false)
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun getDefaultVideoPoster(): Bitmap {
                            // Return a transparent bitmap to prevent the default play button overlay while video (camera) is loading
                            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                        }

                        override fun onJsBeforeUnload(
                            view: WebView?,
                            url: String?,
                            message: String?,
                            result: JsResult
                        ): Boolean {
                            result.confirm()
                            return true
                        }

                        override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                            request?.let { permissionRequest ->
                                val requestedResources = permissionRequest.resources
                                if (requestedResources.contains(android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                                    val cameraPermissionEnsureable = context as? CameraPermissionEnsureable
                                    if (ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.CAMERA
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        permissionRequest.grant(arrayOf(android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                                    } else {
                                        cameraPermissionEnsureable?.ensureCameraPermission(
                                            onCameraReady = {
                                                permissionRequest.grant(arrayOf(android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                                            },
                                            onUserDeniedCameraPermission = {
                                                permissionRequest.deny()
                                            }
                                        )
                                    }
                                } else {
                                    permissionRequest.deny()
                                }
                            }
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest
                        ): Boolean {
                            val url = request.url.toString()

                            // Let WebView handle verification URLs internally
                            if (url.startsWith(verificationPage.fallbackUrl)) {
                                return false
                            }

                            // Open external URLs in custom tabs
                            CustomTabsIntent.Builder()
                                .setShowTitle(true)
                                .build()
                                .launchUrl(context, request.url)

                            return true
                        }

                        @RequiresApi(Build.VERSION_CODES.M)
                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            val errorMessage = "WebView error: ${error?.description}"
                            identityViewModel.identityAnalyticsRequestFactory.verificationFailed(
                                isFromFallbackUrl = true,
                                throwable = IllegalStateException(errorMessage)
                            )
                            verificationFlowFinishable.finishWithResult(
                                VerificationFlowResult.Failed(IllegalStateException(errorMessage))
                            )
                        }

                        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                            super.doUpdateVisitedHistory(view, url, isReload)
                            url?.let {
                                when {
                                    it.endsWith("/success") -> {
                                        identityViewModel.identityAnalyticsRequestFactory
                                            .verificationSucceeded(isFromFallbackUrl = true)
                                        verificationFlowFinishable.finishWithResult(
                                            VerificationFlowResult.Completed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            update = { webView ->
                webView.clearCache(true)
                webView.loadUrl(verificationPage.fallbackUrl)
            }
        )
    }
} 