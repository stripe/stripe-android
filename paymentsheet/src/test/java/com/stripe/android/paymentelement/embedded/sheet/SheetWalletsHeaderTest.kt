package com.stripe.android.paymentelement.embedded.sheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import org.junit.Test

internal class SheetWalletsHeaderTest {
    @Test
    fun `root payment options shows header`() {
        val result = shouldShowWalletsHeader(
            launchMode = EmbeddedLaunchMode.PaymentOptions,
            canGoBack = false,
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `nested payment options hides header`() {
        val result = shouldShowWalletsHeader(
            launchMode = EmbeddedLaunchMode.PaymentOptions,
            canGoBack = true,
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `form launch mode hides header`() {
        val result = shouldShowWalletsHeader(
            launchMode = EmbeddedLaunchMode.Form(selectedPaymentMethodCode = "card"),
            canGoBack = false,
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `manage launch mode hides header`() {
        val result = shouldShowWalletsHeader(
            launchMode = EmbeddedLaunchMode.Manage,
            canGoBack = false,
        )

        assertThat(result).isFalse()
    }
}
