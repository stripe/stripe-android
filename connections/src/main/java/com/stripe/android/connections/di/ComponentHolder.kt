package com.stripe.android.connections.di

/**
 * Global instance that holds application component with entire DI graph.
 */

private var _appComponent: ConnectionsAppComponent? = null

internal var appComponent: ConnectionsAppComponent
    get() = requireNotNull(_appComponent)
    set(value) {
        _appComponent = value
    }




