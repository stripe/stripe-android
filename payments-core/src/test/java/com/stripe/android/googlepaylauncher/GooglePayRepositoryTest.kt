package com.stripe.android.googlepaylauncher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wallet.PaymentsClient
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.testing.FakeErrorReporter
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class GooglePayRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        PaymentConfiguration.init(context, "pk_123")
    }

    @Test
    fun `when google pay is ready, 'isReady' should return true`() = runTest {
        val paymentsClient = mock<PaymentsClient>()

        whenever(paymentsClient.isReadyToPay(any())) doReturn Tasks.forResult(true)

        val repository = createGooglePayRepository(paymentsClient)

        repository.isReady().test {
            assertEquals(true, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when google pay is not ready, 'isReady' should return false`() = runTest {
        val paymentsClient = mock<PaymentsClient>()

        whenever(paymentsClient.isReadyToPay(any())) doReturn Tasks.forResult(false)

        val repository = createGooglePayRepository(paymentsClient)

        repository.isReady().test {
            assertEquals(false, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when google pay is ready request fails, 'isReady' should be false & error should be reported`() = runTest {
        val paymentsClient = mock<PaymentsClient>()
        val errorReporter = FakeErrorReporter()

        whenever(paymentsClient.isReadyToPay(any())) doReturn Tasks.forException(
            ApiException(Status.RESULT_INTERNAL_ERROR)
        )

        val repository = createGooglePayRepository(paymentsClient, errorReporter)

        repository.isReady().test {
            assertEquals(false, awaitItem())

            assertEquals(
                ErrorReporter.ExpectedErrorEvent.GOOGLE_PAY_IS_READY_API_CALL.eventName,
                errorReporter.getLoggedErrors().first(),
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createGooglePayRepository(
        paymentsClient: PaymentsClient,
        errorReporter: ErrorReporter = FakeErrorReporter()
    ): DefaultGooglePayRepository {
        return DefaultGooglePayRepository(
            context = context,
            environment = GooglePayEnvironment.Test,
            billingAddressParameters = GooglePayJsonFactory.BillingAddressParameters(),
            existingPaymentMethodRequired = true,
            allowCreditCards = true,
            paymentsClientFactory = { paymentsClient },
            errorReporter = errorReporter,
            logger = Logger.noop(),
        )
    }
}
