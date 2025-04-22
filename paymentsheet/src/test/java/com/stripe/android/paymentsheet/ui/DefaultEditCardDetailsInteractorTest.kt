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
    fun testInitialStateWithFullDetailsEditEnabled() {
        val handler = handler()

        val state = handler.uiState
        assertThat(state.billingDetailsForm).isNotNull()
        assertThat(state.expiryDateState.enabled).isTrue()
    }

    @Test
    fun testInitialStateWithFullDetailsEditDisabled() {
        val handler = handler(
            areExpiryDateAndAddressModificationSupported = false
        )

        val state = handler.uiState
        assertThat(state.billingDetailsForm).isNull()
        assertThat(state.expiryDateState.enabled).isFalse()
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
    fun unModifiableCardShouldNotShowCbcDropdown() {
        val handler = handler(
            isCbcModifiable = false
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

    @Test
    fun whenAddressCollectionModeIsNeverAndBillingDetailsNotNullThenCardUpdateParamsIsNull() {
        var cardUpdateParams: CardUpdateParams? = null
        val handler = handler(
            addressCollectionMode = AddressCollectionMode.Never,
            onCardUpdateParamsChanged = {
                cardUpdateParams = it
            }
        )

        handler.updateBillingDetails(
            billingDetailsFormState = PaymentSheetFixtures.billingDetailsFormState(
                postalCode = FormFieldEntry("94444", isComplete = true),
            )
        )

        assertThat(cardUpdateParams).isNull()
    }

    @Test
    fun whenAddressCollectionModeIsAutomaticAndBillingDetailsIsNullThenCardUpdateParamsIsUpdated() {
        var cardUpdateParams: CardUpdateParams? = null
        val handler = handler(
            addressCollectionMode = AddressCollectionMode.Automatic,
            billingDetails = null,
            onCardUpdateParamsChanged = {
                cardUpdateParams = it
            }
        )

        handler.updateCardBrand(CardBrand.Visa)

        assertThat(cardUpdateParams).isNotNull()
        assertThat(cardUpdateParams?.cardBrand).isEqualTo(CardBrand.Visa)
        assertThat(cardUpdateParams?.billingDetails).isNull()
    }

    @Test
    fun whenAddressCollectionModeIsFullAndBillingDetailsIncompleteThenCardUpdateParamsIsNull() {
        var cardUpdateParams: CardUpdateParams? = null
        val handler = handler(
            addressCollectionMode = AddressCollectionMode.Full,
            onCardUpdateParamsChanged = {
                cardUpdateParams = it
            }
        )

        handler.updateCardBrand(CardBrand.Visa)

        handler.updateBillingDetails(
            billingDetailsFormState = PaymentSheetFixtures.billingDetailsFormState(
                postalCode = FormFieldEntry("94444", isComplete = true),
                line1 = FormFieldEntry("", isComplete = false), // Line1 is required for FULL mode but incomplete
            )
        )

        assertThat(cardUpdateParams).isNull()
    }

    @Test
    fun whenNoChangesButFormIsCompleteThenCardUpdateParamsIsNull() {
        var cardUpdateParams: CardUpdateParams? = null
        val handler = handler(
            onCardUpdateParamsChanged = {
                cardUpdateParams = it
            }
        )

        handler.updateBillingDetails(
            billingDetailsFormState = PaymentSheetFixtures.billingDetailsFormState(
                postalCode = FormFieldEntry(
                    value = PaymentMethodFixtures.BILLING_DETAILS.address?.postalCode ?: "",
                    isComplete = true
                ),
                country = FormFieldEntry(
                    value = PaymentMethodFixtures.BILLING_DETAILS.address?.country ?: "",
                    isComplete = true
                ),
            )
        )

        assertThat(cardUpdateParams).isNull()
    }

    @Test
    fun whenChangesAndFormIsCompleteThenCardUpdateParamsIsUpdated() {
        var cardUpdateParams: CardUpdateParams? = null
        val handler = handler(
            onCardUpdateParamsChanged = {
                cardUpdateParams = it
            }
        )

        handler.updateBillingDetails(
            billingDetailsFormState = PaymentSheetFixtures.billingDetailsFormState(
                postalCode = FormFieldEntry("94444", isComplete = true), // Changed postal code
                country = FormFieldEntry("US", isComplete = true),
            )
        )

        assertThat(cardUpdateParams).isNotNull()
        assertThat(cardUpdateParams?.billingDetails?.address?.postalCode).isEqualTo("94444")
    }

    private val EditCardDetailsInteractor.uiState
        get() = this.state.value

    private val EditCardDetailsInteractor.selectedBrand
        get() = uiState.selectedCardBrand.brand

    private fun handler(
        card: PaymentMethod.Card = PaymentMethodFixtures.CARD_WITH_NETWORKS,
        cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter,
        isCbcModifiable: Boolean = true,
        areExpiryDateAndAddressModificationSupported: Boolean = true,
        addressCollectionMode: AddressCollectionMode = AddressCollectionMode.Automatic,
        billingDetails: PaymentMethod.BillingDetails? = PaymentMethodFixtures.BILLING_DETAILS,
        onBrandChoiceChanged: (CardBrand) -> Unit = {},
        onCardUpdateParamsChanged: (CardUpdateParams?) -> Unit = {}
    ): EditCardDetailsInteractor {
        return DefaultEditCardDetailsInteractor.Factory().create(
            cardBrandFilter = cardBrandFilter,
            onBrandChoiceChanged = onBrandChoiceChanged,
            coroutineScope = TestScope(testDispatcher),
            isCbcModifiable = isCbcModifiable,
            card = card,
            onCardUpdateParamsChanged = onCardUpdateParamsChanged,
            areExpiryDateAndAddressModificationSupported = areExpiryDateAndAddressModificationSupported,
            billingDetails = billingDetails,
            addressCollectionMode = addressCollectionMode,
        )
    }
}
