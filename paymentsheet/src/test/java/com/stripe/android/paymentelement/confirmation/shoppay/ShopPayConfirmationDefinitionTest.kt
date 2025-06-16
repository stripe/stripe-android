package com.stripe.android.paymentelement.confirmation.shoppay

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.SHOP_PAY_CONFIGURATION
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationOption
import com.stripe.android.paymentelement.confirmation.asFailed
import com.stripe.android.paymentelement.confirmation.asLaunch
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.shoppay.ShopPayActivityResult
import com.stripe.android.shoppay.ShopPayLauncher
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.utils.DummyActivityResultCaller
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

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
    fun `'createLauncher' should return shopPayLauncher and register it properly`() {
        val mockShopPayLauncher = mock<ShopPayLauncher>()
        val definition = createShopPayConfirmationDefinition(shopPayLauncher = mockShopPayLauncher)

        val activityResultCaller = DummyActivityResultCaller.noOp()
        val onResult: (ShopPayActivityResult) -> Unit = {}

        val launcher = definition.createLauncher(
            activityResultCaller = activityResultCaller,
            onResult = onResult,
        )

        assertThat(launcher).isEqualTo(mockShopPayLauncher)
        verify(mockShopPayLauncher).register(activityResultCaller, onResult)
    }

    @Test
    fun `'unregister' should unregister launcher properly`() {
        val mockShopPayLauncher = mock<ShopPayLauncher>()
        val definition = createShopPayConfirmationDefinition(shopPayLauncher = mockShopPayLauncher)

        definition.unregister(mockShopPayLauncher)

        verify(mockShopPayLauncher).unregister()
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
    fun `'launch' should launch properly with provided parameters`() {
        val mockShopPayLauncher = mock<ShopPayLauncher>()
        val definition = createShopPayConfirmationDefinition(shopPayLauncher = mockShopPayLauncher)

        definition.launch(
            confirmationOption = SHOP_PAY_CONFIRMATION_OPTION,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            launcher = mockShopPayLauncher,
            arguments = Unit,
        )

        verify(mockShopPayLauncher).present(SHOP_PAY_CONFIRMATION_OPTION.shopPayConfiguration)
    }

    @Test
    fun `'toResult' should return 'Failed' when result is 'Completed'`() {
        val definition = createShopPayConfirmationDefinition()

        val result = definition.toResult(
            confirmationOption = SHOP_PAY_CONFIRMATION_OPTION,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            deferredIntentConfirmationType = null,
            result = ShopPayActivityResult.Completed("pm_test_123"),
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.Failed>()

        val failedResult = result.asFailed()

        assertThat(failedResult.cause).isInstanceOf<Throwable>()
        assertThat(failedResult.cause.message).isEqualTo("ShopPay is not supported yet")
        assertThat(failedResult.message).isEqualTo("ShopPay is not supported yet".resolvableString)
        assertThat(failedResult.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
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

        assertThat(failedResult.cause).isInstanceOf<Throwable>()
        assertThat(failedResult.cause.message).isEqualTo("ShopPay is not supported yet")
        assertThat(failedResult.message).isEqualTo("ShopPay is not supported yet".resolvableString)
        assertThat(failedResult.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
    }

    @Test
    fun `'toResult' should return 'Failed' when result is 'Canceled'`() {
        val definition = createShopPayConfirmationDefinition()

        val result = definition.toResult(
            confirmationOption = SHOP_PAY_CONFIRMATION_OPTION,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            deferredIntentConfirmationType = null,
            result = ShopPayActivityResult.Canceled,
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.Failed>()

        val failedResult = result.asFailed()

        assertThat(failedResult.cause).isInstanceOf<Throwable>()
        assertThat(failedResult.cause.message).isEqualTo("ShopPay is not supported yet")
        assertThat(failedResult.message).isEqualTo("ShopPay is not supported yet".resolvableString)
        assertThat(failedResult.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
    }

    private fun createShopPayConfirmationDefinition(
        shopPayLauncher: ShopPayLauncher = mock()
    ): ShopPayConfirmationDefinition {
        return ShopPayConfirmationDefinition(
            shopPayLauncher = shopPayLauncher
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
        )
    }
}
