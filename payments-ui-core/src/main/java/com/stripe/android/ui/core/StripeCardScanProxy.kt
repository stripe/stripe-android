package com.stripe.android.ui.core

import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.BuildConfig
import com.stripe.android.stripecardscan.cardscan.CardScanSheet
import com.stripe.android.stripecardscan.cardscan.CardScanSheetResult

/**
 * Proxy to access stripecardscan code safely
 *
 */
internal interface StripeCardScanProxy {
    fun present()

    fun attachCardScanFragment(
        lifecycleOwner: LifecycleOwner,
        supportFragmentManager: FragmentManager,
        @IdRes fragmentContainer: Int,
        onFinished: (cardScanSheetResult: CardScanSheetResult) -> Unit
    )

    companion object {
        fun create(
            fragment: Fragment,
            stripePublishableKey: String,
            onFinished: (cardScanSheetResult: CardScanSheetResult) -> Unit,
            provider: () -> StripeCardScanProxy = {
                DefaultStripeCardScanProxy(CardScanSheet.create(fragment, stripePublishableKey, onFinished))
            },
            isStripeCardScanAvailable: IsStripeCardScanAvailable = DefaultIsStripeCardScanAvailable()
        ): StripeCardScanProxy {
            return if (isStripeCardScanAvailable()) {
                provider()
            } else {
                UnsupportedStripeCardScanProxy()
            }
        }

        fun create(
            activity: AppCompatActivity,
            stripePublishableKey: String,
            onFinished: (cardScanSheetResult: CardScanSheetResult) -> Unit,
            provider: () -> StripeCardScanProxy = {
                DefaultStripeCardScanProxy(CardScanSheet.create(activity, stripePublishableKey, onFinished))
            },
            isStripeCardScanAvailable: IsStripeCardScanAvailable = DefaultIsStripeCardScanAvailable()
        ): StripeCardScanProxy {
            return if (isStripeCardScanAvailable()) {
                provider()
            } else {
                UnsupportedStripeCardScanProxy()
            }
        }

        fun removeCardScanFragment(
            supportFragmentManager: FragmentManager,
            isStripeCardScanAvailable: IsStripeCardScanAvailable = DefaultIsStripeCardScanAvailable()
        ) {
            if (isStripeCardScanAvailable()) {
                CardScanSheet.removeCardScanFragment(supportFragmentManager)
            }
        }
    }
}

internal class DefaultStripeCardScanProxy(
    private val cardScanSheet: CardScanSheet
) : StripeCardScanProxy {
    override fun present() {
        cardScanSheet.present()
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

internal class UnsupportedStripeCardScanProxy : StripeCardScanProxy {
    override fun present() {
        if (BuildConfig.DEBUG) {
            throw IllegalStateException(
                "Missing stripecardscan dependency, please add it to your apps build.gradle"
            )
        }
    }

    override fun attachCardScanFragment(
        lifecycleOwner: LifecycleOwner,
        supportFragmentManager: FragmentManager,
        fragmentContainer: Int,
        onFinished: (cardScanSheetResult: CardScanSheetResult) -> Unit
    ) {
        if (BuildConfig.DEBUG) {
            throw IllegalStateException(
                "Missing stripecardscan dependency, please add it to your apps build.gradle"
            )
        }
    }
}
