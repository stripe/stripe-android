package com.stripe.android.ui.core

import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.stripecardscan.cardscan.CardScanConfiguration
import com.stripe.android.stripecardscan.cardscan.CardScanSheet
import com.stripe.android.stripecardscan.cardscan.CardScanSheetResult

/**
 * Proxy to access stripecardscan code safely
 *
 */
internal interface StripeCardScanProxy {
    fun present(configuration: CardScanConfiguration)

    fun attachCardScanFragment(
        lifecycleOwner: LifecycleOwner,
        supportFragmentManager: FragmentManager,
        @IdRes fragmentContainer: Int,
        onFinished: (cardScanSheetResult: CardScanSheetResult) -> Unit
    )

    companion object {
        fun create(
            fragment: Fragment,
            onFinished: (cardScanSheetResult: CardScanSheetResult) -> Unit,
            provider: () -> StripeCardScanProxy = {
                DefaultStripeCardScanProxy(CardScanSheet.create(fragment, onFinished))
            },
        ): StripeCardScanProxy {
            return provider()
        }

        fun create(
            activity: AppCompatActivity,
            onFinished: (cardScanSheetResult: CardScanSheetResult) -> Unit,
            provider: () -> StripeCardScanProxy = {
                DefaultStripeCardScanProxy(CardScanSheet.create(activity, onFinished))
            },
        ): StripeCardScanProxy {
            return provider()
        }

        fun removeCardScanFragment(
            supportFragmentManager: FragmentManager,
        ) {
            CardScanSheet.removeCardScanFragment(supportFragmentManager)
        }
    }
}

internal class DefaultStripeCardScanProxy(
    private val cardScanSheet: CardScanSheet
) : StripeCardScanProxy {
    override fun present(configuration: CardScanConfiguration) {
        cardScanSheet.present(configuration)
    }

    override fun attachCardScanFragment(
        lifecycleOwner: LifecycleOwner,
        supportFragmentManager: FragmentManager,
        fragmentContainer: Int,
        onFinished: (cardScanSheetResult: CardScanSheetResult) -> Unit
    ) {
        cardScanSheet.attachCardScanFragment(lifecycleOwner, supportFragmentManager, fragmentContainer, onFinished)
    }
}
