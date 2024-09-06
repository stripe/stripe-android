package com.stripe.android.customersheet

import android.app.Application
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import com.google.common.truth.Truth.assertThat
import com.stripe.android.R
import com.stripe.android.model.CardBrand
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.CountDownLatch
import kotlin.test.Test

@OptIn(ExperimentalCustomerSheetApi::class)
@RunWith(RobolectricTestRunner::class)
class CustomerSheetTest {
    @Before
    fun setup() {
        val appContext = ApplicationProvider.getApplicationContext<Application>()

        val activityInfo = ActivityInfo().apply {
            name = TestActivity::class.java.name
            packageName = appContext.packageName
            theme = R.style.StripePaymentSheetDefaultTheme
        }

        shadowOf(appContext.packageManager).addOrUpdateActivity(activityInfo)
    }

    @Test
    fun `When presenting without configuring, should return error`() = runTestActivityTest { activity, complete ->
        val customerSheet = CustomerSheet.create(
            activity = activity,
            customerAdapter = FakeCustomerAdapter(),
            callback = { result ->
                val failedResult = result.asFailed()

                assertThat(failedResult.exception).isInstanceOf(IllegalStateException::class.java)
                assertThat(failedResult.exception.message).isEqualTo(
                    "Must call `configure` first before attempting to present `CustomerSheet`!"
                )

                complete()
            },
        )

        customerSheet.present()
    }

    @Test
    fun `When presenting, should launch 'CustomerSheetActivity'`() = runTestActivityTest { activity, complete ->
        Intents.init()

        val customerSheet = CustomerSheet.create(
            activity = activity,
            customerAdapter = FakeCustomerAdapter(),
            callback = {},
        )

        val configuration = CustomerSheet.Configuration.builder(merchantDisplayName = "Merchant, Inc.")
            .googlePayEnabled(googlePayConfiguration = true)
            .preferredNetworks(preferredNetworks = listOf(CardBrand.CartesBancaires))
            .build()

        customerSheet.configure(
            configuration = configuration,
        )

        customerSheet.present()

        intended(hasComponent(CustomerSheetActivity::class.java.name))
        intended(
            hasExtra(
                "args",
                CustomerSheetContract.Args(
                    configuration = configuration,
                    statusBarColor = 0
                )
            )
        )

        Intents.release()

        complete()
    }

    @Test
    fun `When retrieving a payment option without configuring, should return error`() = runPaymentOptionTest(
        configuration = null,
    ) { result ->
        val failedResult = result.asFailed()

        assertThat(failedResult.exception).isInstanceOf(IllegalStateException::class.java)
        assertThat(failedResult.exception.message).isEqualTo(
            "Must call `configure` first before attempting to fetch the saved payment option!"
        )
    }

    @Test
    fun `When Google payment option, should return option is config enables Google Pay`() = runPaymentOptionTest(
        paymentOption = CustomerAdapter.PaymentOption.GooglePay,
        configuration = CustomerSheet.Configuration.builder(merchantDisplayName = "Merchant, Inc.")
            .googlePayEnabled(googlePayConfiguration = true)
            .build(),
    ) { result ->
        val selectedResult = result.asSelected()

        assertThat(selectedResult.selection).isInstanceOf(PaymentOptionSelection.GooglePay::class.java)
    }

    @Test
    fun `When Google payment option, should not return option is config disables Google Pay`() = runPaymentOptionTest(
        paymentOption = CustomerAdapter.PaymentOption.GooglePay,
        configuration = CustomerSheet.Configuration.builder(merchantDisplayName = "Merchant, Inc.")
            .googlePayEnabled(googlePayConfiguration = false)
            .build(),
    ) { result ->
        val selectedResult = result.asSelected()

        assertThat(selectedResult.selection).isNull()
    }

    @Test
    fun `When Google payment option, should not return option is config has not Google Pay config`() =
        runPaymentOptionTest(
            paymentOption = CustomerAdapter.PaymentOption.GooglePay,
            configuration = CustomerSheet.Configuration.builder(merchantDisplayName = "Merchant, Inc.")
                .build(),
        ) { result ->
            val selectedResult = result.asSelected()

            assertThat(selectedResult.selection).isNull()
        }

    private fun runPaymentOptionTest(
        configuration: CustomerSheet.Configuration?,
        paymentOption: CustomerAdapter.PaymentOption? = null,
        test: (result: CustomerSheetResult) -> Unit,
    ) {
        runTestActivityTest { activity, complete ->
            val customerSheet = CustomerSheet.create(
                activity = activity,
                customerAdapter = FakeCustomerAdapter(
                    selectedPaymentOption = CustomerAdapter.Result.success(paymentOption),
                ),
                callback = {},
            )

            configuration?.let {
                customerSheet.configure(configuration)
            }

            runBlocking {
                test(customerSheet.retrievePaymentOptionSelection())

                complete()
            }
        }
    }

    private fun runTestActivityTest(
        test: (TestActivity, () -> Unit) -> Unit,
    ) {
        ActivityScenario.launch(TestActivity::class.java).use { scenario ->
            val countDownLatch = CountDownLatch(1)

            scenario.onActivity {
                test(it) {
                    countDownLatch.countDown()
                }
            }

            countDownLatch.await()
        }
    }

    private fun CustomerSheetResult.asSelected(): CustomerSheetResult.Selected {
        return this as CustomerSheetResult.Selected
    }

    private fun CustomerSheetResult.asFailed(): CustomerSheetResult.Failed {
        return this as CustomerSheetResult.Failed
    }

    private class TestActivity : AppCompatActivity()
}
