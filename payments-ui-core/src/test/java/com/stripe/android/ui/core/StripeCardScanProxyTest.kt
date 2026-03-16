package com.stripe.android.ui.core

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.stripecardscan.cardscan.CardScanConfiguration
import com.stripe.android.stripecardscan.cardscan.CardScanSheet
import com.stripe.android.stripecardscan.cardscan.CardScanSheetResult
import org.junit.Test
import org.mockito.kotlin.mock
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StripeCardScanProxyTest {
    companion object {
        private const val CARDSCANSHEET_CANONICAL_NAME =
            "com.stripe.android.stripecardscan.cardscan.CardScanSheet"
    }

    private val mockFragment: Fragment = mock()
    private val mockActivity: AppCompatActivity = mock()

    private class FakeProxy : StripeCardScanProxy {
        override fun present(configuration: CardScanConfiguration) {
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
    fun `StripeCardScan proxy returns provided proxy for fragment`() {
        assertTrue(
            StripeCardScanProxy.create(
                fragment = mockFragment,
                onFinished = {},
                provider = { FakeProxy() },
            ) is FakeProxy
        )
    }

    @Test
    fun `StripeCardScan proxy returns provided proxy for activity`() {
        assertTrue(
            StripeCardScanProxy.create(
                activity = mockActivity,
                onFinished = {},
                provider = { FakeProxy() },
            ) is FakeProxy
        )
    }

    @Test
    fun `ensure CardScanSheet exists`() {
        assertEquals(CARDSCANSHEET_CANONICAL_NAME, CardScanSheet::class.qualifiedName)
    }
}
