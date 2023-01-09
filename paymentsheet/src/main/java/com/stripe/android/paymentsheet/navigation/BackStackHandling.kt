@file:Suppress("MatchingDeclarationName")

package com.stripe.android.paymentsheet.navigation

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.stripe.android.paymentsheet.BaseAddPaymentMethodFragment
import com.stripe.android.paymentsheet.BasePaymentMethodsListFragment
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel.TransitionTarget
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel.TransitionTarget.AddAnotherPaymentMethod
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel.TransitionTarget.AddFirstPaymentMethod
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel.TransitionTarget.SelectSavedPaymentMethods

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
        SelectSavedPaymentMethods -> {
            if (hasBackStack) {
                NavigationEffect.GoBack
            } else {
                NavigationEffect.Navigate(this)
            }
        }
        AddFirstPaymentMethod,
        AddAnotherPaymentMethod -> {
            NavigationEffect.Navigate(this)
        }
    }
}

internal fun canIgnoreTransition(
    currentFragment: Fragment?,
    target: TransitionTarget,
): Boolean {
    return when (target) {
        SelectSavedPaymentMethods -> {
            currentFragment is BasePaymentMethodsListFragment
        }
        AddFirstPaymentMethod,
        AddAnotherPaymentMethod -> {
            currentFragment is BaseAddPaymentMethodFragment
        }
    }
}

internal fun FragmentManager.constructBackStack(): List<TransitionTarget> {
    return fragments.mapIndexedNotNull { index, fragment ->
        when (fragment) {
            is BaseAddPaymentMethodFragment -> {
                if (index > 0) {
                    AddAnotherPaymentMethod
                } else {
                    AddFirstPaymentMethod
                }
            }
            is BasePaymentMethodsListFragment -> {
                SelectSavedPaymentMethods
            }
            else -> {
                null
            }
        }
    }
}
