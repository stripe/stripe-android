package com.stripe.android.paymentelement.confirmation.gpay

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.InternalGooglePayPaymentMethodLauncher
import com.stripe.android.isInstanceOf
import com.stripe.android.paymentelement.confirmation.CONFIRMATION_PARAMETERS
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationMediator
import com.stripe.android.paymentelement.confirmation.ConfirmationMediator.Parameters
import com.stripe.android.paymentelement.confirmation.EmptyConfirmationLauncherArgs
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.asLaunch
import com.stripe.android.paymentelement.confirmation.fakeLifecycleOwner
import com.stripe.android.paymentelement.confirmation.runResultTest
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.utils.RecordingInternalGooglePayPaymentMethodLauncherFactory
import com.stripe.android.testing.DummyActivityResultCaller
import com.stripe.android.testing.PaymentMethodFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GooglePayConfirmationFlowTest {
    @Test
    fun `on launch, should persist parameters & launch using launcher as expected`() = runTest {
        val internalGooglePayPaymentMethodLauncher = mock<InternalGooglePayPaymentMethodLauncher>()

        RecordingInternalGooglePayPaymentMethodLauncherFactory.test(internalGooglePayPaymentMethodLauncher) {
            DummyActivityResultCaller.test {
                val savedStateHandle = SavedStateHandle()
                val mediator = ConfirmationMediator(
                    savedStateHandle = savedStateHandle,
                    definition = GooglePayConfirmationDefinition(
                        instanceId = "instanceId",
                        context = ApplicationProvider.getApplicationContext<Context>(),
                        googlePayPaymentMethodLauncherFactory = factory,
                        userFacingLogger = null,
                    ),
                )

                mediator.register(
                    activityResultCaller = activityResultCaller,
                    lifecycleOwner = fakeLifecycleOwner(),
                    onResult = {}
                )

                assertThat(awaitRegisterCall()).isNotNull()

                val activityResultLauncher = awaitNextRegisteredLauncher()

                assertThat(activityResultLauncher).isNotNull()
                assertThat(createGooglePayPaymentMethodLauncherCalls.awaitItem().activityResultLauncher)
                    .isEqualTo(activityResultLauncher)

                val action = mediator.action(
                    option = GOOGLE_PAY_CONFIRMATION_OPTION,
                    arguments = CONFIRMATION_PARAMETERS,
                )

                assertThat(action).isInstanceOf<ConfirmationMediator.Action.Launch>()

                val launchAction = action.asLaunch()

                launchAction.launch()

                val parameters = savedStateHandle
                    .get<Parameters<GooglePayConfirmationOption, EmptyConfirmationLauncherArgs>>("GooglePayParameters")

                assertThat(parameters?.confirmationOption).isEqualTo(GOOGLE_PAY_CONFIRMATION_OPTION)
                assertThat(parameters?.confirmationArgs).isEqualTo(CONFIRMATION_PARAMETERS)

                verify(internalGooglePayPaymentMethodLauncher, times(1)).present(
                    currencyCode = "usd",
                    amount = 1000L,
                    config = EXPECTED_LAUNCHER_CONFIG,
                    cardBrandFilter = DefaultCardBrandFilter,
                    cardFundingFilter = DefaultCardFundingFilter,
                    clientAttributionMetadata = CONFIRMATION_PARAMETERS.paymentMethodMetadata.clientAttributionMetadata,
                    transactionId = "pi_12345",
                    label = null,
                    isElements = true,
                    publishableKey = null,
                    displayItems = emptyList(),
                    billingEmailOverride = null,
                    shippingAddressParameters = null,
                )
            }
        }
    }

    @Test
    fun `on result, should return confirmation result as expected`() = runResultTest(
        confirmationOption = GOOGLE_PAY_CONFIRMATION_OPTION,
        parameters = CONFIRMATION_PARAMETERS,
        definition = GooglePayConfirmationDefinition(
            instanceId = "instanceId",
            context = ApplicationProvider.getApplicationContext<Context>(),
            googlePayPaymentMethodLauncherFactory =
                RecordingInternalGooglePayPaymentMethodLauncherFactory.noOp(mock()),
            userFacingLogger = null,
        ),
        launcherResult = GooglePayPaymentMethodLauncher.Result.Completed(PAYMENT_METHOD),
        launcherArgs = EmptyConfirmationLauncherArgs,
        definitionResult = ConfirmationDefinition.Result.NextStep(
            confirmationOption = PaymentMethodConfirmationOption.Saved(
                shippingInformation = null,
                paymentMethod = PAYMENT_METHOD,
                optionsParams = null,
                originatedFromWallet = true,
            ),
            arguments = CONFIRMATION_PARAMETERS,
        )
    )

    private companion object {
        private val GOOGLE_PAY_CONFIRMATION_OPTION = GooglePayConfirmationOption(
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
                cardFundingFilter = DefaultCardFundingFilter,
            ),
        )

        private val EXPECTED_LAUNCHER_CONFIG = GooglePayPaymentMethodLauncher.Config(
            environment = GooglePayEnvironment.Test,
            merchantCountryCode = "US",
            merchantName = "Test merchant Inc.",
            isEmailRequired = false,
            billingAddressConfig = GooglePayPaymentMethodLauncher.BillingAddressConfig(
                isRequired = true,
                format = GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Full,
                isPhoneNumberRequired = false,
            ),
            existingPaymentMethodRequired = true,
            additionalEnabledNetworks = emptyList(),
        )

        private val PAYMENT_METHOD = PaymentMethodFactory.card()
    }
}
