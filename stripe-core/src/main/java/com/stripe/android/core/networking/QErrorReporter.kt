package com.stripe.android.core.networking

import javax.inject.Inject

class QErrorReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory
) : ErrorReporter {
    override fun report(error: ErrorReporter.ErrorEvent) {
        analyticsRequestExecutor.executeAsync(analyticsRequestFactory.createRequest(error, additionalParams = emptyMap()))
    }
}