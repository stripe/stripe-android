package com.stripe.android.common.taptoadd

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardBrandFilter
import com.stripe.android.PaymentConfiguration
import com.stripe.android.common.model.PaymentMethodRemovePermission
import com.stripe.android.common.taptoadd.ui.createTapToAddUxConfiguration
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.paymentelement.CreateCardPresentSetupIntentCallback
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.paymentelement.confirmation.intent.CallbackNotFoundException
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.paymentdatacollection.ach.asAddressModel
import com.stripe.android.paymentsheet.utils.FakeUserFacingLogger
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.PaymentMethodFactory.update
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.SetupIntentCallback
import com.stripe.stripeterminal.external.models.AllowRedisplay
import com.stripe.stripeterminal.external.models.CardDetails
import com.stripe.stripeterminal.external.models.CollectSetupIntentConfiguration
import com.stripe.stripeterminal.external.models.SetupIntent
import com.stripe.stripeterminal.external.models.TapToPayUxConfiguration
import com.stripe.stripeterminal.external.models.TerminalErrorCode
import com.stripe.stripeterminal.external.models.TerminalException
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.KStubbing
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.fail
import com.stripe.android.core.R as StripeCoreR
import com.stripe.stripeterminal.external.models.PaymentMethod as TerminalPaymentMethod

@OptIn(TapToAddPreview::class)
@RunWith(RobolectricTestRunner::class)
class TapToAddCollectionHandlerTest {
    @Test
    fun `create returns UnsupportedTapToAddCollectionHandler when terminal SDK is not available`() {
        val handler = TapToAddCollectionHandler.create(
            isStripeTerminalSdkAvailable = { false },
            terminalWrapper = TestTerminalWrapper.noOp(),
            stripeRepository = FakeTapToAddStripeRepository(Result.failure(NotImplementedError())),
            paymentConfiguration = TEST_PAYMENT_CONFIGURATION,
            connectionManager = FakeTapToAddConnectionManager.noOp(isSupported = true),
            tapToPayUxConfiguration = tapToPayUxConfiguration,
            userFacingLogger = FakeUserFacingLogger(),
            errorReporter = FakeErrorReporter(),
            createCardPresentSetupIntentCallbackRetriever = FakeCreateCardPresentSetupIntentCallbackRetriever.noOp(
                callbackResult = Result.success(DEFAULT_CALLBACK),
            ),
        )

        assertThat(handler).isInstanceOf(UnsupportedTapToAddCollectionHandler::class.java)
    }

    @Test
    fun `create returns DefaultTapToAddCollectionHandler when terminal SDK is available`() {
        val handler = TapToAddCollectionHandler.create(
            isStripeTerminalSdkAvailable = { true },
            terminalWrapper = TestTerminalWrapper.noOp(),
            stripeRepository = FakeTapToAddStripeRepository(),
            paymentConfiguration = TEST_PAYMENT_CONFIGURATION,
            connectionManager = FakeTapToAddConnectionManager.noOp(isSupported = true),
            tapToPayUxConfiguration = tapToPayUxConfiguration,
            userFacingLogger = FakeUserFacingLogger(),
            errorReporter = FakeErrorReporter(),
            createCardPresentSetupIntentCallbackRetriever = FakeCreateCardPresentSetupIntentCallbackRetriever.noOp(
                callbackResult = Result.success(DEFAULT_CALLBACK),
            ),
        )

        assertThat(handler).isInstanceOf(DefaultTapToAddCollectionHandler::class.java)
    }

    @Test
    fun `handler returns FailedCollection when connect fails`() {
        val error = IllegalStateException("Failed")

        runScenario(
            connectResult = Result.failure(error)
        ) {
            val result = handler.collect(DEFAULT_METADATA)

            assertFailedCollection(
                result = result,
                expectedError = error,
                expectedErrorCodeValue = "unknown",
                expectedErrorMessage = GENERIC_USER_ERROR,
            )
        }
    }

    @Test
    fun `handler returns UnsupportedDevice when await fails with unsupported device terminal error`() {
        val error = TerminalException(
            errorCode = TerminalErrorCode.TAP_TO_PAY_UNSUPPORTED_DEVICE,
            errorMessage = "Unsupported device",
        )

        runScenario(
            connectResult = Result.failure(error)
        ) {
            val result = handler.collect(DEFAULT_METADATA)

            assertThat(result).isEqualTo(
                TapToAddCollectionHandler.CollectionState.UnsupportedDevice(
                    error = error,
                    errorMessage = TapToAddErrorMessage(
                        title = R.string.stripe_tap_to_add_unsupported_device_error_title.resolvableString,
                        action = R.string.stripe_tap_to_add_unsupported_device_error_action.resolvableString,
                    ),
                )
            )
        }
    }

    @Test
    fun `handler returns FailedCollection when callback retriever throws`() {
        val error = CallbackNotFoundException(
            message = "Callback not found",
            analyticsValue = "notFound",
            resolvableError = "Callback not implemented".resolvableString
        )

        runScenario(
            callbackResult = Result.failure(error),
        ) {
            val result = handler.collect(DEFAULT_METADATA)

            assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()

            assertFailedCollection(
                result = result,
                expectedError = error,
                expectedErrorCodeValue = "noCardPresentCallbackFailure",
                expectedErrorMessage = GENERIC_USER_ERROR,
            )
        }
    }

    @Test
    fun `handler returns FailedCollection when callback returns Failure`() {
        val cause = IllegalStateException("Failed to create intent")

        runScenario(
            callbackResult = Result.success(
                CreateCardPresentSetupIntentCallback {
                    CreateIntentResult.Failure(cause = cause, displayMessage = "Something went wrong")
                }
            ),
        ) {
            val result = handler.collect(DEFAULT_METADATA)

            assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()

            assertFailedCollection(
                result = result,
                expectedError = cause,
                expectedErrorCodeValue = "failureFromMerchantCardPresentCallback",
                expectedErrorMessage = GENERIC_USER_ERROR,
            )
        }
    }

