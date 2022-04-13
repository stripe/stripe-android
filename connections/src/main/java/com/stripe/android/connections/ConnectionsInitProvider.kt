package com.stripe.android.connections

import com.stripe.android.connections.di.DaggerConnectionsAppComponent
import com.stripe.android.connections.di.appComponent
import com.stripe.android.core.injection.InitProvider

/**
 * Simplified content provider that gets called on Application#onCreate.
 *
 * Needs to be declared on the manifest, that will get merged with the SDK consumer app.
 */
class ConnectionsInitProvider : InitProvider() {
    override fun onCreate(): Boolean {
        appComponent = DaggerConnectionsAppComponent.builder()
            .application(application)
            .build()
        return true
    }
}