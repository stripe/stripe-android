package com.stripe.android.paymentelement.confirmation.gpay

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.Checkout
import com.stripe.android.checkout.CheckoutInstances
import com.stripe.android.checkout.CheckoutStateFactory
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.CONFIRMATION_PARAMETERS
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.utils.FakeUserFacingLogger
import com.stripe.android.paymentsheet.utils.RecordingGooglePayPaymentMethodLauncherFactory
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.utils.FakeActivityResultLauncher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
class GooglePayCheckoutSessionEmailCollectionTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()

    @get:Rule
    val paymentConfigRule = PaymentConfigurationTestRule(applicationContext)

    @After
    fun tearDown() {
        CheckoutInstances.clear()
    }

    @Test
    fun `launch requires email for checkout session when email collection is automatic and session email is missing`() =
        runTest {
            val billingConfig = billingDetailsConfig(
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
            )

            val metadata = createCheckoutSessionMetadata(
                billingConfig = billingConfig,
                customerEmail = null,
            )

            RecordingGooglePayPaymentMethodLauncherFactory.test(mock()) {
                val definition = createDefinition(factory)

                definition.launch(
                    launcher = FakeActivityResultLauncher(),
                    arguments = com.stripe.android.paymentelement.confirmation.EmptyConfirmationLauncherArgs,
                    confirmationOption = confirmationOption(billingConfig),
                    confirmationArgs = CONFIRMATION_PARAMETERS.copy(paymentMethodMetadata = metadata),
                )

                val call = createGooglePayPaymentMethodLauncherCalls.awaitItem()

                assertThat(call.config.isEmailRequired).isTrue()
            }
        }

    @Test
    fun `launch does not require email for checkout session when session email already exists`() = runTest {
        val billingConfig = billingDetailsConfig(
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
        )

        val metadata = createCheckoutSessionMetadata(
            billingConfig = billingConfig,
            customerEmail = "customer@example.com",
        )

        RecordingGooglePayPaymentMethodLauncherFactory.test(mock()) {
            val definition = createDefinition(factory)

            definition.launch(
                launcher = FakeActivityResultLauncher(),
                arguments = com.stripe.android.paymentelement.confirmation.EmptyConfirmationLauncherArgs,
                confirmationOption = confirmationOption(billingConfig),
                confirmationArgs = CONFIRMATION_PARAMETERS.copy(paymentMethodMetadata = metadata),
            )

            val call = createGooglePayPaymentMethodLauncherCalls.awaitItem()

            assertThat(call.config.isEmailRequired).isFalse()
        }
    }

    @Test
    fun `launch passes checkout session email as fallback billing details when session email already exists`() = runTest {
        val billingConfig = billingDetailsConfig(
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
        )

        val metadata = createCheckoutSessionMetadata(
            billingConfig = billingConfig,
            customerEmail = "customer@example.com",
        )

        val googlePayLauncher = mock<GooglePayPaymentMethodLauncher>()

        RecordingGooglePayPaymentMethodLauncherFactory.test(googlePayLauncher) {
            val definition = createDefinition(factory)

            definition.launch(
                launcher = FakeActivityResultLauncher(),
                arguments = com.stripe.android.paymentelement.confirmation.EmptyConfirmationLauncherArgs,
                confirmationOption = confirmationOption(billingConfig),
                confirmationArgs = CONFIRMATION_PARAMETERS.copy(paymentMethodMetadata = metadata),
            )

            createGooglePayPaymentMethodLauncherCalls.awaitItem()

            val billingDetailsCaptor = argumentCaptor<com.stripe.android.model.PaymentMethod.BillingDetails>()

            verify(googlePayLauncher).present(
                currencyCode = any(),
                amount = any(),
                transactionId = anyOrNull(),
                label = anyOrNull(),
                clientAttributionMetadata = anyOrNull(),
                isElements = any(),
                publishableKey = anyOrNull(),
                displayItems = any(),
                defaultBillingDetails = billingDetailsCaptor.capture(),
            )

            assertThat(billingDetailsCaptor.firstValue.email).isEqualTo("customer@example.com")
        }
    }

    @Test
    fun `launch does not require email when merchant disables email collection`() = runTest {
        val billingConfig = billingDetailsConfig(
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
        )

        val metadata = createCheckoutSessionMetadata(
            billingConfig = billingConfig,
            customerEmail = null,
        )

        RecordingGooglePayPaymentMethodLauncherFactory.test(mock()) {
            val definition = createDefinition(factory)

            definition.launch(
                launcher = FakeActivityResultLauncher(),
                arguments = com.stripe.android.paymentelement.confirmation.EmptyConfirmationLauncherArgs,
                confirmationOption = confirmationOption(billingConfig),
                confirmationArgs = CONFIRMATION_PARAMETERS.copy(paymentMethodMetadata = metadata),
            )

            val call = createGooglePayPaymentMethodLauncherCalls.awaitItem()

            assertThat(call.config.isEmailRequired).isFalse()
        }
    }

    private fun createCheckoutSessionMetadata(
        billingConfig: PaymentSheet.BillingDetailsCollectionConfiguration,
        customerEmail: String?,
    ) = PaymentMethodMetadataFactory.create(
        billingDetailsCollectionConfiguration = billingConfig,
        integrationMetadata = IntegrationMetadata.CheckoutSession(
            id = "cs_test_123",
            instancesKey = CheckoutStateFactory.DEFAULT_KEY,
        ),
    ).also {
        Checkout.createWithState(
            context = applicationContext,
            state = CheckoutStateFactory.create(
                key = CheckoutStateFactory.DEFAULT_KEY,
                checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                    customerEmail = customerEmail,
                ),
            ),
        )
    }

    private fun createDefinition(
        factory: com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory,
    ): GooglePayConfirmationDefinition {
        return GooglePayConfirmationDefinition(
            googlePayPaymentMethodLauncherFactory = factory,
            userFacingLogger = FakeUserFacingLogger(),
        )
    }

    private fun confirmationOption(
        billingConfig: PaymentSheet.BillingDetailsCollectionConfiguration,
    ) = GooglePayConfirmationOption(
        config = GooglePayConfirmationOption.Config(
            environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
            merchantName = "Test merchant Inc.",
            merchantCountryCode = "US",
            merchantCurrencyCode = "USD",
            customAmount = 1099L,
            customLabel = null,
            billingDetailsCollectionConfiguration = billingConfig,
            cardBrandFilter = com.stripe.android.DefaultCardBrandFilter,
            cardFundingFilter = com.stripe.android.DefaultCardFundingFilter,
        ),
    )

    private fun billingDetailsConfig(
        email: PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode,
    ) = PaymentSheet.BillingDetailsCollectionConfiguration(
        email = email,
        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
    )
}
