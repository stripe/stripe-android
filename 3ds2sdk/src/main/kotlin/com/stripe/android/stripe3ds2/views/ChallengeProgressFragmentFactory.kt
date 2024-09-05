package com.stripe.android.stripe3ds2.views

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId

class ChallengeProgressFragmentFactory(
    private val directoryServerName: String,
    private val sdkTransactionId: SdkTransactionId,
    private val accentColor: Int?
) : FragmentFactory() {

    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return when (className) {
            ChallengeProgressFragment::class.java.name -> {
                ChallengeProgressFragment(
                    directoryServerName,
                    sdkTransactionId,
                    accentColor
                )
            }
            else -> {
                super.instantiate(classLoader, className)
            }
        }
    }
}
