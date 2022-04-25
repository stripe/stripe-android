package com.stripe.android.ui.core

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.stripecardscan.cardscan.CardScanSheet
import com.stripe.android.stripecardscan.cardscan.CardScanSheetResult
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StripeCardScanProxyTest {
    companion object {
        private const val CARDSCANSHEET_CANONICAL_NAME =
            "com.stripe.android.stripecardscan.cardscan.CardScanSheet"
    }

    private val mockIsStripeCardScanAvailable: IsStripeCardScanAvailable = mock()
    private val mockFragment: Fragment = mock()
    private val mockActivity: AppCompatActivity = mock()

    private class FakeProxy : StripeCardScanProxy {
        override fun present() {
            // noop
        }

        override fun attachCardScanFragment(
            lifecycleOwner: LifecycleOwner,
            supportFragmentManager: FragmentManager,
            fragmentContainer: Int,
            onFinished: (cardScanSheetResult: CardScanSheetResult) -> Unit
        ) {
            // noop
        }
    }

    @Test
    fun `StripeCardScan SDK availability returns null when connections module is not loaded`() {
        assertTrue(
            StripeCardScanProxy.create(
                fragment = mockFragment,
                stripePublishableKey = "test",
                onFinished = {},
                isStripeCardScanAvailable = mockIsStripeCardScanAvailable
            ) is UnsupportedStripeCardScanProxy
        )
        assertTrue(
            StripeCardScanProxy.create(
                activity = mockActivity,
                stripePublishableKey = "test",
                onFinished = {},
                isStripeCardScanAvailable = mockIsStripeCardScanAvailable
            ) is UnsupportedStripeCardScanProxy
        )
    }

    @Test
    fun `StripeCardScan SDK availability returns sdk when stripecardscan module is loaded`() {
        whenever(mockIsStripeCardScanAvailable()).thenAnswer { true }

        assertTrue(
            StripeCardScanProxy.create(
                fragment = mockFragment,
                stripePublishableKey = "test",
                onFinished = {},
                isStripeCardScanAvailable = mockIsStripeCardScanAvailable,
                provider = { FakeProxy() }
            ) is FakeProxy
        )
        assertTrue(
            StripeCardScanProxy.create(
                activity = mockActivity,
                stripePublishableKey = "test",
                onFinished = {},
                isStripeCardScanAvailable = mockIsStripeCardScanAvailable,
                provider = { FakeProxy() }
            ) is FakeProxy
        )
    }

    @Test
    fun `calling present on UnsupportedStripeCardScanProxy throws an exception`() {
        assertFailsWith<IllegalStateException> {
            StripeCardScanProxy.create(
                fragment = mockFragment,
                stripePublishableKey = "",
                onFinished = {},
                isStripeCardScanAvailable = mockIsStripeCardScanAvailable
            ).present()
        }
        assertFailsWith<IllegalStateException> {
            StripeCardScanProxy.create(
                activity = mockActivity,
                stripePublishableKey = "",
                onFinished = {},
                isStripeCardScanAvailable = mockIsStripeCardScanAvailable
            ).present()
        }
    }

    @Test
    fun `ensure CardScanSheet exists`() {
        assertEquals(CARDSCANSHEET_CANONICAL_NAME, CardScanSheet::class.qualifiedName)
    }
}
