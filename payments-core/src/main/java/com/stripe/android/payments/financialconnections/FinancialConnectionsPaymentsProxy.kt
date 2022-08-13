package com.stripe.android.payments.financialconnections

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.stripe.android.BuildConfig
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult

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
        fun create(
            fragment: Fragment,
            onComplete: (FinancialConnectionsSheetResult) -> Unit,
            provider: () -> FinancialConnectionsPaymentsProxy = {
                DefaultFinancialConnectionsPaymentsProxy(
                    FinancialConnectionsSheet.create(
                        fragment,
                        onComplete
                    )
                )
            },
            isFinancialConnectionsAvailable: IsFinancialConnectionsAvailable = DefaultIsFinancialConnectionsAvailable()
        ): FinancialConnectionsPaymentsProxy {
            return if (isFinancialConnectionsAvailable()) {
                provider()
            } else {
                UnsupportedFinancialConnectionsPaymentsProxy()
            }
        }

        fun create(
            activity: AppCompatActivity,
            onComplete: (FinancialConnectionsSheetResult) -> Unit,
            provider: () -> FinancialConnectionsPaymentsProxy = {
                DefaultFinancialConnectionsPaymentsProxy(
                    FinancialConnectionsSheet.create(
                        activity,
                        onComplete
                    )
                )
            },
            isFinancialConnectionsAvailable: IsFinancialConnectionsAvailable = DefaultIsFinancialConnectionsAvailable()
        ): FinancialConnectionsPaymentsProxy {
            return if (isFinancialConnectionsAvailable()) {
                provider()
            } else {
                UnsupportedFinancialConnectionsPaymentsProxy()
            }
        }
    }
}

internal class DefaultFinancialConnectionsPaymentsProxy(
    private val financialConnectionsSheet: FinancialConnectionsSheet
) : FinancialConnectionsPaymentsProxy {
    override fun present(
        financialConnectionsSessionClientSecret: String,
        publishableKey: String,
        stripeAccountId: String?
    ) {
        financialConnectionsSheet.present(
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
