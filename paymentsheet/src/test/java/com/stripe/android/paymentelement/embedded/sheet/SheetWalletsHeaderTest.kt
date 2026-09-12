package com.stripe.android.paymentelement.embedded.sheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import org.junit.Test

internal class SheetWalletsHeaderTest {
    @Test
    fun `payment options shows header when behavior is always with back stack`() {
        val result = shouldShowWalletsHeader(
            launchMode = EmbeddedLaunchMode.PaymentOptions,
            behavior = EmbeddedNavigator.WalletsHeaderBehavior.Always,
            canGoBack = true,
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `payment options shows header when behavior is root only without back stack`() {
        val result = shouldShowWalletsHeader(
            launchMode = EmbeddedLaunchMode.PaymentOptions,
            behavior = EmbeddedNavigator.WalletsHeaderBehavior.RootOnly,
            canGoBack = false,
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `payment options hides header when behavior is root only with back stack`() {
        val result = shouldShowWalletsHeader(
            launchMode = EmbeddedLaunchMode.PaymentOptions,
            behavior = EmbeddedNavigator.WalletsHeaderBehavior.RootOnly,
            canGoBack = true,
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `payment options hides header when behavior is never`() {
        val result = shouldShowWalletsHeader(
            launchMode = EmbeddedLaunchMode.PaymentOptions,
            behavior = EmbeddedNavigator.WalletsHeaderBehavior.Never,
            canGoBack = false,
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `form launch mode hides header`() {
        val result = shouldShowWalletsHeader(
            launchMode = EmbeddedLaunchMode.Form(selectedPaymentMethodCode = "card"),
            behavior = EmbeddedNavigator.WalletsHeaderBehavior.Always,
            canGoBack = false,
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `manage launch mode hides header`() {
        val result = shouldShowWalletsHeader(
            launchMode = EmbeddedLaunchMode.Manage,
            behavior = EmbeddedNavigator.WalletsHeaderBehavior.Always,
            canGoBack = false,
        )

        assertThat(result).isFalse()
    }
}
