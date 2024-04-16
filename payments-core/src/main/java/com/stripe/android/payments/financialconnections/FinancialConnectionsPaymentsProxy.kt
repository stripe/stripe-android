package com.stripe.android.payments.financialconnections

import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.BuildConfig
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetForDataLauncher
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetLauncher

/**
 * Proxy to access financial connections code safely in payments.
 *
 */
internal interface FinancialConnectionsPaymentsProxy {
    fun present(
        financialConnectionsSessionClientSecret: String,
        publishableKey: String,
        stripeAccountId: String?
    )

    companion object {

        fun createForInstantDebits(
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
                TODO("Instant Debits not implemented yet.")
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
        stripeAccountId: String?
    ) {
        launcher.present(
            FinancialConnectionsSheet.Configuration(
                financialConnectionsSessionClientSecret,
                publishableKey,
                stripeAccountId
            )
        )
    }
}

internal class UnsupportedFinancialConnectionsPaymentsProxy : FinancialConnectionsPaymentsProxy {
    override fun present(
        financialConnectionsSessionClientSecret: String,
        publishableKey: String,
        stripeAccountId: String?
    ) {
        if (BuildConfig.DEBUG) {
            throw IllegalStateException(
                "Missing financial-connections dependency, please add it to your apps build.gradle"
            )
        }
    }
}
