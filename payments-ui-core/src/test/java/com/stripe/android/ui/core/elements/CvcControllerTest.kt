package com.stripe.android.ui.core.elements

import androidx.compose.ui.unit.LayoutDirection
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.stripe.android.R as StripeR
import com.stripe.android.uicore.R as StripeUiCoreR

@RunWith(RobolectricTestRunner::class)
internal class CvcControllerTest {
    private val cardBrandFlow = MutableStateFlow(CardBrand.Visa)

    @Test
    fun `When invalid card number verify visible error`() = runTest {
        val cvcController = createController()
        cvcController.error.test {
            assertThat(awaitItem()).isNull()

            cvcController.onValueChange("12")

            assertThat(awaitItem()?.errorMessage)
                .isEqualTo(StripeUiCoreR.string.stripe_blank_and_required)

            assertThat(awaitItem()?.errorMessage)
                .isEqualTo(StripeR.string.stripe_invalid_cvc)
        }
    }

    @Test
    fun `Verify get the form field value correctly`() = runTest {
        val cvcController = createController()
        cvcController.formFieldValue.test {
            with(awaitItem()) {
                assertThat(isComplete).isFalse()
                assertThat(value).isEmpty()
            }

            cvcController.onValueChange("13")

            with(awaitItem()) {
                assertThat(isComplete).isFalse()
                assertThat(value).isEqualTo("13")
            }

            cvcController.onValueChange("123")
            assertThat(awaitItem().isComplete).isTrue()
            assertThat(awaitItem().value).isEqualTo("123")
        }
    }

    @Test
    fun `Verify error is visible based on the focus`() = runTest {
        val cvcController = createController()
        cvcController.visibleError.test {
            cvcController.onFocusChange(true)
            cvcController.onValueChange("12")

            assertThat(awaitItem()).isFalse()

            cvcController.onFocusChange(false)
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `Controller should always have an Ltr layout`() = runTest {
        val cvcController = createController()

        assertThat(cvcController.layoutDirection).isEqualTo(LayoutDirection.Ltr)
    }

    private fun createController(): CvcController {
        return CvcController(
            CvcConfig(),
            cardBrandFlow,
            initialValue = null
        )
    }
}
