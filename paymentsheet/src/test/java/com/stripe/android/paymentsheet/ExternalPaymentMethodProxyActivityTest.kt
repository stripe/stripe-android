package com.stripe.android.paymentsheet

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.testing.FakeErrorReporter
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.fail

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ExternalPaymentMethodProxyActivityTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `ExternalPaymentMethodInterceptor calls confirm handler`() {
        val confirmHandler = DefaultExternalPaymentMethodConfirmHandler()
        val activityLauncher = ExternalPaymentMethodActivityResultLauncher(
            context,
        )
        val expectedExternalPaymentMethodType = "external_fawry"
        val expectedBillingDetails =
            PaymentMethod.BillingDetails(name = "Joe", address = Address(city = "Seattle", line1 = "123 Main St"))

        PaymentElementCallbackReferences["ExternalPaymentMethod"] = PaymentElementCallbacks(
            createIntentCallback = null,
            externalPaymentMethodConfirmHandler = confirmHandler,
        )

        activityLauncher.launch(
            input = ExternalPaymentMethodInput(
                instanceId = "ExternalPaymentMethod",
                type = expectedExternalPaymentMethodType,
                billingDetails = expectedBillingDetails,
            )
        )

        assertThat(confirmHandler.confirmedType).isEqualTo(expectedExternalPaymentMethodType)
        assertThat(confirmHandler.confirmedBillingDetails).isEqualTo(expectedBillingDetails)
        runCatching {
            activityLauncher.scenario?.result
        }.onSuccess {
            fail(message = "Activity has finished, but it should not have finished yet!")
        }
    }

    @Test
    fun `Confirm is not called again when activity is recreated`() {
        val confirmHandler = DefaultExternalPaymentMethodConfirmHandler()
        val activityLauncher = ExternalPaymentMethodActivityResultLauncher(
            context,
        )

        PaymentElementCallbackReferences["ExternalPaymentMethod"] = PaymentElementCallbacks(
            createIntentCallback = null,
            externalPaymentMethodConfirmHandler = confirmHandler,
        )

        activityLauncher.launch(
            input = ExternalPaymentMethodInput(
                instanceId = "ExternalPaymentMethod",
                type = "external_fawry",
                billingDetails = PaymentMethod.BillingDetails(),
            )
        )

        assertThat(confirmHandler.callCount).isEqualTo(1)

        activityLauncher.scenario?.recreate()

        assertThat(confirmHandler.callCount).isEqualTo(1)
    }

    @Test
    fun `ExternalPaymentMethodResultHandler finishes EPM activity with correct result on completed`() {
        val scenario = ActivityScenario.launchActivityForResult<ExternalPaymentMethodProxyActivity>(
            ExternalPaymentMethodResultHandler.createResultIntent(context, ExternalPaymentMethodResult.completed())
        )

        assertThat(scenario.result.resultCode).isEqualTo(ExternalPaymentMethodResult.Completed.RESULT_CODE)
    }

    @Test
    fun `ExternalPaymentMethodResultHandler finishes EPM activity with correct result on canceled`() {
        val scenario = ActivityScenario.launchActivityForResult<ExternalPaymentMethodProxyActivity>(
            ExternalPaymentMethodResultHandler.createResultIntent(context, ExternalPaymentMethodResult.canceled())
        )

        assertThat(scenario.result.resultCode).isEqualTo(ExternalPaymentMethodResult.Canceled.RESULT_CODE)
    }

    @Test
    fun `ExternalPaymentMethodResultHandler finishes EPM activity with correct result on failed`() {
        val expectedDisplayMessage = "error!"
        val scenario = ActivityScenario.launchActivityForResult<ExternalPaymentMethodProxyActivity>(
            ExternalPaymentMethodResultHandler.createResultIntent(
                context,
                ExternalPaymentMethodResult.failed(displayMessage = expectedDisplayMessage)
            )
        )

        assertThat(scenario.result.resultCode).isEqualTo(ExternalPaymentMethodResult.Failed.RESULT_CODE)
        assertThat(
            scenario.result.resultData.getStringExtra(ExternalPaymentMethodResult.Failed.DISPLAY_MESSAGE_EXTRA)
        ).isEqualTo(expectedDisplayMessage)
    }

    @Test
    fun `Start ExternalPaymentMethodProxyActivity with unexpected intent and it immediately finishes`() {
        val scenario = ActivityScenario.launchActivityForResult<ExternalPaymentMethodProxyActivity>(
            Intent().setClass(context, ExternalPaymentMethodProxyActivity::class.java)
        )

        assertThat(scenario.result).isNotNull()
    }

    @Test
    fun `On separate instances, should be able to fetch proper callback`() {
        val firstConfirmHandler = DefaultExternalPaymentMethodConfirmHandler()
        val secondConfirmHandler = DefaultExternalPaymentMethodConfirmHandler()

        val activityLauncher = ExternalPaymentMethodActivityResultLauncher(
            context,
        )

        PaymentElementCallbackReferences["ExternalPaymentMethodOne"] = PaymentElementCallbacks(
            createIntentCallback = null,
            externalPaymentMethodConfirmHandler = firstConfirmHandler,
        )

        PaymentElementCallbackReferences["ExternalPaymentMethodTwo"] = PaymentElementCallbacks(
            createIntentCallback = null,
            externalPaymentMethodConfirmHandler = secondConfirmHandler,
        )

        activityLauncher.launch(
            input = ExternalPaymentMethodInput(
                instanceId = "ExternalPaymentMethodOne",
                type = "external_paypal",
                billingDetails = PaymentMethod.BillingDetails(
                    email = "email@email.com",
                ),
            ),
        )

        activityLauncher.launch(
            input = ExternalPaymentMethodInput(
                instanceId = "ExternalPaymentMethodTwo",
                type = "external_fawry",
                billingDetails = PaymentMethod.BillingDetails(
                    email = "email2@email.com",
                ),
            ),
        )

        assertThat(firstConfirmHandler.callCount).isEqualTo(1)
        assertThat(firstConfirmHandler.confirmedType).isEqualTo("external_paypal")
        assertThat(firstConfirmHandler.confirmedBillingDetails).isEqualTo(
            PaymentMethod.BillingDetails(
                email = "email@email.com",
            ),
        )

        assertThat(secondConfirmHandler.callCount).isEqualTo(1)
        assertThat(secondConfirmHandler.confirmedType).isEqualTo("external_fawry")
        assertThat(secondConfirmHandler.confirmedBillingDetails).isEqualTo(
            PaymentMethod.BillingDetails(
                email = "email2@email.com",
            ),
        )
    }

    class DefaultExternalPaymentMethodConfirmHandler : ExternalPaymentMethodConfirmHandler {

        var confirmedType: String? = null
        var confirmedBillingDetails: PaymentMethod.BillingDetails? = null
        var callCount: Int = 0

        override fun confirmExternalPaymentMethod(
            externalPaymentMethodType: String,
            billingDetails: PaymentMethod.BillingDetails
        ) {
            this.callCount += 1
            this.confirmedType = externalPaymentMethodType
            this.confirmedBillingDetails = billingDetails
        }
    }

    internal class ExternalPaymentMethodActivityResultLauncher(
        private val context: Context,
    ) : ActivityResultLauncher<ExternalPaymentMethodInput>() {
        var scenario: ActivityScenario<ExternalPaymentMethodProxyActivity>? = null

        override fun launch(input: ExternalPaymentMethodInput, options: ActivityOptionsCompat?) {
            val contract = ExternalPaymentMethodContract(errorReporter = FakeErrorReporter())
            val scenario: ActivityScenario<ExternalPaymentMethodProxyActivity> =
                ActivityScenario.launchActivityForResult(
                    contract.createIntent(context, input)
                )

            this.scenario = scenario
        }

        override fun unregister() {
            TODO("Not yet implemented")
        }

        override val contract: ActivityResultContract<ExternalPaymentMethodInput, *>
            get() = TODO("Not yet implemented")
    }
}
