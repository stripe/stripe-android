package com.stripe.android.paymentelement.confirmation.cvc

import androidx.activity.result.ActivityResultCallback
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationOption
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.asCallbackFor
import com.stripe.android.paymentelement.confirmation.asCanceled
import com.stripe.android.paymentelement.confirmation.asLaunch
import com.stripe.android.paymentelement.confirmation.asNextStep
import com.stripe.android.paymentelement.confirmation.asSaved
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.cvcrecollection.CvcRecollectionHandler
import com.stripe.android.paymentsheet.cvcrecollection.FakeCvcRecollectionHandler
import com.stripe.android.paymentsheet.cvcrecollection.RecordingCvcRecollectionLauncher
import com.stripe.android.paymentsheet.cvcrecollection.RecordingCvcRecollectionLauncherFactory
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionContract
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionLauncherFactory
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionResult
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.utils.DummyActivityResultCaller
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CvcRecollectionConfirmationDefinitionTest {
    @Test
    fun `'key' should be 'CvcRecollection'`() = test {
        assertThat(definition.key).isEqualTo("CvcRecollection")
    }

    @Test
    fun `'option' return casted 'Saved' variant of 'PaymentMethodConfirmationOption'`() = test {
        val confirmationOption = createSavedConfirmationOption()

        assertThat(definition.option(confirmationOption)).isEqualTo(confirmationOption)
    }

    @Test
    fun `'option' return null for unknown option`() = test {
        assertThat(definition.option(FakeConfirmationOption())).isNull()
    }

    @Test
    fun `'canConfirm' returns 'true' if CVC recollection is required`() = test(
        handler = FakeCvcRecollectionHandler().apply {
            requiresCVCRecollection = true
        }
    ) {
        assertThat(
            definition.canConfirm(
                confirmationOption = createSavedConfirmationOption(),
                confirmationParameters = CONFIRMATION_PARAMETERS,
            )
        ).isTrue()
    }

    @Test
    fun `'canConfirm' returns 'false' if CVC recollection is not required`() = test(
        handler = FakeCvcRecollectionHandler().apply {
            requiresCVCRecollection = false
        }
    ) {
        assertThat(
            definition.canConfirm(
                confirmationOption = createSavedConfirmationOption(),
                confirmationParameters = CONFIRMATION_PARAMETERS,
            )
        ).isFalse()
    }

    @Test
    fun `'canConfirm' returns 'false' if CVC is already recollected`() = test(
        handler = FakeCvcRecollectionHandler().apply {
            requiresCVCRecollection = true
        }
    ) {
        assertThat(
            definition.canConfirm(
                confirmationOption = createSavedConfirmationOption(
                    optionsParams = PaymentMethodOptionsParams.Card(cvc = "444")
                ),
                confirmationParameters = CONFIRMATION_PARAMETERS,
            )
        ).isFalse()
    }

    @Test
    fun `'createLauncher' should register launcher properly for activity result`() = test {
        var onResultCalled = false
        val onResult: (CvcRecollectionResult) -> Unit = { onResultCalled = true }

        DummyActivityResultCaller.test {
            definition.createLauncher(
                activityResultCaller = activityResultCaller,
                onResult = onResult,
            )

            val registerCall = awaitRegisterCall()

            assertThat(awaitNextRegisteredLauncher()).isNotNull()

            assertThat(registerCall.contract).isInstanceOf<CvcRecollectionContract>()
            assertThat(registerCall.callback).isInstanceOf<ActivityResultCallback<*>>()

            assertThat(factoryScenario.awaitCreateCall()).isNotNull()

            val callback = registerCall.callback.asCallbackFor<CvcRecollectionResult>()

            callback.onActivityResult(CvcRecollectionResult.Confirmed(cvc = "444"))

            assertThat(onResultCalled).isTrue()
        }
    }

    @Test
    fun `'action' should return launch action`() = test {
        val action = definition.action(
            confirmationOption = createSavedConfirmationOption(),
            confirmationParameters = CONFIRMATION_PARAMETERS,
        )

        assertThat(action).isInstanceOf<ConfirmationDefinition.Action.Launch<Unit>>()

        val launchAction = action.asLaunch()

        assertThat(launchAction.launcherArguments).isEqualTo(Unit)
        assertThat(launchAction.receivesResultInProcess).isTrue()
        assertThat(launchAction.deferredIntentConfirmationType).isNull()
    }

    @Test
    fun `'launch' should launch using launcher`() = test {
        val option = createSavedConfirmationOption()

        definition.launch(
            launcher = launcherScenario.launcher,
            arguments = Unit,
            confirmationOption = option,
            confirmationParameters = CONFIRMATION_PARAMETERS,
        )

        val launchCall = launcherScenario.awaitLaunchCall()

        assertThat(launchCall.appearance).isEqualTo(CONFIRMATION_PARAMETERS.appearance)
        assertThat(launchCall.data.brand).isEqualTo(option.paymentMethod.card?.brand)
        assertThat(launchCall.data.lastFour).isEqualTo(option.paymentMethod.card?.last4)
        assertThat(launchCall.isLiveMode).isEqualTo(CONFIRMATION_PARAMETERS.intent.isLiveMode)
    }

    @Test
    fun `'toResult' should return 'NexStep' when CVC is confirmed`() = test {
        val option = createSavedConfirmationOption()

        val result = definition.toResult(
            confirmationOption = option,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            result = CvcRecollectionResult.Confirmed(
                cvc = "444",
            ),
            deferredIntentConfirmationType = null,
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.NextStep>()

        val nextStepResult = result.asNextStep()
        val nextConfirmationOption = nextStepResult.confirmationOption

        assertThat(nextConfirmationOption).isInstanceOf<PaymentMethodConfirmationOption.Saved>()

        val savedOption = nextConfirmationOption.asSaved()

        assertThat(savedOption.paymentMethod).isEqualTo(option.paymentMethod)
        assertThat(savedOption.optionsParams).isInstanceOf<PaymentMethodOptionsParams.Card>()

        val cardParams = savedOption.optionsParams?.asCardParams()

        assertThat(cardParams?.cvc).isEqualTo("444")
    }

    @Test
    fun `'toResult' should return 'NexStep' with copied card options parameters`() = test {
        val option = createSavedConfirmationOption(
            optionsParams = PaymentMethodOptionsParams.Card(
                network = "cartes_bancaries"
            )
        )

        val result = definition.toResult(
            confirmationOption = option,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            result = CvcRecollectionResult.Confirmed(
                cvc = "555",
            ),
            deferredIntentConfirmationType = null,
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.NextStep>()

        val nextStepResult = result.asNextStep()
        val nextConfirmationOption = nextStepResult.confirmationOption

        assertThat(nextConfirmationOption).isInstanceOf<PaymentMethodConfirmationOption.Saved>()

        val savedOption = nextConfirmationOption.asSaved()

        assertThat(savedOption.paymentMethod).isEqualTo(option.paymentMethod)
        assertThat(savedOption.optionsParams).isInstanceOf<PaymentMethodOptionsParams.Card>()

        val cardParams = savedOption.optionsParams?.asCardParams()

        assertThat(cardParams?.network).isEqualTo("cartes_bancaries")
        assertThat(cardParams?.cvc).isEqualTo("555")
    }

    @Test
    fun `'toResult' should return 'Canceled' when CVC recollection is canceled`() = test {
        val option = createSavedConfirmationOption()

        val result = definition.toResult(
            confirmationOption = option,
            confirmationParameters = CONFIRMATION_PARAMETERS,
            result = CvcRecollectionResult.Cancelled,
            deferredIntentConfirmationType = null,
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.Canceled>()

        val canceledResult = result.asCanceled()

        assertThat(canceledResult.action).isEqualTo(ConfirmationHandler.Result.Canceled.Action.InformCancellation)
    }

    private fun test(
        handler: CvcRecollectionHandler = FakeCvcRecollectionHandler(),
        test: suspend Scenario.() -> Unit,
    ) = runTest {
        RecordingCvcRecollectionLauncher.test {
            val launcherScenario = this

            RecordingCvcRecollectionLauncherFactory.test(launcher) {
                val factoryScenario = this
                val definition = createCvcRecollectionConfirmationDefinition(
                    cvcRecollectionHandler = handler,
                    cvcRecollectionLauncherFactory = factory,
                )

                test(
                    Scenario(
                        definition = definition,
                        launcherScenario = launcherScenario,
                        factoryScenario = factoryScenario,
                    )
                )
            }
        }
    }

    private fun createCvcRecollectionConfirmationDefinition(
        cvcRecollectionLauncherFactory: CvcRecollectionLauncherFactory =
            RecordingCvcRecollectionLauncherFactory.noOp(),
        cvcRecollectionHandler: CvcRecollectionHandler = FakeCvcRecollectionHandler()
    ): CvcRecollectionConfirmationDefinition {
        return CvcRecollectionConfirmationDefinition(
            factory = cvcRecollectionLauncherFactory,
            handler = cvcRecollectionHandler,
        )
    }

    private fun createSavedConfirmationOption(
        optionsParams: PaymentMethodOptionsParams? = null
    ): PaymentMethodConfirmationOption.Saved {
        return PaymentMethodConfirmationOption.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            optionsParams = optionsParams,
        )
    }

    private data class Scenario(
        val definition: CvcRecollectionConfirmationDefinition,
        val launcherScenario: RecordingCvcRecollectionLauncher.Scenario,
        val factoryScenario: RecordingCvcRecollectionLauncherFactory.Scenario,
    )

    private fun PaymentMethodOptionsParams.asCardParams(): PaymentMethodOptionsParams.Card {
        return this as PaymentMethodOptionsParams.Card
    }

    companion object {
        private val CONFIRMATION_PARAMETERS = ConfirmationDefinition.Parameters(
            intent = PaymentIntentFactory.create(),
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123_secret_123",
            ),
            appearance = PaymentSheet.Appearance().copy(
                colorsDark = PaymentSheet.Colors.defaultLight,
            ),
            shippingDetails = null,
        )
    }
}
