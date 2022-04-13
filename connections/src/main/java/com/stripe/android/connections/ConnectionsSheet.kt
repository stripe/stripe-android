package com.stripe.android.connections

import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import com.stripe.android.connections.launcher.ConnectionsSheetForTokenLauncher
import kotlinx.parcelize.Parcelize

/**
 * A drop in class to present the Link Account Session Auth Flow.
 *
 * This *must* be called unconditionally, as part of initialization path,
 * typically as a field initializer of an Activity or Fragment.
 */
class ConnectionsSheet private constructor(
    private val connectionsSheetLauncher: ConnectionsSheetLauncher
) {
    /**
     * Configuration for a Connections Sheet
     *
     * @param linkAccountSessionClientSecret the client secret for the Link Account Session
     * @param publishableKey the Stripe publishable key
     */
    @Parcelize
    data class Configuration(
        val linkAccountSessionClientSecret: String,
        val publishableKey: String,
    ) : Parcelable

    /**
     * Present the connections sheet.
     *
     * @param configuration the connections sheet configuration
     */
    fun present(
        configuration: Configuration
    ) {
        connectionsSheetLauncher.present(configuration)
    }

    companion object {
        /**
         * Constructor to be used when launching the connections sheet from an Activity.
         *
         * @param activity  the Activity that is presenting the connections sheet.
         * @param callback  called with the result of the connections session after the connections sheet is dismissed.
         */
        fun create(
            activity: ComponentActivity,
            callback: ConnectionsSheetResultCallback
        ): ConnectionsSheet {
            return ConnectionsSheet(
                DefaultConnectionsSheetLauncher(activity, callback)
            )
        }

        /**
         * Constructor to be used when launching the payment sheet from a Fragment.
         *
         * @param fragment the Fragment that is presenting the payment sheet.
         * @param callback called with the result of the payment after the payment sheet is dismissed.
         */
        fun create(
            fragment: Fragment,
            callback: ConnectionsSheetResultCallback
        ): ConnectionsSheet {
            return ConnectionsSheet(
                DefaultConnectionsSheetLauncher(fragment, callback)
            )
        }


        /**
         * Constructor to be used when launching the connections sheet from an Activity.
         *
         * @param activity  the Activity that is presenting the connections sheet.
         * @param callback  called with the result of the connections session after the connections sheet is dismissed.
         */
        fun createForToken(
            activity: ComponentActivity,
            callback: (ConnectionsSheetForTokenResult) -> Unit
        ): ConnectionsSheet {
            return ConnectionsSheet(
                ConnectionsSheetForTokenLauncher(activity, callback)
            )
        }

        /**
         * Constructor to be used when launching the payment sheet from a Fragment.
         *
         * @param fragment the Fragment that is presenting the payment sheet.
         * @param callback called with the result of the payment after the payment sheet is dismissed.
         */
        fun createForToken(
            fragment: Fragment,
            callback: (ConnectionsSheetForTokenResult) -> Unit
        ): ConnectionsSheet {
            return ConnectionsSheet(
                ConnectionsSheetForTokenLauncher(fragment, callback)
            )
        }
    }
}
