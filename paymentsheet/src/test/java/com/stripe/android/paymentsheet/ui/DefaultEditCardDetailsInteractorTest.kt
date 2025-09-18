package com.stripe.android.paymentsheet.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.isInstanceOf
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.CardUpdateParams
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.ui.core.elements.CardBillingAddressElement
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
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
        val card = PaymentMethodFixtures.CARD_WITH_NETWORKS
        val handler = handler()

        val state = handler.uiState
        assertThat(state.payload).isEqualTo(
            EditCardPayload(
                last4 = card.last4,
                expiryMonth = card.expiryMonth,
                expiryYear = card.expiryYear,
                brand = card.brand,
                displayBrand = card.displayBrand,
                networks = card.networks?.available,
                billingDetails = PaymentMethodFixtures.BILLING_DETAILS,
            )
        )
        assertThat(state.cardDetailsState?.selectedCardBrand?.brand).isEqualTo(CardBrand.CartesBancaires)
    }

    @Test
    fun testInitialStateForCardWithNoNetworks() {
        val card = PaymentMethodFixtures.CARD
        val handler = handler(card = card)

        val state = handler.uiState
        assertThat(state.payload).isEqualTo(
            EditCardPayload(
                last4 = card.last4,
                expiryMonth = card.expiryMonth,
                expiryYear = card.expiryYear,
                brand = card.brand,
                displayBrand = card.displayBrand,
                networks = card.networks?.available,
                billingDetails = PaymentMethodFixtures.BILLING_DETAILS,
            )
        )
        assertThat(state.cardDetailsState?.selectedCardBrand?.brand).isEqualTo(CardBrand.Unknown)
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
        assertThat(state.cardDetailsState?.expiryDateState?.enabled).isTrue()
    }

    @Test
    fun testInitialStateWithFullDetailsEditDisabled() {
        val handler = handler(
            areExpiryDateAndAddressModificationSupported = false
        )

        val state = handler.uiState
        assertThat(state.billingDetailsForm).isNull()
        assertThat(state.cardDetailsState?.expiryDateState?.enabled).isFalse()
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
        assertThat(state.cardDetailsState?.shouldShowCardBrandDropdown).isFalse()
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

    @Test
    fun whenRequiresModificationIsFalseAndFormIsCompleteThenCardUpdateParamsIsUpdated() {
        var cardUpdateParams: CardUpdateParams? = null
        val handler = handler(
            requiresModification = false,
            onCardUpdateParamsChanged = {
                cardUpdateParams = it
            }
        )

        // No changes made, but requiresModification is false, so params should be updated
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

        assertThat(cardUpdateParams).isNotNull()
    }

    @Test
    fun whenRequiresModificationIsTrueAndNoChangesAndFormIsCompleteThenCardUpdateParamsIsNull() {
        var cardUpdateParams: CardUpdateParams? = null
        val handler = handler(
            requiresModification = true,
            onCardUpdateParamsChanged = {
                cardUpdateParams = it
            }
        )

        // No changes made, and requiresModification is true, so params should be null
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
    fun whenRequiresModificationIsFalseAndFormIsIncompleteThenCardUpdateParamsIsNull() {
        var cardUpdateParams: CardUpdateParams? = null
        val handler = handler(
            requiresModification = false,
            onCardUpdateParamsChanged = {
                cardUpdateParams = it
            }
        )

        // Form is incomplete, so params should be null regardless of requiresModification
        handler.updateBillingDetails(
            billingDetailsFormState = PaymentSheetFixtures.billingDetailsFormState(
                postalCode = FormFieldEntry("", isComplete = false),
                country = FormFieldEntry("US", isComplete = true),
            )
        )

        assertThat(cardUpdateParams).isNull()
    }

    @Test
    fun whenRequiresModificationIsFalseAndCardBrandNotChangedThenCardUpdateParamsIsUpdated() {
        var cardUpdateParams: CardUpdateParams? = null
        val handler = handler(
            requiresModification = false,
            onCardUpdateParamsChanged = {
                cardUpdateParams = it
            }
        )

        // Select the same card brand (no change)
        handler.updateCardBrand(CardBrand.CartesBancaires)

        assertThat(cardUpdateParams).isNotNull()
        assertThat(cardUpdateParams?.cardBrand).isEqualTo(CardBrand.CartesBancaires)
    }

    @Test
    fun whenAllBillingCountriesProvidedThenShouldUseAll() = runTest {
        val handler = handler(
            allowedCountries = emptySet()
        )

        handler.state.test {
            val state = awaitItem()

            val billingAddressForm = state.billingDetailsForm

            assertThat(billingAddressForm).isNotNull()

            val addressSectionElement = requireNotNull(billingAddressForm).addressSectionElement

            assertThat(addressSectionElement.fields.size).isEqualTo(1)
            assertThat(addressSectionElement.fields.firstOrNull()).isInstanceOf<CardBillingAddressElement>()

            val cardBillingAddressElement = addressSectionElement.fields[0] as CardBillingAddressElement

            assertThat(cardBillingAddressElement.countryElement.controller.displayItems)
                .hasSize(CountryUtils.supportedBillingCountries.size)
        }
    }

    @Test
    fun whenLimitedBillingCountriesUsedThenShouldUseOnlyProvidedCountries() = runTest {
        val handler = handler(
            allowedCountries = setOf("US", "CA")
        )

        handler.state.test {
            val state = awaitItem()

            val billingAddressForm = state.billingDetailsForm

            assertThat(billingAddressForm).isNotNull()

            val addressSectionElement = requireNotNull(billingAddressForm).addressSectionElement

            assertThat(addressSectionElement.fields.size).isEqualTo(1)
            assertThat(addressSectionElement.fields.firstOrNull()).isInstanceOf<CardBillingAddressElement>()

            val cardBillingAddressElement = addressSectionElement.fields[0] as CardBillingAddressElement

            assertThat(cardBillingAddressElement.countryElement.controller.displayItems).containsExactly(
                "\uD83C\uDDFA\uD83C\uDDF8 United States",
                "\uD83C\uDDE8\uD83C\uDDE6 Canada"
            )
        }
    }

    @Test
    fun whenValidateIsCalled_shouldSetAllFieldsToValidationState() = runTest {
        val handler = handler(
            billingDetails = null,
            nameCollection = CollectionMode.Always,
            addressCollectionMode = AddressCollectionMode.Full
        )

        handler.handleViewAction(EditCardDetailsInteractor.ViewAction.DateChanged(""))

        handler.state.test {
            val initialState = awaitItem()

            val initialCardState = requireNotNull(initialState.cardDetailsState)
            val initialBillingForm = requireNotNull(initialState.billingDetailsForm)

            assertThat(initialCardState.expiryDateState.shouldShowError()).isFalse()
            assertThat(initialCardState.expiryDateState.sectionError()).isNull()

            assertThat(initialBillingForm.nameElement).isNotNull()
            assertThat(initialBillingForm.nameElement?.controller?.error?.value).isNull()
            assertThat(initialBillingForm.addressSectionElement.controller.error.value).isNull()

            handler.handleViewAction(EditCardDetailsInteractor.ViewAction.Validate)

            val validatingState = awaitItem()

            val validatingCardState = requireNotNull(validatingState.cardDetailsState)
            val validatingBillingForm = requireNotNull(validatingState.billingDetailsForm)

            assertThat(validatingCardState.expiryDateState.shouldShowError()).isTrue()
            assertThat(validatingCardState.expiryDateState.sectionError()).isNotNull()

            assertThat(validatingBillingForm.nameElement).isNotNull()
            assertThat(validatingBillingForm.nameElement?.controller?.error?.value).isNotNull()
            assertThat(validatingBillingForm.addressSectionElement.controller.error.value).isNotNull()
        }
    }

    private val EditCardDetailsInteractor.uiState
        get() = this.state.value

    private val EditCardDetailsInteractor.selectedBrand
        get() = uiState.cardDetailsState?.selectedCardBrand?.brand ?: CardBrand.Unknown

    private fun handler(
        card: PaymentMethod.Card = PaymentMethodFixtures.CARD_WITH_NETWORKS,
        cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter,
        isCbcModifiable: Boolean = true,
        areExpiryDateAndAddressModificationSupported: Boolean = true,
        addressCollectionMode: AddressCollectionMode = AddressCollectionMode.Automatic,
        nameCollection: CollectionMode = CollectionMode.Automatic,
        allowedCountries: Set<String> = emptySet(),
        billingDetails: PaymentMethod.BillingDetails? = PaymentMethodFixtures.BILLING_DETAILS,
        requiresModification: Boolean = true,
        onBrandChoiceChanged: (CardBrand) -> Unit = {},
        onCardUpdateParamsChanged: (CardUpdateParams?) -> Unit = {}
    ): EditCardDetailsInteractor {
        return DefaultEditCardDetailsInteractor.Factory().create(
            coroutineScope = TestScope(testDispatcher),
            cardEditConfiguration = CardEditConfiguration(
                cardBrandFilter = cardBrandFilter,
                isCbcModifiable = isCbcModifiable,
                areExpiryDateAndAddressModificationSupported = areExpiryDateAndAddressModificationSupported,
            ),
            requiresModification = requiresModification,
            payload = EditCardPayload.create(card, billingDetails),
            billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
                address = addressCollectionMode,
                name = nameCollection,
                allowedCountries = allowedCountries,
            ),
            onBrandChoiceChanged = onBrandChoiceChanged,
            onCardUpdateParamsChanged = onCardUpdateParamsChanged,
        )
    }
}
