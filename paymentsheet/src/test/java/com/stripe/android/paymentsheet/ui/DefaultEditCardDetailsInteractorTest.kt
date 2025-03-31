package com.stripe.android.paymentsheet.ui

import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.CardUpdateParams
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DefaultEditCardDetailsInteractorTest {

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

        handler.brandChanged(CardBrand.Visa)

        assertThat(handler.selectedBrand).isEqualTo(CardBrand.Visa)
    }

    @Test
    fun cardUpdateParamsIsUpdatedWhenNewCardBrandIsSelected() {
        var cardUpdateParams: CardUpdateParams? = null
        val handler = handler(
            onCardDetailsChanged = {
                cardUpdateParams = it
            }
        )

        assertThat(handler.selectedBrand).isEqualTo(CardBrand.CartesBancaires)

        handler.brandChanged(CardBrand.Visa)

        assertThat(cardUpdateParams).isEqualTo(cardUpdateParams(cardBrand = CardBrand.Visa))

        handler.brandChanged(CardBrand.CartesBancaires)

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

        handler.brandChanged(CardBrand.CartesBancaires)

        assertThat(newBrandChoice).isNull()

        handler.brandChanged(CardBrand.Visa)

        assertThat(newBrandChoice).isEqualTo(CardBrand.Visa)
    }

    private fun DefaultEditCardDetailsInteractor.brandChanged(cardBrand: CardBrand) {
        onBrandChoiceChanged(CardBrandChoice(brand = cardBrand, enabled = true))
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
        showCardBrandDropdown: Boolean = true,
        onBrandChoiceChanged: (CardBrand) -> Unit = {},
        onCardDetailsChanged: (CardUpdateParams?) -> Unit = {}
    ): DefaultEditCardDetailsInteractor {
        return DefaultEditCardDetailsInteractor(
            card = card,
            cardBrandFilter = cardBrandFilter,
            paymentMethodIcon = 0,
            showCardBrandDropdown = showCardBrandDropdown,
            scope = TestScope(UnconfinedTestDispatcher()),
            onBrandChoiceChanged = onBrandChoiceChanged,
            onCardDetailsChanged = onCardDetailsChanged
        )
    }
}
