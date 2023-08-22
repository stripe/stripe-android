package com.stripe.android.core.browser.customtabs

import android.content.ComponentName
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import java.lang.ref.WeakReference

/**
 * Implementation for the [CustomTabsServiceConnection] that avoids leaking the callback.
 */
internal class ServiceConnection(
    connectionCallback: ServiceConnectionCallback
) : CustomTabsServiceConnection() {
    private val callbackReference: WeakReference<ServiceConnectionCallback>

    init {
        callbackReference = WeakReference(connectionCallback)
    }

    override fun onCustomTabsServiceConnected(
        name: ComponentName,
        client: CustomTabsClient
    ) {
        callbackReference.get()?.onServiceConnected(client)
    }

    override fun onServiceDisconnected(name: ComponentName) {
        callbackReference.get()?.onServiceDisconnected()
    }
}

/**
 * Callback for events when connecting and disconnecting from Custom Tabs Service.
 */
internal interface ServiceConnectionCallback {
    fun onServiceConnected(client: CustomTabsClient)
    fun onServiceDisconnected()
}
