package com.stripe.android.financialconnections.lite

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.annotation.RestrictTo
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.FinancialConnectionsSheetResultCallback
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetForDataLauncher
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetLauncher
import kotlinx.parcelize.Parcelize

/**
 * A drop in class to present the Financial Connections Auth Flow.
 *
 * This *must* be called unconditionally, as part of initialization path,
 * typically as a field initializer of an Activity or Fragment.
 */
internal class FinancialConnectionsSheetLite internal constructor(
    private val financialConnectionsSheetLauncher: FinancialConnectionsSheetLauncher
) {

    /**
     * Configuration for a [FinancialConnectionsSheetLite]
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
     * Present the [FinancialConnectionsSheetLite].
     *
     * @param configuration the [FinancialConnectionsSheetLite] configuration
     */
    fun present(
        configuration: Configuration
    ) {
        financialConnectionsSheetLauncher.present(
            configuration = configuration.toInternal(),
            elementsSessionContext = null,
        )
    }

    private fun Configuration.toInternal(): FinancialConnectionsSheetConfiguration {
        return FinancialConnectionsSheetConfiguration(
            financialConnectionsSessionClientSecret = financialConnectionsSessionClientSecret,
            publishableKey = publishableKey,
            stripeAccountId = stripeAccountId
        )
    }

    companion object {
        /**
         * Constructor to be used when launching the [FinancialConnectionsSheetLite] from an Activity.
         *
         * @param activity  the Activity that is presenting the [FinancialConnectionsSheetLite].
         * @param callback  called with the result of the connections session after the connections sheet is dismissed.
         */
        @JvmStatic
        fun create(
            activity: ComponentActivity,
            callback: FinancialConnectionsSheetResultCallback
        ): FinancialConnectionsSheetLite {
            return FinancialConnectionsSheetLite(
                FinancialConnectionsSheetForDataLauncher(
                    activity = activity,
                    callback = callback,
                    intentBuilder = intentBuilder(activity)
                )
            )
        }
    }
}

/**
 * Creates an [Intent] to launch the [FinancialConnectionsSheetLiteActivity].
 *
 * @param context the context to use for creating the intent
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun intentBuilder(context: Context): (FinancialConnectionsSheetActivityArgs) -> Intent =
    { args: FinancialConnectionsSheetActivityArgs ->
        FinancialConnectionsSheetLiteActivity.intent(
            context = context,
            args = args
        )
    }
