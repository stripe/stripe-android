package com.stripe.android.payments.financialconnections

import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.BuildConfig
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheet.ElementsSessionContext
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetForDataLauncher
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetForInstantDebitsLauncher
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetInstantDebitsResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetLauncher

/**
 * Proxy to access financial connections code safely in payments.
 *
 */
internal interface FinancialConnectionsPaymentsProxy {
    fun present(
        financialConnectionsSessionClientSecret: String,
        publishableKey: String,
        stripeAccountId: String?,
        elementsSessionContext: ElementsSessionContext?,
    )

    companion object {

        fun createForInstantDebits(
            activity: AppCompatActivity,
            onComplete: (FinancialConnectionsSheetInstantDebitsResult) -> Unit,
            provider: () -> FinancialConnectionsPaymentsProxy = {
                FinancialConnectionsLauncherProxy(
                    FinancialConnectionsSheetForInstantDebitsLauncher(
                        activity,
                        onComplete
                    )
                )
            },
            isFinancialConnectionsAvailable: IsFinancialConnectionsAvailable = DefaultIsFinancialConnectionsAvailable
        ): FinancialConnectionsPaymentsProxy {
            return if (isFinancialConnectionsAvailable()) {
                provider()
            } else {
                UnsupportedFinancialConnectionsPaymentsProxy()
            }
        }

        fun createForACH(
            activity: AppCompatActivity,
            onComplete: (FinancialConnectionsSheetResult) -> Unit,
            provider: () -> FinancialConnectionsPaymentsProxy = {
                FinancialConnectionsLauncherProxy(
                    FinancialConnectionsSheetForDataLauncher(
                        activity,
                        onComplete
                    )
                )
            },
            isFinancialConnectionsAvailable: IsFinancialConnectionsAvailable = DefaultIsFinancialConnectionsAvailable
        ): FinancialConnectionsPaymentsProxy {
            return if (isFinancialConnectionsAvailable()) {
                provider()
            } else {
                UnsupportedFinancialConnectionsPaymentsProxy()
            }
        }
    }
}

internal class FinancialConnectionsLauncherProxy<T : FinancialConnectionsSheetLauncher>(
    private val launcher: T
) : FinancialConnectionsPaymentsProxy {
    override fun present(
        financialConnectionsSessionClientSecret: String,
        publishableKey: String,
        stripeAccountId: String?,
        elementsSessionContext: ElementsSessionContext?,
    ) {
        launcher.present(
            configuration = FinancialConnectionsSheet.Configuration(
                financialConnectionsSessionClientSecret,
                publishableKey,
                stripeAccountId,
            ),
            elementsSessionContext = elementsSessionContext,
        )
    }
}

internal class UnsupportedFinancialConnectionsPaymentsProxy : FinancialConnectionsPaymentsProxy {
    override fun present(
        financialConnectionsSessionClientSecret: String,
        publishableKey: String,
        stripeAccountId: String?,
        elementsSessionContext: ElementsSessionContext?,
    ) {
        if (BuildConfig.DEBUG) {
            throw IllegalStateException(
                "Missing financial-connections dependency, please add it to your apps build.gradle"
            )
        }
    }
}
