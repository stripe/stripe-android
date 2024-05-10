package com.stripe.android.paymentsheet

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.TestUtils
import com.stripe.android.utils.injectableActivityScenario
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class ExternalPaymentMethodActivityTest {

    // if the intent contains a type, we will call externalPaymentMEthodConfirmHAndler

    // if the intent contains a result and the activity is already running, then it finishes with result
    // if the intent contains a result and the activity is not already running, then it finishes with result

    private val errorReporter = FakeErrorReporter()
    private val contract = ExternalPaymentMethodContract(errorReporter)

    private var confirmedType: String? = null
    private var confirmedBillingDetails: PaymentMethod.BillingDetails? = null
    private val externalPaymentMethodActivityLauncher =

    @Before
    fun clearErrorReporter() {
        errorReporter.clear()
    }

    @Before
    fun clearConfirmationDetails() {
        confirmedType = null
        confirmedBillingDetails = null
    }

    @Test
    fun `ExternalPaymentMethodInterceptor starts EPM activity, calls confirm handler`() {
        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler = ExternalPaymentMethodInterceptorTest.DefaultExternalPaymentMethodConfirmHandler(
            confirmedType = ::confirmedType,
            confirmedBillingDetails = ::confirmedBillingDetails,
        )
        val expectedExternalPaymentMethodType = "external_fawry"
        val expectedBillingDetails = PaymentMethod.BillingDetails() // TODO: add content.
        ExternalPaymentMethodInterceptor.intercept(
            externalPaymentMethodType = expectedExternalPaymentMethodType,
            billingDetails = expectedBillingDetails,
            onPaymentResult = {},
            // TODO: how do I get the launcher in a test env?
            externalPaymentMethodLauncher = activityScenario().launchForResult(),
            errorReporter = errorReporter,
        )


    }

    @Test
    fun `ExternalPaymentMethodResultHandler finishes EPM activity when already running`() {

    }

    @Test
    fun `ExternalPaymentMethodResultHandler finishes EPM activity when not already running`() {

    }

    class DefaultExternalPaymentMethodConfirmHandler(
        private val setConfirmedType: (String?) -> Unit,
        private val setConfirmedBillingDetails: (PaymentMethod.BillingDetails?) -> Unit
    ) : ExternalPaymentMethodConfirmHandler {
        override fun confirmExternalPaymentMethod(
            externalPaymentMethodType: String,
            billingDetails: PaymentMethod.BillingDetails
        ) {
            setConfirmedType(externalPaymentMethodType)
            setConfirmedBillingDetails(billingDetails)
        }
    }

    class ExternalPaymentMethodActivityResultLauncher : ActivityResultLauncher<ExternalPaymentMethodInput> {
        override fun launch(input: ExternalPaymentMethodInput?, options: ActivityOptionsCompat?) {

            TODO("Not yet implemented")
        }

        override fun unregister() {
            TODO("Not yet implemented")
        }

        override fun getContract(): ActivityResultContract<ExternalPaymentMethodInput, *> {
            TODO("Not yet implemented")
        }

    }

    private fun activityScenario(): InjectableActivityScenario<ExternalPaymentMethodActivity> {
        return injectableActivityScenario {
            injectActivity {}
        }
    }
}