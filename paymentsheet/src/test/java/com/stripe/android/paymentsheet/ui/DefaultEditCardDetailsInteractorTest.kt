package com.stripe.android.paymentsheet.ui

import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.CardUpdateParams
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DefaultEditCardDetailsInteractorTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val testRule = CoroutineTestRule(testDispatcher)

    @Test
    fun testInitialStateForCardWithNetworks() {
        val handler = handler()

        val state = handler.uiState
        assertThat(state.card).isEqualTo(PaymentMethodFixtures.CARD_WITH_NETWORKS)
        assertThat(state.selectedCardBrand.brand).isEqualTo(CardBrand.CartesBancaires)
    }

    @Test
    fun testInitialStateForCardWithNoNetworks() {
        val handler = handler(card = PaymentMethodFixtures.CARD)

        val state = handler.uiState
        assertThat(state.card).isEqualTo(PaymentMethodFixtures.CARD)
        assertThat(state.selectedCardBrand.brand).isEqualTo(CardBrand.Unknown)
    }

    @Test
    fun stateIsUpdateWhenNewCardBrandIsSelected() {
        val handler = handler()

        assertThat(handler.selectedBrand).isEqualTo(CardBrand.CartesBancaires)

        handler.updateCardBrand(CardBrand.Visa)

        assertThat(handler.selectedBrand).isEqualTo(CardBrand.Visa)
    }

    @Test
    fun cardUpdateParamsIsUpdatedWhenNewCardBrandIsSelected() {
        var cardUpdateParams: CardUpdateParams? = null
        val handler = handler(
            onCardUpdateParamsChanged = {
                cardUpdateParams = it
            }
        )

        handler.updateCardBrand(CardBrand.Visa)
        assertThat(cardUpdateParams).isEqualTo(cardUpdateParams(cardBrand = CardBrand.Visa))

        handler.updateCardBrand(CardBrand.CartesBancaires)

        assertThat(cardUpdateParams).isNull()
    }

    @Test
    fun cardUpdateParamsIsNotUpdatedWhenCurrentCardBrandIsSelected() {
        var cardUpdateParams: CardUpdateParams? = null
        val handler = handler(
            onCardUpdateParamsChanged = {
                cardUpdateParams = it
            }
        )

        handler.updateCardBrand(CardBrand.CartesBancaires)
        assertThat(cardUpdateParams).isNull()
    }

    @Test
    fun brandChangedCallbackIsOnlyInvokedForNewBrandSelection() {
        var newBrandChoice: CardBrand? = null
        val handler = handler(
            onBrandChoiceChanged = {
                newBrandChoice = it
            }
        )

        assertThat(handler.selectedBrand).isEqualTo(CardBrand.CartesBancaires)

        handler.updateCardBrand(CardBrand.CartesBancaires)

        assertThat(newBrandChoice).isNull()

        handler.updateCardBrand(CardBrand.Visa)

        assertThat(newBrandChoice).isEqualTo(CardBrand.Visa)
    }

    @Test
    fun expiredCardShouldNotShowCbcDropdown() {
        val handler = handler(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS.copy(
                expiryYear = 1975
            )
        )

        val state = handler.uiState
        assertThat(state.shouldShowCardBrandDropdown).isFalse()
    }

    @Test
    fun unModifiableCardShouldNotShowCbcDropdown() {
        val handler = handler(
            isModifiable = false
        )

        val state = handler.uiState
        assertThat(state.shouldShowCardBrandDropdown).isFalse()
    }

    private val DefaultEditCardDetailsInteractor.uiState
        get() = this.state.value

    private val DefaultEditCardDetailsInteractor.selectedBrand
        get() = uiState.selectedCardBrand.brand

    private fun cardUpdateParams(
        expiryMonth: Int? = null,
        expiryYear: Int? = null,
        cardBrand: CardBrand? = null,
        billingDetails: PaymentMethod.BillingDetails? = null
    ): CardUpdateParams {
        return CardUpdateParams(
            expiryMonth = expiryMonth,
            expiryYear = expiryYear,
            cardBrand = cardBrand,
            billingDetails = billingDetails
        )
    }

    private fun handler(
        card: PaymentMethod.Card = PaymentMethodFixtures.CARD_WITH_NETWORKS,
        cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter,
        isModifiable: Boolean = true,
        onBrandChoiceChanged: (CardBrand) -> Unit = {},
        onCardUpdateParamsChanged: (CardUpdateParams?) -> Unit = {}
    ): DefaultEditCardDetailsInteractor {
        return DefaultEditCardDetailsInteractor.Factory(
            cardBrandFilter = cardBrandFilter,
            onBrandChoiceChanged = onBrandChoiceChanged,
            workContext = testDispatcher,
            isModifiable = isModifiable,
        ).create(
            card = card,
            onCardUpdateParamsChanged = onCardUpdateParamsChanged
        ) as DefaultEditCardDetailsInteractor
    }
}
