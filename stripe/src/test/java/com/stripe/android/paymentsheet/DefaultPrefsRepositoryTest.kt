package com.stripe.android.paymentsheet

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class DefaultPrefsRepositoryTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val googlePayRepository = FakeGooglePayRepository(true)
    private val prefsRepository = DefaultPrefsRepository(
        ApplicationProvider.getApplicationContext(),
        "cus_123",
        googlePayRepository,
        testDispatcher
    )

    @Test
    fun `save GooglePay should return GooglePay`() = testDispatcher.runBlockingTest {
        prefsRepository.savePaymentSelection(
            PaymentSelection.GooglePay
        )
        assertThat(
            prefsRepository.getSavedSelection()
        ).isEqualTo(
            SavedSelection.GooglePay
        )
    }

    @Test
    fun `save then get GooglePay should return null if Google Pay is not available`() = testDispatcher.runBlockingTest {
        googlePayRepository.isReady = false

        prefsRepository.savePaymentSelection(
            PaymentSelection.GooglePay
        )
        assertThat(
            prefsRepository.getSavedSelection()
        ).isNull()
    }

    @Test
    fun `save Saved should return PaymentMethod`() = testDispatcher.runBlockingTest {
        prefsRepository.savePaymentSelection(
            PaymentSelection.Saved(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD
            )
        )
        assertThat(
            prefsRepository.getSavedSelection()
        ).isEqualTo(
            SavedSelection.PaymentMethod(
                id = "pm_123456789"
            )
        )
    }
}