    @Test
    fun `handler returns FailedCollection when metadata has no customer`() = runScenario(
        hasConnectCall = false,
    ) {
        val metadataWithoutCustomer = PaymentMethodMetadataFactory.create(
            isTapToAddSupported = true,
            hasCustomerConfiguration = false,
        )

        val result = handler.collect(metadataWithoutCustomer)

        assertFailedCollection(
            result = result,
            expectedErrorType = IllegalStateException::class.java,
            expectedThrowableMessage = "Internal Stripe Error: Attempted to collect with tap to add without a customer",
            expectedErrorCodeValue = "noCustomer",
            expectedUserError = GENERIC_USER_ERROR,
        )
    }

    @Test
    fun `handler returns FailedCollection when metadata has checkout session customer`() {
        val checkoutSessionMetadata = PaymentMethodMetadataFactory.create(
            isTapToAddSupported = true,
            hasCustomerConfiguration = true,
        ).copy(
            customerMetadata = CustomerMetadata.CheckoutSession(
                sessionId = "cs_123",
                customerId = "cus_123",
                removePaymentMethod = PaymentMethodRemovePermission.Full,
                saveConsent = PaymentMethodSaveConsentBehavior.Disabled(overrideAllowRedisplay = null),
            )
        )

        runScenario(
            callbackResult = Result.success(
                CreateCardPresentSetupIntentCallback {
                    CreateIntentResult.Success("si_123_secret")
                }
            ),
        ) {
            val result = testScope.backgroundScope.async {
                handler.collect(checkoutSessionMetadata)
            }

            assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()
            assertThat(terminalScenario.setTapToPayUxConfigurationCalls.awaitItem()).isNotNull()

            val retrievedSetupIntent = checkRetrieveSetupIntent("si_123_secret")
            val collectedIntent = checkCollectCall(retrievedSetupIntent)
            val paymentMethod = createTerminalPaymentMethod(id = "pm_4563", last4 = "7294", brand = "mastercard")
            checkConfirmCall(
                collectedSetupIntent = collectedIntent,
                paymentMethod = paymentMethod,
            )

            val collectionResult = result.await()

            assertFailedCollection(
                result = collectionResult,
                expectedErrorType = NotImplementedError::class.java,
                expectedThrowableMessage = "Checkout sessions do not support retrieving individual payment methods!",
                expectedErrorCodeValue = "unknown",
                expectedUserError = GENERIC_USER_ERROR,
            )
        }
    }

