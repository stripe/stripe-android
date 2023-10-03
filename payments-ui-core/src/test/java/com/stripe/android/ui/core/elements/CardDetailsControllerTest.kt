package com.stripe.android.ui.core.elements

import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.asLiveData
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.stripecardscan.R
import com.stripe.android.uicore.elements.FieldError
import com.stripe.android.uicore.elements.IdentifierSpec
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
    fun `When eligible for card brand choice and preferred card brand is passed, initial value should have been set`() {
        val cardController = CardDetailsController(
            context,
            mapOf(
                IdentifierSpec.CardNumber to "4000002500001001",
                IdentifierSpec.PreferredCardBrand to CardBrand.CartesBancaires.code
            ),
            isEligibleForCardBrandChoice = true
        )

        val flowValues = mutableListOf<CardBrand?>()
        cardController.numberElement.controller.cardBrandFlow.asLiveData()
            .observeForever {
                flowValues.add(it)
            }

        assertThat(flowValues[flowValues.size - 1]).isEqualTo(CardBrand.CartesBancaires)
    }
}
