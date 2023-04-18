package com.stripe.android

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.Source
import com.stripe.android.model.SourceFixtures
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.utils.TestUtils.idleLooper
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class StripePaymentAuthTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private val paymentController: PaymentController = mock()
    private val paymentCallback: ApiResultCallback<PaymentIntentResult> = mock()
    private val setupCallback: ApiResultCallback<SetupIntentResult> = mock()
    private val sourceCallback: ApiResultCallback<Source> = mock()

    @Test
    fun onPaymentResult_whenShouldHandleResultAndControllerReturnsCorrectResult_shouldGetCorrectResult() =
        runTest {
            val data = Intent()
            val result = PaymentIntentResult(PaymentIntentFixtures.PI_SUCCEEDED)
            whenever(
                paymentController.shouldHandlePaymentResult(
                    StripePaymentController.PAYMENT_REQUEST_CODE,
                    data
                )
            ).thenReturn(true)
            whenever(
                paymentController.getPaymentIntentResult(
                    data
                )
            ).thenReturn(Result.success(result))

            val stripe = createStripe()
            stripe.onPaymentResult(
                StripePaymentController.PAYMENT_REQUEST_CODE,
                data,
                callback = paymentCallback
            )

            idleLooper()

            verify(paymentCallback).onSuccess(result)
        }

    @Test
    fun onPaymentResult_whenShouldHandleResultAndControllerReturnsFailure_shouldThrowException() =
        runTest {
            val data = Intent()
            val exception = APIException()

            whenever(
                paymentController.shouldHandlePaymentResult(
                    StripePaymentController.PAYMENT_REQUEST_CODE,
                    data
                )
            ).thenReturn(true)

            whenever(
                paymentController.getPaymentIntentResult(
                    data
                )
            ).thenReturn(Result.failure(exception))

            val stripe = createStripe()
            stripe.onPaymentResult(
                StripePaymentController.PAYMENT_REQUEST_CODE,
                data,
                callback = paymentCallback
            )

            idleLooper()

            verify(paymentCallback).onError(eq(exception))
        }

    @Test
    fun onSetupResult_whenShouldHandleResultAndControllerReturnsCorrectResult_shouldGetCorrectResult() =
        runTest {
            val data = Intent()
            val result = SetupIntentResult(SetupIntentFixtures.SI_SUCCEEDED)
            whenever(
                paymentController.shouldHandleSetupResult(
                    StripePaymentController.SETUP_REQUEST_CODE,
                    data
                )
            ).thenReturn(true)
            whenever(
                paymentController.getSetupIntentResult(
                    data
                )
            ).thenReturn(Result.success(result))

            val stripe = createStripe()
            stripe.onSetupResult(
                StripePaymentController.SETUP_REQUEST_CODE,
                data,
                callback = setupCallback
            )

            idleLooper()
            verify(setupCallback).onSuccess(result)
        }

    @Test
    fun onSetupResult_whenShouldHandleResultAndControllerReturnsFailure_shouldThrowException() =
        runTest {
            val data = Intent()
            val exception = APIException()

            whenever(
                paymentController.shouldHandleSetupResult(
                    StripePaymentController.SETUP_REQUEST_CODE,
                    data
                )
            ).thenReturn(true)

            whenever(
                paymentController.getSetupIntentResult(
                    data
                )
            ).thenReturn(Result.failure(exception))

            val stripe = createStripe()
            stripe.onSetupResult(
                StripePaymentController.SETUP_REQUEST_CODE,
                data,
                callback = setupCallback
            )

            idleLooper()

            verify(setupCallback).onError(eq(exception))
        }

    @Test
    fun onAuthenticateSourceResult_whenControllerReturnsCorrectResult_shouldGetCorrectResult() =
        runTest {
            val source = SourceFixtures.CARD
            val data = Intent()
            whenever(
                paymentController.getAuthenticateSourceResult(
                    data
                )
            ).thenReturn(Result.success(source))

            val stripe = createStripe()
            stripe.onAuthenticateSourceResult(
                data,
                callback = sourceCallback
            )

            idleLooper()

            verify(sourceCallback).onSuccess(source)
        }

    @Test
    fun onAuthenticateSourceResult_whenControllerReturnsFailure_shouldThrowException() =
        runTest {
            val data = Intent()
            val exception = APIException()

            whenever(
                paymentController.getAuthenticateSourceResult(
                    data
                )
            ).thenReturn(Result.failure(exception))

            val stripe = createStripe()
            stripe.onAuthenticateSourceResult(
                data,
                callback = sourceCallback
            )

            idleLooper()

            verify(sourceCallback).onError(eq(exception))
        }

    private fun createStripe(): Stripe {
        return Stripe(
            StripeApiRepository(
                ApplicationProvider.getApplicationContext(),
                { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
                stripeNetworkClient = DefaultStripeNetworkClient(
                    workContext = testDispatcher
                ),
                analyticsRequestExecutor = {}
            ),
            paymentController,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            null,
            testDispatcher
        )
    }
}
