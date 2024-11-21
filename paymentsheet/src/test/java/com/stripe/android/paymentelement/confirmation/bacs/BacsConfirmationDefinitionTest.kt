package com.stripe.android.paymentelement.confirmation.bacs

import androidx.activity.result.ActivityResultCallback
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationOption
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.asCallbackFor
import com.stripe.android.paymentelement.confirmation.asCanceled
import com.stripe.android.paymentelement.confirmation.asFail
import com.stripe.android.paymentelement.confirmation.asLaunch
import com.stripe.android.paymentelement.confirmation.asNextStep
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationContract
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncherFactory
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationResult
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateData
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.FakeBacsMandateConfirmationLauncher
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.utils.DummyActivityResultCaller
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BacsConfirmationDefinitionTest {
    @Test
    fun `'key' should be 'Bacs`() {
        val definition = createBacsConfirmationDefinition()

        assertThat(definition.key).isEqualTo("Bacs")
    }

    @Test
    fun `'option' return casted 'BacsPaymentMethodConfirmationOption'`() {
        val definition = createBacsConfirmationDefinition()
        val confirmationOption = createBacsConfirmationOption()

        assertThat(definition.option(confirmationOption)).isEqualTo(confirmationOption)
    }

    @Test
    fun `'option' return null for unknown option`() {
        val definition = createBacsConfirmationDefinition()

        assertThat(definition.option(FakeConfirmationOption())).isNull()
    }

    @Test
    fun `'createLauncher' should call factory with registered activity launcher`() = runTest {
        val bacsMandateConfirmationLauncherFactory = FakeBacsMandateConfirmationLauncherFactory()
        val definition = createBacsConfirmationDefinition(
            bacsMandateConfirmationLauncherFactory = bacsMandateConfirmationLauncherFactory,
        )

        var onResultCalled = false
        val onResult: (BacsMandateConfirmationResult) -> Unit = { onResultCalled = true }
        DummyActivityResultCaller.test {
            definition.createLauncher(
                activityResultCaller = activityResultCaller,
                onResult = onResult,
            )

            val call = awaitRegisterCall()
            val registeredLauncher = awaitNextRegisteredLauncher()

            assertThat(call.contract).isInstanceOf<BacsMandateConfirmationContract>()
            assertThat(call.callback).isInstanceOf<ActivityResultCallback<BacsMandateConfirmationResult>>()

            val callback = call.callback.asCallbackFor<BacsMandateConfirmationResult>()

            callback.onActivityResult(BacsMandateConfirmationResult.Confirmed)

            assertThat(onResultCalled).isTrue()

            val factoryCall = bacsMandateConfirmationLauncherFactory.calls.awaitItem()

            assertThat(factoryCall.activityResultLauncher).isEqualTo(registeredLauncher)
        }
    }

    @Test
    fun `'toResult' should return 'NextStep' when 'BacsMandateConfirmationResult' is 'Confirmed'`() = runTest {
        val definition = createBacsConfirmationDefinition()

        val bacsConfirmationOption = createBacsConfirmationOption()

        val result = definition.toResult(
            confirmationOption = createBacsConfirmationOption(),
            intent = PAYMENT_INTENT,
            deferredIntentConfirmationType = null,
            result = BacsMandateConfirmationResult.Confirmed,
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Result.NextStep>()

        val successResult = result.asNextStep()

        assertThat(successResult.intent).isEqualTo(PAYMENT_INTENT)

        val confirmationOption = successResult.confirmationOption

        assertThat(confirmationOption).isInstanceOf<PaymentMethodConfirmationOption.New>()

        val newPaymentMethodOption = confirmationOption.asNewPaymentMethodOption()

        assertThat(newPaymentMethodOption.createParams).isEqualTo(bacsConfirmationOption.createParams)
        assertThat(newPaymentMethodOption.optionsParams).isEqualTo(bacsConfirmationOption.optionsParams)
        assertThat(newPaymentMethodOption.initializationMode).isEqualTo(bacsConfirmationOption.initializationMode)
        assertThat(newPaymentMethodOption.shippingDetails).isEqualTo(bacsConfirmationOption.shippingDetails)
        assertThat(newPaymentMethodOption.shouldSave).isFalse()
    }

    @Test
    fun `'toResult' should return 'Canceled' with no action when 'BacsMandateConfirmationResult' is 'Canceled'`() =
        runTest {
            val definition = createBacsConfirmationDefinition()

            val result = definition.toResult(
                confirmationOption = createBacsConfirmationOption(),
                intent = PAYMENT_INTENT,
                deferredIntentConfirmationType = null,
                result = BacsMandateConfirmationResult.Cancelled,
            )

            assertThat(result).isInstanceOf<ConfirmationDefinition.Result.Canceled>()

            val canceledResult = result.asCanceled()

            assertThat(canceledResult.action).isEqualTo(ConfirmationHandler.Result.Canceled.Action.None)
        }

    @Test
    fun `'toResult' should return 'Canceled' with modify action when 'BacsMandateConfirmationResult' is 'Canceled'`() =
        runTest {
            val definition = createBacsConfirmationDefinition()

            val result = definition.toResult(
                confirmationOption = createBacsConfirmationOption(),
                intent = PAYMENT_INTENT,
                deferredIntentConfirmationType = null,
                result = BacsMandateConfirmationResult.ModifyDetails,
            )

            assertThat(result).isInstanceOf<ConfirmationDefinition.Result.Canceled>()

            val canceledResult = result.asCanceled()

            assertThat(canceledResult.action)
                .isEqualTo(ConfirmationHandler.Result.Canceled.Action.ModifyPaymentDetails)
        }

    @Test
    fun `'Fail' action should be returned if name is missing in confirmation option`() = runTest {
        val definition = createBacsConfirmationDefinition()

        val action = definition.action(
            confirmationOption = createBacsConfirmationOption(
                name = null,
            ),
            intent = PAYMENT_INTENT,
        )

        assertThat(action).isInstanceOf<ConfirmationDefinition.Action.Fail<BacsMandateData>>()

        val failAction = action.asFail()

        assertThat(failAction.cause).isInstanceOf<IllegalArgumentException>()
        assertThat(failAction.cause.message).isEqualTo(
            "Given confirmation option does not have expected Bacs data!"
        )
        assertThat(failAction.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Internal)
    }

    @Test
    fun `'Fail' action should be returned if email is missing in confirmation option`() = runTest {
        val definition = createBacsConfirmationDefinition()

        val action = definition.action(
            confirmationOption = createBacsConfirmationOption(
                email = null,
            ),
            intent = PAYMENT_INTENT,
        )

        assertThat(action).isInstanceOf<ConfirmationDefinition.Action.Fail<BacsMandateData>>()

        val failAction = action.asFail()

        assertThat(failAction.cause).isInstanceOf<IllegalArgumentException>()
        assertThat(failAction.cause.message).isEqualTo(
            "Given confirmation option does not have expected Bacs data!"
        )
        assertThat(failAction.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Internal)
    }

    @Test
    fun `'Launch' action should be returned if option is converted to mandate data`() = runTest {
        val definition = createBacsConfirmationDefinition()

        val action = definition.action(
            confirmationOption = createBacsConfirmationOption(),
            intent = PAYMENT_INTENT,
        )

        assertThat(action).isInstanceOf<ConfirmationDefinition.Action.Launch<BacsMandateData>>()

        val launchAction = action.asLaunch()

        val mandateData = launchAction.launcherArguments

        assertThat(mandateData.name).isEqualTo("John Doe")
        assertThat(mandateData.email).isEqualTo("johndoe@email.com")
        assertThat(mandateData.sortCode).isEqualTo("108800")
        assertThat(mandateData.accountNumber).isEqualTo("00012345")

        assertThat(launchAction.deferredIntentConfirmationType).isNull()
        assertThat(launchAction.receivesResultInProcess).isTrue()
    }

    @Test
    fun `On 'launch', should use launcher to launch`() = runTest {
        val definition = createBacsConfirmationDefinition()

        val launcher = FakeBacsMandateConfirmationLauncher()

        val appearance = PaymentSheet.Appearance().copy(
            typography = PaymentSheet.Typography.default.copy(
                sizeScaleFactor = 2f
            )
        )
        val bacsMandateData = BacsMandateData(
            name = "John Doe",
            email = "johndoe@email.com",
            accountNumber = "00012345",
            sortCode = "108800",
        )

        definition.launch(
            confirmationOption = createBacsConfirmationOption().copy(appearance = appearance),
            intent = PAYMENT_INTENT,
            arguments = bacsMandateData,
            launcher = launcher,
        )

        val call = launcher.calls.awaitItem()

        assertThat(call.data).isEqualTo(bacsMandateData)
        assertThat(call.appearance).isEqualTo(appearance)
    }

    private fun createBacsConfirmationDefinition(
        bacsMandateConfirmationLauncherFactory: BacsMandateConfirmationLauncherFactory =
            FakeBacsMandateConfirmationLauncherFactory(),
    ): BacsConfirmationDefinition {
        return BacsConfirmationDefinition(
            bacsMandateConfirmationLauncherFactory = bacsMandateConfirmationLauncherFactory,
        )
    }

    private fun createBacsConfirmationOption(
        name: String? = "John Doe",
        email: String? = "johndoe@email.com",
    ): BacsConfirmationOption {
        return BacsConfirmationOption(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123_secret_123"
            ),
            createParams = PaymentMethodCreateParams.create(
                bacsDebit = PaymentMethodCreateParams.BacsDebit(
                    accountNumber = "00012345",
                    sortCode = "108800"
                ),
                billingDetails = PaymentMethod.BillingDetails(
                    name = name,
                    email = email,
                )
            ),
            optionsParams = null,
            shippingDetails = null,
            appearance = PaymentSheet.Appearance(),
        )
    }

    private fun ConfirmationHandler.Option.asNewPaymentMethodOption(): PaymentMethodConfirmationOption.New {
        return this as PaymentMethodConfirmationOption.New
    }

    private companion object {
        private val PAYMENT_INTENT = PaymentIntentFactory.create()
    }
}
