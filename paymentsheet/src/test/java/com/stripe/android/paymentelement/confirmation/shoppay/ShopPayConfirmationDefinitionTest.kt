package com.stripe.android.paymentelement.confirmation.shoppay

import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.FakeConfirmationOption
import com.stripe.android.paymentelement.confirmation.asFailed
import com.stripe.android.paymentelement.confirmation.asLaunch
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.shoppay.ShopPayActivityResult
import com.stripe.android.shoppay.ShopPayLauncher
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.utils.DummyActivityResultCaller
import com.stripe.android.utils.RecordingShopPayLauncher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

internal class ShopPayConfirmationDefinitionTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

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
    fun `'createLauncher' should create launcher properly`() = test {
        val definition = createShopPayConfirmationDefinition(shopPayLauncher = launcherScenario.launcher)

        val activityResultCaller = DummyActivityResultCaller.noOp()
        val onResult: (ShopPayActivityResult) -> Unit = {}

        definition.createLauncher(
            activityResultCaller = activityResultCaller,
            onResult = onResult,
        )

        val registerCall = launcherScenario.registerCalls.awaitItem()

        assertThat(registerCall.activityResultCaller).isEqualTo(activityResultCaller)
        assertThat(registerCall.callback).isEqualTo(onResult)
    }

    @Test
    fun `'unregister' should unregister launcher properly`() = test {
        val definition = createShopPayConfirmationDefinition()

        definition.unregister(launcherScenario.launcher)

        assertThat(launcherScenario.unregisterCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `'action' should return expected 'Launch' action`() = test {
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
    fun `'launch' should launch properly with provided parameters`() = test {
        val definition = createShopPayConfirmationDefinition()

        definition.launch(
            confirmationOption = SHOP_PAY_CONFIRMATION_OPTION,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            launcher = launcherScenario.launcher,
            arguments = Unit,
        )

        val presentCall = launcherScenario.presentCalls.awaitItem()

        assertThat(presentCall.confirmationUrl).isEqualTo(SHOP_PAY_CONFIRMATION_OPTION.checkoutUrl)
    }

    @Test
    fun `'toResult' should return 'NextStep' when result is 'Completed'`() = test {
        verifyShopPayResultFails(ShopPayActivityResult.Completed("spm_123"))
    }

    @Test
    fun `'toResult' should return 'NextStep' when result is 'Canceled'`() = test {
        verifyShopPayResultFails(ShopPayActivityResult.Canceled)
    }

    @Test
    fun `'toResult' should return 'NextStep' when result is 'Failed'`() = test {
        verifyShopPayResultFails(ShopPayActivityResult.Failed(Throwable("ShopPay is not supported yet")))
    }

    private fun verifyShopPayResultFails(activityResult: ShopPayActivityResult) {
        val definition = createShopPayConfirmationDefinition()

        val result = definition.toResult(
            confirmationOption = SHOP_PAY_CONFIRMATION_OPTION,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            result = activityResult,
            deferredIntentConfirmationType = null,
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.Failed>()
        val failedResult = result.asFailed()
        assertThat(failedResult.cause.message).isEqualTo("ShopPay is not supported yet")
    }

    private fun createShopPayConfirmationDefinition(
        shopPayLauncher: ShopPayLauncher = mock()
    ): ShopPayConfirmationDefinition {
        return ShopPayConfirmationDefinition(shopPayLauncher, mock())
    }

    private fun test(test: suspend Scenario.() -> Unit) = runTest(
        StandardTestDispatcher()
    ) {
        RecordingShopPayLauncher.test {
            test(
                Scenario(
                    launcherScenario = this
                )
            )
        }
    }

    data class Scenario(
        val launcherScenario: RecordingShopPayLauncher.Scenario,
    )

    companion object {
        private const val CONFIRMATION_URL = "https://example.com"
        private val SHOP_PAY_CONFIRMATION_OPTION = ShopPayConfirmationOption(CONFIRMATION_URL)

        private val CONFIRMATION_PARAMETERS = ConfirmationDefinition.Parameters(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123"
            ),
            intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            appearance = PaymentSheet.Appearance(),
            shippingDetails = AddressDetails(name = "John Doe"),
        )
    }
}
