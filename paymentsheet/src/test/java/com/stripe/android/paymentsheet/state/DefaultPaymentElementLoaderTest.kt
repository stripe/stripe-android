package com.stripe.android.paymentsheet.state

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ExperimentalCardBrandFilteringApi
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.model.CountryCode
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.LinkSignupMode.AlongsideSaveForFutureUse
import com.stripe.android.link.ui.inline.LinkSignupMode.InsteadOfSaveForFutureUse
import com.stripe.android.lpmfoundations.luxe.LpmRepository
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentIntent.ConfirmationMethod.Manual
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeIntent.Status.Canceled
import com.stripe.android.model.StripeIntent.Status.Succeeded
import com.stripe.android.model.wallets.Wallet
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.ExperimentalCustomerSessionApi
import com.stripe.android.paymentsheet.FakePrefsRepository
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.cvcrecollection.CvcRecollectionHandlerImpl
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import com.stripe.android.paymentsheet.state.PaymentSheetLoadingException.PaymentIntentInTerminalState
import com.stripe.android.paymentsheet.utils.FakeUserFacingLogger
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.PaymentMethodFactory.update
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.ExternalPaymentMethodsRepository
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.FakeElementsSessionRepository
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.eq
import org.mockito.Captor
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.capture
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class DefaultPaymentElementLoaderTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val eventReporter = mock<EventReporter>()

    private val customerRepository = FakeCustomerRepository(PAYMENT_METHODS)
    private val lpmRepository = LpmRepository()

    private val prefsRepository = FakePrefsRepository()

    @Captor
    private lateinit var paymentMethodTypeCaptor: ArgumentCaptor<List<PaymentMethod.Type>>

    private val readyGooglePayRepository = mock<GooglePayRepository>()
    private val unreadyGooglePayRepository = mock<GooglePayRepository>()

    @BeforeTest
    fun setup() {
        MockitoAnnotations.openMocks(this)

        whenever(readyGooglePayRepository.isReady()).thenReturn(
            flow {
                emit(true)
            }
        )

        whenever(unreadyGooglePayRepository.isReady()).thenReturn(
            flow {
                emit(false)
            }
        )
    }

    @OptIn(ExperimentalCardBrandFilteringApi::class)
    @Test
    fun `load with configuration should return expected result`() = runTest {
        prefsRepository.savePaymentSelection(
            PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )

        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
        )

        val config = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY

        assertThat(
            loader.load(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                config,
                initializedViaCompose = false,
            ).getOrThrow()
        ).isEqualTo(
            PaymentSheetState.Full(
                config = config.asCommonConfiguration(),
                customer = CustomerState(
                    id = config.customer!!.id,
                    ephemeralKeySecret = config.customer.ephemeralKeySecret,
                    paymentMethods = PAYMENT_METHODS,
                    permissions = CustomerState.Permissions(
                        canRemovePaymentMethods = true,
                        canRemoveDuplicates = false,
                    ),
                ),
                paymentSelection = PaymentSelection.Saved(
                    paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                ),
                linkState = null,
                validationError = null,
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                    stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
                    allowsDelayedPaymentMethods = false,
                    sharedDataSpecs = emptyList(),
                    hasCustomerConfiguration = true,
                    isGooglePayReady = true,
                    linkMode = null,
                    cardBrandFilter = PaymentSheetCardBrandFilter(PaymentSheet.CardBrandAcceptance.all())
                ),
            )
        )
    }

    @Test
    fun `load without customer should return expected result`() = runTest {
        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            PaymentSheetFixtures.CONFIG_MINIMUM,
            initializedViaCompose = false,
        ).getOrThrow()
        assertThat(result.paymentMethodMetadata.hasCustomerConfiguration).isFalse()
    }

    @Test
    fun `load with google pay kill switch enabled should return expected result`() = runTest {
        prefsRepository.savePaymentSelection(
            PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )

        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
            isGooglePayReady = true,
            isGooglePayEnabledFromBackend = false,
        )

        assertThat(
            loader.load(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                initializedViaCompose = false,
            ).getOrThrow().paymentMethodMetadata.isGooglePayReady
        ).isFalse()
    }

    @Test
    fun `Should default to first payment method if customer has payment methods`() = runTest {
        prefsRepository.savePaymentSelection(null)

        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
            isGooglePayReady = true,
            customerRepo = FakeCustomerRepository(paymentMethods = PAYMENT_METHODS),
        )

        val state = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
            initializedViaCompose = false,
        ).getOrThrow()

        assertThat(state.paymentSelection).isEqualTo(
            PaymentSelection.Saved(paymentMethod = PAYMENT_METHODS.first())
        )
    }

    @Test
    fun `Should default to last used payment method if available even if customer has payment methods`() = runTest {
        prefsRepository.savePaymentSelection(PaymentSelection.GooglePay)

        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
            isGooglePayReady = true,
            customerRepo = FakeCustomerRepository(paymentMethods = PAYMENT_METHODS),
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
            initializedViaCompose = false,
        ).getOrThrow()

        assertThat(result.paymentSelection).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `Should default to no payment method if customer has no payment methods and no last used payment method`() =
        runTest {
            prefsRepository.savePaymentSelection(null)

            val loader = createPaymentElementLoader(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
                isGooglePayReady = true,
                customerRepo = FakeCustomerRepository(paymentMethods = emptyList()),
            )

            val result = loader.load(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                paymentSheetConfiguration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                initializedViaCompose = false,
            ).getOrThrow()

            assertThat(result.paymentSelection).isNull()
        }

    @Test
    fun `load() with customer should fetch only supported payment method types`() =
        runTest {
            val customerRepository = mock<CustomerRepository> {
                whenever(it.getPaymentMethods(any(), any(), any())).thenReturn(Result.success(emptyList()))
            }

            val paymentMethodTypes = listOf(
                "card", // valid and supported
                "fpx", // valid but not supported
                "invalid_type" // unknown type
            )

            val loader = createPaymentElementLoader(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = paymentMethodTypes
                ),
                customerRepo = customerRepository
            )

            loader.load(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                initializedViaCompose = false,
            )

            verify(customerRepository).getPaymentMethods(
                any(),
                capture(paymentMethodTypeCaptor),
                any(),
            )
            assertThat(paymentMethodTypeCaptor.allValues.flatten())
                .containsExactly(PaymentMethod.Type.Card)
        }

    @Test
    fun `when allowsDelayedPaymentMethods is false then delayed payment methods are filtered out`() =
        runTest {
            val customerRepository = mock<CustomerRepository> {
                whenever(it.getPaymentMethods(any(), any(), any())).thenReturn(Result.success(emptyList()))
            }

            val loader = createPaymentElementLoader(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf(
                        PaymentMethod.Type.Card.code,
                        PaymentMethod.Type.SepaDebit.code,
                        PaymentMethod.Type.AuBecsDebit.code
                    )
                ),
                customerRepo = customerRepository
            )

            loader.load(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                initializedViaCompose = false,
            )

            verify(customerRepository).getPaymentMethods(
                any(),
                capture(paymentMethodTypeCaptor),
                any(),
            )
            assertThat(paymentMethodTypeCaptor.value)
                .containsExactly(PaymentMethod.Type.Card)
        }

    @Test
    fun `when getPaymentMethods fails in test mode, load() fails`() =
        runTest {
            val expectedException = IllegalArgumentException("invalid API key provided")
            val customerRepository =
                FakeCustomerRepository(PAYMENT_METHODS, onGetPaymentMethods = { Result.failure(expectedException) })
            val loader = createPaymentElementLoader(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    isLiveMode = false,
                ),
                customerRepo = customerRepository
            )

            val loadResult = loader.load(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                initializedViaCompose = false,
            )

            val actualException = loadResult.exceptionOrNull()
            assertThat(actualException?.cause).isEqualTo(expectedException)
        }

    @Test
    fun `load() with customer should filter out invalid payment method types`() =
        runTest {
            val result = createPaymentElementLoader(
                customerRepo = FakeCustomerRepository(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(card = null), // invalid
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                )
            ).load(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                initializedViaCompose = false,
            ).getOrThrow()

            assertThat(result.customer?.paymentMethods)
                .containsExactly(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        }

    @Test
    fun `load() with customer should not filter out cards attached to a wallet`() =
        runTest {
            val cardWithAmexWallet = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
                card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.card?.copy(
                    wallet = Wallet.AmexExpressCheckoutWallet("3000")
                )
            )
            val result = createPaymentElementLoader(
                customerRepo = FakeCustomerRepository(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                        cardWithAmexWallet
                    )
                )
            ).load(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                initializedViaCompose = false,
            ).getOrThrow()

            assertThat(result.customer?.paymentMethods)
                .containsExactly(PaymentMethodFixtures.CARD_PAYMENT_METHOD, cardWithAmexWallet)
        }

    @Test
    fun `load() with customer should allow sepa`() = runTest {
        var requestPaymentMethodTypes: List<PaymentMethod.Type>? = null
        val result = createPaymentElementLoader(
            customerRepo = object : FakeCustomerRepository() {
                override suspend fun getPaymentMethods(
                    customerInfo: CustomerRepository.CustomerInfo,
                    types: List<PaymentMethod.Type>,
                    silentlyFail: Boolean
                ): Result<List<PaymentMethod>> {
                    requestPaymentMethodTypes = types
                    return Result.success(
                        listOf(
                            PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                            PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD,
                        )
                    )
                }
            },
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "sepa_debit")
            )
        ).load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.CLIENT_SECRET,
            ),
            paymentSheetConfiguration = PaymentSheetFixtures.CONFIG_CUSTOMER.copy(
                allowsDelayedPaymentMethods = true,
            ),
            initializedViaCompose = false,
        ).getOrThrow()

        assertThat(result.customer?.paymentMethods)
            .containsExactly(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD,
            )
        assertThat(requestPaymentMethodTypes)
            .containsExactly(PaymentMethod.Type.Card, PaymentMethod.Type.SepaDebit)
    }

    @Test
    fun `load() when PaymentIntent has invalid status should return null`() = runTest {
        val paymentIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
            paymentMethod = PaymentMethodFactory.card(),
        )

        val result = createPaymentElementLoader(
            stripeIntent = paymentIntent,
        ).load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
            initializedViaCompose = false,
        ).getOrThrow()

        assertThat(result.validationError).isEqualTo(PaymentIntentInTerminalState(Succeeded))
    }

    @Test
    fun `load() when PaymentIntent has invalid confirmationMethod should return null`() = runTest {
        val result = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                confirmationMethod = Manual,
            ),
        ).load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            initializedViaCompose = false,
        ).getOrThrow()

        assertThat(result.validationError).isEqualTo(PaymentSheetLoadingException.InvalidConfirmationMethod(Manual))
    }

    @Test
    fun `Defaults to first existing payment method for known customer`() = runTest {
        val result = createPaymentElementLoader(
            customerRepo = FakeCustomerRepository(paymentMethods = PAYMENT_METHODS)
        ).load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = mockConfiguration(
                customer = PaymentSheet.CustomerConfiguration(
                    id = "some_id",
                    ephemeralKeySecret = "some_key"
                )
            ),
            initializedViaCompose = false,
        ).getOrThrow()

        val expectedPaymentMethod = requireNotNull(PAYMENT_METHODS.first())
        assertThat(result.paymentSelection).isEqualTo(PaymentSelection.Saved(expectedPaymentMethod))
    }

    @Test
    fun `Considers Link logged in if the account is verified`() = runTest {
        val loader = createPaymentElementLoader(linkAccountState = AccountStatus.Verified)

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            initializedViaCompose = false,
        ).getOrThrow()

        assertThat(result.linkState?.loginState).isEqualTo(LinkState.LoginState.LoggedIn)
    }

    @Test
    fun `Considers Link as needing verification if the account needs verification`() = runTest {
        val loader = createPaymentElementLoader(linkAccountState = AccountStatus.NeedsVerification)

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            initializedViaCompose = false,
        ).getOrThrow()

        assertThat(result.linkState?.loginState).isEqualTo(LinkState.LoginState.NeedsVerification)
    }

    @Test
    fun `Considers Link as needing verification if the account is being verified`() = runTest {
        val loader = createPaymentElementLoader(linkAccountState = AccountStatus.VerificationStarted)

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            initializedViaCompose = false,
        ).getOrThrow()

        assertThat(result.linkState?.loginState).isEqualTo(LinkState.LoginState.NeedsVerification)
    }

    @Test
    fun `Considers Link as logged out correctly`() = runTest {
        val loader = createPaymentElementLoader(linkAccountState = AccountStatus.SignedOut)

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            initializedViaCompose = false,
        ).getOrThrow()

        assertThat(result.linkState?.loginState).isEqualTo(LinkState.LoginState.LoggedOut)
    }

    @Test
    fun `Populates Link configuration correctly from billing details`() = runTest {
        val loader = createPaymentElementLoader()

        val billingDetails = PaymentSheet.BillingDetails(
            address = PaymentSheet.Address(country = "CA"),
            name = "Till",
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(
                defaultBillingDetails = billingDetails,
            ),
            initializedViaCompose = false,
        ).getOrThrow()

        val expectedLinkConfig = LinkConfiguration(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            merchantName = "Merchant",
            merchantCountryCode = null,
            customerInfo = LinkConfiguration.CustomerInfo(
                name = "Till",
                email = null,
                phone = null,
                billingCountryCode = "CA",
            ),
            shippingValues = null,
            passthroughModeEnabled = false,
            cardBrandChoice = null,
            flags = emptyMap(),
        )

        assertThat(result.linkState?.configuration).isEqualTo(expectedLinkConfig)
    }

    @Test
    fun `Populates Link configuration with shipping details if checkbox is selected`() = runTest {
        val loader = createPaymentElementLoader()

        val shippingDetails = AddressDetails(
            name = "Not Till",
            address = PaymentSheet.Address(country = "US"),
            isCheckboxSelected = true,
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(
                shippingDetails = shippingDetails,
            ),
            initializedViaCompose = false,
        ).getOrThrow()

        assertThat(result.linkState?.configuration?.shippingValues).isNotNull()
    }

    @Test
    fun `Populates Link configuration with passthrough mode`() = runTest {
        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
            linkSettings = ElementsSession.LinkSettings(
                linkFundingSources = emptyList(),
                linkPassthroughModeEnabled = true,
                linkMode = LinkMode.Passthrough,
                linkFlags = emptyMap(),
                disableLinkSignup = false,
            )
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            initializedViaCompose = false,
        ).getOrThrow()

        assertThat(result.linkState?.configuration?.passthroughModeEnabled).isTrue()
    }

    @Test
    fun `Populates Link configuration with link flags`() = runTest {
        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            linkSettings = ElementsSession.LinkSettings(
                linkFundingSources = emptyList(),
                linkPassthroughModeEnabled = false,
                linkMode = LinkMode.LinkPaymentMethod,
                linkFlags = mapOf(
                    "link_authenticated_change_event_enabled" to false,
                    "link_bank_incentives_enabled" to false,
                    "link_bank_onboarding_enabled" to false,
                    "link_email_verification_login_enabled" to false,
                    "link_financial_incentives_experiment_enabled" to false,
                    "link_local_storage_login_enabled" to true,
                    "link_only_for_payment_method_types_enabled" to false,
                    "link_passthrough_mode_enabled" to true,
                ),
                disableLinkSignup = false,
            )
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            initializedViaCompose = false,
        ).getOrThrow()

        val expectedFlags = mapOf(
            "link_authenticated_change_event_enabled" to false,
            "link_bank_incentives_enabled" to false,
            "link_bank_onboarding_enabled" to false,
            "link_email_verification_login_enabled" to false,
            "link_financial_incentives_experiment_enabled" to false,
            "link_local_storage_login_enabled" to true,
            "link_only_for_payment_method_types_enabled" to false,
            "link_passthrough_mode_enabled" to true,
        )

        assertThat(result.linkState?.configuration?.flags).containsExactlyEntriesIn(expectedFlags)
    }

    @Test
    fun `Populates Link configuration correctly with eligible card brand choice information`() = runTest {
        val loader = createPaymentElementLoader(
            cardBrandChoice = ElementsSession.CardBrandChoice(
                eligible = true,
                preferredNetworks = listOf("cartes_bancaires"),
            ),
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            initializedViaCompose = false,
        ).getOrThrow()

        val cardBrandChoice = result.linkState?.configuration?.cardBrandChoice

        assertThat(cardBrandChoice?.eligible).isTrue()
        assertThat(cardBrandChoice?.preferredNetworks).isEqualTo(listOf("cartes_bancaires"))
    }

    @Test
    fun `Populates Link configuration correctly with ineligible card brand choice information`() = runTest {
        val loader = createPaymentElementLoader(
            cardBrandChoice = ElementsSession.CardBrandChoice(
                eligible = false,
                preferredNetworks = listOf("cartes_bancaires"),
            ),
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            initializedViaCompose = false,
        ).getOrThrow()

        val cardBrandChoice = result.linkState?.configuration?.cardBrandChoice

        assertThat(cardBrandChoice?.eligible).isFalse()
        assertThat(cardBrandChoice?.preferredNetworks).isEqualTo(listOf("cartes_bancaires"))
    }

    @Test
    fun `Disables link sign up if used before`() = runTest {
        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            linkSettings = ElementsSession.LinkSettings(
                linkFundingSources = emptyList(),
                linkPassthroughModeEnabled = false,
                linkMode = LinkMode.LinkPaymentMethod,
                linkFlags = mapOf(),
                disableLinkSignup = false,
            ),
            linkStore = mock {
                on { hasUsedLink() } doReturn true
            }
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            initializedViaCompose = false,
        ).getOrThrow()

        assertThat(result.linkState?.signupMode).isNull()
        assertThat(result.paymentMethodMetadata.linkInlineConfiguration?.signupMode).isNull()
    }

    @Test
    fun `Disables link sign up when settings have it disabled`() = runTest {
        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            linkSettings = ElementsSession.LinkSettings(
                linkFundingSources = emptyList(),
                linkPassthroughModeEnabled = false,
                linkMode = LinkMode.LinkPaymentMethod,
                linkFlags = mapOf(),
                disableLinkSignup = true,
            )
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            initializedViaCompose = false,
        ).getOrThrow()

        assertThat(result.linkState?.signupMode).isNull()
        assertThat(result.paymentMethodMetadata.linkInlineConfiguration?.signupMode).isNull()
    }

    @Test
    fun `Disables Link inline signup if no valid funding source`() = runTest {
        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                linkFundingSources = listOf("us_bank_account")
            ),
            linkAccountState = AccountStatus.SignedOut,
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            initializedViaCompose = false,
        ).getOrThrow()

        assertThat(result.linkState?.signupMode).isNull()
        assertThat(result.paymentMethodMetadata.linkInlineConfiguration?.signupMode).isNull()
    }

    @Test
    fun `Disables Link inline signup if user already has an unverified account`() = runTest {
        val loader = createPaymentElementLoader(
            linkAccountState = AccountStatus.NeedsVerification,
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            initializedViaCompose = false,
        ).getOrThrow()

        assertThat(result.linkState?.signupMode).isNull()
        assertThat(result.paymentMethodMetadata.linkInlineConfiguration?.signupMode).isNull()
    }

    @Test
    fun `Disables Link inline signup if user already has an verified account`() = runTest {
        val loader = createPaymentElementLoader(
            linkAccountState = AccountStatus.Verified,
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            initializedViaCompose = false,
        ).getOrThrow()

        assertThat(result.linkState?.signupMode).isNull()
        assertThat(result.paymentMethodMetadata.linkInlineConfiguration?.signupMode).isNull()
    }

    @Test
    fun `Disables Link inline signup if there is an account error`() = runTest {
        val loader = createPaymentElementLoader(
            linkAccountState = AccountStatus.Error,
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            initializedViaCompose = false,
        ).getOrThrow()

        assertThat(result.linkState?.signupMode).isNull()
        assertThat(result.paymentMethodMetadata.linkInlineConfiguration?.signupMode).isNull()
    }

    @Test
    fun `Enables Link inline signup if valid card funding source`() = runTest {
        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                linkFundingSources = listOf("card"),
            ),
            linkAccountState = AccountStatus.SignedOut,
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            initializedViaCompose = false,
        ).getOrThrow()

        assertThat(result.linkState?.signupMode).isEqualTo(InsteadOfSaveForFutureUse)
        assertThat(result.paymentMethodMetadata.linkInlineConfiguration?.signupMode)
            .isEqualTo(InsteadOfSaveForFutureUse)
    }

    @Test
    fun `Enables Link inline signup if user has no account`() = runTest {
        val loader = createPaymentElementLoader(
            linkAccountState = AccountStatus.SignedOut,
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            initializedViaCompose = false,
        ).getOrThrow()

        assertThat(result.linkState?.signupMode).isEqualTo(InsteadOfSaveForFutureUse)
        assertThat(result.paymentMethodMetadata.linkInlineConfiguration?.signupMode)
            .isEqualTo(InsteadOfSaveForFutureUse)
    }

    @Test
    fun `Uses shipping address phone number if checkbox is selected`() = runTest {
        val loader = createPaymentElementLoader()

        val billingDetails = PaymentSheet.BillingDetails(phone = "123-456-7890")

        val shippingDetails = AddressDetails(
            address = PaymentSheet.Address(country = "US"),
            phoneNumber = "098-765-4321",
            isCheckboxSelected = true,
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(
                shippingDetails = shippingDetails,
                defaultBillingDetails = billingDetails,
            ),
            initializedViaCompose = false,
        ).getOrThrow()

        assertThat(result.linkState?.configuration?.customerInfo?.phone).isEqualTo(shippingDetails.phoneNumber)
    }

    @Test
    fun `Falls back to customer email if billing address email not provided`() = runTest {
        val loader = createPaymentElementLoader(
            customerRepo = FakeCustomerRepository(
                customer = mock {
                    on { email } doReturn "email@stripe.com"
                }
            ),
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                setupFutureUsage = StripeIntent.Usage.OffSession,
            )
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(
                customer = PaymentSheet.CustomerConfiguration(
                    id = "id",
                    ephemeralKeySecret = "key"
                ),
            ),
            initializedViaCompose = false,
        ).getOrThrow()

        assertThat(result.linkState?.configuration?.customerInfo?.email).isEqualTo("email@stripe.com")
    }

    @Test
    fun `Moves last-used customer payment method to the front of the list`() = runTest {
        val paymentMethods = PaymentMethodFixtures.createCards(10)
        val lastUsed = paymentMethods[6]
        prefsRepository.savePaymentSelection(PaymentSelection.Saved(lastUsed))

        val loader = createPaymentElementLoader(
            customerRepo = FakeCustomerRepository(paymentMethods = paymentMethods),
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(
                customer = PaymentSheet.CustomerConfiguration(
                    id = "id",
                    ephemeralKeySecret = "key",
                ),
            ),
            initializedViaCompose = false,
        ).getOrThrow()

        val observedElements = result.customer?.paymentMethods
        val expectedElements = listOf(lastUsed) + (paymentMethods - lastUsed)

        assertThat(observedElements).containsExactlyElementsIn(expectedElements).inOrder()
    }

    @Test
    fun `Returns failure if StripeIntent does not contain any supported payment method type`() = runTest {
        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("gold", "silver", "bronze"),
            ),
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            initializedViaCompose = false,
        ).exceptionOrNull()

        assertThat(result).isEqualTo(
            PaymentSheetLoadingException.NoPaymentMethodTypesAvailable(
                requested = "gold, silver, bronze",
            )
        )
    }

    @Test
    fun `Emits correct events when loading succeeds for non-deferred intent`() = runTest {
        val loader = createPaymentElementLoader(
            linkSettings = createLinkSettings(passthroughModeEnabled = false),
        )
        val initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret")

        loader.load(
            initializationMode = initializationMode,
            paymentSheetConfiguration = PaymentSheet.Configuration(
                merchantDisplayName = "Some Name",
                customer = PaymentSheet.CustomerConfiguration(
                    id = "cus_123",
                    ephemeralKeySecret = "some_secret",
                ),
            ),
            initializedViaCompose = false,
        )

        verify(eventReporter).onLoadStarted(eq(false))
        verify(eventReporter).onLoadSucceeded(
            paymentSelection = PaymentSelection.Saved(
                paymentMethod = PAYMENT_METHODS.first()
            ),
            linkMode = LinkMode.LinkPaymentMethod,
            googlePaySupported = true,
            currency = "usd",
            initializationMode = initializationMode,
            orderedLpms = listOf("card", "link"),
            requireCvcRecollection = false
        )
    }

    @Test
    fun `Emits correct events when loading succeeds with saved LPM selection`() = runTest {
        testSuccessfulLoadSendsEventsCorrectly(
            paymentSelection = PaymentSelection.Saved(
                paymentMethod = PAYMENT_METHODS.last()
            )
        )
    }

    @Test
    fun `Emits correct events when loading succeeds with saved Google Pay selection`() = runTest {
        testSuccessfulLoadSendsEventsCorrectly(
            paymentSelection = PaymentSelection.GooglePay
        )
    }

    @Test
    fun `Emits correct events when loading succeeds with saved Link selection`() = runTest {
        testSuccessfulLoadSendsEventsCorrectly(
            paymentSelection = PaymentSelection.Link
        )
    }

    @Test
    fun `Emits correct events when loading succeeds for deferred intent`() = runTest {
        val loader = createPaymentElementLoader(
            linkSettings = createLinkSettings(passthroughModeEnabled = false),
        )
        val initializationMode = PaymentSheet.InitializationMode.DeferredIntent(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                    amount = 1234,
                    currency = "cad",
                ),
            ),
        )

        loader.load(
            initializationMode = initializationMode,
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            initializedViaCompose = true,
        )

        verify(eventReporter).onLoadStarted(eq(true))
        verify(eventReporter).onLoadSucceeded(
            paymentSelection = null,
            linkMode = LinkMode.LinkPaymentMethod,
            googlePaySupported = true,
            currency = "usd",
            initializationMode = initializationMode,
            orderedLpms = listOf("card", "link"),
            requireCvcRecollection = false
        )
    }

    @Test
    fun `Emits correct events when loading fails for non-deferred intent`() = runTest {
        val error = PaymentSheetLoadingException.MissingAmountOrCurrency
        val loader = createPaymentElementLoader(error = error)

        loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            initializedViaCompose = false,
        )

        verify(eventReporter).onLoadStarted(eq(false))
        verify(eventReporter).onLoadFailed(error)
    }

    @Test
    fun `Emits correct events when loading fails for deferred intent`() = runTest {
        val error = PaymentIntentInTerminalState(Canceled)
        val loader = createPaymentElementLoader(error = error)

        loader.load(
            initializationMode = PaymentSheet.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1234,
                        currency = "cad",
                    ),
                ),
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            initializedViaCompose = false,
        )

        verify(eventReporter).onLoadStarted(eq(false))
        verify(eventReporter).onLoadFailed(error)
    }

    @Test
    fun `Emits correct events when loading fails with invalid confirmation method`() = runTest {
        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                confirmationMethod = Manual,
            )
        )

        loader.load(
            initializationMode = PaymentSheet.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1234,
                        currency = "cad",
                    ),
                ),
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            initializedViaCompose = false,
        )

        verify(eventReporter).onLoadStarted(eq(false))
        verify(eventReporter).onLoadFailed(PaymentSheetLoadingException.InvalidConfirmationMethod(Manual))
    }

    @Test
    fun `Emits correct events when we fallback to the core Stripe API`() = runTest {
        val intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val error = APIConnectionException()

        val loader = createPaymentElementLoader(fallbackError = error)

        loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = intent.clientSecret!!,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            initializedViaCompose = false,
        )

        verify(eventReporter).onLoadStarted(eq(false))
        verify(eventReporter).onElementsSessionLoadFailed(error)
    }

    @Test
    fun `Includes card brand choice state if feature is enabled`() = runTest {
        val loader = createPaymentElementLoader(
            cardBrandChoice = ElementsSession.CardBrandChoice(
                eligible = true,
                preferredNetworks = listOf("cartes_bancaires"),
            ),
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            initializedViaCompose = false,
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.cbcEligibility)
            .isEqualTo(CardBrandChoiceEligibility.Eligible(listOf()))
    }

    @Test
    fun `Returns correct Link signup mode if not saving for future use`() = runTest {
        val loader = createPaymentElementLoader(
            linkAccountState = AccountStatus.SignedOut,
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            initializedViaCompose = false,
        ).getOrThrow()

        assertThat(result.linkState?.signupMode).isEqualTo(InsteadOfSaveForFutureUse)
        assertThat(result.paymentMethodMetadata.linkInlineConfiguration?.signupMode)
            .isEqualTo(InsteadOfSaveForFutureUse)
    }

    @Test
    fun `Returns correct Link signup mode if saving for future use`() = runTest {
        val loader = createPaymentElementLoader(
            linkAccountState = AccountStatus.SignedOut,
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration(
                merchantDisplayName = "Some Name",
                customer = PaymentSheet.CustomerConfiguration(
                    id = "cus_123",
                    ephemeralKeySecret = "some_secret",
                ),
            ),
            initializedViaCompose = false,
        ).getOrThrow()

        assertThat(result.linkState?.signupMode).isEqualTo(AlongsideSaveForFutureUse)
        assertThat(result.paymentMethodMetadata.linkInlineConfiguration?.signupMode)
            .isEqualTo(AlongsideSaveForFutureUse)
    }

    @Test
    fun `Returns correct Link signup mode when payment sheet save is disabled`() = runTest {
        val loader = createPaymentElementLoader(
            linkAccountState = AccountStatus.SignedOut,
            customer = createElementsSessionCustomer(
                isPaymentMethodSaveEnabled = false,
            )
        )

        val result = loader.load(
            initializationMode = DEFAULT_INITIALIZATION_MODE,
            paymentSheetConfiguration = DEFAULT_PAYMENT_SHEET_CONFIG,
            initializedViaCompose = false,
        ).getOrThrow()

        assertThat(result.linkState?.signupMode).isEqualTo(InsteadOfSaveForFutureUse)
        assertThat(result.paymentMethodMetadata.linkInlineConfiguration?.signupMode)
            .isEqualTo(InsteadOfSaveForFutureUse)
    }

    @Test
    fun `Returns correct Link signup mode when payment sheet save is enabled`() = runTest {
        val loader = createPaymentElementLoader(
            linkAccountState = AccountStatus.SignedOut,
            customer = createElementsSessionCustomer(
                isPaymentMethodSaveEnabled = true,
            )
        )

        val result = loader.load(
            initializationMode = DEFAULT_INITIALIZATION_MODE,
            paymentSheetConfiguration = DEFAULT_PAYMENT_SHEET_CONFIG,
            initializedViaCompose = false,
        ).getOrThrow()

        assertThat(result.linkState?.signupMode).isEqualTo(AlongsideSaveForFutureUse)
        assertThat(result.paymentMethodMetadata.linkInlineConfiguration?.signupMode)
            .isEqualTo(AlongsideSaveForFutureUse)
    }

    @Test
    fun `Disables Link if BDCC collects anything`() = runTest {
        val loader = createPaymentElementLoader()

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration(
                merchantDisplayName = "Some Name",
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                ),
            ),
            initializedViaCompose = false,
        ).getOrThrow()

        assertThat(result.linkState).isNull()
        assertThat(result.paymentMethodMetadata.linkInlineConfiguration).isNull()
    }

    @Test
    fun `When EPMs are requested but not returned by elements session, no EPMs are used`() = runTest {
        testExternalPaymentMethods(
            requestedExternalPaymentMethods = listOf("external_paypal"),
            externalPaymentMethodData = null,
            expectedExternalPaymentMethods = emptyList(),
            expectedLogMessages = listOf(
                "Requested external payment method external_paypal is not supported. View all available external " +
                    "payment methods here: https://docs.stripe.com/payments/external-payment-methods?" +
                    "platform=android#available-external-payment-methods"
            ),
        )
    }

    @Test
    fun `When EPMs are requested and returned by elements session, EPMs are used`() = runTest {
        val requestedExternalPaymentMethods = listOf("external_venmo", "external_paypal")

        testExternalPaymentMethods(
            requestedExternalPaymentMethods,
            externalPaymentMethodData = PaymentSheetFixtures.PAYPAL_AND_VENMO_EXTERNAL_PAYMENT_METHOD_DATA,
            expectedExternalPaymentMethods = requestedExternalPaymentMethods,
            expectedLogMessages = emptyList(),
        )
    }

    @OptIn(ExperimentalCustomerSessionApi::class)
    @Test
    fun `When customer session configuration is provided, should pass it to 'ElementsSessionRepository'`() = runTest {
        val repository = FakeElementsSessionRepository(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            error = null,
            linkSettings = null,
        )

        val loader = createPaymentElementLoader(
            elementsSessionRepository = repository
        )

        loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = "client_secret"
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration(
                merchantDisplayName = "Merchant, Inc.",
                customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                    id = "cus_1",
                    clientSecret = "customer_client_secret",
                ),
            ),
            initializedViaCompose = false,
        )

        assertThat(repository.lastParams?.customer).isEqualTo(
            PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                id = "cus_1",
                clientSecret = "customer_client_secret",
            )
        )
    }

    @OptIn(ExperimentalCustomerSessionApi::class)
    @Test
    fun `When 'CustomerSession' config is provided, should use payment methods from elements_session and not fetch`() =
        runTest {
            var attemptedToRetrievePaymentMethods = false

            val repository = FakeCustomerRepository(
                onGetPaymentMethods = {
                    attemptedToRetrievePaymentMethods = true
                    Result.success(listOf())
                }
            )

            val cards = PaymentMethodFactory.cards(4)
            val loader = createPaymentElementLoader(
                customerRepo = repository,
                customer = ElementsSession.Customer(
                    paymentMethods = cards,
                    session = ElementsSession.Customer.Session(
                        id = "cuss_1",
                        customerId = "cus_1",
                        liveMode = false,
                        apiKey = "ek_123",
                        apiKeyExpiry = 555555555,
                        components = ElementsSession.Customer.Components(
                            mobilePaymentElement = ElementsSession.Customer.Components.MobilePaymentElement.Disabled,
                            customerSheet = ElementsSession.Customer.Components.CustomerSheet.Disabled,
                        ),
                    ),
                    defaultPaymentMethod = null,
                )
            )

            val state = loader.load(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                    clientSecret = "client_secret"
                ),
                paymentSheetConfiguration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                    customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = "cus_1",
                        clientSecret = "customer_client_secret",
                    ),
                ),
                initializedViaCompose = false,
            ).getOrThrow()

            assertThat(attemptedToRetrievePaymentMethods).isFalse()

            assertThat(state.customer).isEqualTo(
                CustomerState(
                    id = "cus_1",
                    ephemeralKeySecret = "ek_123",
                    paymentMethods = cards,
                    permissions = CustomerState.Permissions(
                        canRemovePaymentMethods = false,
                        canRemoveDuplicates = true,
                    ),
                )
            )
        }

    @OptIn(ExperimentalCustomerSessionApi::class)
    @Test
    fun `When 'elements_session' has remove permissions enabled, should enable remove permissions in customer state`() =
        runTest {
            val loader = createPaymentElementLoader(
                customer = ElementsSession.Customer(
                    paymentMethods = PaymentMethodFactory.cards(4),
                    session = createElementsSessionCustomerSession(
                        ElementsSession.Customer.Components.MobilePaymentElement.Enabled(
                            isPaymentMethodRemoveEnabled = true,
                            isPaymentMethodSaveEnabled = false,
                            allowRedisplayOverride = null,
                        )
                    ),
                    defaultPaymentMethod = null,
                )
            )

            val state = loader.load(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                    clientSecret = "client_secret"
                ),
                paymentSheetConfiguration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                    customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = "cus_1",
                        clientSecret = "customer_client_secret",
                    ),
                ),
                initializedViaCompose = false,
            ).getOrThrow()

            assertThat(state.customer?.permissions).isEqualTo(
                CustomerState.Permissions(
                    canRemovePaymentMethods = true,
                    canRemoveDuplicates = true,
                )
            )
        }

    @OptIn(ExperimentalCustomerSessionApi::class)
    @Test
    fun `When 'elements_session' has remove permissions disabled, should disable remove permissions in customer state`() =
        runTest {
            val loader = createPaymentElementLoader(
                customer = ElementsSession.Customer(
                    paymentMethods = PaymentMethodFactory.cards(4),
                    session = createElementsSessionCustomerSession(
                        ElementsSession.Customer.Components.MobilePaymentElement.Enabled(
                            isPaymentMethodRemoveEnabled = false,
                            isPaymentMethodSaveEnabled = false,
                            allowRedisplayOverride = null,
                        )
                    ),
                    defaultPaymentMethod = null,
                )
            )

            val state = loader.load(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                    clientSecret = "client_secret"
                ),
                paymentSheetConfiguration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                    customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = "cus_1",
                        clientSecret = "customer_client_secret",
                    ),
                ),
                initializedViaCompose = false,
            ).getOrThrow()

            assertThat(state.customer?.permissions).isEqualTo(
                CustomerState.Permissions(
                    canRemovePaymentMethods = false,
                    canRemoveDuplicates = true,
                )
            )
        }

    @OptIn(ExperimentalCustomerSessionApi::class)
    @Test
    fun `When 'elements_session' has Payment Sheet component disabled, should disable permissions in customer state`() =
        runTest {
            val loader = createPaymentElementLoader(
                customer = ElementsSession.Customer(
                    paymentMethods = PaymentMethodFactory.cards(4),
                    session = createElementsSessionCustomerSession(
                        ElementsSession.Customer.Components.MobilePaymentElement.Enabled(
                            isPaymentMethodRemoveEnabled = false,
                            isPaymentMethodSaveEnabled = false,
                            allowRedisplayOverride = null,
                        )
                    ),
                    defaultPaymentMethod = null,
                )
            )

            val state = loader.load(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                    clientSecret = "client_secret"
                ),
                paymentSheetConfiguration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                    customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = "cus_1",
                        clientSecret = "customer_client_secret",
                    ),
                ),
                initializedViaCompose = false,
            ).getOrThrow()

            assertThat(state.customer?.permissions).isEqualTo(
                CustomerState.Permissions(
                    canRemovePaymentMethods = false,
                    canRemoveDuplicates = true,
                )
            )
        }

    @Test
    fun `When 'LegacyEphemeralKey' config is provided, permissions should always be enabled and remove duplicates should be disabled`() =
        runTest {
            val loader = createPaymentElementLoader()

            val state = loader.load(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                    clientSecret = "client_secret"
                ),
                paymentSheetConfiguration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                    customer = PaymentSheet.CustomerConfiguration(
                        id = "cus_1",
                        ephemeralKeySecret = "ephemeral_key",
                    ),
                ),
                initializedViaCompose = false,
            ).getOrThrow()

            assertThat(state.customer?.permissions).isEqualTo(
                CustomerState.Permissions(
                    canRemovePaymentMethods = true,
                    canRemoveDuplicates = false,
                )
            )
        }

    @OptIn(ExperimentalCustomerSessionApi::class)
    @Test
    fun `When 'CustomerSession' config is provided but no customer object was returned in test mode, should report error and return error`() =
        runTest {
            val errorReporter = FakeErrorReporter()

            val loader = createPaymentElementLoader(
                errorReporter = errorReporter,
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    isLiveMode = false
                )
            )

            val exception = loader.load(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                    clientSecret = "client_secret"
                ),
                paymentSheetConfiguration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                    customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = "cus_1",
                        clientSecret = "customer_client_secret",
                    ),
                ),
                initializedViaCompose = false,
            ).exceptionOrNull()

            assertThat(exception).isInstanceOf<IllegalStateException>()

            assertThat(errorReporter.getLoggedErrors())
                .contains(
                    ErrorReporter
                        .UnexpectedErrorEvent
                        .PAYMENT_SHEET_LOADER_ELEMENTS_SESSION_CUSTOMER_NOT_FOUND
                        .eventName
                )
        }

    @OptIn(ExperimentalCustomerSessionApi::class)
    @Test
    fun `When 'CustomerSession' config is provided but no customer object was returned in live mode, should report error and continue with loading without customer`() =
        runTest {
            val errorReporter = FakeErrorReporter()

            val loader = createPaymentElementLoader(
                errorReporter = errorReporter,
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    isLiveMode = true
                )
            )

            val state = loader.load(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                    clientSecret = "client_secret"
                ),
                paymentSheetConfiguration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                    customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = "cus_1",
                        clientSecret = "customer_client_secret",
                    ),
                ),
                initializedViaCompose = false,
            ).getOrNull()

            assertThat(state).isNotNull()
            assertThat(state?.customer).isNull()

            assertThat(errorReporter.getLoggedErrors())
                .contains(
                    ErrorReporter
                        .UnexpectedErrorEvent
                        .PAYMENT_SHEET_LOADER_ELEMENTS_SESSION_CUSTOMER_NOT_FOUND
                        .eventName
                )
        }

    @Test
    fun `When 'LegacyEphemeralKey' is provided, should fetch and use payment methods from 'CustomerRepository'`() =
        runTest {
            var attemptedToRetrievePaymentMethods = false

            val cards = PaymentMethodFactory.cards(2)
            val repository = FakeCustomerRepository(
                onGetPaymentMethods = {
                    attemptedToRetrievePaymentMethods = true
                    Result.success(cards)
                }
            )

            val loader = createPaymentElementLoader(
                customerRepo = repository,
                customer = null
            )

            val state = loader.load(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                    clientSecret = "client_secret"
                ),
                paymentSheetConfiguration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                    customer = PaymentSheet.CustomerConfiguration(
                        id = "cus_1",
                        ephemeralKeySecret = "ephemeral_key_secret",
                    ),
                ),
                initializedViaCompose = false,
            ).getOrThrow()

            assertThat(attemptedToRetrievePaymentMethods).isTrue()

            assertThat(state.customer).isEqualTo(
                CustomerState(
                    id = "cus_1",
                    ephemeralKeySecret = "ephemeral_key_secret",
                    paymentMethods = cards,
                    permissions = CustomerState.Permissions(
                        canRemovePaymentMethods = true,
                        canRemoveDuplicates = false,
                    ),
                )
            )
        }

    @OptIn(ExperimentalCustomerSessionApi::class)
    @Test
    fun `When using 'CustomerSession', move last-used customer payment method to the front of the list`() = runTest {
        val paymentMethods = PaymentMethodFixtures.createCards(10)
        val lastUsed = paymentMethods[6]

        prefsRepository.savePaymentSelection(PaymentSelection.Saved(lastUsed))

        val loader = createPaymentElementLoader(
            customer = ElementsSession.Customer(
                paymentMethods = paymentMethods,
                session = ElementsSession.Customer.Session(
                    id = "cuss_1",
                    customerId = "cus_1",
                    liveMode = false,
                    apiKey = "ek_123",
                    apiKeyExpiry = 555555555,
                    components = ElementsSession.Customer.Components(
                        mobilePaymentElement = ElementsSession.Customer.Components.MobilePaymentElement.Disabled,
                        customerSheet = ElementsSession.Customer.Components.CustomerSheet.Disabled,
                    ),
                ),
                defaultPaymentMethod = null,
            )
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(
                customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                    id = "id",
                    clientSecret = "cuss_1",
                ),
            ),
            initializedViaCompose = false,
        ).getOrThrow()

        val observedElements = result.customer?.paymentMethods
        val expectedElements = listOf(lastUsed) + (paymentMethods - lastUsed)

        assertThat(observedElements).containsExactlyElementsIn(expectedElements).inOrder()
    }

    @OptIn(ExperimentalCustomerSessionApi::class)
    @Test
    fun `When using 'CustomerSession', payment methods should be filtered by supported saved payment methods`() =
        runTest {
            val paymentMethods = PaymentMethodFixtures.createCards(2) +
                listOf(
                    PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD,
                    PaymentMethodFixtures.LINK_PAYMENT_METHOD,
                    PaymentMethodFixtures.AU_BECS_DEBIT,
                )

            val loader = createPaymentElementLoader(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "link", "au_becs_debit", "sepa_debit")
                ),
                customer = createElementsSessionCustomer(
                    paymentMethods = paymentMethods,
                ),
            )

            val result = loader.load(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
                paymentSheetConfiguration = mockConfiguration(
                    customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = "id",
                        clientSecret = "cuss_1",
                    ),
                    allowsDelayedPaymentMethods = true,
                ),
                initializedViaCompose = false,
            ).getOrThrow()

            val expectedPaymentMethods = paymentMethods.filter { paymentMethod ->
                paymentMethod != PaymentMethodFixtures.LINK_PAYMENT_METHOD &&
                    paymentMethod != PaymentMethodFixtures.AU_BECS_DEBIT
            }

            assertThat(result.customer?.paymentMethods)
                .containsExactlyElementsIn(expectedPaymentMethods)
        }

    @OptIn(ExperimentalCustomerSessionApi::class)
    @Test
    fun `When using 'CustomerSession' & no default billing details, customer email for Link config is fetched using 'elements_session' ephemeral key`() =
        runTest {
            val customerRepository = spy(
                FakeCustomerRepository(
                    onRetrieveCustomer = {
                        mock {
                            on { email } doReturn "email@stripe.com"
                        }
                    }
                )
            )

            val loader = createPaymentElementLoader(
                customerRepo = customerRepository,
                customer = ElementsSession.Customer(
                    paymentMethods = PaymentMethodFactory.cards(1),
                    session = ElementsSession.Customer.Session(
                        id = "cuss_1",
                        customerId = "cus_1",
                        liveMode = false,
                        apiKey = "ek_123",
                        apiKeyExpiry = 555555555,
                        components = ElementsSession.Customer.Components(
                            mobilePaymentElement = ElementsSession.Customer.Components.MobilePaymentElement.Disabled,
                            customerSheet = ElementsSession.Customer.Components.CustomerSheet.Disabled,
                        ),
                    ),
                    defaultPaymentMethod = null,
                )
            )

            loader.load(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
                paymentSheetConfiguration = mockConfiguration(
                    customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = "id",
                        clientSecret = "cuss_1",
                    ),
                    defaultBillingDetails = null
                ),
                initializedViaCompose = false,
            )

            verify(customerRepository).retrieveCustomer(
                CustomerRepository.CustomerInfo(
                    id = "cus_1",
                    ephemeralKeySecret = "ek_123",
                )
            )
        }

    @OptIn(ExperimentalCustomerSessionApi::class)
    @Test
    fun `When using 'CustomerSession' & has a default saved Stripe payment method, should call 'ElementsSessionRepository' with default id`() =
        runTest {
            prefsRepository.savePaymentSelection(
                PaymentSelection.Saved(
                    paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_1234321"),
                )
            )

            val repository = FakeElementsSessionRepository(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                linkSettings = null,
                error = null,
            )

            val loader = createPaymentElementLoader(
                elementsSessionRepository = repository,
            )

            loader.load(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
                paymentSheetConfiguration = mockConfiguration(
                    customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = "id",
                        clientSecret = "cuss_1",
                    ),
                ),
                initializedViaCompose = false,
            )

            assertThat(repository.lastParams?.defaultPaymentMethodId)
                .isEqualTo("pm_1234321")
        }

    @OptIn(ExperimentalCustomerSessionApi::class)
    @Test
    fun `When using 'CustomerSession' & has a default Google Pay payment method, should not call 'ElementsSessionRepository' with default id`() =
        runTest {
            prefsRepository.savePaymentSelection(PaymentSelection.GooglePay)

            val repository = FakeElementsSessionRepository(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                linkSettings = null,
                error = null,
            )

            val loader = createPaymentElementLoader(
                elementsSessionRepository = repository,
            )

            loader.load(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
                paymentSheetConfiguration = mockConfiguration(
                    customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = "id",
                        clientSecret = "cuss_1",
                    ),
                ),
                initializedViaCompose = false,
            )

            assertThat(repository.lastParams?.defaultPaymentMethodId).isNull()
        }

    @Test
    fun `When using 'LegacyEphemeralKey' & has a default saved Stripe payment method, should not call 'ElementsSessionRepository' with default id`() =
        runTest {
            prefsRepository.savePaymentSelection(
                PaymentSelection.Saved(
                    paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_1234321"),
                )
            )

            val repository = FakeElementsSessionRepository(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                linkSettings = null,
                error = null,
            )

            val loader = createPaymentElementLoader(
                elementsSessionRepository = repository,
            )

            loader.load(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret"),
                paymentSheetConfiguration = mockConfiguration(
                    customer = PaymentSheet.CustomerConfiguration(
                        id = "id",
                        ephemeralKeySecret = "ek_123",
                    ),
                ),
                initializedViaCompose = false,
            )

            assertThat(repository.lastParams?.defaultPaymentMethodId).isNull()
        }

    @Test
    fun `Emits correct loaded event when Link is unavailable`() = runTest {
        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card"),
            )
        )

        loader.load(
            initializationMode = DEFAULT_INITIALIZATION_MODE,
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            initializedViaCompose = true,
        )

        verify(eventReporter).onLoadSucceeded(
            paymentSelection = null,
            linkMode = null,
            googlePaySupported = true,
            currency = "usd",
            initializationMode = DEFAULT_INITIALIZATION_MODE,
            orderedLpms = listOf("card"),
            requireCvcRecollection = false
        )
    }

    @Test
    fun `Emits correct event when Link is in payment method mode`() = runTest {
        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "link"),
            ),
            linkSettings = createLinkSettings(passthroughModeEnabled = false),
        )

        loader.load(
            initializationMode = DEFAULT_INITIALIZATION_MODE,
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            initializedViaCompose = true,
        )

        verify(eventReporter).onLoadSucceeded(
            paymentSelection = null,
            linkMode = LinkMode.LinkPaymentMethod,
            googlePaySupported = true,
            currency = "usd",
            initializationMode = DEFAULT_INITIALIZATION_MODE,
            orderedLpms = listOf("card", "link"),
            requireCvcRecollection = false
        )
    }

    @Test
    fun `Emits correct event when Link is in passthrough mode`() = runTest {
        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "link"),
            ),
            linkSettings = createLinkSettings(passthroughModeEnabled = true),
        )

        loader.load(
            initializationMode = DEFAULT_INITIALIZATION_MODE,
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            initializedViaCompose = true,
        )

        verify(eventReporter).onLoadSucceeded(
            paymentSelection = null,
            linkMode = LinkMode.Passthrough,
            googlePaySupported = true,
            currency = "usd",
            initializationMode = DEFAULT_INITIALIZATION_MODE,
            orderedLpms = listOf("card", "link"),
            requireCvcRecollection = false
        )
    }

    @Test
    fun `Emits correct event when CVC recollection is required`() = runTest {
        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION,
            linkSettings = createLinkSettings(passthroughModeEnabled = false),
        )

        loader.load(
            initializationMode = DEFAULT_INITIALIZATION_MODE,
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            initializedViaCompose = true
        )

        verify(eventReporter).onLoadSucceeded(
            paymentSelection = null,
            linkMode = LinkMode.LinkPaymentMethod,
            googlePaySupported = true,
            currency = "usd",
            initializationMode = DEFAULT_INITIALIZATION_MODE,
            orderedLpms = listOf("card", "link"),
            requireCvcRecollection = true
        )
    }

    @Test
    fun `Emits correct event when CVC recollection is required for deferred`() = runTest {
        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            linkSettings = createLinkSettings(passthroughModeEnabled = false),
        )

        val initializationMode = PaymentSheet.InitializationMode.DeferredIntent(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                    amount = 100L,
                    currency = "usd"
                ),
                requireCvcRecollection = true
            )
        )

        loader.load(
            initializationMode = initializationMode,
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            initializedViaCompose = true
        )

        verify(eventReporter).onLoadSucceeded(
            paymentSelection = null,
            linkMode = LinkMode.LinkPaymentMethod,
            googlePaySupported = true,
            currency = "usd",
            initializationMode = initializationMode,
            orderedLpms = listOf("card", "link"),
            requireCvcRecollection = true
        )
    }

    @Test
    fun `Emits correct event when CVC recollection is required on intent but not deferred config`() = runTest {
        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION,
            linkSettings = createLinkSettings(passthroughModeEnabled = false),
        )

        val initializationMode = PaymentSheet.InitializationMode.DeferredIntent(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                    amount = 100L,
                    currency = "usd"
                ),
                requireCvcRecollection = false
            )
        )

        loader.load(
            initializationMode = initializationMode,
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            initializedViaCompose = true
        )

        verify(eventReporter).onLoadSucceeded(
            paymentSelection = null,
            linkMode = LinkMode.LinkPaymentMethod,
            googlePaySupported = true,
            currency = "usd",
            initializationMode = initializationMode,
            orderedLpms = listOf("card", "link"),
            requireCvcRecollection = false
        )
    }

    @OptIn(ExperimentalCardBrandFilteringApi::class)
    @Test
    fun `Should filter out saved cards with disallowed brands`() = runTest {
        prefsRepository.savePaymentSelection(null)

        val paymentMethods = listOf(
            PaymentMethodFactory.card(id = "pm_12345").update(
                last4 = "1001",
                addCbcNetworks = false,
                brand = CardBrand.Visa,
            ),
            PaymentMethodFactory.card(id = "pm_123456").update(
                last4 = "1000",
                addCbcNetworks = false,
                brand = CardBrand.AmericanExpress,
            )
        )

        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
            isGooglePayReady = true,
            customerRepo = FakeCustomerRepository(paymentMethods = paymentMethods),
        )

        val config = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.copy(
            cardBrandAcceptance = PaymentSheet.CardBrandAcceptance.disallowed(
                listOf(PaymentSheet.CardBrandAcceptance.BrandCategory.Visa)
            )
        )

        val state = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = config,
            initializedViaCompose = false,
        ).getOrThrow()

        assertThat(state.customer?.paymentMethods?.count() ?: 0).isEqualTo(
            1
        )
        assertThat(state.customer?.paymentMethods?.first()?.card?.brand).isEqualTo(
            CardBrand.AmericanExpress
        )
    }

    private suspend fun testExternalPaymentMethods(
        requestedExternalPaymentMethods: List<String>,
        externalPaymentMethodData: String?,
        expectedExternalPaymentMethods: List<String>?,
        expectedLogMessages: List<String>,
    ) {
        val userFacingLogger = FakeUserFacingLogger()
        val loader = createPaymentElementLoader(
            externalPaymentMethodData = externalPaymentMethodData,
            userFacingLogger = userFacingLogger
        )

        val result = loader.load(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration.Builder(merchantDisplayName = "Example, Inc.")
                .externalPaymentMethods(requestedExternalPaymentMethods).build(),
            initializedViaCompose = false,
        ).getOrThrow()

        val actualExternalPaymentMethods = result.paymentMethodMetadata.externalPaymentMethodSpecs.map { it.type }
        assertThat(actualExternalPaymentMethods).isEqualTo(expectedExternalPaymentMethods)
        assertThat(userFacingLogger.getLoggedMessages()).containsExactlyElementsIn(expectedLogMessages)
    }

    private suspend fun testSuccessfulLoadSendsEventsCorrectly(paymentSelection: PaymentSelection?) {
        prefsRepository.savePaymentSelection(paymentSelection)

        val loader = createPaymentElementLoader(
            linkSettings = createLinkSettings(passthroughModeEnabled = false),
        )
        val initializationMode = PaymentSheet.InitializationMode.PaymentIntent("secret")

        loader.load(
            initializationMode = initializationMode,
            paymentSheetConfiguration = PaymentSheet.Configuration(
                merchantDisplayName = "Some Name",
                customer = PaymentSheet.CustomerConfiguration(
                    id = "cus_123",
                    ephemeralKeySecret = "some_secret",
                ),
            ),
            initializedViaCompose = false,
        )

        verify(eventReporter).onLoadStarted(eq(false))
        verify(eventReporter).onLoadSucceeded(
            paymentSelection = paymentSelection,
            linkMode = LinkMode.LinkPaymentMethod,
            googlePaySupported = true,
            currency = "usd",
            initializationMode = initializationMode,
            orderedLpms = listOf("card", "link"),
            requireCvcRecollection = false
        )
    }

    private fun createLinkSettings(passthroughModeEnabled: Boolean): ElementsSession.LinkSettings {
        return ElementsSession.LinkSettings(
            linkFundingSources = listOf("card", "bank"),
            linkPassthroughModeEnabled = passthroughModeEnabled,
            linkMode = if (passthroughModeEnabled) LinkMode.Passthrough else LinkMode.LinkPaymentMethod,
            linkFlags = mapOf(),
            disableLinkSignup = false,
        )
    }

    private fun createElementsSessionCustomerSession(
        mobilePaymentElementComponent: ElementsSession.Customer.Components.MobilePaymentElement,
    ): ElementsSession.Customer.Session {
        return ElementsSession.Customer.Session(
            id = "cuss_1",
            customerId = "cus_1",
            liveMode = false,
            apiKey = "ek_123",
            apiKeyExpiry = 555555555,
            components = ElementsSession.Customer.Components(
                mobilePaymentElement = mobilePaymentElementComponent,
                customerSheet = ElementsSession.Customer.Components.CustomerSheet.Disabled,
            ),
        )
    }

    private fun createElementsSessionCustomer(
        paymentMethods: List<PaymentMethod> = PaymentMethodFactory.cards(1),
        isPaymentMethodSaveEnabled: Boolean? = null,
        mobilePaymentElementComponent: ElementsSession.Customer.Components.MobilePaymentElement =
            isPaymentMethodSaveEnabled?.let {
                ElementsSession.Customer.Components.MobilePaymentElement.Enabled(
                    isPaymentMethodSaveEnabled = it,
                    isPaymentMethodRemoveEnabled = true,
                    allowRedisplayOverride = null,
                )
            } ?: ElementsSession.Customer.Components.MobilePaymentElement.Disabled
    ): ElementsSession.Customer {
        return ElementsSession.Customer(
            paymentMethods = paymentMethods,
            session = ElementsSession.Customer.Session(
                id = "cuss_1",
                customerId = "cus_1",
                liveMode = false,
                apiKey = "ek_123",
                apiKeyExpiry = 555555555,
                components = ElementsSession.Customer.Components(
                    mobilePaymentElement = mobilePaymentElementComponent,
                    customerSheet = ElementsSession.Customer.Components.CustomerSheet.Disabled,
                ),
            ),
            defaultPaymentMethod = null,
        )
    }

    private fun createPaymentElementLoader(
        isGooglePayReady: Boolean = true,
        stripeIntent: StripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        customerRepo: CustomerRepository = customerRepository,
        linkAccountState: AccountStatus = AccountStatus.Verified,
        error: Throwable? = null,
        linkSettings: ElementsSession.LinkSettings? = null,
        isGooglePayEnabledFromBackend: Boolean = true,
        fallbackError: Throwable? = null,
        cardBrandChoice: ElementsSession.CardBrandChoice? = null,
        linkStore: LinkStore = mock(),
        customer: ElementsSession.Customer? = null,
        externalPaymentMethodData: String? = null,
        errorReporter: ErrorReporter = FakeErrorReporter(),
        elementsSessionRepository: ElementsSessionRepository = FakeElementsSessionRepository(
            stripeIntent = stripeIntent,
            error = error,
            sessionsError = fallbackError,
            linkSettings = linkSettings,
            sessionsCustomer = customer,
            isGooglePayEnabled = isGooglePayEnabledFromBackend,
            cardBrandChoice = cardBrandChoice,
            externalPaymentMethodData = externalPaymentMethodData,
        ),
        userFacingLogger: FakeUserFacingLogger = FakeUserFacingLogger(),
    ): PaymentElementLoader {
        return DefaultPaymentElementLoader(
            prefsRepositoryFactory = { prefsRepository },
            googlePayRepositoryFactory = {
                if (isGooglePayReady) readyGooglePayRepository else unreadyGooglePayRepository
            },
            elementsSessionRepository = elementsSessionRepository,
            customerRepository = customerRepo,
            lpmRepository = lpmRepository,
            logger = Logger.noop(),
            eventReporter = eventReporter,
            errorReporter = errorReporter,
            workContext = testDispatcher,
            accountStatusProvider = { linkAccountState },
            linkStore = linkStore,
            externalPaymentMethodsRepository = ExternalPaymentMethodsRepository(errorReporter = FakeErrorReporter()),
            userFacingLogger = userFacingLogger,
            cvcRecollectionHandler = CvcRecollectionHandlerImpl()
        )
    }

    private fun mockConfiguration(
        customer: PaymentSheet.CustomerConfiguration? = null,
        isGooglePayEnabled: Boolean = true,
        allowsDelayedPaymentMethods: Boolean = false,
        shippingDetails: AddressDetails? = null,
        defaultBillingDetails: PaymentSheet.BillingDetails? = null,
    ): PaymentSheet.Configuration {
        return PaymentSheet.Configuration(
            merchantDisplayName = "Merchant",
            customer = customer,
            shippingDetails = shippingDetails,
            defaultBillingDetails = defaultBillingDetails,
            allowsDelayedPaymentMethods = allowsDelayedPaymentMethods,
            googlePay = PaymentSheet.GooglePayConfiguration(
                environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                countryCode = CountryCode.US.value
            ).takeIf { isGooglePayEnabled }
        )
    }

    private companion object {
        private val PAYMENT_METHODS =
            listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD) + PaymentMethodFixtures.createCards(5)
        private val DEFAULT_PAYMENT_SHEET_CONFIG = PaymentSheet.Configuration(
            merchantDisplayName = "Some Name",
            customer = PaymentSheet.CustomerConfiguration(
                id = "cus_123",
                ephemeralKeySecret = "some_secret",
            ),
        )
        private val DEFAULT_INITIALIZATION_MODE = PaymentSheet.InitializationMode.PaymentIntent(
            clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
        )
    }

    private suspend fun PaymentElementLoader.load(
        initializationMode: PaymentSheet.InitializationMode,
        paymentSheetConfiguration: PaymentSheet.Configuration,
        isReloadingAfterProcessDeath: Boolean = false,
        initializedViaCompose: Boolean,
    ): Result<PaymentSheetState.Full> = load(
        initializationMode = initializationMode,
        configuration = paymentSheetConfiguration.asCommonConfiguration(),
        isReloadingAfterProcessDeath = isReloadingAfterProcessDeath,
        initializedViaCompose = initializedViaCompose,
    )
}