    @Test
    fun `handler returns Collected with updated payment method when attachDefaultsToPaymentMethod is true`() {
        val paymentMethod = PaymentMethodFactory.card(id = "pm_4563").update(
            last4 = "7294",
            brand = CardBrand.MasterCard,
            addCbcNetworks = false,
        )

        runScenario(
            retrievePaymentMethodResult = Result.success(paymentMethod),
            updatePaymentMethodResult = Result.success(
                paymentMethod.copy(
                    billingDetails = DEFAULT_PAYMENT_METHOD_BILLING_DETAILS,
                )
            ),
        ) {
            val metadata = PaymentMethodMetadataFactory.create(
                isTapToAddSupported = true,
                hasCustomerConfiguration = true,
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    attachDefaultsToPaymentMethod = true,
                ),
                defaultBillingDetails = DEFAULT_BILLING_DETAILS,
            )

            val result = testScope.backgroundScope.async {
                handler.collect(metadata)
            }

            assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()
            assertThat(terminalScenario.setTapToPayUxConfigurationCalls.awaitItem())
                .isEqualTo(tapToPayUxConfiguration)

            val retrievedSetupIntent = checkRetrieveSetupIntent("si_123_secret")
            val collectedIntent = checkCollectCall(retrievedSetupIntent)
            val paymentMethod = createTerminalPaymentMethod(id = "pm_4563", last4 = "7294", brand = "mastercard")
            checkConfirmCall(
                collectedSetupIntent = collectedIntent,
                paymentMethod = paymentMethod,
            )

            val updateCall = stripeRepository.updatePaymentMethodCalls.awaitItem()
            assertThat(updateCall.paymentMethodId).isEqualTo("pm_4563")
            assertThat(updateCall.paymentMethodUpdateParams).isEqualTo(
                PaymentMethodUpdateParams.createCard(
                    billingDetails = DEFAULT_PAYMENT_METHOD_BILLING_DETAILS,
                )
            )
            assertThat(updateCall.options).isEqualTo(
                ApiRequest.Options(
                    apiKey = "ek_123",
                    stripeAccount = TEST_PAYMENT_CONFIGURATION.stripeAccountId,
                )
            )

            val collectionResult = result.await()
            assertThat(collectionResult)
                .isInstanceOf(TapToAddCollectionHandler.CollectionState.Collected::class.java)
            val collected = collectionResult as TapToAddCollectionHandler.CollectionState.Collected
            assertThat(collected.paymentMethod.billingDetails)
                .isEqualTo(DEFAULT_PAYMENT_METHOD_BILLING_DETAILS)
            assertThat(collected.paymentMethod.id).isEqualTo("pm_4563")
        }
    }

    @Test
    fun `handler returns FailedCollection when updatePaymentMethod fails after attachDefaultsToPaymentMethod`() {
        val updateFailure = IllegalStateException("Update failed")
        runScenario(
            retrievePaymentMethodResult = Result.success(PaymentMethodFactory.card(id = "pm_4563")),
            updatePaymentMethodResult = Result.failure(updateFailure),
        ) {
            val metadata = PaymentMethodMetadataFactory.create(
                isTapToAddSupported = true,
                hasCustomerConfiguration = true,
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    attachDefaultsToPaymentMethod = true,
                ),
                defaultBillingDetails = PaymentSheet.BillingDetails(name = "Jane"),
            )

            val result = testScope.backgroundScope.async {
                handler.collect(metadata)
            }

            assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()
            assertThat(terminalScenario.setTapToPayUxConfigurationCalls.awaitItem()).isNotNull()

            val retrievedSetupIntent = checkRetrieveSetupIntent("si_123_secret")
            val collectedIntent = checkCollectCall(retrievedSetupIntent)
            val paymentMethod = createTerminalPaymentMethod(id = "pm_4563")
            checkConfirmCall(
                collectedSetupIntent = collectedIntent,
                paymentMethod = paymentMethod,
            )

            assertThat(stripeRepository.updatePaymentMethodCalls.awaitItem()).isNotNull()

            assertFailedCollection(
                result = result.await(),
                expectedError = updateFailure,
                expectedErrorCodeValue = "unknown",
                expectedErrorMessage = GENERIC_USER_ERROR,
            )
        }
    }

    @Test
    fun `handler returns Collected when flow is successfully completed with card`() = runScenario(
        retrievePaymentMethodResult = Result.success(
            PaymentMethodFactory.card(id = "pm_4563")
                .copy(
                    card = PaymentMethodFactory.card(id = "pm_4563").card?.copy(
                        last4 = "7294",
                        brand = CardBrand.MasterCard,
                    ),
                )
        ),
        callbackResult = Result.success(
            CreateCardPresentSetupIntentCallback {
                CreateIntentResult.Success("si_123_secret")
            }
        ),
    ) {
        val result = testScope.backgroundScope.async {
            handler.collect(DEFAULT_METADATA)
        }

        assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()

        assertThat(terminalScenario.setTapToPayUxConfigurationCalls.awaitItem())
            .isEqualTo(tapToPayUxConfiguration)

        val retrievedSetupIntent = checkRetrieveSetupIntent("si_123_secret")
        val collectedIntent = checkCollectCall(retrievedSetupIntent)
        val paymentMethod = createTerminalPaymentMethod(id = "pm_4563", last4 = "7294", brand = "mastercard")
        checkConfirmCall(
            collectedSetupIntent = collectedIntent,
            paymentMethod = paymentMethod,
        )

        val collectionResult = result.await()
        assertThat(collectionResult)
            .isInstanceOf(TapToAddCollectionHandler.CollectionState.Collected::class.java)

        val collected = collectionResult as TapToAddCollectionHandler.CollectionState.Collected

        assertThat(collected.paymentMethod).isNotNull()
        assertThat(collected.paymentMethod.type).isEqualTo(PaymentMethod.Type.Card)
        assertThat(collected.paymentMethod.id).isEqualTo("pm_4563")
        assertThat(collected.paymentMethod.card?.last4).isEqualTo("7294")
        assertThat(collected.paymentMethod.card?.brand).isEqualTo(CardBrand.MasterCard)
    }

    @Test
    fun `handler returns FailedCollection and reports error when no payment method after confirmation`() =
        runScenario(
            callbackResult = Result.success(
                CreateCardPresentSetupIntentCallback {
                    CreateIntentResult.Success("si_123_secret")
                }
            ),
        ) {
            val result = testScope.backgroundScope.async {
                handler.collect(DEFAULT_METADATA)
            }

            assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()
            assertThat(terminalScenario.setTapToPayUxConfigurationCalls.awaitItem()).isNotNull()

            val retrievedSetupIntent = checkRetrieveSetupIntent("si_123_secret")
            val collectedIntent = checkCollectCall(retrievedSetupIntent)
            checkConfirmCall(collectedSetupIntent = collectedIntent, paymentMethod = null)

            val collectionResult = result.await()

            assertFailedCollection(
                result = collectionResult,
                expectedErrorType = IllegalStateException::class.java,
                expectedThrowableMessage = "No card payment method after collecting through tap!",
                expectedErrorCodeValue = "unknown",
                expectedUserError = GENERIC_USER_ERROR,
            )

            val errorCall = errorReporter.awaitCall()
            assertThat(errorCall.errorEvent).isEqualTo(
                ErrorReporter
                    .UnexpectedErrorEvent
                    .TAP_TO_ADD_NO_GENERATED_CARD_AFTER_SUCCESSFUL_INTENT_CONFIRMATION
            )
        }

    @Test
    fun `handler returns FailedCollection when retrieveSetupIntent fails`() = runScenario(
        callbackResult = Result.success(
            CreateCardPresentSetupIntentCallback {
                CreateIntentResult.Success("si_123_secret")
            }
        ),
    ) {
        val result = testScope.backgroundScope.async {
            handler.collect(DEFAULT_METADATA)
        }

        assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()
        assertThat(terminalScenario.setTapToPayUxConfigurationCalls.awaitItem()).isNotNull()

        val terminalException = TerminalException(
            errorCode = TerminalErrorCode.UNEXPECTED_SDK_ERROR,
            errorMessage = "Failed to retrieve setup intent"
        )

        val retrieveSetupIntentCall = terminalScenario.retrieveSetupIntentCalls.awaitItem()
        retrieveSetupIntentCall.callback.onFailure(terminalException)

        assertFailedCollection(
            result = result.await(),
            expectedError = terminalException,
            expectedErrorCodeValue = "error_code_unexpected_error_unexpected_sdk_error",
            expectedErrorMessage = TapToAddErrorMessage(
                title = StripeCoreR.string.stripe_error.resolvableString,
                action = R.string.stripe_tap_to_add_card_default_error_action.resolvableString,
            ),
        )
    }

    @Test
    fun `handler returns FailedCollection when collectSetupIntentPaymentMethod fails`() = runScenario(
        callbackResult = Result.success(
            CreateCardPresentSetupIntentCallback {
                CreateIntentResult.Success("si_123_secret")
            }
        ),
    ) {
        val result = testScope.backgroundScope.async {
            handler.collect(DEFAULT_METADATA)
        }

        assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()
        assertThat(terminalScenario.setTapToPayUxConfigurationCalls.awaitItem()).isNotNull()

        val retrievedSetupIntent = checkRetrieveSetupIntent("si_123_secret")

        val terminalException = TerminalException(
            errorCode = TerminalErrorCode.DECLINED_BY_STRIPE_API,
            errorMessage = "Card declined"
        )

        val collectPaymentMethodCall = terminalScenario.collectPaymentMethodCalls.awaitItem()
        assertThat(collectPaymentMethodCall.intent).isEqualTo(retrievedSetupIntent)
        collectPaymentMethodCall.callback.onFailure(terminalException)

        assertFailedCollection(
            result = result.await(),
            expectedError = terminalException,
            expectedErrorCodeValue = "error_code_payment_error_declined_by_stripe_api",
            expectedErrorMessage = TapToAddErrorMessage(
                title = R.string.stripe_tap_to_add_card_declined_error_title.resolvableString,
                action = R.string.stripe_tap_to_add_unusable_error_action.resolvableString,
            ),
        )
    }

    @Test
    fun `handler returns Canceled when collectSetupIntentPaymentMethod fails with CANCELED`() = runScenario(
        callbackResult = Result.success(
            CreateCardPresentSetupIntentCallback {
                CreateIntentResult.Success("si_123_secret")
            }
        ),
    ) {
        val result = testScope.backgroundScope.async {
            handler.collect(DEFAULT_METADATA)
        }

        assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()
        assertThat(terminalScenario.setTapToPayUxConfigurationCalls.awaitItem()).isNotNull()

        val retrievedSetupIntent = checkRetrieveSetupIntent("si_123_secret")

        val terminalException = TerminalException(
            errorCode = TerminalErrorCode.CANCELED,
            errorMessage = "Customer canceled",
        )

        val collectPaymentMethodCall = terminalScenario.collectPaymentMethodCalls.awaitItem()
        assertThat(collectPaymentMethodCall.intent).isEqualTo(retrievedSetupIntent)
        collectPaymentMethodCall.callback.onFailure(terminalException)

        assertThat(result.await()).isEqualTo(TapToAddCollectionHandler.CollectionState.Canceled)
    }

    @Test
    fun `handler returns FailedCollection when confirmSetupIntent fails`() = runScenario(
        callbackResult = Result.success(
            CreateCardPresentSetupIntentCallback {
                CreateIntentResult.Success("si_123_secret")
            }
        ),
    ) {
        val result = testScope.backgroundScope.async {
            handler.collect(DEFAULT_METADATA)
        }

        assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()
        assertThat(terminalScenario.setTapToPayUxConfigurationCalls.awaitItem()).isNotNull()

        val retrievedSetupIntent = checkRetrieveSetupIntent("si_123_secret")
        val collectedIntent = checkCollectCall(retrievedSetupIntent)

        val terminalException = TerminalException(
            errorCode = TerminalErrorCode.DECLINED_BY_STRIPE_API,
            errorMessage = "Setup intent confirmation failed"
        )

        val confirmSetupIntentCall = terminalScenario.confirmSetupIntentCalls.awaitItem()
        assertThat(confirmSetupIntentCall.intent).isEqualTo(collectedIntent)
        confirmSetupIntentCall.callback.onFailure(terminalException)

        assertFailedCollection(
            result = result.await(),
            expectedError = terminalException,
            expectedErrorCodeValue = "error_code_payment_error_declined_by_stripe_api",
            expectedErrorMessage = TapToAddErrorMessage(
                title = R.string.stripe_tap_to_add_card_declined_error_title.resolvableString,
                action = R.string.stripe_tap_to_add_unusable_error_action.resolvableString,
            ),
        )
    }

    @Test
    fun `handler returns FailedCollection when retrieve payment method fails`() {
        val retrieveError = IllegalStateException("Failed to retrieve payment method")
        runScenario(
            retrievePaymentMethodResult = Result.failure(retrieveError),
            callbackResult = Result.success(
                CreateCardPresentSetupIntentCallback {
                    CreateIntentResult.Success("si_123_secret")
                }
            ),
        ) {
            val result = testScope.backgroundScope.async {
                handler.collect(DEFAULT_METADATA)
            }

            assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()
            assertThat(terminalScenario.setTapToPayUxConfigurationCalls.awaitItem()).isNotNull()

            val retrievedSetupIntent = checkRetrieveSetupIntent("si_123_secret")
            val collectedIntent = checkCollectCall(retrievedSetupIntent)
            val paymentMethod = createTerminalPaymentMethod(id = "pm_4563", last4 = "7294", brand = "mastercard")

            checkConfirmCall(
                collectedSetupIntent = collectedIntent,
                paymentMethod = paymentMethod,
            )

            assertFailedCollection(
                result = result.await(),
                expectedError = retrieveError,
                expectedErrorMessage = GENERIC_USER_ERROR,
                expectedErrorCodeValue = "unknown"
            )
        }
    }

    @Test
    fun `handler returns FailedCollection when collected card brand is disallowed by filter`() =
        testCardBrandChoiceFilterFlow(
            cardBrandFilter = PaymentSheetCardBrandFilter(
                PaymentSheet.CardBrandAcceptance.disallowed(
                    listOf(PaymentSheet.CardBrandAcceptance.BrandCategory.Mastercard),
                ),
            ),
        ) { result ->
            assertFailedCollection(
                result = result,
                expectedErrorType = TapToAddCardNotSupportedException::class.java,
                expectedThrowableMessage = "Payment method is not supported by card brand filter!",
                expectedErrorCodeValue = "cardBrandNotSupportedByMerchant",
                expectedUserError = TapToAddErrorMessage(
                    title = R.string.stripe_tap_to_add_card_not_supported_error_title.resolvableString,
                    action = R.string.stripe_tap_to_add_unusable_error_action.resolvableString,
                ),
            )
        }

    @Test
    fun `handler returns Collected when collected card brand is allowed by filter`() =
        testCardBrandChoiceFilterFlow(
            cardBrandFilter = PaymentSheetCardBrandFilter(
                PaymentSheet.CardBrandAcceptance.allowed(
                    listOf(PaymentSheet.CardBrandAcceptance.BrandCategory.Mastercard),
                ),
            ),
        ) { result ->
            assertThat(result)
                .isInstanceOf(TapToAddCollectionHandler.CollectionState.Collected::class.java)
            val collected = result as TapToAddCollectionHandler.CollectionState.Collected
            assertThat(collected.paymentMethod.card?.brand).isEqualTo(CardBrand.Visa)
        }

    @Test
    fun `handler cancels collectSetupIntentPaymentMethod when coroutine is cancelled`() = runScenario(
        callbackResult = Result.success(
            CreateCardPresentSetupIntentCallback {
                CreateIntentResult.Success("si_123_secret")
            }
        ),
    ) {
        val job = testScope.backgroundScope.async {
            handler.collect(DEFAULT_METADATA)
        }

        assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()
        assertThat(terminalScenario.setTapToPayUxConfigurationCalls.awaitItem()).isNotNull()

        val retrievedSetupIntent = checkRetrieveSetupIntent("si_123_secret")

        val collectPaymentMethodCall = terminalScenario.collectPaymentMethodCalls.awaitItem()
        assertThat(collectPaymentMethodCall.intent).isEqualTo(retrievedSetupIntent)

        job.cancel()

        verify(collectPaymentMethodCall.cancelable, timeout(1000)).cancel(any())
    }

    @Test
    fun `handler cancels confirmSetupIntent when coroutine is cancelled`() = runScenario(
        callbackResult = Result.success(
            CreateCardPresentSetupIntentCallback {
                CreateIntentResult.Success("si_123_secret")
            }
        ),
    ) {
        val job = testScope.backgroundScope.async {
            handler.collect(DEFAULT_METADATA)
        }

        assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()
        assertThat(terminalScenario.setTapToPayUxConfigurationCalls.awaitItem()).isNotNull()

        val retrievedSetupIntent = checkRetrieveSetupIntent("si_123_secret")
        val collectedIntent = checkCollectCall(retrievedSetupIntent)

        val confirmSetupIntentCall = terminalScenario.confirmSetupIntentCalls.awaitItem()
        assertThat(confirmSetupIntentCall.intent).isEqualTo(collectedIntent)

        job.cancel()

        verify(confirmSetupIntentCall.cancelable, timeout(1000)).cancel(any())
    }

    @Test
    fun `handler reports error when collectSetupIntentPaymentMethod cancel fails after coroutine cancelled`() =
        runScenario(
            callbackResult = Result.success(
                CreateCardPresentSetupIntentCallback {
                    CreateIntentResult.Success("si_123_secret")
                }
            ),
        ) {
            val job = testScope.backgroundScope.async {
                handler.collect(DEFAULT_METADATA)
            }

            assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()
            assertThat(terminalScenario.setTapToPayUxConfigurationCalls.awaitItem()).isNotNull()

            checkRetrieveSetupIntent("si_123_secret")

            val collectPaymentMethodCall = terminalScenario.collectPaymentMethodCalls.awaitItem()

            val cancelFailure = TerminalException(
                errorCode = TerminalErrorCode.CANCEL_FAILED,
                errorMessage = "Failed to cancel collect",
            )

            whenever(collectPaymentMethodCall.cancelable.cancel(any()))
                .thenAnswer { invocation ->
                    val callback = invocation.getArgument<Callback>(0)
                    callback.onFailure(cancelFailure)
                }

            job.cancel()

            verify(collectPaymentMethodCall.cancelable, timeout(1000)).cancel(any())

            val reportCall = errorReporter.awaitCall()
            assertThat(reportCall.errorEvent)
                .isEqualTo(ErrorReporter.UnexpectedErrorEvent.TAP_TO_ADD_COLLECT_SETUP_INTENT_CANCEL_FAILURE)
            assertThat(reportCall.stripeException?.cause).isEqualTo(cancelFailure)
            assertThat(reportCall.additionalNonPiiParams).isEqualTo(
                mapOf("terminalErrorCode" to cancelFailure.errorCode.toLogString())
            )
        }

    @Test
    fun `handler reports error when confirmSetupIntent cancel fails after coroutine cancelled`() =
        runScenario(
            callbackResult = Result.success(
                CreateCardPresentSetupIntentCallback {
                    CreateIntentResult.Success("si_123_secret")
                }
            ),
        ) {
            val job = testScope.backgroundScope.async {
                handler.collect(DEFAULT_METADATA)
            }

            assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()
            assertThat(terminalScenario.setTapToPayUxConfigurationCalls.awaitItem()).isNotNull()

            val retrievedSetupIntent = checkRetrieveSetupIntent("si_123_secret")
            checkCollectCall(retrievedSetupIntent)

            val confirmSetupIntentCall = terminalScenario.confirmSetupIntentCalls.awaitItem()

            val cancelFailure = TerminalException(
                errorCode = TerminalErrorCode.CANCEL_FAILED,
                errorMessage = "Failed to cancel confirm",
            )

            whenever(confirmSetupIntentCall.cancelable.cancel(any())).thenAnswer { invocation ->
                val callback = invocation.getArgument<Callback>(0)
                callback.onFailure(cancelFailure)
            }

            job.cancel()

            verify(confirmSetupIntentCall.cancelable, timeout(1000)).cancel(any())

            val reportCall = errorReporter.awaitCall()
            assertThat(reportCall.errorEvent)
                .isEqualTo(ErrorReporter.UnexpectedErrorEvent.TAP_TO_ADD_CONFIRM_SETUP_INTENT_CANCEL_FAILURE)
            assertThat(reportCall.stripeException?.cause).isEqualTo(cancelFailure)
            assertThat(reportCall.additionalNonPiiParams).isEqualTo(
                mapOf("terminalErrorCode" to cancelFailure.errorCode.toLogString())
            )
        }

    private fun assertFailedCollection(
        result: TapToAddCollectionHandler.CollectionState,
    ): TapToAddCollectionHandler.CollectionState.FailedCollection {
        assertThat(result).isInstanceOf<TapToAddCollectionHandler.CollectionState.FailedCollection>()

        return result as TapToAddCollectionHandler.CollectionState.FailedCollection
    }

    private fun assertFailedCollection(
        result: TapToAddCollectionHandler.CollectionState,
        expectedError: Throwable,
        expectedErrorCodeValue: String,
        expectedErrorMessage: TapToAddErrorMessage,
    ) {
        val failed = assertFailedCollection(result)
        assertThat(failed.error).isEqualTo(expectedError)
        assertThat(failed.errorCode.value).isEqualTo(expectedErrorCodeValue)
        assertThat(failed.errorMessage).isEqualTo(expectedErrorMessage)
    }

    private fun assertFailedCollection(
        result: TapToAddCollectionHandler.CollectionState,
        expectedErrorType: Class<*>,
        expectedThrowableMessage: String,
        expectedErrorCodeValue: String,
        expectedUserError: TapToAddErrorMessage,
    ) {
        val failed = assertFailedCollection(result)
        assertThat(failed.error).isInstanceOf(expectedErrorType)
        assertThat(failed.error.message).isEqualTo(expectedThrowableMessage)
        assertThat(failed.errorCode.value).isEqualTo(expectedErrorCodeValue)
        assertThat(failed.errorMessage).isEqualTo(expectedUserError)
    }

    private fun testCardBrandChoiceFilterFlow(
        cardBrandFilter: CardBrandFilter,
        block: suspend Scenario.(result: TapToAddCollectionHandler.CollectionState) -> Unit
    ) = runScenario(
        retrievePaymentMethodResult = Result.success(
            PaymentMethodFactory.card(id = "pm_4563")
                .copy(
                    card = PaymentMethodFactory.card(id = "pm_4563").card?.copy(
                        last4 = "7294",
                        brand = CardBrand.MasterCard,
                    ),
                )
        ),
        callbackResult = Result.success(
            CreateCardPresentSetupIntentCallback {
                CreateIntentResult.Success("si_123_secret")
            }
        ),
    ) {
        val metadata = PaymentMethodMetadataFactory.create(
            isTapToAddSupported = true,
            hasCustomerConfiguration = true,
            cardBrandFilter = cardBrandFilter,
        )

        val result = testScope.backgroundScope.async {
            handler.collect(metadata)
        }

        assertThat(retrieverScenario.waitForCallbackCalls.awaitItem()).isNotNull()
        assertThat(terminalScenario.setTapToPayUxConfigurationCalls.awaitItem()).isNotNull()

        val retrievedSetupIntent = checkRetrieveSetupIntent("si_123_secret")
        val collectedIntent = checkCollectCall(retrievedSetupIntent)
        val paymentMethod = createTerminalPaymentMethod(id = "pm_4563", last4 = "7294", brand = "mastercard")

        checkConfirmCall(
            collectedSetupIntent = collectedIntent,
            paymentMethod = paymentMethod,
        )

        block(result.await())
    }

    private fun runScenario(
        hasConnectCall: Boolean = true,
        connectResult: Result<Unit> = Result.success(Unit),
        callbackResult: Result<CreateCardPresentSetupIntentCallback> =
            Result.success(DEFAULT_CALLBACK),
        retrievePaymentMethodResult: Result<PaymentMethod> =
            Result.success(PaymentMethodFactory.card()),
        updatePaymentMethodResult: Result<PaymentMethod> = Result.failure(
            IllegalStateException("updatePaymentMethod was not expected"),
        ),
        coroutineContext: CoroutineContext = EmptyCoroutineContext,
        block: suspend Scenario.() -> Unit,
    ) = runTest(coroutineContext) {
        val terminalScenario = createTerminalScenario()
        val terminalWrapper = TestTerminalWrapper.noOp(terminalScenario.terminalInstance)
        val errorReporter = FakeErrorReporter()
        val stripeRepository = FakeTapToAddStripeRepository(
            retrieveSavedPaymentMethodFromCardPresentPaymentMethodResult = retrievePaymentMethodResult,
            updatePaymentMethodResult = updatePaymentMethodResult,
        )
        FakeTapToAddConnectionManager.test(
            isSupported = true,
            connectResult = connectResult,
        ) {
            val managerScenario = this
            FakeCreateCardPresentSetupIntentCallbackRetriever.test(
                callbackResult = callbackResult,
            ) {
                val retrieverScenario = this
                block(
                    Scenario(
                        handler = DefaultTapToAddCollectionHandler(
                            terminalWrapper = terminalWrapper,
                            stripeRepository = stripeRepository,
                            paymentConfiguration = TEST_PAYMENT_CONFIGURATION,
                            connectionManager = managerScenario.tapToAddConnectionManager,
                            errorReporter = errorReporter,
                            userFacingLogger = FakeUserFacingLogger(),
                            tapToPayUxConfiguration = tapToPayUxConfiguration,
                            createCardPresentSetupIntentCallbackRetriever = retrieverScenario.retriever,
                        ),
                        tapToPayUxConfiguration = tapToPayUxConfiguration,
                        managerScenario = managerScenario,
                        retrieverScenario = retrieverScenario,
                        terminalScenario = terminalScenario,
                        errorReporter = errorReporter,
                        stripeRepository = stripeRepository,
                        testScope = this@runTest,
                    )
                )
            }

            if (hasConnectCall) {
                val connectCall = managerScenario.connectCalls.awaitItem()

                assertThat(connectCall.config.merchantDisplayName)
                    .isEqualTo(PaymentSheetFixtures.MERCHANT_DISPLAY_NAME)
            }
        }

        terminalScenario.setTapToPayUxConfigurationCalls.ensureAllEventsConsumed()
        terminalScenario.confirmSetupIntentCalls.ensureAllEventsConsumed()
        terminalScenario.retrieveSetupIntentCalls.ensureAllEventsConsumed()
        terminalScenario.collectPaymentMethodCalls.ensureAllEventsConsumed()
        stripeRepository.updatePaymentMethodCalls.ensureAllEventsConsumed()
        errorReporter.ensureAllEventsConsumed()
    }

    private fun createTerminalScenario(): TerminalScenario {
        val retrieveSetupIntentCalls = Turbine<TerminalScenario.RetrieveSetupIntentCall>()
        val collectPaymentMethodCalls = Turbine<TerminalScenario.CollectPaymentMethodCall>()
        val confirmSetupIntentCalls = Turbine<TerminalScenario.ConfirmSetupIntentCall>()
        val setTapToPayUxConfigurationCalls = Turbine<TapToPayUxConfiguration>()

        val terminalInstance: Terminal = mock {
            mockSetTapToPayUxConfiguration(setTapToPayUxConfigurationCalls)
            mockRetrieveSetupIntent(retrieveSetupIntentCalls)
            mockCollectPaymentMethod(collectPaymentMethodCalls)
            mockConfirmSetupIntent(confirmSetupIntentCalls)
        }

        return TerminalScenario(
            terminalInstance = terminalInstance,
            retrieveSetupIntentCalls = retrieveSetupIntentCalls,
            collectPaymentMethodCalls = collectPaymentMethodCalls,
            confirmSetupIntentCalls = confirmSetupIntentCalls,
            setTapToPayUxConfigurationCalls = setTapToPayUxConfigurationCalls,
        )
    }

    private suspend fun Scenario.checkRetrieveSetupIntent(clientSecret: String): SetupIntent {
        val retrievedSetupIntent = mock<SetupIntent>()
        val retrieveSetupIntentCall = terminalScenario.retrieveSetupIntentCalls.awaitItem()
        assertThat(retrieveSetupIntentCall.clientSecret).isEqualTo(clientSecret)
        retrieveSetupIntentCall.callback.onSuccess(retrievedSetupIntent)
        return retrievedSetupIntent
    }

    private suspend fun Scenario.checkCollectCall(
        retrievedSetupIntent: SetupIntent,
    ): SetupIntent {
        val collectedIntent = mock<SetupIntent>()
        val collectPaymentMethodCall = terminalScenario.collectPaymentMethodCalls.awaitItem()
        assertThat(collectPaymentMethodCall.intent).isEqualTo(retrievedSetupIntent)
        assertThat(collectPaymentMethodCall.allowRedisplay).isEqualTo(AllowRedisplay.ALWAYS)
        assertThat(collectPaymentMethodCall.config).isEqualTo(
            CollectSetupIntentConfiguration.Builder()
                .build()
            )
        collectPaymentMethodCall.callback.onSuccess(collectedIntent)
        return collectedIntent
    }

    private fun createTerminalPaymentMethod(
        id: String = "pm_generated_123",
        last4: String? = "4242",
        brand: String? = "visa",
    ): TerminalPaymentMethod {
        val cardDetails: CardDetails? = if (last4 != null || brand != null) {
            mock {
                on { this.last4 } doReturn last4
                on { this.brand } doReturn brand
            }
        } else {
            null
        }

        return mock {
            on { this.id } doReturn id
            on { this.cardDetails } doReturn cardDetails
        }
    }

    private suspend fun Scenario.checkConfirmCall(
        collectedSetupIntent: SetupIntent,
        paymentMethod: TerminalPaymentMethod?,
    ) {
        val confirmSetupIntentCall = terminalScenario.confirmSetupIntentCalls.awaitItem()
        assertThat(confirmSetupIntentCall.intent).isEqualTo(collectedSetupIntent)

        val paymentMethodIdValue = paymentMethod?.id
        val confirmedSetupIntent: SetupIntent = mock {
            on { paymentMethodId } doReturn paymentMethodIdValue
        }

        confirmSetupIntentCall.callback.onSuccess(confirmedSetupIntent)
    }

    private fun KStubbing<Terminal>.mockSetTapToPayUxConfiguration(
        setTapToPayUxConfigurationCalls: Turbine<TapToPayUxConfiguration>
    ) {
        on { setTapToPayUxConfiguration(any<TapToPayUxConfiguration>()) } doAnswer { invocation ->
            val config = invocation.arguments[0] as? TapToPayUxConfiguration
                ?: fail("Invalid argument: Not a TapToPayUxConfiguration!")
            setTapToPayUxConfigurationCalls.add(config)
        }
    }

    private fun KStubbing<Terminal>.mockRetrieveSetupIntent(
        retrieveSetupIntentCalls: Turbine<TerminalScenario.RetrieveSetupIntentCall>
    ) {
        on {
            retrieveSetupIntent(
                clientSecret = any<String>(),
                callback = any<SetupIntentCallback>(),
            )
        } doAnswer { invocation ->
            val setupIntentClientSecret = invocation.arguments[0] as? String
                ?: fail("Invalid argument: Not a setup intent!")

            val callback = invocation.arguments[1] as? SetupIntentCallback
                ?: fail("Invalid argument: Not a setup intent callback!")

            retrieveSetupIntentCalls.add(
                TerminalScenario.RetrieveSetupIntentCall(
                    clientSecret = setupIntentClientSecret,
                    callback = callback,
                )
            )
        }
    }

    private fun KStubbing<Terminal>.mockCollectPaymentMethod(
        collectPaymentMethodCalls: Turbine<TerminalScenario.CollectPaymentMethodCall>
    ) {
        on {
            collectSetupIntentPaymentMethod(
                intent = any<SetupIntent>(),
                allowRedisplay = any<AllowRedisplay>(),
                config = any<CollectSetupIntentConfiguration>(),
                callback = any<SetupIntentCallback>()
            )
        } doAnswer { invocation ->
            val setupIntent = invocation.arguments[0] as? SetupIntent
                ?: fail("Invalid argument: Not a setup intent!")

            val allowRedisplay = invocation.arguments[1] as? AllowRedisplay
                ?: fail("Invalid argument: Not an allow redisplay value!")

            val config = invocation.arguments[2] as? CollectSetupIntentConfiguration
                ?: fail("Invalid argument: Not a setup intent config!")

            val callback = invocation.arguments[3] as? SetupIntentCallback
                ?: fail("Invalid argument: Not a setup intent callback!")

            val cancelable = mock<Cancelable>()

            collectPaymentMethodCalls.add(
                TerminalScenario.CollectPaymentMethodCall(
                    intent = setupIntent,
                    allowRedisplay = allowRedisplay,
                    config = config,
                    callback = callback,
                    cancelable = cancelable,
                )
            )

            cancelable
        }
    }

    private fun KStubbing<Terminal>.mockConfirmSetupIntent(
        confirmSetupIntentCalls: Turbine<TerminalScenario.ConfirmSetupIntentCall>
    ) {
        on {
            confirmSetupIntent(
                intent = any<SetupIntent>(),
                callback = any<SetupIntentCallback>()
            )
        } doAnswer { invocation ->
            val setupIntent = invocation.arguments[0] as? SetupIntent
                ?: fail("Invalid argument: Not a setup intent!")

            val callback = invocation.arguments[1] as? SetupIntentCallback
                ?: fail("Invalid argument: Not a setup intent callback!")

            val cancelable = mock<Cancelable>()

            confirmSetupIntentCalls.add(
                TerminalScenario.ConfirmSetupIntentCall(
                    intent = setupIntent,
                    callback = callback,
                    cancelable = cancelable,
                )
            )

            cancelable
        }
    }

    private class Scenario(
        val testScope: TestScope,
        val handler: TapToAddCollectionHandler,
        val tapToPayUxConfiguration: TapToPayUxConfiguration,
        val managerScenario: FakeTapToAddConnectionManager.Scenario,
        val retrieverScenario: FakeCreateCardPresentSetupIntentCallbackRetriever.Scenario,
        val terminalScenario: TerminalScenario,
        val errorReporter: FakeErrorReporter,
        val stripeRepository: FakeTapToAddStripeRepository,
    )

    private class TerminalScenario(
        val terminalInstance: Terminal,
        val retrieveSetupIntentCalls: ReceiveTurbine<RetrieveSetupIntentCall>,
        val collectPaymentMethodCalls: ReceiveTurbine<CollectPaymentMethodCall>,
        val confirmSetupIntentCalls: ReceiveTurbine<ConfirmSetupIntentCall>,
        val setTapToPayUxConfigurationCalls: ReceiveTurbine<TapToPayUxConfiguration>,
    ) {
        class RetrieveSetupIntentCall(
            val clientSecret: String,
            val callback: SetupIntentCallback,
        )

        class CollectPaymentMethodCall(
            val intent: SetupIntent,
            val allowRedisplay: AllowRedisplay,
            val config: CollectSetupIntentConfiguration,
            val callback: SetupIntentCallback,
            val cancelable: Cancelable,
        )

        class ConfirmSetupIntentCall(
            val intent: SetupIntent,
            val callback: SetupIntentCallback,
            val cancelable: Cancelable,
        )
    }

    private class FakeCreateCardPresentSetupIntentCallbackRetriever private constructor(
        private val callbackResult: Result<CreateCardPresentSetupIntentCallback>,
    ) : CreateCardPresentSetupIntentCallbackRetriever {
        private val waitForCallbackCalls = Turbine<Unit>()

        override suspend fun waitForCallback(): CreateCardPresentSetupIntentCallback {
            waitForCallbackCalls.add(Unit)
            return callbackResult.getOrThrow()
        }

        class Scenario(
            val retriever: CreateCardPresentSetupIntentCallbackRetriever,
            val waitForCallbackCalls: ReceiveTurbine<Unit>,
        )

        companion object {
            suspend fun test(
                callbackResult: Result<CreateCardPresentSetupIntentCallback>,
                block: suspend Scenario.() -> Unit,
            ) {
                val retriever = FakeCreateCardPresentSetupIntentCallbackRetriever(callbackResult)

                block(
                    Scenario(
                        retriever = retriever,
                        waitForCallbackCalls = retriever.waitForCallbackCalls,
                    )
                )

                retriever.waitForCallbackCalls.ensureAllEventsConsumed()
            }

            fun noOp(
                callbackResult: Result<CreateCardPresentSetupIntentCallback>,
            ) = FakeCreateCardPresentSetupIntentCallbackRetriever(callbackResult)
        }
    }

    private companion object {
        val GENERIC_USER_ERROR = TapToAddErrorMessage(
            title = StripeCoreR.string.stripe_error.resolvableString,
            action = StripeCoreR.string.stripe_try_again_later.resolvableString,
        )

        val tapToPayUxConfiguration = createTapToAddUxConfiguration()
        val DEFAULT_METADATA = PaymentMethodMetadataFactory.create(
            isTapToAddSupported = true,
            hasCustomerConfiguration = true,
        )
        val DEFAULT_CALLBACK = CreateCardPresentSetupIntentCallback {
            CreateIntentResult.Success("si_123_secret")
        }
        val TEST_PAYMENT_CONFIGURATION = PaymentConfiguration(publishableKey = "pk_test")
        val DEFAULT_BILLING_DETAILS = PaymentSheet.BillingDetails(
            name = "Jane Doe",
            email = "jane@example.com",
            phone = "+15551234567",
            address = PaymentSheet.Address(
                line1 = "123 Main St",
                city = "San Francisco",
                state = "CA",
                country = "US",
                postalCode = "94103",
            ),
        )
        val DEFAULT_PAYMENT_METHOD_BILLING_DETAILS = PaymentMethod.BillingDetails(
            name = DEFAULT_BILLING_DETAILS.name,
            phone = DEFAULT_BILLING_DETAILS.phone,
            email = DEFAULT_BILLING_DETAILS.email,
            address = DEFAULT_BILLING_DETAILS.address?.asAddressModel(),
        )
    }
}

