package com.stripe.android.paymentsheet.ui

import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.CardUpdateParams
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.test.TestScope
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
    fun testInitialStateWithNoAddressCollection() {
        val handler = handler(addressCollectionMode = AddressCollectionMode.Never)

        val state = handler.uiState
        assertThat(state.billingDetailsForm).isNull()
    }

    @Test
    fun testInitialStateWithAutomaticAddressCollection() {
        val handler = handler(addressCollectionMode = AddressCollectionMode.Automatic)

        val state = handler.uiState
        assertThat(state.billingDetailsForm).isNotNull()
    }

    @Test
    fun testInitialStateWithFullAddressCollection() {
        val handler = handler(addressCollectionMode = AddressCollectionMode.Full)

        val state = handler.uiState
        assertThat(state.billingDetailsForm).isNotNull()
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
        assertThat(cardUpdateParams?.cardBrand).isEqualTo(CardBrand.Visa)

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
    fun validExpiryDateChangeShouldProduceNewCardParams() {
        var cardUpdateParams: CardUpdateParams? = null
        val handler = handler(
            onCardUpdateParamsChanged = {
                cardUpdateParams = it
            }
        )

        handler.updateExpiryDate(text = "1240")

        assertThat(cardUpdateParams?.expiryMonth).isEqualTo(12)
        assertThat(cardUpdateParams?.expiryYear).isEqualTo(2040)
    }

    @Test
    fun invalidExpiryDateChangeShouldNotProduceNewCardParams() {
        var cardUpdateParams: CardUpdateParams? = null
        val handler = handler(
            onCardUpdateParamsChanged = {
                cardUpdateParams = it
            }
        )

        handler.updateExpiryDate(text = "12/40")

        assertThat(cardUpdateParams).isNull()
    }

    @Test
    fun pastExpiryDateChangeShouldProduceNewCardParams() {
        var cardUpdateParams: CardUpdateParams? = null
        val handler = handler(
            onCardUpdateParamsChanged = {
                cardUpdateParams = it
            }
        )

        handler.updateExpiryDate(text = "1240")

        assertThat(cardUpdateParams?.expiryMonth).isEqualTo(12)
        assertThat(cardUpdateParams?.expiryYear).isEqualTo(2040)

        handler.updateExpiryDate(text = "1215")

        assertThat(cardUpdateParams?.expiryMonth).isNull()
        assertThat(cardUpdateParams?.expiryYear).isNull()
    }

    @Test
    fun addressUpdateShouldProduceNewCardParams() {
        var cardUpdateParams: CardUpdateParams? = null
        val handler = handler(
            onCardUpdateParamsChanged = {
                cardUpdateParams = it
            }
        )

        handler.updateBillingDetails(
            billingDetailsFormState = PaymentSheetFixtures.billingDetailsFormState(
                postalCode = FormFieldEntry("94444", isComplete = true),
            )
        )

        val address = cardUpdateParams?.billingDetails?.address
        assertThat(address?.postalCode).isEqualTo("94444")
        assertThat(address?.country).isEqualTo("US")
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

    @Test
    fun cardUpdateParamsIsUpdatedForValidAddressUpdate() {
        var capturedCardUpdateParams: CardUpdateParams? = null
        val handler = handler(
            onCardUpdateParamsChanged = {
                capturedCardUpdateParams = it
            }
        )

        handler.handleViewAction(
            EditCardDetailsInteractor.ViewAction.BillingDetailsChanged(
                PaymentSheetFixtures.billingDetailsFormState(
                    postalCode = FormFieldEntry("11111", isComplete = true),
                )
            )
        )

        assertThat(capturedCardUpdateParams?.billingDetails?.address?.postalCode).isEqualTo("11111")
    }

    @Test
    fun cardUpdateParamsIsUpdatedForValidExpDateUpdate() {
        var capturedCardUpdateParams: CardUpdateParams? = null
        val handler = handler(
            onCardUpdateParamsChanged = {
                capturedCardUpdateParams = it
            }
        )

        handler.handleViewAction(
            EditCardDetailsInteractor.ViewAction.DateChanged("1230") // Dec 2030
        )

        assertThat(capturedCardUpdateParams).isNotNull()
        assertThat(capturedCardUpdateParams?.expiryMonth).isEqualTo(12)
        assertThat(capturedCardUpdateParams?.expiryYear).isEqualTo(2030)
    }

    @Test
    fun cardUpdateParamsIsUpdatedWithNullForInvalidExpDateUpdate() {
        var capturedCardUpdateParams: CardUpdateParams? = null
        val handler = handler(
            onCardUpdateParamsChanged = {
                capturedCardUpdateParams = it
            }
        )

        handler.handleViewAction(
            EditCardDetailsInteractor.ViewAction.DateChanged("00/00")
        )

        assertThat(capturedCardUpdateParams).isNull()

        handler.handleViewAction(
            EditCardDetailsInteractor.ViewAction.DateChanged("1220")
        )

        assertThat(capturedCardUpdateParams).isNull()
    }

    @Test
    fun cardUpdateParamsIsUpdatedWithNullForInvalidAddressUpdate() {
        var capturedCardUpdateParams: CardUpdateParams? = null
        val handler = handler(
            onCardUpdateParamsChanged = {
                capturedCardUpdateParams = it
            }
        )

        handler.handleViewAction(
            EditCardDetailsInteractor.ViewAction.BillingDetailsChanged(
                PaymentSheetFixtures.billingDetailsFormState(
                    postalCode = FormFieldEntry("", isComplete = false),
                    country = FormFieldEntry("US", isComplete = true),
                )
            )
        )

        assertThat(capturedCardUpdateParams).isNull()
    }

    private val EditCardDetailsInteractor.uiState
        get() = this.state.value

    private val EditCardDetailsInteractor.selectedBrand
        get() = uiState.selectedCardBrand.brand

    private fun handler(
        card: PaymentMethod.Card = PaymentMethodFixtures.CARD_WITH_NETWORKS,
        cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter,
        isModifiable: Boolean = true,
        addressCollectionMode: AddressCollectionMode = AddressCollectionMode.Automatic,
        onBrandChoiceChanged: (CardBrand) -> Unit = {},
        onCardUpdateParamsChanged: (CardUpdateParams?) -> Unit = {}
    ): EditCardDetailsInteractor {
        return DefaultEditCardDetailsInteractor.Factory().create(
            cardBrandFilter = cardBrandFilter,
            onBrandChoiceChanged = onBrandChoiceChanged,
            coroutineScope = TestScope(testDispatcher),
            isModifiable = isModifiable,
            card = card,
            onCardUpdateParamsChanged = onCardUpdateParamsChanged,
            areExpiryDateAndAddressModificationSupported = true,
            billingDetails = PaymentMethodFixtures.BILLING_DETAILS,
            addressCollectionMode = addressCollectionMode,
        )
    }
}
