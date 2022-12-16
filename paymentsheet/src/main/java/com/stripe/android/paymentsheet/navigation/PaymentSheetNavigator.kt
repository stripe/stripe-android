package com.stripe.android.paymentsheet.navigation

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.paymentsheet.PaymentSheetAddPaymentMethodFragment
import com.stripe.android.paymentsheet.PaymentSheetListFragment
import com.stripe.android.paymentsheet.PaymentSheetLoadingFragment
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.utils.AnimationConstants

/**
 * This is a helper class that transforms our backstack state into events for [FragmentManager].
 *
 * Once more of PaymentSheet is powered by Compose, we can instead make the backstack an observable
 * property on our view model.
 */
internal class PaymentSheetNavigator {

    private var fragmentManager: FragmentManager? = null

    fun attach(activity: BaseSheetActivity<*>) {
        this.fragmentManager = activity.supportFragmentManager

        activity.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    fragmentManager = null
                    super.onDestroy(owner)
                }
            }
        )
    }

    fun handle(backstack: List<BaseSheetViewModel.TransitionTarget>) {
        val fragmentManager = fragmentManager ?: return
        val expectedSize = backstack.size

        val isLoading =
            fragmentManager.findFragmentById(R.id.fragment_container) is PaymentSheetLoadingFragment

        val observedSize = if (isLoading) {
            0
        } else {
            fragmentManager.backStackEntryCount + 1
        }

        if (observedSize > expectedSize) {
            fragmentManager.popBackStack()
        } else if (observedSize < expectedSize) {
            navigate(backstack.last())
        }
    }

    private fun navigate(
        transitionTarget: BaseSheetViewModel.TransitionTarget,
    ) {
        val fragmentManager = fragmentManager ?: return
        val fragmentContainerId = R.id.fragment_container

        fragmentManager.commit {
            when (transitionTarget) {
                is BaseSheetViewModel.TransitionTarget.AddAnotherPaymentMethod -> {
                    setCustomAnimations(
                        AnimationConstants.FADE_IN,
                        AnimationConstants.FADE_OUT,
                        AnimationConstants.FADE_IN,
                        AnimationConstants.FADE_OUT
                    )
                    addToBackStack(null)
                    replace(
                        fragmentContainerId,
                        PaymentSheetAddPaymentMethodFragment::class.java,
                        null
                    )
                }
                is BaseSheetViewModel.TransitionTarget.SelectSavedPaymentMethods -> {
                    setCustomAnimations(
                        AnimationConstants.FADE_IN,
                        AnimationConstants.FADE_OUT,
                        AnimationConstants.FADE_IN,
                        AnimationConstants.FADE_OUT
                    )
                    replace(
                        fragmentContainerId,
                        PaymentSheetListFragment::class.java,
                        null
                    )
                }
                is BaseSheetViewModel.TransitionTarget.AddFirstPaymentMethod -> {
                    setCustomAnimations(
                        AnimationConstants.FADE_IN,
                        AnimationConstants.FADE_OUT,
                        AnimationConstants.FADE_IN,
                        AnimationConstants.FADE_OUT
                    )
                    replace(
                        fragmentContainerId,
                        PaymentSheetAddPaymentMethodFragment::class.java,
                        null
                    )
                }
            }
        }
    }
}
