package com.stripe.android.paymentsheet.state

import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardBrandFilter
import com.stripe.android.CardFundingFilter
import com.stripe.android.LinkDisallowFundingSourceCreationPreview
import com.stripe.android.PaymentConfiguration
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.common.analytics.experiment.LogLinkHoldbackExperiment
import com.stripe.android.common.configuration.ConfigurationDefaults
import com.stripe.android.common.model.PaymentMethodRemovePermission
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.common.taptoadd.FakeTapToAddConnectionManager
import com.stripe.android.common.taptoadd.TapToAddConnectionManager
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.googlepaylauncher.injection.GooglePayRepositoryFactory
import com.stripe.android.isInstanceOf
import com.stripe.android.link.FakeIntegrityRequestManager
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.gate.FakeLinkGate
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.LinkSignupMode.AlongsideSaveForFutureUse
import com.stripe.android.link.ui.inline.LinkSignupMode.InsteadOfSaveForFutureUse
import com.stripe.android.lpmfoundations.luxe.LpmRepository
import com.stripe.android.lpmfoundations.paymentmethod.AnalyticsMetadata
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.DisplayableCustomPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardFundingFilter
import com.stripe.android.model.Address
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.LinkDisabledReason
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentIntent.ConfirmationMethod.Manual
import com.stripe.android.model.PaymentIntentCreationFlow
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodSelectionFlow
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeIntent.Status.Canceled
import com.stripe.android.model.StripeIntent.Status.Succeeded
import com.stripe.android.model.wallets.Wallet
import com.stripe.android.paymentelement.WalletButtonsPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.paymentsheet.CardFundingFilteringPrivatePreview
import com.stripe.android.paymentsheet.ConfigFixtures
import com.stripe.android.paymentsheet.FakePrefsRepository
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.PaymentSheetFixtures.MERCHANT_DISPLAY_NAME
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.analytics.FakeLoadingEventReporter
import com.stripe.android.paymentsheet.analytics.FakeLogLinkHoldbackExperiment
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.toSavedSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import com.stripe.android.paymentsheet.repositories.FakeCheckoutSessionRepository
import com.stripe.android.paymentsheet.state.PaymentSheetLoadingException.PaymentIntentInTerminalState
import com.stripe.android.paymentsheet.utils.FakeUserFacingLogger
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.ExternalPaymentMethodsRepository
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.FakeElementsSessionRepository
import com.stripe.android.utils.FakeElementsSessionRepository.Companion.DEFAULT_ELEMENTS_SESSION_CONFIG_ID
import com.stripe.android.utils.FakePaymentMethodFilter
import com.stripe.attestation.IntegrityRequestManager
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.capture
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

internal class DefaultPaymentElementLoaderTest {

    @AfterTest
    fun tearDown() {
        PaymentElementCallbackReferences.clear()
    }

    @Test
    @Suppress("LongMethod")
    fun `load with configuration should return expected result`() = runScenario {
        prefsRepository.setSavedSelection(
            SavedSelection.PaymentMethod(PaymentMethodFixtures.CARD_PAYMENT_METHOD.id)
        )

        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
        )

        val config = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY

