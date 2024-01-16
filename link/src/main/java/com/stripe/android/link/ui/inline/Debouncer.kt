package com.stripe.android.link.ui.inline

import com.stripe.android.link.ui.signup.SignUpState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal class Debouncer {
    fun startWatching(
        coroutineScope: CoroutineScope,
        emailFlow: StateFlow<String?>,
        onStateChanged: (SignUpState) -> Unit,
        onValidEmailEntered: (String) -> Unit
    ) {
        coroutineScope.launch {
            emailFlow.collectLatest { email ->
                if (!email.isNullOrBlank()) {
                    delay(LOOKUP_DEBOUNCE_MS)
                    onStateChanged(SignUpState.VerifyingEmail)
                    onValidEmailEntered(email)
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
