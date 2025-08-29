package com.stripe.android.ui.core.elements

import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.model.CardBrand
import com.stripe.android.stripecardscan.R
import com.stripe.android.ui.core.cardscan.CardScanResult
import com.stripe.android.ui.core.cardscan.ScannedCard
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
        val cardController = CardDetailsController(
            cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context),
            initialValues = emptyMap(),
        )

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

            assertThat(awaitItem()?.errorMessage).isEqualTo(
                UiCoreR.string.stripe_incomplete_expiry_date
            )
        }
    }

    @Test
    fun `When eligible for card brand choice and preferred card brand is passed, initial value should have been set`() = runTest {
        val cardController = CardDetailsController(
            cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context),
            initialValues = mapOf(
                IdentifierSpec.CardNumber to "4000002500001001",
                IdentifierSpec.PreferredCardBrand to CardBrand.CartesBancaires.code
            ),
            cbcEligibility = CardBrandChoiceEligibility.Eligible(listOf())
        )

        cardController.numberElement.controller.cardBrandFlow.test {
            assertThat(awaitItem()).isEqualTo(CardBrand.CartesBancaires)
        }
    }

    @Test
    fun `When new card scanned with no existing card, fields properly filled in`() = runTest {
        val cardController = CardDetailsController(
            cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context),
            initialValues = emptyMap(),
        )
        assertThat(cardController.numberElement.controller.rawFieldValue.value)
            .isEqualTo("")
        assertThat(cardController.expirationDateElement.controller.rawFieldValue.value)
            .isEqualTo("")
        assertThat(cardController.cvcElement.controller.rawFieldValue.value)
            .isEqualTo("")

        val scannedCard = ScannedCard(
            pan = "5555555555554444",
            expirationYear = 2044,
            expirationMonth = 4,
        )

        val cardScanResult = CardScanResult.Completed(
            scannedCard = scannedCard
        )
        idleLooper()

        cardController.onCardScanResult.invoke(cardScanResult)

        assertThat(cardController.numberElement.controller.rawFieldValue.value)
            .isEqualTo("5555555555554444")
        assertThat(cardController.expirationDateElement.controller.rawFieldValue.value)
            .isEqualTo("444")
        assertThat(cardController.cvcElement.controller.rawFieldValue.value)
            .isEqualTo("")
    }

    @Test
    fun `When new card overwrites existing card, fields properly filled in`() = runTest {
        val cardController = CardDetailsController(
            cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context),
            initialValues = mapOf(
                IdentifierSpec.CardNumber to "4242424242424242",
                IdentifierSpec.CardExpYear to "2042",
                IdentifierSpec.CardExpMonth to "2",
                IdentifierSpec.CardCvc to "123",
            ),
        )
        assertThat(cardController.numberElement.controller.rawFieldValue.value)
            .isEqualTo("4242424242424242")
        assertThat(cardController.expirationDateElement.controller.rawFieldValue.value)
            .isEqualTo("242")
        assertThat(cardController.cvcElement.controller.rawFieldValue.value)
            .isEqualTo("123")

        val scannedCard = ScannedCard(
            pan = "5555555555554444",
            expirationYear = 2044,
            expirationMonth = 4,
        )

        val cardScanResult = CardScanResult.Completed(
            scannedCard = scannedCard
        )
        idleLooper()

        cardController.onCardScanResult.invoke(cardScanResult)

        assertThat(cardController.numberElement.controller.rawFieldValue.value)
            .isEqualTo("5555555555554444")
        assertThat(cardController.expirationDateElement.controller.rawFieldValue.value)
            .isEqualTo("444")
        assertThat(cardController.cvcElement.controller.rawFieldValue.value)
            .isEqualTo("")
    }

    @Test
    fun `When new card scanned with invalid expiry date, should not use invalid date`() = runTest {
        val cardController = CardDetailsController(
            cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context),
            initialValues = mapOf(
                IdentifierSpec.CardNumber to "4242424242424242",
                IdentifierSpec.CardExpYear to "2042",
                IdentifierSpec.CardExpMonth to "2",
                IdentifierSpec.CardCvc to "123",
            ),
        )
        assertThat(cardController.numberElement.controller.rawFieldValue.value)
            .isEqualTo("4242424242424242")
        assertThat(cardController.expirationDateElement.controller.rawFieldValue.value)
            .isEqualTo("242")
        assertThat(cardController.cvcElement.controller.rawFieldValue.value)
            .isEqualTo("123")

        val scannedCard = ScannedCard(
            pan = "5555555555554444",
            expirationYear = 2009,
            expirationMonth = 12,
        )

        val cardScanResult = CardScanResult.Completed(
            scannedCard = scannedCard
        )
        idleLooper()

        cardController.onCardScanResult.invoke(cardScanResult)

        assertThat(cardController.numberElement.controller.rawFieldValue.value)
            .isEqualTo("5555555555554444")
        assertThat(cardController.expirationDateElement.controller.rawFieldValue.value)
            .isEqualTo("")
    }
}
