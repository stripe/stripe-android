package com.stripe.android.paymentsheet

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class DefaultPrefsRepositoryTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private var isGooglePayReady = true
    private val prefsRepository = DefaultPrefsRepository(
        ApplicationProvider.getApplicationContext(),
        "cus_123",
        testDispatcher
    )

    @Test
    fun `save GooglePay should return GooglePay`() = runTest {
        prefsRepository.savePaymentSelection(
            PaymentSelection.GooglePay
        )
        assertThat(
            prefsRepository.getSavedSelection(isGooglePayReady)
        ).isEqualTo(
            SavedSelection.GooglePay
        )
    }

    @Test
    fun `save then get GooglePay should return None if Google Pay is not available`() =
        runTest {
            isGooglePayReady = false

            prefsRepository.savePaymentSelection(
                PaymentSelection.GooglePay
            )
            assertThat(
                prefsRepository.getSavedSelection(isGooglePayReady)
            ).isEqualTo(
                SavedSelection.None
            )
        }

    @Test
    fun `save Saved should return PaymentMethod`() = runTest {
        prefsRepository.savePaymentSelection(
            PaymentSelection.Saved(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD
            )
        )
        assertThat(
            prefsRepository.getSavedSelection(isGooglePayReady)
        ).isEqualTo(
            SavedSelection.PaymentMethod(
                id = "pm_123456789"
            )
        )
    }
}