        assertThat(
            loader.load(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                config,
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            ).getOrThrow()
        ).isEqualTo(
            PaymentElementLoader.State(
                config = config.asCommonConfiguration(),
                customer = CustomerState(
                    paymentMethods = PAYMENT_METHODS,
                    defaultPaymentMethodId = null,
                ),
                paymentSelection = PaymentSelection.Saved(
                    paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                ),
                validationError = null,
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                    stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
                    allowsDelayedPaymentMethods = false,
                    sharedDataSpecs = emptyList(),
                    isGooglePayReady = true,
                    linkMode = null,
                    linkState = LinkDisabledState(listOf(LinkDisabledReason.NotSupportedInElementsSession)),
                    availableWallets = emptyList(),
                    cardBrandFilter = PaymentSheetCardBrandFilter(PaymentSheet.CardBrandAcceptance.all()),
                    cardFundingFilter = PaymentSheetCardFundingFilter(ConfigurationDefaults.allowedCardFundingTypes),
                    hasCustomerConfiguration = true,
                    financialConnectionsAvailability = FinancialConnectionsAvailability.Full,
                    customerMetadataPermissions = CustomerMetadata.Permissions(
                        canRemoveDuplicates = false,
                        removePaymentMethod = PaymentMethodRemovePermission.Full,
                        saveConsent = PaymentMethodSaveConsentBehavior.Legacy,
                        canRemoveLastPaymentMethod = true,
                        canUpdateFullPaymentMethodDetails = false,
                    ),
                    clientAttributionMetadata = ClientAttributionMetadata(
                        elementsSessionConfigId = DEFAULT_ELEMENTS_SESSION_CONFIG_ID,
                        paymentIntentCreationFlow = PaymentIntentCreationFlow.Standard,
                        paymentMethodSelectionFlow = PaymentMethodSelectionFlow.MerchantSpecified,
                    ),
                    integrationMetadata = IntegrationMetadata.IntentFirst("pi_1234_secret_1234"),
                ),
            )
        )

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `load with empty merchantDisplayName returns failure`() = runScenario {
        val result = createPaymentElementLoader().load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration(merchantDisplayName = ""),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo(
            "When a Configuration is passed to PaymentSheet, the Merchant display name cannot be an empty string."
        )

        assertThat(eventReporter.loadFailedTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `load with empty client secret returns failure`() = runScenario {
        val result = createPaymentElementLoader().load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = " ",
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some name"),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo(
            "The PaymentIntent client_secret cannot be an empty string."
        )

        assertThat(eventReporter.loadFailedTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `load with empty customer id returns failure`() = runScenario {
        val result = createPaymentElementLoader().load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration(
                merchantDisplayName = "Some name",
                customer = PaymentSheet.CustomerConfiguration(
                    id = " ",
                    ephemeralKeySecret = "ek_123",
                ),
            ),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo(
            "When a CustomerConfiguration is passed to PaymentSheet, the Customer ID cannot be an empty string."
        )

        assertThat(eventReporter.loadFailedTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `load with conflicting ephemeral key returns failure`() = runScenario {
        val result = createPaymentElementLoader().load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration(
                merchantDisplayName = "Some name",
                customer = PaymentSheet.CustomerConfiguration(
                    id = "customer_id",
                    ephemeralKeySecret = " ",
                    accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_different"),
                ),
            ),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo(
            "Conflicting ephemeralKeySecrets between CustomerConfiguration and CustomerConfiguration.customerAccessType"
        )

        assertThat(eventReporter.loadFailedTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `load without customer should return expected result`() = runScenario {
        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
        )

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            PaymentSheetFixtures.CONFIG_MINIMUM,
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()
        assertThat(result.paymentMethodMetadata.customerMetadata).isNull()

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `load with google pay kill switch enabled should return expected result`() = runScenario {
        prefsRepository.setSavedSelection(
            SavedSelection.PaymentMethod(PaymentMethodFixtures.CARD_PAYMENT_METHOD.id)
        )

        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
            isGooglePayReady = true,
            isGooglePayEnabledFromBackend = false,
        )

        assertThat(
            loader.load(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            ).getOrThrow().paymentMethodMetadata.isGooglePayReady
        ).isFalse()

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Should default to first payment method if customer has payment methods`() = runScenario {
        prefsRepository.setSavedSelection(null)

        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
            isGooglePayReady = true,
        )

        val state = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(state.paymentSelection).isEqualTo(
            PaymentSelection.Saved(paymentMethod = PAYMENT_METHODS.first())
        )

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Should default to last used payment method if available even if customer has payment methods`() = runScenario {
        prefsRepository.setSavedSelection(SavedSelection.GooglePay)

        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
            isGooglePayReady = true,
        )

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentSelection).isEqualTo(PaymentSelection.GooglePay)

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Should default to google if customer has no payment methods and no last used payment method`() =
        runScenario {
            prefsRepository.setSavedSelection(null)

            val loader = createPaymentElementLoader(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                isGooglePayReady = true,
                customerRepo = FakeCustomerRepository(paymentMethods = emptyList()),
            )

            val result = loader.load(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                paymentSheetConfiguration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            ).getOrThrow()

            assertThat(result.paymentSelection).isEqualTo(PaymentSelection.GooglePay)

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
        }

    @Test
    fun `Should default to no payment method when google pay is not ready`() =
        runScenario {
            prefsRepository.setSavedSelection(null)

            val loader = createPaymentElementLoader(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                isGooglePayReady = false,
                customerRepo = FakeCustomerRepository(paymentMethods = emptyList()),
            )

            val result = loader.load(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                paymentSheetConfiguration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            ).getOrThrow()

            assertThat(result.paymentSelection).isNull()

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
        }

    @Test
    fun `Should default to no payment method when saved selection is Google Pay & its not ready`() =
        runScenario {
            prefsRepository.setSavedSelection(SavedSelection.GooglePay)

            val loader = createPaymentElementLoader(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                isGooglePayReady = false,
                customerRepo = FakeCustomerRepository(paymentMethods = emptyList()),
            )

            val result = loader.load(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                paymentSheetConfiguration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            ).getOrThrow()

            assertThat(result.paymentSelection).isNull()

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
        }

    @OptIn(WalletButtonsPreview::class)
    @Test
    fun `Should default to no payment method when using wallet buttons`() =
        runScenario {
            prefsRepository.setSavedSelection(null)

            val loader = createPaymentElementLoader(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                isGooglePayReady = true,
                customerRepo = FakeCustomerRepository(paymentMethods = emptyList()),
            )

            val result = loader.load(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                paymentSheetConfiguration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.newBuilder()
                    .walletButtons(
                        PaymentSheet.WalletButtonsConfiguration(
                            willDisplayExternally = true,
                        )
                    )
                    .build(),
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            ).getOrThrow()

            assertThat(result.paymentSelection).isNull()

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
        }

    @Test
    fun `isTapToAddSupported should be false when tap to add is not supported`() =
        runScenario {
            FakeTapToAddConnectionManager.test(
                isSupported = false,
                isConnected = false,
            ) {
                val loader = createPaymentElementLoader(
                    stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                    isGooglePayReady = true,
                    customerRepo = FakeCustomerRepository(paymentMethods = emptyList()),
                    tapToAddConnectionManager = tapToAddConnectionManager,
                )

                val result = loader.load(
                    initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                        clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                    ),
                    paymentSheetConfiguration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                    metadata = PaymentElementLoader.Metadata(
                        initializedViaCompose = false,
                    ),
                ).getOrThrow()

                assertThat(connectCalls.awaitItem()).isNotNull()

                assertThat(result.paymentMethodMetadata.isTapToAddSupported).isFalse()

                assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
                assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
            }
        }

    @Test
    fun `isTapToAddSupported should be true when tap to add is supported`() =
        runScenario {
            FakeTapToAddConnectionManager.test(
                isSupported = true,
                isConnected = true,
            ) {
                val loader = createPaymentElementLoader(
                    stripeIntent = PaymentIntentFactory.create(),
                    isGooglePayReady = true,
                    customerRepo = FakeCustomerRepository(paymentMethods = emptyList()),
                    tapToAddConnectionManager = tapToAddConnectionManager,
                )

                val result = loader.load(
                    initializationMode = PaymentElementLoader.InitializationMode.SetupIntent(
                        clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value
                    ),
                    paymentSheetConfiguration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                    metadata = PaymentElementLoader.Metadata(
                        initializedViaCompose = false,
                    ),
                ).getOrThrow()

                assertThat(connectCalls.awaitItem()).isNotNull()

                assertThat(result.paymentMethodMetadata.isTapToAddSupported).isTrue()

                assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
                assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
            }
        }

    @OptIn(WalletButtonsPreview::class)
    @Test
    fun `Should default to no payment method when using wallet buttons & google is saved selection`() =
        runScenario {
            prefsRepository.setSavedSelection(SavedSelection.GooglePay)

            val loader = createPaymentElementLoader(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                isGooglePayReady = true,
                customerRepo = FakeCustomerRepository(paymentMethods = emptyList()),
            )

            val result = loader.load(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                paymentSheetConfiguration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.newBuilder()
                    .walletButtons(
                        PaymentSheet.WalletButtonsConfiguration(
                            willDisplayExternally = true,
                        )
                    )
                    .build(),
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            ).getOrThrow()

            assertThat(result.paymentSelection).isNull()

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
        }

    @OptIn(WalletButtonsPreview::class)
    @Test
    fun `Should default to no payment method when using wallet buttons & link is saved selection`() =
        runScenario {
            prefsRepository.setSavedSelection(SavedSelection.Link)

            val loader = createPaymentElementLoader(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                isGooglePayReady = true,
                customerRepo = FakeCustomerRepository(paymentMethods = emptyList()),
            )

            val result = loader.load(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                paymentSheetConfiguration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.newBuilder()
                    .walletButtons(
                        PaymentSheet.WalletButtonsConfiguration(
                            willDisplayExternally = true,
                        )
                    )
                    .build(),
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            ).getOrThrow()

            assertThat(result.paymentSelection).isNull()

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
        }

    @Test
    fun `Should default to no payment method when google pay is not configured`() =
        runScenario {
            prefsRepository.setSavedSelection(null)

            val userFacingLogger = FakeUserFacingLogger()
            val loader = createPaymentElementLoader(
                userFacingLogger = userFacingLogger,
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                isGooglePayReady = true,
                customerRepo = FakeCustomerRepository(paymentMethods = emptyList()),
            )

            val result = loader.load(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                paymentSheetConfiguration = PaymentSheetFixtures.CONFIG_CUSTOMER,
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            ).getOrThrow()

            assertThat(result.paymentSelection).isNull()
            assertThat(userFacingLogger.getLoggedMessages())
                .containsExactlyElementsIn(listOf("GooglePayConfiguration is not set."))

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
        }

    @Test
    fun `Should default to no payment method if customer has no payment methods and no last used payment method`() =
        runScenario {
            prefsRepository.setSavedSelection(null)

            val loader = createPaymentElementLoader(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
                isGooglePayReady = true,
                customerRepo = FakeCustomerRepository(paymentMethods = emptyList()),
                isGooglePayEnabledFromBackend = false,
            )

            val result = loader.load(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                paymentSheetConfiguration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            ).getOrThrow()

            assertThat(result.paymentSelection).isNull()

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
        }

    @Test
    fun `load() with customer should fetch only supported payment method types`() =
        runScenario {
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
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            )

            verify(customerRepository).getPaymentMethods(
                any(),
                capture(paymentMethodTypeCaptor),
                any(),
            )
            assertThat(paymentMethodTypeCaptor.allValues.flatten())
                .containsExactly(PaymentMethod.Type.Card)

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
        }

    @Test
    fun `when allowsDelayedPaymentMethods is false then delayed payment methods are filtered out`() =
        runScenario {
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
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            )

            verify(customerRepository).getPaymentMethods(
                any(),
                capture(paymentMethodTypeCaptor),
                any(),
            )
            assertThat(paymentMethodTypeCaptor.value)
                .containsExactly(PaymentMethod.Type.Card)

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
        }

    @Test
    fun `when getPaymentMethods fails in test mode, load() fails`() =
        runScenario {
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
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            )

            val actualException = loadResult.exceptionOrNull()
            assertThat(actualException?.cause).isEqualTo(expectedException)

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadFailedTurbine.awaitItem()).isNotNull()
        }

    @Test
    fun `load() with customer should filter out invalid payment method types`() =
        runScenario {
            val result = createPaymentElementLoader(
                customerRepo = FakeCustomerRepository(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(card = null), // invalid
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                )
            ).load(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            ).getOrThrow()

            assertThat(result.customer?.paymentMethods)
                .containsExactly(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
        }

    @Test
    fun `load() with customer should not filter out cards attached to a wallet`() =
        runScenario {
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
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            ).getOrThrow()

            assertThat(result.customer?.paymentMethods)
                .containsExactly(PaymentMethodFixtures.CARD_PAYMENT_METHOD, cardWithAmexWallet)

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
        }

    @Test
    fun `load() with customer should allow sepa`() = runScenario {
        var requestPaymentMethodTypes: List<PaymentMethod.Type>? = null
        val result = createPaymentElementLoader(
            customerRepo = object : FakeCustomerRepository() {
                override suspend fun getPaymentMethods(
                    accessInfo: CustomerMetadata.AccessInfo,
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
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.CLIENT_SECRET,
            ),
            paymentSheetConfiguration = PaymentSheetFixtures.CONFIG_CUSTOMER.newBuilder()
                .allowsDelayedPaymentMethods(true)
                .build(),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.customer?.paymentMethods)
            .containsExactly(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD,
            )
        assertThat(requestPaymentMethodTypes)
            .containsExactly(PaymentMethod.Type.Card, PaymentMethod.Type.SepaDebit)

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `load() when PaymentIntent has invalid status should return null`() = runScenario {
        val paymentIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
            paymentMethod = PaymentMethodFactory.card(),
        )

        val result = createPaymentElementLoader(
            stripeIntent = paymentIntent,
        ).load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.validationError).isEqualTo(PaymentIntentInTerminalState(Succeeded))

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `load() when PaymentIntent has invalid confirmationMethod should return null`() = runScenario {
        val result = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                confirmationMethod = Manual,
            ),
        ).load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.validationError).isEqualTo(PaymentSheetLoadingException.InvalidConfirmationMethod(Manual))

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadFailedTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Defaults to first existing payment method for known customer`() = runScenario {
        val result = createPaymentElementLoader(
            customerRepo = FakeCustomerRepository(paymentMethods = PAYMENT_METHODS)
        ).load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = mockConfiguration(
                customer = PaymentSheet.CustomerConfiguration(
                    id = "some_id",
                    ephemeralKeySecret = "ek_123",
                )
            ),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        val expectedPaymentMethod = requireNotNull(PAYMENT_METHODS.first())
        assertThat(result.paymentSelection).isEqualTo(PaymentSelection.Saved(expectedPaymentMethod))

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Considers Link logged in if the account is verified`() = runScenario {
        val loader = createPaymentElementLoader(linkAccountState = AccountStatus.Verified(consentPresentation = null))

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.linkState?.loginState).isEqualTo(LinkState.LoginState.LoggedIn)

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Considers Link as needing verification if the account needs verification`() = runScenario {
        val loader = createPaymentElementLoader(linkAccountState = AccountStatus.NeedsVerification())

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.linkState?.loginState).isEqualTo(LinkState.LoginState.NeedsVerification)

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Considers Link as needing verification if the account is being verified`() = runScenario {
        val loader = createPaymentElementLoader(linkAccountState = AccountStatus.VerificationStarted)

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.linkState?.loginState).isEqualTo(LinkState.LoginState.NeedsVerification)

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Considers Link as logged out correctly`() = runScenario {
        val loader = createPaymentElementLoader(linkAccountState = AccountStatus.SignedOut)

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.linkState?.loginState).isEqualTo(LinkState.LoginState.LoggedOut)

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Populates Link configuration correctly from billing details`() = runScenario {
        val billingDetails = PaymentSheet.BillingDetails(
            address = PaymentSheet.Address(country = "CA"),
            name = "Till",
        )

        val initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret")
        val result = createPaymentElementLoader().load(
            initializationMode = initializationMode,
            paymentSheetConfiguration = mockConfiguration(
                defaultBillingDetails = billingDetails,
            ),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        val configuration = result.paymentMethodMetadata.linkState?.configuration
        assertThat(configuration?.customerInfo).isEqualTo(
            LinkConfiguration.CustomerInfo(
                name = "Till",
                email = null,
                phone = null,
                billingCountryCode = "CA",
            )
        )

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Populates Link configuration with shipping details if checkbox is selected`() = runScenario {
        val shippingDetails = AddressDetails(
            name = "Not Till",
            address = PaymentSheet.Address(country = "US"),
            isCheckboxSelected = true,
        )

        val result = createPaymentElementLoader().load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(
                shippingDetails = shippingDetails,
            ),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.linkState?.configuration?.shippingDetails).isNotNull()

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Populates Link configuration with passthrough mode`() = runScenario {
        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
            linkSettings = ElementsSession.LinkSettings(
                linkFundingSources = emptyList(),
                linkPassthroughModeEnabled = true,
                linkMode = LinkMode.Passthrough,
                linkFlags = emptyMap(),
                disableLinkSignup = false,
                linkConsumerIncentive = null,
                useAttestationEndpoints = false,
                suppress2faModal = false,
                disableLinkRuxInFlowController = false,
                linkEnableDisplayableDefaultValuesInEce = false,
                linkMobileSkipWalletInFlowController = false,
                linkSignUpOptInFeatureEnabled = false,
                linkSignUpOptInInitialValue = false,
                linkSupportedPaymentMethodsOnboardingEnabled = listOf("CARD"),
            )
        )

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.linkState?.configuration?.passthroughModeEnabled).isTrue()

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Populates Link configuration with link flags`() = runScenario {
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
                linkConsumerIncentive = null,
                useAttestationEndpoints = false,
                suppress2faModal = false,
                disableLinkRuxInFlowController = false,
                linkEnableDisplayableDefaultValuesInEce = false,
                linkMobileSkipWalletInFlowController = false,
                linkSignUpOptInFeatureEnabled = false,
                linkSignUpOptInInitialValue = false,
                linkSupportedPaymentMethodsOnboardingEnabled = listOf("CARD"),
            )
        )

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
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

        assertThat(result.paymentMethodMetadata.linkState?.configuration?.flags).containsExactlyEntriesIn(expectedFlags)

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Populates Link configuration correctly with eligible card brand choice information`() = runScenario {
        val loader = createPaymentElementLoader(
            cardBrandChoice = ElementsSession.CardBrandChoice(
                eligible = true,
                preferredNetworks = listOf("cartes_bancaires"),
            ),
        )

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        val cardBrandChoice = result.paymentMethodMetadata.linkState?.configuration?.cardBrandChoice

        assertThat(cardBrandChoice?.eligible).isTrue()
        assertThat(cardBrandChoice?.preferredNetworks).isEqualTo(listOf("cartes_bancaires"))

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Populates Link configuration correctly with ineligible card brand choice information`() = runScenario {
        val loader = createPaymentElementLoader(
            cardBrandChoice = ElementsSession.CardBrandChoice(
                eligible = false,
                preferredNetworks = listOf("cartes_bancaires"),
            ),
        )

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        val cardBrandChoice = result.paymentMethodMetadata.linkState?.configuration?.cardBrandChoice

        assertThat(cardBrandChoice?.eligible).isFalse()
        assertThat(cardBrandChoice?.preferredNetworks).isEqualTo(listOf("cartes_bancaires"))

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Disables link sign up if used before`() = runScenario {
        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            linkSettings = ElementsSession.LinkSettings(
                linkFundingSources = emptyList(),
                linkPassthroughModeEnabled = false,
                linkMode = LinkMode.LinkPaymentMethod,
                linkFlags = mapOf(),
                disableLinkSignup = false,
                linkConsumerIncentive = null,
                useAttestationEndpoints = false,
                suppress2faModal = false,
                disableLinkRuxInFlowController = false,
                linkEnableDisplayableDefaultValuesInEce = false,
                linkMobileSkipWalletInFlowController = false,
                linkSignUpOptInFeatureEnabled = false,
                linkSignUpOptInInitialValue = false,
                linkSupportedPaymentMethodsOnboardingEnabled = listOf("CARD"),
            ),
            linkStore = mock {
                on { hasUsedLink() } doReturn true
            }
        )

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.linkState?.signupMode).isNull()

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Disables link sign up when settings have it disabled`() = runScenario {
        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            linkSettings = ElementsSession.LinkSettings(
                linkFundingSources = emptyList(),
                linkPassthroughModeEnabled = false,
                linkMode = LinkMode.LinkPaymentMethod,
                linkFlags = mapOf(),
                disableLinkSignup = true,
                linkConsumerIncentive = null,
                useAttestationEndpoints = false,
                suppress2faModal = false,
                disableLinkRuxInFlowController = false,
                linkEnableDisplayableDefaultValuesInEce = false,
                linkMobileSkipWalletInFlowController = false,
                linkSignUpOptInFeatureEnabled = false,
                linkSignUpOptInInitialValue = false,
                linkSupportedPaymentMethodsOnboardingEnabled = listOf("CARD"),
            )
        )

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.linkState?.signupMode).isNull()

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Disables Link inline signup if no valid funding source`() = runScenario {
        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                linkFundingSources = listOf("us_bank_account")
            ),
            linkAccountState = AccountStatus.SignedOut,
        )

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.linkState?.signupMode).isNull()

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Disables Link inline signup if user already has an unverified account`() = runScenario {
        val loader = createPaymentElementLoader(
            linkAccountState = AccountStatus.NeedsVerification(),
        )

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.linkState?.signupMode).isNull()

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Disables Link inline signup if user already has an verified account`() = runScenario {
        val loader = createPaymentElementLoader(
            linkAccountState = AccountStatus.Verified(consentPresentation = null),
        )

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.linkState?.signupMode).isNull()

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Disables Link inline signup if there is an account error`() = runScenario {
        val loader = createPaymentElementLoader(
            linkAccountState = AccountStatus.Error(Exception()),
        )

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.linkState?.signupMode).isNull()

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Enables Link inline signup if valid card funding source`() = runScenario {
        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                linkFundingSources = listOf("card"),
            ),
            linkAccountState = AccountStatus.SignedOut,
        )

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.linkState?.signupMode).isEqualTo(InsteadOfSaveForFutureUse)

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Enables Link inline signup if user has no account`() = runScenario {
        val loader = createPaymentElementLoader(
            linkAccountState = AccountStatus.SignedOut,
        )

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.linkState?.signupMode).isEqualTo(InsteadOfSaveForFutureUse)

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Uses shipping address phone number if checkbox is selected`() = runScenario {
        val billingDetails = PaymentSheet.BillingDetails(phone = "123-456-7890")

        val shippingDetails = AddressDetails(
            address = PaymentSheet.Address(country = "US"),
            phoneNumber = "098-765-4321",
            isCheckboxSelected = true,
        )

        val result = createPaymentElementLoader().load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(
                shippingDetails = shippingDetails,
                defaultBillingDetails = billingDetails,
            ),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.linkState?.configuration?.customerInfo?.phone)
            .isEqualTo(shippingDetails.phoneNumber)

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Falls back to customer email if billing address email not provided`() = runScenario {
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
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(
                customer = PaymentSheet.CustomerConfiguration(
                    id = "id",
                    ephemeralKeySecret = "ek_123",
                ),
            ),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.linkState?.configuration?.customerInfo?.email)
            .isEqualTo("email@stripe.com")

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Should use filtered payment methods in loaded state`() {
        val unfilteredPaymentMethods = PaymentMethodFixtures.createCards(10)
        val filteredPaymentMethods = unfilteredPaymentMethods.takeLast(5)

        runFilterScenario(
            filteredPaymentMethods = filteredPaymentMethods,
        ) {
            val lastUsed = unfilteredPaymentMethods[6]
            loaderScenario.prefsRepository.setSavedSelection(SavedSelection.PaymentMethod(lastUsed.id))

            val loader = loaderScenario.createPaymentElementLoader(
                customerRepo = FakeCustomerRepository(paymentMethods = unfilteredPaymentMethods),
                paymentMethodFilter = paymentMethodFilterScenario.paymentMethodFilter,
            )

            val result = loader.load(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
                paymentSheetConfiguration = mockConfiguration(
                    customer = PaymentSheet.CustomerConfiguration(
                        id = "id",
                        ephemeralKeySecret = "ek_123",
                    ),
                ),
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            ).getOrThrow()

            val filterCall = paymentMethodFilterScenario.filterCalls.awaitItem()

            assertThat(filterCall.paymentMethods).isEqualTo(unfilteredPaymentMethods)
            assertThat(filterCall.params.billingDetailsCollectionConfiguration)
                .isEqualTo(PaymentSheet.BillingDetailsCollectionConfiguration())
            assertThat(filterCall.params.localSavedSelection.await())
                .isEqualTo(SavedSelection.PaymentMethod(lastUsed.id))
            assertThat(filterCall.params.remoteDefaultPaymentMethodId).isNull()
            assertThat(filterCall.params.cardBrandFilter).isEqualTo(
                PaymentSheetCardBrandFilter(PaymentSheet.CardBrandAcceptance.all())
            )
            assertThat(filterCall.params.cardFundingFilter).isEqualTo(
                PaymentSheetCardFundingFilter(PaymentSheet.CardFundingType.entries)
            )

            assertThat(result.customer?.paymentMethods).isEqualTo(filteredPaymentMethods)
        }
    }

    @Test
    fun `Pass last-used customer payment method to payment method filter`() = runFilterScenario {
        val paymentMethods = PaymentMethodFixtures.createCards(10)
        val lastUsed = paymentMethods[6]
        loaderScenario.prefsRepository.setSavedSelection(SavedSelection.PaymentMethod(lastUsed.id))

        val loader = loaderScenario.createPaymentElementLoader(
            customerRepo = FakeCustomerRepository(paymentMethods = paymentMethods),
            paymentMethodFilter = paymentMethodFilterScenario.paymentMethodFilter,
        )

        loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(
                customer = PaymentSheet.CustomerConfiguration(
                    id = "id",
                    ephemeralKeySecret = "ek_123",
                ),
            ),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        )

        val filterCall = paymentMethodFilterScenario.filterCalls.awaitItem()

        assertThat(filterCall.params.localSavedSelection.await())
            .isEqualTo(SavedSelection.PaymentMethod(lastUsed.id))
    }

    @Test
    fun `Returns failure if StripeIntent does not contain any supported payment method type`() = runScenario {
        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("gold", "silver", "bronze"),
            ),
        )

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).exceptionOrNull()

        assertThat(result).isEqualTo(
            PaymentSheetLoadingException.NoPaymentMethodTypesAvailable(
                requested = "gold, silver, bronze",
            )
        )

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadFailedTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Returns failure if configuring deferred intent with negative amounts`() = runScenario {
        assertFailsWith<IllegalArgumentException>("Payment IntentConfiguration requires a positive amount.") {
            PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = -1099,
                        currency = "USD"
                    ),
                ),
            ).validate()
        }
    }

    @Test
    fun `Returns failure if configuring deferred intent with zero amounts`() = runScenario {
        assertFailsWith<IllegalArgumentException>("Payment IntentConfiguration requires a positive amount.") {
            PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 0,
                        currency = "USD"
                    ),
                ),
            ).validate()
        }
    }

    @Test
    fun `Returns failure if configuring checkout session with invalid prefix`() = runScenario {
        assertFailsWith<IllegalArgumentException>(
            "Must use a checkout session client secret (format: cs_*_secret_*)."
        ) {
            PaymentElementLoader.InitializationMode.CheckoutSession(
                clientSecret = "pi_test_123_secret_abc",
            ).validate()
        }
    }

    @Test
    fun `Returns failure if configuring checkout session without secret part`() = runScenario {
        assertFailsWith<IllegalArgumentException>(
            "Must use a checkout session client secret (format: cs_*_secret_*)."
        ) {
            PaymentElementLoader.InitializationMode.CheckoutSession(
                clientSecret = "cs_test_123",
            ).validate()
        }
    }

    @Test
    fun `CheckoutSession validate succeeds with valid client secret`() = runScenario {
        PaymentElementLoader.InitializationMode.CheckoutSession(
            clientSecret = "cs_test_123_secret_abc",
        ).validate()
    }

    @Test
    fun `CheckoutSession id property extracts id from client secret`() = runScenario {
        val checkoutSession = PaymentElementLoader.InitializationMode.CheckoutSession(
            clientSecret = "cs_test_123_secret_abc",
        )
        assertThat(checkoutSession.id).isEqualTo("cs_test_123")
    }

    @Test
    fun `integrationMetadata returns checkout session with extracted id`() = runScenario {
        val checkoutSession = PaymentElementLoader.InitializationMode.CheckoutSession(
            clientSecret = "cs_test_123_secret_abc"
        )
        assertThat(checkoutSession.integrationMetadata(null))
            .isEqualTo(IntegrationMetadata.CheckoutSession("cs_test_123"))
    }

    @Test
    fun `integrationMetadata returns intent first for payment intent`() = runScenario {
        val paymentIntent = PaymentElementLoader.InitializationMode.PaymentIntent("secret")
        assertThat(paymentIntent.integrationMetadata(null))
            .isEqualTo(IntegrationMetadata.IntentFirst("secret"))
        assertThat(paymentIntent.integrationMetadata(PaymentElementCallbacks.Builder().build()))
            .isEqualTo(IntegrationMetadata.IntentFirst("secret"))
    }

    @Test
    fun `integrationMetadata returns intent first for setup intent`() = runScenario {
        val setupIntent = PaymentElementLoader.InitializationMode.SetupIntent("secret")
        assertThat(setupIntent.integrationMetadata(null))
            .isEqualTo(IntegrationMetadata.IntentFirst("secret"))
        assertThat(setupIntent.integrationMetadata(PaymentElementCallbacks.Builder().build()))
            .isEqualTo(IntegrationMetadata.IntentFirst("secret"))
    }

    @Test
    @OptIn(SharedPaymentTokenSessionPreview::class)
    fun `integrationMetadata returns spt`() = runScenario {
        val intentConfiguration = PaymentSheet.IntentConfiguration(
            mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                amount = 1234,
                currency = "cad",
            ),
        )
        val initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
            intentConfiguration = intentConfiguration
        )
        assertThat(
            initializationMode.integrationMetadata(
                PaymentElementCallbacks.Builder()
                    .preparePaymentMethodHandler { _, _ ->
                        error("Should not be called.")
                    }.build()
            )
        ).isEqualTo(IntegrationMetadata.DeferredIntent.WithSharedPaymentToken(intentConfiguration))
    }

    @Test
    fun `integrationMetadata returns confirmation token`() = runScenario {
        val intentConfiguration = PaymentSheet.IntentConfiguration(
            mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                amount = 1234,
                currency = "cad",
            ),
        )
        val initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
            intentConfiguration = intentConfiguration
        )
        assertThat(
            initializationMode.integrationMetadata(
                PaymentElementCallbacks.Builder()
                    .createIntentCallback { _ ->
                        error("Should not be called.")
                    }.build()
            )
        ).isEqualTo(IntegrationMetadata.DeferredIntent.WithConfirmationToken(intentConfiguration))
    }

    @Test
    fun `integrationMetadata returns payment method`() = runScenario {
        val intentConfiguration = PaymentSheet.IntentConfiguration(
            mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                amount = 1234,
                currency = "cad",
            ),
        )
        val initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
            intentConfiguration = intentConfiguration
        )
        assertThat(
            initializationMode.integrationMetadata(
                PaymentElementCallbacks.Builder()
                    .createIntentCallback { _, _ ->
                        error("Should not be called.")
                    }.build()
            )
        ).isEqualTo(IntegrationMetadata.DeferredIntent.WithPaymentMethod(intentConfiguration))
    }

    @Test
    fun `integrationMetadata throws when no callbacks set`() = runScenario {
        val initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                    amount = 1234,
                    currency = "cad",
                ),
            ),
        )

        assertFailsWith<IllegalStateException>("No callback for deferred intent.") {
            initializationMode.integrationMetadata(null)
        }
        assertFailsWith<IllegalStateException>("No callback for deferred intent.") {
            initializationMode.integrationMetadata(PaymentElementCallbacks.Builder().build())
        }
    }

    @Test
    fun `integrationMetadata returns cryptoOnramp`() = runScenario {
        val initializationMode = PaymentElementLoader.InitializationMode.CryptoOnramp
        assertThat(initializationMode.integrationMetadata(null))
            .isEqualTo(IntegrationMetadata.CryptoOnramp)
        assertThat(initializationMode.integrationMetadata(PaymentElementCallbacks.Builder().build()))
            .isEqualTo(IntegrationMetadata.CryptoOnramp)
    }

    @Test
    fun `Emits correct events when loading succeeds for non-deferred intent`() = runScenario {
        val loader = createPaymentElementLoader(
            linkSettings = createLinkSettings(passthroughModeEnabled = false),
        )
        val initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret")

        loader.load(
            initializationMode = initializationMode,
            paymentSheetConfiguration = PaymentSheet.Configuration(
                merchantDisplayName = "Some Name",
                customer = PaymentSheet.CustomerConfiguration(
                    id = "cus_123",
                    ephemeralKeySecret = "ek_123",
                ),
            ),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        )

        assertThat(eventReporter.loadStartedTurbine.awaitItem().initializedViaCompose).isFalse()
        val loadSucceededCall = eventReporter.loadSucceededTurbine.awaitItem()
        assertThat(loadSucceededCall.paymentSelection)
            .isEqualTo(PaymentSelection.Saved(paymentMethod = PAYMENT_METHODS.first()))
    }

    @Test
    fun `Emits correct events when loading succeeds with saved LPM selection`() = runScenario {
        testSuccessfulLoadSendsEventsCorrectly(
            paymentSelection = PaymentSelection.Saved(
                paymentMethod = PAYMENT_METHODS.last()
            )
        )
    }

    @Test
    fun `Emits correct events when loading succeeds with saved Google Pay selection`() = runScenario {
        testSuccessfulLoadSendsEventsCorrectly(
            paymentSelection = PaymentSelection.GooglePay
        )
    }

    @Test
    fun `Emits correct events when loading succeeds with saved Link selection`() = runScenario {
        testSuccessfulLoadSendsEventsCorrectly(
            paymentSelection = PaymentSelection.Link()
        )
    }

    @Test
    fun `Emits correct events when loading succeeds for deferred intent`() = runScenario {
        PaymentElementCallbackReferences[PAYMENT_ELEMENT_CALLBACKS_IDENTIFIER] = PaymentElementCallbacks.Builder()
            .createIntentCallback { _ ->
                error("Should not be called.")
            }
            .build()
        val loader = createPaymentElementLoader(
            linkSettings = createLinkSettings(passthroughModeEnabled = false),
        )
        val initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
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
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = true,
            ),
        )

        assertThat(eventReporter.loadStartedTurbine.awaitItem().initializedViaCompose).isTrue()
        val loadSucceededCall = eventReporter.loadSucceededTurbine.awaitItem()
        assertThat(loadSucceededCall.paymentSelection).isNull()
    }

    @Test
    fun `Fails to load when missing deferred intent callback`() = runScenario {
        val loader = createPaymentElementLoader(
            linkSettings = createLinkSettings(passthroughModeEnabled = false),
        )
        val initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                    amount = 1234,
                    currency = "cad",
                ),
            ),
        )

        val result = loader.load(
            initializationMode = initializationMode,
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = true,
            ),
        ).exceptionOrNull()

        assertThat(result).isInstanceOf<IllegalStateException>()
        assertThat(result?.message).isEqualTo("No callback for deferred intent.")

        assertThat(eventReporter.loadStartedTurbine.awaitItem().initializedViaCompose).isTrue()
        val loadFailedCall = eventReporter.loadFailedTurbine.awaitItem()
        assertThat(loadFailedCall.error.message).isEqualTo("No callback for deferred intent.")
    }

    @Test
    fun `Emits correct events when loading fails for non-deferred intent`() = runScenario {
        val error = PaymentSheetLoadingException.MissingAmountOrCurrency
        val loader = createPaymentElementLoader(error = error)

        loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        )

        assertThat(eventReporter.loadStartedTurbine.awaitItem().initializedViaCompose).isFalse()
        assertThat(eventReporter.loadFailedTurbine.awaitItem().error).isEqualTo(error)
    }

    @Test
    fun `Emits correct events when loading fails for deferred intent`() = runScenario {
        val error = PaymentIntentInTerminalState(Canceled)
        val loader = createPaymentElementLoader(error = error)

        loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1234,
                        currency = "cad",
                    ),
                ),
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        )

        assertThat(eventReporter.loadStartedTurbine.awaitItem().initializedViaCompose).isFalse()
        assertThat(eventReporter.loadFailedTurbine.awaitItem().error).isEqualTo(error)
    }

    @Test
    fun `Emits correct events when loading fails with invalid confirmation method`() = runScenario {
        PaymentElementCallbackReferences[PAYMENT_ELEMENT_CALLBACKS_IDENTIFIER] = PaymentElementCallbacks.Builder()
            .createIntentCallback { _ ->
                error("Should not be called.")
            }
            .build()
        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                confirmationMethod = Manual,
            )
        )

        loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1234,
                        currency = "cad",
                    ),
                ),
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        )

        assertThat(eventReporter.loadStartedTurbine.awaitItem().initializedViaCompose).isFalse()
        assertThat(eventReporter.loadFailedTurbine.awaitItem().error)
            .isEqualTo(PaymentSheetLoadingException.InvalidConfirmationMethod(Manual))
    }

    @Test
    fun `Emits correct events when we fallback to the core Stripe API`() = runScenario {
        val intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val error = APIConnectionException()

        val loader = createPaymentElementLoader(fallbackError = error)

        loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = intent.clientSecret!!,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        )

        assertThat(eventReporter.loadStartedTurbine.awaitItem().initializedViaCompose).isFalse()
        assertThat(eventReporter.elementsSessionLoadFailedTurbine.awaitItem().error).isEqualTo(error)
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Includes card brand choice state if feature is enabled`() = runScenario {
        val loader = createPaymentElementLoader(
            cardBrandChoice = ElementsSession.CardBrandChoice(
                eligible = true,
                preferredNetworks = listOf("cartes_bancaires"),
            ),
        )

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.cbcEligibility)
            .isEqualTo(CardBrandChoiceEligibility.Eligible(listOf()))

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Returns correct Link signup mode if has used Link before`() = runScenario {
        val linkStore = mock<LinkStore> {
            on { hasUsedLink() } doReturn true
        }

        val stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            isLiveMode = true,
        )

        val loader = createPaymentElementLoader(
            linkAccountState = AccountStatus.SignedOut,
            linkStore = linkStore,
            stripeIntent = stripeIntent,
        )

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.linkState?.signupMode).isNull()

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Returns correct Link signup mode if signup is disabled`() = runScenario {
        val linkStore = mock<LinkStore> {
            on { hasUsedLink() } doReturn false
        }

        val loader = createPaymentElementLoader(
            linkAccountState = AccountStatus.SignedOut,
            linkSettings = ElementsSession.LinkSettings(
                linkFundingSources = listOf("CARD"),
                linkPassthroughModeEnabled = true,
                linkFlags = emptyMap(),
                linkMode = LinkMode.Passthrough,
                disableLinkSignup = true,
                linkConsumerIncentive = null,
                useAttestationEndpoints = false,
                suppress2faModal = false,
                disableLinkRuxInFlowController = false,
                linkEnableDisplayableDefaultValuesInEce = false,
                linkMobileSkipWalletInFlowController = false,
                linkSignUpOptInFeatureEnabled = false,
                linkSignUpOptInInitialValue = false,
                linkSupportedPaymentMethodsOnboardingEnabled = listOf("CARD"),
            ),
            linkStore = linkStore,
        )

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.linkState?.signupMode).isNull()

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Returns correct Link signup mode if not saving for future use`() = runScenario {
        val loader = createPaymentElementLoader(
            linkAccountState = AccountStatus.SignedOut,
        )

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.linkState?.signupMode).isEqualTo(InsteadOfSaveForFutureUse)

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Returns correct Link signup mode if saving for future use`() = runScenario {
        val loader = createPaymentElementLoader(
            linkAccountState = AccountStatus.SignedOut,
        )

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration(
                merchantDisplayName = "Some Name",
                customer = PaymentSheet.CustomerConfiguration(
                    id = "cus_123",
                    ephemeralKeySecret = "ek_123",
                ),
            ),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.linkState?.signupMode).isEqualTo(AlongsideSaveForFutureUse)

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Returns correct Link signup mode when payment sheet save is disabled`() = runScenario {
        val loader = createPaymentElementLoader(
            linkAccountState = AccountStatus.SignedOut,
            customer = createElementsSessionCustomer(
                isPaymentMethodSaveEnabled = false,
            )
        )

        val result = loader.load(
            initializationMode = DEFAULT_INITIALIZATION_MODE,
            paymentSheetConfiguration = DEFAULT_PAYMENT_SHEET_CONFIG,
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.linkState?.signupMode).isEqualTo(InsteadOfSaveForFutureUse)

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Returns correct Link signup mode when payment sheet save is enabled`() = runScenario {
        val loader = createPaymentElementLoader(
            linkAccountState = AccountStatus.SignedOut,
            customer = createElementsSessionCustomer(
                isPaymentMethodSaveEnabled = true,
            )
        )

        val result = loader.load(
            initializationMode = DEFAULT_INITIALIZATION_MODE,
            paymentSheetConfiguration = DEFAULT_PAYMENT_SHEET_CONFIG,
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.linkState?.signupMode).isEqualTo(AlongsideSaveForFutureUse)

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Returns InsteadOfSaveForFutureUse signup mode when linkSignUpOptInFeatureEnabled is true`() = runScenario {
        val loader = createPaymentElementLoader(
            linkAccountState = AccountStatus.SignedOut,
            linkSettings = createLinkSettings(
                passthroughModeEnabled = false,
                linkSignUpOptInFeatureEnabled = true
            )
        )

        val result = loader.load(
            initializationMode = DEFAULT_INITIALIZATION_MODE,
            paymentSheetConfiguration = DEFAULT_PAYMENT_SHEET_CONFIG.newBuilder()
                .defaultBillingDetails(
                    defaultBillingDetails = PaymentSheet.BillingDetails(
                        email = "john@doe.com",
                    )
                ).build(),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.linkState?.signupMode).isEqualTo(InsteadOfSaveForFutureUse)

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Returns InsteadOfSaveForFutureUse signup mode when linkSignUpOptInFeatureEnabled is true even with customer config`() =
        runScenario {
            val loader = createPaymentElementLoader(
                linkAccountState = AccountStatus.SignedOut,
                linkSettings = createLinkSettings(
                    passthroughModeEnabled = false,
                    linkSignUpOptInFeatureEnabled = true
                )
            )

            val result = loader.load(
                initializationMode = DEFAULT_INITIALIZATION_MODE,
                paymentSheetConfiguration = PaymentSheet.Configuration(
                    merchantDisplayName = "Some Name",
                    customer = PaymentSheet.CustomerConfiguration(
                        id = "cus_123",
                        ephemeralKeySecret = "ek_123",
                    ),
                    defaultBillingDetails = PaymentSheet.BillingDetails(
                        email = "john@doe.com",
                    ),
                ),
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            ).getOrThrow()

            // Even with customer config that would normally trigger AlongsideSaveForFutureUse,
            // the feature flag should override it to InsteadOfSaveForFutureUse
            assertThat(result.paymentMethodMetadata.linkState?.signupMode).isEqualTo(InsteadOfSaveForFutureUse)

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
        }

    @Test
    fun `Returns null signup mode when linkSignUpOptInFeatureEnabled is true but user has used Link`() = runScenario {
        val linkStore = mock<LinkStore>()
        whenever(linkStore.hasUsedLink()).thenReturn(true)

        val stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            isLiveMode = true,
        )

        val loader = createPaymentElementLoader(
            linkAccountState = AccountStatus.SignedOut,
            linkStore = linkStore,
            stripeIntent = stripeIntent,
            linkSettings = createLinkSettings(
                passthroughModeEnabled = false,
                linkSignUpOptInFeatureEnabled = true
            )
        )

        val result = loader.load(
            initializationMode = DEFAULT_INITIALIZATION_MODE,
            paymentSheetConfiguration = DEFAULT_PAYMENT_SHEET_CONFIG,
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        // Feature flag should override the hasUsedLink check
        assertThat(result.paymentMethodMetadata.linkState?.signupMode).isEqualTo(null)

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Returns null signup mode when linkSignUpOptInFeatureEnabled is true but signup is disabled`() = runScenario {
        val loader = createPaymentElementLoader(
            linkAccountState = AccountStatus.SignedOut,
            linkSettings = createLinkSettings(
                passthroughModeEnabled = false,
                linkSignUpOptInFeatureEnabled = true
            ).copy(disableLinkSignup = true)
        )

        val result = loader.load(
            initializationMode = DEFAULT_INITIALIZATION_MODE,
            paymentSheetConfiguration = DEFAULT_PAYMENT_SHEET_CONFIG,
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        // Feature flag should override the disableLinkSignup check
        assertThat(result.paymentMethodMetadata.linkState?.signupMode).isEqualTo(null)

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Returns correct Link enablement based on card brand filtering`() = runScenario {
        testLinkEnablementWithCardBrandFiltering(
            passthroughModeEnabled = false,
            useNativeLink = true,
            expectedEnabled = true
        )
        testLinkEnablementWithCardBrandFiltering(
            passthroughModeEnabled = true,
            useNativeLink = true,
            expectedEnabled = true
        )
        testLinkEnablementWithCardBrandFiltering(
            passthroughModeEnabled = false,
            useNativeLink = false,
            expectedEnabled = true
        )
        testLinkEnablementWithCardBrandFiltering(
            passthroughModeEnabled = true,
            useNativeLink = false,
            expectedEnabled = false
        )
    }

    @Test
    fun `Retains all payment method when 'allowedCountries' is empty`() = runScenario {
        val paymentMethods = createCardsWithDifferentBillingDetails()

        val loader = createPaymentElementLoader(
            customer = createElementsSessionCustomer(
                paymentMethods = paymentMethods,
            ),
        )

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123_secret_123"
            ),
            integrationConfiguration = PaymentElementLoader.Configuration.PaymentSheet(
                PaymentSheet.Configuration.Builder("Example, Inc.")
                    .billingDetailsCollectionConfiguration(
                        PaymentSheet.BillingDetailsCollectionConfiguration(
                            allowedCountries = emptySet(),
                        )
                    )
                    .customer(
                        PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                            id = "cus_1",
                            clientSecret = "cuss_123",
                        )
                    )
                    .build()
            ),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        )

        val customerPaymentMethods = result.getOrNull()?.customer?.paymentMethods

        assertThat(customerPaymentMethods).isNotNull()
        assertThat(customerPaymentMethods).containsExactlyElementsIn(paymentMethods)

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Passes merchant configured billing details configuration to filter`() = runFilterScenario {
        val paymentMethods = createCardsWithDifferentBillingDetails()

        val loader = loaderScenario.createPaymentElementLoader(
            customer = createElementsSessionCustomer(
                paymentMethods = paymentMethods,
            ),
            paymentMethodFilter = paymentMethodFilterScenario.paymentMethodFilter,
        )

        val billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            allowedCountries = setOf("CA", "mx"),
        )

        loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123_secret_123"
            ),
            integrationConfiguration = PaymentElementLoader.Configuration.PaymentSheet(
                PaymentSheet.Configuration.Builder("Example, Inc.")
                    .billingDetailsCollectionConfiguration(billingDetailsCollectionConfiguration)
                    .customer(
                        PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                            id = "cus_1",
                            clientSecret = "cuss_123",
                        )
                    )
                    .build()
            ),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        )

        val filterCall = paymentMethodFilterScenario.filterCalls.awaitItem()

        assertThat(filterCall.params.billingDetailsCollectionConfiguration)
            .isEqualTo(billingDetailsCollectionConfiguration)
    }

    private suspend fun Scenario.testLinkEnablementWithCardBrandFiltering(
        passthroughModeEnabled: Boolean,
        useNativeLink: Boolean,
        expectedEnabled: Boolean,
    ) {
        val loader = createPaymentElementLoader(
            linkSettings = createLinkSettings(passthroughModeEnabled = passthroughModeEnabled),
            linkGate = FakeLinkGate().apply { setUseNativeLink(useNativeLink) }
        )

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration(
                merchantDisplayName = "Some Name",
                cardBrandAcceptance = PaymentSheet.CardBrandAcceptance.disallowed(
                    listOf(
                        PaymentSheet.CardBrandAcceptance.BrandCategory.Amex
                    )
                )
            ),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        if (expectedEnabled) {
            assertThat(result.paymentMethodMetadata.linkState).isNotNull()
        } else {
            assertThat(result.paymentMethodMetadata.linkState).isNull()
        }

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `When EPMs are requested but not returned by elements session, no EPMs are used`() = runScenario {
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
    fun `When EPMs are requested and returned by elements session, EPMs are used`() = runScenario {
        val requestedExternalPaymentMethods = listOf("external_venmo", "external_paypal")

        testExternalPaymentMethods(
            requestedExternalPaymentMethods,
            externalPaymentMethodData = PaymentSheetFixtures.PAYPAL_AND_VENMO_EXTERNAL_PAYMENT_METHOD_DATA,
            expectedExternalPaymentMethods = requestedExternalPaymentMethods,
            expectedLogMessages = emptyList(),
        )
    }

    @Test
    fun `When CPMs are requested and returned by elements session, CPMs are available`() = testCustomPaymentMethods(
        requestedCustomPaymentMethods = listOf(
            PaymentSheet.CustomPaymentMethod(
                id = "cpmt_123",
                subtitle = "Pay now".resolvableString,
                disableBillingDetailCollection = false,
            ),
            PaymentSheet.CustomPaymentMethod(
                id = "cpmt_456",
                subtitle = "Pay later".resolvableString,
                disableBillingDetailCollection = true,
            ),
            PaymentSheet.CustomPaymentMethod(
                id = "cpmt_789",
                subtitle = "Pay later".resolvableString,
                disableBillingDetailCollection = true,
            )
        ),
        returnedCustomPaymentMethods = listOf(
            ElementsSession.CustomPaymentMethod.Available(
                type = "cpmt_123",
                displayName = "CPM #1",
                logoUrl = "https://image1",
            ),
            ElementsSession.CustomPaymentMethod.Available(
                type = "cpmt_456",
                displayName = "CPM #2",
                logoUrl = "https://image2",
            ),
            ElementsSession.CustomPaymentMethod.Unavailable(
                type = "cpmt_789",
                error = "not_found",
            ),
        ),
        expectedCustomPaymentMethods = listOf(
            DisplayableCustomPaymentMethod(
                id = "cpmt_123",
                displayName = "CPM #1",
                subtitle = "Pay now".resolvableString,
                logoUrl = "https://image1",
                doesNotCollectBillingDetails = false,
            ),
            DisplayableCustomPaymentMethod(
                id = "cpmt_456",
                displayName = "CPM #2",
                subtitle = "Pay later".resolvableString,
                logoUrl = "https://image2",
                doesNotCollectBillingDetails = true,
            )
        ),
        expectedLogMessages = listOf(
            "Requested custom payment method cpmt_789 contained an " +
                "error \"not_found\"!"
        ),
    )

    @Test
    fun `When customer session configuration is provided, should pass it to 'ElementsSessionRepository'`() = runScenario {
        val repository = FakeElementsSessionRepository(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            error = null,
            linkSettings = null,
        )

        val loader = createPaymentElementLoader(
            elementsSessionRepository = repository
        )

        loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "client_secret"
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration(
                merchantDisplayName = "Merchant, Inc.",
                customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                    id = "cus_1",
                    clientSecret = "cuss_123",
                ),
            ),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        )

        assertThat(repository.lastParams?.customer).isEqualTo(
            PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                id = "cus_1",
                clientSecret = "cuss_123",
            )
        )

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadFailedTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `When 'CustomerSession' config is provided, should use payment methods from elements_session and not fetch`() =
        runScenario {
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
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = "client_secret"
                ),
                paymentSheetConfiguration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                    customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = "cus_1",
                        clientSecret = "cuss_123",
                    ),
                ),
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            ).getOrThrow()

            assertThat(attemptedToRetrievePaymentMethods).isFalse()

            assertThat(state.customer?.paymentMethods).isEqualTo(cards)

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
        }

    @Test
    fun `When 'elements_session' has remove permissions enabled, should enable remove permissions in customerMetadata`() =
        runScenario {
            val loader = createPaymentElementLoader(
                customer = ElementsSession.Customer(
                    paymentMethods = PaymentMethodFactory.cards(4),
                    session = createElementsSessionCustomerSession(
                        createEnabledMobilePaymentElement(
                            paymentMethodRemove =
                            ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Enabled,
                            paymentMethodRemoveLast =
                            ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.Enabled,
                            isPaymentMethodSaveEnabled = false,
                            allowRedisplayOverride = null,
                        )
                    ),
                    defaultPaymentMethod = null,
                )
            )

            val state = loader.load(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = "client_secret"
                ),
                paymentSheetConfiguration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                    customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = "cus_1",
                        clientSecret = "cuss_123",
                    ),
                ),
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            ).getOrThrow()

            assertThat(state.paymentMethodMetadata.customerMetadata?.permissions).isEqualTo(
                CustomerMetadata.Permissions(
                    removePaymentMethod = PaymentMethodRemovePermission.Full,
                    saveConsent = PaymentMethodSaveConsentBehavior.Disabled(overrideAllowRedisplay = null),
                    canRemoveLastPaymentMethod = true,
                    canRemoveDuplicates = true,
                    canUpdateFullPaymentMethodDetails = true
                )
            )

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
        }

    @Test
    fun `When 'elements_session' has remove permissions disabled, should disable remove permissions in customerMetadata`() =
        runScenario {
            val loader = createPaymentElementLoader(
                customer = ElementsSession.Customer(
                    paymentMethods = PaymentMethodFactory.cards(4),
                    session = createElementsSessionCustomerSession(
                        createEnabledMobilePaymentElement(
                            paymentMethodRemove =
                            ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Disabled,
                            isPaymentMethodSaveEnabled = false,
                            paymentMethodRemoveLast =
                            ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.Enabled,
                            allowRedisplayOverride = null,
                        )
                    ),
                    defaultPaymentMethod = null,
                )
            )

            val state = loader.load(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = "client_secret"
                ),
                paymentSheetConfiguration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                    customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = "cus_1",
                        clientSecret = "cuss_123",
                    ),
                ),
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            ).getOrThrow()

            assertThat(state.paymentMethodMetadata.customerMetadata?.permissions).isEqualTo(
                CustomerMetadata.Permissions(
                    removePaymentMethod = PaymentMethodRemovePermission.None,
                    saveConsent = PaymentMethodSaveConsentBehavior.Disabled(overrideAllowRedisplay = null),
                    canRemoveLastPaymentMethod = true,
                    canRemoveDuplicates = true,
                    canUpdateFullPaymentMethodDetails = true
                )
            )

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
        }

    @Test
    fun `When 'elements_session' has Payment Sheet component disabled, should disable permissions in customerMetadata`() =
        runScenario {
            val loader = createPaymentElementLoader(
                customer = ElementsSession.Customer(
                    paymentMethods = PaymentMethodFactory.cards(4),
                    session = createElementsSessionCustomerSession(
                        createEnabledMobilePaymentElement(
                            paymentMethodRemove =
                            ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Disabled,
                            isPaymentMethodSaveEnabled = false,
                            paymentMethodRemoveLast =
                            ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.Enabled,
                            allowRedisplayOverride = null,
                        )
                    ),
                    defaultPaymentMethod = null,
                )
            )

            val state = loader.load(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = "client_secret"
                ),
                paymentSheetConfiguration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                    customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = "cus_1",
                        clientSecret = "cuss_123",
                    ),
                ),
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            ).getOrThrow()

            assertThat(state.paymentMethodMetadata.customerMetadata?.permissions).isEqualTo(
                CustomerMetadata.Permissions(
                    removePaymentMethod = PaymentMethodRemovePermission.None,
                    saveConsent = PaymentMethodSaveConsentBehavior.Disabled(overrideAllowRedisplay = null),
                    canRemoveLastPaymentMethod = true,
                    canRemoveDuplicates = true,
                    canUpdateFullPaymentMethodDetails = true
                )
            )

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
        }

    @Test
    fun `When 'elements_session' has partial remove permissions, should enable partial remove permissions in customerMetadata`() =
        runScenario {
            val loader = createPaymentElementLoader(
                customer = ElementsSession.Customer(
                    paymentMethods = PaymentMethodFactory.cards(4),
                    session = createElementsSessionCustomerSession(
                        createEnabledMobilePaymentElement(
                            paymentMethodRemove =
                            ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Partial,
                            isPaymentMethodSaveEnabled = false,
                            paymentMethodRemoveLast =
                            ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.Enabled,
                            allowRedisplayOverride = null,
                        )
                    ),
                    defaultPaymentMethod = null,
                )
            )

            val state = loader.load(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = "client_secret"
                ),
                paymentSheetConfiguration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                    customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = "cus_1",
                        clientSecret = "cuss_123",
                    ),
                ),
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            ).getOrThrow()

            assertThat(state.paymentMethodMetadata.customerMetadata?.permissions).isEqualTo(
                CustomerMetadata.Permissions(
                    removePaymentMethod = PaymentMethodRemovePermission.Partial,
                    saveConsent = PaymentMethodSaveConsentBehavior.Disabled(overrideAllowRedisplay = null),
                    canRemoveLastPaymentMethod = true,
                    canRemoveDuplicates = true,
                    canUpdateFullPaymentMethodDetails = true
                )
            )

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
        }

    @Test
    fun `customer session should have canUpdateFullPaymentMethodDetails permission enabled`() =
        runScenario {
            val loader = createPaymentElementLoader(
                customer = ElementsSession.Customer(
                    paymentMethods = PaymentMethodFactory.cards(4),
                    session = createElementsSessionCustomerSession(
                        createEnabledMobilePaymentElement(
                            paymentMethodRemove =
                            ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Disabled,
                            isPaymentMethodSaveEnabled = false,
                            paymentMethodRemoveLast =
                            ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.Enabled,
                            allowRedisplayOverride = null,
                        )
                    ),
                    defaultPaymentMethod = null,
                )
            )

            val state = loader.load(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = "client_secret"
                ),
                paymentSheetConfiguration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                    customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = "cus_1",
                        clientSecret = "cuss_123",
                    ),
                ),
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            ).getOrThrow()

            assertThat(state.paymentMethodMetadata.customerMetadata?.permissions).isEqualTo(
                CustomerMetadata.Permissions(
                    removePaymentMethod = PaymentMethodRemovePermission.None,
                    saveConsent = PaymentMethodSaveConsentBehavior.Disabled(overrideAllowRedisplay = null),
                    canRemoveLastPaymentMethod = true,
                    canRemoveDuplicates = true,
                    canUpdateFullPaymentMethodDetails = true
                )
            )

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
        }

    @Test
    fun `When 'LegacyEphemeralKey' config is provided, permissions should always be enabled and remove duplicates, payment method update should be disabled`() =
        runScenario {
            val state = createPaymentElementLoader().load(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = "client_secret"
                ),
                paymentSheetConfiguration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                    customer = PaymentSheet.CustomerConfiguration(
                        id = "cus_1",
                        ephemeralKeySecret = "ek_123",
                    ),
                ),
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            ).getOrThrow()

            assertThat(state.paymentMethodMetadata.customerMetadata?.permissions).isEqualTo(
                CustomerMetadata.Permissions(
                    removePaymentMethod = PaymentMethodRemovePermission.Full,
                    saveConsent = PaymentMethodSaveConsentBehavior.Legacy,
                    canRemoveLastPaymentMethod = true,
                    canRemoveDuplicates = false,
                    canUpdateFullPaymentMethodDetails = false
                )
            )

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
        }

    @Test
    fun `When checkout session has canDetachPaymentMethod true, should enable remove permissions`() =
        testCheckoutSessionRemovePermission(
            canDetachPaymentMethod = true,
            expectedPermission = PaymentMethodRemovePermission.Full,
        )

    @Test
    fun `When checkout session has canDetachPaymentMethod false, should disable remove permissions`() =
        testCheckoutSessionRemovePermission(
            canDetachPaymentMethod = false,
            expectedPermission = PaymentMethodRemovePermission.None,
        )

    private fun testCheckoutSessionRemovePermission(
        canDetachPaymentMethod: Boolean,
        expectedPermission: PaymentMethodRemovePermission,
    ) {
        val checkoutSessionResponse = createCheckoutSessionResponse(
            canDetachPaymentMethod = canDetachPaymentMethod,
        )

        runScenario(
            checkoutSessionRepository = FakeCheckoutSessionRepository(
                initResult = Result.success(checkoutSessionResponse),
            ),
        ) {
            val loader = createPaymentElementLoader()

            val state = loader.load(
                initializationMode = PaymentElementLoader.InitializationMode.CheckoutSession(
                    clientSecret = "cs_test_123_secret_abc",
                ),
                paymentSheetConfiguration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                ),
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            ).getOrThrow()

            assertThat(state.paymentMethodMetadata.customerMetadata?.permissions?.removePaymentMethod)
                .isEqualTo(expectedPermission)

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
        }
    }

    @Test
    fun `When 'CustomerSession' config is provided but no customer object was returned in test mode, should report error and return error`() =
        runScenario {
            val errorReporter = FakeErrorReporter()

            val loader = createPaymentElementLoader(
                errorReporter = errorReporter,
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    isLiveMode = false
                )
            )

            val exception = loader.load(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = "client_secret"
                ),
                paymentSheetConfiguration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                    customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = "cus_1",
                        clientSecret = "cuss_123",
                    ),
                ),
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            ).exceptionOrNull()

            assertThat(exception).isInstanceOf<IllegalStateException>()

            assertThat(errorReporter.getLoggedErrors())
                .contains(
                    ErrorReporter
                        .UnexpectedErrorEvent
                        .PAYMENT_SHEET_LOADER_ELEMENTS_SESSION_CUSTOMER_NOT_FOUND
                        .eventName
                )

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadFailedTurbine.awaitItem()).isNotNull()
        }

    @Test
    fun `When 'CustomerSession' config is provided but no customer object was returned in live mode, should report error and continue with loading without customer`() =
        runScenario {
            val errorReporter = FakeErrorReporter()

            val loader = createPaymentElementLoader(
                errorReporter = errorReporter,
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    isLiveMode = true
                ),
                isLiveMode = true,
            )

            val state = loader.load(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = "client_secret"
                ),
                paymentSheetConfiguration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                    customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = "cus_1",
                        clientSecret = "cuss_123",
                    ),
                ),
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
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

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
        }

    @Test
    fun `When 'LegacyEphemeralKey' is provided, should fetch and use payment methods from 'CustomerRepository'`() =
        runScenario {
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
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = "client_secret"
                ),
                paymentSheetConfiguration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                    customer = PaymentSheet.CustomerConfiguration(
                        id = "cus_1",
                        ephemeralKeySecret = "ek_123",
                    ),
                ),
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            ).getOrThrow()

            assertThat(attemptedToRetrievePaymentMethods).isTrue()

            assertThat(state.customer?.paymentMethods).isEqualTo(cards)

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
        }

    @Test
    fun `When using 'CustomerSession', pass last-used customer payment method to filter`() = runFilterScenario {
        val paymentMethods = PaymentMethodFixtures.createCards(10)
        val lastUsed = paymentMethods[6]

        loaderScenario.prefsRepository.setSavedSelection(SavedSelection.PaymentMethod(lastUsed.id))

        val loader = loaderScenario.createPaymentElementLoader(
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
            ),
            paymentMethodFilter = paymentMethodFilterScenario.paymentMethodFilter,
        )

        loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(
                customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                    id = "id",
                    clientSecret = "cuss_1",
                ),
            ),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        )

        val filterCall = paymentMethodFilterScenario.filterCalls.awaitItem()

        assertThat(filterCall.params.localSavedSelection.await())
            .isEqualTo(SavedSelection.PaymentMethod(lastUsed.id))
    }

    @Test
    fun `When using 'CustomerSession', payment methods should be filtered by supported saved payment methods`() =
        runScenario {
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
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
                paymentSheetConfiguration = mockConfiguration(
                    customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = "id",
                        clientSecret = "cuss_1",
                    ),
                    allowsDelayedPaymentMethods = true,
                ),
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            ).getOrThrow()

            val expectedPaymentMethods = paymentMethods.filter { paymentMethod ->
                paymentMethod != PaymentMethodFixtures.LINK_PAYMENT_METHOD &&
                    paymentMethod != PaymentMethodFixtures.AU_BECS_DEBIT
            }

            assertThat(result.customer?.paymentMethods)
                .containsExactlyElementsIn(expectedPaymentMethods)

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
        }

    @Test
    fun `When using 'CustomerSession' & no default billing details, customer email for Link config is fetched using 'elements_session' ephemeral key`() =
        runScenario {
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
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
                paymentSheetConfiguration = mockConfiguration(
                    customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = "id",
                        clientSecret = "cuss_1",
                    ),
                    defaultBillingDetails = null
                ),
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            )

            verify(customerRepository).retrieveCustomer(
                CustomerMetadata.AccessInfo.CustomerSession(
                    customerId = "cus_1",
                    ephemeralKeySecret = "ek_123",
                    customerSessionClientSecret = "cuss_1",
                )
            )

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
        }

    @Test
    fun `When using 'CustomerSession' & has a default saved Stripe payment method, should call 'ElementsSessionRepository' with default id`() =
        runScenario {
            prefsRepository.setSavedSelection(SavedSelection.PaymentMethod("pm_1234321"))

            val repository = FakeElementsSessionRepository(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                linkSettings = null,
                error = null,
            )

            val loader = createPaymentElementLoader(
                elementsSessionRepository = repository,
            )

            loader.load(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
                paymentSheetConfiguration = mockConfiguration(
                    customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = "id",
                        clientSecret = "cuss_1",
                    ),
                ),
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            )

            assertThat(repository.lastParams?.savedPaymentMethodSelectionId)
                .isEqualTo("pm_1234321")

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadFailedTurbine.awaitItem()).isNotNull()
        }

    @OptIn(LinkDisallowFundingSourceCreationPreview::class)
    @Test
    fun `Passes Link disallowed funding source creation along to ElementsSessionRepository`() = runScenario {
        val repository = FakeElementsSessionRepository(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            linkSettings = null,
            error = null,
        )

        val loader = createPaymentElementLoader(
            linkSettings = createLinkSettings(passthroughModeEnabled = false),
            elementsSessionRepository = repository,
        )

        val config = PaymentSheet.Configuration(
            merchantDisplayName = MERCHANT_DISPLAY_NAME,
            link = PaymentSheet.LinkConfiguration.Builder()
                .display(PaymentSheet.LinkConfiguration.Display.Automatic)
                .disallowFundingSourceCreation(setOf("somethingThatsNotAllowed"))
                .build(),
        )

        loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = config,
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(repository.lastParams?.linkDisallowedFundingSourceCreation)
            .containsExactly("somethingThatsNotAllowed")

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `When using 'CustomerSession' & has a default Google Pay payment method, should not call 'ElementsSessionRepository' with default id`() =
        runScenario {
            prefsRepository.setSavedSelection(SavedSelection.GooglePay)

            val repository = FakeElementsSessionRepository(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                linkSettings = null,
                error = null,
            )

            val loader = createPaymentElementLoader(
                elementsSessionRepository = repository,
            )

            loader.load(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
                paymentSheetConfiguration = mockConfiguration(
                    customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = "id",
                        clientSecret = "cuss_1",
                    ),
                ),
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            )

            assertThat(repository.lastParams?.savedPaymentMethodSelectionId).isNull()

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadFailedTurbine.awaitItem()).isNotNull()
        }

    @Test
    fun `When DefaultPaymentMethod is provided, should pass to payment method filter`() {
        val paymentMethods = PaymentMethodFixtures.createCards(10)
        val defaultPaymentMethod = paymentMethods[4].id

        runFilterScenario {
            val loader = loaderScenario.createPaymentElementLoader(
                customer = ElementsSession.Customer(
                    paymentMethods = paymentMethodsForTestingOrdering,
                    session = createElementsSessionCustomerSession(
                        mobilePaymentElementComponent =
                        ElementsSession.Customer.Components.MobilePaymentElement.Enabled(
                            isPaymentMethodSetAsDefaultEnabled = true,
                            isPaymentMethodSaveEnabled = true,
                            paymentMethodRemove =
                            ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Enabled,
                            paymentMethodRemoveLast =
                            ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.NotProvided,
                            allowRedisplayOverride = null,
                        ),
                    ),
                    defaultPaymentMethod = defaultPaymentMethod,
                ),
                paymentMethodFilter = paymentMethodFilterScenario.paymentMethodFilter,
            )

            loader.load(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                ),
                paymentSheetConfiguration = PaymentSheet.Configuration.Builder(
                    merchantDisplayName = "Merchant, Inc."
                )
                    .customer(
                        customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                            id = "cus_1",
                            clientSecret = "cuss_123_secret_123"
                        )
                    )
                    .build(),
                metadata = PaymentElementLoader.Metadata(initializedViaCompose = false),
            )

            val filterCall = paymentMethodFilterScenario.filterCalls.awaitItem()

            assertThat(filterCall.params.remoteDefaultPaymentMethodId).isEqualTo(defaultPaymentMethod)
        }
    }

    @Test
    fun `When DefaultPaymentMethod not null, no saved selection, defaultPaymentMethod selected`() = runScenario {
        val defaultPaymentMethod = paymentMethodsForTestingOrdering[2]

        val result = getPaymentElementLoaderStateForTestingOfPaymentMethodsWithDefaultPaymentMethodId(
            lastUsedPaymentMethod = null,
            defaultPaymentMethod = defaultPaymentMethod,
        )

        assertThat((result.paymentSelection as? PaymentSelection.Saved)?.paymentMethod).isEqualTo(
            defaultPaymentMethod
        )
    }

    @Test
    fun `When DefaultPaymentMethod not null, saved selection, defaultPaymentMethod selected`() = runScenario {
        val defaultPaymentMethod = paymentMethodsForTestingOrdering[2]

        val result = getPaymentElementLoaderStateForTestingOfPaymentMethodsWithDefaultPaymentMethodId(
            lastUsedPaymentMethod = paymentMethodsForTestingOrdering[1],
            defaultPaymentMethod = defaultPaymentMethod,
        )

        assertThat((result.paymentSelection as? PaymentSelection.Saved)?.paymentMethod).isEqualTo(
            defaultPaymentMethod
        )
    }

    @Test
    fun `When DefaultPaymentMethod not null, saved selection is same as defaultPaymentMethod, defaultPaymentMethod selected`() =
        runScenario {
            val defaultPaymentMethod = paymentMethodsForTestingOrdering[2]

            val result = getPaymentElementLoaderStateForTestingOfPaymentMethodsWithDefaultPaymentMethodId(
                lastUsedPaymentMethod = paymentMethodsForTestingOrdering[2],
                defaultPaymentMethod = defaultPaymentMethod,
            )

            assertThat((result.paymentSelection as? PaymentSelection.Saved)?.paymentMethod).isEqualTo(
                defaultPaymentMethod
            )
        }

    @Test
    fun `When DefaultPaymentMethod null, no saved selection, order unchanged`() = runScenario {
        val result = getPaymentElementLoaderStateForTestingOfPaymentMethodsWithDefaultPaymentMethodId(
            lastUsedPaymentMethod = null,
            defaultPaymentMethod = null,
        )

        val observedElements = result.customer?.paymentMethods
        val expectedElements = paymentMethodsForTestingOrdering
        assertThat(observedElements).containsExactlyElementsIn(expectedElements).inOrder()
    }

    @Test
    fun `When DefaultPaymentMethod null, no saved selection, first payment method selected`() = runScenario {
        val firstPaymentMethod = paymentMethodsForTestingOrdering[0]

        val result = getPaymentElementLoaderStateForTestingOfPaymentMethodsWithDefaultPaymentMethodId(
            lastUsedPaymentMethod = null,
            defaultPaymentMethod = null,
        )

        assertThat((result.paymentSelection as? PaymentSelection.Saved)?.paymentMethod).isEqualTo(
            firstPaymentMethod
        )
    }

    @Test
    fun `When DefaultPaymentMethod null, saved selection first, order unchanged`() = runScenario {
        val result = getPaymentElementLoaderStateForTestingOfPaymentMethodsWithDefaultPaymentMethodId(
            lastUsedPaymentMethod = paymentMethodsForTestingOrdering[0],
            defaultPaymentMethod = null,
        )

        val observedElements = result.customer?.paymentMethods
        val expectedElements = paymentMethodsForTestingOrdering
        assertThat(observedElements).containsExactlyElementsIn(expectedElements).inOrder()
    }

    @Test
    fun `When DefaultPaymentMethod null, saved selection first, first payment method selected`() = runScenario {
        val firstPaymentMethod = paymentMethodsForTestingOrdering[0]

        val result = getPaymentElementLoaderStateForTestingOfPaymentMethodsWithDefaultPaymentMethodId(
            lastUsedPaymentMethod = paymentMethodsForTestingOrdering[0],
            defaultPaymentMethod = null,
        )

        assertThat((result.paymentSelection as? PaymentSelection.Saved)?.paymentMethod).isEqualTo(
            firstPaymentMethod
        )
    }

    @Test
    fun `When DefaultPaymentMethod null, saved selection not first, order unchanged`() = runScenario {
        val result = getPaymentElementLoaderStateForTestingOfPaymentMethodsWithDefaultPaymentMethodId(
            lastUsedPaymentMethod = paymentMethodsForTestingOrdering[1],
            defaultPaymentMethod = null,
        )

        val observedElements = result.customer?.paymentMethods
        val expectedElements = paymentMethodsForTestingOrdering
        assertThat(observedElements).containsExactlyElementsIn(expectedElements).inOrder()
    }

    @Test
    fun `When DefaultPaymentMethod null, saved selection not first, first payment method selected`() = runScenario {
        val firstPaymentMethod = paymentMethodsForTestingOrdering[0]

        val result = getPaymentElementLoaderStateForTestingOfPaymentMethodsWithDefaultPaymentMethodId(
            lastUsedPaymentMethod = paymentMethodsForTestingOrdering[1],
            defaultPaymentMethod = null,
        )

        assertThat((result.paymentSelection as? PaymentSelection.Saved)?.paymentMethod).isEqualTo(
            firstPaymentMethod
        )
    }

    @Test
    fun `When using 'LegacyEphemeralKey' & has a default saved Stripe payment method, should not call 'ElementsSessionRepository' with default id`() =
        runScenario {
            prefsRepository.setSavedSelection(SavedSelection.PaymentMethod("pm_1234321"))

            val repository = FakeElementsSessionRepository(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                linkSettings = null,
                error = null,
            )

            val loader = createPaymentElementLoader(
                elementsSessionRepository = repository,
            )

            loader.load(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
                paymentSheetConfiguration = mockConfiguration(
                    customer = PaymentSheet.CustomerConfiguration(
                        id = "id",
                        ephemeralKeySecret = "ek_123",
                    ),
                ),
                metadata = PaymentElementLoader.Metadata(
                    initializedViaCompose = false,
                ),
            )

            assertThat(repository.lastParams?.savedPaymentMethodSelectionId).isNull()

            assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
            assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
        }

    @Test
    fun `Should pass merchant configured card brand filter settings to payment method filter`() = runFilterScenario {
        val loader = loaderScenario.createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
            isGooglePayReady = true,
            paymentMethodFilter = paymentMethodFilterScenario.paymentMethodFilter,
        )

        val cardBrandAcceptance = PaymentSheet.CardBrandAcceptance.disallowed(
            listOf(PaymentSheet.CardBrandAcceptance.BrandCategory.Visa)
        )

        val config = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.newBuilder()
            .cardBrandAcceptance(cardBrandAcceptance)
            .build()

        loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = config,
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        )

        val filterCall = paymentMethodFilterScenario.filterCalls.awaitItem()

        assertThat(filterCall.params.cardBrandFilter).isEqualTo(
            PaymentSheetCardBrandFilter(cardBrandAcceptance)
        )
    }

    @OptIn(CardFundingFilteringPrivatePreview::class)
    @Test
    fun `Should filter saved cards by allowed funding types when flag is enabled`() = testCardFundingFiltering(
        cardFundFilteringFlagEnabled = true,
    ) {
        val filterCall = filterCalls.awaitItem()

        assertThat(filterCall.params.cardFundingFilter).isEqualTo(
            PaymentSheetCardFundingFilter(
                allowedCardFundingTypes = listOf(PaymentSheet.CardFundingType.Credit)
            )
        )
    }

    @OptIn(CardFundingFilteringPrivatePreview::class)
    @Test
    fun `Should not filter saved cards when flag is disabled`() = testCardFundingFiltering(
        cardFundFilteringFlagEnabled = false,
    ) {
        val filterCall = filterCalls.awaitItem()

        assertThat(filterCall.params.cardFundingFilter).isEqualTo(
            PaymentSheetCardFundingFilter(
                allowedCardFundingTypes = PaymentSheet.CardFundingType.entries
            )
        )
    }

    @OptIn(CardFundingFilteringPrivatePreview::class)
    private fun testCardFundingFiltering(
        cardFundFilteringFlagEnabled: Boolean?,
        block: suspend FakePaymentMethodFilter.Scenario.() -> Unit
    ) = runFilterScenario {
        val loader = loaderScenario.createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
            isGooglePayReady = true,
            elementsSessionRepository = FakeElementsSessionRepository(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
                error = null,
                linkSettings = null,
                flags = if (cardFundFilteringFlagEnabled != null) {
                    mapOf(
                        ElementsSession.Flag.ELEMENTS_MOBILE_CARD_FUND_FILTERING to cardFundFilteringFlagEnabled
                    )
                } else {
                    emptyMap()
                }
            ),
            paymentMethodFilter = paymentMethodFilterScenario.paymentMethodFilter,
        )

        val creditOnlyConfig = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.newBuilder()
            .allowedCardFundingTypes(listOf(PaymentSheet.CardFundingType.Credit))
            .build()

        loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = creditOnlyConfig,
            metadata = PaymentElementLoader.Metadata(initializedViaCompose = false),
        )

        block(paymentMethodFilterScenario)
    }

    @Test
    fun `When using 'LegacyEphemeralKey',last PM permission should be true if config value is true`() =
        removeLastPaymentMethodTest(
            customer = PaymentSheet.CustomerConfiguration(
                id = "cus_1",
                ephemeralKeySecret = "ek_123",
            ),
            canRemoveLastPaymentMethodFromConfig = true,
        ) { permissions ->
            assertThat(permissions.canRemoveLastPaymentMethod).isTrue()
        }

    @Test
    fun `When using 'CustomerSession', last PM permission should be true if server & config value is true`() =
        removeLastPaymentMethodTest(
            customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                id = "cus_1",
                clientSecret = "cuss_123",
            ),
            paymentMethodRemoveLastFeature =
            ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.Enabled,
            canRemoveLastPaymentMethodFromConfig = true,
        ) { permissions ->
            assertThat(permissions.canRemoveLastPaymentMethod).isTrue()
        }

    @Test
    fun `When using 'CustomerSession', last PM permission should be true if config value is true & no server value`() =
        removeLastPaymentMethodTest(
            customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                id = "cus_1",
                clientSecret = "cuss_123",
            ),
            paymentMethodRemoveLastFeature =
            ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.NotProvided,
            canRemoveLastPaymentMethodFromConfig = true,
        ) { permissions ->
            assertThat(permissions.canRemoveLastPaymentMethod).isTrue()
        }

    @Test
    fun `Sets client attribution metadata correctly in LinkState`() = runScenario {
        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            linkSettings = createLinkSettings(passthroughModeEnabled = false),
        )

        val config = PaymentSheet.Configuration(
            merchantDisplayName = MERCHANT_DISPLAY_NAME,
            link = PaymentSheet.LinkConfiguration(
                display = PaymentSheet.LinkConfiguration.Display.Automatic,
            ),
        )

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = config,
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.linkState).isNotNull()
        assertThat(result.paymentMethodMetadata.linkState?.configuration?.clientAttributionMetadata).isNotNull()
        assertThat(result.paymentMethodMetadata.linkState?.configuration?.clientAttributionMetadata).isEqualTo(
            result.paymentMethodMetadata.clientAttributionMetadata
        )

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Allows Link if Link display is set to 'automatic'`() = runScenario {
        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            linkSettings = createLinkSettings(passthroughModeEnabled = false),
        )

        val config = PaymentSheet.Configuration(
            merchantDisplayName = MERCHANT_DISPLAY_NAME,
            link = PaymentSheet.LinkConfiguration(
                display = PaymentSheet.LinkConfiguration.Display.Automatic,
            ),
        )

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = config,
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.linkState).isNotNull()
        assertThat(result.paymentMethodMetadata.supportedPaymentMethodTypes()).contains("link")

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Hides Link if Link display is set to 'never'`() = runScenario {
        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        )

        val config = PaymentSheet.Configuration(
            merchantDisplayName = MERCHANT_DISPLAY_NAME,
            link = PaymentSheet.LinkConfiguration(
                display = PaymentSheet.LinkConfiguration.Display.Never,
            ),
        )

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = config,
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.linkState).isNull()
        assertThat(result.paymentMethodMetadata.supportedPaymentMethodTypes()).doesNotContain("link")

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Hides Link if using web flow and collecting extra billing details`() = runScenario {
        val linkGate = FakeLinkGate()
        linkGate.setUseNativeLink(false)

        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            linkSettings = createLinkSettings(
                passthroughModeEnabled = false,
                useAttestationEndpoints = false,
            ),
            linkGate = linkGate,
        )

        val config = PaymentSheet.Configuration(
            merchantDisplayName = MERCHANT_DISPLAY_NAME,
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            ),
        )

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = config,
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.linkState).isNull()

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Should call prepare on integrity manager when attestation endpoints are enabled`() = runScenario {
        val integrityRequestManager = FakeIntegrityRequestManager()

        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                isLiveMode = true // In live mode, shouldWarmUpIntegrity depends on useAttestationEndpointsForLink
            ),
            linkSettings = createLinkSettings(
                passthroughModeEnabled = false
            ).copy(useAttestationEndpoints = true),
            integrityRequestManager = integrityRequestManager,
        )

        loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        )

        // Verify prepare was called
        integrityRequestManager.awaitPrepareCall()
        integrityRequestManager.ensureAllEventsConsumed()

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Should not call prepare on integrity manager when attestation endpoints are disabled`() = runScenario {
        val integrityRequestManager = FakeIntegrityRequestManager()

        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                isLiveMode = true // In live mode, shouldWarmUpIntegrity depends on useAttestationEndpointsForLink
            ),
            linkSettings = createLinkSettings(
                passthroughModeEnabled = false
            ).copy(useAttestationEndpoints = false),
            integrityRequestManager = integrityRequestManager,
        )

        loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        )

        // Verify prepare was not called by ensuring all events are consumed (no calls made)
        integrityRequestManager.ensureAllEventsConsumed()

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Should call prepare on integrity manager in test mode when attestation endpoints are enabled`() = runScenario {
        val integrityRequestManager = FakeIntegrityRequestManager()

        val loader = createPaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                isLiveMode = false // In test mode, behavior depends on feature flag + useAttestationEndpointsForLink
            ),
            linkSettings = createLinkSettings(
                passthroughModeEnabled = false
            ).copy(useAttestationEndpoints = true),
            integrityRequestManager = integrityRequestManager,
        )

        loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        )

        // In test mode with attestation endpoints enabled, prepare should still be called
        // (the exact behavior depends on the feature flag, but this tests the useAttestationEndpoints path)
        integrityRequestManager.awaitPrepareCall()
        integrityRequestManager.ensureAllEventsConsumed()

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    private fun removeLastPaymentMethodTest(
        customer: PaymentSheet.CustomerConfiguration,
        shouldDisableMobilePaymentElement: Boolean = false,
        paymentMethodRemoveLastFeature: ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature =
            ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.NotProvided,
        canRemoveLastPaymentMethodFromConfig: Boolean = true,
        test: (CustomerMetadata.Permissions) -> Unit,
    ) = runScenario {
        val loader = createPaymentElementLoader(
            customer = ElementsSession.Customer(
                paymentMethods = PaymentMethodFactory.cards(4),
                session = createElementsSessionCustomerSession(
                    if (shouldDisableMobilePaymentElement) {
                        ElementsSession.Customer.Components.MobilePaymentElement.Disabled
                    } else {
                        createEnabledMobilePaymentElement(
                            paymentMethodRemove =
                            ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Disabled,
                            isPaymentMethodSaveEnabled = false,
                            paymentMethodRemoveLast = paymentMethodRemoveLastFeature,
                            allowRedisplayOverride = null,
                        )
                    }
                ),
                defaultPaymentMethod = null,
            )
        )

        val state = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "client_secret"
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration(
                merchantDisplayName = "Merchant, Inc.",
                customer = customer,
                allowsRemovalOfLastSavedPaymentMethod = canRemoveLastPaymentMethodFromConfig
            ),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        test(requireNotNull(state.paymentMethodMetadata.customerMetadata).permissions)

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    private suspend fun Scenario.testExternalPaymentMethods(
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
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration.Builder(merchantDisplayName = "Example, Inc.")
                .googlePay(ConfigFixtures.GOOGLE_PAY)
                .externalPaymentMethods(requestedExternalPaymentMethods).build(),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        val actualExternalPaymentMethods = result.paymentMethodMetadata.externalPaymentMethodSpecs.map { it.type }
        assertThat(actualExternalPaymentMethods).isEqualTo(expectedExternalPaymentMethods)
        assertThat(userFacingLogger.getLoggedMessages()).containsExactlyElementsIn(expectedLogMessages)

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `All Link holdback experiments are triggered when loading PaymentSheet when Link is unavailable`() = runScenario {
        val logLinkHoldbackExperiment = FakeLogLinkHoldbackExperiment()

        val loader = createPaymentElementLoader(
            logLinkHoldbackExperiment = logLinkHoldbackExperiment,
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card"),
            )
        )

        loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        )

        val globalHoldback = logLinkHoldbackExperiment.calls.awaitItem()
        assertThat(globalHoldback.experiment).isEqualTo(ElementsSession.ExperimentAssignment.LINK_GLOBAL_HOLD_BACK)
        val globalHoldbackAA = logLinkHoldbackExperiment.calls.awaitItem()
        assertThat(globalHoldbackAA.experiment).isEqualTo(ElementsSession.ExperimentAssignment.LINK_GLOBAL_HOLD_BACK_AA)
        val abTest = logLinkHoldbackExperiment.calls.awaitItem()
        assertThat(abTest.experiment).isEqualTo(ElementsSession.ExperimentAssignment.LINK_AB_TEST)

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `All Link holdback experiments are triggered when loading PaymentSheet when Link is available`() = runScenario {
        val logLinkHoldbackExperiment = FakeLogLinkHoldbackExperiment()

        val loader = createPaymentElementLoader(
            logLinkHoldbackExperiment = logLinkHoldbackExperiment
        )

        loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = true,
            ),
        )

        val globalHoldback = logLinkHoldbackExperiment.calls.awaitItem()
        assertThat(globalHoldback.experiment).isEqualTo(ElementsSession.ExperimentAssignment.LINK_GLOBAL_HOLD_BACK)
        val globalHoldbackAA = logLinkHoldbackExperiment.calls.awaitItem()
        assertThat(globalHoldbackAA.experiment).isEqualTo(ElementsSession.ExperimentAssignment.LINK_GLOBAL_HOLD_BACK_AA)
        val abTest = logLinkHoldbackExperiment.calls.awaitItem()
        assertThat(abTest.experiment).isEqualTo(ElementsSession.ExperimentAssignment.LINK_AB_TEST)

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `Loads successfully for cryptoOnramp`() = runScenario {
        val loader = createPaymentElementLoader(
            linkSettings = createLinkSettings(passthroughModeEnabled = false),
        )
        val initializationMode = PaymentElementLoader.InitializationMode.CryptoOnramp

        val result = loader.load(
            initializationMode = initializationMode,
            paymentSheetConfiguration = PaymentSheet.Configuration("Some Name"),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = true,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.integrationMetadata).isEqualTo(IntegrationMetadata.CryptoOnramp)

        assertThat(eventReporter.loadStartedTurbine.awaitItem().initializedViaCompose).isTrue()
        eventReporter.loadSucceededTurbine.awaitItem()
    }

    @Test
    fun `analyticsMetadataFactory is called with correct parameters`() = runScenario {
        val analyticsMetadataFactory = FakeDefaultPaymentElementLoaderAnalyticsMetadataFactory {
            AnalyticsMetadata(emptyMap())
        }

        val loader = createPaymentElementLoader(
            isGooglePayReady = true,
            analyticsMetadataFactory = analyticsMetadataFactory
        )

        val config = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        val initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
            clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
        )

        loader.load(
            initializationMode = initializationMode,
            paymentSheetConfiguration = config,
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        val createCall = analyticsMetadataFactory.createCall.awaitItem()
        assertThat(createCall.initializationMode).isEqualTo(initializationMode)
        assertThat(createCall.configuration).isEqualTo(
            PaymentElementLoader.Configuration.PaymentSheet(config)
        )
        assertThat(createCall.isGooglePaySupported).isTrue()
        assertThat(createCall.customerMetadata).isNotNull()
        assertThat(createCall.integrationMetadata).isEqualTo(
            IntegrationMetadata.IntentFirst(PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value)
        )
        assertThat(createCall.elementsSession).isNotNull()
        assertThat(createCall.linkStateResult).isNotNull()
        analyticsMetadataFactory.validate()

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `analyticsMetadataFactory is called with correct parameters without customer`() = runScenario {
        PaymentElementCallbackReferences[PAYMENT_ELEMENT_CALLBACKS_IDENTIFIER] = PaymentElementCallbacks.Builder()
            .createIntentCallback { _ ->
                error("Should not be called.")
            }
            .build()

        val analyticsMetadataFactory = FakeDefaultPaymentElementLoaderAnalyticsMetadataFactory {
            AnalyticsMetadata(emptyMap())
        }

        val loader = createPaymentElementLoader(
            isGooglePayReady = false,
            linkAccountState = AccountStatus.SignedOut,
            analyticsMetadataFactory = analyticsMetadataFactory
        )

        val config = PaymentSheet.Configuration(
            merchantDisplayName = "Merchant"
        )
        val initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                    amount = 5000,
                    currency = "USD"
                ),
            ),
        )

        loader.load(
            initializationMode = initializationMode,
            paymentSheetConfiguration = config,
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        val createCall = analyticsMetadataFactory.createCall.awaitItem()
        assertThat(createCall.initializationMode).isEqualTo(initializationMode)
        assertThat(createCall.configuration).isEqualTo(
            PaymentElementLoader.Configuration.PaymentSheet(config)
        )
        assertThat(createCall.isGooglePaySupported).isFalse()
        assertThat(createCall.customerMetadata).isNull()
        assertThat(createCall.integrationMetadata).isEqualTo(
            IntegrationMetadata.DeferredIntent.WithConfirmationToken(initializationMode.intentConfiguration)
        )
        assertThat(createCall.elementsSession).isNotNull()
        assertThat(createCall.linkStateResult).isNotNull()
        analyticsMetadataFactory.validate()

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    private fun testCustomPaymentMethods(
        requestedCustomPaymentMethods: List<PaymentSheet.CustomPaymentMethod>,
        returnedCustomPaymentMethods: List<ElementsSession.CustomPaymentMethod>,
        expectedCustomPaymentMethods: List<DisplayableCustomPaymentMethod>,
        expectedLogMessages: List<String>,
    ) = runScenario {
        val userFacingLogger = FakeUserFacingLogger()
        val loader = createPaymentElementLoader(
            customPaymentMethods = returnedCustomPaymentMethods,
            userFacingLogger = userFacingLogger
        )

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            paymentSheetConfiguration = PaymentSheet.Configuration.Builder(merchantDisplayName = "Example, Inc.")
                .googlePay(ConfigFixtures.GOOGLE_PAY)
                .customPaymentMethods(requestedCustomPaymentMethods).build(),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(result.paymentMethodMetadata.displayableCustomPaymentMethods)
            .isEqualTo(expectedCustomPaymentMethods)

        assertThat(userFacingLogger.getLoggedMessages()).containsExactlyElementsIn(expectedLogMessages)

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    private suspend fun Scenario.testSuccessfulLoadSendsEventsCorrectly(paymentSelection: PaymentSelection?) {
        prefsRepository.setSavedSelection(paymentSelection?.toSavedSelection())

        val loader = createPaymentElementLoader(
            linkSettings = createLinkSettings(passthroughModeEnabled = false),
        )
        val initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret")

        loader.load(
            initializationMode = initializationMode,
            paymentSheetConfiguration = PaymentSheet.Configuration(
                merchantDisplayName = "Some Name",
                customer = PaymentSheet.CustomerConfiguration(
                    id = "cus_123",
                    ephemeralKeySecret = "ek_123",
                ),
                googlePay = PaymentSheet.GooglePayConfiguration(
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                    countryCode = "US"
                )
            ),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        )

        assertThat(eventReporter.loadStartedTurbine.awaitItem().initializedViaCompose).isFalse()

        val loadSucceededCall = eventReporter.loadSucceededTurbine.awaitItem()
        assertThat(loadSucceededCall.paymentSelection).isEqualTo(paymentSelection)
    }

    private fun createLinkSettings(
        passthroughModeEnabled: Boolean,
        linkSignUpOptInFeatureEnabled: Boolean = false,
        useAttestationEndpoints: Boolean = false,
    ): ElementsSession.LinkSettings {
        return ElementsSession.LinkSettings(
            linkFundingSources = listOf("card", "bank"),
            linkPassthroughModeEnabled = passthroughModeEnabled,
            linkMode = if (passthroughModeEnabled) LinkMode.Passthrough else LinkMode.LinkPaymentMethod,
            linkFlags = mapOf(),
            disableLinkSignup = false,
            linkConsumerIncentive = null,
            useAttestationEndpoints = useAttestationEndpoints,
            suppress2faModal = false,
            disableLinkRuxInFlowController = false,
            linkEnableDisplayableDefaultValuesInEce = false,
            linkMobileSkipWalletInFlowController = false,
            linkSignUpOptInFeatureEnabled = linkSignUpOptInFeatureEnabled,
            linkSignUpOptInInitialValue = false,
            linkSupportedPaymentMethodsOnboardingEnabled = listOf("CARD", "INSTANT_DEBITS"),
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
                createEnabledMobilePaymentElement(
                    isPaymentMethodSaveEnabled = it,
                    paymentMethodRemove = ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Enabled,
                    paymentMethodRemoveLast =
                    ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.NotProvided,
                    allowRedisplayOverride = null,
                    isPaymentMethodSetAsDefaultEnabled = false,
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

    private fun createCardsWithDifferentBillingDetails(): List<PaymentMethod> = listOf(
        PaymentMethodFactory.card(
            last4 = "4242",
            billingDetails = null,
        ),
        PaymentMethodFactory.card(
            last4 = "4444",
            billingDetails = PaymentMethod.BillingDetails(
                address = Address(
                    country = "CA",
                )
            )
        ),
        PaymentMethodFactory.card(
            last4 = "4444",
            billingDetails = PaymentMethod.BillingDetails(
                address = Address(
                    country = "US",
                )
            )
        ),
        PaymentMethodFactory.card(
            last4 = "4444",
            billingDetails = PaymentMethod.BillingDetails(
                address = Address(
                    country = "US",
                )
            )
        ),
        PaymentMethodFactory.card(
            last4 = "4444",
            billingDetails = PaymentMethod.BillingDetails(
                address = Address(
                    country = "MX",
                )
            )
        ),
    )

    private fun runFilterScenario(
        filteredPaymentMethods: List<PaymentMethod>? = null,
        block: suspend FilterScenario.() -> Unit
    ) = runScenario {
        FakePaymentMethodFilter.test(filteredPaymentMethods) {
            block(
                FilterScenario(
                    loaderScenario = this@runScenario,
                    paymentMethodFilterScenario = this
                )
            )
        }

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
    }

    private fun runScenario(
        checkoutSessionRepository: CheckoutSessionRepository = FakeCheckoutSessionRepository(),
        block: suspend Scenario.() -> Unit
    ) {
        val testDispatcher = UnconfinedTestDispatcher()
        val eventReporter = FakeLoadingEventReporter()
        val prefsRepository = FakePrefsRepository()

        @Suppress("UNCHECKED_CAST")
        val paymentMethodTypeCaptor = ArgumentCaptor.forClass(List::class.java)
            as ArgumentCaptor<List<PaymentMethod.Type>>

        Scenario(
            testDispatcher = testDispatcher,
            eventReporter = eventReporter,
            prefsRepository = prefsRepository,
            checkoutSessionRepository = checkoutSessionRepository,
            paymentMethodTypeCaptor = paymentMethodTypeCaptor,
        ).apply {
            runTest {
                block()
            }
            eventReporter.validate()
        }
    }

    private data class FilterScenario(
        val loaderScenario: Scenario,
        val paymentMethodFilterScenario: FakePaymentMethodFilter.Scenario,
    )

    private data class Scenario(
        val testDispatcher: TestDispatcher,
        val eventReporter: FakeLoadingEventReporter,
        val prefsRepository: FakePrefsRepository,
        val checkoutSessionRepository: CheckoutSessionRepository,
        val paymentMethodTypeCaptor: ArgumentCaptor<List<PaymentMethod.Type>>,
    )

    private fun Scenario.createPaymentElementLoader(
        isGooglePayReady: Boolean = true,
        stripeIntent: StripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        customerRepo: CustomerRepository = FakeCustomerRepository(paymentMethods = PAYMENT_METHODS),
        linkAccountState: AccountStatus = AccountStatus.Verified(consentPresentation = null),
        error: Throwable? = null,
        linkSettings: ElementsSession.LinkSettings? = null,
        linkGate: LinkGate = FakeLinkGate(),
        isGooglePayEnabledFromBackend: Boolean = true,
        fallbackError: Throwable? = null,
        cardBrandChoice: ElementsSession.CardBrandChoice? = null,
        linkStore: LinkStore = mock(),
        customer: ElementsSession.Customer? = null,
        externalPaymentMethodData: String? = null,
        logLinkHoldbackExperiment: LogLinkHoldbackExperiment = FakeLogLinkHoldbackExperiment(),
        errorReporter: ErrorReporter = FakeErrorReporter(),
        customPaymentMethods: List<ElementsSession.CustomPaymentMethod> = emptyList(),
        elementsSessionRepository: ElementsSessionRepository = FakeElementsSessionRepository(
            stripeIntent = stripeIntent,
            error = error,
            sessionsError = fallbackError,
            linkSettings = linkSettings,
            sessionsCustomer = customer,
            isGooglePayEnabled = isGooglePayEnabledFromBackend,
            cardBrandChoice = cardBrandChoice,
            customPaymentMethods = customPaymentMethods,
            externalPaymentMethodData = externalPaymentMethodData,
        ),
        userFacingLogger: FakeUserFacingLogger = FakeUserFacingLogger(),
        integrityRequestManager: IntegrityRequestManager = FakeIntegrityRequestManager(),
        tapToAddConnectionManager: TapToAddConnectionManager = FakeTapToAddConnectionManager.noOp(
            isSupported = false,
            isConnected = false,
        ),
        isLiveMode: Boolean = false,
        analyticsMetadataFactory: DefaultPaymentElementLoader.AnalyticsMetadataFactory =
            FakeDefaultPaymentElementLoaderAnalyticsMetadataFactory {
                AnalyticsMetadata(emptyMap())
            },
        paymentMethodFilter: PaymentMethodFilter = FakePaymentMethodFilter.noOp(),
    ): PaymentElementLoader {
        val retrieveCustomerEmailImpl = DefaultRetrieveCustomerEmail(customerRepo)
        val createLinkState = DefaultCreateLinkState(
            accountStatusProvider = { linkAccountState },
            retrieveCustomerEmail = retrieveCustomerEmailImpl,
            linkStore = linkStore,
            linkGateFactory = FakeLinkGate.Factory(linkGate),
            cardFundingFilterFactory = PaymentSheetCardFundingFilter.Factory()
        )

        return DefaultPaymentElementLoader(
            checkoutSessionRepository = checkoutSessionRepository,
            prefsRepositoryFactory = { prefsRepository },
            googlePayRepositoryFactory = object : GooglePayRepositoryFactory {
                override fun invoke(
                    environment: GooglePayEnvironment,
                    cardFundingFilter: CardFundingFilter,
                    cardBrandFilter: CardBrandFilter
                ): GooglePayRepository {
                    return GooglePayRepository { flowOf(isGooglePayReady) }
                }
            },
            elementsSessionRepository = elementsSessionRepository,
            customerRepository = customerRepo,
            lpmRepository = LpmRepository(),
            logger = Logger.noop(),
            eventReporter = eventReporter,
            errorReporter = errorReporter,
            workContext = testDispatcher,
            createLinkState = createLinkState,
            logLinkHoldbackExperiment = logLinkHoldbackExperiment,
            externalPaymentMethodsRepository = ExternalPaymentMethodsRepository(errorReporter = FakeErrorReporter()),
            userFacingLogger = userFacingLogger,
            integrityRequestManager = integrityRequestManager,
            paymentElementCallbackIdentifier = PAYMENT_ELEMENT_CALLBACKS_IDENTIFIER,
            analyticsMetadataFactory = analyticsMetadataFactory,
            tapToAddConnectionManager = tapToAddConnectionManager,
            paymentConfiguration = { PaymentConfiguration(publishableKey = if (isLiveMode) "pk_live" else "pk_test") },
            paymentMethodFilter = paymentMethodFilter,
            cardFundingFilterFactory = PaymentSheetCardFundingFilter.Factory(),
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
        private const val PAYMENT_ELEMENT_CALLBACKS_IDENTIFIER = "PaymentElementLoaderTest"
        private val PAYMENT_METHODS =
            listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD) + PaymentMethodFixtures.createCards(5)
        private val DEFAULT_PAYMENT_SHEET_CONFIG = PaymentSheet.Configuration(
            merchantDisplayName = "Some Name",
            customer = PaymentSheet.CustomerConfiguration(
                id = "cus_123",
                ephemeralKeySecret = "ek_123",
            ),
        )
        private val DEFAULT_INITIALIZATION_MODE = PaymentElementLoader.InitializationMode.PaymentIntent(
            clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
        )
    }

    private suspend fun PaymentElementLoader.load(
        initializationMode: PaymentElementLoader.InitializationMode,
        paymentSheetConfiguration: PaymentSheet.Configuration,
        metadata: PaymentElementLoader.Metadata,
    ): Result<PaymentElementLoader.State> = load(
        initializationMode = initializationMode,
        integrationConfiguration = PaymentElementLoader.Configuration.PaymentSheet(paymentSheetConfiguration),
        metadata = metadata,
    )

    private val paymentMethodsForTestingOrdering = listOf(
        PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "a1", customerId = "alice"),
        PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "b2", customerId = "bob"),
        PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "c3", customerId = "carol"),
        PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "d4", customerId = "dan")
    )

    private suspend fun Scenario.getPaymentElementLoaderStateForTestingOfPaymentMethodsWithDefaultPaymentMethodId(
        lastUsedPaymentMethod: PaymentMethod?,
        defaultPaymentMethod: PaymentMethod?,
    ): PaymentElementLoader.State {
        val defaultPaymentMethodId = defaultPaymentMethod?.id

        lastUsedPaymentMethod?.let {
            prefsRepository.setSavedSelection(SavedSelection.PaymentMethod(lastUsedPaymentMethod.id))
        }

        val loader = createPaymentElementLoader(
            customer = ElementsSession.Customer(
                paymentMethods = paymentMethodsForTestingOrdering,
                session = createElementsSessionCustomerSession(
                    mobilePaymentElementComponent = ElementsSession.Customer.Components.MobilePaymentElement.Enabled(
                        isPaymentMethodSetAsDefaultEnabled = true,
                        isPaymentMethodSaveEnabled = true,
                        paymentMethodRemove = ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Enabled,
                        paymentMethodRemoveLast =
                        ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.NotProvided,
                        allowRedisplayOverride = null,
                    ),
                ),
                defaultPaymentMethod = defaultPaymentMethodId,
            )
        )

        val result = loader.load(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("secret"),
            paymentSheetConfiguration = mockConfiguration(
                customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                    id = "id",
                    clientSecret = "cuss_1",
                ),
            ),
            metadata = PaymentElementLoader.Metadata(
                initializedViaCompose = false,
            ),
        ).getOrThrow()

        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()

        return result
    }

    private fun createCheckoutSessionResponse(
        canDetachPaymentMethod: Boolean,
    ): CheckoutSessionResponse {
        return CheckoutSessionResponse(
            id = "cs_test_123",
            amount = 5099,
            currency = "usd",
            elementsSession = ElementsSession(
                linkSettings = null,
                paymentMethodSpecs = null,
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                merchantCountry = null,
                isGooglePayEnabled = true,
                sessionsError = null,
                externalPaymentMethodData = null,
                customer = null,
                cardBrandChoice = null,
                customPaymentMethods = emptyList(),
                elementsSessionId = "es_123",
                flags = emptyMap(),
                orderedPaymentMethodTypesAndWallets = listOf("card"),
                experimentsData = null,
                passiveCaptcha = null,
                merchantLogoUrl = null,
                elementsSessionConfigId = "config_123",
                accountId = "acct_123",
                merchantId = "acct_123",
            ),
            customer = CheckoutSessionResponse.Customer(
                id = "cus_test_123",
                paymentMethods = PaymentMethodFactory.cards(2),
                canDetachPaymentMethod = canDetachPaymentMethod,
            ),
        )
    }

    private fun createEnabledMobilePaymentElement(
        isPaymentMethodSaveEnabled: Boolean = true,
        paymentMethodRemove: ElementsSession.Customer.Components.PaymentMethodRemoveFeature =
            ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Disabled,
        paymentMethodRemoveLast: ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature =
            ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.NotProvided,
        allowRedisplayOverride: PaymentMethod.AllowRedisplay? = null,
        isPaymentMethodSetAsDefaultEnabled: Boolean = false,
    ): ElementsSession.Customer.Components.MobilePaymentElement {
        return ElementsSession.Customer.Components.MobilePaymentElement.Enabled(
            isPaymentMethodSaveEnabled = isPaymentMethodSaveEnabled,
            paymentMethodRemove = paymentMethodRemove,
            paymentMethodRemoveLast = paymentMethodRemoveLast,
            allowRedisplayOverride = allowRedisplayOverride,
            isPaymentMethodSetAsDefaultEnabled = isPaymentMethodSetAsDefaultEnabled,
        )
    }
}
