package com.stripe.android.core.utils

import android.os.SystemClock
import androidx.annotation.RestrictTo
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.Logger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface DurationProvider {
    fun start(key: Key, reset: Boolean = true)
    fun elapsed(key: Key): Duration?
    fun end(key: Key): Duration?
    fun completedDuration(key: Key): Duration?

    suspend fun <T> measureDuration(
        key: Key,
        block: suspend () -> T,
    ): T

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class Key {
        Loading,
        PaymentSheetLoadIsGooglePaySupported,
        PaymentSheetLoadIsGooglePayReady,
        PaymentSheetLoadRetrieveSavedPaymentMethodSelection,
        PaymentSheetLoadSessionLoad,
        PaymentSheetLoadPrefetchPMs,
        PaymentSheetLoadCreateLinkState,
        PaymentSheetLoadCreateCustomerState,
        PaymentSheetLoadRetrieveInitialPaymentSelection,
        Checkout,
        LinkSignup,
        ConfirmButtonClicked,
        TapToAdd,
        CardScan,
        Captcha,
        CaptchaAttach,
        PaymentLauncher,
        PrepareAttestation,
        Attest,
        IntentConfirmationChallenge,
        IntentConfirmationChallengeWebViewLoaded,
        PaymentMethodMessaging
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultDurationProvider private constructor() : DurationProvider {
    private val logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG)

    private val store = mutableMapOf<DurationProvider.Key, Long>()
    private val completedStore = mutableMapOf<DurationProvider.Key, Duration>()

    override fun start(key: DurationProvider.Key, reset: Boolean) {
        if (reset || key !in store) {
            if (reset) {
                completedStore.remove(key)
            }
            val startTime = SystemClock.uptimeMillis()
            store[key] = startTime
            logger.debug("DURATION_STARTED: ${key.name}: $startTime")
        }
    }

    override fun elapsed(key: DurationProvider.Key): Duration? {
        val startTime = store[key] ?: return null
        return (SystemClock.uptimeMillis() - startTime).milliseconds
    }

    override fun end(key: DurationProvider.Key): Duration? {
        val startTime = store.remove(key) ?: return null
        val endTime = SystemClock.uptimeMillis()
        logger.debug("DURATION_ENDED: ${key.name}: $endTime")
        val duration = (endTime - startTime).milliseconds
        completedStore[key] = duration
        return duration
    }

    override fun completedDuration(key: DurationProvider.Key): Duration? {
        return completedStore[key]
    }

    override suspend fun <T> measureDuration(key: DurationProvider.Key, block: suspend () -> T): T {
        start(key, reset = true)
        return try {
            block()
        } finally {
            end(key)
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        val instance = DefaultDurationProvider()
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Duration?.mapOfDurationInSeconds(): Map<String, Float> {
    return this?.let {
        mapOf("duration" to it.toDouble(DurationUnit.SECONDS).toFloat())
    } ?: emptyMap()
}
