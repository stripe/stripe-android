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
    val canIgnoreTransition = currentFragment?.canIgnoreTransition(this) ?: true

    if (canIgnoreTransition) {
        return null
    }

    return when (this) {
        SelectSavedPaymentMethods -> {
            if (currentFragment is BaseAddPaymentMethodFragment) {
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

internal fun Fragment.canIgnoreTransition(
    target: TransitionTarget,
): Boolean {
    return when (target) {
        SelectSavedPaymentMethods -> {
            this is BasePaymentMethodsListFragment
        }
        AddFirstPaymentMethod,
        AddAnotherPaymentMethod -> {
            this is BaseAddPaymentMethodFragment
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
