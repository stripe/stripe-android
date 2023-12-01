package com.stripe.android.ui.core.elements

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.utils.TestUtils.idleLooper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.stripe.android.R as StripeR

@RunWith(RobolectricTestRunner::class)
internal class CvcControllerTest {
    private val cardBrandFlow = MutableStateFlow(CardBrand.Visa)
    private val cvcController = CvcController(
        CvcConfig(),
        cardBrandFlow,
        initialValue = null
    )

    @Test
    fun `When invalid card number verify visible error`() = runTest {
        cvcController.error.test {
            assertThat(awaitItem()).isNull()

            cvcController.onValueChange("12")
            idleLooper()

            skipItems(1)

            assertThat(awaitItem()?.errorMessage)
                .isEqualTo(StripeR.string.stripe_invalid_cvc)
        }
    }

    @Test
    fun `Verify get the form field value correctly`() = runTest {
        cvcController.formFieldValue.test {
            skipItems(1)

            cvcController.onValueChange("13")
            idleLooper()

            assertThat(awaitItem().isComplete).isFalse()
            assertThat(awaitItem().value).isEqualTo("13")

            cvcController.onValueChange("123")
            idleLooper()

            assertThat(awaitItem().isComplete).isTrue()
            assertThat(awaitItem().value).isEqualTo("123")
        }
    }

    @Test
    fun `Verify error is visible based on the focus`() = runTest {
        // incomplete
        cvcController.visibleError.test {
            cvcController.onFocusChange(true)
            cvcController.onValueChange("12")
            idleLooper()

            assertThat(awaitItem()).isFalse()

            cvcController.onFocusChange(false)
            idleLooper()

            assertThat(awaitItem()).isTrue()
        }
    }
}
