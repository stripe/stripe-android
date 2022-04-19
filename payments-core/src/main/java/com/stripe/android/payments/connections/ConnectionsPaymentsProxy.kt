package com.stripe.android.payments.connections

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.stripe.android.BuildConfig
import com.stripe.android.financialconnections.ConnectionsSheet
import com.stripe.android.financialconnections.ConnectionsSheetResult
import com.stripe.android.payments.connections.reflection.DefaultIsConnectionsAvailable
import com.stripe.android.payments.connections.reflection.IsConnectionsAvailable

/**
 * Proxy to access connections code safely in payments.
 *
 */
internal interface ConnectionsPaymentsProxy {
    fun present(
        linkAccountSessionClientSecret: String,
        publishableKey: String
    )

    companion object {
        fun create(
            fragment: Fragment,
            onComplete: (ConnectionsSheetResult) -> Unit,
            provider: () -> ConnectionsPaymentsProxy = {
                DefaultConnectionsPaymentsProxy(ConnectionsSheet.create(fragment, onComplete))
            },
            isConnectionsAvailable: IsConnectionsAvailable = DefaultIsConnectionsAvailable()
        ): ConnectionsPaymentsProxy {
            return if (isConnectionsAvailable()) {
                provider()
            } else {
                UnsupportedConnectionsPaymentsProxy()
            }
        }

        fun create(
            activity: AppCompatActivity,
            onComplete: (ConnectionsSheetResult) -> Unit,
            provider: () -> ConnectionsPaymentsProxy = {
                DefaultConnectionsPaymentsProxy(ConnectionsSheet.create(activity, onComplete))
            },
            isConnectionsAvailable: IsConnectionsAvailable = DefaultIsConnectionsAvailable()
        ): ConnectionsPaymentsProxy {
            return if (isConnectionsAvailable()) {
                provider()
            } else {
                UnsupportedConnectionsPaymentsProxy()
            }
        }
    }
}

internal class DefaultConnectionsPaymentsProxy(
    private val connectionsSheet: ConnectionsSheet
) : ConnectionsPaymentsProxy {
    override fun present(
        linkAccountSessionClientSecret: String,
        publishableKey: String
    ) {
        connectionsSheet.present(
            ConnectionsSheet.Configuration(
                linkAccountSessionClientSecret,
                publishableKey
            )
        )
    }
}

internal class UnsupportedConnectionsPaymentsProxy : ConnectionsPaymentsProxy {
    override fun present(linkAccountSessionClientSecret: String, publishableKey: String) {
        if (BuildConfig.DEBUG) {
            throw IllegalStateException(
                "Missing connections dependency, please add it to your apps build.gradle"
            )
        }
    }
}
