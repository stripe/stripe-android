package com.stripe.android.stripe3ds2.views

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization
import com.stripe.android.stripe3ds2.observability.ErrorReporter
import com.stripe.android.stripe3ds2.transaction.ChallengeActionHandler
import com.stripe.android.stripe3ds2.transaction.ErrorRequestExecutor
import com.stripe.android.stripe3ds2.transaction.IntentData
import com.stripe.android.stripe3ds2.transaction.TransactionTimer
import com.stripe.android.stripe3ds2.transactions.UiType
import com.stripe.android.stripe3ds2.utils.AnalyticsDelegate

internal class ChallengeFragmentFactory(
    private val uiCustomization: StripeUiCustomization,
    private val analyticsDelegate: AnalyticsDelegate?,
    private val transactionTimer: TransactionTimer,
    private val errorRequestExecutor: ErrorRequestExecutor,
    private val errorReporter: ErrorReporter,
    private val challengeActionHandler: ChallengeActionHandler,
    private val initialUiType: UiType?,
    private val intentData: IntentData,
) : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return when (className) {
            ChallengeFragment::class.java.name -> {
                ChallengeFragment(
                    uiCustomization,
                    analyticsDelegate,
                    transactionTimer,
                    errorRequestExecutor,
                    errorReporter,
                    challengeActionHandler,
                    initialUiType,
                    intentData,
                )
            }
            else -> {
                super.instantiate(classLoader, className)
            }
        }
    }
}
