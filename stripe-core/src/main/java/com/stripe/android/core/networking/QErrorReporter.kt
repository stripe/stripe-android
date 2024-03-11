package com.stripe.android.core.networking

import javax.inject.Inject

class QErrorReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory
) : ErrorReporter {
    override fun report(error: ErrorReporter.ErrorEvent, errorCode : Int?) {
        val additionalParams = mapOf("errorCode" to errorCode.toString())
        analyticsRequestExecutor.executeAsync(analyticsRequestFactory.createRequest(error, additionalParams))
    }
}