package com.stripe.android.paymentelement.confirmation.interceptor

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.Address
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.PreparePaymentMethodHandler
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.createIntentConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.state.PaymentElementLoader.InitializationMode
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentIntentFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.stripe.android.R as PaymentsCoreR

@OptIn(SharedPaymentTokenSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
class SharedPaymentTokenConfirmationInterceptorTest {
    @Test
    fun `If initialized with shared payment token, should fail if 'preparePaymentMethodHandler' is null`() =
        testNoProvider(
            event = ErrorReporter.ExpectedErrorEvent.PREPARE_PAYMENT_METHOD_HANDLER_NULL,
            failureMessage = PREPARE_PAYMENT_METHOD_HANDLER_MESSAGE,
            userMessage = PaymentsCoreR.string.stripe_internal_error.resolvableString,
        ) { errorReporter ->

            val interceptor = createIntentConfirmationInterceptor(
                initializationMode = InitializationMode.DeferredIntent(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        sharedPaymentTokenSessionWithMode = PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = 1099L,
                            currency = "usd",
                        ),
                        sellerDetails = PaymentSheet.IntentConfiguration.SellerDetails(
                            businessName = "My business, Inc.",
                            networkId = "network_id",
                            externalId = "external_id"
                        )
                    ),
                ),
                stripeRepository = stripeRepositoryReturning(
                    onCreatePaymentMethodId = "pm_1234",
                    onRetrievePaymentMethodId = "pm_5678"
                ),
                errorReporter = errorReporter,
            )

            interceptor.interceptDefaultSavedPaymentMethod()
        }

    @Test
    @Suppress("LongMethod")
    fun `If initialized with shared payment token, should call 'onPreparePaymentMethod' with saved PM`() =
        runTest {
            val completablePaymentMethod = CompletableDeferred<PaymentMethod>()
            val completableShippingAddress = CompletableDeferred<AddressDetails?>()
            val createSavedPaymentMethodRadarSessionCalls = Turbine<CreateSavedPaymentMethodRadarSessionCall>()

            val providedPaymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
            val providedShippingAddress = SHIPPING_ADDRESS

            val interceptor = createIntentConfirmationInterceptor(
                initializationMode = InitializationMode.DeferredIntent(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        sharedPaymentTokenSessionWithMode = PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = 1099L,
                            currency = "usd"
                        ),
                        sellerDetails = PaymentSheet.IntentConfiguration.SellerDetails(
                            businessName = "My business, Inc.",
                            networkId = "network_id",
                            externalId = "external_id"
                        )
                    ),
                ),
                stripeRepository = stripeRepositoryReturning(
                    onCreatePaymentMethodId = "pm_1234",
                    onRetrievePaymentMethodId = "pm_5678",
                    createSavedPaymentMethodRadarSessionCalls = createSavedPaymentMethodRadarSessionCalls,
                ),
                preparePaymentMethodHandlerProvider = {
                    PreparePaymentMethodHandler { paymentMethod, shippingAddress ->
                        completablePaymentMethod.complete(paymentMethod)
                        completableShippingAddress.complete(shippingAddress)
                    }
                },
            )

            val intent = PaymentIntentFactory.create()
            val nextStep = interceptor.intercept(
                intent = intent,
                confirmationOption = PaymentMethodConfirmationOption.Saved(
                    paymentMethod = providedPaymentMethod,
                    optionsParams = null,
                    passiveCaptchaParams = null,
                    hCaptchaToken = null,
                ),
                shippingValues = providedShippingAddress,
            )

            assertThat(nextStep).isEqualTo(
                ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>(
                    intent = intent,
                    deferredIntentConfirmationType = DeferredIntentConfirmationType.None,
                    completedFullPaymentFlow = false,
                )
            )

            val paymentMethod = completablePaymentMethod.await()

            assertThat(paymentMethod).isEqualTo(providedPaymentMethod)

            val shippingAddress = completableShippingAddress.await()

            verifyShipping(providedShippingAddress, shippingAddress)

            val createRadarSessionCall = createSavedPaymentMethodRadarSessionCalls.awaitItem()

            assertThat(createRadarSessionCall.paymentMethodId).isEqualTo("pm_123456789")
            assertThat(createRadarSessionCall.requestOptions.apiKey).isEqualTo("pk")
            assertThat(createRadarSessionCall.requestOptions.stripeAccount).isNull()

            createSavedPaymentMethodRadarSessionCalls.ensureAllEventsConsumed()
        }

    @Test
    fun `If initialized with shared payment token, should call 'onPreparePaymentMethod' with new PM`() =
        runTest {
            val completablePaymentMethod = CompletableDeferred<PaymentMethod>()
            val completableShippingAddress = CompletableDeferred<AddressDetails?>()
            val createSavedPaymentMethodRadarSessionCalls = Turbine<CreateSavedPaymentMethodRadarSessionCall>()

            val interceptor = createIntentConfirmationInterceptor(
                initializationMode = InitializationMode.DeferredIntent(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        sharedPaymentTokenSessionWithMode = PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = 1099L,
                            currency = "usd",
                        ),
                        sellerDetails = PaymentSheet.IntentConfiguration.SellerDetails(
                            businessName = "My business, Inc.",
                            networkId = "network_id",
                            externalId = "external_id"
                        )
                    ),
                ),
                stripeRepository = stripeRepositoryReturning(
                    onCreatePaymentMethodId = "pm_1234",
                    onRetrievePaymentMethodId = "pm_5678",
                    createSavedPaymentMethodRadarSessionCalls = createSavedPaymentMethodRadarSessionCalls,
                ),
                preparePaymentMethodHandlerProvider = {
                    PreparePaymentMethodHandler { paymentMethod, shippingAddress ->
                        completablePaymentMethod.complete(paymentMethod)
                        completableShippingAddress.complete(shippingAddress)
                    }
                },
            )

            val intent = PaymentIntentFactory.create()
            val nextStep = interceptor.interceptDefaultNewPaymentMethod()

            assertThat(nextStep).isEqualTo(
                ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>(
                    intent = intent,
                    deferredIntentConfirmationType = DeferredIntentConfirmationType.None,
                    completedFullPaymentFlow = false,
                )
            )

            val paymentMethod = completablePaymentMethod.await()

            assertThat(paymentMethod.id).isEqualTo("pm_1234")

            val shippingAddress = completableShippingAddress.await()

            assertThat(shippingAddress).isNull()

            createSavedPaymentMethodRadarSessionCalls.verify()
        }

    @Test
    fun `If failed to make radar session, should still continue with preparing payment method`() =
        runTest {
            val completablePaymentMethod = CompletableDeferred<PaymentMethod>()
            val completableShippingAddress = CompletableDeferred<AddressDetails?>()
            val createSavedPaymentMethodRadarSessionCalls = Turbine<CreateSavedPaymentMethodRadarSessionCall>()

            val error = IllegalStateException("Failed to make radar session!")
            val eventReporter = FakeErrorReporter()

            val interceptor = createIntentConfirmationInterceptor(
                initializationMode = InitializationMode.DeferredIntent(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        sharedPaymentTokenSessionWithMode = PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = 1099L,
                            currency = "usd",
                        ),
                        sellerDetails = PaymentSheet.IntentConfiguration.SellerDetails(
                            businessName = "My business, Inc.",
                            networkId = "network_id",
                            externalId = "external_id"
                        )
                    ),
                ),
                stripeRepository = stripeRepositoryReturning(
                    onCreatePaymentMethodId = "pm_1234",
                    onRetrievePaymentMethodId = "pm_5678",
                    createSavedPaymentMethodRadarSessionResult = Result.failure(error),
                    createSavedPaymentMethodRadarSessionCalls = createSavedPaymentMethodRadarSessionCalls,
                ),
                errorReporter = eventReporter,
                preparePaymentMethodHandlerProvider = {
                    PreparePaymentMethodHandler { paymentMethod, shippingAddress ->
                        completablePaymentMethod.complete(paymentMethod)
                        completableShippingAddress.complete(shippingAddress)
                    }
                },
            )

            val intent = PaymentIntentFactory.create()
            val nextStep = interceptor.interceptDefaultNewPaymentMethod()

            assertThat(nextStep).isEqualTo(
                ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>(
                    intent = intent,
                    deferredIntentConfirmationType = DeferredIntentConfirmationType.None,
                    completedFullPaymentFlow = false,
                )
            )

            val paymentMethod = completablePaymentMethod.await()

            assertThat(paymentMethod.id).isEqualTo("pm_1234")

            val shippingAddress = completableShippingAddress.await()

            assertThat(shippingAddress).isNull()

            createSavedPaymentMethodRadarSessionCalls.verify()

            eventReporter.verifyCreateSavedPaymentMethodRadarSessionCall(error)
        }

    private suspend fun FakeErrorReporter.verifyCreateSavedPaymentMethodRadarSessionCall(
        error: Exception,
    ) {
        val failedRadarEvent = awaitCall()

        assertThat(failedRadarEvent.errorEvent)
            .isEqualTo(ErrorReporter.ExpectedErrorEvent.SAVED_PAYMENT_METHOD_RADAR_SESSION_FAILURE)
        assertThat(failedRadarEvent.stripeException?.cause).isEqualTo(error)

        ensureAllEventsConsumed()
    }

    private fun verifyShipping(
        expectedShippingAddress: ConfirmPaymentIntentParams.Shipping,
        actualShippingAddress: AddressDetails?
    ) {
        assertThat(actualShippingAddress?.name).isEqualTo(expectedShippingAddress.getName())
        assertThat(actualShippingAddress?.phoneNumber).isEqualTo(expectedShippingAddress.getPhone())
        assertThat(actualShippingAddress?.address?.line1).isEqualTo(expectedShippingAddress.getAddress().line1)
        assertThat(actualShippingAddress?.address?.line2).isEqualTo(expectedShippingAddress.getAddress().line2)
        assertThat(actualShippingAddress?.address?.city).isEqualTo(expectedShippingAddress.getAddress().city)
        assertThat(actualShippingAddress?.address?.state).isEqualTo(expectedShippingAddress.getAddress().state)
        assertThat(actualShippingAddress?.address?.country)
            .isEqualTo(expectedShippingAddress.getAddress().country)
    }

    private suspend fun Turbine<CreateSavedPaymentMethodRadarSessionCall>.verify() {
        val createRadarSessionCall = awaitItem()

        assertThat(createRadarSessionCall.paymentMethodId).isEqualTo("pm_1234")
        assertThat(createRadarSessionCall.requestOptions.apiKey).isEqualTo("pk")
        assertThat(createRadarSessionCall.requestOptions.stripeAccount).isNull()

        ensureAllEventsConsumed()
    }

    private companion object {
        const val PREPARE_PAYMENT_METHOD_HANDLER_MESSAGE =
            "PreparePaymentMethodHandler must be implemented when using IntentConfiguration with " +
                "shared payment tokens!"

        val SHIPPING_ADDRESS = ConfirmPaymentIntentParams.Shipping(
            address = Address(
                city = "South San Francisc",
                line1 = "123 Apple Street",
                line2 = "Unit #2",
                state = "CA",
                postalCode = "99999",
                country = "US"
            ),
            phone = "11234567890",
            name = "John Doe"
        )
    }
}
