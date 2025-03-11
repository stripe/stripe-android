package com.stripe.android.financialconnections

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetForDataLauncher
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
    private val financialConnectionsSheetLauncher: FinancialConnectionsSheetLauncher
) {

    /**
     * Configuration for a [FinancialConnectionsSheet]
     *
     * @param financialConnectionsSessionClientSecret the session client secret
     * @param publishableKey the Stripe publishable key
     * @param stripeAccountId (optional) connected account ID
     */
    @Parcelize
    data class Configuration(
        val financialConnectionsSessionClientSecret: String,
        val publishableKey: String,
        val stripeAccountId: String? = null
    ) : Parcelable

    /**
     * Present the [FinancialConnectionsSheet].
     *
     * @param configuration the [FinancialConnectionsSheet] configuration
     */
    fun present(
        configuration: Configuration
    ) {
        financialConnectionsSheetLauncher.present(
            configuration = configuration.toInternal(),
            elementsSessionContext = null,
        )
    }

    private fun FinancialConnectionsSheet.Configuration.toInternal(): FinancialConnectionsSheetConfiguration {
        return FinancialConnectionsSheetConfiguration(
            financialConnectionsSessionClientSecret = financialConnectionsSessionClientSecret,
            publishableKey = publishableKey,
            stripeAccountId = stripeAccountId
        )
    }

    companion object {
        /**
         * Constructor to be used when launching the [FinancialConnectionsSheet] from an Activity.
         *
         * @param activity  the Activity that is presenting the [FinancialConnectionsSheet].
         * @param callback  called with the result of the connections session after the connections sheet is dismissed.
         */
        @JvmStatic
        fun create(
            activity: ComponentActivity,
            callback: FinancialConnectionsSheetResultCallback
        ): FinancialConnectionsSheet {
            return FinancialConnectionsSheet(
                FinancialConnectionsSheetForDataLauncher(
                    activity = activity,
                    callback = callback,
                    intentBuilder = intentBuilder(activity)
                )
            )
        }

        /**
         * Constructor to be used when launching the payment sheet from a Fragment.
         *
         * @param fragment the Fragment that is presenting the payment sheet.
         * @param callback called with the result of the payment after the payment sheet is dismissed.
         */
        @JvmStatic
        fun create(
            fragment: Fragment,
            callback: FinancialConnectionsSheetResultCallback
        ): FinancialConnectionsSheet {
            return FinancialConnectionsSheet(
                FinancialConnectionsSheetForDataLauncher(
                    fragment = fragment,
                    callback = callback,
                    intentBuilder = intentBuilder(fragment.requireContext())
                )
            )
        }

        /**
         * Constructor to be used when launching the connections sheet from an Activity.
         *
         * @param activity  the Activity that is presenting the connections sheet.
         * @param callback  called with the result of the connections session after the connections sheet is dismissed.
         */
        @JvmStatic
        fun createForBankAccountToken(
            activity: ComponentActivity,
            callback: FinancialConnectionsSheetResultForTokenCallback
        ): FinancialConnectionsSheet {
            return FinancialConnectionsSheet(
                FinancialConnectionsSheetForTokenLauncher(
                    activity = activity,
                    callback = callback,
                    intentBuilder = intentBuilder(activity)
                )
            )
        }

        /**
         * Constructor to be used when launching the payment sheet from a Fragment.
         *
         * @param fragment the Fragment that is presenting the payment sheet.
         * @param callback called with the result of the payment after the payment sheet is dismissed.
         */
        @JvmStatic
        fun createForBankAccountToken(
            fragment: Fragment,
            callback: FinancialConnectionsSheetResultForTokenCallback
        ): FinancialConnectionsSheet {
            return FinancialConnectionsSheet(
                FinancialConnectionsSheetForTokenLauncher(
                    fragment = fragment,
                    callback = callback,
                    intentBuilder = intentBuilder(fragment.requireContext())
                )
            )
        }
    }
}

fun intentBuilder(context: Context): (FinancialConnectionsSheetActivityArgs) -> Intent =
    { args: FinancialConnectionsSheetActivityArgs ->
        FinancialConnectionsSheetActivity.intent(
            context = context,
            args = args
        )
    }
