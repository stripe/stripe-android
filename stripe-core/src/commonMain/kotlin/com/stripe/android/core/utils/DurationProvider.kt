package com.stripe.android.core.utils

import androidx.annotation.RestrictTo
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.TimeMark
import kotlin.time.TimeSource

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface DurationProvider {
    fun start(key: Key, reset: Boolean = true)
    fun end(key: Key): Duration?

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class Key {
        Loading,
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
        IntentConfirmationChallengeWebViewLoaded
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultDurationProvider private constructor() : DurationProvider {

    private val store = mutableMapOf<DurationProvider.Key, TimeMark>()

    override fun start(key: DurationProvider.Key, reset: Boolean) {
        if (reset || key !in store) {
            val startTime = TimeSource.Monotonic.markNow()
            store[key] = startTime
        }
    }

    override fun end(key: DurationProvider.Key): Duration? {
        val startTime = store.remove(key) ?: return null
        return startTime.elapsedNow()
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
