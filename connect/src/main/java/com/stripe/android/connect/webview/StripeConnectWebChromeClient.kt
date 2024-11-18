package com.stripe.android.connect.webview

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.lifecycle.LifecycleCoroutineScope
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.PrivateBetaConnectSDK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A [WebChromeClient] that handles permission requests for the Stripe Connect WebView.
 * This should be used in conjunction with [StripeConnectWebViewClient].
 */
@OptIn(PrivateBetaConnectSDK::class)
internal class StripeConnectWebChromeClient(
    private val context: Context,
    private val embeddedComponentManager: EmbeddedComponentManager,
    private val viewScope: () -> LifecycleCoroutineScope,
) : WebChromeClient() {

    private val inProgressRequests: MutableMap<PermissionRequest, Job> = mutableMapOf()

    override fun onPermissionRequest(request: PermissionRequest) {
        // we only care about camera permissions at this time (video/audio)
        val permissionsRequested = request.resources.filter {
            it in listOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE, PermissionRequest.RESOURCE_AUDIO_CAPTURE)
        }.toTypedArray()
        if (permissionsRequested.isEmpty()) {
            request.deny() // no supported permissions were requested, so reject the request
            return
        }

        if (checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            request.grant(permissionsRequested)
        } else {
            val job = viewScope().launch {
                val isGranted = embeddedComponentManager.requestCameraPermission()
                withContext(Dispatchers.Main) {
                    if (isGranted) {
                        request.grant(permissionsRequested)
                    } else {
                        request.deny()
                    }
                }
                inProgressRequests.remove(request)
            }
            inProgressRequests[request] = job
        }
    }

    override fun onPermissionRequestCanceled(request: PermissionRequest?) {
        if (request == null) return
        inProgressRequests.remove(request)?.also { it.cancel() }
    }
}