private class FakeTapToAddStripeRepository(
    private val retrieveSavedPaymentMethodFromCardPresentPaymentMethodResult: Result<PaymentMethod> =
        Result.failure(IllegalStateException("Failed!")),
    private val updatePaymentMethodResult: Result<PaymentMethod> = Result.failure(
        IllegalStateException("updatePaymentMethod was not expected"),
    ),
) : AbsFakeStripeRepository() {

    val updatePaymentMethodCalls = Turbine<UpdatePaymentMethodCall>()

    data class UpdatePaymentMethodCall(
        val paymentMethodId: String,
        val paymentMethodUpdateParams: PaymentMethodUpdateParams,
        val options: ApiRequest.Options,
    )

    override suspend fun retrieveSavedPaymentMethodFromCardPresentPaymentMethod(
        cardPresentPaymentMethodId: String,
        customerId: String,
        options: ApiRequest.Options
    ): Result<PaymentMethod> = retrieveSavedPaymentMethodFromCardPresentPaymentMethodResult

    override suspend fun updatePaymentMethod(
        paymentMethodId: String,
        paymentMethodUpdateParams: PaymentMethodUpdateParams,
        options: ApiRequest.Options
    ): Result<PaymentMethod> {
        updatePaymentMethodCalls.add(
            UpdatePaymentMethodCall(
                paymentMethodId = paymentMethodId,
                paymentMethodUpdateParams = paymentMethodUpdateParams,
                options = options,
            )
        )
        return updatePaymentMethodResult
    }
}
