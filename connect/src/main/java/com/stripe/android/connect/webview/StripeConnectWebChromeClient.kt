package com.stripe.android.connect.webview

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import androidx.core.content.ContextCompat.checkSelfPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * A [WebChromeClient] that handles permission requests for the Stripe Connect WebView.
 * This should be used in conjunction with [StripeConnectWebViewClient].
 */
internal class StripeConnectWebChromeClient(
    private val context: Context,
    private val requestPermissionFromUser: suspend (String) -> Boolean,
    private val permissionScopeBuilder: () -> CoroutineScope = { CoroutineScope(Dispatchers.Default) },
) : WebChromeClient() {

    private val inProgressRequests: MutableMap<PermissionRequest, CoroutineScope> = mutableMapOf()

    override fun onPermissionRequest(request: PermissionRequest) {
        if (!request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
            request.deny()
            return
        }

        if (checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            request.grant(request.resources)
        } else {
            val scope = permissionScopeBuilder().also {
                inProgressRequests[request] = it
            }
            scope.launch {
                val granted = requestPermissionFromUser(Manifest.permission.CAMERA)
                if (granted) {
                    request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                } else {
                    request.deny()
                }
                inProgressRequests.remove(request)
            }
        }
    }

    override fun onPermissionRequestCanceled(request: PermissionRequest?) {
        inProgressRequests[request]?.cancel()
    }
}