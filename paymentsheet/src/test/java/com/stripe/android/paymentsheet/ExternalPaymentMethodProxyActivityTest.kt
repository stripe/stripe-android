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
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.utils.TestUtils.idleLooper
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
        var confirmedType: String? = null
        var confirmedBillingDetails: PaymentMethod.BillingDetails? = null
        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler =
            DefaultExternalPaymentMethodConfirmHandler(
                setConfirmedType = { confirmedType = it },
                setConfirmedBillingDetails = { confirmedBillingDetails = it }
            )
        val expectedExternalPaymentMethodType = "external_fawry"
        val expectedBillingDetails =
            PaymentMethod.BillingDetails(name = "Joe", address = Address(city = "Seattle", line1 = "123 Main St"))
        var scenario: ActivityScenario<ExternalPaymentMethodProxyActivity>? = null

        ExternalPaymentMethodInterceptor.intercept(
            externalPaymentMethodType = expectedExternalPaymentMethodType,
            billingDetails = expectedBillingDetails,
            onPaymentResult = {},
            externalPaymentMethodLauncher = ExternalPaymentMethodActivityResultLauncher(
                context,
                setScenario = { scenario = it }
            ),
            errorReporter = FakeErrorReporter(),
        )

        idleLooper()

        assertThat(confirmedType).isEqualTo(expectedExternalPaymentMethodType)
        assertThat(confirmedBillingDetails).isEqualTo(expectedBillingDetails)
        runCatching {
            scenario?.result
        }.onSuccess {
            fail(message = "Activity has finished, but it should not have finished yet!")
        }
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

    class DefaultExternalPaymentMethodConfirmHandler(
        private val setConfirmedType: (String?) -> Unit,
        private val setConfirmedBillingDetails: (PaymentMethod.BillingDetails?) -> Unit,
    ) : ExternalPaymentMethodConfirmHandler {
        override fun confirmExternalPaymentMethod(
            externalPaymentMethodType: String,
            billingDetails: PaymentMethod.BillingDetails
        ) {
            setConfirmedType(externalPaymentMethodType)
            setConfirmedBillingDetails(billingDetails)
        }
    }

    internal class ExternalPaymentMethodActivityResultLauncher(
        private val context: Context,
        private val setScenario: (ActivityScenario<ExternalPaymentMethodProxyActivity>) -> Unit,
    ) : ActivityResultLauncher<ExternalPaymentMethodInput>() {
        override fun launch(input: ExternalPaymentMethodInput?, options: ActivityOptionsCompat?) {
            val contract = ExternalPaymentMethodContract(errorReporter = FakeErrorReporter())
            val scenario: ActivityScenario<ExternalPaymentMethodProxyActivity> =
                ActivityScenario.launchActivityForResult(
                    contract.createIntent(context, input!!)
                )

            setScenario(scenario)
        }

        override fun unregister() {
            TODO("Not yet implemented")
        }

        override fun getContract(): ActivityResultContract<ExternalPaymentMethodInput, *> {
            TODO("Not yet implemented")
        }
    }
}
