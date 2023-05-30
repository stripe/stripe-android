package com.stripe.android.link.ui.inline

import com.stripe.android.link.ui.signup.SignUpState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class Debouncer(
    private val initialEmail: String?
) {
    /**
     * Holds a Job that looks up the email after a delay, so that we can cancel it if the user
     * continues typing.
     */
    private var lookupJob: Job? = null

    fun startWatching(
        coroutineScope: CoroutineScope,
        emailFlow: StateFlow<String?>,
        onStateChanged: (SignUpState) -> Unit,
        onValidEmailEntered: (String) -> Unit
    ) {
        coroutineScope.launch {
            emailFlow.collect { email ->
                // The first emitted value is the one provided in the constructor arguments, and
                // shouldn't trigger a lookup.
                if (email == initialEmail && lookupJob == null) {
                    // If it's a valid email, collect phone number
                    if (email != null) {
                        onStateChanged(SignUpState.InputtingPhoneOrName)
                    }
                    return@collect
                }

                lookupJob?.cancel()

                if (email != null) {
                    lookupJob = launch {
                        delay(LOOKUP_DEBOUNCE_MS)
                        if (isActive) {
                            onStateChanged(SignUpState.VerifyingEmail)
                            onValidEmailEntered(email)
                        }
                    }
                } else {
                    onStateChanged(SignUpState.InputtingEmail)
                }
            }
        }
    }

    companion object {
        // How long to wait (in milliseconds) before triggering a call to lookup the email
        const val LOOKUP_DEBOUNCE_MS = 1000L
    }
}
