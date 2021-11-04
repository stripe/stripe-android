package com.stripe.android

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.Source
import com.stripe.android.model.SourceFixtures
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.utils.TestUtils.idleLooper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import org.mockito.kotlin.isA
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class StripePaymentAuthTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val paymentController: PaymentController = mock()
    private val paymentCallback: ApiResultCallback<PaymentIntentResult> = mock()
    private val setupCallback: ApiResultCallback<SetupIntentResult> = mock()
    private val sourceCallback: ApiResultCallback<Source> = mock()

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun onPaymentResult_whenShouldHandleResultAndControllerReturnsCorrectResult_shouldGetCorrectResult() =
        testDispatcher.runBlockingTest {
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
            ).thenReturn(result)

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
    fun onPaymentResult_whenShouldHandleResultAndControllerReturnsNull_shouldThrowException() =
        testDispatcher.runBlockingTest {
            val data = Intent()
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
            ).thenReturn(null)

            val stripe = createStripe()
            stripe.onPaymentResult(
                StripePaymentController.PAYMENT_REQUEST_CODE,
                data,
                callback = paymentCallback
            )

            idleLooper()

            verify(paymentCallback).onError(isA<InvalidRequestException>())
        }

    @Test
    fun onSetupResult_whenShouldHandleResultAndControllerReturnsCorrectResult_shouldGetCorrectResult() =
        testDispatcher.runBlockingTest {
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
            ).thenReturn(result)

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
    fun onSetupResult_whenShouldHandleResultAndControllerReturnsNull_shouldThrowException() =
        testDispatcher.runBlockingTest {
            val data = Intent()
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
            ).thenReturn(null)

            val stripe = createStripe()
            stripe.onSetupResult(
                StripePaymentController.SETUP_REQUEST_CODE,
                data,
                callback = setupCallback
            )

            idleLooper()

            verify(setupCallback).onError(isA<InvalidRequestException>())
        }

    @Test
    fun onAuthenticateSourceResult_whenControllerReturnsCorrectResult_shouldGetCorrectResult() =
        testDispatcher.runBlockingTest {
            val source = SourceFixtures.CARD
            val data = Intent()
            whenever(
                paymentController.getAuthenticateSourceResult(
                    data
                )
            ).thenReturn(source)

            val stripe = createStripe()
            stripe.onAuthenticateSourceResult(
                data,
                callback = sourceCallback
            )

            idleLooper()

            verify(sourceCallback).onSuccess(source)
        }

    @Test
    fun onAuthenticateSourceResult_whenControllerReturnsNull_shouldThrowException() =
        testDispatcher.runBlockingTest {
            val data = Intent()
            whenever(
                paymentController.getAuthenticateSourceResult(
                    data
                )
            ).thenReturn(null)

            val stripe = createStripe()
            stripe.onAuthenticateSourceResult(
                data,
                callback = sourceCallback
            )

            idleLooper()

            verify(sourceCallback).onError(isA<InvalidRequestException>())
        }

    private fun createStripe(): Stripe {
        return Stripe(
            StripeApiRepository(
                ApplicationProvider.getApplicationContext(),
                { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
//                stripeApiRequestExecutor = DefaultApiRequestExecutor(
//                    workContext = testDispatcher
//                ),
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
