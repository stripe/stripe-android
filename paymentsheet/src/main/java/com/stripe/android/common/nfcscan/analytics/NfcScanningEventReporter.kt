package com.stripe.android.common.nfcscan.analytics

import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.core.utils.mapOfDurationInSeconds
import javax.inject.Inject

internal interface NfcScanningEventReporter {
    /**
     * NFC scan flow has been started.
     */
    fun onNfcScanStarted()

    /**
     * User attempts to scan their card using NFC.
     */
    fun onNfcScanAttemptStarted()

    /**
     * User attempt to scan their card with NFC succeeded.
     */
    fun onNfcScanAttemptSucceeded()

    /**
     * User attempt to scan their card with NFC failed.
     */
    fun onNfcScanAttemptFailed()

    /**
     * NFC scan flow completed successfully.
     */
    fun onNfcScanSucceeded()

    /**
     * NFC scan was canceled by the user.
     */
    fun onNfcScanCancelled()
}

internal class DefaultNfcScanningEventReporter @Inject constructor(
    private val durationProvider: DurationProvider,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    @EventPrefix private val eventPrefix: String,
) : NfcScanningEventReporter {
    override fun onNfcScanStarted() {
        durationProvider.start(DurationProvider.Key.NfcScan)
        fireEvent(eventName = SCAN_STARTED_EVENT_NAME)
    }

    override fun onNfcScanSucceeded() {
        val duration = durationProvider.end(DurationProvider.Key.NfcScan)
        fireEvent(eventName = SCAN_SUCCESS_EVENT_NAME, additionalParams = duration.mapOfDurationInSeconds())
    }

    override fun onNfcScanAttemptStarted() {
        durationProvider.start(DurationProvider.Key.NfcScanAttempt)
        fireEvent(eventName = SCAN_ATTEMPT_STARTED_EVENT_NAME)
    }

    override fun onNfcScanAttemptSucceeded() {
        val duration = durationProvider.end(DurationProvider.Key.NfcScanAttempt)
        fireEvent(eventName = SCAN_ATTEMPT_SUCCEEDED_EVENT_NAME, additionalParams = duration.mapOfDurationInSeconds())
    }

    override fun onNfcScanAttemptFailed() {
        val duration = durationProvider.end(DurationProvider.Key.NfcScanAttempt)
        fireEvent(eventName = SCAN_ATTEMPT_FAILED_EVENT_NAME, additionalParams = duration.mapOfDurationInSeconds())
    }

    override fun onNfcScanCancelled() {
        durationProvider.end(DurationProvider.Key.NfcScan)
        fireEvent(eventName = SCAN_CANCELED_EVENT_NAME)
    }

    private fun fireEvent(
        eventName: String,
        additionalParams: Map<String, Any> = emptyMap()
    ) {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createRequest(
                event = object : AnalyticsEvent {
                    override val eventName: String
                        get() = eventPrefix + eventName
                },
                additionalParams = additionalParams,
            )
        )
    }

    private companion object {
        const val SCAN_STARTED_EVENT_NAME = "nfc_scan_started"
        const val SCAN_SUCCESS_EVENT_NAME = "nfc_scan_success"
        const val SCAN_CANCELED_EVENT_NAME = "nfc_scan_canceled"
        const val SCAN_ATTEMPT_STARTED_EVENT_NAME = "nfc_scan_attempt_started"
        const val SCAN_ATTEMPT_SUCCEEDED_EVENT_NAME = "nfc_scan_attempt_succeeded"
        const val SCAN_ATTEMPT_FAILED_EVENT_NAME = "nfc_scan_attempt_failed"
    }
}
