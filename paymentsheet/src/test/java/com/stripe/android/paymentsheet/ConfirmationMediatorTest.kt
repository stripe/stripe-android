package com.stripe.android.paymentsheet

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.confirmation.ConfirmationDefinition
import com.stripe.android.paymentsheet.confirmation.ConfirmationHandler
import com.stripe.android.paymentsheet.confirmation.ConfirmationMediator
import com.stripe.android.paymentsheet.confirmation.DeferredIntentConfirmationType
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.utils.FakeConfirmationDefinition
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.SetupIntentFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ConfirmationMediatorTest {
    @Test
    fun `On can confirm, should return true if definition is the same type`() = runTest {
        val mediator = ConfirmationMediator(
            savedStateHandle = SavedStateHandle(),
            definition = FakeConfirmationDefinition()
        )

        val canConfirm = mediator.canConfirm(
            confirmationOption = ConfirmationHandler.Option.PaymentMethod.Saved(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = "pi_123_secret_123",
                ),
                optionsParams = null,
                shippingDetails = null,
                paymentMethod = PaymentMethodFactory.card(),
            ),
        )

        assertThat(canConfirm).isTrue()
    }

    @Test
    fun `On can confirm, should return false if definition is not the same type`() = runTest {
        val mediator = ConfirmationMediator(
            savedStateHandle = SavedStateHandle(),
            definition = FakeConfirmationDefinition()
        )

        val canConfirm = mediator.canConfirm(
            confirmationOption = ConfirmationHandler.Option.ExternalPaymentMethod(
                type = "paypal",
                billingDetails = null,
            ),
        )

        assertThat(canConfirm).isFalse()
    }

    @Test
    fun `On register, should create launcher`() = runTest {
        val definition = FakeConfirmationDefinition()
        val mediator = ConfirmationMediator(
            savedStateHandle = SavedStateHandle(),
            definition = definition,
        )

        val activityResultCaller = mock<ActivityResultCaller>()

        mediator.register(
            activityResultCaller = activityResultCaller,
            onResult = {},
        )

        val createLauncherCall = definition.createLauncherCalls.awaitItem()

        assertThat(createLauncherCall.activityResultCaller).isEqualTo(activityResultCaller)
    }

    @Test
    fun `On incorrect confirmation option provided on action, should return fail action`() = runTest {
        val mediator = ConfirmationMediator(
            savedStateHandle = SavedStateHandle(),
            definition = FakeConfirmationDefinition()
        )

        val action = mediator.action(
            option = ConfirmationHandler.Option.ExternalPaymentMethod(
                type = "paypal",
                billingDetails = null,
            ),
            intent = PaymentIntentFixtures.PI_SUCCEEDED,
        )

        val failAction = action.asFail()

        assertThat(failAction.cause).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(failAction.cause.message).isEqualTo(
            "Parameter type of 'ExternalPaymentMethod' cannot be used with " +
                "PaymentConfirmationMediator to read a result"
        )
        assertThat(failAction.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Internal)
    }

    @Test
    fun `On complete confirmation action, should return mediator complete action`() = runTest {
        val definition = FakeConfirmationDefinition(
            onAction = { confirmationOption, intent ->
                ConfirmationDefinition.ConfirmationAction.Complete(
                    confirmationOption = confirmationOption,
                    intent = intent,
                    deferredIntentConfirmationType = DeferredIntentConfirmationType.Client,
                )
            }
        )

        val mediator = ConfirmationMediator(
            savedStateHandle = SavedStateHandle(),
            definition = definition
        )

        val action = mediator.action(
            option = SAVED_CONFIRMATION_OPTION,
            intent = INTENT,
        )

        val completeAction = action.asComplete()

        assertThat(completeAction.confirmationOption).isEqualTo(SAVED_CONFIRMATION_OPTION)
        assertThat(completeAction.intent).isEqualTo(INTENT)
        assertThat(completeAction.deferredIntentConfirmationType).isEqualTo(DeferredIntentConfirmationType.Client)
    }

    @Test
    fun `On failed confirmation action, should return mediator fail action`() = runTest {
        val exception = IllegalStateException("Failed!")
        val message = R.string.stripe_something_went_wrong.resolvableString
        val errorType = ConfirmationHandler.Result.Failed.ErrorType.Fatal

        val definition = FakeConfirmationDefinition(
            onAction = { _, _ ->
                ConfirmationDefinition.ConfirmationAction.Fail(
                    cause = exception,
                    message = R.string.stripe_something_went_wrong.resolvableString,
                    errorType = errorType,
                )
            }
        )

        val mediator = ConfirmationMediator(
            savedStateHandle = SavedStateHandle(),
            definition = definition
        )

        val action = mediator.action(
            option = SAVED_CONFIRMATION_OPTION,
            intent = INTENT,
        )

        val failAction = action.asFail()

        assertThat(failAction.cause).isInstanceOf(IllegalStateException::class.java)
        assertThat(failAction.cause.message).isEqualTo("Failed!")
        assertThat(failAction.message).isEqualTo(message)
        assertThat(failAction.errorType).isEqualTo(errorType)
    }

    @Test
    fun `On launch action, should call definition launch and persist parameters`() = runTest {
        val launcherArguments = FakeConfirmationDefinition.LauncherArgs(amount = 5000)
        val launcher = FakeConfirmationDefinition.Launcher()

        val definition = FakeConfirmationDefinition(
            onAction = { _, _ ->
                ConfirmationDefinition.ConfirmationAction.Launch(
                    launcherArguments = launcherArguments,
                    deferredIntentConfirmationType = DeferredIntentConfirmationType.Client,
                )
            },
            launcher = launcher,
        )

        val savedStateHandle = SavedStateHandle()

        val mediator = ConfirmationMediator(
            savedStateHandle = savedStateHandle,
            definition = definition
        ).apply {
            register(
                activityResultCaller = mock(),
                onResult = {}
            )
        }

        val action = mediator.action(
            option = SAVED_CONFIRMATION_OPTION,
            intent = INTENT,
        )

        val launchAction = action.asLaunch()

        launchAction.launch()

        val launchCall = definition.launchCalls.awaitItem()

        assertThat(launchCall.confirmationOption).isEqualTo(SAVED_CONFIRMATION_OPTION)
        assertThat(launchCall.arguments).isEqualTo(launcherArguments)
        assertThat(launchCall.intent).isEqualTo(INTENT)
        assertThat(launchCall.launcher).isEqualTo(launcher)

        val parameters = savedStateHandle
            .get<ConfirmationMediator.Parameters<ConfirmationHandler.Option.PaymentMethod.Saved>>(
                "TestParameters"
            )

        assertThat(parameters?.confirmationOption).isEqualTo(SAVED_CONFIRMATION_OPTION)
        assertThat(parameters?.intent).isEqualTo(INTENT)
        assertThat(parameters?.deferredIntentConfirmationType).isEqualTo(DeferredIntentConfirmationType.Client)
    }

    @Test
    fun `On confirmation action without registering, should return fail action`() = runTest {
        val definition = FakeConfirmationDefinition(
            onAction = { _, _ ->
                ConfirmationDefinition.ConfirmationAction.Launch(
                    launcherArguments = FakeConfirmationDefinition.LauncherArgs(amount = 5000),
                    deferredIntentConfirmationType = null,
                )
            },
        )

        val mediator = ConfirmationMediator(
            savedStateHandle = SavedStateHandle(),
            definition = definition
        )

        val action = mediator.action(
            option = SAVED_CONFIRMATION_OPTION,
            intent = INTENT,
        )

        val failAction = action.asFail()

        assertThat(failAction.cause).isInstanceOf(IllegalStateException::class.java)
        assertThat(failAction.cause.message).isEqualTo(
            "No launcher for FakePaymentConfirmationDefinition was found, did you call register?"
        )
        assertThat(failAction.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Fatal)
    }

    @Test
    fun `On confirmation action after un-registering, should return fail action`() = runTest {
        val definition = FakeConfirmationDefinition(
            onAction = { _, _ ->
                ConfirmationDefinition.ConfirmationAction.Launch(
                    launcherArguments = FakeConfirmationDefinition.LauncherArgs(amount = 5000),
                    deferredIntentConfirmationType = null,
                )
            },
        )

        val mediator = ConfirmationMediator(
            savedStateHandle = SavedStateHandle(),
            definition = definition
        )

        mediator.register(
            activityResultCaller = mock(),
            onResult = {}
        )
        mediator.unregister()

        val action = mediator.action(
            option = SAVED_CONFIRMATION_OPTION,
            intent = INTENT,
        )

        val failAction = action.asFail()

        assertThat(failAction.cause).isInstanceOf(IllegalStateException::class.java)
        assertThat(failAction.cause.message).isEqualTo(
            "No launcher for FakePaymentConfirmationDefinition was found, did you call register?"
        )
        assertThat(failAction.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Fatal)
    }

    @Test
    fun `On result, should attempt to convert launcher result to confirmation result and return it`() = runTest {
        val waitForResultLatch = CountDownLatch(1)

        val intent = SetupIntentFactory.create(
            paymentMethod = PaymentMethodFactory.card(random = true)
        )
        val deferredIntentConfirmationType = DeferredIntentConfirmationType.Client
        val launcherResult = FakeConfirmationDefinition.LauncherResult(amount = 50)
        val confirmationResult = ConfirmationHandler.Result.Succeeded(
            intent = intent,
            deferredIntentConfirmationType = deferredIntentConfirmationType,
        )

        val definition = FakeConfirmationDefinition(
            confirmationResult = confirmationResult,
            onAction = { _, _ ->
                ConfirmationDefinition.ConfirmationAction.Launch(
                    launcherArguments = FakeConfirmationDefinition.LauncherArgs(amount = 5000),
                    deferredIntentConfirmationType = DeferredIntentConfirmationType.Client,
                )
            },
        )

        val mediator = ConfirmationMediator(
            savedStateHandle = SavedStateHandle(),
            definition = definition,
        )

        val confirmationOption = ConfirmationHandler.Option.PaymentMethod.Saved(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123_secret_123",
            ),
            optionsParams = null,
            shippingDetails = null,
            paymentMethod = PaymentMethodFactory.card(),
        )

        var receivedResult: ConfirmationHandler.Result? = null

        mediator.register(
            activityResultCaller = mock(),
            onResult = { result ->
                receivedResult = result

                waitForResultLatch.countDown()
            },
        )

        val createLauncherCall = definition.createLauncherCalls.awaitItem()

        val action = mediator.action(
            intent = intent,
            option = confirmationOption,
        )

        assertThat(action).isInstanceOf<ConfirmationMediator.Action.Launch>()

        val launchAction = action.asLaunch()

        launchAction.launch()

        createLauncherCall.onResult(launcherResult)

        waitForResultLatch.await(2, TimeUnit.SECONDS)

        val toPaymentConfirmationResultCall = definition.toPaymentConfirmationResultCalls.awaitItem()

        assertThat(toPaymentConfirmationResultCall.confirmationOption).isEqualTo(confirmationOption)
        assertThat(toPaymentConfirmationResultCall.intent).isEqualTo(intent)
        assertThat(toPaymentConfirmationResultCall.result).isEqualTo(launcherResult)
        assertThat(toPaymentConfirmationResultCall.deferredIntentConfirmationType)
            .isEqualTo(deferredIntentConfirmationType)

        assertThat(receivedResult).isEqualTo(confirmationResult)
    }

    @Test
    fun `On result with no persisted parameters, should return failed result`() = runTest {
        val countDownLatch = CountDownLatch(1)
        val definition = FakeConfirmationDefinition()
        val mediator = ConfirmationMediator(
            savedStateHandle = SavedStateHandle(),
            definition = definition,
        )

        val activityResultCaller = mock<ActivityResultCaller>()

        mediator.register(
            activityResultCaller = activityResultCaller,
            onResult = { result ->
                assertThat(result).isInstanceOf<ConfirmationHandler.Result.Failed>()

                val failAction = result.asFailed()

                assertThat(failAction.cause).isInstanceOf(IllegalStateException::class.java)
                assertThat(failAction.cause.message).isEqualTo(
                    "Arguments should have been initialized before handling result!"
                )
                assertThat(failAction.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
                assertThat(failAction.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Internal)

                countDownLatch.countDown()
            },
        )

        val createLauncherCall = definition.createLauncherCalls.awaitItem()

        createLauncherCall.onResult(
            FakeConfirmationDefinition.LauncherResult(amount = 50)
        )

        countDownLatch.await(2, TimeUnit.SECONDS)
    }

    private fun ConfirmationHandler.Result.asFailed(): ConfirmationHandler.Result.Failed {
        return this as ConfirmationHandler.Result.Failed
    }

    private fun ConfirmationMediator.Action.asFail(): ConfirmationMediator.Action.Fail {
        return this as ConfirmationMediator.Action.Fail
    }

    private fun ConfirmationMediator.Action.asComplete(): ConfirmationMediator.Action.Complete {
        return this as ConfirmationMediator.Action.Complete
    }

    private fun ConfirmationMediator.Action.asLaunch(): ConfirmationMediator.Action.Launch {
        return this as ConfirmationMediator.Action.Launch
    }

    private companion object {
        private val SAVED_CONFIRMATION_OPTION = ConfirmationHandler.Option.PaymentMethod.Saved(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(clientSecret = "pi_123"),
            shippingDetails = null,
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            optionsParams = null,
        )

        private val INTENT = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
    }
}
