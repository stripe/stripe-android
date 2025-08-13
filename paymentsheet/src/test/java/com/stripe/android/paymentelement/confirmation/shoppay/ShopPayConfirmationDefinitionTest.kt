package com.stripe.android.paymentelement.confirmation.shoppay

import com.google.common.truth.Truth.assertThat
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.common.model.SHOP_PAY_CONFIGURATION
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationOption
import com.stripe.android.paymentelement.confirmation.asCallbackFor
import com.stripe.android.paymentelement.confirmation.asCanceled
import com.stripe.android.paymentelement.confirmation.asFailed
import com.stripe.android.paymentelement.confirmation.asLaunch
import com.stripe.android.paymentelement.confirmation.asSucceeded
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.shoppay.ShopPayActivityContract
import com.stripe.android.shoppay.ShopPayActivityResult
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.utils.DummyActivityResultCaller
import com.stripe.android.utils.FakeActivityResultLauncher
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class ShopPayConfirmationDefinitionTest {

    @Test
    fun `'key' should be 'ShopPay'`() {
        val definition = createShopPayConfirmationDefinition()

        assertThat(definition.key).isEqualTo("ShopPay")
    }

    @Test
    fun `'option' return casted 'ShopPayConfirmationOption'`() {
        val definition = createShopPayConfirmationDefinition()

        assertThat(definition.option(SHOP_PAY_CONFIRMATION_OPTION)).isEqualTo(SHOP_PAY_CONFIRMATION_OPTION)
    }

    @Test
    fun `'option' return null for unknown option`() {
        val definition = createShopPayConfirmationDefinition()

        assertThat(definition.option(FakeConfirmationOption())).isNull()
    }

    @Test
    fun `'createLauncher' should register launcher properly for activity result`() = runTest {
        val definition = createShopPayConfirmationDefinition()

        var onResultCalled = false
        val onResult: (ShopPayActivityResult) -> Unit = { onResultCalled = true }

        DummyActivityResultCaller.test {
            definition.createLauncher(
                activityResultCaller = activityResultCaller,
                onResult = onResult,
            )

            val registerCall = awaitRegisterCall()

            assertThat(awaitNextRegisteredLauncher()).isNotNull()

            assertThat(registerCall.contract).isInstanceOf<ShopPayActivityContract>()

            val callback = registerCall.callback.asCallbackFor<ShopPayActivityResult>()

            callback.onActivityResult(ShopPayActivityResult.Completed)

            assertThat(onResultCalled).isTrue()
        }
    }

    @Test
    fun `'action' should return expected 'Launch' action`() = runTest {
        val definition = createShopPayConfirmationDefinition()

        val action = definition.action(
            confirmationOption = SHOP_PAY_CONFIRMATION_OPTION,
            confirmationParameters = CONFIRMATION_PARAMETERS,
        )

        assertThat(action).isInstanceOf<ConfirmationDefinition.Action.Launch<Unit>>()

        val launchAction = action.asLaunch()

        assertThat(launchAction.launcherArguments).isEqualTo(Unit)
        assertThat(launchAction.receivesResultInProcess).isFalse()
        assertThat(launchAction.deferredIntentConfirmationType).isNull()
    }

    @Test
    fun `'launch' should launch properly with provided parameters`() = runTest {
        val definition = createShopPayConfirmationDefinition()

        val launcher = FakeActivityResultLauncher<ShopPayActivityContract.Args>()

        definition.launch(
            confirmationOption = SHOP_PAY_CONFIRMATION_OPTION,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            launcher = launcher,
            arguments = Unit,
        )

        val launchCall = launcher.calls.awaitItem()

        assertThat(launchCall.input.shopPayConfiguration).isEqualTo(SHOP_PAY_CONFIRMATION_OPTION.shopPayConfiguration)
        assertThat(launchCall.input.customerSessionClientSecret)
            .isEqualTo(SHOP_PAY_CONFIRMATION_OPTION.customerSessionClientSecret)
        assertThat(launchCall.input.businessName).isEqualTo(SHOP_PAY_CONFIRMATION_OPTION.merchantDisplayName)
    }

    @OptIn(SharedPaymentTokenSessionPreview::class)
    @Test
    fun `'launch' should launch properly with merchant name as seller`() = runTest {
        val definition = createShopPayConfirmationDefinition()

        val launcher = FakeActivityResultLauncher<ShopPayActivityContract.Args>()

        definition.launch(
            confirmationOption = SHOP_PAY_CONFIRMATION_OPTION,
            confirmationParameters = CONFIRMATION_PARAMETERS.copy(
                initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        sharedPaymentTokenSessionWithMode = PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = 5000,
                            currency = "CAD",
                        ),
                        sellerDetails = PaymentSheet.IntentConfiguration.SellerDetails(
                            businessName = "My business, Inc.",
                            networkId = "network_123",
                            externalId = "external_123"
                        )
                    )
                )
            ),
            launcher = launcher,
            arguments = Unit,
        )

        val launchCall = launcher.calls.awaitItem()

        assertThat(launchCall.input.businessName).isEqualTo("My business, Inc.")
    }

    @Test
    fun `'toResult' should return 'Succeeded' when result is 'Completed'`() {
        val definition = createShopPayConfirmationDefinition()

        val result = definition.toResult(
            confirmationOption = SHOP_PAY_CONFIRMATION_OPTION,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            deferredIntentConfirmationType = null,
            result = ShopPayActivityResult.Completed,
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.Succeeded>()

        val succeededResult = result.asSucceeded()

        assertThat(succeededResult.intent).isEqualTo(CONFIRMATION_PARAMETERS.intent)
        assertThat(succeededResult.deferredIntentConfirmationType).isNull()
        assertThat(succeededResult.completedFullPaymentFlow).isFalse()
    }

    @Test
    fun `'toResult' should return 'Failed' when result is 'Failed'`() {
        val definition = createShopPayConfirmationDefinition()

        val exception = IllegalStateException("ShopPay failed!")
        val result = definition.toResult(
            confirmationOption = SHOP_PAY_CONFIRMATION_OPTION,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            deferredIntentConfirmationType = null,
            result = ShopPayActivityResult.Failed(exception),
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.Failed>()

        val failedResult = result.asFailed()

        assertThat(failedResult.cause).isEqualTo(exception)
        assertThat(failedResult.message).isEqualTo(exception.message.orEmpty().resolvableString)
        assertThat(failedResult.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
    }

    @Test
    fun `'toResult' should return 'Canceled' when result is 'Canceled'`() {
        val definition = createShopPayConfirmationDefinition()

        val result = definition.toResult(
            confirmationOption = SHOP_PAY_CONFIRMATION_OPTION,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            deferredIntentConfirmationType = null,
            result = ShopPayActivityResult.Canceled,
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.Canceled>()

        val canceledResult = result.asCanceled()

        assertThat(canceledResult.action).isEqualTo(ConfirmationHandler.Result.Canceled.Action.None)
    }

    @Test
    fun `'toResult' should return 'Failed' when result is 'Failed' with null message`() {
        val definition = createShopPayConfirmationDefinition()

        val exception = Exception("oops")
        val result = definition.toResult(
            confirmationOption = SHOP_PAY_CONFIRMATION_OPTION,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            deferredIntentConfirmationType = null,
            result = ShopPayActivityResult.Failed(exception),
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.Failed>()

        val failedResult = result.asFailed()

        assertThat(failedResult.cause).isEqualTo(exception)
        assertThat(failedResult.message).isEqualTo("oops".resolvableString)
        assertThat(failedResult.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
    }

    private fun createShopPayConfirmationDefinition(): ShopPayConfirmationDefinition {
        return ShopPayConfirmationDefinition(
            shopPayActivityContract = ShopPayActivityContract(
                paymentElementCallbackIdentifier = "paymentElementCallbackIdentifier"
            )
        )
    }

    private companion object {
        private val PAYMENT_INTENT = PaymentIntentFactory.create()

        private val CONFIRMATION_PARAMETERS = ConfirmationDefinition.Parameters(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123_secret_123",
            ),
            intent = PAYMENT_INTENT,
            appearance = PaymentSheet.Appearance(),
            shippingDetails = AddressDetails(),
        )

        private val SHOP_PAY_CONFIRMATION_OPTION = ShopPayConfirmationOption(
            shopPayConfiguration = SHOP_PAY_CONFIGURATION,
            customerSessionClientSecret = "customer_secret",
            merchantDisplayName = "Example Inc.",
        )
    }
}
