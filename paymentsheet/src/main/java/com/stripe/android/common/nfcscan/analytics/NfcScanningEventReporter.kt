package com.stripe.android.common.nfcscan.analytics

import com.stripe.android.common.nfcscan.scanner.NfcScanningError
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.core.utils.mapOfDurationInSeconds
import javax.inject.Inject

internal interface NfcScanningEventReporter {
    /**
     * The NFC scan flow has been started and the user is presented with the NFC scanning UI which has the scanner
     * running in the background
     */
    fun onNfcScanStarted()

    /**
     * User attempts to scan their card using NFC, meaning the scanner has detected a readable NFC card placed
     * against the device.
     */
    fun onNfcScanAttemptStarted()

    /**
     * The user's attempt to scan their card with NFC succeeded meaning the scanner was able to produce valid card
     * details from the NFC card held against the device.
     */
    fun onNfcScanAttemptSucceeded()

    /**
     * User attempt to scan their card with NFC failed meaning the scanner was NOT able to produce valid card
     * details from the NFC card held against the device.
     *
     * @param error error that occurred within the NFC scanning flow
     */
    fun onNfcScanAttemptFailed(error: NfcScanningError)

    /**
     * NFC scan flow completed successfully meaning the user is being returned to the calling payment flow after
     * being shown that they successfully scanned valid card details.
     *
     * @param numberOfAttempts number of times the user attempted to scan their card with NFC
     */
    fun onNfcScanSucceeded(numberOfAttempts: Int)

    /**
     * The user has chosen to exit the NFC scanning flow without scanning valid card details.
     *
     * @param reason why there was an NFC flow cancellation
     * @param numberOfAttempts number of times the user attempted to scan their card with NFC
     */
    fun onNfcScanCancelled(
        reason: NfcScanCancellationReason,
        numberOfAttempts: Int,
    )
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

    override fun onNfcScanSucceeded(numberOfAttempts: Int) {
        val duration = durationProvider.end(DurationProvider.Key.NfcScan)
        fireEvent(
            eventName = SCAN_SUCCESS_EVENT_NAME,
            additionalParams = duration.mapOfDurationInSeconds() +
                mapOf(FIELD_NUMBER_OF_ATTEMPTS to numberOfAttempts),
        )
    }

    override fun onNfcScanAttemptStarted() {
        durationProvider.start(DurationProvider.Key.NfcScanAttempt)
        fireEvent(eventName = SCAN_ATTEMPT_STARTED_EVENT_NAME)
    }

    override fun onNfcScanAttemptSucceeded() {
        val duration = durationProvider.end(DurationProvider.Key.NfcScanAttempt)
        fireEvent(eventName = SCAN_ATTEMPT_SUCCEEDED_EVENT_NAME, additionalParams = duration.mapOfDurationInSeconds())
    }

    override fun onNfcScanAttemptFailed(error: NfcScanningError) {
        val duration = durationProvider.end(DurationProvider.Key.NfcScanAttempt)

        fireEvent(
            eventName = SCAN_ATTEMPT_FAILED_EVENT_NAME,
            additionalParams = duration.mapOfDurationInSeconds() +
                mapOf(FIELD_ERROR_CODE to error.errorCode) +
                error.parameters,
        )
    }

    override fun onNfcScanCancelled(
        reason: NfcScanCancellationReason,
        numberOfAttempts: Int,
    ) {
        val duration = durationProvider.end(DurationProvider.Key.NfcScan)
        fireEvent(
            eventName = SCAN_CANCELED_EVENT_NAME,
            additionalParams = duration.mapOfDurationInSeconds() +
                mapOf(
                    FIELD_CANCELLATION_REASON to reason.analyticsValue,
                    FIELD_NUMBER_OF_ATTEMPTS to numberOfAttempts,
                )
        )
    }

    private fun fireEvent(
        eventName: String,
        additionalParams: Map<String, Any?> = emptyMap()
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
        const val FIELD_ERROR_CODE = "error_code"

        const val FIELD_CANCELLATION_REASON = "cancellation_reason"
        const val FIELD_NUMBER_OF_ATTEMPTS = "number_of_attempts"

        const val SCAN_STARTED_EVENT_NAME = "nfc_scan_started"
        const val SCAN_SUCCESS_EVENT_NAME = "nfc_scan_success"
        const val SCAN_CANCELED_EVENT_NAME = "nfc_scan_canceled"
        const val SCAN_ATTEMPT_STARTED_EVENT_NAME = "nfc_scan_attempt_started"
        const val SCAN_ATTEMPT_SUCCEEDED_EVENT_NAME = "nfc_scan_attempt_succeeded"
        const val SCAN_ATTEMPT_FAILED_EVENT_NAME = "nfc_scan_attempt_failed"
    }
}
