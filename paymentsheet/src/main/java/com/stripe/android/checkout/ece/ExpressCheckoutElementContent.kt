package com.stripe.android.checkout.ece

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun ExpressCheckoutElementContent(
    interactor: ExpressCheckoutElementInteractor,
) {
    val state by interactor.state.collectAsState()

    state.expressButtons.forEach { button ->
        when (button) {
            ExpressButton.GooglePay -> Text("Google Pay Button")
            ExpressButton.Link -> Text("Link Button")
        }
    }
}
