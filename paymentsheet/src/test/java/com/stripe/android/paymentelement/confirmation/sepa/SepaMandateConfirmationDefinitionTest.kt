package com.stripe.android.paymentelement.confirmation.sepa

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationOption
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.asCallbackFor
import com.stripe.android.paymentelement.confirmation.asCanceled
import com.stripe.android.paymentelement.confirmation.asLaunch
import com.stripe.android.paymentelement.confirmation.asNextStep
import com.stripe.android.paymentelement.confirmation.fakeLifecycleOwner
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.ui.SepaMandateContract
import com.stripe.android.paymentsheet.ui.SepaMandateResult
import com.stripe.android.testing.DummyActivityResultCaller
import com.stripe.android.utils.FakeActivityResultLauncher
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class SepaMandateConfirmationDefinitionTest {
    @Test
    fun `key orders mandate before other confirmation steps`() = runScenario {
        assertThat(definition.key).isEqualTo("AcknowledgeSepaMandate")
        assertThat(definition.key).isLessThan("Attestation")
        assertThat(definition.key).isLessThan("CvcRecollection")
        assertThat(definition.key).isLessThan("Intent")
    }

    @Test
    fun `option accepts saved payment method confirmation option`() = runScenario {
        assertThat(definition.option(confirmationOption)).isEqualTo(confirmationOption)
        assertThat(definition.option(FakeConfirmationOption())).isNull()
    }

    @Test
    fun `canConfirm returns true for unacknowledged saved SEPA`() = runScenario {
        assertThat(definition.canConfirm(confirmationOption, confirmationArgs)).isTrue()
    }

    @Test
    fun `canConfirm returns false when option is acknowledged`() = runScenario(
        acknowledged = true,
    ) {
        assertThat(definition.canConfirm(confirmationOption, confirmationArgs)).isFalse()
    }

    @Test
    fun `canConfirm returns false for a non-SEPA option`() {
        val option = PaymentMethodConfirmationOption.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            optionsParams = null,
            shippingInformation = null,
        )
        runScenario(confirmationOption = option) {
            assertThat(definition.canConfirm(confirmationOption, confirmationArgs)).isFalse()
        }
    }

    @Test
    fun `action launches in process with metadata merchant and appearance`() = runScenario {
        val action = definition.action(confirmationOption, confirmationArgs).asLaunch()

        assertThat(action.receivesResultInProcess).isTrue()
        assertThat(action.launcherArguments.merchantName)
            .isEqualTo(PaymentSheetFixtures.MERCHANT_DISPLAY_NAME)
        assertThat(action.launcherArguments.appearance)
            .isEqualTo(confirmationArgs.paymentMethodMetadata.appearance)
    }

    @Test
    fun `createLauncher registers SEPA mandate contract and callback`() = runScenario {
        DummyActivityResultCaller.test {
            val results = Turbine<SepaMandateResult>()
            val launcher = definition.createLauncher(
                activityResultCaller = activityResultCaller,
                lifecycleOwner = fakeLifecycleOwner(),
                onResult = results::add,
            )

            val registerCall = awaitRegisterCall()
            assertThat(launcher).isEqualTo(awaitNextRegisteredLauncher())
            assertThat(registerCall.contract).isInstanceOf<SepaMandateContract>()

            registerCall.callback.asCallbackFor<SepaMandateResult>()
                .onActivityResult(SepaMandateResult.Acknowledged)
            assertThat(results.awaitItem()).isEqualTo(SepaMandateResult.Acknowledged)
            results.ensureAllEventsConsumed()
        }
    }

    @Test
    fun `launch forwards launcher arguments`() = runScenario {
        val launcher = FakeActivityResultLauncher<SepaMandateContract.Args>()
        val args = definition.action(confirmationOption, confirmationArgs).asLaunch().launcherArguments

        definition.launch(launcher, args, confirmationOption, confirmationArgs)

        assertThat(launcher.calls.awaitItem().input).isEqualTo(args)
    }

    @Test
    fun `acknowledgement continues with acknowledged option without mutating original`() = runScenario {
        val result = definition.toResult(
            confirmationOption = confirmationOption,
            confirmationArgs = confirmationArgs,
            launcherArgs = mandateArgs,
            result = SepaMandateResult.Acknowledged,
        ).asNextStep()

        assertThat(confirmationOption.hasAcknowledgedSepaMandate).isFalse()
        assertThat(result.confirmationOption).isEqualTo(
            confirmationOption.copy(hasAcknowledgedSepaMandate = true)
        )
        assertThat(result.arguments).isEqualTo(confirmationArgs)
    }

    @Test
    fun `cancellation informs consumer`() = runScenario {
        val result = definition.toResult(
            confirmationOption = confirmationOption,
            confirmationArgs = confirmationArgs,
            launcherArgs = mandateArgs,
            result = SepaMandateResult.Canceled,
        ).asCanceled()

        assertThat(result.action).isEqualTo(ConfirmationHandler.Result.Canceled.Action.InformCancellation)
    }

    @Test
    fun `unregister unregisters launcher`() = runScenario {
        val launcher = FakeActivityResultLauncher<SepaMandateContract.Args>()

        definition.unregister(launcher)

        launcher.unregisterCalls.awaitItem()
    }

    private fun runScenario(
        acknowledged: Boolean = false,
        confirmationOption: PaymentMethodConfirmationOption.Saved = CONFIRMATION_OPTION.copy(
            hasAcknowledgedSepaMandate = acknowledged,
        ),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val confirmationArgs = ConfirmationHandler.Args(
            confirmationOption = confirmationOption,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            statusBarColor = null,
        )
        Scenario(
            definition = SepaMandateConfirmationDefinition(),
            confirmationOption = confirmationOption,
            confirmationArgs = confirmationArgs,
            mandateArgs = SepaMandateContract.Args(
                merchantName = confirmationArgs.paymentMethodMetadata.merchantName,
                appearance = confirmationArgs.paymentMethodMetadata.appearance,
            ),
        ).block()
    }

    private data class Scenario(
        val definition: SepaMandateConfirmationDefinition,
        val confirmationOption: PaymentMethodConfirmationOption.Saved,
        val confirmationArgs: ConfirmationHandler.Args,
        val mandateArgs: SepaMandateContract.Args,
    )

    private companion object {
        val CONFIRMATION_OPTION = PaymentMethodConfirmationOption.Saved(
            paymentMethod = PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD,
            optionsParams = null,
            shippingInformation = null,
        )
    }
}
