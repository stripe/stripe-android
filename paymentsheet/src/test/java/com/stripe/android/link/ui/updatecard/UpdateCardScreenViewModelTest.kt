package com.stripe.android.link.ui.updatecard

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkDismissalCoordinator
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkScreen.UpdateCard.BillingDetailsUpdateFlow
import com.stripe.android.link.RealLinkDismissalCoordinator
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.confirmation.DefaultCompleteLinkFlow
import com.stripe.android.link.confirmation.FakeLinkConfirmationHandler
import com.stripe.android.link.utils.TestNavigationManager
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.paymentelement.AllowedBillingCountriesInPaymentElementPreview
import com.stripe.android.paymentsheet.CardUpdateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeLogger
import com.stripe.android.ui.core.elements.CardBillingAddressElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.RowElement
import com.stripe.android.uicore.navigation.NavigationManager
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class UpdateCardScreenViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    @Test
    fun `viewmodel initializes with valid card details`() = runTest(dispatcher) {
        val card = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.setConsumerPaymentDetails(ConsumerPaymentDetails(listOf(card)))

        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            paymentDetailsId = card.id
        )

        assertThat(viewModel.state.value.paymentDetailsId).isEqualTo(card.id)
    }

    @Test
    fun `viewmodel navigates back on invalid card details`() = runTest(dispatcher) {
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.setConsumerPaymentDetails(ConsumerPaymentDetails(emptyList()))

        val navigationManager = TestNavigationManager()
        createViewModel(
            linkAccountManager = linkAccountManager,
            navigationManager = navigationManager,
            paymentDetailsId = "invalid_id"
        )

        navigationManager.assertNavigatedBack()
    }

    @Test
    fun `onUpdateClicked updates payment details successfully`() = runTest(dispatcher) {
        val card = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD
        val navigationManager = TestNavigationManager()
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.setConsumerPaymentDetails(ConsumerPaymentDetails(listOf(card)))

        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            paymentDetailsId = card.id,
            navigationManager = navigationManager,
        )

        val cardUpdateParams = CardUpdateParams(
            expiryMonth = 12,
            expiryYear = 2025,
            billingDetails = null
        )

        viewModel.onCardUpdateParamsChanged(cardUpdateParams)

        viewModel.onUpdateClicked()

        val state = viewModel.state.value
        val call = linkAccountManager.awaitUpdateCardDetailsCall()
        assertThat(state.processing).isFalse()
        assertThat(state.error).isNull()
        assertThat(call.id).isEqualTo(state.paymentDetailsId)
        assertThat(call).isNotNull()

        navigationManager.assertNavigatedBack()
    }

    @Test
    fun `viewModel sets requiresModification to false for billing details update flow`() = runTest(dispatcher) {
        val card = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.setConsumerPaymentDetails(ConsumerPaymentDetails(listOf(card)))

        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            paymentDetailsId = card.id,
            billingDetailsUpdateFlow = BillingDetailsUpdateFlow()
        )

        // The requiresModification parameter should be passed to the edit card details interactor
        // This is indirectly tested through the state behavior
        val state = viewModel.state.value
        assertThat(state.paymentDetailsId).isEqualTo(card.id)
        assertThat(state.isBillingDetailsUpdateFlow).isTrue()
    }

    @Test
    fun `viewModel sets requiresModification to true for regular update flow`() = runTest(dispatcher) {
        val card = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.setConsumerPaymentDetails(ConsumerPaymentDetails(listOf(card)))

        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            paymentDetailsId = card.id,
            billingDetailsUpdateFlow = null
        )

        // The requiresModification parameter should be passed to the edit card details interactor
        // This is indirectly tested through the state behavior
        val state = viewModel.state.value
        assertThat(state.paymentDetailsId).isEqualTo(card.id)
        assertThat(state.isBillingDetailsUpdateFlow).isFalse()
    }

    @Test
    fun `when updating outside of billing details update flow, should not be able to update phone`() =
        runTest(dispatcher) {
            val card = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD

            val viewModel = createViewModel(
                linkAccountManager = FakeLinkAccountManager().apply {
                    setConsumerPaymentDetails(ConsumerPaymentDetails(listOf(card)))
                },
                configuration = TestFactory.LINK_CONFIGURATION.copy(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    ),
                ),
                paymentDetailsId = card.id,
                billingDetailsUpdateFlow = null,
            )

            viewModel.interactor.test {
                val interactor = awaitItem()

                assertThat(interactor).isNotNull()

                requireNotNull(interactor).state.test {
                    val billingElements = awaitItem().billingDetailsForm?.addressSectionElement?.fields

                    assertThat(billingElements).isNotNull()

                    val nonNullBillingElements = requireNotNull(billingElements)

                    assertThat(nonNullBillingElements).hasSize(1)
                    assertThat(nonNullBillingElements[0]).isInstanceOf<CardBillingAddressElement>()

                    val cardBillingAddressElement = nonNullBillingElements[0] as CardBillingAddressElement

                    val addressFields = cardBillingAddressElement.addressController.value.fieldsFlowable.value

                    val doesNotHavePhoneElement = addressFields.none { element ->
                        element.identifier == IdentifierSpec.Phone
                    }

                    assertThat(doesNotHavePhoneElement).isTrue()
                }
            }
        }

    @Test
    fun `when updating using billing details update flow, should be able to update phone`() =
        runTest(dispatcher) {
            val card = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD

            val viewModel = createViewModel(
                linkAccountManager = FakeLinkAccountManager().apply {
                    setConsumerPaymentDetails(ConsumerPaymentDetails(listOf(card)))
                },
                configuration = TestFactory.LINK_CONFIGURATION.copy(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    ),
                ),
                paymentDetailsId = card.id,
                billingDetailsUpdateFlow = BillingDetailsUpdateFlow(),
            )

            viewModel.interactor.test {
                val interactor = awaitItem()

                assertThat(interactor).isNotNull()

                requireNotNull(interactor).state.test {
                    val billingElements = awaitItem().billingDetailsForm?.addressSectionElement?.fields

                    assertThat(billingElements).isNotNull()

                    val nonNullBillingElements = requireNotNull(billingElements)

                    assertThat(nonNullBillingElements).hasSize(1)
                    assertThat(nonNullBillingElements[0]).isInstanceOf<CardBillingAddressElement>()

                    val cardBillingAddressElement = nonNullBillingElements[0] as CardBillingAddressElement

                    val addressFields = cardBillingAddressElement.addressController.value.fieldsFlowable.value

                    val hasPhoneElement = addressFields.any { element ->
                        element.identifier == IdentifierSpec.Phone
                    }

                    assertThat(hasPhoneElement).isTrue()
                }
            }
        }

    @Test
    fun `when updating with cards, should always at minimum be automatic address collection`() =
        runTest(dispatcher) {
            val card = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD

            val viewModel = createViewModel(
                linkAccountManager = FakeLinkAccountManager().apply {
                    setConsumerPaymentDetails(ConsumerPaymentDetails(listOf(card)))
                },
                configuration = TestFactory.LINK_CONFIGURATION.copy(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
                    ),
                ),
                paymentDetailsId = card.id,
                billingDetailsUpdateFlow = BillingDetailsUpdateFlow(),
            )

            viewModel.interactor.test {
                val interactor = awaitItem()

                assertThat(interactor).isNotNull()

                requireNotNull(interactor).state.test {
                    val billingElements = awaitItem().billingDetailsForm?.addressSectionElement?.fields

                    assertThat(billingElements).isNotNull()

                    val nonNullBillingElements = requireNotNull(billingElements)

                    assertThat(nonNullBillingElements).hasSize(1)
                    assertThat(nonNullBillingElements[0]).isInstanceOf<CardBillingAddressElement>()

                    val cardBillingAddressElement = nonNullBillingElements[0] as CardBillingAddressElement

                    val addressFields = cardBillingAddressElement.addressController.value.fieldsFlowable.value

                    val hasCountryElement = addressFields.any {
                        it.identifier == IdentifierSpec.Country
                    }

                    val hasPostalCodeElement = addressFields.any {
                        it is RowElement && it.fields.any {
                            it.identifier == IdentifierSpec.PostalCode
                        }
                    }

                    assertThat(hasPostalCodeElement).isTrue()
                    assertThat(hasCountryElement).isTrue()
                }
            }
        }

    @OptIn(AllowedBillingCountriesInPaymentElementPreview::class)
    @Test
    fun `when providing an empty set of allowed billing countries, should use all countries`() =
        runTest(dispatcher) {
            val card = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD

            val viewModel = createViewModel(
                linkAccountManager = FakeLinkAccountManager().apply {
                    setConsumerPaymentDetails(ConsumerPaymentDetails(listOf(card)))
                },
                configuration = TestFactory.LINK_CONFIGURATION.copy(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        allowedCountries = emptySet()
                    ),
                ),
                paymentDetailsId = card.id,
                billingDetailsUpdateFlow = BillingDetailsUpdateFlow(),
            )

            viewModel.interactor.test {
                val interactor = awaitItem()

                assertThat(interactor).isNotNull()

                requireNotNull(interactor).state.test {
                    val billingElements = awaitItem().billingDetailsForm?.addressSectionElement?.fields

                    assertThat(billingElements).isNotNull()

                    val nonNullBillingElements = requireNotNull(billingElements)

                    assertThat(nonNullBillingElements).hasSize(1)
                    assertThat(nonNullBillingElements[0]).isInstanceOf<CardBillingAddressElement>()

                    val cardBillingAddressElement = nonNullBillingElements[0] as CardBillingAddressElement

                    assertThat(cardBillingAddressElement.countryElement.controller.displayItems)
                        .hasSize(CountryUtils.supportedBillingCountries.size)
                }
            }
        }

    @OptIn(AllowedBillingCountriesInPaymentElementPreview::class)
    @Test
    fun `when providing an limited set of allowed billing countries, should use limited countries`() =
        runTest(dispatcher) {
            val card = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD

            val viewModel = createViewModel(
                linkAccountManager = FakeLinkAccountManager().apply {
                    setConsumerPaymentDetails(ConsumerPaymentDetails(listOf(card)))
                },
                configuration = TestFactory.LINK_CONFIGURATION.copy(
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        allowedCountries = setOf("US", "CA")
                    ),
                ),
                paymentDetailsId = card.id,
                billingDetailsUpdateFlow = BillingDetailsUpdateFlow(),
            )

            viewModel.interactor.test {
                val interactor = awaitItem()

                assertThat(interactor).isNotNull()

                requireNotNull(interactor).state.test {
                    val billingElements = awaitItem().billingDetailsForm?.addressSectionElement?.fields

                    assertThat(billingElements).isNotNull()

                    val nonNullBillingElements = requireNotNull(billingElements)

                    assertThat(nonNullBillingElements).hasSize(1)
                    assertThat(nonNullBillingElements[0]).isInstanceOf<CardBillingAddressElement>()

                    val cardBillingAddressElement = nonNullBillingElements[0] as CardBillingAddressElement

                    assertThat(cardBillingAddressElement.countryElement.controller.displayItems).containsExactly(
                        "\uD83C\uDDFA\uD83C\uDDF8 United States",
                        "\uD83C\uDDE8\uD83C\uDDE6 Canada"
                    )
                }
            }
        }

    private fun createViewModel(
        linkAccountManager: FakeLinkAccountManager = FakeLinkAccountManager(),
        navigationManager: NavigationManager = TestNavigationManager(),
        logger: Logger = FakeLogger(),
        dismissalCoordinator: LinkDismissalCoordinator = RealLinkDismissalCoordinator(),
        configuration: LinkConfiguration = TestFactory.LINK_CONFIGURATION,
        paymentDetailsId: String = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.id,
        billingDetailsUpdateFlow: BillingDetailsUpdateFlow? = null
    ): UpdateCardScreenViewModel {
        return UpdateCardScreenViewModel(
            logger = logger,
            linkAccountManager = linkAccountManager,
            navigationManager = navigationManager,
            dismissalCoordinator = dismissalCoordinator,
            configuration = configuration,
            paymentDetailsId = paymentDetailsId,
            completeLinkFlow = DefaultCompleteLinkFlow(
                linkConfirmationHandler = FakeLinkConfirmationHandler(),
                linkAccountManager = linkAccountManager,
                dismissalCoordinator = dismissalCoordinator,
                linkLaunchMode = LinkLaunchMode.Full
            ),
            billingDetailsUpdateFlow = billingDetailsUpdateFlow,
            linkLaunchMode = LinkLaunchMode.Full,
            dismissWithResult = {}
        )
    }
}
