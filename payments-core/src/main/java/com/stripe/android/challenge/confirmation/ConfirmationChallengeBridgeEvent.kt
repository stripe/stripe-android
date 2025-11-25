package com.stripe.android.challenge.confirmation

internal sealed interface ConfirmationChallengeBridgeEvent {
    data object Ready : ConfirmationChallengeBridgeEvent
    data class Success(val clientSecret: String) : ConfirmationChallengeBridgeEvent
    data class Error(val error: BridgeException) : ConfirmationChallengeBridgeEvent
}
