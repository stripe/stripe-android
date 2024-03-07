package com.stripe.android.financialconnections.exception

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.toEventParams
import javax.inject.Inject

internal class FinancialConnectionsErrorHandler @Inject constructor(
    private val analyticsTracker: FinancialConnectionsAnalyticsTracker,
) {

    fun setup(lifecycleOwner: LifecycleOwner) {
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val params = error.toEventParams(extraMessage = null)
                analyticsTracker.track(FinancialConnectionsAnalyticsEvent.UncaughtException(params))
            } finally {
                originalHandler?.uncaughtException(thread, error)
            }
        }

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    Thread.setDefaultUncaughtExceptionHandler(originalHandler)
                    super.onDestroy(owner)
                }
            }
        )
    }
}
