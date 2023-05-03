package com.stripe.android.ui.core.elements

import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.asLiveData
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripecardscan.R
import com.stripe.android.uicore.elements.FieldError
import com.stripe.android.utils.TestUtils.idleLooper
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.stripe.android.R as StripeR
import com.stripe.android.uicore.R as UiCoreR

@RunWith(RobolectricTestRunner::class)
class CardDetailsControllerTest {

    private val context =
        ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.StripeCardScanDefaultTheme)

    @Test
    fun `Verify the first field in error is returned in error flow`() {
        val cardController = CardDetailsController(context, emptyMap())

        val flowValues = mutableListOf<FieldError?>()
        cardController.error.asLiveData()
            .observeForever {
                flowValues.add(it)
            }

        cardController.numberElement.controller.onValueChange("4242424242424243")
        cardController.cvcElement.controller.onValueChange("123")
        cardController.expirationDateElement.controller.onValueChange("13")

        idleLooper()

        assertThat(flowValues[flowValues.size - 1]?.errorMessage).isEqualTo(
            StripeR.string.stripe_invalid_card_number
        )

        cardController.numberElement.controller.onValueChange("4242424242424242")
        idleLooper()

        assertThat(flowValues[flowValues.size - 1]?.errorMessage).isEqualTo(
            UiCoreR.string.stripe_incomplete_expiry_date
        )
    }

    @Test
    fun `When card number is not view only then CardNumberControllerEditable is used`() {
        val cardController =
            CardDetailsController(context, emptyMap())

        assertThat(cardController.numberElement.controller)
            .isInstanceOf(CardNumberEditableController::class.java)
    }

    @Test
    fun `When card number is view only then CardNumberControllerViewOnly is used`() {
        val cardController =
            CardDetailsController(context, emptyMap(), true)

        assertThat(cardController.numberElement.controller)
            .isInstanceOf(CardNumberViewOnlyController::class.java)
    }
}
