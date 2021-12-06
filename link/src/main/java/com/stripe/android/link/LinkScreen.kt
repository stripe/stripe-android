package com.stripe.android.link

import androidx.compose.runtime.Composable
import com.stripe.android.link.ui.signup.SignUpBody

internal enum class LinkScreen(
    val body: @Composable () -> Unit
) {
    SignUp(
        body = {
            SignUpBody()
        }
    );
}
