package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.PaymentElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.utils.simulateProcessDeath
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CheckoutElementsConfigurationStateHolderTest {
    @Test
    fun `configuration survives process death without loaded controller state`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val stateHolder = CheckoutElementsConfigurationStateHolder(savedStateHandle)
        val configuration = CheckoutElementsController.Configuration().paymentElement(
            PaymentElement.Configuration().appearance(
                PaymentElement.Configuration.Appearance()
                    .themeMode(PaymentElement.Configuration.Appearance.ThemeMode.AlwaysDark)
            )
        ).build()
        assertThat(stateHolder.state).isNull()

        stateHolder.state = configuration
        val restored = CheckoutElementsConfigurationStateHolder(savedStateHandle.simulateProcessDeath())

        assertThat(restored.state).isEqualTo(configuration)
    }
}
