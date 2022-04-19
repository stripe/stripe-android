package com.stripe.android.payments.connections

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.stripe.android.BuildConfig
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.payments.connections.reflection.DefaultIsConnectionsAvailable
import com.stripe.android.payments.connections.reflection.IsConnectionsAvailable

/**
 * Proxy to access financial connections code safely in payments.
 *
 */
internal interface FinancialConnectionsPaymentsProxy {
    fun present(
        linkAccountSessionClientSecret: String,
        publishableKey: String
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
            isConnectionsAvailable: IsConnectionsAvailable = DefaultIsConnectionsAvailable()
        ): FinancialConnectionsPaymentsProxy {
            return if (isConnectionsAvailable()) {
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
            isConnectionsAvailable: IsConnectionsAvailable = DefaultIsConnectionsAvailable()
        ): FinancialConnectionsPaymentsProxy {
            return if (isConnectionsAvailable()) {
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
        linkAccountSessionClientSecret: String,
        publishableKey: String
    ) {
        financialConnectionsSheet.present(
            FinancialConnectionsSheet.Configuration(
                linkAccountSessionClientSecret,
                publishableKey
            )
        )
    }
}

internal class UnsupportedFinancialConnectionsPaymentsProxy : FinancialConnectionsPaymentsProxy {
    override fun present(linkAccountSessionClientSecret: String, publishableKey: String) {
        if (BuildConfig.DEBUG) {
            throw IllegalStateException(
                "Missing financial-connections dependency, please add it to your apps build.gradle"
            )
        }
    }
}
