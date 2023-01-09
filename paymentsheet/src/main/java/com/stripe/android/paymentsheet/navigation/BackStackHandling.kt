package com.stripe.android.paymentsheet.navigation

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.stripe.android.paymentsheet.BaseAddPaymentMethodFragment
import com.stripe.android.paymentsheet.BasePaymentMethodsListFragment
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel.TransitionTarget

internal sealed interface NavigationEffect {
    data class Navigate(val target: TransitionTarget) : NavigationEffect
    object GoBack : NavigationEffect
}

internal fun TransitionTarget.toNavigationEffect(host: AppCompatActivity): NavigationEffect? {
    val currentFragment = host.supportFragmentManager.findFragmentById(R.id.fragment_container)
    val canIgnoreTransition = canIgnoreTransition(currentFragment, target = this)

    if (canIgnoreTransition) {
        return null
    }

    val hasBackStack = host.supportFragmentManager.backStackEntryCount > 0

    return when (this) {
        TransitionTarget.SelectSavedPaymentMethods -> {
            if (hasBackStack) {
                NavigationEffect.GoBack
            } else {
                NavigationEffect.Navigate(this)
            }
        }
        TransitionTarget.AddFirstPaymentMethod,
        TransitionTarget.AddAnotherPaymentMethod -> {
            NavigationEffect.Navigate(this)
        }
    }
}

internal fun canIgnoreTransition(
    currentFragment: Fragment?,
    target: TransitionTarget,
): Boolean {
    return when (target) {
        TransitionTarget.SelectSavedPaymentMethods -> {
            currentFragment is BasePaymentMethodsListFragment
        }
        TransitionTarget.AddFirstPaymentMethod,
        TransitionTarget.AddAnotherPaymentMethod -> {
            currentFragment is BaseAddPaymentMethodFragment
        }
    }
}
