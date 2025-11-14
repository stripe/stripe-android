package com.stripe.android.paymentsheet

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.toSavedSelection
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class DefaultPrefsRepositoryTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private var isGooglePayReady = true
    private var isLinkAvailable = true
    private val prefsRepository = DefaultPrefsRepository(
        ApplicationProvider.getApplicationContext(),
        "cus_123",
        testDispatcher
    )

    @Test
    fun `setSavedSelection should return the saved payment method`() = runTest {
        prefsRepository.setSavedSelection(
            PaymentSelection.Saved(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD
            ).toSavedSelection()
        )
        assertThat(
            prefsRepository.getSavedSelection(isGooglePayReady, isLinkAvailable)
        ).isEqualTo(
            SavedSelection.PaymentMethod(
                id = "pm_123456789",
                isLinkOrigin = false,
            )
        )
    }

    @Test
    fun `setSavedSelection null should return none`() = runTest {
        prefsRepository.setSavedSelection(
            PaymentSelection.Saved(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD
            ).toSavedSelection()
        )
        assertThat(
            prefsRepository.getSavedSelection(isGooglePayReady, isLinkAvailable)
        ).isEqualTo(
            SavedSelection.PaymentMethod(
                id = "pm_123456789",
                isLinkOrigin = false,
            )
        )

        prefsRepository.setSavedSelection(null)
        assertThat(
            prefsRepository.getSavedSelection(isGooglePayReady, isLinkAvailable)
        ).isEqualTo(
            SavedSelection.None
        )
    }

    @Test
    fun `setSavedSelection with a Link SPM should return isLinkOrigin=true`() = runTest {
        prefsRepository.setSavedSelection(
            PaymentSelection.Saved(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
                    isLinkPassthroughMode = true,
                )
            ).toSavedSelection()
        )
        assertThat(
            prefsRepository.getSavedSelection(isGooglePayReady, isLinkAvailable)
        ).isEqualTo(
            SavedSelection.PaymentMethod(
                id = "pm_123456789",
                isLinkOrigin = true,
            )
        )
    }
}
