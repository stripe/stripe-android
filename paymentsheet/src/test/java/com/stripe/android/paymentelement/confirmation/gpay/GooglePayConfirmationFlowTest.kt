package com.stripe.android.paymentelement.confirmation.gpay

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.isInstanceOf
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationMediator
import com.stripe.android.paymentelement.confirmation.ConfirmationMediator.Parameters
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.asLaunch
import com.stripe.android.paymentelement.confirmation.runResultTest
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.utils.RecordingGooglePayPaymentMethodLauncherFactory
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.utils.DummyActivityResultCaller
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class GooglePayConfirmationFlowTest {
    @Test
    fun `on launch, should persist parameters & launch using launcher as expected`() = runTest {
        val googlePayPaymentMethodLauncher = mock<GooglePayPaymentMethodLauncher>()

        RecordingGooglePayPaymentMethodLauncherFactory.test(googlePayPaymentMethodLauncher) {
            DummyActivityResultCaller.test {
                val savedStateHandle = SavedStateHandle()
                val mediator = ConfirmationMediator(
                    savedStateHandle = savedStateHandle,
                    definition = GooglePayConfirmationDefinition(
                        googlePayPaymentMethodLauncherFactory = factory,
                        userFacingLogger = null,
                    ),
                )

                mediator.register(
                    activityResultCaller = activityResultCaller,
                    onResult = {}
                )

                assertThat(awaitRegisterCall()).isNotNull()
                assertThat(awaitNextRegisteredLauncher()).isNotNull()

                val action = mediator.action(
                    option = GOOGLE_PAY_CONFIRMATION_OPTION,
                    intent = PAYMENT_INTENT,
                )

                assertThat(action).isInstanceOf<ConfirmationMediator.Action.Launch>()

                val launchAction = action.asLaunch()

                launchAction.launch()

                assertThat(createGooglePayPaymentMethodLauncherCalls.awaitItem()).isNotNull()

                val parameters = savedStateHandle
                    .get<Parameters<GooglePayConfirmationOption>>("GooglePayParameters")

                assertThat(parameters?.confirmationOption).isEqualTo(GOOGLE_PAY_CONFIRMATION_OPTION)
                assertThat(parameters?.intent).isEqualTo(PAYMENT_INTENT)
                assertThat(parameters?.deferredIntentConfirmationType).isNull()

                verify(googlePayPaymentMethodLauncher, times(1)).present(
                    currencyCode = "usd",
                    amount = 1000L,
                    transactionId = "pi_12345",
                    label = null,
                )
            }
        }
    }

    @Test
    fun `on result, should return confirmation result as expected`() = runResultTest(
        confirmationOption = GOOGLE_PAY_CONFIRMATION_OPTION,
        intent = PAYMENT_INTENT,
        definition = GooglePayConfirmationDefinition(
            googlePayPaymentMethodLauncherFactory = RecordingGooglePayPaymentMethodLauncherFactory.noOp(mock()),
            userFacingLogger = null,
        ),
        launcherResult = GooglePayPaymentMethodLauncher.Result.Completed(PAYMENT_METHOD),
        definitionResult = ConfirmationDefinition.Result.NextStep(
            intent = PAYMENT_INTENT,
            confirmationOption = PaymentMethodConfirmationOption.Saved(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = "pi_123_secret_123",
                ),
                paymentMethod = PAYMENT_METHOD,
                optionsParams = null,
                shippingDetails = null,
            )
        )
    )

    private companion object {
        private val GOOGLE_PAY_CONFIRMATION_OPTION = GooglePayConfirmationOption(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123_secret_123",
            ),
            shippingDetails = null,
            config = GooglePayConfirmationOption.Config(
                environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                merchantName = "Test merchant Inc.",
                merchantCountryCode = "US",
                merchantCurrencyCode = "CA",
                customAmount = 1099,
                customLabel = null,
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                ),
                cardBrandFilter = DefaultCardBrandFilter,
            )
        )

        private val PAYMENT_METHOD = PaymentMethodFactory.card()

        private val PAYMENT_INTENT = PaymentIntentFactory.create()
    }
}
