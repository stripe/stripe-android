package com.stripe.android.ui.core

import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.BuildConfig
import com.stripe.android.core.exception.StripeException
import com.stripe.android.payments.core.analytics.ErrorReporter
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
            errorReporter: ErrorReporter,
            provider: () -> StripeCardScanProxy = {
                DefaultStripeCardScanProxy(CardScanSheet.create(fragment, stripePublishableKey, onFinished))
            },
            isStripeCardScanAvailable: IsStripeCardScanAvailable = DefaultIsStripeCardScanAvailable(),
        ): StripeCardScanProxy {
            return if (isStripeCardScanAvailable()) {
                provider()
            } else {
                UnsupportedStripeCardScanProxy(errorReporter)
            }
        }

        fun create(
            activity: AppCompatActivity,
            stripePublishableKey: String,
            onFinished: (cardScanSheetResult: CardScanSheetResult) -> Unit,
            errorReporter: ErrorReporter,
            provider: () -> StripeCardScanProxy = {
                DefaultStripeCardScanProxy(CardScanSheet.create(activity, stripePublishableKey, onFinished))
            },
            isStripeCardScanAvailable: IsStripeCardScanAvailable = DefaultIsStripeCardScanAvailable(),
        ): StripeCardScanProxy {
            return if (isStripeCardScanAvailable()) {
                provider()
            } else {
                UnsupportedStripeCardScanProxy(errorReporter)
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

internal class UnsupportedStripeCardScanProxy(private val errorReporter: ErrorReporter) : StripeCardScanProxy {
    override fun present() {
        val illegalStateException = IllegalStateException(
            "Missing stripecardscan dependency, please add it to your apps build.gradle"
        )
        if (BuildConfig.DEBUG) {
            throw illegalStateException
        } else {
            errorReporter.report(
                ErrorReporter.ErrorEvent.MISSING_CARDSCAN_DEPENDENCY,
                StripeException.create(illegalStateException)
            )
        }
    }

    override fun attachCardScanFragment(
        lifecycleOwner: LifecycleOwner,
        supportFragmentManager: FragmentManager,
        fragmentContainer: Int,
        onFinished: (cardScanSheetResult: CardScanSheetResult) -> Unit
    ) {
        val illegalStateException = IllegalStateException(
            "Missing stripecardscan dependency, please add it to your apps build.gradle"
        )
        if (BuildConfig.DEBUG) {
            throw illegalStateException
        } else {
            errorReporter.report(
                ErrorReporter.ErrorEvent.MISSING_CARDSCAN_DEPENDENCY,
                StripeException.create(illegalStateException)
            )
        }
    }
}
