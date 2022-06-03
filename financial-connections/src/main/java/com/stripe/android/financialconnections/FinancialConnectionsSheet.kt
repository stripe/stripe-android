package com.stripe.android.financialconnections

import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.fragment.app.Fragment
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetForDataContract
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetForDataLauncher
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetForTokenContract
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetForTokenLauncher
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetLauncher
import kotlinx.parcelize.Parcelize

/**
 * A drop in class to present the Financial Connections Auth Flow.
 *
 * This *must* be called unconditionally, as part of initialization path,
 * typically as a field initializer of an Activity or Fragment.
 */
class FinancialConnectionsSheet internal constructor(
    private val financialConnectionsSheetLauncher: FinancialConnectionsSheetLauncher,
) {

    /**
     * Configuration for a [FinancialConnectionsSheet]
     *
     * @param financialConnectionsSessionClientSecret the session client secret
     * @param publishableKey the Stripe publishable key
     */
    @Parcelize
    data class Configuration(
        val financialConnectionsSessionClientSecret: String,
        val publishableKey: String,
    ) : Parcelable

    /**
     * Present the [FinancialConnectionsSheet].
     *
     * @param configuration the [FinancialConnectionsSheet] configuration
     */
    fun present(
        configuration: Configuration
    ) {
        financialConnectionsSheetLauncher.present(configuration)
    }

    companion object {
        /**
         * Constructor to be used when launching the [FinancialConnectionsSheet] from an Activity.
         *
         * @param activity  the Activity that is presenting the [FinancialConnectionsSheet].
         * @param callback  called with the result of the connections session after the connections sheet is dismissed.
         */
        fun create(
            activity: ComponentActivity,
            callback: FinancialConnectionsSheetResultCallback
        ): FinancialConnectionsSheet {
            return FinancialConnectionsSheet(
                FinancialConnectionsSheetForDataLauncher(activity, callback)
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
            callback: FinancialConnectionsSheetResultCallback
        ): FinancialConnectionsSheet {
            return FinancialConnectionsSheet(
                FinancialConnectionsSheetForDataLauncher(fragment, callback)
            )
        }

        /**
         * Create a [FinancialConnectionsSheet] used for Jetpack Compose.
         *
         * This API uses Compose specific API [rememberLauncherForActivityResult] to register a
         * [androidx.activity.result.ActivityResultLauncher] into the current activity,
         * so it should be called as part of Compose initialization path.
         */
        @Composable
        fun createComposable(
            callback: (FinancialConnectionsSheetResult) -> Unit
        ): FinancialConnectionsSheet = FinancialConnectionsSheet(
            FinancialConnectionsSheetForDataLauncher(
                rememberLauncherForActivityResult(
                    FinancialConnectionsSheetForDataContract()
                ) { callback(it) }
            )
        )

        /**
         * Constructor to be used when launching the connections sheet from an Activity.
         *
         * @param activity  the Activity that is presenting the connections sheet.
         * @param callback  called with the result of the connections session after the connections sheet is dismissed.
         */
        fun createForBankAccountToken(
            activity: ComponentActivity,
            callback: (FinancialConnectionsSheetForTokenResult) -> Unit
        ): FinancialConnectionsSheet {
            return FinancialConnectionsSheet(
                FinancialConnectionsSheetForTokenLauncher(activity, callback)
            )
        }

        /**
         * Constructor to be used when launching the payment sheet from a Fragment.
         *
         * @param fragment the Fragment that is presenting the payment sheet.
         * @param callback called with the result of the payment after the payment sheet is dismissed.
         */
        fun createForBankAccountToken(
            fragment: Fragment,
            callback: (FinancialConnectionsSheetForTokenResult) -> Unit
        ): FinancialConnectionsSheet {
            return FinancialConnectionsSheet(
                FinancialConnectionsSheetForTokenLauncher(fragment, callback)
            )
        }

        /**
         * Create a [FinancialConnectionsSheet] used for Jetpack Compose.
         *
         * This API uses Compose specific API [rememberLauncherForActivityResult] to register a
         * [androidx.activity.result.ActivityResultLauncher] into the current activity,
         * so it should be called as part of Compose initialization path.
         */
        @Composable
        private fun createComposableForToken(
            callback: (FinancialConnectionsSheetForTokenResult) -> Unit
        ): FinancialConnectionsSheet = FinancialConnectionsSheet(
            FinancialConnectionsSheetForTokenLauncher(
                rememberLauncherForActivityResult(
                    FinancialConnectionsSheetForTokenContract()
                ) { callback(it) }
            )
        )
    }
}
