package com.stripe.android.ui.core.elements

import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.stripecardscan.R
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.utils.TestUtils.idleLooper
import kotlinx.coroutines.test.runTest
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
    fun `Verify the first field in error is returned in error flow`() = runTest {
        val cardController = CardDetailsController(context, emptyMap())

        cardController.error.test {
            assertThat(awaitItem()).isNull()

            cardController.numberElement.controller.onValueChange("4242424242424243")
            cardController.cvcElement.controller.onValueChange("123")
            cardController.expirationDateElement.controller.onValueChange("13")

            idleLooper()

            assertThat(awaitItem()?.errorMessage).isEqualTo(
                StripeR.string.stripe_invalid_card_number
            )

            cardController.numberElement.controller.onValueChange("4242424242424242")
            idleLooper()

            skipItems(1)

            assertThat(awaitItem()?.errorMessage).isEqualTo(
                UiCoreR.string.stripe_incomplete_expiry_date
            )
        }
    }

    @Test
    fun `When eligible for card brand choice and preferred card brand is passed, initial value should have been set`() = runTest {
        val cardController = CardDetailsController(
            context,
            mapOf(
                IdentifierSpec.CardNumber to "4000002500001001",
                IdentifierSpec.PreferredCardBrand to CardBrand.CartesBancaires.code
            ),
            cbcEligibility = CardBrandChoiceEligibility.Eligible(listOf())
        )

        cardController.numberElement.controller.cardBrandFlow.test {
            assertThat(awaitItem()).isEqualTo(CardBrand.CartesBancaires)
        }
    }
}
